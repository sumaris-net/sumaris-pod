package net.sumaris.core.dao.administration.programStrategy;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.sumaris.core.dao.cache.CacheNames;
import net.sumaris.core.dao.referential.BaseRefRepository;
import net.sumaris.core.dao.referential.ReferentialRepositoryImpl;
import net.sumaris.core.dao.referential.taxon.TaxonGroupRepository;
import net.sumaris.core.dao.schema.DatabaseSchemaDao;
import net.sumaris.core.dao.schema.event.DatabaseSchemaListener;
import net.sumaris.core.dao.schema.event.SchemaUpdatedEvent;
import net.sumaris.core.model.administration.programStrategy.*;
import net.sumaris.core.model.referential.Status;
import net.sumaris.core.model.referential.StatusEnum;
import net.sumaris.core.model.referential.gear.Gear;
import net.sumaris.core.model.referential.gear.GearClassification;
import net.sumaris.core.model.referential.location.Location;
import net.sumaris.core.model.referential.location.LocationClassification;
import net.sumaris.core.model.referential.taxon.TaxonGroup;
import net.sumaris.core.model.referential.taxon.TaxonGroupType;
import net.sumaris.core.util.Beans;
import net.sumaris.core.vo.administration.programStrategy.ProgramFetchOptions;
import net.sumaris.core.vo.administration.programStrategy.ProgramVO;
import net.sumaris.core.vo.filter.ProgramFilterVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import net.sumaris.core.vo.referential.TaxonGroupVO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.jpa.domain.Specification;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.criteria.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author peck7 on 24/08/2020.
 */
public class ProgramRepositoryImpl
    extends ReferentialRepositoryImpl<Program, ProgramVO, ProgramFilterVO, ProgramFetchOptions>
    implements ProgramSpecifications, DatabaseSchemaListener {

    private static final Logger log =
        LoggerFactory.getLogger(ReferentialRepositoryImpl.class);


    @Autowired
    private BaseRefRepository baseRefRepository;

    @Autowired
    private DatabaseSchemaDao databaseSchemaDao;

    @Autowired
    private TaxonGroupRepository taxonGroupRepository;

    @Override
    public void onSchemaUpdated(SchemaUpdatedEvent event) {
        initProgramEnumerations();
    }

    public ProgramRepositoryImpl(EntityManager entityManager) {
        super(Program.class, ProgramVO.class, entityManager);
        setLockForUpdate(true);
    }

    @PostConstruct
    protected void init() {
        databaseSchemaDao.addListener(this);
    }

    @Override
    @Cacheable(cacheNames = CacheNames.PROGRAM_BY_ID)
    public Optional<ProgramVO> findById(int id) {
        return super.findById(id);
    }

    @Override
    @Cacheable(cacheNames = CacheNames.PROGRAM_BY_LABEL)
    public Optional<ProgramVO> findByLabel(String label) {
        return super.findByLabel(label);
    }

    @Override
    protected Specification<Program> toSpecification(ProgramFilterVO filter) {
        return super.toSpecification(filter)
            .and(hasProperty(filter.getWithProperty()));
    }

    @Override
    public ProgramVO toVO(Program source, ProgramFetchOptions fetchOptions) {
        if (fetchOptions == null)
            fetchOptions = ProgramFetchOptions.builder()
                .withProperties(true) // force with properties if fetch options not provided
                .build();
        return super.toVO(source, fetchOptions);
    }

    @Override
    protected void toVO(Program source, ProgramVO target, ProgramFetchOptions fetchOptions, boolean copyIfNull) {
        super.toVO(source, target, fetchOptions, copyIfNull);

        // properties
        if (fetchOptions != null && fetchOptions.isWithProperties()) {
            Map<String, String> properties = Maps.newHashMap();
            Beans.getStream(source.getProperties())
                .filter(prop -> Objects.nonNull(prop)
                    && Objects.nonNull(prop.getLabel())
                    && Objects.nonNull(prop.getName())
                )
                .forEach(prop -> {
                    if (properties.containsKey(prop.getLabel())) {
                        log.warn(String.format("Duplicate program property with label {%s}. Overriding existing value with {%s}", prop.getLabel(), prop.getName()));
                    }
                    properties.put(prop.getLabel(), prop.getName());
                });
            target.setProperties(properties);
        }

        // Other attributes
        target.setGearClassificationId(source.getGearClassification() != null ? source.getGearClassification().getId() : null);
        target.setTaxonGroupTypeId(source.getTaxonGroupType() != null ? source.getTaxonGroupType().getId() : null);

        // locations
        if (fetchOptions != null && fetchOptions.isWithLocations()) {
            target.setLocationClassifications(
                Beans.getStream(source.getLocationClassifications())
                    .map(baseRefRepository::toVO)
                    .collect(Collectors.toList()));

            target.setLocationClassificationIds(
                Beans.getStream(target.getLocationClassifications())
                    .map(ReferentialVO::getId)
                    .collect(Collectors.toList()));

            target.setLocations(
                Beans.getStream(source.getLocations())
                    .map(baseRefRepository::toVO)
                    .collect(Collectors.toList()));
        }
    }

    @Override
    @Caching(
        evict = {
            @CacheEvict(cacheNames = CacheNames.PROGRAM_BY_ID, key = "#vo.id", condition = "#vo.id != null"),
            @CacheEvict(cacheNames = CacheNames.PROGRAM_BY_LABEL, key = "#vo.label", condition = "#vo.label != null"),
        },
        put = {
            @CachePut(cacheNames= CacheNames.PROGRAM_BY_ID, key="#vo.id", condition = " #vo.id != null"),
            @CachePut(cacheNames= CacheNames.PROGRAM_BY_LABEL, key="#vo.label", condition = "#vo.label != null")
        }
    )
    public ProgramVO save(ProgramVO vo) {
        Preconditions.checkNotNull(vo);
        Preconditions.checkNotNull(vo.getLabel(), "Missing 'label'");
        Preconditions.checkNotNull(vo.getName(), "Missing 'name'");
        Preconditions.checkNotNull(vo.getStatusId(), "Missing 'statusId'");

        if (vo.getId() == null && vo.getStatusId() == null)
            // Set default status to Temporary
            vo.setStatusId(config.getStatusIdTemporary());

        return super.save(vo);
    }

    @Override
    public void toEntity(ProgramVO source, Program target, boolean copyIfNull) {
        super.toEntity(source, target, copyIfNull);

        // Gear classification
        Integer gearClassificationId = source.getGearClassificationId() != null ? source.getGearClassificationId() :
            (source.getGearClassification() != null ? source.getGearClassification().getId() : null);
        if (copyIfNull || gearClassificationId != null) {
            if (gearClassificationId == null) {
                target.setGearClassification(null);
            }
            else {
                target.setGearClassification(load(GearClassification.class, gearClassificationId));
            }
        }

        // Taxon group type
        Integer taxonGroupTypeId = source.getTaxonGroupTypeId() != null ? source.getTaxonGroupTypeId() :
            (source.getTaxonGroupType() != null ? source.getTaxonGroupType().getId() : null);
        if (copyIfNull || taxonGroupTypeId != null) {
            if (taxonGroupTypeId == null) {
                target.setTaxonGroupType(null);
            }
            else {
                target.setTaxonGroupType(load(TaxonGroupType.class, taxonGroupTypeId));
            }
        }

        // Location classifications
        List<Integer> locationClassificationIds = CollectionUtils.isNotEmpty(source.getLocationClassificationIds()) ?
            source.getLocationClassificationIds() :
            (CollectionUtils.isNotEmpty(source.getLocationClassifications()) ?
                Beans.collectIds(source.getLocationClassifications()) :
                null);
        if (copyIfNull || CollectionUtils.isNotEmpty(locationClassificationIds)) {
            target.getLocationClassifications().clear();
            if (CollectionUtils.isNotEmpty(locationClassificationIds)) {
                target.getLocationClassifications().addAll(loadAllAsSet(LocationClassification.class, locationClassificationIds, true));
            }
        }

        // Locations
        List<Integer> locationIds = CollectionUtils.isNotEmpty(source.getLocationIds()) ?
            source.getLocationIds() :
            (CollectionUtils.isNotEmpty(source.getLocations()) ?
                Beans.collectIds(source.getLocations()) :
                null);
        if (copyIfNull || CollectionUtils.isNotEmpty(locationIds)) {
            target.getLocations().clear();
            if (CollectionUtils.isNotEmpty(locationIds)) {
                target.getLocations().addAll(loadAllAsSet(Location.class, locationIds, true));
            }
        }
    }

    @Override
    protected void onAfterSaveEntity(ProgramVO vo, Program savedEntity, boolean isNew) {
        super.onAfterSaveEntity(vo, savedEntity, isNew);

        // Save properties
        saveProperties(vo.getProperties(), savedEntity, savedEntity.getUpdateDate());

        getEntityManager().flush();
        getEntityManager().clear();

    }

    protected void saveProperties(Map<String, String> source, Program parent, Date updateDate) {
        final EntityManager em = getEntityManager();
        if (MapUtils.isEmpty(source)) {
            if (parent.getProperties() != null) {
                List<ProgramProperty> toRemove = ImmutableList.copyOf(parent.getProperties());
                parent.getProperties().clear();
                toRemove.forEach(em::remove);
            }
        }
        else {
            Map<String, ProgramProperty> existingProperties = Beans.splitByProperty(
                Beans.getList(parent.getProperties()),
                ProgramProperty.Fields.LABEL);
            final Status enableStatus = em.getReference(Status.class, StatusEnum.ENABLE.getId());
            if (parent.getProperties() == null) {
                parent.setProperties(Lists.newArrayList());
            }
            final List<ProgramProperty> targetProperties = parent.getProperties();

            // Transform each entry into ProgramProperty
            source.entrySet().stream()
                .filter(e -> Objects.nonNull(e.getKey())
                    && Objects.nonNull(e.getValue())
                )
                .map(e -> {
                    ProgramProperty prop = existingProperties.remove(e.getKey());
                    boolean isNew = (prop == null);
                    if (isNew) {
                        prop = new ProgramProperty();
                        prop.setLabel(e.getKey());
                        prop.setProgram(parent);
                        prop.setCreationDate(updateDate);
                    }
                    prop.setName(e.getValue());
                    prop.setStatus(enableStatus);
                    prop.setUpdateDate(updateDate);
                    if (isNew) {
                        em.persist(prop);
                    }
                    else {
                        em.merge(prop);
                    }
                    return prop;
                })
                .forEach(targetProperties::add);

            // Remove old properties
            if (MapUtils.isNotEmpty(existingProperties)) {
                parent.getProperties().removeAll(existingProperties.values());
                existingProperties.values().forEach(em::remove);
            }

        }
    }

    @Override
    @Caching(
        evict = {
            @CacheEvict(cacheNames = CacheNames.PROGRAM_BY_ID, key = "#id", condition = "#id != null"),
            @CacheEvict(cacheNames = CacheNames.PROGRAM_BY_LABEL, allEntries = true)
        }
    )
    public void deleteById(Integer id) {
        super.deleteById(id);
    }

    protected boolean initProgramEnumerations() {
        log.debug("Initialize enumeration for all programs...");
        for (ProgramEnum programEnum: ProgramEnum.values()) {
            try {
                ProgramVO program = getByLabel(programEnum.name());
                if (program != null) {
                    programEnum.setId(program.getId());
                } else {
                    // TODO query by id and show program code/name
                    log.warn("Missing program with label=" + programEnum.name());
                }
            } catch(Throwable t) {
                log.error(String.format("Could not initialized enumeration for program {%s}: %s", programEnum.name(), t.getMessage()), t);
                return false;
            }
        }
        return true;
    }

    @Override
    public List<TaxonGroupVO> getTaxonGroups(int programId) {
        CriteriaBuilder builder = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<TaxonGroup> query = builder.createQuery(TaxonGroup.class);
        Root<TaxonGroup> root = query.from(TaxonGroup.class);

        ParameterExpression<Integer> programIdParam = builder.parameter(Integer.class);

        Join<TaxonGroup, TaxonGroupStrategy> innerJoinTGS = root.joinList(TaxonGroup.Fields.STRATEGIES, JoinType.INNER);
        Join<TaxonGroupStrategy, Strategy> innerJoinS = innerJoinTGS.join(TaxonGroupStrategy.Fields.STRATEGY, JoinType.INNER);


        query.select(root)
            .where(
                builder.and(
                    // program
                    builder.equal(innerJoinS.get(Strategy.Fields.PROGRAM).get(Program.Fields.ID), programIdParam),
                    // Status (temporary or valid)
                    builder.in(root.get(TaxonGroup.Fields.STATUS).get(Status.Fields.ID)).value(ImmutableList.of(StatusEnum.ENABLE.getId(), StatusEnum.TEMPORARY.getId()))
                ));

        // Sort by rank order
        query.orderBy(builder.asc(root.get(TaxonGroup.Fields.LABEL)));

        return getEntityManager()
            .createQuery(query)
            .setParameter(programIdParam, programId)
            .getResultStream()
            .map(taxonGroupRepository::toVO)
            .collect(Collectors.toList());
    }

    @Override
    public List<ReferentialVO> getGears(int programId) {
        CriteriaBuilder builder = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<Gear> query = builder.createQuery(Gear.class);
        Root<Gear> root = query.from(Gear.class);

        ParameterExpression<Integer> programIdParam = builder.parameter(Integer.class);

        Join<Gear, Strategy> gearInnerJoin = root.joinList(Gear.Fields.STRATEGIES, JoinType.INNER);

        query.select(root)
            .where(
                builder.and(
                    // program
                    builder.equal(gearInnerJoin.get(Strategy.Fields.PROGRAM).get(Program.Fields.ID), programIdParam),
                    // Status (temporary or valid)
                    builder.in(root.get(Gear.Fields.STATUS).get(Status.Fields.ID)).value(ImmutableList.of(StatusEnum.ENABLE.getId(), StatusEnum.TEMPORARY.getId()))
                ));

        // Sort by rank order
        query.orderBy(builder.asc(root.get(Gear.Fields.LABEL)));

        return getEntityManager()
            .createQuery(query)
            .setParameter(programIdParam, programId)
            .getResultStream()
            .map(baseRefRepository::toVO)
            .collect(Collectors.toList());
    }
}

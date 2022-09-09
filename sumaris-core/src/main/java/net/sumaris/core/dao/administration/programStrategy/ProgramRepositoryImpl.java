package net.sumaris.core.dao.administration.programStrategy;

/*-
 * #%L
 * SUMARiS:: Core
 * %%
 * Copyright (C) 2018 - 2020 SUMARiS Consortium
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.config.CacheConfiguration;
import net.sumaris.core.dao.administration.user.DepartmentRepository;
import net.sumaris.core.dao.administration.user.PersonRepository;
import net.sumaris.core.dao.referential.ReferentialDao;
import net.sumaris.core.dao.referential.ReferentialRepositoryImpl;
import net.sumaris.core.dao.referential.location.LocationRepository;
import net.sumaris.core.dao.referential.taxon.TaxonGroupRepository;
import net.sumaris.core.dao.technical.jpa.BindableSpecification;
import net.sumaris.core.event.config.ConfigurationEvent;
import net.sumaris.core.event.config.ConfigurationReadyEvent;
import net.sumaris.core.event.config.ConfigurationUpdatedEvent;
import net.sumaris.core.model.administration.programStrategy.*;
import net.sumaris.core.model.administration.user.Department;
import net.sumaris.core.model.administration.user.Person;
import net.sumaris.core.model.referential.Status;
import net.sumaris.core.model.referential.StatusEnum;
import net.sumaris.core.model.referential.gear.Gear;
import net.sumaris.core.model.referential.gear.GearClassification;
import net.sumaris.core.model.referential.location.Location;
import net.sumaris.core.model.referential.location.LocationClassification;
import net.sumaris.core.model.referential.taxon.TaxonGroup;
import net.sumaris.core.model.referential.taxon.TaxonGroupType;
import net.sumaris.core.util.Beans;
import net.sumaris.core.vo.administration.programStrategy.ProgramDepartmentVO;
import net.sumaris.core.vo.administration.programStrategy.ProgramFetchOptions;
import net.sumaris.core.vo.administration.programStrategy.ProgramPersonVO;
import net.sumaris.core.vo.administration.programStrategy.ProgramVO;
import net.sumaris.core.vo.filter.ProgramFilterVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import net.sumaris.core.vo.referential.TaxonGroupVO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.EntityManager;
import javax.persistence.criteria.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author peck7 on 24/08/2020.
 */
@Slf4j
public class ProgramRepositoryImpl
    extends ReferentialRepositoryImpl<Integer, Program, ProgramVO, ProgramFilterVO, ProgramFetchOptions>
    implements ProgramSpecifications {

    @Autowired
    private ReferentialDao referentialDao;

    @Autowired
    private TaxonGroupRepository taxonGroupRepository;

    @Autowired
    private StrategyRepository strategyRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    protected DepartmentRepository departmentRepository;

    @Autowired
    protected PersonRepository personRepository;

    @Autowired
    protected ProgramPrivilegeRepository programPrivilegeRepository;

    @Autowired
    protected AcquisitionLevelRepository acquisitionLevelRepository;

    public Logger getLogger() {
        return log;
    }

    public ProgramRepositoryImpl(EntityManager entityManager) {
        super(Program.class, ProgramVO.class, entityManager);
        setLockForUpdate(true);
        setPublishEvent(true);
    }

    @EventListener({ConfigurationReadyEvent.class, ConfigurationUpdatedEvent.class})
    protected void onConfigurationReady(ConfigurationEvent event) {
        // Force clear cache, because authorized programs can depends on the configuration
        clearCache();
    }

    @Override
    public Optional<ProgramVO> findIfNewerById(int id, Date updateDate, ProgramFetchOptions fetchOptions) {
        return getQuery(
                BindableSpecification.where(hasId(id)).and(newerThan(updateDate)),
                Program.class, Sort.by(Program.Fields.ID)
            )
            .getResultStream()
            .findFirst()
            .map(source -> toVO(source, fetchOptions));
    }

    @Override
    @Cacheable(cacheNames = CacheConfiguration.Names.PROGRAM_BY_ID)
    public Optional<ProgramVO> findVOById(Integer id) {
        return super.findVOById(id);
    }

    @Override
    @Cacheable(cacheNames = CacheConfiguration.Names.PROGRAM_BY_LABEL)
    public Optional<ProgramVO> findByLabel(String label) {
        return super.findByLabel(label);
    }

    @Override
    protected Specification<Program> toSpecification(ProgramFilterVO filter, ProgramFetchOptions fetchOptions) {
        return super.toSpecification(filter, fetchOptions)
            .and(newerThan(filter.getMinUpdateDate()))
            .and(hasAcquisitionLevelLabels(filter.getAcquisitionLevelLabels()))
            .and(hasProperty(filter.getWithProperty()))
            ;
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
                    .map(referentialDao::toVO)
                    .collect(Collectors.toList()));
            target.setLocations(
                Beans.getStream(source.getLocations())
                    .map(referentialDao::toVO)
                    .collect(Collectors.toList()));

            target.setLocationClassificationIds(Beans.collectIds(source.getLocationClassifications()));
            target.setLocationIds(Beans.collectIds(source.getLocations()));
        }
        // Location classifications (only IDs)
        else if (fetchOptions != null && fetchOptions.isWithLocationClassifications()) {
            if (copyIfNull || source.getLocationClassifications() != null) {
                target.setLocationClassificationIds(Beans.collectIds(source.getLocationClassifications()));
            }
        }

        // strategies
        if (fetchOptions != null && fetchOptions.isWithStrategies()) {
            target.setStrategies(
                Beans.getStream(source.getStrategies())
                    .map(strategyRepository::toVO)
                    .collect(Collectors.toList()));
        }

        // Departments
        if (fetchOptions != null && fetchOptions.isWithDepartments()) {
            target.setDepartments(getDepartments(source));
        }

        // Persons
        if (fetchOptions != null && fetchOptions.isWithPersons()) {
            target.setPersons(getPersons(source));
        }

        // AcquisitionLevels
        if (fetchOptions != null && fetchOptions.isWithAcquisitionLevels()) {
            if (target.getId() != null) {
                target.setAcquisitionLevels(getAcquisitionLevelsByProgramId(target.getId()));
            }
            else {
                target.setAcquisitionLevels(null);
            }
        }
    }

    @Override
    @Caching(evict = {
        @CacheEvict(cacheNames = CacheConfiguration.Names.PROGRAM_BY_ID, allEntries = true),
        @CacheEvict(cacheNames = CacheConfiguration.Names.PROGRAM_BY_LABEL, allEntries = true),
        @CacheEvict(cacheNames = CacheConfiguration.Names.PROGRAM_IDS_BY_USER_ID, allEntries = true)
    })
    public void clearCache() {
        log.debug("Cleaning Program's cache...");
    }

    @Override
    @Caching(
        evict = {
            @CacheEvict(cacheNames = CacheConfiguration.Names.PROGRAM_BY_ID, key = "#source.id", condition = "#source.id != null"),
            @CacheEvict(cacheNames = CacheConfiguration.Names.PROGRAM_BY_LABEL, key = "#source.label", condition = "#source.label != null"),
            @CacheEvict(cacheNames = CacheConfiguration.Names.PROGRAM_IDS_BY_USER_ID, allEntries = true)
        },
        put = {
            @CachePut(cacheNames = CacheConfiguration.Names.PROGRAM_BY_ID, key = "#source.id", condition = " #source.id != null"),
            @CachePut(cacheNames = CacheConfiguration.Names.PROGRAM_BY_LABEL, key = "#source.label", condition = "#source.label != null")
        }
    )
    public ProgramVO save(ProgramVO source) {
        Preconditions.checkNotNull(source);
        Preconditions.checkNotNull(source.getLabel(), "Missing 'label'");
        Preconditions.checkNotNull(source.getName(), "Missing 'name'");
        return super.save(source);
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
            } else {
                target.setGearClassification(getReference(GearClassification.class, gearClassificationId));
            }
        }

        // Taxon group type
        Integer taxonGroupTypeId = source.getTaxonGroupTypeId() != null ? source.getTaxonGroupTypeId() :
            (source.getTaxonGroupType() != null ? source.getTaxonGroupType().getId() : null);
        if (copyIfNull || taxonGroupTypeId != null) {
            if (taxonGroupTypeId == null) {
                target.setTaxonGroupType(null);
            } else {
                target.setTaxonGroupType(getReference(TaxonGroupType.class, taxonGroupTypeId));
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
    @Caching(
        evict = {
            @CacheEvict(cacheNames = CacheConfiguration.Names.PROGRAM_BY_ID, key = "#id", condition = "#id != null"),
            @CacheEvict(cacheNames = CacheConfiguration.Names.PROGRAM_BY_LABEL, allEntries = true),
            @CacheEvict(cacheNames = CacheConfiguration.Names.PROGRAM_IDS_BY_USER_ID, allEntries = true)
        }
    )
    public void deleteById(Integer id) {
        super.deleteById(id);
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
            .map(referentialDao::toVO)
            .collect(Collectors.toList());
    }

    @Override
    public boolean hasUserPrivilege(int programId, int personId, ProgramPrivilegeEnum privilege) {
        return getEntityManager().createNamedQuery("ProgramPerson.count", Long.class)
            .setParameter("programId", programId)
            .setParameter("personId", personId)
            .setParameter("privilegeId", privilege.getId())
            .getSingleResult() > 0;
    }

    @Override
    public boolean hasDepartmentPrivilege(int programId, int departmentId, ProgramPrivilegeEnum privilege) {
        return getEntityManager().createNamedQuery("ProgramDepartment.count", Long.class)
            .setParameter("programId", programId)
            .setParameter("departmentId", departmentId)
            .setParameter("privilegeId", privilege.getId())
            .getSingleResult() > 0;
    }

    @Override
    public List<ProgramDepartmentVO> getDepartmentsById(int id) {
        return getDepartments(getReference(Program.class, id));
    }

    @Override
    public List<ProgramPersonVO> getPersonsById(int id) {
        return getPersons(getReference(Program.class, id));
    }

    /* -- protected functions -- */

    @Override
    protected void onBeforeSaveEntity(ProgramVO source, Program target, boolean isNew) {
        // Set default status to Temporary
        if (isNew && source.getStatusId() == null) {
            source.setStatusId(StatusEnum.TEMPORARY.getId());
        }
    }

    @Override
    protected void onAfterSaveEntity(final ProgramVO vo, final Program savedEntity, boolean isNew) {

        super.onAfterSaveEntity(vo, savedEntity, isNew);

        // Save properties
        saveProperties(vo.getProperties(), savedEntity, savedEntity.getUpdateDate());

        // Flush
        EntityManager em = getEntityManager();
        em.flush();
        em.clear();
    }

    @Override
    public List<ProgramDepartmentVO> saveDepartmentsByProgramId(int programId, List<ProgramDepartmentVO> sources) {
        Preconditions.checkNotNull(sources);

        final Program parent = getById(Program.class, programId);

        return saveChildren(
                sources,
                parent.getDepartments(),
                ProgramDepartment.class,
                (source, target, copyIfNull) -> this.toDepartmentEntity(source, target, parent, copyIfNull),
                parent);
    }

    @Override
    public List<ProgramPersonVO> savePersonsByProgramId(int programId, List<ProgramPersonVO> sources) {
        Preconditions.checkNotNull(sources);

        final Program parent = getById(Program.class, programId);

        return saveChildren(
                sources,
                parent.getPersons(),
                ProgramPerson.class,
                (source, target, copyIfNull) -> this.toPersonEntity(source, target, parent, copyIfNull),
                parent);
    }

    protected void saveProperties(Map<String, String> source, Program parent, Date updateDate) {
        final EntityManager em = getEntityManager();
        if (MapUtils.isEmpty(source)) {
            if (parent.getProperties() != null) {
                List<ProgramProperty> toRemove = ImmutableList.copyOf(parent.getProperties());
                parent.getProperties().clear();
                toRemove.forEach(em::remove);
            }
        } else {
            // WARN: database can stored many values for the same keys.
            // Only the first existing instance will be reused. Duplicate properties will be removed
            ListMultimap<String, ProgramProperty> existingPropertiesMap = Beans.splitByNotUniqueProperty(
                Beans.getList(parent.getProperties()),
                ProgramProperty.Fields.LABEL);
            List<ProgramProperty> existingValues = Beans.getList(existingPropertiesMap.values());
            final Status enableStatus = em.getReference(Status.class, StatusEnum.ENABLE.getId());
            if (parent.getProperties() == null) {
                parent.setProperties(Lists.newArrayList());
            }
            final List<ProgramProperty> targetProperties = parent.getProperties();

            // Transform each entry into ProgramProperty
            source.keySet().stream()
                .map(key -> {
                    ProgramProperty prop = existingPropertiesMap.containsKey(key) ? existingPropertiesMap.get(key).get(0) : null;
                    boolean isNew = (prop == null);
                    if (isNew) {
                        prop = new ProgramProperty();
                        prop.setLabel(key);
                        prop.setProgram(parent);
                        prop.setCreationDate(updateDate);
                    } else {
                        existingValues.remove(prop);
                    }
                    prop.setName(source.get(key));
                    prop.setStatus(enableStatus);
                    prop.setUpdateDate(updateDate);
                    if (isNew) {
                        em.persist(prop);
                    } else {
                        em.merge(prop);
                    }
                    return prop;
                })
                .forEach(targetProperties::add);

            // Remove old properties
            if (CollectionUtils.isNotEmpty(existingValues)) {
                parent.getProperties().removeAll(existingValues);
                existingValues.forEach(em::remove);
            }

        }
    }

    protected List<ProgramDepartmentVO> getDepartments(Program source) {
        if (CollectionUtils.isEmpty(source.getDepartments())) return null;
        return source.getDepartments()
            .stream()
            .map(item -> {
                ProgramDepartmentVO target = new ProgramDepartmentVO();
                target.setId(item.getId());
                target.setUpdateDate(item.getUpdateDate());
                target.setProgramId(source.getId());

                if (item.getLocation() != null) {
                    target.setLocation(locationRepository.get(item.getLocation().getId()));
                }

                target.setDepartment(departmentRepository.get(item.getDepartment().getId()));
                target.setPrivilege(programPrivilegeRepository.get(item.getPrivilege().getId()));

                return target;
            })
            .collect(Collectors.toList());
    }

    protected List<ProgramPersonVO> getPersons(Program source) {
        if (CollectionUtils.isEmpty(source.getPersons())) return null;
        return source.getPersons()
            .stream()
            .map(item -> {
                ProgramPersonVO target = new ProgramPersonVO();
                target.setId(item.getId());
                target.setUpdateDate(item.getUpdateDate());
                target.setProgramId(source.getId());

                if (item.getLocation() != null) {
                    target.setLocation(locationRepository.get(item.getLocation().getId()));
                }

                target.setPerson(personRepository.get(item.getPerson().getId()));
                target.setPrivilege(programPrivilegeRepository.get(item.getPrivilege().getId()));

                return target;
            })
            .collect(Collectors.toList());
    }

    @Override
    public List<ReferentialVO> getAcquisitionLevelsByProgramId(int programId) {
        return acquisitionLevelRepository.getDistinctAcquisitionLevelsByProgramId(programId)
            .stream()
            .map(acquisitionLevelRepository::toVO)
            .collect(Collectors.toList());
    }

    protected void toDepartmentEntity(@NonNull ProgramDepartmentVO source,
                                      @NonNull ProgramDepartment target,
                                      @NonNull Program parent, boolean copyIfNull) {
        Preconditions.checkNotNull(parent.getId());

        Beans.copyProperties(source, target);

        // Program
        source.setProgramId(parent.getId());
        target.setProgram(parent);

        // Location
        Integer locationId = source.getLocation() != null ? source.getLocation().getId() : null;
        if (copyIfNull || locationId != null) {
            target.setLocation(locationId != null ? getReference(Location.class, locationId) : null);
        }

        // Department
        Integer departmentId = source.getDepartment() != null ? source.getDepartment().getId() : null;
        if (copyIfNull || departmentId != null) {
            target.setDepartment(departmentId != null ? getReference(Department.class, departmentId) : null);
        }

        // Privilege
        Integer privilegeId = source.getPrivilege() != null ? source.getPrivilege().getId() : null;
        if (copyIfNull || privilegeId != null) {
            target.setPrivilege(privilegeId != null ? getReference(ProgramPrivilege.class, privilegeId) : null);
        }
    }

    protected void toPersonEntity(@NonNull ProgramPersonVO source,
                                  @NonNull ProgramPerson target,
                                  @NonNull Program parent, boolean copyIfNull) {
        Preconditions.checkNotNull(parent.getId());

        Beans.copyProperties(source, target);

        // Program
        source.setProgramId(parent.getId());
        target.setProgram(parent);

        // Location
        Integer locationId = source.getLocation() != null ? source.getLocation().getId() : null;
        if (copyIfNull || locationId != null) {
            if (locationId == null) {
                target.setLocation(null);
            } else {
                target.setLocation(getReference(Location.class, locationId));
            }
        }

        // Person
        Integer personId = source.getPerson() != null ? source.getPerson().getId() : null;
        if (copyIfNull || personId != null) {
            if (personId == null) {
                target.setPerson(null);
            } else {
                target.setPerson(getReference(Person.class, personId));
            }
        }

        // Privilege
        Integer privilegeId = source.getPrivilege() != null ? source.getPrivilege().getId() : null;
        if (copyIfNull || privilegeId != null) {
            if (privilegeId == null) {
                target.setPrivilege(null);
            } else {
                target.setPrivilege(getReference(ProgramPrivilege.class, privilegeId));
            }
        }
    }

    @Override
    public boolean hasPropertyValueByProgramId(@NonNull Integer id, @NonNull ProgramPropertyEnum property, @NonNull String expectedValue) {
        String value = findVOById(id)
                .map(program -> program.getProperties().get(property.getLabel()))
                .orElse(property.getDefaultValue());

        // If boolean: true = TRUE
        if (property.getType() == Boolean.class) {
            return expectedValue.equalsIgnoreCase(value);
        }

        return expectedValue.equals(value);
    }

    @Override
    public boolean hasPropertyValueByProgramLabel(@NonNull String label, @NonNull ProgramPropertyEnum property, @NonNull String expectedValue) {
        String value = findByLabel(label)
                .map(program -> program.getProperties().get(property.getLabel()))
                .orElse(property.getDefaultValue());

        return expectedValue.equals(value);
    }
}

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
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.config.CacheConfiguration;
import net.sumaris.core.dao.referential.ReferentialDao;
import net.sumaris.core.dao.referential.ReferentialRepositoryImpl;
import net.sumaris.core.dao.referential.taxon.TaxonGroupRepository;
import net.sumaris.core.dao.technical.jpa.BindableSpecification;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
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
    extends ReferentialRepositoryImpl<Program, ProgramVO, ProgramFilterVO, ProgramFetchOptions>
    implements ProgramSpecifications {

    @Autowired
    private ReferentialDao referentialDao;

    @Autowired
    private TaxonGroupRepository taxonGroupRepository;

    @Autowired
    private StrategyRepository strategyRepository;

    public ProgramRepositoryImpl(EntityManager entityManager) {
        super(Program.class, ProgramVO.class, entityManager);
        setLockForUpdate(true);
    }

    @Override
    public Optional<ProgramVO> findIfNewerById(int id, Date updateDate, ProgramFetchOptions fetchOptions) {
        return getQuery(BindableSpecification
                .where(hasId(id))
                .and(newerThan(updateDate)), Program.class, Sort.by(Program.Fields.ID))
            .getResultStream()
            .findFirst()
            .map(source -> toVO(source, fetchOptions));
    }

    @Override
    @Cacheable(cacheNames = CacheConfiguration.Names.PROGRAM_BY_ID)
    public Optional<ProgramVO> findById(int id) {
        return super.findById(id);
    }

    @Override
    @Cacheable(cacheNames = CacheConfiguration.Names.PROGRAM_BY_LABEL)
    public Optional<ProgramVO> findByLabel(String label) {
        return super.findByLabel(label);
    }

    @Override
    protected Specification<Program> toSpecification(ProgramFilterVO filter, ProgramFetchOptions fetchOptions) {
        return super.toSpecification(filter, fetchOptions)
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
    }

    @Override
    @Caching(
        evict = {
            @CacheEvict(cacheNames = CacheConfiguration.Names.PROGRAM_BY_ID, key = "#vo.id", condition = "#vo.id != null"),
            @CacheEvict(cacheNames = CacheConfiguration.Names.PROGRAM_BY_LABEL, key = "#vo.label", condition = "#vo.label != null"),
        },
        put = {
            @CachePut(cacheNames= CacheConfiguration.Names.PROGRAM_BY_ID, key="#vo.id", condition = " #vo.id != null"),
            @CachePut(cacheNames= CacheConfiguration.Names.PROGRAM_BY_LABEL, key="#vo.label", condition = "#vo.label != null")
        }
    )
    public ProgramVO save(ProgramVO vo) {
        Preconditions.checkNotNull(vo);
        Preconditions.checkNotNull(vo.getLabel(), "Missing 'label'");
        Preconditions.checkNotNull(vo.getName(), "Missing 'name'");
        Preconditions.checkNotNull(vo.getStatusId(), "Missing 'statusId'");

        if (vo.getId() == null && vo.getStatusId() == null)
            // Set default status to Temporary
            vo.setStatusId(StatusEnum.TEMPORARY.getId());

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
                target.setGearClassification(getReference(GearClassification.class, gearClassificationId));
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
                        }
                        else {
                            existingValues.remove(prop);
                        }
                        prop.setName(source.get(key));
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
            if (CollectionUtils.isNotEmpty(existingValues)) {
                parent.getProperties().removeAll(existingValues);
                existingValues.forEach(em::remove);
            }

        }
    }

    @Override
    @Caching(
        evict = {
            @CacheEvict(cacheNames = CacheConfiguration.Names.PROGRAM_BY_ID, key = "#id", condition = "#id != null"),
            @CacheEvict(cacheNames = CacheConfiguration.Names.PROGRAM_BY_LABEL, allEntries = true)
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
    public boolean hasUserPrivilege(int id, int personId, ProgramPrivilegeEnum privilege) {
        return getEntityManager().createNamedQuery("ProgramPerson.count", Long.class)
                .setParameter("programId", id)
                .setParameter("personId", personId)
                .setParameter("privilegeId", privilege.getId())
                .getSingleResult() > 0;
    }

    @Override
    public boolean hasDepartmentPrivilege(int id, int departmentId, ProgramPrivilegeEnum privilege) {
        return getEntityManager().createNamedQuery("ProgramDepartment.count", Long.class)
                .setParameter("programId", id)
                .setParameter("departmentId", departmentId)
                .setParameter("privilegeId", privilege.getId())
                .getSingleResult() > 0;
    }
}

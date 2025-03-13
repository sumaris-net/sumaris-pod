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
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.dao.administration.programStrategy.denormalized.DenormalizedPmfmStrategyRepository;
import net.sumaris.core.dao.administration.user.DepartmentRepository;
import net.sumaris.core.dao.referential.ReferentialDao;
import net.sumaris.core.dao.referential.ReferentialRepositoryImpl;
import net.sumaris.core.dao.referential.gear.GearRepository;
import net.sumaris.core.dao.referential.location.LocationRepository;
import net.sumaris.core.dao.referential.taxon.TaxonNameRepository;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.dao.technical.hibernate.AdditionalSQLFunctions;
import net.sumaris.core.dao.technical.jpa.BindableSpecification;
import net.sumaris.core.exception.NotUniqueException;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.model.IEntity;
import net.sumaris.core.model.administration.programStrategy.*;
import net.sumaris.core.model.administration.user.Department;
import net.sumaris.core.model.data.*;
import net.sumaris.core.model.referential.Status;
import net.sumaris.core.model.referential.StatusEnum;
import net.sumaris.core.model.referential.gear.Gear;
import net.sumaris.core.model.referential.location.Location;
import net.sumaris.core.model.referential.pmfm.PmfmEnum;
import net.sumaris.core.model.referential.taxon.ReferenceTaxon;
import net.sumaris.core.model.referential.taxon.TaxonGroup;
import net.sumaris.core.util.Beans;
import net.sumaris.core.util.Dates;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.administration.programStrategy.*;
import net.sumaris.core.vo.filter.LocationFilterVO;
import net.sumaris.core.vo.filter.PmfmStrategyFilterVO;
import net.sumaris.core.vo.filter.StrategyFilterVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import net.sumaris.core.vo.referential.gear.GearVO;
import net.sumaris.core.vo.referential.location.LocationVO;
import net.sumaris.core.vo.referential.taxon.TaxonGroupVO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.hibernate.jpa.QueryHints;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.domain.Specification;

import javax.annotation.Nullable;
import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author peck7 on 24/08/2020.
 */
@Slf4j
public class StrategyRepositoryImpl
    extends ReferentialRepositoryImpl<Integer, Strategy, StrategyVO, StrategyFilterVO, StrategyFetchOptions>
    implements StrategySpecifications {

    private final ReferentialDao referentialDao;

    private final PmfmStrategyRepository pmfmStrategyRepository;

    private final DenormalizedPmfmStrategyRepository denormalizedPmfmStrategyRepository;

    private final GearRepository gearRepository;

    private final TaxonNameRepository taxonNameRepository;

    private final LocationRepository locationRepository;

    protected final DepartmentRepository departmentRepository;

    protected final ProgramPrivilegeRepository programPrivilegeRepository;

    protected final TimeZone dbTimeZone;

    public StrategyRepositoryImpl(EntityManager entityManager, ReferentialDao referentialDao, PmfmStrategyRepository pmfmStrategyRepository,
                                  DenormalizedPmfmStrategyRepository denormalizedPmfmStrategyRepository, TaxonNameRepository taxonNameRepository, LocationRepository locationRepository, DepartmentRepository departmentRepository, ProgramPrivilegeRepository programPrivilegeRepository,
                                  GearRepository gearRepository,
                                  SumarisConfiguration configuration) {
        super(Strategy.class, StrategyVO.class, entityManager);
        this.referentialDao = referentialDao;
        this.pmfmStrategyRepository = pmfmStrategyRepository;
        this.denormalizedPmfmStrategyRepository = denormalizedPmfmStrategyRepository;
        this.taxonNameRepository = taxonNameRepository;
        this.locationRepository = locationRepository;
        this.departmentRepository = departmentRepository;
        this.gearRepository = gearRepository;
        this.programPrivilegeRepository = programPrivilegeRepository;
        this.dbTimeZone = configuration.getDbTimezone();
        setLockForUpdate(true);
    }

    @Override
    @Cacheable(cacheNames = CacheConfiguration.Names.STRATEGY_BY_ID, condition = "#result.present")
    public Optional<StrategyVO> findVOById(Integer id) {
        return super.findVOById(id);
    }

    @Override
    @Cacheable(cacheNames = CacheConfiguration.Names.STRATEGY_BY_LABEL, condition = "#result.present")
    public Optional<StrategyVO> findByLabel(String label) {
        return super.findByLabel(label);
    }

    @Override
    @Cacheable(cacheNames = CacheConfiguration.Names.STRATEGIES_BY_FILTER)
    public List<StrategyVO> findAll(StrategyFilterVO filter, Page page, StrategyFetchOptions fetchOptions) {
        return super.findAll(filter, page, fetchOptions);
    }

    @Override
    public List<StrategyVO> findAll(StrategyFilterVO filter, StrategyFetchOptions fetchOptions) {
        return this.findAll(filter, (Page) null, fetchOptions);
    }

    @Override
    @Caching(
        evict = {
            @CacheEvict(cacheNames = CacheConfiguration.Names.STRATEGY_BY_ID, key = "#source.id", condition = "#source.id != null"),
            @CacheEvict(cacheNames = CacheConfiguration.Names.STRATEGY_BY_LABEL, key = "#source.label", condition = "#source.label != null"),
            @CacheEvict(cacheNames = CacheConfiguration.Names.STRATEGIES_BY_FILTER, allEntries = true),
            @CacheEvict(cacheNames = CacheConfiguration.Names.PROGRAM_IDS_BY_READ_USER_ID, allEntries = true),
            @CacheEvict(cacheNames = CacheConfiguration.Names.PROGRAM_IDS_BY_WRITE_USER_ID, allEntries = true),
            @CacheEvict(cacheNames = CacheConfiguration.Names.PROGRAM_LOCATION_IDS_BY_USER_ID, allEntries = true),
            @CacheEvict(cacheNames = CacheConfiguration.Names.PROGRAM_ACQUISITION_LEVELS_BY_ID, key = "#source.programId", condition = "#source.programId != null")
        }
    )
    public StrategyVO save(StrategyVO source) {
        Preconditions.checkNotNull(source);
        Preconditions.checkNotNull(source.getProgramId(), "Missing 'programId'");
        Preconditions.checkNotNull(source.getName(), "Missing 'name'");
        Preconditions.checkNotNull(source.getStatusId(), "Missing 'statusId'");

        if (source.getId() == null && source.getStatusId() == null)
            // Set default status to Temporary
            source.setStatusId(StatusEnum.TEMPORARY.getId());

        return super.save(source);
    }

    @Override
    @Caching(
        evict = {
            @CacheEvict(cacheNames = CacheConfiguration.Names.STRATEGY_BY_ID, allEntries = true),
            @CacheEvict(cacheNames = CacheConfiguration.Names.STRATEGY_BY_LABEL, allEntries = true),
            @CacheEvict(cacheNames = CacheConfiguration.Names.STRATEGIES_BY_FILTER, allEntries = true),
            @CacheEvict(cacheNames = CacheConfiguration.Names.PMFM_STRATEGIES_BY_FILTER, allEntries = true),
            @CacheEvict(cacheNames = CacheConfiguration.Names.DENORMALIZED_PMFM_BY_FILTER, allEntries = true),
            @CacheEvict(cacheNames = CacheConfiguration.Names.PROGRAM_IDS_BY_READ_USER_ID, allEntries = true),
            @CacheEvict(cacheNames = CacheConfiguration.Names.PROGRAM_IDS_BY_WRITE_USER_ID, allEntries = true),
            @CacheEvict(cacheNames = CacheConfiguration.Names.PROGRAM_LOCATION_IDS_BY_USER_ID, allEntries = true),
            @CacheEvict(cacheNames = CacheConfiguration.Names.PROGRAM_ACQUISITION_LEVELS_BY_ID, key = "#programId", condition = "#programId != null")
        }
    )
    public List<StrategyVO> saveByProgramId(int programId, List<StrategyVO> sources) {
        Preconditions.checkNotNull(sources);

        // Load parent entity
        Program parent = getById(Program.class, programId);

        // Remember existing entities
        final List<Integer> sourcesIdsToRemove = Beans.collectIds(Beans.getList(parent.getStrategies()));

        // Save each entity
        List<StrategyVO> result = sources.stream().map(source -> {
            source.setProgramId(programId);
            if (source.getId() != null) {
                sourcesIdsToRemove.remove(source.getId());
            }
            return save(source);
        }).collect(Collectors.toList());

        // Remove unused entities
        if (CollectionUtils.isNotEmpty(sourcesIdsToRemove)) {
            // Warn: avoid to call this.deleteById() to avoid unnecessary cache evictions
            // => super.deleteById() is better in this case
            sourcesIdsToRemove.forEach(super::deleteById);
        }

        return result;
    }

    @Override
    @Caching(
        evict = {
            @CacheEvict(cacheNames = CacheConfiguration.Names.STRATEGY_BY_ID, key = "#id", condition = "#id != null"),
            @CacheEvict(cacheNames = CacheConfiguration.Names.STRATEGY_BY_LABEL, allEntries = true),
            @CacheEvict(cacheNames = CacheConfiguration.Names.STRATEGIES_BY_FILTER, allEntries = true),
            @CacheEvict(cacheNames = CacheConfiguration.Names.PMFM_STRATEGIES_BY_FILTER, allEntries = true),
            @CacheEvict(cacheNames = CacheConfiguration.Names.DENORMALIZED_PMFM_BY_FILTER, allEntries = true),
            @CacheEvict(cacheNames = CacheConfiguration.Names.PROGRAM_IDS_BY_READ_USER_ID, allEntries = true),
            @CacheEvict(cacheNames = CacheConfiguration.Names.PROGRAM_ACQUISITION_LEVELS_BY_ID, allEntries = true)
        }
    )
    public void deleteById(Integer id) {
        super.deleteById(id);
    }

    @Override
    public List<ReferentialVO> getGears(int strategyId) {
        CriteriaBuilder builder = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<Gear> query = builder.createQuery(Gear.class);
        Root<Gear> root = query.from(Gear.class);

        ParameterExpression<Integer> strategyIdParam = builder.parameter(Integer.class);

        Join<Gear, Strategy> gearInnerJoin = root.joinList(Gear.Fields.STRATEGIES, JoinType.INNER);

        query.select(root)
            .where(
                builder.and(
                    // strategy
                    builder.equal(gearInnerJoin.get(Strategy.Fields.ID), strategyIdParam),
                    // Status (temporary or valid)
                    builder.in(root.get(Gear.Fields.STATUS).get(Status.Fields.ID)).value(ImmutableList.of(StatusEnum.ENABLE.getId(), StatusEnum.TEMPORARY.getId()))
                ));

        // Sort by label
        query.orderBy(builder.asc(root.get(Gear.Fields.LABEL)));

        try (Stream<Gear> stream = getEntityManager()
            .createQuery(query)
            .setParameter(strategyIdParam, strategyId)
            .getResultStream()) {
            return stream.map(referentialDao::toVO).toList();
        }
    }

    @Override
    public List<TaxonGroupStrategyVO> getTaxonGroupStrategies(int strategyId) {
        return getTaxonGroupStrategies(getReference(Strategy.class, strategyId));
    }

    @Override
    public List<TaxonNameStrategyVO> getTaxonNameStrategies(int strategyId) {
        return getTaxonNameStrategies(getReference(Strategy.class, strategyId));
    }

    @Override
    public List<AppliedStrategyVO> getAppliedStrategies(int strategyId) {
        return getAppliedStrategies(getReference(Strategy.class, strategyId));
    }

    @Override
    public List<StrategyDepartmentVO> getDepartmentsById(int strategyId) {
        return getDepartments(getReference(Strategy.class, strategyId));
    }

    /**
     * @param programId   program id
     * @param labelPrefix label prefix (ex: 20-LEUCCIR-)
     * @return next strategy label for this prefix (ex: 20LEUCCIR001)
     */
    @Override
    public String computeNextLabelByProgramId(int programId, String labelPrefix, int nbDigit) {
        final String prefix = (labelPrefix == null) ? "" : labelPrefix;

        CriteriaBuilder builder = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<String> query = builder.createQuery(String.class);
        Root<Strategy> root = query.from(Strategy.class);

        ParameterExpression<Integer> programIdParam = builder.parameter(Integer.class);

        query.select(root.get(Strategy.Fields.LABEL))
            .where(
                builder.and(
                    // Program
                    builder.equal(root.get(Strategy.Fields.PROGRAM).get(Program.Fields.ID), programIdParam),
                    // Label
                    builder.like(root.get(Strategy.Fields.LABEL), prefix.concat("%"))
                ));

        String result = getEntityManager()
            .createQuery(query)
            .setParameter(programIdParam, programId)
            .getResultStream()
            .max(String::compareTo)
            .map(source -> StringUtils.removeStart(source, prefix))
            .orElse("0");

        if (!StringUtils.isNumeric(result)) {
            throw new SumarisTechnicalException(String.format("Unable to increment label '%s' on strategy", prefix.concat(result)));
        }
        result = String.valueOf(Integer.parseInt(result) + 1);
        result = prefix.concat(StringUtils.leftPad(result, nbDigit, '0'));
        return result;
    }

    /**
     * @param strategyLabel strategy label (ex: 20LEUCCIR001)
     * @param separator     label separator (ex: -)
     * @return next strategy sample label for this strategy (ex: 20LEUCCIR001-0001)
     */
    @Override
    public String computeNextSampleLabelByStrategy(@NonNull String strategyLabel, String separator, int nbDigit) {
        final String prefix = strategyLabel.concat(StringUtils.nullToEmpty(separator));

        EntityManager em = getEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<String> query = cb.createQuery(String.class);
        Root<Sample> root = query.from(Sample.class);

        ParameterExpression<Integer> tagIdPmfmIdParam = cb.parameter(Integer.class);
        ParameterExpression<String> tagLikeParam = cb.parameter(String.class);
        ParameterExpression<Integer> strategyPmfmIdParam = cb.parameter(Integer.class);
        ParameterExpression<String> strategyLabelParam = cb.parameter(String.class);
        ParameterExpression<Integer> lpadSizeParam = cb.parameter(Integer.class);
        ParameterExpression<String> lpadFillParam = cb.parameter(String.class);

        Join<Sample, Operation> operationJoin = root.join(Sample.Fields.OPERATION, JoinType.INNER);
        Join<Operation, Trip> tripJoin = operationJoin.join(Operation.Fields.TRIP, JoinType.INNER);
        Join<Trip, Landing> landingInnerJoin = tripJoin.joinList(Trip.Fields.LANDINGS, JoinType.INNER);
        Join<Landing, LandingMeasurement> landingMeasurementJoin = landingInnerJoin.joinList(Landing.Fields.LANDING_MEASUREMENTS, JoinType.INNER);
        Join<Sample, SampleMeasurement> sampleMeasurementJoin = root.joinList(Sample.Fields.MEASUREMENTS, JoinType.INNER);

        Expression<String> lpadValue = cb.function(AdditionalSQLFunctions.lpad.name(), String.class,
            cb.substring(
                sampleMeasurementJoin.get(SampleMeasurement.Fields.ALPHANUMERICAL_VALUE),
                prefix.length() + 1
            ),
            lpadSizeParam,
            lpadFillParam
        );

        query.select(lpadValue)
            .where(
                cb.and(
                    // Sample measurement: select Pmfm = Tag id
                    cb.equal(sampleMeasurementJoin.get(SampleMeasurement.Fields.PMFM).get(IEntity.Fields.ID), tagIdPmfmIdParam),
                    cb.like(sampleMeasurementJoin.get(SampleMeasurement.Fields.ALPHANUMERICAL_VALUE), tagLikeParam),
                    // Sample measurement: select Pmfm = Strategy label
                    cb.equal(landingMeasurementJoin.get(LandingMeasurement.Fields.PMFM).get(IEntity.Fields.ID), strategyPmfmIdParam),
                    cb.equal(landingMeasurementJoin.get(LandingMeasurement.Fields.ALPHANUMERICAL_VALUE), strategyLabelParam)
                ))
            .orderBy(cb.desc(lpadValue));

        List<String> results = em
            .createQuery(query)
            .setParameter(tagIdPmfmIdParam, PmfmEnum.TAG_ID.getId())
            .setParameter(tagLikeParam, prefix + "%")
            .setParameter(lpadSizeParam, nbDigit)
            .setParameter(lpadFillParam, "0")
            .setParameter(strategyPmfmIdParam, PmfmEnum.STRATEGY_LABEL.getId())
            .setParameter(strategyLabelParam, strategyLabel)
            .setMaxResults(10).getResultList();

        String result = results.stream()
            .filter(StringUtils::isNumeric)
            .findFirst()
            .orElse("0");

        if (!StringUtils.isNumeric(result)) {
            throw new SumarisTechnicalException(String.format("Unable to increment label '%s' on sample", prefix.concat(result)));
        }
        result = String.valueOf(Integer.parseInt(result) + 1);
        result = prefix.concat(StringUtils.leftPad(result, nbDigit, '0'));
        return result;
    }

    @Override
    public List<StrategyVO> findNewerByProgramId(final int programId, final Date updateDate, final StrategyFetchOptions fetchOptions) {
        try (Stream<Strategy> stream = streamAll(
            BindableSpecification.where(hasProgramIds(programId))
                .and(newerThan(updateDate)))) {
            return stream.map(entity -> toVO(entity, fetchOptions)).toList();
        }
    }

    @Override
    public void saveProgramLocationsByStrategyId(int strategyId) {
        EntityManager em = getEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();

        Strategy strategy = getById(Strategy.class, strategyId);

        // Get existing program locations
        Map<Integer, Program2Location> programLocations = new HashMap<>();
        {
            CriteriaQuery<Program2Location> query = cb.createQuery(Program2Location.class);
            Root<Program2Location> root = query.from(Program2Location.class);
            query.where(cb.equal(root.get(Program2Location.Fields.PROGRAM), strategy.getProgram()));
            em.createQuery(query).getResultStream().forEach(p2l ->
                programLocations.putIfAbsent(p2l.getLocation().getId(), p2l));
        }

        // Get existing strategy locations
        Map<Integer, Location> strategyLocations = new HashMap<>();
        {
            CriteriaQuery<Location> query = cb.createQuery(Location.class);
            Root<Strategy> root = query.from(Strategy.class);
            Join<Strategy, AppliedStrategy> appliedStrategyInnerJoin = root.joinList(Strategy.Fields.APPLIED_STRATEGIES, JoinType.INNER);
            query.select(appliedStrategyInnerJoin.get(AppliedStrategy.Fields.LOCATION))
                .where(cb.equal(root.get(Strategy.Fields.PROGRAM), strategy.getProgram()));
            em.createQuery(query).getResultStream().forEach(l ->
                strategyLocations.putIfAbsent(l.getId(), l));
        }

        // Persist new entities
        strategyLocations.values()
            .stream()
            .filter(location -> !programLocations.containsKey(location.getId()))
            .forEach(location -> {
                Program2Location p2l = new Program2Location();
                p2l.setProgram(strategy.getProgram());
                p2l.setLocation(location);
                em.persist(p2l);
            });

        // Remove unused entities
        programLocations.values()
            .stream()
            .filter(p2l -> !strategyLocations.containsKey(p2l.getLocation().getId()))
            .forEach(em::remove);
    }

    @Override
    public void toEntity(StrategyVO source, Strategy target, boolean copyIfNull) {
        super.toEntity(source, target, copyIfNull);

        // Program
        if (copyIfNull || source.getProgramId() != null) {
            if (source.getProgramId() == null) {
                target.setProgram(null);
            } else {
                target.setProgram(getReference(Program.class, source.getProgramId()));
            }
        }

        // Gears
        List<Integer> gearIds = CollectionUtils.isNotEmpty(source.getGearIds()) ?
            source.getGearIds() :
            (CollectionUtils.isNotEmpty(source.getGears()) ?
                Beans.collectIds(source.getGears()) :
                null);
        if (copyIfNull || CollectionUtils.isNotEmpty(gearIds)) {
            target.getGears().clear();
            if (CollectionUtils.isNotEmpty(gearIds)) {
                target.getGears().addAll(loadAllAsSet(Gear.class, gearIds, true));
            }
        }
    }

    @Override
    public List<TaxonGroupStrategyVO> saveTaxonGroupStrategiesByStrategyId(int strategyId, List<TaxonGroupStrategyVO> sources) {
        Preconditions.checkNotNull(sources);

        Strategy parent = getById(Strategy.class, strategyId);

        sources.forEach(source -> source.setStrategyId(strategyId));

        EntityManager em = getEntityManager();

        // Remember existing entities
        Map<Integer, TaxonGroupStrategy> sourcesToRemove = Beans.splitByProperty(parent.getTaxonGroups(),
            TaxonGroupStrategy.Fields.TAXON_GROUP + "." + TaxonGroup.Fields.ID);

        // Save each taxon group strategy
        Beans.getList(sources).forEach(source -> {
            Integer taxonGroupId = source.getTaxonGroup() != null ? source.getTaxonGroup().getId() : null;
            if (taxonGroupId == null)
                throw new DataIntegrityViolationException("Missing taxonGroup.id in a TaxonGroupStrategyVO");
            TaxonGroupStrategy target = sourcesToRemove.remove(taxonGroupId);
            boolean isNew = target == null;
            if (isNew) {
                target = new TaxonGroupStrategy();
                target.setTaxonGroup(getReference(TaxonGroup.class, taxonGroupId));
                target.setStrategy(parent);
            }
            target.setPriorityLevel(source.getPriorityLevel());
            if (isNew) {
                em.persist(target);
            } else {
                em.merge(target);
            }
        });

        // Remove unused entities
        if (MapUtils.isNotEmpty(sourcesToRemove)) {
            sourcesToRemove.values().forEach(em::remove);
        }

        return sources.isEmpty() ? null : sources;
    }

    @Override
    public List<TaxonNameStrategyVO> saveReferenceTaxonStrategiesByStrategyId(int strategyId, List<TaxonNameStrategyVO> sources) {
        Preconditions.checkNotNull(sources);

        Strategy parent = getById(Strategy.class, strategyId);

        sources.forEach(source -> source.setStrategyId(strategyId));

        EntityManager em = getEntityManager();

        // Remember existing entities
        Map<Integer, ReferenceTaxonStrategy> sourcesToRemove = Beans.splitByProperty(parent.getReferenceTaxons(),
            ReferenceTaxonStrategy.Fields.REFERENCE_TAXON + "." + ReferenceTaxon.Fields.ID);

        // Save each reference taxon strategy
        Beans.getStream(sources).forEach(source -> {
            Integer referenceTaxonId = source.getReferenceTaxonId() != null ? source.getReferenceTaxonId() :
                (source.getTaxonName() != null ? source.getTaxonName().getReferenceTaxonId() : null);

            if (referenceTaxonId == null) {
                referenceTaxonId = taxonNameRepository.getReferenceTaxonIdById(source.getTaxonName().getId());
            }
            if (referenceTaxonId == null)
                throw new DataIntegrityViolationException("Missing referenceTaxon.id in a ReferenceTaxonStrategyVO");
            ReferenceTaxonStrategy target = sourcesToRemove.remove(referenceTaxonId);
            boolean isNew = target == null;
            if (isNew) {
                target = new ReferenceTaxonStrategy();
                target.setReferenceTaxon(getReference(ReferenceTaxon.class, referenceTaxonId));
                target.setStrategy(parent);
            }
            target.setPriorityLevel(source.getPriorityLevel());
            if (isNew) {
                em.persist(target);
            } else {
                em.merge(target);
            }
        });

        // Remove unused entities
        if (MapUtils.isNotEmpty(sourcesToRemove)) {
            sourcesToRemove.values().forEach(em::remove);
        }

        return sources.isEmpty() ? null : sources;
    }

    @Override
    public List<AppliedStrategyVO> saveAppliedStrategiesByStrategyId(int strategyId, List<AppliedStrategyVO> sources) {
        Preconditions.checkNotNull(sources);

        final Strategy parent = getById(Strategy.class, strategyId);

        saveChildren(
            sources,
            parent.getAppliedStrategies(),
            AppliedStrategy.class,
            (source, target, copyIfNull) -> {
                source.setStrategyId(parent.getId());
                target.setStrategy(parent);

                // Location
                Integer locationId = source.getLocation() != null ? source.getLocation().getId() : null;
                if (copyIfNull || locationId != null) {
                    target.setLocation(locationId != null ? getReference(Location.class, locationId) : null);
                }
            },
            parent);

        // Save applied periods
        sources.forEach(source -> saveAppliedPeriodsByAppliedStrategyId(source.getId(), source.getAppliedPeriods()));

        return sources;
    }

    @Override
    public List<StrategyDepartmentVO> saveDepartmentsByStrategyId(int strategyId, List<StrategyDepartmentVO> sources) {
        Preconditions.checkNotNull(sources);

        final Strategy parent = getById(Strategy.class, strategyId);

        return saveChildren(
            sources,
            parent.getDepartments(),
            StrategyDepartment.class,
            (source, target, copyIfNull) -> this.toDepartmentEntity(source, target, parent, copyIfNull),
            parent);
    }


    @Override
    public boolean hasUserPrivilege(int strategyId, int personId, ProgramPrivilegeEnum privilege) {
        log.warn("TODO: implement StrategyService.hasUserPrivilege()");

        return false;
    }

    @Override
    public boolean hasDepartmentPrivilege(int strategyId, int departmentId, ProgramPrivilegeEnum privilege) {
        return getEntityManager().createNamedQuery("StrategyDepartment.count", Long.class)
            .setParameter("strategyId", strategyId)
            .setParameter("departmentId", departmentId)
            .setParameter("privilegeId", privilege.getId())
            .getSingleResult() > 0;
    }

    @Override
    public Date maxUpdateDateByFilter(StrategyFilterVO filter) {
        CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<Date> criteriaQuery = cb.createQuery(Date.class);
        Root<Strategy> root = criteriaQuery.from(Strategy.class);

        // Select max(update date)
        criteriaQuery.select(cb.greatest(root.get(Strategy.Fields.UPDATE_DATE).as(Date.class)));

        Specification<Strategy> spec = toSpecification(filter);
        if (spec != null) {
            criteriaQuery.where(spec.toPredicate(root, criteriaQuery, cb));
        }

        TypedQuery<Date> query = getEntityManager().createQuery(criteriaQuery);

        // Bind parameters
        applyBindings(query, spec);

        return query.getSingleResult();
    }

    /* -- protected methods -- **/


    @Override
    protected void onBeforeSaveEntity(StrategyVO source, Strategy target, boolean isNew) {
        super.onBeforeSaveEntity(source, target, isNew);

        // Verify label is unique by program (if was set - in Adagio DB, the label can be null)
        if (StringUtils.isNotBlank(source.getLabel())) {
            long count = this.count(StrategyFilterVO.builder()
                .label(source.getLabel())
                .excludedIds(isNew ? null : new Integer[]{source.getId()})
                .programIds(new Integer[]{source.getProgramId()})
                .build());
            if (count > 0) {
                // Fix the label, by adding a suffix
                if (isNew) {
                    source.setLabel(source.getLabel() + "_2");
                } else {
                    throw new NotUniqueException("Strategy label already exists in program", List.of(source.getLabel()));
                }
            }
        }
    }

    @Override
    protected void onAfterSaveEntity(StrategyVO vo, Strategy savedEntity, boolean isNew) {
        super.onAfterSaveEntity(vo, savedEntity, isNew);

        // Save properties
        saveProperties(vo.getProperties(), savedEntity, savedEntity.getUpdateDate());

        EntityManager em = getEntityManager();
        em.flush();
        em.clear();
    }

    @Override
    protected Specification<Strategy> toSpecification(@NonNull StrategyFilterVO filter, StrategyFetchOptions fetchOptions) {
        Specification<Strategy> specification = super.toSpecification(filter, fetchOptions);
        if (filter.getId() != null) return specification;
        return specification
            .and(betweenDate(filter.getStartDate(), filter.getEndDate()))
            .and(hasAnalyticReferences(filter.getAnalyticReferences()))
            .and(hasReferenceTaxonIds(filter.getReferenceTaxonIds()))
            .and(hasDepartmentIds(filter.getDepartmentIds()))
            .and(hasLocationIds(filter.getLocationIds()))
            .and(hasParameterIds(filter.getParameterIds()))
            .and(hasPeriods(filter.getPeriods()))
            .and(hasAcquisitionLevelLabels(filter.getAcquisitionLevels()))
            .and(newerThan(filter.getMinUpdateDate()))
            ;
    }

    @Override
    protected void toVO(Strategy source, StrategyVO target, StrategyFetchOptions fetchOptions, boolean copyIfNull) {
        fetchOptions = StrategyFetchOptions.nullToDefault(fetchOptions);

        super.toVO(source, target, fetchOptions, copyIfNull);

        // Program
        target.setProgramId(source.getProgram().getId());

        // Gears
        if (fetchOptions.isWithGears() && CollectionUtils.isNotEmpty(source.getGears())) {
            // Set Gears
            List<ReferentialVO> refGears = source.getGears()
                    .stream()
                    .map(referentialDao::toVO)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            target.setGears(refGears);

            // Set FulGears
            List<GearVO> gears = source.getGears()
                .stream()
                .map(gearRepository::toVO)
                .filter(Objects::nonNull)
                .toList();
            target.setFullGears(gears);


        }

        // Taxon groups
        if (fetchOptions.isWithTaxonGroups()) {
            target.setTaxonGroups(getTaxonGroupStrategies(source));
        }

        // Taxon names
        if (fetchOptions.isWithTaxonNames()) {
            target.setTaxonNames(getTaxonNameStrategies(source));
        }

        // Applied Strategies
        if (fetchOptions.isWithAppliedStrategies()) {
            target.setAppliedStrategies(getAppliedStrategies(source));
        }

        // Strategy departments
        if (fetchOptions.isWithDepartments()) {
            target.setDepartments(getDepartments(source));
        }

        // Pmfms
        if (fetchOptions.isWithPmfms()) {
            target.setPmfms(getPmfms(source, fetchOptions.getPmfmsFetchOptions()));
        }

        // Denormalized pmfms
        if (fetchOptions.isWithDenormalizedPmfms()) {
            target.setDenormalizedPmfms(getDenormalizedPmfms(source, fetchOptions.getPmfmsFetchOptions()));
        }

        // Properties
        if (fetchOptions.isWithProperties()) {
            Map<String, String> properties = Maps.newHashMap();
            Beans.getStream(source.getProperties())
                .filter(prop -> Objects.nonNull(prop)
                    && Objects.nonNull(prop.getLabel())
                    && Objects.nonNull(prop.getName())
                )
                .forEach(prop -> {
                    if (properties.containsKey(prop.getLabel())) {
                        log.warn(String.format("Duplicate strategy property with label {%s}. Overriding existing value with {%s}", prop.getLabel(), prop.getName()));
                    }
                    properties.put(prop.getLabel(), prop.getName());
                });
            target.setProperties(properties);
        }
    }

    protected List<TaxonNameStrategyVO> getTaxonNameStrategies(Strategy source) {
        if (CollectionUtils.isEmpty(source.getReferenceTaxons())) return null;

        return source.getReferenceTaxons()
            .stream()
            // Sort by priority level (or if not set, by id)
            .sorted(Comparator.comparingInt(item -> item.getPriorityLevel() != null ?
                item.getPriorityLevel() :
                item.getReferenceTaxon().getId()))
            .map(item -> {
                TaxonNameStrategyVO target = new TaxonNameStrategyVO();
                target.setStrategyId(source.getId());

                // Priority level
                target.setPriorityLevel(item.getPriorityLevel());

                // Taxon name
                taxonNameRepository.findReferentByReferenceTaxonId(item.getReferenceTaxon().getId())
                    .ifPresent(taxonName -> {
                        target.setTaxonName(taxonName);
                        target.setReferenceTaxonId(taxonName.getReferenceTaxonId());
                        target.setIsReferent(taxonName.getIsReferent());
                    });
                return target;
            })
            .filter(target -> target.getTaxonName() != null)
            .collect(Collectors.toList());
    }

    protected List<TaxonGroupStrategyVO> getTaxonGroupStrategies(Strategy source) {
        if (CollectionUtils.isEmpty(source.getTaxonGroups())) return null;
        return source.getTaxonGroups()
            .stream()
            // Sort by priority level (or if not set, by id)
            .sorted(Comparator.comparingInt((item) -> item.getPriorityLevel() != null ?
                item.getPriorityLevel() :
                item.getTaxonGroup().getId()))
            .map(item -> {
                TaxonGroupStrategyVO target = new TaxonGroupStrategyVO();
                target.setStrategyId(source.getId());

                // Priority level
                target.setPriorityLevel(item.getPriorityLevel());

                // Taxon group
                TaxonGroupVO tg = new TaxonGroupVO();
                Beans.copyProperties(item.getTaxonGroup(), tg);
                tg.setStatusId(item.getTaxonGroup().getStatus().getId());
                target.setTaxonGroup(tg);

                return target;
            })
            .collect(Collectors.toList());
    }

    protected List<AppliedStrategyVO> getAppliedStrategies(final Strategy source) {
        if (CollectionUtils.isEmpty(source.getAppliedStrategies())) return null;

        Integer[] locationIds = source.getAppliedStrategies()
            .stream().map(AppliedStrategy::getLocation)
            .filter(Objects::nonNull)
            .map(Location::getId)
            .filter(Objects::nonNull)
            .distinct()
            .toArray(Integer[]::new);
        Map<Integer, LocationVO> locationsById = Beans.splitById(
            locationRepository.findAll(LocationFilterVO.builder().includedIds(locationIds).build()));

        return Beans.getStream(source.getAppliedStrategies())
            // Sort by id (this is need for IMAGINE, as the first AppliedStrategy holds AppliedPeriod)
            .sorted(Comparator.comparingInt(AppliedStrategy::getId))
            .map(item -> {
                final AppliedStrategyVO target = new AppliedStrategyVO();
                target.setId(item.getId());
                target.setStrategyId(source.getId());

                if (item.getLocation() != null) {
                    target.setLocation(locationsById.get(item.getLocation().getId()));
                }

                // AppliedPeriod
                if (CollectionUtils.isNotEmpty(item.getAppliedPeriods())) {
                    List<AppliedPeriodVO> targetAppliedPeriods = Beans.getStream(item.getAppliedPeriods())
                        .map(sourceAppliedPeriod -> {
                            AppliedPeriodVO targetAppliedPeriod = new AppliedPeriodVO();
                            Beans.copyProperties(sourceAppliedPeriod, targetAppliedPeriod);

                            // Convert date to have a timezone (see issue sumaris-app#500)
                            targetAppliedPeriod.setStartDate(Dates.resetTime(targetAppliedPeriod.getStartDate(), this.dbTimeZone));
                            targetAppliedPeriod.setEndDate(Dates.resetTime(targetAppliedPeriod.getEndDate(), this.dbTimeZone));

                            targetAppliedPeriod.setAppliedStrategyId(target.getId());
                            return targetAppliedPeriod;
                        }).collect(Collectors.toList());
                    target.setAppliedPeriods(targetAppliedPeriods);
                }

                return target;
            })
            .collect(Collectors.toList());
    }


    protected List<AppliedPeriodVO> saveAppliedPeriodsByAppliedStrategyId(int appliedStrategyId, @NonNull List<AppliedPeriodVO> sources) {

        EntityManager em = getEntityManager();

        // Load parent entity
        AppliedStrategy parent = getById(AppliedStrategy.class, appliedStrategyId);

        sources.forEach(source -> source.setAppliedStrategyId(appliedStrategyId));

        // Remove existing entities
        Map<Date, AppliedPeriod> sourcesToRemove = Beans.splitByProperty(parent.getAppliedPeriods(), AppliedPeriod.Fields.START_DATE);
        parent.getAppliedPeriods().clear();

        // Save each applied period
        List<AppliedPeriod> targets = sources.stream().map(source -> {
            if (source.getStartDate() == null)
                throw new DataIntegrityViolationException("Missing startDate in a AppliedPeriodVO");

            AppliedPeriod target = sourcesToRemove.remove(source.getStartDate());
            boolean isNew = target == null;
            if (isNew) {
                target = new AppliedPeriod();
                target.setAppliedStrategy(parent);
                target.setStartDate(source.getStartDate());
            } else {
                log.warn("Already existing AppliedPeriod on date: {}. Will update it", source.getStartDate().toString());
            }
            target.setEndDate(source.getEndDate());
            target.setAcquisitionNumber(source.getAcquisitionNumber());

            if (isNew) {
                em.persist(target);
            } else {
                em.merge(target);
            }
            return target;
        }).toList();

        parent.setAppliedPeriods(targets);

        if (MapUtils.isNotEmpty(sourcesToRemove)) {
            sourcesToRemove.values().forEach(em::remove);
        }

        return sources.isEmpty() ? null : sources;
    }

    protected List<StrategyDepartmentVO> getDepartments(Strategy source) {
        if (CollectionUtils.isEmpty(source.getDepartments())) return null;
        return source.getDepartments()
            .stream()
            // Sort by id
            .sorted(Comparator.comparingInt(StrategyDepartment::getId))
            .map(item -> {
                StrategyDepartmentVO target = new StrategyDepartmentVO();
                target.setId(item.getId());
                target.setUpdateDate(item.getUpdateDate());
                target.setStrategyId(source.getId());

                if (item.getLocation() != null) {
                    target.setLocation(locationRepository.get(item.getLocation().getId()));
                }

                target.setDepartment(departmentRepository.get(item.getDepartment().getId()));
                target.setPrivilege(programPrivilegeRepository.get(item.getPrivilege().getId()));

                return target;
            })
            .collect(Collectors.toList());
    }

    protected List<PmfmStrategyVO> getPmfms(Strategy source, PmfmStrategyFetchOptions fetchOptions) {
        Preconditions.checkNotNull(fetchOptions);
        if (CollectionUtils.isEmpty(source.getPmfms())) return null;

        return pmfmStrategyRepository.findByFilter(PmfmStrategyFilterVO.builder()
                .strategyId(source.getId())
                .build(), fetchOptions)
            .stream()
            .filter(Objects::nonNull)
            // Sort by acquisitionLevel and rankOrder
            .sorted(Comparator.comparing(ps -> String.format("%s#%s", ps.getAcquisitionLevel(), ps.getRankOrder())))
            .collect(Collectors.toList());
    }

    /**
     * Get denormalized PmfmStrategy (e.g. compute each pmfms from the parameter, method)
     *
     * @param source
     * @param fetchOptions
     * @return
     */
    protected List<DenormalizedPmfmStrategyVO> getDenormalizedPmfms(Strategy source, PmfmStrategyFetchOptions fetchOptions) {
        Preconditions.checkNotNull(fetchOptions);
        if (CollectionUtils.isEmpty(source.getPmfms())) return null;

        // Applied inheritance:
        List<PmfmStrategy> pmfms = source.getPmfms();
        List<DenormalizedPmfmStrategyVO> result = denormalizedPmfmStrategyRepository.findByFilter(PmfmStrategyFilterVO.builder()
                .strategyId(source.getId())
                .build(), fetchOptions)
            .stream()
            .filter(Objects::nonNull)
            // Sort by acquisitionLevel and rankOrder
            .sorted(Comparator.comparing(ps -> String.format("%s#%s", ps.getAcquisitionLevel(), ps.getRankOrder())))
            .collect(Collectors.toList());
        return result;
    }

    protected void toDepartmentEntity(@NonNull StrategyDepartmentVO source,
                                      @NonNull StrategyDepartment target,
                                      @NonNull Strategy parent, boolean copyIfNull) {
        Preconditions.checkNotNull(parent.getId());

        Beans.copyProperties(source, target);

        source.setStrategyId(parent.getId());
        target.setStrategy(parent);

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

    protected void saveProperties(Map<String, String> source, Strategy parent, Date updateDate) {
        final EntityManager em = getEntityManager();
        if (MapUtils.isEmpty(source)) {
            if (parent.getProperties() != null) {
                List<StrategyProperty> toRemove = ImmutableList.copyOf(parent.getProperties());
                parent.getProperties().clear();
                toRemove.forEach(em::remove);
            }
        } else {
            // WARN: database can stored many values for the same keys.
            // Only the first existing instance will be reused. Duplicate properties will be removed
            ListMultimap<String, StrategyProperty> existingPropertiesMap = Beans.splitByNotUniqueProperty(
                    Beans.getList(parent.getProperties()),
                    StrategyProperty.Fields.LABEL);
            List<StrategyProperty> existingValues = Beans.getList(existingPropertiesMap.values());
            final Status enableStatus = em.getReference(Status.class, StatusEnum.ENABLE.getId());
            if (parent.getProperties() == null) {
                parent.setProperties(Lists.newArrayList());
            }
            final List<StrategyProperty> targetProperties = parent.getProperties();
            targetProperties.clear();

            // Transform each entry into StrategyProperty
            source.keySet().stream()
                    .map(key -> {
                        StrategyProperty prop = existingPropertiesMap.containsKey(key) ? existingPropertiesMap.get(key).get(0) : null;
                        boolean isNew = (prop == null);
                        if (isNew) {
                            prop = new StrategyProperty();
                            prop.setLabel(key);
                            prop.setStrategy(parent);
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
                            prop = em.merge(prop);
                        }
                        return prop;
                    })
                    .forEach(targetProperties::add);

            // Remove old properties
            if (CollectionUtils.isNotEmpty(existingValues)) {
                existingValues.forEach(em::remove);
            }

        }
    }

    protected void configureQuery(TypedQuery<Strategy> query, @Nullable net.sumaris.core.dao.technical.Page page, @Nullable StrategyFetchOptions fetchOptions) {
        super.configureQuery(query, page, fetchOptions);

        if (page == null) {
            // Prepare load graph
            EntityManager em = getEntityManager();
            if (fetchOptions != null && (fetchOptions.isWithPmfms() || fetchOptions.isWithDenormalizedPmfms())) {
                EntityGraph<?> entityGraph = em.getEntityGraph(Strategy.GRAPH_PMFMS);
                query.setHint(QueryHints.HINT_LOADGRAPH, entityGraph);
            }
        }
    }

}

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
import com.google.common.collect.Lists;
import net.sumaris.core.dao.administration.user.DepartmentRepository;
import net.sumaris.core.dao.cache.CacheNames;
import net.sumaris.core.dao.referential.ReferentialDao;
import net.sumaris.core.dao.referential.ReferentialRepositoryImpl;
import net.sumaris.core.dao.referential.location.LocationRepository;
import net.sumaris.core.dao.referential.pmfm.PmfmRepository;
import net.sumaris.core.dao.referential.taxon.TaxonNameRepository;
import net.sumaris.core.model.administration.programStrategy.*;
import net.sumaris.core.model.administration.user.Department;
import net.sumaris.core.model.referential.Status;
import net.sumaris.core.model.referential.StatusEnum;
import net.sumaris.core.model.referential.gear.Gear;
import net.sumaris.core.model.referential.location.Location;
import net.sumaris.core.model.referential.taxon.ReferenceTaxon;
import net.sumaris.core.model.referential.taxon.TaxonGroup;
import net.sumaris.core.util.Beans;
import net.sumaris.core.vo.administration.programStrategy.*;
import net.sumaris.core.vo.filter.StrategyFilterVO;
import net.sumaris.core.vo.referential.PmfmVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import net.sumaris.core.vo.referential.TaxonGroupVO;
import net.sumaris.core.vo.referential.TaxonNameVO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.EntityManager;
import javax.persistence.criteria.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author peck7 on 24/08/2020.
 */
public class StrategyRepositoryImpl
    extends ReferentialRepositoryImpl<Strategy, StrategyVO, StrategyFilterVO, StrategyFetchOptions>
    implements StrategySpecifications {

    private static final Logger log =
        LoggerFactory.getLogger(ReferentialRepositoryImpl.class);

    @Autowired
    private ReferentialDao referentialDao;

    @Autowired
    private PmfmStrategyRepository pmfmStrategyRepository;

    @Autowired
    private PmfmRepository pmfmRepository;

    @Autowired
    private TaxonNameRepository taxonNameRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    protected DepartmentRepository departmentRepository;

    public StrategyRepositoryImpl(EntityManager entityManager) {
        super(Strategy.class, StrategyVO.class, entityManager);
        setLockForUpdate(true);
    }

    @Override
    @Cacheable(cacheNames = CacheNames.PROGRAM_BY_ID)
    public Optional<StrategyVO> findById(int id) {
        return super.findById(id);
    }

    @Override
    @Cacheable(cacheNames = CacheNames.PROGRAM_BY_LABEL)
    public Optional<StrategyVO> findByLabel(String label) {
        return super.findByLabel(label);
    }

    @Override
    @Cacheable(cacheNames = CacheNames.STRATEGIES_BY_PROGRAM_ID, key = "#filter.programId * #fetchOptions.hashCode()")
    public List<StrategyVO> findAll(StrategyFilterVO filter, StrategyFetchOptions fetchOptions) {
        return super.findAll(filter, fetchOptions);
    }

    @Override
    @Caching(
        evict = {
            @CacheEvict(cacheNames = CacheNames.STRATEGIES_BY_ID, key = "#vo.id", condition = "#vo.id != null"),
            @CacheEvict(cacheNames = CacheNames.STRATEGIES_BY_LABEL, key = "#vo.label", condition = "#vo.label != null"),
            @CacheEvict(cacheNames = CacheNames.STRATEGIES_BY_PROGRAM_ID, allEntries = true)
        }
    )
    public StrategyVO save(StrategyVO vo) {
        Preconditions.checkNotNull(vo);
        Preconditions.checkNotNull(vo.getProgramId(), "Missing 'programId'");
        Preconditions.checkNotNull(vo.getLabel(), "Missing 'label'");
        Preconditions.checkNotNull(vo.getName(), "Missing 'name'");
        Preconditions.checkNotNull(vo.getStatusId(), "Missing 'statusId'");

        if (vo.getId() == null && vo.getStatusId() == null)
            // Set default status to Temporary
            vo.setStatusId(getConfig().getStatusIdTemporary());

        return super.save(vo);
    }

    @Override
    @Caching(
        evict = {
                @CacheEvict(cacheNames = CacheNames.STRATEGIES_BY_ID, allEntries = true),
                @CacheEvict(cacheNames = CacheNames.STRATEGIES_BY_LABEL, allEntries = true),
                @CacheEvict(cacheNames = CacheNames.STRATEGIES_BY_PROGRAM_ID, allEntries = true)
        }
    )
    public List<StrategyVO> saveByProgramId(int programId, List<StrategyVO> sources) {
        // Load parent entity
        Program parent = getOne(Program.class, programId);

        // Remember existing entities
        final List<Integer> sourcesIdsToRemove = Beans.collectIds(Beans.getList(parent.getStrategies()));

        // Save each entities
        List<StrategyVO> result = sources.stream().map(source -> {
            source.setProgramId(programId);
            if (source.getId() != null) {
                sourcesIdsToRemove.remove(source.getId());
            }
            return save(source);
        }).collect(Collectors.toList());

        // Remove unused entities
        if (CollectionUtils.isNotEmpty(sourcesIdsToRemove)) {
            sourcesIdsToRemove.forEach(this::deleteById);
        }

        return result;
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

        return getEntityManager()
            .createQuery(query)
            .setParameter(strategyIdParam, strategyId)
            .getResultStream()
            .map(referentialDao::toVO)
            .collect(Collectors.toList());

    }

    @Override
    public List<TaxonGroupStrategyVO> getTaxonGroupStrategies(int strategyId) {
        return getTaxonGroupStrategies(load(Strategy.class, strategyId));
    }

    @Override
    public List<TaxonNameStrategyVO> getTaxonNameStrategies(int strategyId) {
        return getTaxonNameStrategies(load(Strategy.class, strategyId));
    }

    @Override
    public List<AppliedStrategyVO> getAppliedStrategies(int strategyId) {
        return getAppliedStrategies(load(Strategy.class, strategyId));
    }

    @Override
    public List<StrategyDepartmentVO> getStrategyDepartments(int strategyId) {
        return getStrategyDepartments(load(Strategy.class, strategyId));
    }

    @Override
    protected void onAfterSaveEntity(StrategyVO vo, Strategy savedEntity, boolean isNew) {
        super.onAfterSaveEntity(vo, savedEntity, isNew);

        getEntityManager().flush();
        getEntityManager().clear();
    }

    public void saveProgramLocationsByStrategyId(int strategyId) {
        EntityManager em = getEntityManager();
        CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();

        Strategy strategy = getOne(Strategy.class, strategyId);

        // Get existing locations
        Set<Location> programLocations = new HashSet<>();
        CriteriaQuery<Program2Location> query = cb.createQuery(Program2Location.class);
        Root<Program2Location> root = query.from(Program2Location.class);
        query.where(cb.equal(root.get(Program2Location.Fields.PROGRAM), strategy.getProgram()));
        em.createQuery(query).getResultStream().forEach(p2l -> programLocations.add(p2l.getLocation()));

        Beans.getList(strategy.getAppliedStrategies()).forEach(appliedStrategy -> {
            if (appliedStrategy.getLocation() != null && !programLocations.contains(appliedStrategy.getLocation())) {
                Program2Location p2l = new Program2Location();
                p2l.setProgram(strategy.getProgram());
                p2l.setLocation(appliedStrategy.getLocation());
                em.persist(p2l);
                programLocations.add(appliedStrategy.getLocation());
            }
        });
    }

    @Override
    @Caching(
        evict = {
            @CacheEvict(cacheNames = CacheNames.STRATEGIES_BY_ID, key = "#id", condition = "#id != null"),
            @CacheEvict(cacheNames = CacheNames.STRATEGIES_BY_LABEL, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.STRATEGIES_BY_PROGRAM_ID, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.PMFM_BY_STRATEGY_ID, key = "#id")
        }
    )
    public void deleteById(Integer id) {
        super.deleteById(id);
    }

    @Override
    protected Specification<Strategy> toSpecification(StrategyFilterVO filter) {
        return super.toSpecification(filter)
            .and(hasProgramId(filter.getProgramId()));
    }

    @Override
    protected void toVO(Strategy source, StrategyVO target, StrategyFetchOptions fetchOptions, boolean copyIfNull) {
        super.toVO(source, target, fetchOptions, copyIfNull);
        StrategyFetchOptions finalFetchOptions = (fetchOptions != null) ? fetchOptions
                : StrategyFetchOptions.builder().withPmfmStrategyInheritance(false).withPmfmStrategyExpanded(false).build();

        // Program
        target.setProgramId(source.getProgram().getId());

        // Gears
        if (CollectionUtils.isNotEmpty(source.getGears())) {
            List<ReferentialVO> gears = source.getGears()
                .stream()
                .map(referentialDao::toVO)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
            target.setGears(gears);
        }

        // Taxon groups
        target.setTaxonGroups(getTaxonGroupStrategies(source));

        // Taxon names
        target.setTaxonNames(getTaxonNameStrategies(source));

        // Applied strategies
        target.setAppliedStrategies(getAppliedStrategies(source));

        // Strategy departments
        target.setStrategyDepartments(getStrategyDepartments(source));

        // Pmfm strategies
        target.setPmfmStrategies(getPmfmStrategies(source, finalFetchOptions));
    }

    @Override
    public void toEntity(StrategyVO source, Strategy target, boolean copyIfNull) {
        super.toEntity(source, target, copyIfNull);

        // Program
        if (copyIfNull || source.getProgramId() != null) {
            if (source.getProgramId() == null) {
                target.setProgram(null);
            }
            else {
                target.setProgram(load(Program.class, source.getProgramId()));
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
                Optional<TaxonNameVO> taxonName = taxonNameRepository.findTaxonNameReferent(item.getReferenceTaxon().getId());
                if (taxonName.isPresent()) {
                    target.setTaxonName(taxonName.get());
                    target.setReferenceTaxonId(taxonName.get().getReferenceTaxonId());
                    target.setIsReferent(taxonName.get().getIsReferent());
                }
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

    public List<TaxonGroupStrategyVO> saveTaxonGroupStrategiesByStrategyId(int strategyId, List<TaxonGroupStrategyVO> sources) {
        Preconditions.checkNotNull(sources);

        Strategy parent = getOne(Strategy.class, strategyId);

        sources.forEach(source -> {
            source.setStrategyId(strategyId);
        });

        EntityManager em = getEntityManager();

        // Remember existing entities
        Map<Integer, TaxonGroupStrategy> sourcesToRemove = Beans.splitByProperty(parent.getTaxonGroups(),
            TaxonGroupStrategy.Fields.TAXON_GROUP + "." + TaxonGroup.Fields.ID);

        // Save each taxon group strategy
        List<TaxonGroupStrategy> result = Beans.getStream(sources).map(source -> {
            Integer taxonGroupId = source.getTaxonGroup() != null ? source.getTaxonGroup().getId() : null;
            if (taxonGroupId == null) throw new DataIntegrityViolationException("Missing taxonGroup.id in a TaxonGroupStrategyVO");
            TaxonGroupStrategy target = sourcesToRemove.remove(taxonGroupId);
            boolean isNew = target == null;
            if (isNew) {
                target = new TaxonGroupStrategy();
                target.setTaxonGroup(load(TaxonGroup.class, taxonGroupId));
                target.setStrategy(parent);
            }
            target.setPriorityLevel(source.getPriorityLevel());
            if (isNew) {
                em.persist(target);
            }
            else {
                em.merge(target);
            }
            return target;
        }).collect(Collectors.toList());

        // Remove unused entities
        if (MapUtils.isNotEmpty(sourcesToRemove)) {
            sourcesToRemove.values().forEach(em::remove);
        }

        return sources.isEmpty() ? null : sources;
    }

    public List<TaxonNameStrategyVO> saveReferenceTaxonStrategiesByStrategyId(int strategyId, List<TaxonNameStrategyVO> sources) {
        Preconditions.checkNotNull(sources);

        Strategy parent = getOne(Strategy.class, strategyId);

        sources.forEach(source -> {
            source.setStrategyId(strategyId);
        });

        EntityManager em = getEntityManager();

        // Remember existing entities
        Map<Integer, ReferenceTaxonStrategy> sourcesToRemove = Beans.splitByProperty(parent.getReferenceTaxons(),
            ReferenceTaxonStrategy.Fields.REFERENCE_TAXON + "." + ReferenceTaxon.Fields.ID);

        // Save each reference taxon strategy
        List<ReferenceTaxonStrategy> result = Beans.getStream(sources).map(source -> {
            Integer referenceTaxonId = source.getReferenceTaxonId() != null ? source.getReferenceTaxonId() :
                (source.getTaxonName() != null ? source.getTaxonName().getReferenceTaxonId() : null);
            if (referenceTaxonId == null) throw new DataIntegrityViolationException("Missing referenceTaxon.id in a ReferenceTaxonStrategyVO");
            ReferenceTaxonStrategy target = sourcesToRemove.remove(referenceTaxonId);
            boolean isNew = target == null;
            if (isNew) {
                target = new ReferenceTaxonStrategy();
                target.setReferenceTaxon(load(ReferenceTaxon.class, referenceTaxonId));
                target.setStrategy(parent);
            }
            target.setPriorityLevel(source.getPriorityLevel());
            if (isNew) {
                em.persist(target);
            }
            else {
                em.merge(target);
            }
            return target;
        }).collect(Collectors.toList());

        // Remove unused entities
        if (MapUtils.isNotEmpty(sourcesToRemove)) {
            sourcesToRemove.values().forEach(em::remove);
        }

        return sources.isEmpty() ? null : sources;
    }

    protected List<AppliedStrategyVO> getAppliedStrategies(Strategy source) {
        if (CollectionUtils.isEmpty(source.getAppliedStrategies())) return null;
        return source.getAppliedStrategies()
                .stream()
                // Sort by id
                .sorted(Comparator.comparingInt((item) -> item.getId()))
                .map(item -> {
                    AppliedStrategyVO target = new AppliedStrategyVO();
                    target.setId(item.getId());
                    target.setStrategyId(source.getId());

                    target.setLocation(locationRepository.toVO(item.getLocation()));

                    // AppliedPeriod
                    List<AppliedPeriodVO> targetPeriods = Lists.newArrayList();
                    for (AppliedPeriod itemPeriod : item.getAppliedPeriods()) {
                        AppliedPeriodVO targetPeriod = new AppliedPeriodVO();
                        Beans.copyProperties(itemPeriod, targetPeriod);
                        targetPeriod.setAppliedStrategyId(itemPeriod.getAppliedStrategy().getId());
                        targetPeriods.add(targetPeriod);
                    }
                    target.setAppliedPeriods(targetPeriods);

                    return target;
                })
                .collect(Collectors.toList());
    }

    public List<AppliedStrategyVO> saveAppliedStrategiesByStrategyId(int strategyId, List<AppliedStrategyVO> sources) {
        Preconditions.checkNotNull(sources);

        Strategy parent = getOne(Strategy.class, strategyId);

        sources.forEach(source -> {
            source.setStrategyId(strategyId);
        });

        EntityManager em = getEntityManager();

        // Remember existing entities
        Map<Integer, AppliedStrategy> sourcesToRemove = Beans.splitById(parent.getAppliedStrategies());

        // Save each applied strategy
        List<AppliedStrategy> result = Beans.getStream(sources).map(source -> {
            Integer appliedStrategyId = source.getId();
            //if (appliedStrategyId == null) throw new DataIntegrityViolationException("Missing id in a AppliedStrategyVO");
            AppliedStrategy target = sourcesToRemove.remove(appliedStrategyId);
            boolean isNew = target == null;
            if (isNew) {
                target = new AppliedStrategy();
                target.setStrategy(parent);
            }
            if (source.getLocation() != null) {
                target.setLocation(load(Location.class, source.getLocation().getId()));
            }

            if (isNew) {
                em.persist(target);
            }
            else {
                em.merge(target);
            }

            // AppliedPeriod
            saveAppliedPeriodsByAppliedStrategyId(target.getId(), source.getAppliedPeriods());

            return target;
        }).collect(Collectors.toList());

        // Remove unused entities
        if (MapUtils.isNotEmpty(sourcesToRemove)) {
            sourcesToRemove.values().forEach(em::remove);
        }

        return sources.isEmpty() ? null : sources;
    }

    protected List<AppliedPeriodVO> saveAppliedPeriodsByAppliedStrategyId(int appliedStrategyId, List<AppliedPeriodVO> sources) {
        Preconditions.checkNotNull(sources);

        AppliedStrategy parent = getOne(AppliedStrategy.class, appliedStrategyId);

        sources.forEach(source -> {
            source.setAppliedStrategyId(appliedStrategyId);
        });

        EntityManager em = getEntityManager();

        // Remove existing entities
        List<AppliedPeriod> sourcesToRemove = parent.getAppliedPeriods();
        if (CollectionUtils.isNotEmpty(sourcesToRemove)) {
            sourcesToRemove.forEach(em::remove);
        }

        // Save each applied period
        List<AppliedPeriod> result = Beans.getStream(sources).map(source -> {
            if (source.getStartDate() == null) throw new DataIntegrityViolationException("Missing startDate in a AppliedPeriodVO");
            AppliedPeriod target = new AppliedPeriod();
            target.setAppliedStrategy(parent);
            target.setStartDate(source.getStartDate());
            target.setEndDate(source.getEndDate());
            target.setAcquisitionNumber(source.getAcquisitionNumber());

            em.persist(target);
            return target;
        }).collect(Collectors.toList());

        return sources.isEmpty() ? null : sources;
    }

    protected List<StrategyDepartmentVO> getStrategyDepartments(Strategy source) {
        if (CollectionUtils.isEmpty(source.getStrategyDepartments())) return null;
        return source.getStrategyDepartments()
                .stream()
                // Sort by id
                .sorted(Comparator.comparingInt((item) -> item.getId()))
                .map(item -> {
                    StrategyDepartmentVO target = new StrategyDepartmentVO();
                    target.setId(item.getId());
                    target.setUpdateDate(item.getUpdateDate());
                    target.setStrategyId(source.getId());

                    if (item.getLocation() != null) {
                        target.setLocation(referentialDao.toVO(item.getLocation()));
                    }
                    target.setDepartment(referentialDao.toVO(item.getDepartment()));
                    target.setPrivilege(referentialDao.toVO(item.getPrivilege()));

                    return target;
                })
                .collect(Collectors.toList());
    }

    public List<StrategyDepartmentVO> saveStrategyDepartmentsByStrategyId(int strategyId, List<StrategyDepartmentVO> sources) {
        Preconditions.checkNotNull(sources);

        Strategy parent = getOne(Strategy.class, strategyId);

        sources.forEach(source -> {
            source.setStrategyId(strategyId);
        });

        EntityManager em = getEntityManager();

        // Remember existing entities
        Map<Integer, StrategyDepartment> sourcesToRemove = Beans.splitById(parent.getStrategyDepartments());

        // Save each strategy department
        List<StrategyDepartment> result = Beans.getStream(sources).map(source -> {
            Integer strategyDepartmentId = source.getId();
            //if (strategyDepartmentId == null) throw new DataIntegrityViolationException("Missing id in a StrategyDepartmentVO");
            StrategyDepartment target = sourcesToRemove.remove(strategyDepartmentId);
            boolean isNew = target == null;
            if (isNew) {
                target = new StrategyDepartment();
                target.setStrategy(parent);
                target.setUpdateDate(getDatabaseCurrentTimestamp());
            }
            if (source.getLocation() != null) {
                target.setLocation(load(Location.class, source.getLocation().getId()));
            }
            if (source.getDepartment() != null) {
                target.setDepartment(load(Department.class, source.getDepartment().getId()));
            }
            if (source.getPrivilege() != null) {
                target.setPrivilege(load(ProgramPrivilege.class, source.getPrivilege().getId()));
            }

            if (isNew) {
                em.persist(target);
            }
            else {
                em.merge(target);
            }
            return target;
        }).collect(Collectors.toList());

        // Remove unused entities
        if (MapUtils.isNotEmpty(sourcesToRemove)) {
            sourcesToRemove.values().forEach(em::remove);
        }

        return sources.isEmpty() ? null : sources;
    }

    protected List<PmfmStrategyVO> getPmfmStrategies(Strategy source, StrategyFetchOptions fetchOptions) {
        Preconditions.checkNotNull(fetchOptions);
        if (CollectionUtils.isEmpty(source.getPmfmStrategies())) return null;
        List<PmfmStrategyVO> pmfmStrategies = new ArrayList<>();

        if (fetchOptions.isWithPmfmStrategyExpanded()) {
            for (PmfmStrategy pmfmStrategy : source.getPmfmStrategies()) {
                if (pmfmStrategy.getPmfm() != null) {
                    pmfmStrategies.add(pmfmStrategyRepository.toVO(pmfmStrategy, fetchOptions));
                } else {
                    List<PmfmVO> targetPmfms = pmfmRepository.findAll(pmfmStrategy.getParameter(), pmfmStrategy.getMatrix(), pmfmStrategy.getFraction(), pmfmStrategy.getMethod());
                    for (PmfmVO targetPmfm : targetPmfms) {
                        PmfmStrategyVO targetPmfmStrategy = pmfmStrategyRepository.toVO(pmfmStrategy, fetchOptions);
                        targetPmfmStrategy.setPmfm(targetPmfm);
                        targetPmfmStrategy.setPmfmId(targetPmfm.getId());
                        pmfmStrategies.add(targetPmfmStrategy);
                    }
                }
            }
        } else {
            pmfmStrategies = source.getPmfmStrategies()
                    .stream()
                    .map(ps -> pmfmStrategyRepository.toVO(ps, fetchOptions))
                    .collect(Collectors.toList());
        }

        return pmfmStrategies
                .stream()
                .filter(Objects::nonNull)
                // Sort by acquisitionLevel and rankOrder
                .sorted(Comparator.comparing(ps -> String.format("%s#%s", ps.getAcquisitionLevel(), ps.getRankOrder())))
                .collect(Collectors.toList());
    }

}

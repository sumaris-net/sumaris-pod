package net.sumaris.core.dao.administration.programStrategy;

/*-
 * #%L
 * SUMARiS:: Core
 * %%
 * Copyright (C) 2018 SUMARiS Consortium
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
import com.google.common.collect.Maps;
import net.sumaris.core.dao.referential.ReferentialDao;
import net.sumaris.core.dao.referential.taxon.TaxonNameDao;
import net.sumaris.core.dao.technical.hibernate.HibernateDaoSupport;
import net.sumaris.core.model.administration.programStrategy.*;
import net.sumaris.core.model.referential.Status;
import net.sumaris.core.model.referential.StatusEnum;
import net.sumaris.core.model.referential.gear.Gear;
import net.sumaris.core.model.referential.pmfm.Parameter;
import net.sumaris.core.model.referential.pmfm.Pmfm;
import net.sumaris.core.model.referential.taxon.ReferenceTaxon;
import net.sumaris.core.model.referential.taxon.TaxonGroup;
import net.sumaris.core.util.Beans;
import net.sumaris.core.vo.administration.programStrategy.*;
import net.sumaris.core.vo.filter.ReferentialFilterVO;
import net.sumaris.core.vo.referential.PmfmValueType;
import net.sumaris.core.vo.referential.ReferentialVO;
import net.sumaris.core.vo.referential.TaxonGroupVO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.criteria.*;
import java.sql.Timestamp;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Repository("strategyDao")
public class StrategyDaoImpl extends HibernateDaoSupport implements StrategyDao {

    /**
     * Logger.
     */
    private static final Logger log =
            LoggerFactory.getLogger(StrategyDaoImpl.class);


    @Autowired
    private ReferentialDao referentialDao;

    @Autowired
    private TaxonNameDao taxonNameDao;

    private Map<String, Integer> acquisitionLevelIdByLabel = Maps.newConcurrentMap();

    private int unitIdNone;

    @PostConstruct
    protected void init() {
        this.unitIdNone = config.getUnitIdNone();
    }

    @Override
    public List<StrategyVO> findByProgram(int programId, StrategyFetchOptions fetchOptions) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Strategy> query = builder.createQuery(Strategy.class);
        Root<Strategy> root = query.from(Strategy.class);

        ParameterExpression<Integer> programIdParam = builder.parameter(Integer.class);

        query.select(root)
                .where(
                        builder.equal(root.get(Strategy.Fields.PROGRAM).get(Program.Fields.ID), programIdParam));

        // Sort by rank order
        query.orderBy(builder.asc(root.get(Strategy.Fields.ID)));

        return getEntityManager()
                .createQuery(query)
                .setParameter(programIdParam, programId)
                .getResultStream()
                .map(s -> this.toStrategyVO(s, fetchOptions))
                .collect(Collectors.toList());
    }

    @Override
    public List<PmfmStrategyVO> getPmfmStrategies(int strategyId) {

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<PmfmStrategy> query = builder.createQuery(PmfmStrategy.class);
        Root<PmfmStrategy> root = query.from(PmfmStrategy.class);

        ParameterExpression<Integer> strategyIdParam = builder.parameter(Integer.class);

        Join<PmfmStrategy, Strategy> strategyInnerJoin = root.join(PmfmStrategy.Fields.STRATEGY, JoinType.INNER);

        query.select(root)
                .where(builder.equal(strategyInnerJoin.get(Strategy.Fields.ID), strategyIdParam))
                // Sort by rank order
                .orderBy(builder.asc(root.get(PmfmStrategy.Fields.RANK_ORDER)));

        return getEntityManager()
                .createQuery(query)
                .setParameter(strategyIdParam, strategyId)
                .getResultStream()
                .map(this::toPmfmStrategyVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<PmfmStrategyVO> getPmfmStrategiesByAcquisitionLevel(int programId, int acquisitionLevelId) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<PmfmStrategy> query = builder.createQuery(PmfmStrategy.class);
        Root<PmfmStrategy> root = query.from(PmfmStrategy.class);

        ParameterExpression<Integer> programIdParam = builder.parameter(Integer.class);
        ParameterExpression<Integer> acquisitionLevelIdParam = builder.parameter(Integer.class);

        Join<PmfmStrategy, Strategy> strategyInnerJoin = root.join(PmfmStrategy.Fields.STRATEGY, JoinType.INNER);

        query.select(root)
                .where(
                        builder.and(
                                builder.equal(strategyInnerJoin.get(Strategy.Fields.PROGRAM).get(Program.Fields.ID), programIdParam),
                                builder.equal(root.get(PmfmStrategy.Fields.ACQUISITION_LEVEL).get(AcquisitionLevel.Fields.ID), acquisitionLevelIdParam)
                        ));

        // Sort by rank order
        query.orderBy(builder.asc(root.get(PmfmStrategy.Fields.RANK_ORDER)));

        return getEntityManager()
                .createQuery(query)
                .setParameter(programIdParam, programId)
                .setParameter(acquisitionLevelIdParam, acquisitionLevelId)
                .getResultStream()
                .map(this::toPmfmStrategyVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ReferentialVO> getGears(int strategyId) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
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
                .map(referentialDao::toReferentialVO)
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


    /* -- protected methods -- */

    protected StrategyVO toStrategyVO(Strategy source, StrategyFetchOptions fetchOptions) {
        if (source == null) return null;

        StrategyVO target = new StrategyVO();

        Beans.copyProperties(source, target);

        // Program
        target.setProgramId(source.getProgram().getId());

        // Status id
        target.setStatusId(source.getStatus().getId());

        // Gears
        if (CollectionUtils.isNotEmpty(source.getGears())) {
            List<ReferentialVO> gears = source.getGears()
                    .stream()
                    .map(referentialDao::toReferentialVO)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            target.setGears(gears);
        }

        // Taxon groups
        target.setTaxonGroups(getTaxonGroupStrategies(source));

        // Taxon names
        target.setTaxonNames(getTaxonNameStrategies(source));

        // Pmfm strategies
        if (CollectionUtils.isNotEmpty(source.getPmfmStrategies())) {
            List<PmfmStrategyVO> pmfmStrategies = source.getPmfmStrategies()
                    .stream()
                    // Transform to VO
                    .map(ps -> toPmfmStrategyVO(ps, fetchOptions))
                    .filter(Objects::nonNull)
                    // Sort by acquisitionLevel and rankOrder
                    .sorted(Comparator.comparing(ps -> String.format("%s#%s", ps.getAcquisitionLevel(), ps.getRankOrder())))
                    .collect(Collectors.toList());
            target.setPmfmStrategies(pmfmStrategies);
        }

        return target;
    }

    protected PmfmStrategyVO toPmfmStrategyVO(PmfmStrategy source) {
        return toPmfmStrategyVO(source, StrategyFetchOptions.builder().withPmfmStrategyInheritance(false).build());
    }

    protected PmfmStrategyVO toPmfmStrategyVO(PmfmStrategy source, StrategyFetchOptions fetchOptions) {
        return toPmfmStrategyVO(source, fetchOptions.isWithPmfmStrategyInheritance());
    }

    @Override
    public PmfmStrategyVO toPmfmStrategyVO(PmfmStrategy source, boolean enablePmfmInheritance) {
        if (source == null) return null;

        Pmfm pmfm = source.getPmfm();
        Preconditions.checkNotNull(pmfm);

        PmfmStrategyVO target = new PmfmStrategyVO();

        // Copy properties, from Pmfm first (if inherit enable), then from source
        if (enablePmfmInheritance) {
            Beans.copyProperties(pmfm, target);
        }
        Beans.copyProperties(source, target);

        // Set some attributes from Pmfm
        target.setPmfmId(pmfm.getId());

        // Apply default values from Pmfm
        if (pmfm.getMethod() != null) {
            target.setMethodId(pmfm.getMethod().getId());
        }
        if (target.getMinValue() == null) {
            target.setMinValue(pmfm.getMinValue());
        }
        if (target.getMaxValue() == null) {
            target.setMaxValue(pmfm.getMaxValue());
        }
        if (target.getDefaultValue() == null) {
            target.setDefaultValue(pmfm.getDefaultValue());
        }

        // Parameter name
        Parameter parameter = pmfm.getParameter();
        target.setName(parameter.getName());

        // Value Type
        PmfmValueType type = PmfmValueType.fromPmfm(pmfm);
        target.setType(type.name().toLowerCase());

        // Unit symbol
        if (pmfm.getUnit() != null && pmfm.getUnit().getId().intValue() != unitIdNone) {
            target.setUnitLabel(pmfm.getUnit().getLabel());
        }

        // Acquisition Level
        if (source.getAcquisitionLevel() != null) {
            target.setAcquisitionLevel(source.getAcquisitionLevel().getLabel());
        }

        // Qualitative values
        if (CollectionUtils.isNotEmpty(parameter.getQualitativeValues())) {
            List<ReferentialVO> qualitativeValues = parameter.getQualitativeValues()
                    .stream()
                    .map(referentialDao::toReferentialVO)
                    .collect(Collectors.toList());
            target.setQualitativeValues(qualitativeValues);
        }

        // Gears
        if (CollectionUtils.isNotEmpty(source.getGears())) {
            List<String> gears = source.getGears()
                    .stream()
                    .map(Gear::getLabel)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            target.setGears(gears);

            target.setGearIds(Beans.collectIds(source.getGears()));
        }

        // Taxon groups
        if (CollectionUtils.isNotEmpty(source.getTaxonGroups())) {
            target.setTaxonGroupIds(Beans.collectIds(source.getTaxonGroups()));
        }

        // Reference taxons
        if (CollectionUtils.isNotEmpty(source.getReferenceTaxons())) {
            target.setReferenceTaxonIds(Beans.collectIds(source.getReferenceTaxons()));
        }

        return target;
    }

    @Override
    public List<StrategyVO> saveByProgramId(int programId, List<StrategyVO> sources) {
        // Load parent entity
        Program parent = get(Program.class, programId);

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
            sourcesIdsToRemove.forEach(this::delete);
        }

        return result;
    }

    @Override
    public StrategyVO save(StrategyVO source) {
        Preconditions.checkNotNull(source);
        Preconditions.checkNotNull(source.getProgramId(), "Missing 'programId'");
        Preconditions.checkNotNull(source.getLabel(), "Missing 'label'");
        Preconditions.checkNotNull(source.getName(), "Missing 'name'");
        Preconditions.checkNotNull(source.getStatusId(), "Missing 'statusId'");

        EntityManager entityManager = getEntityManager();
        Strategy entity = null;
        if (source.getId() != null) {
            entity = get(Strategy.class, source.getId());
        }
        boolean isNew = (entity == null);
        if (isNew) {
            entity = new Strategy();
        }

        // If new
        if (isNew) {
            // Set default status to Temporary
            if (source.getStatusId() == null) {
                source.setStatusId(config.getStatusIdTemporary());
            }
        }
        // If update
        else {

            // Check update date
            checkUpdateDateForUpdate(source, entity);

            // Lock entityName
            lockForUpdate(entity, LockModeType.PESSIMISTIC_WRITE);
        }

        toEntity(source, entity, true);

        // Update update_dt
        Timestamp newUpdateDate = getDatabaseCurrentTimestamp();
        entity.setUpdateDate(newUpdateDate);

        // Save entity
        if (isNew) {
            // Force creation date
            entity.setCreationDate(newUpdateDate);
            source.setCreationDate(newUpdateDate);

            entityManager.persist(entity);
            source.setId(entity.getId());
        } else {
            entityManager.merge(entity);
        }

        source.setUpdateDate(newUpdateDate);

        // Save pmfm stratgeies
        //saveProperties(source.getProperties(), entity, newUpdateDate);

        //getEntityManager().flush();
        //getEntityManager().clear();

        return source;
    }

    public void delete(int id) {
        log.debug(String.format("Deleting strategy {id=%s}...", id));
        delete(Strategy.class, id);
    }

    /* -- protected method -- */

    protected void toEntity(StrategyVO source, Strategy target, boolean copyIfNull) {
        EntityManager em = getEntityManager();

        Beans.copyProperties(source, target);

        // Program
        if (copyIfNull || source.getProgramId() != null) {
            if (source.getProgramId() == null) {
                target.setProgram(null);
            }
            else {
                target.setProgram(load(Program.class, source.getProgramId()));
            }
        }

        // Status
        if (copyIfNull || source.getStatusId() != null) {
            if (source.getStatusId() == null) {
                target.setStatus(null);
            }
            else {
                target.setStatus(load(Status.class, source.getStatusId()));
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

        // Taxon Group strategy
        if (copyIfNull || CollectionUtils.isNotEmpty(source.getTaxonGroups())) {
            saveTaxonGroupStrategiesByStrategy(source.getTaxonGroups(), target);
        }

        // Reference Names strategy
        if (copyIfNull || CollectionUtils.isNotEmpty(source.getTaxonNames())) {
            saveReferenceTaxonStrategiesByStrategy(source.getTaxonNames(), target);
        }

        // Pmfm Strategies
        if (copyIfNull || CollectionUtils.isNotEmpty(source.getPmfmStrategies())) {
            savePmfmStrategiesByStrategy(source.getPmfmStrategies(), target);
        }
    }

    protected List<TaxonNameStrategyVO> getTaxonNameStrategies(Strategy source) {
        if (CollectionUtils.isEmpty(source.getReferenceTaxons())) return null;

        return source.getReferenceTaxons()
                .stream()
                // Sort by priority level (or if not set, by id)
                .sorted(Comparator.comparingInt(item -> item.getPriorityLevel() != null ?
                        item.getPriorityLevel().intValue() :
                        item.getReferenceTaxon().getId().intValue()))
                .map(item -> {
                    TaxonNameStrategyVO target = new TaxonNameStrategyVO();
                    target.setStrategyId(source.getId());

                    // Priority level
                    target.setPriorityLevel(item.getPriorityLevel());

                    // Taxon name
                    target.setTaxonName(taxonNameDao.getTaxonNameReferent(item.getReferenceTaxon().getId()));
                    return target;
                })
                .collect(Collectors.toList());
    }

    protected List<TaxonGroupStrategyVO> getTaxonGroupStrategies(Strategy source) {
        if (CollectionUtils.isEmpty(source.getTaxonGroups())) return null;
        return source.getTaxonGroups()
                .stream()
                // Sort by priority level (or if not set, by id)
                .sorted(Comparator.comparingInt((item) -> item.getPriorityLevel() != null ?
                        item.getPriorityLevel().intValue() :
                        item.getTaxonGroup().getId().intValue()))
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

    protected void saveTaxonGroupStrategiesByStrategy(List<TaxonGroupStrategyVO> sources, Strategy parent) {
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

        // Update the target strategy
        parent.setTaxonGroups(result);

        // Remove unused entities
        if (MapUtils.isNotEmpty(sourcesToRemove)) {
            sourcesToRemove.values().forEach(em::remove);
        }
    }

    protected void saveReferenceTaxonStrategiesByStrategy(List<TaxonNameStrategyVO> sources, Strategy parent) {
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

        // Update the target strategy
        parent.setReferenceTaxons(result);

        // Remove unused entities
        if (MapUtils.isNotEmpty(sourcesToRemove)) {
            sourcesToRemove.values().forEach(em::remove);
        }
    }


    protected void savePmfmStrategiesByStrategy(List<PmfmStrategyVO> sources, Strategy parent) {
        EntityManager em = getEntityManager();


        // Remember existing entities
        Map<Integer, PmfmStrategy> sourcesToRemove = Beans.splitById(parent.getPmfmStrategies());

        // Save each reference taxon strategy
        List<PmfmStrategy> result = Beans.getStream(sources).map(source -> {
            PmfmStrategy target = source.getId() != null ? sourcesToRemove.remove(source.getId()) : null;
            boolean isNew = target == null;
            if (isNew) {
                target = new PmfmStrategy();
                target.setStrategy(parent);
            }

            toPmfmStrategyEntity(source, target, true);

            if (isNew) {
                em.persist(target);

                // Update the VO
                source.setId(target.getId());
            }
            else {
                em.merge(target);
            }


            return target;
        }).collect(Collectors.toList());

        // Update the target strategy
        parent.setPmfmStrategies(result);

        // Remove unused entities
        if (MapUtils.isNotEmpty(sourcesToRemove)) {
            sourcesToRemove.values().forEach(em::remove);
        }
    }

    protected void toPmfmStrategyEntity(PmfmStrategyVO source, PmfmStrategy target, boolean copyIfNull) {

        Beans.copyProperties(source, target);

        // Pmfm
        Integer pmfmId = source.getPmfmId() != null ? source.getPmfmId() :
                (source.getPmfm() != null ? source.getPmfm().getId() : null);
        if (pmfmId == null) throw new DataIntegrityViolationException("Missing pmfmId or pmfm.id in a PmfmStrategyVO");
        target.setPmfm(load(Pmfm.class, pmfmId));

        // Acquisition Level
        Integer acquisitionLevelId = acquisitionLevelIdByLabel.get(source.getAcquisitionLevel());
        if (acquisitionLevelId == null) {
            // Try to reload
            loadAcquisitionLevels();

            // Retry to find it
            acquisitionLevelId = acquisitionLevelIdByLabel.get(source.getAcquisitionLevel());
            if (acquisitionLevelId == null) {
                throw new DataIntegrityViolationException("Unknown acquistion level with label=" + source.getAcquisitionLevel());
            }
        }
        target.setAcquisitionLevel(load(AcquisitionLevel.class, acquisitionLevelId));

        // Gears
        if (copyIfNull || CollectionUtils.isNotEmpty(source.getGearIds())) {
            target.getGears().clear();
            if (CollectionUtils.isNotEmpty(source.getGearIds())) {
                target.getGears().addAll(loadAllAsSet(Gear.class, source.getGearIds(), true));
            }
        }

        // Taxon Groups
        if (copyIfNull || CollectionUtils.isNotEmpty(source.getTaxonGroupIds())) {
            target.getTaxonGroups().clear();
            if (CollectionUtils.isNotEmpty(source.getTaxonGroupIds())) {
                target.getTaxonGroups().addAll(loadAllAsSet(TaxonGroup.class, source.getTaxonGroupIds(), true));
            }
        }

        // Taxon Names
        if (copyIfNull || CollectionUtils.isNotEmpty(source.getReferenceTaxonIds())) {
            target.getReferenceTaxons().clear();
            if (CollectionUtils.isNotEmpty(source.getReferenceTaxonIds())) {
                target.getReferenceTaxons().addAll(loadAllAsSet(ReferenceTaxon.class, source.getReferenceTaxonIds(), true));
            }
        }
    }

    private synchronized void loadAcquisitionLevels() {
        acquisitionLevelIdByLabel.clear();

        // Fill acquisition levels map
        List<ReferentialVO> items = referentialDao.findByFilter(AcquisitionLevel.class.getSimpleName(), new ReferentialFilterVO(), 0, 1000, null, null);
        items.forEach(item -> acquisitionLevelIdByLabel.put(item.getLabel(), item.getId()));
    }
}

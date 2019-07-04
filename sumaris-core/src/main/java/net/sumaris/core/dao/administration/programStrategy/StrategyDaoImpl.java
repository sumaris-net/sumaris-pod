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
import net.sumaris.core.dao.referential.ReferentialDao;
import net.sumaris.core.dao.referential.taxon.TaxonNameDao;
import net.sumaris.core.dao.technical.model.IEntity;
import net.sumaris.core.model.administration.programStrategy.*;
import net.sumaris.core.model.referential.taxon.TaxonGroup;
import net.sumaris.core.util.Beans;
import net.sumaris.core.dao.technical.hibernate.HibernateDaoSupport;
import net.sumaris.core.model.referential.*;
import net.sumaris.core.model.referential.gear.Gear;
import net.sumaris.core.model.referential.pmfm.Parameter;
import net.sumaris.core.model.referential.pmfm.Pmfm;
import net.sumaris.core.vo.administration.programStrategy.PmfmStrategyVO;
import net.sumaris.core.vo.administration.programStrategy.StrategyVO;
import net.sumaris.core.vo.referential.ParameterValueType;
import net.sumaris.core.vo.referential.ReferentialVO;
import net.sumaris.core.vo.referential.TaxonNameVO;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import javax.persistence.criteria.*;
import java.util.Comparator;
import java.util.List;
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

    private int unitIdNone;

    @PostConstruct
    protected void init() {
        this.unitIdNone = config.getUnitIdNone();
    }

    @Override
    public List<StrategyVO> findByProgram(int programId) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Strategy> query = builder.createQuery(Strategy.class);
        Root<Strategy> root = query.from(Strategy.class);

        ParameterExpression<Integer> programIdParam = builder.parameter(Integer.class);

        query.select(root)
                .where(
                        builder.equal(root.get(Strategy.PROPERTY_PROGRAM).get(Program.PROPERTY_ID), programIdParam));

        // Sort by rank order
        query.orderBy(builder.asc(root.get(Strategy.PROPERTY_ID)));

        return getEntityManager()
                .createQuery(query)
                .setParameter(programIdParam, programId)
                .getResultStream()
                .map(this::toStrategyVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<PmfmStrategyVO> getPmfmStrategies(int programId) {

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<PmfmStrategy> query = builder.createQuery(PmfmStrategy.class);
        Root<PmfmStrategy> root = query.from(PmfmStrategy.class);

        ParameterExpression<Integer> programIdParam = builder.parameter(Integer.class);
        //ParameterExpression<Date> appliedDateParam = builder.parameter(Date.class);
        //ParameterExpression<Integer> appliedLocationIdParam = builder.parameter(Integer.class);

        Join<PmfmStrategy, Strategy> strategyInnerJoin = root.join(PmfmStrategy.PROPERTY_STRATEGY, JoinType.INNER);

        query.select(root)
                .where(
                        builder.equal(strategyInnerJoin.get(Strategy.PROPERTY_PROGRAM).get(Program.PROPERTY_ID), programIdParam));

        // Sort by rank order
        query.orderBy(builder.asc(root.get(PmfmStrategy.PROPERTY_RANK_ORDER)));

        return getEntityManager()
                .createQuery(query)
                .setParameter(programIdParam, programId)
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

        Join<PmfmStrategy, Strategy> strategyInnerJoin = root.join(PmfmStrategy.PROPERTY_STRATEGY, JoinType.INNER);

        query.select(root)
                .where(
                        builder.and(
                                builder.equal(strategyInnerJoin.get(Strategy.PROPERTY_PROGRAM).get(Program.PROPERTY_ID), programIdParam),
                                builder.equal(root.get(PmfmStrategy.PROPERTY_ACQUISITION_LEVEL).get(AcquisitionLevel.PROPERTY_ID), acquisitionLevelIdParam)
                        ));

        // Sort by rank order
        query.orderBy(builder.asc(root.get(PmfmStrategy.PROPERTY_RANK_ORDER)));

        return getEntityManager()
                .createQuery(query)
                .setParameter(programIdParam, programId)
                .setParameter(acquisitionLevelIdParam, acquisitionLevelId)
                .getResultStream()
                .map(this::toPmfmStrategyVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ReferentialVO> getGears(int programId) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Gear> query = builder.createQuery(Gear.class);
        Root<Gear> root = query.from(Gear.class);

        ParameterExpression<Integer> programIdParam = builder.parameter(Integer.class);

        Join<Gear, Strategy> gearInnerJoin = root.joinList(Gear.PROPERTY_STRATEGIES, JoinType.INNER);

        query.select(root)
                .where(
                        builder.and(
                                // program
                                builder.equal(gearInnerJoin.get(Strategy.PROPERTY_PROGRAM).get(Program.PROPERTY_ID), programIdParam),
                                // Status (temporary or valid)
                                builder.in(root.get(Gear.PROPERTY_STATUS).get(Status.PROPERTY_ID)).value(ImmutableList.of(StatusEnum.ENABLE.getId(), StatusEnum.TEMPORARY.getId()))
                        ));

        // Sort by rank order
        query.orderBy(builder.asc(root.get(Gear.PROPERTY_LABEL)));

        return getEntityManager()
                .createQuery(query)
                .setParameter(programIdParam, programId)
                .getResultStream()
                .map(referentialDao::toReferentialVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ReferentialVO> getTaxonGroups(int programId) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<TaxonGroup> query = builder.createQuery(TaxonGroup.class);
        Root<TaxonGroup> root = query.from(TaxonGroup.class);

        ParameterExpression<Integer> programIdParam = builder.parameter(Integer.class);

        Join<TaxonGroup, TaxonGroupStrategy> innerJoinTGS = root.joinList(TaxonGroup.PROPERTY_STRATEGIES, JoinType.INNER);
        Join<TaxonGroupStrategy, Strategy> innerJoinS = innerJoinTGS.join(TaxonGroupStrategy.PROPERTY_STRATEGY, JoinType.INNER);


        query.select(root)
                .where(
                        builder.and(
                                // program
                                builder.equal(innerJoinS.get(Strategy.PROPERTY_PROGRAM).get(Program.PROPERTY_ID), programIdParam),
                                // Status (temporary or valid)
                                builder.in(root.get(TaxonGroup.PROPERTY_STATUS).get(Status.PROPERTY_ID)).value(ImmutableList.of(StatusEnum.ENABLE.getId(), StatusEnum.TEMPORARY.getId()))
                        ));

        // Sort by rank order
        query.orderBy(builder.asc(root.get(TaxonGroup.PROPERTY_LABEL)));

        return getEntityManager()
                .createQuery(query)
                .setParameter(programIdParam, programId)
                .getResultStream()
                .map(referentialDao::toReferentialVO)
                .collect(Collectors.toList());
    }



    /* -- protected methods -- */

    protected StrategyVO toStrategyVO(Strategy source) {
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
        if (CollectionUtils.isNotEmpty(source.getTaxonGroups())) {
            List<ReferentialVO> taxonGroups = source.getTaxonGroups()
                    .stream()
                    // Sort by priority level (or if not set, by id)
                    .sorted(Comparator.comparingInt((item) -> item.getPriorityLevel() != null ?
                            item.getPriorityLevel().intValue() :
                            item.getTaxonGroup().getId().intValue()))
                    .map(item -> referentialDao.toReferentialVO(item.getTaxonGroup()))
                    .collect(Collectors.toList());
            target.setTaxonGroups(taxonGroups);
        }

        // Taxon names
        if (CollectionUtils.isNotEmpty(source.getReferenceTaxons())) {
            List<TaxonNameVO> taxonNames = source.getReferenceTaxons()
                    .stream()
                    // Sort by priority level (or if not set, by id)
                    .sorted(Comparator.comparingInt(item -> item.getPriorityLevel() != null ?
                            item.getPriorityLevel().intValue() :
                            item.getReferenceTaxon().getId().intValue()))
                    .map(rf -> taxonNameDao.getTaxonNameReferent(rf.getReferenceTaxon().getId()))
                    .collect(Collectors.toList());
            target.setTaxonNames(taxonNames);
        }

        // Pmfm strategies
        if (CollectionUtils.isNotEmpty(source.getPmfmStrategies())) {
            List<PmfmStrategyVO> pmfmStrategies = source.getPmfmStrategies()
                    .stream()
                    // Transform to VO
                    .map(ps -> toPmfmStrategyVO(ps, false))
                    .filter(Objects::nonNull)
                    // Sort by acquisitionLevel and rankOrder
                    .sorted(Comparator.comparing(ps -> String.format("%s#%s", ps.getAcquisitionLevel(), ps.getRankOrder())))
                    .collect(Collectors.toList());
            target.setPmfmStrategies(pmfmStrategies);
        }

        return target;
    }

    protected PmfmStrategyVO toPmfmStrategyVO(PmfmStrategy source) {
        return toPmfmStrategyVO(source, true);
    }

    @Override
    public PmfmStrategyVO toPmfmStrategyVO(PmfmStrategy source, boolean inheritPmfmValue) {
        if (source == null) return null;

        Pmfm pmfm = source.getPmfm();
        Preconditions.checkNotNull(pmfm);

        PmfmStrategyVO target = new PmfmStrategyVO();

        // Copy properties, from Pmfm first (if inherit enable), then from source
        if (inheritPmfmValue) {
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

        // Parameter Type
        ParameterValueType type = ParameterValueType.fromPmfm(pmfm);
        target.setType(type.name().toLowerCase());

        // Unit symbol
        if (pmfm.getUnit() != null && pmfm.getUnit().getId().intValue() != unitIdNone) {
            target.setUnit(pmfm.getUnit().getLabel());
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
        }

        // Taxon groups
        if (CollectionUtils.isNotEmpty(source.getTaxonGroups())) {
            List<Integer> taxonGroupIds = source.getTaxonGroups()
                    .stream()
                    .map(IEntity::getId)
                    .collect(Collectors.toList());
            target.setTaxonGroupIds(taxonGroupIds);
        }

        // Reference taxons
        if (CollectionUtils.isNotEmpty(source.getReferenceTaxons())) {
            List<Integer> referenceTaxonIds = source.getReferenceTaxons()
                    .stream()
                    .map(IEntity::getId)
                    .collect(Collectors.toList());
            target.setReferenceTaxonIds(referenceTaxonIds);
        }

        return target;
    }

}

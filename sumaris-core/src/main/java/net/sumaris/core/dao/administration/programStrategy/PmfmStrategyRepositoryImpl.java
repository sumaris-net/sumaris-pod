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
import com.google.common.collect.Maps;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.dao.referential.ReferentialDao;
import net.sumaris.core.dao.technical.jpa.SumarisJpaRepositoryImpl;
import net.sumaris.core.dao.technical.model.IEntity;
import net.sumaris.core.event.config.ConfigurationEvent;
import net.sumaris.core.event.config.ConfigurationReadyEvent;
import net.sumaris.core.event.config.ConfigurationUpdatedEvent;
import net.sumaris.core.model.administration.programStrategy.AcquisitionLevel;
import net.sumaris.core.model.administration.programStrategy.PmfmStrategy;
import net.sumaris.core.model.administration.programStrategy.Program;
import net.sumaris.core.model.administration.programStrategy.Strategy;
import net.sumaris.core.model.referential.gear.Gear;
import net.sumaris.core.model.referential.pmfm.Parameter;
import net.sumaris.core.model.referential.pmfm.Pmfm;
import net.sumaris.core.model.referential.pmfm.UnitEnum;
import net.sumaris.core.model.referential.taxon.ReferenceTaxon;
import net.sumaris.core.model.referential.taxon.TaxonGroup;
import net.sumaris.core.util.Beans;
import net.sumaris.core.vo.administration.programStrategy.PmfmStrategyVO;
import net.sumaris.core.vo.administration.programStrategy.StrategyFetchOptions;
import net.sumaris.core.vo.filter.ReferentialFilterVO;
import net.sumaris.core.vo.referential.PmfmValueType;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.criteria.*;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Repository("pmfmStrategyRepository")
public class PmfmStrategyRepositoryImpl
        extends SumarisJpaRepositoryImpl<PmfmStrategy, Integer, PmfmStrategyVO>
        implements PmfmStrategyRepository {

    /**
     * Logger.
     */
    private static final Logger log =
            LoggerFactory.getLogger(PmfmStrategyRepositoryImpl.class);


    private Map<String, Integer> acquisitionLevelIdByLabel = Maps.newConcurrentMap();

    @Autowired
    private ReferentialDao referentialDao;

    @Autowired
    private SumarisConfiguration config;


    @Autowired
    PmfmStrategyRepositoryImpl(EntityManager entityManager) {
        super(PmfmStrategy.class, entityManager);
    }

    @EventListener({ConfigurationReadyEvent.class, ConfigurationUpdatedEvent.class})
    protected void onConfigurationReady(ConfigurationEvent event) {
        this.loadAcquisitionLevels();
    }

    @Override
    public Class<PmfmStrategyVO> getVOClass() {
        return PmfmStrategyVO.class;
    }

    @Override
    public List<PmfmStrategyVO> findByStrategyId(int strategyId, boolean enablePmfmInheritance) {

        CriteriaBuilder builder = getEntityManager().getCriteriaBuilder();
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
                .map(entity -> this.toVO(entity, enablePmfmInheritance))
                .collect(Collectors.toList());
    }

    @Override
    public List<PmfmStrategyVO> findByProgramAndAcquisitionLevel(int programId, int acquisitionLevelId, boolean enablePmfmInheritance) {
        CriteriaBuilder builder = getEntityManager().getCriteriaBuilder();
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
                .map(entity -> this.toVO(entity, enablePmfmInheritance))
                .collect(Collectors.toList());
    }


    @Override
    public PmfmStrategyVO toVO(PmfmStrategy source) {
        return toVO(source, StrategyFetchOptions.builder().withPmfmStrategyInheritance(false).build());
    }

    @Override
    public PmfmStrategyVO toVO(PmfmStrategy source, StrategyFetchOptions fetchOptions) {
        return toVO(source, fetchOptions.isWithPmfmStrategyInheritance());
    }

    @Override
    public PmfmStrategyVO toVO(PmfmStrategy source, boolean enablePmfmInheritance) {
        if (source == null) return null;

        Pmfm pmfm = source.getPmfm();
        Preconditions.checkNotNull(pmfm);

        PmfmStrategyVO target = new PmfmStrategyVO();

        // Copy properties, from Pmfm first (if inherit enable), then from source
        if (enablePmfmInheritance) {
            Beans.copyProperties(pmfm, target);
        }
        Beans.copyProperties(source, target);

        // Strategy Id
        target.setStrategyId(source.getStrategy().getId());

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
        if (pmfm.getUnit() != null && pmfm.getUnit().getId() != UnitEnum.NONE.getId()) {
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
    public List<PmfmStrategyVO> saveByStrategyId(int strategyId, List<PmfmStrategyVO> sources) {
        Preconditions.checkNotNull(sources);

        Strategy parent = get(Strategy.class, strategyId);

        sources.forEach(source -> {
            source.setStrategyId(strategyId);
        });

        // Load existing entities
        Map<Integer, PmfmStrategy> existingEntities = Beans.splitById(parent.getPmfmStrategies());

        sources.forEach(source -> {
            save(source);
            existingEntities.remove(source.getId());
        });

        // Delete remaining
        existingEntities.values().forEach(ps -> {
            this.delete(ps);
            parent.getPmfmStrategies().remove(ps);
        });

        return sources;
    }

    @Override
    public void toEntity(PmfmStrategyVO source, PmfmStrategy target, boolean copyIfNull) {

        Beans.copyProperties(source, target);

        // Parent
        if (source.getStrategyId() != null) {
            target.setStrategy(load(Strategy.class, source.getStrategyId()));
        }

        // Pmfm
        Integer pmfmId = source.getPmfmId() != null ? source.getPmfmId() :
                (source.getPmfm() != null ? source.getPmfm().getId() : null);
        if (pmfmId == null) throw new DataIntegrityViolationException("Missing pmfmId or pmfm.id in a PmfmStrategyVO");
        target.setPmfm(load(Pmfm.class, pmfmId));

        // Acquisition Level
        target.setAcquisitionLevel(load(AcquisitionLevel.class, getAcquisitionLevelIdByLabel(source.getAcquisitionLevel())));

        // Gears
        if (copyIfNull || CollectionUtils.isNotEmpty(source.getGearIds())) {
            target.getGears().clear();
            if (CollectionUtils.isNotEmpty(source.getGearIds())) {
                target.getGears().addAll(loadAllAsSet(Gear.class, IEntity.Fields.ID, source.getGearIds(), true));
            }
        }

        // Taxon Groups
        if (copyIfNull || CollectionUtils.isNotEmpty(source.getTaxonGroupIds())) {
            target.getTaxonGroups().clear();
            if (CollectionUtils.isNotEmpty(source.getTaxonGroupIds())) {
                target.getTaxonGroups().addAll(loadAllAsSet(TaxonGroup.class, IEntity.Fields.ID, source.getTaxonGroupIds(), true));
            }
        }

        // Taxon Names
        if (copyIfNull || CollectionUtils.isNotEmpty(source.getReferenceTaxonIds())) {
            target.getReferenceTaxons().clear();
            if (CollectionUtils.isNotEmpty(source.getReferenceTaxonIds())) {
                target.getReferenceTaxons().addAll(loadAllAsSet(ReferenceTaxon.class, IEntity.Fields.ID, source.getReferenceTaxonIds(), true));
            }
        }
    }

    /* -- protected methods -- */


    private int getAcquisitionLevelIdByLabel(String label) {
        Integer acquisitionLevelId = acquisitionLevelIdByLabel.get(label);
        if (acquisitionLevelId == null) {

            // Try to reload
            synchronized (this) {
                loadAcquisitionLevels();
            }

            // Retry to find it
            acquisitionLevelId = acquisitionLevelIdByLabel.get(label);
            if (acquisitionLevelId == null) {
                throw new DataIntegrityViolationException("Unknown acquisition level's label=" + label);
            }
        }

        return acquisitionLevelId;
    }

    private void loadAcquisitionLevels() {
        acquisitionLevelIdByLabel.clear();

        // Fill acquisition levels map
        List<ReferentialVO> items = referentialDao.findByFilter(AcquisitionLevel.class.getSimpleName(), new ReferentialFilterVO(), 0, 1000, null, null);
        items.forEach(item -> acquisitionLevelIdByLabel.put(item.getLabel(), item.getId()));
    }
}

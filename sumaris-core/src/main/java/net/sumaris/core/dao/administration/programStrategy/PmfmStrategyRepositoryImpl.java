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
import net.sumaris.core.dao.cache.CacheNames;
import net.sumaris.core.dao.referential.ReferentialDao;
import net.sumaris.core.dao.technical.jpa.BindableSpecification;
import net.sumaris.core.dao.technical.jpa.SumarisJpaRepositoryImpl;
import net.sumaris.core.dao.technical.model.IEntity;
import net.sumaris.core.event.config.ConfigurationEvent;
import net.sumaris.core.event.config.ConfigurationReadyEvent;
import net.sumaris.core.event.config.ConfigurationUpdatedEvent;
import net.sumaris.core.model.administration.programStrategy.AcquisitionLevel;
import net.sumaris.core.model.administration.programStrategy.PmfmStrategy;
import net.sumaris.core.model.administration.programStrategy.Strategy;
import net.sumaris.core.model.referential.gear.Gear;
import net.sumaris.core.model.referential.pmfm.*;
import net.sumaris.core.model.referential.taxon.ReferenceTaxon;
import net.sumaris.core.model.referential.taxon.TaxonGroup;
import net.sumaris.core.util.Beans;
import net.sumaris.core.vo.administration.programStrategy.PmfmStrategyVO;
import net.sumaris.core.vo.administration.programStrategy.StrategyFetchOptions;
import net.sumaris.core.vo.filter.ReferentialFilterVO;
import net.sumaris.core.vo.filter.StrategyRelatedFilterVO;
import net.sumaris.core.vo.referential.PmfmValueType;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
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
        super(PmfmStrategy.class, PmfmStrategyVO.class, entityManager);
    }

    @EventListener({ConfigurationReadyEvent.class, ConfigurationUpdatedEvent.class})
    protected void onConfigurationReady(ConfigurationEvent event) {
        this.loadAcquisitionLevels();
    }

    @Override
    @Cacheable(cacheNames = CacheNames.PMFM_BY_STRATEGY_ID)
    public List<PmfmStrategyVO> findByStrategyId(int strategyId, StrategyFetchOptions fetchOptions) {

        return findAll(
            toSpecification(StrategyRelatedFilterVO.builder().strategyId(strategyId).build()),
            Sort.by(PmfmStrategy.Fields.RANK_ORDER)
        )
            .stream()
            .map(entity -> toVO(entity, fetchOptions))
            .collect(Collectors.toList());

    }

    @Override
    public List<PmfmStrategyVO> findByProgramAndAcquisitionLevel(int programId, int acquisitionLevelId, StrategyFetchOptions fetchOptions) {

        return findAll(
            toSpecification(StrategyRelatedFilterVO.builder().programId(programId).acquisitionLevelId(acquisitionLevelId).build()),
            Sort.by(PmfmStrategy.Fields.RANK_ORDER)
        )
            .stream()
            .map(entity -> toVO(entity, fetchOptions))
            .collect(Collectors.toList());

    }

    protected Specification<PmfmStrategy> toSpecification(StrategyRelatedFilterVO filter) {
        return BindableSpecification.where(hasProgramId(filter.getProgramId()))
            .and(hasStrategyId(filter.getStrategyId()))
            .and(hasAcquisitionLevelId(filter.getAcquisitionLevelId()));
    }

    @Override
    public PmfmStrategyVO toVO(PmfmStrategy source) {
        return toVO(source, StrategyFetchOptions.builder().withPmfmStrategyInheritance(false).build());
    }

    @Override
    public PmfmStrategyVO toVO(PmfmStrategy source, StrategyFetchOptions fetchOptions) {
        if (source == null) return null;

        Pmfm pmfm = source.getPmfm();

        PmfmStrategyVO target = new PmfmStrategyVO();

        // Copy properties, from Pmfm first (if inherit enable), then from source
        if (fetchOptions.isWithPmfmStrategyInheritance() && pmfm != null) {
            Beans.copyProperties(pmfm, target);
        }
        Beans.copyProperties(source, target);

        // Strategy Id
        target.setStrategyId(source.getStrategy().getId());

        // Set some attributes from Pmfm
        if (pmfm != null) {
            target.setPmfmId(pmfm.getId());

            // Apply default values from Pmfm
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

            // Qualitative values
            if (CollectionUtils.isNotEmpty(parameter.getQualitativeValues())) {
                List<ReferentialVO> qualitativeValues = parameter.getQualitativeValues()
                        .stream()
                        .map(referentialDao::toVO)
                        .collect(Collectors.toList());
                target.setQualitativeValues(qualitativeValues);
            }
        }

        // Parameter, Matrix, Fraction, Method Ids
        if (source.getParameter() != null) {
            target.setParameterId(source.getParameter().getId());
            target.setParameter(referentialDao.toVO(source.getParameter()));
        }
        if (source.getMatrix() != null) {
            target.setMatrixId(source.getMatrix().getId());
            target.setMatrix(referentialDao.toVO(source.getMatrix()));
        }
        if (source.getFraction() != null) {
            target.setFractionId(source.getFraction().getId());
            target.setFraction(referentialDao.toVO(source.getFraction()));
        }
        if (source.getMethod() != null) {
            target.setMethodId(source.getMethod().getId());
            target.setMethod(referentialDao.toVO(source.getMethod()));
        }

        // Acquisition Level
        if (source.getAcquisitionLevel() != null) {
            target.setAcquisitionLevel(source.getAcquisitionLevel().getLabel());
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
    @Caching(
        evict = {
            @CacheEvict(cacheNames = CacheNames.PMFM_BY_STRATEGY_ID, allEntries = true) // FIXME fix error 'null' when using key='#strategyId'
        }
    )
    public List<PmfmStrategyVO> saveByStrategyId(int strategyId, List<PmfmStrategyVO> sources) {
        Preconditions.checkNotNull(sources);

        Strategy parent = getOne(Strategy.class, strategyId);

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

        return sources.isEmpty() ? null : sources;
    }

    @Override
    public void toEntity(PmfmStrategyVO source, PmfmStrategy target, boolean copyIfNull) {

        Beans.copyProperties(source, target);

        // Parent
        if (source.getStrategyId() != null) {
            target.setStrategy(load(Strategy.class, source.getStrategyId()));
        }

        // Pmfm
        Integer pmfmId = source.getPmfmId() != null ? source.getPmfmId()
                : (source.getPmfm() != null ? source.getPmfm().getId() : null);
        //if (pmfmId == null) throw new DataIntegrityViolationException("Missing pmfmId or pmfm.id in a PmfmStrategyVO");
        if (pmfmId != null) {
            target.setPmfm(load(Pmfm.class, pmfmId));
        }

        // Parameter, Matrix, Fraction, Method
        Integer parameterId = source.getParameterId() != null ? source.getParameterId()
                : (source.getParameter() != null ? source.getParameter().getId() : null);
        Integer matrixId = source.getMatrixId() != null ? source.getMatrixId()
                : (source.getMatrix() != null ? source.getMatrix().getId() : null);
        Integer fractionId = source.getFractionId() != null ? source.getFractionId()
                : (source.getFraction() != null ? source.getFraction().getId() : null);
        Integer methodId = source.getMethodId() != null ? source.getMethodId()
                : (source.getMethod() != null ? source.getMethod().getId() : null);

        if (parameterId != null) {
            target.setParameter(load(Parameter.class, parameterId));
        }
        if (matrixId != null) {
            target.setMatrix(load(Matrix.class, matrixId));
        }
        if (fractionId != null) {
            target.setFraction(load(Fraction.class, fractionId));
        }
        if (methodId != null) {
            target.setMethod(load(Method.class, methodId));
        }

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
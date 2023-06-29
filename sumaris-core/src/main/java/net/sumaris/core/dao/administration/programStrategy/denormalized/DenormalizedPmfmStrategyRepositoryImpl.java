package net.sumaris.core.dao.administration.programStrategy.denormalized;

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
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.config.CacheConfiguration;
import net.sumaris.core.dao.referential.ReferentialDao;
import net.sumaris.core.dao.referential.pmfm.PmfmRepository;
import net.sumaris.core.dao.technical.jpa.SumarisJpaRepositoryImpl;
import net.sumaris.core.model.administration.programStrategy.PmfmStrategy;
import net.sumaris.core.model.referential.gear.Gear;
import net.sumaris.core.model.referential.pmfm.Parameter;
import net.sumaris.core.model.referential.pmfm.Pmfm;
import net.sumaris.core.model.referential.pmfm.QualitativeValue;
import net.sumaris.core.model.referential.pmfm.UnitEnum;
import net.sumaris.core.util.Beans;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.administration.programStrategy.DenormalizedPmfmStrategyVO;
import net.sumaris.core.vo.administration.programStrategy.PmfmStrategyFetchOptions;
import net.sumaris.core.vo.filter.PmfmStrategyFilterVO;
import net.sumaris.core.vo.referential.PmfmValueType;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Sort;

import javax.persistence.EntityManager;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
public class DenormalizedPmfmStrategyRepositoryImpl
    extends SumarisJpaRepositoryImpl<PmfmStrategy, Integer, DenormalizedPmfmStrategyVO>
        implements DenormalizedPmfmStrategyRepository {

    @Autowired
    private ReferentialDao referentialDao;

    @Autowired
    private PmfmRepository pmfmRepository;

    @Autowired
    DenormalizedPmfmStrategyRepositoryImpl(EntityManager entityManager) {
        super(PmfmStrategy.class, DenormalizedPmfmStrategyVO.class, entityManager);
    }

    @Override
    @Cacheable(cacheNames = CacheConfiguration.Names.DENORMALIZED_PMFM_BY_FILTER)
    public List<DenormalizedPmfmStrategyVO> findByFilter(PmfmStrategyFilterVO filter, PmfmStrategyFetchOptions fetchOptions) {
        return findAll(toSpecification(filter),
                Sort.by(PmfmStrategy.Fields.STRATEGY, PmfmStrategy.Fields.ACQUISITION_LEVEL, PmfmStrategy.Fields.RANK_ORDER)
        )
                .stream()
                .filter(entity -> entity.getPmfm() != null)
                .map(entity -> toVO(entity, fetchOptions))
                //.sorted(Comparator.comparing(ps -> String.format("%s#%s#%s", ps.getStrategyId(), ps.getAcquisitionLevel(), ps.getRankOrder())))
                .collect(Collectors.toList());
    }

    @Override
    public DenormalizedPmfmStrategyVO toVO(PmfmStrategy source) {
        return toVO(source, PmfmStrategyFetchOptions.DEFAULT);
    }

    @Override
    public DenormalizedPmfmStrategyVO toVO(PmfmStrategy source, PmfmStrategyFetchOptions fetchOptions) {
        if (source == null) return null;
        return toVO(source, source.getPmfm(), fetchOptions);
    }

    @Override
    public DenormalizedPmfmStrategyVO toVO(PmfmStrategy source, Pmfm pmfm, PmfmStrategyFetchOptions fetchOptions) {
        if (source == null) return null;
        Preconditions.checkNotNull(pmfm);

        DenormalizedPmfmStrategyVO target = new DenormalizedPmfmStrategyVO();

        // Copy properties from Pmfm first (if inherit enable), then from source
        Beans.copyProperties(pmfm, target);

        // Then copy properties from source entity
        Beans.copyProperties(source, target);

        // Strategy Id
        target.setStrategyId(source.getStrategy().getId());

        // ID = the Pmfm id
        target.setId(pmfm.getId());

        // Fraction, Matrix, method
        if (pmfm.getFraction() != null) target.setFractionId(pmfm.getFraction().getId());
        if (pmfm.getMatrix() != null) target.setMatrixId(pmfm.getMatrix().getId());
        if (pmfm.getMethod() != null) {
            target.setMethodId(pmfm.getMethod().getId());
            target.setIsComputed(pmfm.getMethod().getIsCalculated());
            target.setIsEstimated(pmfm.getMethod().getIsEstimated());
        }

        // Apply default values from Pmfm (only if NOT already defined)
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

        // Complete name
        if (fetchOptions.isWithCompleteName()) {
            String completeName = pmfmRepository.computeCompleteName(pmfm.getId());
            target.setCompleteName(completeName);
        }

        // Value Type
        PmfmValueType type = PmfmValueType.fromPmfm(pmfm);
        target.setType(type.name().toLowerCase());

        // Unit symbol
        if (pmfm.getUnit() != null && !Objects.equals(pmfm.getUnit().getId(), UnitEnum.NONE.getId())) {
            target.setUnitLabel(pmfm.getUnit().getLabel());
        }

        // Label
        if (StringUtils.isBlank(target.getLabel())) {
            target.setLabel(parameter.getLabel() + (StringUtils.isNotBlank(target.getUnitLabel()) ? "_" + target.getUnitLabel() : ""));
        }

        // Qualitative values (from Pmfm if any, or from Parameter)
        if (type == PmfmValueType.QUALITATIVE_VALUE) {
            Collection<QualitativeValue> qualitativeValues = CollectionUtils.isNotEmpty(pmfm.getQualitativeValues()) ?
                pmfm.getQualitativeValues() : parameter.getQualitativeValues();
            if (CollectionUtils.isNotEmpty(qualitativeValues)) {
                target.setQualitativeValues(qualitativeValues
                    .stream()
                    .map(referentialDao::toVO)
                    .collect(Collectors.toList()));
            }
            else {
                log.warn("Missing qualitative values, in PMFM #{}", pmfm.getId());
            }
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

}

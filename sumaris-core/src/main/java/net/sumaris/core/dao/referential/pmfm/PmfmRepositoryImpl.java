package net.sumaris.core.dao.referential.pmfm;

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

import net.sumaris.core.dao.cache.CacheNames;
import net.sumaris.core.dao.referential.ReferentialDao;
import net.sumaris.core.dao.referential.ReferentialRepositoryImpl;
import net.sumaris.core.dao.technical.Daos;
import net.sumaris.core.model.referential.pmfm.*;
import net.sumaris.core.util.Beans;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.filter.IReferentialFilter;
import net.sumaris.core.vo.filter.ReferentialFilterVO;
import net.sumaris.core.vo.referential.PmfmVO;
import net.sumaris.core.vo.referential.PmfmValueType;
import net.sumaris.core.vo.referential.ReferentialFetchOptions;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;

import javax.persistence.EntityManager;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author peck7 on 19/08/2020.
 */
public class PmfmRepositoryImpl
    extends ReferentialRepositoryImpl<Pmfm, PmfmVO, IReferentialFilter, ReferentialFetchOptions>
    implements PmfmSpecifications {

    @Autowired
    private ReferentialDao referentialDao;

    public PmfmRepositoryImpl(EntityManager entityManager) {
        super(Pmfm.class, PmfmVO.class, entityManager);
    }

    @Override
    @Cacheable(cacheNames = CacheNames.PMFM_BY_ID, unless = "#result == null")
    public PmfmVO get(int id) {
        return super.get(id);
    }

    @Override
    protected void toVO(Pmfm source, PmfmVO target, ReferentialFetchOptions fetchOptions, boolean copyIfNull) {
        super.toVO(source, target, fetchOptions, copyIfNull);

        // Parameter name
        Parameter parameter = source.getParameter();
        target.setName(parameter.getName());

        // Type
        PmfmValueType type = PmfmValueType.fromPmfm(source);
        target.setType(type.name().toLowerCase());

        // Parameter
        if (source.getParameter() != null) {
            target.setParameterId(source.getParameter().getId());
        }

        // Matrix
        if (source.getMatrix() != null) {
            target.setMatrixId(source.getMatrix().getId());
        }

        // Fraction
        if (source.getFraction() != null) {
            target.setFractionId(source.getFraction().getId());
        }

        // Method: copy isEstimated, isCalculated
        Method method = source.getMethod();
        if (method != null) {
            target.setMethodId(method.getId());
            target.setIsCalculated(method.getIsCalculated());
            target.setIsEstimated(method.getIsEstimated());
        }

        // Unit symbol
        Unit unit = source.getUnit();
        if (unit != null && unit.getId() != null) {
            target.setUnitId(unit.getId());
            if (UnitEnum.NONE.getId() != unit.getId()) {
                target.setUnitLabel(unit.getLabel());
            }
        }

        // Qualitative values: from pmfm first, or (if empty) from parameter
        if (CollectionUtils.isNotEmpty(source.getQualitativeValues())) {
            List<ReferentialVO> qualitativeValues = source.getQualitativeValues()
                .stream()
                .map(referentialDao::toVO)
                .collect(Collectors.toList());
            target.setQualitativeValues(qualitativeValues);
        } else if ((fetchOptions == null || fetchOptions.isWithInheritance()) // load parameter qv list is fetch option allows it
            && CollectionUtils.isNotEmpty(parameter.getQualitativeValues())) {
            List<ReferentialVO> qualitativeValues = parameter.getQualitativeValues()
                .stream()
                .map(referentialDao::toVO)
                .collect(Collectors.toList());
            target.setQualitativeValues(qualitativeValues);
        }

        // EntityName (as metadata - see ReferentialVO)
        target.setEntityName(Pmfm.class.getSimpleName());

        // Level Id (see ReferentialVO)
        if (source.getParameter() != null) {
            target.setLevelId(source.getParameter().getId());
        }
    }

    @Override
    @Caching(
        evict = {
            @CacheEvict(cacheNames = CacheNames.PMFM_BY_ID, key = "#vo.id", condition = "#vo != null && #vo.id != null"),
            @CacheEvict(cacheNames = CacheNames.PMFM_HAS_PREFIX, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.PMFM_HAS_SUFFIX, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.PMFM_HAS_MATRIX, allEntries = true)
        }
    )
    public PmfmVO save(PmfmVO vo) {
        PmfmVO savedVO = super.save(vo);
        // Force reload
        return get(savedVO.getId(), ReferentialFetchOptions.builder().withInheritance(false).build());
    }

    @Override
    public void toEntity(PmfmVO source, Pmfm target, boolean copyIfNull) {
        super.toEntity(source, target, copyIfNull);

        // Link to other entities
        Daos.setEntityProperties(getEntityManager(), target,
            Pmfm.Fields.PARAMETER, Parameter.class, source.getParameterId(),
            Pmfm.Fields.MATRIX, Matrix.class, source.getMatrixId(),
            Pmfm.Fields.FRACTION, Fraction.class, source.getFractionId(),
            Pmfm.Fields.METHOD, Method.class, source.getMethodId(),
            Pmfm.Fields.UNIT, Unit.class, source.getUnitId());

        // Remember existing QV
        Set<QualitativeValue> entities = Beans.getSet(target.getQualitativeValues());
        Map<Integer, QualitativeValue> entitiesToRemove = Beans.splitByProperty(entities, QualitativeValue.Fields.ID);

        Beans.getStream(source.getQualitativeValues())
            .map(ReferentialVO::getId)
            .filter(Objects::nonNull)
            .forEach(qvId -> {
                if (entitiesToRemove.remove(qvId) == null) {
                    entities.add(load(QualitativeValue.class, qvId));
                }
            });

        // Remove orphan
        if (!entitiesToRemove.isEmpty()) {
            entities.removeAll(entitiesToRemove.values());
        }

        target.setQualitativeValues(entities);
    }

    @Override
    @Cacheable(cacheNames = CacheNames.PMFM_HAS_PREFIX)
    public boolean hasLabelPrefix(int pmfmId, String... labelPrefixes) {
        return Optional.of(getOne(pmfmId))
            .map(Pmfm::getLabel)
            .map(StringUtils.startsWithFunction(labelPrefixes))
            .orElse(false);
    }

    @Override
    @Cacheable(cacheNames = CacheNames.PMFM_HAS_SUFFIX)
    public boolean hasLabelSuffix(int pmfmId, String... labelSuffixes) {
        return Optional.of(getOne(pmfmId))
            .map(Pmfm::getLabel)
            .map(StringUtils.endsWithFunction(labelSuffixes))
            .orElse(false);
    }

    @Override
    @Cacheable(cacheNames = CacheNames.PMFM_HAS_MATRIX)
    public boolean hasMatrixId(int pmfmId, int... matrixIds) {
        return Optional.of(getOne(pmfmId))
            .map(Pmfm::getMatrix)
            .map(matrix -> Arrays.binarySearch(matrixIds, matrix.getId()) != -1)
            .orElse(false);
    }

}

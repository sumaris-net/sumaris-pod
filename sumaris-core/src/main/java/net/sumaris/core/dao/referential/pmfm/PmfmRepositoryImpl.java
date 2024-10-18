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

import com.google.common.base.Joiner;
import lombok.NonNull;
import net.sumaris.core.config.CacheConfiguration;
import net.sumaris.core.dao.referential.ReferentialDao;
import net.sumaris.core.dao.referential.ReferentialRepositoryImpl;
import net.sumaris.core.dao.technical.Daos;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.dao.technical.jpa.BindableSpecification;
import net.sumaris.core.model.referential.pmfm.*;
import net.sumaris.core.util.Beans;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.filter.IReferentialFilter;
import net.sumaris.core.vo.filter.PmfmPartsVO;
import net.sumaris.core.vo.referential.PmfmFetchOptions;
import net.sumaris.core.vo.referential.PmfmVO;
import net.sumaris.core.vo.referential.PmfmValueType;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.hibernate.jpa.QueryHints;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.jpa.domain.Specification;

import javax.annotation.Nullable;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author peck7 on 19/08/2020.
 */
public class PmfmRepositoryImpl
    extends ReferentialRepositoryImpl<Integer, Pmfm, PmfmVO, IReferentialFilter, PmfmFetchOptions>
    implements PmfmSpecifications {

    @Autowired
    private ReferentialDao referentialDao;

    public PmfmRepositoryImpl(EntityManager entityManager) {
        super(Pmfm.class, PmfmVO.class, entityManager);
    }

    @Override
    @Cacheable(cacheNames = CacheConfiguration.Names.PMFM_BY_ID, key = "#id", unless = "#result == null")
    public PmfmVO get(Integer id) {
        return super.get(id);
    }

    @Override
    @Cacheable(cacheNames = CacheConfiguration.Names.PMFM, unless = "#result == null")
    public PmfmVO get(Integer id, PmfmFetchOptions fetchOptions) {
        return super.get(id, fetchOptions);
    }

    @Override
    @Cacheable(cacheNames = CacheConfiguration.Names.PMFM, unless = "#result == null")
    public PmfmVO getByLabel(String label) {
        return super.getByLabel(label);
    }

    @Override
    public Optional<PmfmVO> findVOById(Integer id) {
        // Make sure to use cached function
        return Optional.ofNullable(this.get(id));
    }

    @Override
    @Cacheable(cacheNames = CacheConfiguration.Names.PMFM_COMPLETE_NAME_BY_ID, key = "#id")
    public String computeCompleteName(int id) {
        Pmfm pmfm = getById(id);
        String parameterName = pmfm.getParameter().getName();

        String parameterSuffix = (pmfm.getUnit() != null
            // Skip NONE unit
            && !Objects.equals(pmfm.getUnit().getId(), UnitEnum.NONE.getId()))
            ? String.format(" (%s)", pmfm.getUnit().getLabel())
            : "";

        String matrixName = pmfm.getMatrix() != null
            ? pmfm.getMatrix().getName()
            : null;

        String fractionName = pmfm.getFraction() != null
            // Skip UNKNOWN and All fractions
            && !Objects.equals(pmfm.getFraction().getId(), FractionEnum.UNKNOWN.getId())
            && !Objects.equals(pmfm.getFraction().getId(), FractionEnum.ALL.getId())
                ? pmfm.getFraction().getName()
                : null;

        String methodName = pmfm.getMethod() != null
            // Skip UNKNOWN method
            && !Objects.equals(pmfm.getMethod().getId(), MethodEnum.UNKNOWN.getId())
            ? pmfm.getMethod().getName()
            : null;

        return Joiner.on(" - ").skipNulls().join(new String[]{
            parameterName + parameterSuffix,
            matrixName,
            fractionName,
            methodName
        });
    }

    public List<Integer> findIdsByParts(@NonNull PmfmPartsVO parts) {
        return streamAll(BindableSpecification.where(hasPmfmPart(parts))
                .and(inStatusIds(parts.getStatusId())))
            .map(Pmfm::getId)
            .collect(Collectors.toList());
    }

    public Stream<Pmfm> streamAllByParts(@NonNull PmfmPartsVO parts) {
        return streamAll(BindableSpecification.where(hasPmfmPart(parts))
                .and(inStatusIds(parts.getStatusId())));
    }

    @Override
    protected void toVO(Pmfm source, PmfmVO target, PmfmFetchOptions fetchOptions, boolean copyIfNull) {
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
            if (!Objects.equals(UnitEnum.NONE.getId(), unit.getId())) {
                target.setUnitLabel(unit.getLabel());
            }
        }

        // Qualitative values: from pmfm first, or (if empty) from parameter
        if (fetchOptions == null || fetchOptions.isWithQualitativeValue()) {
            // Fetch QV (only if need - to avoid a select on QUALITATIVE_VALUE)
            if (type == PmfmValueType.QUALITATIVE_VALUE) {
                if (CollectionUtils.isNotEmpty(source.getQualitativeValues())) {
                    List<ReferentialVO> qualitativeValues = source.getQualitativeValues()
                        .stream()
                        .map(referentialDao::toVO)
                        .toList();
                    target.setQualitativeValues(qualitativeValues);
                } else if ((fetchOptions == null || fetchOptions.isWithInheritance()) // load parameter qv list is fetch option allows it
                    && CollectionUtils.isNotEmpty(parameter.getQualitativeValues())) {
                    List<ReferentialVO> qualitativeValues = parameter.getQualitativeValues()
                        .stream()
                        .map(referentialDao::toVO)
                        .toList();
                    target.setQualitativeValues(qualitativeValues);
                }
            }
            else {
                target.setQualitativeValues(null);
            }
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
                @CacheEvict(cacheNames = CacheConfiguration.Names.PMFM_BY_ID, key = "#source.id", condition = "#source != null && #source.id != null"),
                @CacheEvict(cacheNames = CacheConfiguration.Names.PMFM, allEntries = true, condition = "#source != null && #source.id != null"),
                @CacheEvict(cacheNames = CacheConfiguration.Names.PMFM_COMPLETE_NAME_BY_ID, key = "#source.id", condition = "#source != null && #source.id != null"),
                @CacheEvict(cacheNames = CacheConfiguration.Names.PMFM_HAS_PREFIX, allEntries = true),
                @CacheEvict(cacheNames = CacheConfiguration.Names.PMFM_HAS_SUFFIX, allEntries = true),
                @CacheEvict(cacheNames = CacheConfiguration.Names.PMFM_HAS_MATRIX, allEntries = true),
                @CacheEvict(cacheNames = CacheConfiguration.Names.PMFM_HAS_PARAMETER_GROUP, allEntries = true)
        }
    )
    public PmfmVO save(PmfmVO source) {
        PmfmVO savedVO = super.save(source);
        // Force reload
        return get(savedVO.getId(), PmfmFetchOptions.builder().withInheritance(false).build());
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
                    entities.add(getReference(QualitativeValue.class, qvId));
                }
            });

        // Remove orphan
        if (!entitiesToRemove.isEmpty()) {
            entities.removeAll(entitiesToRemove.values());
        }

        target.setQualitativeValues(entities);
    }

    @Override
    protected Specification<Pmfm> toSpecification(@NonNull IReferentialFilter filter, PmfmFetchOptions fetchOptions) {
        return super.toSpecification(filter, fetchOptions);
    }

    @Override
    @Cacheable(cacheNames = CacheConfiguration.Names.PMFM_HAS_PREFIX)
    public boolean hasLabelPrefix(int pmfmId, String... labelPrefixes) {
        return Optional.of(getById(pmfmId))
            .map(Pmfm::getLabel)
            .map(StringUtils.startsWithFunction(labelPrefixes))
            .orElse(false);
    }

    @Override
    @Cacheable(cacheNames = CacheConfiguration.Names.PMFM_HAS_SUFFIX)
    public boolean hasLabelSuffix(int pmfmId, String... labelSuffixes) {
        return Optional.of(getById(pmfmId))
            .map(Pmfm::getLabel)
            .map(StringUtils.endsWithFunction(labelSuffixes))
            .orElse(false);
    }

    @Override
    @Cacheable(cacheNames = CacheConfiguration.Names.PMFM_HAS_MATRIX)
    public boolean hasMatrixId(int pmfmId, int... matrixIds) {
        return Optional.of(getById(pmfmId))
            .map(Pmfm::getMatrix)
            .map(matrix -> ArrayUtils.contains(matrixIds, matrix.getId()))
            .orElse(false);
    }

    @Override
    @Cacheable(cacheNames = CacheConfiguration.Names.PMFM_HAS_PARAMETER_GROUP)
    public boolean hasParameterGroupId(int pmfmId, int... parameterGroupIds) {
        return Optional.of(getById(pmfmId))
            .map(Pmfm::getParameter)
            .map(Parameter::getParameterGroup)
            .map(parameterGroup ->  ArrayUtils.contains(parameterGroupIds, parameterGroup.getId()))
            .orElse(false);
    }

    @Override
    protected void configureQuery(TypedQuery<Pmfm> query, Page page, @Nullable PmfmFetchOptions fetchOptions) {
        super.configureQuery(query, page, fetchOptions);

        // Fetch all pmfms (if no page)
        if (page == null && fetchOptions != null && fetchOptions.isWithQualitativeValue()) {
            query.setHint(QueryHints.HINT_LOADGRAPH, getEntityManager().getEntityGraph(Pmfm.GRAPH_QUALITATIVE_VALUES));
        }
    }
}

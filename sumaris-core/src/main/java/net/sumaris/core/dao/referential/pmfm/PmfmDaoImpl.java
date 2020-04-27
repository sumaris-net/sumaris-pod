/*
 * #%L
 * SUMARiS
 * %%
 * Copyright (C) 2019 SUMARiS Consortium
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

package net.sumaris.core.dao.referential.pmfm;

import com.google.common.base.Preconditions;
import net.sumaris.core.dao.referential.BaseReferentialDaoImpl;
import net.sumaris.core.dao.referential.ReferentialDao;
import net.sumaris.core.dao.technical.Daos;
import net.sumaris.core.exception.DataNotFoundException;
import net.sumaris.core.model.referential.pmfm.*;
import net.sumaris.core.util.Beans;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.referential.PmfmVO;
import net.sumaris.core.vo.referential.PmfmValueType;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Root;
import java.util.*;
import java.util.stream.Collectors;

@Repository("pmfmDao")
public class PmfmDaoImpl extends BaseReferentialDaoImpl<Pmfm, PmfmVO> implements PmfmDao {

    /** Logger. */
    private static final Logger log =
            LoggerFactory.getLogger(PmfmDaoImpl.class);

    @Autowired
    private ReferentialDao referentialDao;

    public int unitIdNone;

    @PostConstruct
    protected void init() {
        this.unitIdNone = config.getUnitIdNone();
    }

    @Override
    protected Class getDomainClass() {
        return Pmfm.class;
    }

    @Override
    protected Pmfm createEntity() {
        return new Pmfm();
    }

    @Override
    public Optional<PmfmVO> findByLabel(final String label) {
        try {
            return Optional.of(getByLabel(label));
        }
        catch (DataNotFoundException e) {
            return Optional.empty();
        }
    }

    @Override
    public PmfmVO getByLabel(final String label) {
        Preconditions.checkNotNull(label);
        Preconditions.checkArgument(label.trim().length() > 0);

        CriteriaBuilder builder = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<Pmfm> query = builder.createQuery(Pmfm.class);
        Root<Pmfm> root = query.from(Pmfm.class);

        ParameterExpression<String> labelParam = builder.parameter(String.class);

        query.select(root)
                .where(builder.equal(root.get(Pmfm.Fields.LABEL), labelParam));

        TypedQuery<Pmfm> q = getEntityManager().createQuery(query)
                .setParameter(labelParam, label.trim());
        try {
            return toVO(q.getSingleResult());
        }
        catch (NoResultException | EmptyResultDataAccessException e) {
            throw new DataNotFoundException(String.format("Pmfm with label '%s' not found.", label));
        }
    }

    @Override
    public PmfmVO toVO(Pmfm source) {
        if (source == null) return null;

        PmfmVO target = new PmfmVO();

        Beans.copyProperties(source, target);

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
            if (unit.getId() != unitIdNone) {
                target.setUnitLabel(unit.getLabel());
            }
        }

        // Qualitative values: from pmfm first, or (if empty) from parameter
        if (CollectionUtils.isNotEmpty(source.getQualitativeValues())) {
            List<ReferentialVO> qualitativeValues = source.getQualitativeValues()
                    .stream()
                    .map(referentialDao::toReferentialVO)
                    .collect(Collectors.toList());
            target.setQualitativeValues(qualitativeValues);
        }
        else if (CollectionUtils.isNotEmpty(parameter.getQualitativeValues())) {
            List<ReferentialVO> qualitativeValues = parameter.getQualitativeValues()
                    .stream()
                    .map(referentialDao::toReferentialVO)
                    .collect(Collectors.toList());
            target.setQualitativeValues(qualitativeValues);
        }

        // Status
        if (source.getStatus() != null) {
            target.setStatusId(source.getStatus().getId());
        }

        // EntityName (as metadata - see ReferentialVO)
        target.setEntityName(Pmfm.class.getSimpleName());

        // Level Id (see ReferentialVO)
        if (source.getParameter() != null) {
            target.setLevelId(source.getParameter().getId());
        }

        return target;
    }

    @Override
    public PmfmVO save(PmfmVO vo) {
        PmfmVO savedVO = super.save(vo);

        return get(savedVO.getId());
    }

    @Override
    public boolean hasLabelPrefix(int pmfmId, String... labelPrefixes) {
        return Optional.ofNullable(getOne(pmfmId))
                .map(Pmfm::getLabel)
                .map(StringUtils.startsWithFunction(labelPrefixes))
                .orElse(false);
    }

    @Override
    public boolean hasLabelSuffix(int pmfmId, String... labelSuffixes) {
        return Optional.ofNullable(getOne(pmfmId))
                .map(Pmfm::getLabel)
                .map(StringUtils.endsWithFunction(labelSuffixes))
                .orElse(false);
    }

    /* -- protected methods -- */

    protected void toEntity(PmfmVO source, Pmfm target, boolean copyIfNull) {
        super.toEntity(source, target, copyIfNull);

        // Link to other entities
        Daos.setEntityProperties(entityManager, target,
                Pmfm.Fields.PARAMETER,  Parameter.class,    source.getParameterId(),
                Pmfm.Fields.MATRIX,     Matrix.class,       source.getMatrixId(),
                Pmfm.Fields.FRACTION,   Fraction.class,     source.getFractionId(),
                Pmfm.Fields.METHOD,     Method.class,       source.getMethodId(),
                Pmfm.Fields.UNIT,       Unit.class,         source.getUnitId());

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
}

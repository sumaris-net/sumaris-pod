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

import com.google.common.collect.Lists;
import net.sumaris.core.dao.referential.BaseReferentialDaoImpl;
import net.sumaris.core.dao.referential.ReferentialDao;
import net.sumaris.core.dao.technical.Daos;
import net.sumaris.core.model.referential.IWithStatusEntity;
import net.sumaris.core.model.referential.Status;
import net.sumaris.core.model.referential.pmfm.Parameter;
import net.sumaris.core.model.referential.pmfm.Pmfm;
import net.sumaris.core.model.referential.pmfm.QualitativeValue;
import net.sumaris.core.util.Beans;
import net.sumaris.core.vo.referential.ParameterVO;
import net.sumaris.core.vo.referential.ParameterValueType;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Root;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository("parameterDao")
public class ParameterDaoImpl extends BaseReferentialDaoImpl<Parameter, ParameterVO> implements ParameterDao {

    /** Logger. */
    private static final Logger log =
            LoggerFactory.getLogger(ParameterDaoImpl.class);

    @Autowired
    private ReferentialDao referentialDao;

    @Override
    protected Class<Parameter> getDomainClass() {
        return Parameter.class;
    }

    @Override
    protected Parameter createEntity() {
        return new Parameter();
    }

    @Override
    public ParameterVO getByLabel(final String label) {
        CriteriaBuilder builder = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<Parameter> query = builder.createQuery(Parameter.class);
        Root<Parameter> root = query.from(Parameter.class);

        ParameterExpression<String> labelParam = builder.parameter(String.class);

        query.select(root)
                .where(builder.equal(root.get(Parameter.Fields.LABEL), labelParam));

        TypedQuery<Parameter> q = getEntityManager().createQuery(query)
                .setParameter(labelParam, label);
        return toVO(q.getSingleResult());
    }

    @Override
    public ParameterVO toVO(Parameter source) {
        if (source == null) return null;

        ParameterVO target = new ParameterVO();

        Beans.copyProperties(source, target);

        // Type
        ParameterValueType type = ParameterValueType.fromParameter(source);
        target.setType(type.name().toLowerCase());

        // Qualitative values: from pmfm first, or (if empty) from parameter
        if (CollectionUtils.isNotEmpty(source.getQualitativeValues())) {
            List<ReferentialVO> qualitativeValues = source.getQualitativeValues()
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
        target.setEntityName(Parameter.class.getSimpleName());

        return target;
    }

    @Override
    public ParameterVO save(ParameterVO vo) {
        ParameterVO savedVO = super.save(vo);

        List<ReferentialVO> savedQv = saveQualitativeValues(savedVO.getId(), vo.getQualitativeValues());
        savedVO.setQualitativeValues(savedQv);

        return savedVO;
    }

    /* -- protected methods -- */

    @Override
    protected void toEntity(ParameterVO source, Parameter target, boolean copyIfNull) {
        super.toEntity(source, target, copyIfNull);

        // Type
        if (copyIfNull || source.getType() != null) {
            ParameterValueType type = (source.getType() != null)
                    ? ParameterValueType.fromString(source.getType())
                    : ParameterValueType.DOUBLE; // Default value
            switch (type) {

                case STRING:
                    target.setIsAlphanumeric(true);
                    target.setIsBoolean(false);
                    target.setIsDate(false);
                    target.setIsQualitative(false);
                case BOOLEAN:
                    target.setIsAlphanumeric(false);
                    target.setIsBoolean(true);
                    target.setIsDate(false);
                    target.setIsQualitative(false);
                case DATE:
                    target.setIsAlphanumeric(false);
                    target.setIsBoolean(false);
                    target.setIsDate(true);
                    target.setIsQualitative(false);
                case QUALITATIVE_VALUE:
                    target.setIsAlphanumeric(false);
                    target.setIsBoolean(false);
                    target.setIsDate(false);
                    target.setIsQualitative(true);
                case DOUBLE:
                default:
                    target.setIsAlphanumeric(false);
                    target.setIsBoolean(false);
                    target.setIsDate(false);
                    target.setIsQualitative(false);
            }
        }
    }

    protected List<ReferentialVO> saveQualitativeValues(int parameterId, List<ReferentialVO> sources) {

        Parameter parent = getOne(parameterId);
        Date newUpdateDate = parent.getUpdateDate();

        // Remember existing QV
        List<QualitativeValue> entities = Beans.getList(parent.getQualitativeValues());
        Map<Integer, QualitativeValue> entitiesToRemove = Beans.splitByProperty(entities, QualitativeValue.Fields.ID);

        Beans.getStream(sources).forEach(qvSource -> {
                QualitativeValue qvTarget = null;
                if (qvSource.getId() != null) {
                    qvTarget = entitiesToRemove.remove(qvSource.getId());
                }
                boolean isNew = qvTarget == null;
                if (qvTarget == null) {
                    qvTarget = new QualitativeValue();
                }

                // Fill VO properties to entity
                Beans.copyProperties(qvSource, qvTarget);
                Daos.setEntityProperty(entityManager, qvTarget, Pmfm.Fields.STATUS,
                        Status.class, qvSource.getStatusId());
                qvTarget.setParameter(parent);
                qvTarget.setUpdateDate(newUpdateDate);

                // Save entity
                if (isNew) {
                    parent.getQualitativeValues().add(qvTarget);
                    qvTarget.setCreationDate(newUpdateDate);
                    this.entityManager.persist(qvTarget);
                }
                else {
                    this.entityManager.merge(qvTarget);
                }

                qvSource.setId(qvTarget.getId());
                qvSource.setCreationDate(qvTarget.getCreationDate());
                qvSource.setUpdateDate(qvTarget.getUpdateDate());
            });

        // Remove orphan
        if (!entitiesToRemove.isEmpty()) {
            entities.removeAll(entitiesToRemove.values());
            entitiesToRemove.values().forEach(entityManager::remove);
        }

        parent.setQualitativeValues(entities);
        this.entityManager.merge(parent);

        return sources;
    }
}

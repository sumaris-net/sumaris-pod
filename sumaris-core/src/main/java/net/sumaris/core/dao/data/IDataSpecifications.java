package net.sumaris.core.dao.data;

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

import net.sumaris.core.dao.referential.IEntitySpecifications;
import net.sumaris.core.dao.technical.jpa.BindableSpecification;
import net.sumaris.core.model.IEntity;
import net.sumaris.core.model.data.DataQualityStatusEnum;
import net.sumaris.core.model.data.IDataEntity;
import net.sumaris.core.model.referential.QualityFlag;
import net.sumaris.core.model.referential.QualityFlagEnum;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.NoRepositoryBean;

import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Predicate;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;

/**
 * @author peck7 on 28/08/2020.
 */
@NoRepositoryBean
public interface IDataSpecifications<ID extends Serializable,
    E extends IDataEntity<ID>>
    extends IEntitySpecifications<ID, E> {

    String RECORDER_DEPARTMENT_ID_PARAM = "recorderDepartmentId";
    String RECORDER_DEPARTMENT_IDS_PARAM = "recorderDepartmentIds";
    String RECORDER_PERSON_IDS_PARAM = "recorderPersonIds";
    String QUALITY_FLAG_ID_PARAM = "qualityFlagId";

    default Specification<E> hasRecorderDepartmentId(Integer recorderDepartmentId) {
        if (recorderDepartmentId == null) return null;
        return BindableSpecification.where((root, query, cb) -> {
            query.distinct(true); // Set distinct here because hasRecorderDepartmentId is always used (usually ...)
            ParameterExpression<Integer> param = cb.parameter(Integer.class, RECORDER_DEPARTMENT_ID_PARAM);
            return cb.equal(root.get(E.Fields.RECORDER_DEPARTMENT).get(IEntity.Fields.ID), param);
        }).addBind(RECORDER_DEPARTMENT_ID_PARAM, recorderDepartmentId);
    }

    default Specification<E> hasRecorderDepartmentIds(Integer... recorderDepartmentIds) {
        if (ArrayUtils.isEmpty(recorderDepartmentIds)) return null;

        return BindableSpecification.where((root, query, cb) -> {
                    ParameterExpression<Collection> parameter = cb.parameter(Collection.class, RECORDER_DEPARTMENT_IDS_PARAM);
                    return cb.in(root.get(E.Fields.RECORDER_DEPARTMENT).get(IEntity.Fields.ID)).value(parameter);
                })
                .addBind(RECORDER_DEPARTMENT_IDS_PARAM, Arrays.asList(recorderDepartmentIds));
    }

    default Specification<E> hasRecorderPersonIds(Integer... recorderPersonIds) {
        if (ArrayUtils.isEmpty(recorderPersonIds)) return null;

        return BindableSpecification.where((root, query, cb) -> {
                    ParameterExpression<Collection> parameter = cb.parameter(Collection.class, RECORDER_PERSON_IDS_PARAM);
                    return cb.in(root.get(E.Fields.RECORDER_PERSON).get(IEntity.Fields.ID)).value(parameter);
                })
                .addBind(RECORDER_PERSON_IDS_PARAM, Arrays.asList(recorderPersonIds));
    }
    /**
     * Control date is null
     * @return
     */
    default Specification<E> isNotControlled() {
        return (root, query, cb) ->
            cb.isNull(root.get(IDataEntity.Fields.CONTROL_DATE));
    }

    /**
     * Control date is not null
     * @return
     */
    default Specification<E> isControlled() {
        return (root, query, cb) ->
            cb.and(
                // Control date not null
                cb.isNotNull(root.get(IDataEntity.Fields.CONTROL_DATE)),
                // Not qualified
                cb.or(
                    cb.isNull(root.get(IDataEntity.Fields.QUALIFICATION_DATE)),
                    cb.equal(cb.coalesce(root.get(IDataEntity.Fields.QUALITY_FLAG).get(QualityFlag.Fields.ID), QualityFlagEnum.NOT_QUALIFIED.getId()), QualityFlagEnum.NOT_QUALIFIED.getId())
                )
            );
    }

    default Specification<E> isValidated() {
        return null;
    }

    default Specification<E> isQualified() {
        return (root, query, cb) -> cb.and(
            // Qualification date not null
            cb.isNotNull(root.get(IDataEntity.Fields.QUALIFICATION_DATE)),
            // Quality flag != 0
            cb.notEqual(cb.coalesce(root.get(IDataEntity.Fields.QUALITY_FLAG).get(QualityFlag.Fields.ID), QualityFlagEnum.NOT_QUALIFIED.getId()), QualityFlagEnum.NOT_QUALIFIED.getId())
        );
    }

    default Specification<E> withDataQualityStatus(DataQualityStatusEnum status) {
        if (status != null) {
            return switch (status) {
                case MODIFIED -> isNotControlled();
                case CONTROLLED -> isControlled();
                case VALIDATED -> null; // No validation date here (only in root entity)
                case QUALIFIED -> isQualified();
            };
        }
        return null;
    }

    default Specification<E> inDataQualityStatus(DataQualityStatusEnum... dataQualityStatus) {
        if (ArrayUtils.isEmpty(dataQualityStatus)) return null;
        if (dataQualityStatus.length == 1) {
            return withDataQualityStatus(dataQualityStatus[0]);
        }

        return (root, query, cb) -> cb.or(
            Arrays.stream(dataQualityStatus)
                .map(this::withDataQualityStatus)
                .filter(Objects::nonNull)
                .map(s -> s.toPredicate(root, query, cb))
                .toArray(Predicate[]::new)
        );
    }


    default Specification<E> inQualityFlagIds(Integer... qualityFlagIds) {
        if (ArrayUtils.isEmpty(qualityFlagIds)) return null;

        if (qualityFlagIds.length == 1) {
            return withQualityFlagId(qualityFlagIds[0]);
        }

        return BindableSpecification.where((root, query, cb) -> {
                ParameterExpression<Collection> param = cb.parameter(Collection.class, QUALITY_FLAG_ID_PARAM);
                return cb.in(root.get(IDataEntity.Fields.QUALITY_FLAG).get(QualityFlag.Fields.ID)).value(param);
            })
            .addBind(QUALITY_FLAG_ID_PARAM, Arrays.asList(qualityFlagIds));
    }

    default Specification<E> withQualityFlagId(Integer qualityFlagId) {
        if (qualityFlagId == null) return null;

        return BindableSpecification.where((root, query, cb) -> {
                ParameterExpression<Integer> param = cb.parameter(Integer.class, QUALITY_FLAG_ID_PARAM);
                return cb.equal(root.get(IDataEntity.Fields.QUALITY_FLAG).get(QualityFlag.Fields.ID), param);
            })
            .addBind(QUALITY_FLAG_ID_PARAM, qualityFlagId);

    }
}

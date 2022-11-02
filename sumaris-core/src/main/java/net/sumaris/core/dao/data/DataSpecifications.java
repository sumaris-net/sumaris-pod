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

import net.sumaris.core.dao.technical.jpa.BindableSpecification;
import net.sumaris.core.model.IEntity;
import net.sumaris.core.model.data.DataQualityStatusEnum;
import net.sumaris.core.model.data.IDataEntity;
import net.sumaris.core.model.data.IWithDataQualityEntity;
import net.sumaris.core.model.referential.QualityFlag;
import net.sumaris.core.model.referential.QualityFlagEnum;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Predicate;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;

/**
 * @author peck7 on 28/08/2020.
 */
public interface DataSpecifications<E extends IDataEntity<? extends Serializable>> {

    String ID_PARAM = "id";
    String EXCLUDED_IDS_PARAM = "excludedIds";
    String RECORDER_DEPARTMENT_ID_PARAM = "recorderDepartmentId";

    default Specification<E> excludedIds(Integer[] excludedIds) {
        if (ArrayUtils.isEmpty(excludedIds)) return null;
        return BindableSpecification.where((root, query, cb) -> {
            ParameterExpression<Collection> param = cb.parameter(Collection.class, EXCLUDED_IDS_PARAM);
            return cb.not(
                cb.in(root.get(E.Fields.ID)).value(param)
            );
        }).addBind(EXCLUDED_IDS_PARAM, Arrays.asList(excludedIds));
    }

    default Specification<E> id(Integer id) {
        if (id == null) return null;
        return BindableSpecification.where((root, query, cb) -> {
            ParameterExpression<Integer> param = cb.parameter(Integer.class, ID_PARAM);
            return cb.equal(root.get(E.Fields.ID), param);
        }).addBind(ID_PARAM, id);
    }

    default Specification<E> hasRecorderDepartmentId(Integer recorderDepartmentId) {
        if (recorderDepartmentId == null) return null;
        return BindableSpecification.where((root, query, cb) -> {
            query.distinct(true); // Set distinct here because hasRecorderDepartmentId is always used (usually ...)
            ParameterExpression<Integer> param = cb.parameter(Integer.class, RECORDER_DEPARTMENT_ID_PARAM);
            return cb.equal(root.get(E.Fields.RECORDER_DEPARTMENT).get(IEntity.Fields.ID), param);
        }).addBind(RECORDER_DEPARTMENT_ID_PARAM, recorderDepartmentId);
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
            cb.isNotNull(root.get(IDataEntity.Fields.CONTROL_DATE));
    }

    default Specification<E> isValidated() {
        return (root, query, cb) ->
            // Validation date not null
            cb.isNotNull(root.get(IWithDataQualityEntity.Fields.VALIDATION_DATE));
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
            switch (status) {
                case MODIFIED:
                    return isNotControlled();
                case CONTROLLED:
                    return isControlled();
                case VALIDATED:
                    return isValidated();
                case QUALIFIED:
                    return isQualified();
            }
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
                .toArray(Predicate[]::new)
        );
    }
}

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
import net.sumaris.core.dao.technical.model.IEntity;
import net.sumaris.core.model.data.DataQualityStatusEnum;
import net.sumaris.core.model.data.IRootDataEntity;
import net.sumaris.core.model.referential.IItemReferentialEntity;
import net.sumaris.core.model.referential.QualityFlag;
import net.sumaris.core.model.referential.QualityFlagEnum;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Predicate;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;

/**
 * @author peck7 on 28/08/2020.
 */
public interface RootDataSpecifications<E extends IRootDataEntity<? extends Serializable>> extends DataSpecifications<E> {


    String RECORDER_PERSON_ID_PARAM = "recorderPersonId";
    String PROGRAM_LABEL_PARAM = "programLabel";

    default Specification<E> hasRecorderPersonId(Integer recorderPersonId) {
        BindableSpecification<E> specification = BindableSpecification.where((root, query, criteriaBuilder) -> {
            ParameterExpression<Integer> param = criteriaBuilder.parameter(Integer.class, RECORDER_PERSON_ID_PARAM);
            return criteriaBuilder.or(
                criteriaBuilder.isNull(param),
                criteriaBuilder.equal(root.get(E.Fields.RECORDER_PERSON).get(IEntity.Fields.ID), param)
            );
        });
        specification.addBind(RECORDER_PERSON_ID_PARAM, recorderPersonId);
        return specification;
    }

    default Specification<E> hasProgramLabel(String programLabel) {
        BindableSpecification<E> specification = BindableSpecification.where((root, query, criteriaBuilder) -> {
            ParameterExpression<String> param = criteriaBuilder.parameter(String.class, PROGRAM_LABEL_PARAM);
            return criteriaBuilder.or(
                criteriaBuilder.isNull(param),
                criteriaBuilder.equal(root.get(E.Fields.PROGRAM).get(IItemReferentialEntity.Fields.LABEL), param)
            );
        });
        specification.addBind(PROGRAM_LABEL_PARAM, programLabel);
        return specification;
    }

    default Specification<E> inDataQualityStatus(DataQualityStatusEnum... dataQualityStatus) {
        if (ArrayUtils.isEmpty(dataQualityStatus)) return null;
        if (dataQualityStatus.length == 1) {
            return withDataQualityStatus(dataQualityStatus[0]);
        }

        return (root, query, criteriaBuilder) -> criteriaBuilder.or(
                Arrays.stream(dataQualityStatus)
                    .map(this::withDataQualityStatus)
                    .filter(Objects::nonNull)
                    .toArray(Predicate[]::new)
            );
    }

    default Specification<E> withDataQualityStatus(DataQualityStatusEnum status) {
        if (status != null) {
            switch (status) {
                case DRAFT:
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

    default Specification<E> isValidated() {
        return (root, query, criteriaBuilder) ->
            // Validation date not null
            criteriaBuilder.isNotNull(root.get(IRootDataEntity.Fields.VALIDATION_DATE));
    }

    default Specification<E> isQualified() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.and(
            // Qualification date not null
            criteriaBuilder.isNotNull(root.get(IRootDataEntity.Fields.QUALIFICATION_DATE)),
            // Quality flag != 0
            criteriaBuilder.notEqual(criteriaBuilder.coalesce(root.get(IRootDataEntity.Fields.QUALITY_FLAG).get(QualityFlag.Fields.ID), QualityFlagEnum.NOT_QUALIFIED.getId()), QualityFlagEnum.NOT_QUALIFIED.getId())
        );
    }
}

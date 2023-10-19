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
import net.sumaris.core.model.data.IRootDataEntity;
import net.sumaris.core.model.data.IWithDataQualityEntity;
import net.sumaris.core.model.referential.IItemReferentialEntity;
import net.sumaris.core.model.referential.QualityFlag;
import net.sumaris.core.model.referential.QualityFlagEnum;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.ParameterExpression;
import java.util.Arrays;
import java.util.Collection;

/**
 * @author peck7 on 28/08/2020.
 */
public interface RootDataSpecifications<E extends IRootDataEntity<Integer>>
        extends DataSpecifications<Integer, E> {

    String RECORDER_PERSON_ID_PARAM = "recorderPersonId";
    String PROGRAM_LABEL_PARAM = "programLabel";
    String PROGRAM_IDS_PARAM = "programIds";

    default Specification<E> hasRecorderPersonId(Integer recorderPersonId) {
        if (recorderPersonId == null) return null;
        return BindableSpecification.where((root, query, cb) -> {
            ParameterExpression<Integer> param = cb.parameter(Integer.class, RECORDER_PERSON_ID_PARAM);
            return cb.equal(root.get(E.Fields.RECORDER_PERSON).get(IEntity.Fields.ID), param);
        }).addBind(RECORDER_PERSON_ID_PARAM, recorderPersonId);
    }

    default Specification<E> hasProgramLabel(String programLabel) {
        if (programLabel == null) return null;
        return BindableSpecification.where((root, query, cb) -> {
            ParameterExpression<String> param = cb.parameter(String.class, PROGRAM_LABEL_PARAM);
            return cb.equal(root.get(E.Fields.PROGRAM).get(IItemReferentialEntity.Fields.LABEL), param);
        }).addBind(PROGRAM_LABEL_PARAM, programLabel);
    }

    default Specification<E> withDataQualityStatus(DataQualityStatusEnum status) {
        if (status != null) {
            return switch (status) {
                case MODIFIED -> isNotControlled();
                case CONTROLLED -> isControlled();
                case VALIDATED -> isValidated();
                case QUALIFIED -> isQualified();
            };
        }
        return null;
    }

    default Specification<E> hasProgramIds(Integer[] programIds) {
        if (ArrayUtils.isEmpty(programIds)) return null;
        return BindableSpecification.<E>where((root, query, cb) -> {
            ParameterExpression<Collection> param = cb.parameter(Collection.class, PROGRAM_IDS_PARAM);
            return cb.in(root.get(E.Fields.PROGRAM).get(IItemReferentialEntity.Fields.ID)).value(param);
        }).addBind(PROGRAM_IDS_PARAM, Arrays.asList(programIds));
    }

    /**
     * Control date is not null
     * @return
     */
    default Specification<E> isControlled() {
        return (root, query, cb) ->
            cb.and(
                // Control date not null
                cb.isNotNull(root.get(IRootDataEntity.Fields.CONTROL_DATE)),
                // Not validated
                cb.isNull(root.get(IRootDataEntity.Fields.VALIDATION_DATE)),
                // Not qualified
                cb.or(
                    cb.isNull(root.get(IWithDataQualityEntity.Fields.QUALIFICATION_DATE)),
                    cb.equal(cb.coalesce(root.get(IDataEntity.Fields.QUALITY_FLAG).get(QualityFlag.Fields.ID), QualityFlagEnum.NOT_QUALIFIED.getId()), QualityFlagEnum.NOT_QUALIFIED.getId())
                )
            );
    }

    default Specification<E> isValidated() {
        return (root, query, cb) ->
            cb.and(
                // Validation date not null
                cb.isNotNull(root.get(IWithDataQualityEntity.Fields.VALIDATION_DATE)),
                // Not qualified
                cb.or(
                    cb.isNull(root.get(IWithDataQualityEntity.Fields.QUALIFICATION_DATE)),
                    cb.equal(cb.coalesce(root.get(IDataEntity.Fields.QUALITY_FLAG).get(QualityFlag.Fields.ID), QualityFlagEnum.NOT_QUALIFIED.getId()), QualityFlagEnum.NOT_QUALIFIED.getId())
                )
            );
    }
}

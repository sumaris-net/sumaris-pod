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

import net.sumaris.core.model.data.DataQualityStatusEnum;
import net.sumaris.core.model.data.IValidatableDataEntity;
import net.sumaris.core.model.data.IWithValidationDateEntity;
import net.sumaris.core.model.referential.QualityFlag;
import net.sumaris.core.model.referential.QualityFlagEnum;
import org.springframework.data.jpa.domain.Specification;

import java.io.Serializable;

/**
 * @author peck7 on 28/08/2020.
 */
public interface IValidatableDataSpecifications<ID extends Serializable, E extends IValidatableDataEntity<ID>>
        extends IDataSpecifications<ID, E> {

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

    /**
     * Control date is not null
     * @return
     */
    default Specification<E> isControlled() {
        return (root, query, cb) ->
            cb.and(
                // Control date not null
                cb.isNotNull(root.get(IValidatableDataEntity.Fields.CONTROL_DATE)),
                // Not validated
                cb.isNull(root.get(IValidatableDataEntity.Fields.VALIDATION_DATE)),
                // Not qualified
                cb.or(
                    cb.isNull(root.get(IValidatableDataEntity.Fields.QUALIFICATION_DATE)),
                    cb.equal(cb.coalesce(root.get(IValidatableDataEntity.Fields.QUALITY_FLAG).get(QualityFlag.Fields.ID), QualityFlagEnum.NOT_QUALIFIED.getId()), QualityFlagEnum.NOT_QUALIFIED.getId())
                )
            );
    }

    default Specification<E> isValidated() {
        return (root, query, cb) ->
            cb.and(
                // Validation date not null
                cb.isNotNull(root.get(IWithValidationDateEntity.Fields.VALIDATION_DATE)),
                // Not qualified
                cb.or(
                    cb.isNull(root.get(IValidatableDataEntity.Fields.QUALIFICATION_DATE)),
                    cb.equal(cb.coalesce(root.get(IValidatableDataEntity.Fields.QUALITY_FLAG).get(QualityFlag.Fields.ID), QualityFlagEnum.NOT_QUALIFIED.getId()), QualityFlagEnum.NOT_QUALIFIED.getId())
                )
            );
    }
}

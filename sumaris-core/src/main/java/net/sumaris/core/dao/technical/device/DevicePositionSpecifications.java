package net.sumaris.core.dao.technical.device;

/*-
 * #%L
 * SUMARiS:: Core
 * %%
 * Copyright (C) 2018 - 2023 SUMARiS Consortium
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
import net.sumaris.core.model.referential.ObjectType;
import net.sumaris.core.model.technical.device.DevicePosition;
import net.sumaris.core.util.StringUtils;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.ParameterExpression;
import java.util.Date;

public interface DevicePositionSpecifications extends IEntitySpecifications<Integer, DevicePosition> {
    String START_DATE_PARAM = "startDate";
    String END_DATE_PARAM = "endDate";
    String START_DATE_PARAM_IS_NULL = "startDateIsNull";
    String END_DATE_PARAM_IS_NULL = "endDateIsNull";
    default Specification<DevicePosition> isBetweenDates(Date startDate, Date endDate) {
        if (startDate == null && endDate == null) return null;
        return BindableSpecification.where((root, query, cb) -> {
                ParameterExpression<Date> startDateParam = cb.parameter(Date.class, START_DATE_PARAM);
                ParameterExpression<Boolean> startDateParamIsNull = cb.parameter(Boolean.class, START_DATE_PARAM_IS_NULL);
                ParameterExpression<Date> endDateParam = cb.parameter(Date.class, END_DATE_PARAM);
                ParameterExpression<Boolean> endDateParamIsNull = cb.parameter(Boolean.class, END_DATE_PARAM_IS_NULL);


                return cb.and(
                    cb.or(
                        cb.isTrue(startDateParamIsNull),
                        cb.or(
                            cb.greaterThanOrEqualTo(root.get(DevicePosition.Fields.DATE_TIME), startDateParam)
                        )
                    ),
                    cb.or(
                        cb.isTrue(endDateParamIsNull),
                        cb.or(
                            cb.lessThanOrEqualTo(root.get(DevicePosition.Fields.DATE_TIME), endDateParam)
                        )
                    )
                );
            })
            .addBind(START_DATE_PARAM, startDate)
            .addBind(START_DATE_PARAM_IS_NULL, startDate == null ? Boolean.TRUE : Boolean.FALSE)
            .addBind(END_DATE_PARAM, endDate)
            .addBind(END_DATE_PARAM_IS_NULL, endDate == null ? Boolean.TRUE : Boolean.FALSE);
    }

    default Specification<DevicePosition> hasObjectTypeLabel(String objectTypeLabel) {
        if (StringUtils.isBlank(objectTypeLabel)) return null;
        String parameterName = DevicePosition.Fields.OBJECT_TYPE + ObjectType.Fields.LABEL;
        return BindableSpecification.where((root, query, cb) -> {
            ParameterExpression<String> param = cb.parameter(String.class, parameterName);
            return cb.equal(root.get(DevicePosition.Fields.OBJECT_TYPE).get(ObjectType.Fields.LABEL), param);
        }).addBind(parameterName, objectTypeLabel);
    }

    default Specification<DevicePosition> hasObjectTypeId(Integer objectTypeId) {
        if (objectTypeId == null) return null;
        return BindableSpecification.where((root, query, cb) -> {
            ParameterExpression<Integer> param = cb.parameter(Integer.class, DevicePosition.Fields.OBJECT_TYPE);
            return cb.equal(root.get(DevicePosition.Fields.OBJECT_TYPE).get(IEntity.Fields.ID), param);
        }).addBind(DevicePosition.Fields.OBJECT_TYPE, objectTypeId);
    }

    default Specification<DevicePosition> hasRecorderPersonId(Integer recorderPersonId) {
        if (recorderPersonId == null) return null;
        return BindableSpecification.where((root, query, cb) -> {
            ParameterExpression<Integer> param = cb.parameter(Integer.class, DevicePosition.Fields.RECORDER_PERSON);
            return cb.equal(root.get(DevicePosition.Fields.RECORDER_PERSON).get(IEntity.Fields.ID), param);
        }).addBind(DevicePosition.Fields.RECORDER_PERSON, recorderPersonId);
    }

    default Specification<DevicePosition> hasObjectId(Integer objectId) {
        if (objectId == null) return null;
        return BindableSpecification.where((root, query, cb) -> {
            ParameterExpression<Integer> param = cb.parameter(Integer.class, DevicePosition.Fields.OBJECT_ID);
            return cb.equal(root.get(DevicePosition.Fields.OBJECT_ID), param);
        }).addBind(DevicePosition.Fields.OBJECT_ID, objectId);
    }

}

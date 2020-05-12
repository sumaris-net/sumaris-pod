package net.sumaris.core.dao.data;

/*-
 * #%L
 * SUMARiS:: Core
 * %%
 * Copyright (C) 2018 SUMARiS Consortium
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

import net.sumaris.core.dao.technical.model.IEntity;
import net.sumaris.core.model.administration.programStrategy.Program;
import net.sumaris.core.model.data.Trip;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.data.TripVO;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collection;
import java.util.Date;
import java.util.List;

public interface TripRepositoryExtend
    extends IEntityConverter<Trip, TripVO> {

    default Specification<Trip> hasRecorderPersonId(Integer personId) {
        if (personId == null) return null;
        return (root, query, cb) -> cb.equal(root.get(Trip.Fields.RECORDER_PERSON).get(IEntity.Fields.ID), personId);
    }

    default Specification<Trip> hasRecorderDepartmentId(Integer depId) {
        if (depId == null) return null;
        return (root, query, cb) -> cb.equal(root.get(Trip.Fields.RECORDER_DEPARTMENT).get(IEntity.Fields.ID), depId);
    }

    default Specification<Trip> hasProgramLabel(String programLabel) {
        if (StringUtils.isBlank(programLabel)) return null;
        return (root, query, cb) -> cb.equal(root.get(Trip.Fields.PROGRAM).get(Program.Fields.LABEL), programLabel);
    }

    default Specification<Trip> hasLocationId(Integer locationId) {
        if (locationId == null) return null;
        return (root, query, cb) -> cb.or(
                cb.equal(root.get(Trip.Fields.DEPARTURE_LOCATION).get(IEntity.Fields.ID), locationId),
                cb.equal(root.get(Trip.Fields.RETURN_LOCATION).get(IEntity.Fields.ID), locationId)
        );
    }

    default Specification<Trip> hasVesselId(Integer vesselId) {
        if (vesselId == null) return null;
        return (root, query, cb) -> cb.equal(root.get(Trip.Fields.VESSEL).get(IEntity.Fields.ID), vesselId);
    }

    default Specification<Trip> betweenDate(Date startDate, Date endDate) {
        if (startDate == null && endDate == null) return null;
        return (root, query, cb) -> {
            // Start + end date
            if (startDate != null && endDate != null) {
                return cb.and(
                        cb.not(cb.lessThan(root.get(Trip.Fields.RETURN_DATE_TIME), startDate)),
                        cb.not(cb.greaterThan(root.get(Trip.Fields.DEPARTURE_DATE_TIME), endDate))
                );
            }

            // Start date only
            else if (startDate != null) {
                return cb.greaterThanOrEqualTo(root.get(Trip.Fields.RETURN_DATE_TIME), startDate);
            }

            // End date only
            else {
                return cb.lessThanOrEqualTo(root.get(Trip.Fields.DEPARTURE_DATE_TIME), endDate);
            }
        };
    }



}

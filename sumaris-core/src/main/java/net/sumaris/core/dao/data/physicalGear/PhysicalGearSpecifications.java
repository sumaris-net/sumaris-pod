package net.sumaris.core.dao.data.physicalGear;

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

import net.sumaris.core.dao.data.RootDataSpecifications;
import net.sumaris.core.dao.technical.Daos;
import net.sumaris.core.dao.technical.jpa.BindableSpecification;
import net.sumaris.core.dao.technical.model.IEntity;
import net.sumaris.core.model.administration.programStrategy.Program;
import net.sumaris.core.model.data.PhysicalGear;
import net.sumaris.core.model.data.Trip;
import net.sumaris.core.model.data.Vessel;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.data.PhysicalGearVO;
import net.sumaris.core.vo.filter.PhysicalGearFilterVO;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.Join;
import javax.persistence.criteria.ParameterExpression;
import java.util.Date;
import java.util.List;

public interface PhysicalGearSpecifications extends RootDataSpecifications<PhysicalGear> {


    default Specification<PhysicalGear> hasVesselId(Integer vesselId) {
        if (vesselId == null) return null;
        return BindableSpecification.where((root, query, criteriaBuilder) -> {
            ParameterExpression<Integer> param = criteriaBuilder.parameter(Integer.class, PhysicalGearFilterVO.Fields.VESSEL_ID);
            Join<PhysicalGear, Vessel> vesselJoin = Daos.composeJoin(root, StringUtils.doting(PhysicalGear.Fields.TRIP, Trip.Fields.VESSEL));
            return criteriaBuilder.equal(vesselJoin.get(Vessel.Fields.ID), param);
        }).addBind(PhysicalGearFilterVO.Fields.VESSEL_ID, vesselId);
    }

    default Specification<PhysicalGear> hasTripId(Integer tripId) {
        if (tripId == null) return null;
        return BindableSpecification.where((root, query, criteriaBuilder) -> {
            ParameterExpression<Integer> param = criteriaBuilder.parameter(Integer.class, PhysicalGearFilterVO.Fields.TRIP_ID);
            return criteriaBuilder.equal(Daos.composeJoin(root, PhysicalGear.Fields.TRIP)
                .get(IEntity.Fields.ID), param);
        }).addBind(PhysicalGearFilterVO.Fields.TRIP_ID, tripId);
    }

    default Specification<PhysicalGear> hasProgramLabel(String programLabel) {
        if (programLabel == null) return null;
        return BindableSpecification.where((root, query, criteriaBuilder) -> {
            ParameterExpression<String> param = criteriaBuilder.parameter(String.class, PhysicalGearVO.Fields.PROGRAM);
            return criteriaBuilder.equal(Daos.composeJoin(root, StringUtils.doting(PhysicalGear.Fields.TRIP, Trip.Fields.PROGRAM))
                .get(Program.Fields.LABEL), param);
        }).addBind(PhysicalGearVO.Fields.PROGRAM, programLabel);
    }

    default Specification<PhysicalGear> betweenDate(Date startDate, Date endDate) {
        if (startDate == null && endDate == null) return null;
        return (root, query, cb) -> {
            Join<PhysicalGear, Trip> tripJoin = Daos.composeJoin(root, PhysicalGear.Fields.TRIP);
            // Start + end date
            if (startDate != null && endDate != null) {
                return cb.not(
                    cb.or(
                        cb.lessThan(tripJoin.get(Trip.Fields.RETURN_DATE_TIME), startDate),
                        cb.greaterThan(tripJoin.get(Trip.Fields.DEPARTURE_DATE_TIME), endDate)
                    )
                );
            }
            // Start date
            else if (startDate != null) {
                return cb.greaterThanOrEqualTo(tripJoin.get(Trip.Fields.DEPARTURE_DATE_TIME), startDate);
            }
            // End date
            else {
                return cb.lessThanOrEqualTo(tripJoin.get(Trip.Fields.RETURN_DATE_TIME), endDate);
            }
        };
    }

    List<PhysicalGearVO> saveAllByTripId(final int tripId, final List<PhysicalGearVO> sources, List<Integer> idsToRemove);
}

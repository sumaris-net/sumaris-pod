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
import net.sumaris.core.model.IEntity;
import net.sumaris.core.model.administration.programStrategy.Program;
import net.sumaris.core.model.data.PhysicalGear;
import net.sumaris.core.model.data.Trip;
import net.sumaris.core.model.data.Vessel;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.data.PhysicalGearVO;
import net.sumaris.core.vo.filter.PhysicalGearFilterVO;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.ParameterExpression;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

public interface PhysicalGearSpecifications extends RootDataSpecifications<PhysicalGear> {

    default Specification<PhysicalGear> hasVesselIds(Integer[] vesselIds) {
        if (ArrayUtils.isEmpty(vesselIds)) return null;
        return BindableSpecification.<PhysicalGear>where((root, query, cb) -> {
                ParameterExpression<Collection> param = cb.parameter(Collection.class, PhysicalGearFilterVO.Fields.VESSEL_IDS);
                Join<PhysicalGear, Vessel> vessel = Daos.composeJoin(root, StringUtils.doting(PhysicalGear.Fields.TRIP, Trip.Fields.VESSEL), JoinType.INNER);
                return cb.in(vessel.get(Vessel.Fields.ID)).value(param);
            })
            .addBind(PhysicalGearFilterVO.Fields.VESSEL_IDS, Arrays.asList(vesselIds));
    }

    default Specification<PhysicalGear> hasTripId(Integer tripId) {
        if (tripId == null) return null;
        return BindableSpecification.where((root, query, cb) -> {
            ParameterExpression<Integer> param = cb.parameter(Integer.class, PhysicalGearFilterVO.Fields.TRIP_ID);
            return cb.equal(Daos.composeJoin(root, PhysicalGear.Fields.TRIP)
                .get(IEntity.Fields.ID), param);
        }).addBind(PhysicalGearFilterVO.Fields.TRIP_ID, tripId);
    }

    default Specification<PhysicalGear> excludeTripId(Integer tripId) {
        if (tripId == null) return null;
        return BindableSpecification.where((root, query, cb) -> {
            ParameterExpression<Integer> param = cb.parameter(Integer.class, PhysicalGearFilterVO.Fields.EXCLUDE_TRIP_ID);
            return cb.notEqual(Daos.composeJoin(root, PhysicalGear.Fields.TRIP).get(IEntity.Fields.ID), param);
        }).addBind(PhysicalGearFilterVO.Fields.EXCLUDE_TRIP_ID, tripId);
    }

    default Specification<PhysicalGear> hasProgramLabel(String programLabel) {
        if (programLabel == null) return null;
        return BindableSpecification.where((root, query, cb) -> {
            ParameterExpression<String> param = cb.parameter(String.class, PhysicalGearVO.Fields.PROGRAM);
            return cb.equal(Daos.composeJoin(root, StringUtils.doting(PhysicalGear.Fields.TRIP, Trip.Fields.PROGRAM))
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

    default Specification<PhysicalGear> hasParentGearId(Integer parentGearId) {
        if (parentGearId == null) return null;
        return BindableSpecification.where((root, query, cb) -> {
            ParameterExpression<Integer> param = cb.parameter(Integer.class, PhysicalGearFilterVO.Fields.PARENT_GEAR_ID);
            return cb.equal(Daos.composeJoin(root, PhysicalGear.Fields.PARENT)
                .get(IEntity.Fields.ID), param);
        }).addBind(PhysicalGearFilterVO.Fields.PARENT_GEAR_ID, parentGearId);
    }

    default Specification<PhysicalGear> excludeParentGearId(Integer excludeParentGearId) {
        if (excludeParentGearId == null) return null;
        return BindableSpecification.where((root, query, cb) -> {
            ParameterExpression<Integer> param = cb.parameter(Integer.class, PhysicalGearFilterVO.Fields.EXCLUDE_PARENT_GEAR_ID);
            return cb.notEqual(Daos.composeJoin(root, PhysicalGear.Fields.PARENT)
                .get(IEntity.Fields.ID), param);
        }).addBind(PhysicalGearFilterVO.Fields.EXCLUDE_PARENT_GEAR_ID, excludeParentGearId);
    }
    default Specification<PhysicalGear> excludeParentGear(Boolean excludeParentGear) {
        if (excludeParentGear == null || !excludeParentGear) return null;
        return BindableSpecification.where((root, query, cb) ->
            cb.isNotNull(Daos.composePath(root, PhysicalGear.Fields.PARENT))
        );
    }

    default Specification<PhysicalGear> excludeChildGear(Boolean excludeChildOperation) {
        if (excludeChildOperation == null || !excludeChildOperation) return null;
        return BindableSpecification.where((root, query, cb) ->
            cb.isNull(Daos.composePath(root, PhysicalGear.Fields.PARENT))
        );
    }

    /**
     * Save all physical gears, on a trip
     * @param tripId
     * @param sources
     * @return
     */
    List<PhysicalGearVO> saveAllByTripId(final int tripId, final List<PhysicalGearVO> sources);

    /**
     * Save all physical gears, on a trip.
     * Allow to delete unused entities later (outside this function).
     *
     * @param tripId
     * @param sources
     * @param idsToRemove Used to allow deletion later. Is null, will delete unused entities inside the function
     * @return
     */
    List<PhysicalGearVO> saveAllByTripId(final int tripId, final List<PhysicalGearVO> sources, List<Integer> idsToRemove);
}

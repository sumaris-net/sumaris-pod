package net.sumaris.core.dao.data.trip;

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

import net.sumaris.core.dao.data.IWithObserversSpecifications;
import net.sumaris.core.dao.data.IWithSamplingStrataSpecifications;
import net.sumaris.core.dao.data.IWithVesselSpecifications;
import net.sumaris.core.dao.data.RootDataSpecifications;
import net.sumaris.core.dao.technical.Daos;
import net.sumaris.core.dao.technical.jpa.BindableSpecification;
import net.sumaris.core.model.IEntity;
import net.sumaris.core.model.data.*;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;

public interface TripSpecifications extends RootDataSpecifications<Trip>,
    IWithVesselSpecifications<Integer, Trip>,
    IWithObserversSpecifications<Trip>,
    IWithSamplingStrataSpecifications<Integer, Trip> {

    String LOCATION_IDS_PARAM = "locationIds";
    String OBSERVED_LOCATION_ID_PARAM = "observedLocationId";
    String OPERATION_IDS_PARAM = "operationIds";


    default <T> ListJoin<Vessel, VesselRegistrationPeriod> composeVrpJoin(Root<T> root, CriteriaBuilder cb) {
        Join<T, Vessel> vessel = composeVesselJoin(root);
        return composeVrpJoin(vessel, cb, root.get(Trip.Fields.DEPARTURE_DATE_TIME));
    }

    default <T> ListJoin<Vessel, VesselFeatures> composeVfJoin(Root<T> root, CriteriaBuilder cb) {
        Join<T, Vessel> vessel = composeVesselJoin(root);
        return composeVfJoin(vessel, cb, root.get(Trip.Fields.DEPARTURE_DATE_TIME));
    }

    default Specification<Trip> hasLocationIds(Integer[] locationIds) {
        if (ArrayUtils.isEmpty(locationIds)) return null;
        return BindableSpecification.where((root, query, cb) -> {
            ParameterExpression<Collection> param = cb.parameter(Collection.class, LOCATION_IDS_PARAM);
            return cb.or(
                cb.in(root.get(Trip.Fields.DEPARTURE_LOCATION).get(IEntity.Fields.ID)).value(param),
                cb.in(root.get(Trip.Fields.RETURN_LOCATION).get(IEntity.Fields.ID)).value(param)
            );
        }).addBind(LOCATION_IDS_PARAM, Arrays.asList(locationIds));
    }

    default Specification<Trip> hasObservedLocationId(Integer observedLocationId) {
        if (observedLocationId == null) return null;
        return BindableSpecification.where((root, query, cb) -> {
            ParameterExpression<Integer> param = cb.parameter(Integer.class, OBSERVED_LOCATION_ID_PARAM);
            ListJoin<Trip, Landing> landingJoin = Daos.composeJoinList(root, Trip.Fields.LANDINGS, JoinType.INNER);
            return cb.equal(landingJoin.get(Landing.Fields.OBSERVED_LOCATION).get(IEntity.Fields.ID), param);
        }).addBind(OBSERVED_LOCATION_ID_PARAM, observedLocationId);
    }

    default Specification<Trip> hasObservedLocation(Boolean hasObservedLocation) {
        if (hasObservedLocation == null) return null;
        return BindableSpecification.where((root, query, cb) -> {
            if (hasObservedLocation) {
                query.distinct(true);
                ListJoin<Trip, Landing> landingJoin = Daos.composeJoinList(root, Trip.Fields.LANDINGS, JoinType.INNER);
                return cb.isNotNull(landingJoin.get(Landing.Fields.OBSERVED_LOCATION));
            } else {
                ListJoin<Trip, Landing> landingJoin = Daos.composeJoinList(root, Trip.Fields.LANDINGS, JoinType.LEFT);
                return cb.isNull(landingJoin.get(Landing.Fields.OBSERVED_LOCATION));
            }
        });
    }

    default Specification<Trip> hasScientificCruise(Boolean hasScientificCruise) {
        if (hasScientificCruise == null) return null;
        return BindableSpecification.where((root, query, cb) -> {
            if (hasScientificCruise) {
                Join<Trip, ScientificCruise> cruiseJoin = Daos.composeJoin(root, Trip.Fields.SCIENTIFIC_CRUISE, JoinType.INNER);
                return cb.isNotNull(cruiseJoin);
            } else {
                Join<Trip, ScientificCruise> cruiseJoin = Daos.composeJoin(root, Trip.Fields.SCIENTIFIC_CRUISE, JoinType.LEFT);
                return cb.isNull(cruiseJoin);
            }
        });
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


    default Specification<Trip> withOperationIds(Integer[] operationIds) {
        if (ArrayUtils.isEmpty(operationIds)) return null;

        return BindableSpecification.where((root, query, cb) -> {
            ParameterExpression<Collection> param = cb.parameter(Collection.class, OPERATION_IDS_PARAM);
            Subquery<Operation> subQuery = query.subquery(Operation.class);
            Root<Operation> operation = subQuery.from(Operation.class);
            subQuery.select(operation.get(Operation.Fields.TRIP).get(Operation.Fields.ID));
            subQuery.where(cb.in(operation.get(Operation.Fields.ID)).value(param));
            return cb.in(root.get(IEntity.Fields.ID)).value(subQuery);
        }).addBind(OPERATION_IDS_PARAM, Arrays.asList(operationIds));
    }
}

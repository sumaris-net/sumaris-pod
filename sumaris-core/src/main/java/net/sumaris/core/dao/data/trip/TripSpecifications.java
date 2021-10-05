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

import net.sumaris.core.dao.data.RootDataSpecifications;
import net.sumaris.core.dao.technical.Daos;
import net.sumaris.core.dao.technical.jpa.BindableSpecification;
import net.sumaris.core.dao.technical.model.IEntity;
import net.sumaris.core.model.data.Trip;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.ParameterExpression;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;

public interface TripSpecifications extends RootDataSpecifications<Trip> {

    String VESSEL_ID_PARAM = "vesselId";
    String LOCATION_ID_PARAM = "locationId";
    String OBSERVER_PERSON_IDS_PARAM = "observerPersonIds";
    String INCLUDED_IDS_PARAM = "includedIds";

    default Specification<Trip> hasLocationId(Integer locationId) {
        BindableSpecification<Trip> specification = BindableSpecification.where((root, query, criteriaBuilder) -> {
            ParameterExpression<Integer> param = criteriaBuilder.parameter(Integer.class, LOCATION_ID_PARAM);
            return criteriaBuilder.or(
                criteriaBuilder.isNull(param),
                criteriaBuilder.equal(root.get(Trip.Fields.DEPARTURE_LOCATION).get(IEntity.Fields.ID), param),
                criteriaBuilder.equal(root.get(Trip.Fields.RETURN_LOCATION).get(IEntity.Fields.ID), param)
            );
        });
        specification.addBind(LOCATION_ID_PARAM, locationId);
        return specification;
    }

    default Specification<Trip> hasVesselId(Integer vesselId) {
        BindableSpecification<Trip> specification = BindableSpecification.where((root, query, criteriaBuilder) -> {
            ParameterExpression<Integer> param = criteriaBuilder.parameter(Integer.class, VESSEL_ID_PARAM);
            return criteriaBuilder.or(
                criteriaBuilder.isNull(param),
                criteriaBuilder.equal(root.get(Trip.Fields.VESSEL).get(IEntity.Fields.ID), param)
            );
        });
        specification.addBind(VESSEL_ID_PARAM, vesselId);
        return specification;
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

    default Specification<Trip> hasObserverPersonIds(Integer... observerPersonIds) {
        if (ArrayUtils.isEmpty(observerPersonIds)) return null;

        return BindableSpecification.where((root, query, criteriaBuilder) -> {

            // Avoid duplicated entries (because of inner join)
            query.distinct(true);

            ParameterExpression<Collection> parameter = criteriaBuilder.parameter(Collection.class, OBSERVER_PERSON_IDS_PARAM);
            return criteriaBuilder.in(Daos.composeJoin(root, Trip.Fields.OBSERVERS).get(IEntity.Fields.ID))
                .value(parameter);
        }).addBind(OBSERVER_PERSON_IDS_PARAM, Arrays.asList(observerPersonIds));
    }

    default Specification<Trip> includedIds(Integer[] includedIds) {
        if (ArrayUtils.isEmpty(includedIds)) return null;
        return BindableSpecification.<Trip>where((root, query, criteriaBuilder) -> {
            ParameterExpression<Collection> param = criteriaBuilder.parameter(Collection.class, INCLUDED_IDS_PARAM);
            return criteriaBuilder.in(root.get(Trip.Fields.ID)).value(param);
        })
                .addBind(INCLUDED_IDS_PARAM, Arrays.asList(includedIds));
    }

}

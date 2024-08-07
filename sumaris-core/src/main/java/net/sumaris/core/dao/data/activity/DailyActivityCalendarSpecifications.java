package net.sumaris.core.dao.data.activity;

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

import net.sumaris.core.dao.data.IWithVesselSpecifications;
import net.sumaris.core.dao.data.RootDataSpecifications;
import net.sumaris.core.dao.technical.Daos;
import net.sumaris.core.dao.technical.hibernate.AdditionalSQLFunctions;
import net.sumaris.core.dao.technical.jpa.BindableSpecification;
import net.sumaris.core.model.IEntity;
import net.sumaris.core.model.data.*;
import net.sumaris.core.util.Dates;
import net.sumaris.core.util.StringUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;

public interface DailyActivityCalendarSpecifications extends RootDataSpecifications<DailyActivityCalendar>,
    IWithVesselSpecifications<Integer, DailyActivityCalendar> {

    String LOCATION_ID_PARAM = "locationId";
    String LOCATION_IDS_PARAM = "locationIds";
    String OBSERVED_LOCATION_ID_PARAM = "observedLocationId";


    default <T> ListJoin<Vessel, VesselRegistrationPeriod> composeVrpJoin(Root<T> root, CriteriaBuilder cb) {
        Join<T, Vessel> vessel = composeVesselJoin(root);
        Expression<Date> firstDayOfYear = cb.function(AdditionalSQLFunctions.first_day_of_year.name(),
            Date.class,
            root.get(DailyActivityCalendar.Fields.START_DATE)
        );
        return composeVrpJoin(vessel, cb, firstDayOfYear);
    }

    default <T> ListJoin<Vessel, VesselFeatures> composeVfJoin(Root<T> root, CriteriaBuilder cb) {
        Join<T, Vessel> vessel = composeVesselJoin(root);
        Expression<Date> firstDayOfYear = cb.function(AdditionalSQLFunctions.first_day_of_year.name(),
            Date.class,
            root.get(DailyActivityCalendar.Fields.START_DATE)
        );
        return composeVfJoin(vessel, cb, firstDayOfYear);
    }

    default Specification<DailyActivityCalendar> hasLocationId(Integer locationId) {
        if (locationId == null) return null;
        return BindableSpecification.where((root, query, cb) -> {
            query.distinct(true);
            Join<DailyActivityCalendar, VesselUseFeatures> vuf = Daos.composeJoinList(root, DailyActivityCalendar.Fields.VESSEL_USE_FEATURES, JoinType.INNER);
            ParameterExpression<Integer> param = cb.parameter(Integer.class, LOCATION_ID_PARAM);
            // TODO use LocationHierarchy
            return cb.equal(vuf.get(VesselUseFeatures.Fields.BASE_PORT_LOCATION).get(IEntity.Fields.ID), param);
        }).addBind(LOCATION_ID_PARAM, locationId);
    }

    default Specification<DailyActivityCalendar> hasLocationIds(Integer[] locationIds) {
        if (ArrayUtils.isEmpty(locationIds)) return null;
        return BindableSpecification.where((root, query, cb) -> {
            query.distinct(true);
            Join<DailyActivityCalendar, VesselUseFeatures> vuf = Daos.composeJoinList(root, DailyActivityCalendar.Fields.VESSEL_USE_FEATURES, JoinType.INNER);
            ParameterExpression<Collection> param = cb.parameter(Collection.class, LOCATION_IDS_PARAM);
            // TODO use LocationHierarchy
            return cb.in(vuf.get(VesselUseFeatures.Fields.BASE_PORT_LOCATION).get(IEntity.Fields.ID)).value(param);
        }).addBind(LOCATION_IDS_PARAM, Arrays.asList(locationIds));
    }

    default Specification<DailyActivityCalendar> betweenDate(Date startDate, Date endDate) {
        if (startDate == null && endDate == null) return null;
        return (root, query, cb) -> {
            // Start + end date
            if (startDate != null && endDate != null) {
                return cb.and(
                    cb.not(cb.lessThan(root.get(DailyActivityCalendar.Fields.END_DATE), startDate)),
                    cb.not(cb.greaterThan(root.get(DailyActivityCalendar.Fields.START_DATE), endDate))
                );
            }

            // Start date only
            else if (startDate != null) {
                return cb.greaterThanOrEqualTo(root.get(DailyActivityCalendar.Fields.END_DATE), startDate);
            }

            // End date only
            else {
                return cb.lessThanOrEqualTo(root.get(DailyActivityCalendar.Fields.START_DATE), endDate);
            }
        };
    }
    default Specification<DailyActivityCalendar> hasObservedLocationId(Integer observedLocationId) {
        if (observedLocationId == null) return null;
        return BindableSpecification.where((root, query, cb) -> {
            ParameterExpression<Integer> param = cb.parameter(Integer.class, OBSERVED_LOCATION_ID_PARAM);
            return cb.equal(Daos.composePath(root, StringUtils.doting(DailyActivityCalendar.Fields.OBSERVED_LOCATION, ObservedLocation.Fields.ID)), param);
        }).addBind(OBSERVED_LOCATION_ID_PARAM, observedLocationId);
    }


}

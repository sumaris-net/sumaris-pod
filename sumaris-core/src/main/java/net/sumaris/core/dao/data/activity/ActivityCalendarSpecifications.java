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
import net.sumaris.core.model.referential.location.Location;
import net.sumaris.core.util.Dates;
import net.sumaris.core.vo.filter.ActivityCalendarFilterVO;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;

public interface ActivityCalendarSpecifications extends RootDataSpecifications<ActivityCalendar>,
    IWithVesselSpecifications<Integer, ActivityCalendar> {


    default <T> ListJoin<Vessel, VesselRegistrationPeriod> composeVrpJoin(Root<T> root, CriteriaBuilder cb) {
        Join<T, Vessel> vessel = composeVesselJoin(root);
        Expression<Date> startDate = cb.function(AdditionalSQLFunctions.first_day_of_year.name(),
            Date.class,
            root.get(ActivityCalendar.Fields.YEAR)
        );
        Expression<Date> endDate = cb.function(AdditionalSQLFunctions.last_day_of_year.name(),
            Date.class,
            root.get(ActivityCalendar.Fields.YEAR)
        );
        return composeVrpJoinBetweenDate(vessel, cb, startDate, endDate, JoinType.LEFT);
    }

    default <T> ListJoin<Vessel, VesselFeatures> composeVfJoin(Root<T> root, CriteriaBuilder cb) {
        Join<T, Vessel> vessel = composeVesselJoin(root);
        Expression<Date> firstDayOfYear = cb.function(AdditionalSQLFunctions.first_day_of_year.name(),
            Date.class,
            root.get(ActivityCalendar.Fields.YEAR)
        );
        return composeVfJoin(vessel, cb, firstDayOfYear);
    }

    default Specification<ActivityCalendar> hasRegistrationLocationIds(Integer[] locationIds) {
        if (ArrayUtils.isEmpty(locationIds)) return null;
        return BindableSpecification.where((root, query, cb) -> {
            query.distinct(true);
            ParameterExpression<Collection> param = cb.parameter(Collection.class, ActivityCalendarFilterVO.Fields.REGISTRATION_LOCATION_IDS);
            ListJoin<Vessel, VesselRegistrationPeriod> vrp = composeVrpJoin(root, cb);
            Join<VesselRegistrationPeriod, Location> registrationLocation = Daos.composeJoin(vrp, VesselRegistrationPeriod.Fields.REGISTRATION_LOCATION);
            return cb.in(registrationLocation.get(IEntity.Fields.ID)).value(param);
        }).addBind(ActivityCalendarFilterVO.Fields.REGISTRATION_LOCATION_IDS, Arrays.asList(locationIds));
    }

    default Specification<ActivityCalendar> hasBasePortLocationIds(Integer[] locationIds) {
        if (ArrayUtils.isEmpty(locationIds)) return null;
        return BindableSpecification.where((root, query, cb) -> {
            query.distinct(true);
            Join<ActivityCalendar, VesselUseFeatures> vuf = Daos.composeJoinList(root, ActivityCalendar.Fields.VESSEL_USE_FEATURES, JoinType.INNER);
            ParameterExpression<Collection> param = cb.parameter(Collection.class, ActivityCalendarFilterVO.Fields.BASE_PORT_LOCATION_IDS);
            // TODO use LocationHierarchy ?
            return cb.in(vuf.get(VesselUseFeatures.Fields.BASE_PORT_LOCATION).get(IEntity.Fields.ID)).value(param);
        }).addBind(ActivityCalendarFilterVO.Fields.BASE_PORT_LOCATION_IDS, Arrays.asList(locationIds));
    }

    default Specification<ActivityCalendar> atYear(Integer year) {
        if (year == null) return null;
        return BindableSpecification.where((root, query, cb) -> {
            ParameterExpression<Integer> param = cb.parameter(Integer.class, ActivityCalendarFilterVO.Fields.YEAR);
            return cb.equal(root.get(ActivityCalendar.Fields.YEAR), param);
        }).addBind(ActivityCalendar.Fields.YEAR, year);
    }

    default Specification<ActivityCalendar> betweenDate(Date startDate, Date endDate) {
        if (startDate == null && endDate == null) return null;
        return (root, query, cb) -> {
            // Start + end date
            if (startDate != null && endDate != null) {
                int startYear = Dates.getYear(startDate);
                int endYear = Dates.getYear(endDate);
                return cb.and(
                    cb.not(cb.lessThan(root.get(ActivityCalendar.Fields.YEAR), startYear)),
                    cb.not(cb.greaterThan(root.get(ActivityCalendar.Fields.YEAR), endYear))
                );
            }

            // Start date only
            else if (startDate != null) {
                int startYear = Dates.getYear(startDate);
                return cb.greaterThanOrEqualTo(root.get(ActivityCalendar.Fields.YEAR), startYear);
            }

            // End date only
            else {
                int endYear = Dates.getYear(endDate);
                return cb.lessThanOrEqualTo(root.get(ActivityCalendar.Fields.YEAR), endYear);
            }
        };
    }

}

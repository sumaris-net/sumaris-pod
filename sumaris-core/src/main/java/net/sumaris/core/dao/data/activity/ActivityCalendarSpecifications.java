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
import net.sumaris.core.vo.filter.ActivityCalendarFilterVO;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;

public interface ActivityCalendarSpecifications extends RootDataSpecifications<ActivityCalendar>,
    IWithVesselSpecifications<Integer, ActivityCalendar> {

    default <T extends ActivityCalendar> ListJoin<Vessel, VesselRegistrationPeriod> composeVrpJoin(Root<T> root, CriteriaBuilder cb) {
        Join<T, Vessel> vessel = composeVesselJoin(root);
        return composeVrpJoinBetweenDate(root, cb, vessel);
    }

    default <T extends ActivityCalendar> ListJoin<Vessel, VesselRegistrationPeriod> composeVrpJoinBetweenDate(Root<T> root,
                                                                                     CriteriaBuilder cb,
                                                                                     From<?, Vessel> vessel) {
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

    default <T extends ActivityCalendar> Predicate vrpPredicate(Root<T> root,
                                   CriteriaBuilder cb,
                                   From<?, VesselRegistrationPeriod> vrp
    ) {
        Expression<Date> startDate = cb.function(AdditionalSQLFunctions.first_day_of_year.name(),
            Date.class,
            root.get(ActivityCalendar.Fields.YEAR)
        );
        Expression<Date> endDate = cb.function(AdditionalSQLFunctions.last_day_of_year.name(),
            Date.class,
            root.get(ActivityCalendar.Fields.YEAR)
        );
        return vrpBetweenDatePredicate(vrp, cb, startDate, endDate);
    }

    default <T extends ActivityCalendar> ListJoin<Vessel, VesselFeatures> composeVfJoin(Root<T> root, CriteriaBuilder cb) {
        Join<T, Vessel> vessel = composeVesselJoin(root);
        Expression<Date> firstDayOfYear = cb.function(AdditionalSQLFunctions.first_day_of_year.name(),
            Date.class,
            root.get(ActivityCalendar.Fields.YEAR)
        );
        return composeVfJoin(vessel, cb, firstDayOfYear);
    }

    default Specification<ActivityCalendar> hasRegistrationLocationIds(Integer[] locationIds) {
        if (ArrayUtils.isEmpty(locationIds)) return null;
        return BindableSpecification.<ActivityCalendar>where((root, query, cb) -> {
            ParameterExpression<Collection> param = cb.parameter(Collection.class, ActivityCalendarFilterVO.Fields.REGISTRATION_LOCATION_IDS);
            Join<ActivityCalendar, Vessel> vessel = composeVesselJoin(root);

            Subquery<VesselRegistrationPeriod> subQuery = query.subquery(VesselRegistrationPeriod.class);
            Root<VesselRegistrationPeriod> vrp = subQuery.from(VesselRegistrationPeriod.class);
            subQuery.select(vrp.get(VesselRegistrationPeriod.Fields.ID));

            subQuery.where(
                vrpPredicate(root, cb, vrp),
                cb.equal(vessel, vrp.get(VesselRegistrationPeriod.Fields.VESSEL)),
                cb.in(vrp.get(VesselRegistrationPeriod.Fields.REGISTRATION_LOCATION).get(IEntity.Fields.ID)).value(param)
            );
            return cb.exists(subQuery);
        }).addBind(ActivityCalendarFilterVO.Fields.REGISTRATION_LOCATION_IDS, Arrays.asList(locationIds));
    }

    default Specification<ActivityCalendar> isEmpty() {
        return BindableSpecification.where((root, query, cb) -> {
            Subquery<VesselUseFeatures> subQuery = query.subquery(VesselUseFeatures.class);
            Root<VesselUseFeatures> vuf = subQuery.from(VesselUseFeatures.class);
            subQuery.select(vuf.get(VesselUseFeatures.Fields.ID));
            subQuery.where(cb.equal(vuf.get(VesselUseFeatures.Fields.ACTIVITY_CALENDAR), root));
            return cb.not(cb.exists(subQuery));
        });
    }

    default Specification<ActivityCalendar> hasBasePortLocationIds(Integer[] locationIds) {
        if (ArrayUtils.isEmpty(locationIds)) return null;
        return BindableSpecification.<ActivityCalendar>where((root, query, cb) -> {
            ParameterExpression<Collection> param = cb.parameter(Collection.class, ActivityCalendarFilterVO.Fields.BASE_PORT_LOCATION_IDS);

            Subquery<VesselUseFeatures> subQuery = query.subquery(VesselUseFeatures.class);
            Root<VesselUseFeatures> vuf = subQuery.from(VesselUseFeatures.class);
            subQuery.select(vuf.get(VesselUseFeatures.Fields.ID));
            subQuery.where(
                // Same calendar
                cb.equal(vuf.get(VesselUseFeatures.Fields.ACTIVITY_CALENDAR), root),
                // Base port location
                vuf.get(VesselUseFeatures.Fields.BASE_PORT_LOCATION).get(IEntity.Fields.ID).in(param)
            );
            return cb.exists(subQuery);
        }).addBind(ActivityCalendarFilterVO.Fields.BASE_PORT_LOCATION_IDS, Arrays.asList(locationIds))
            // Or check in previous year, if calendar is empty
            .or(
                isEmpty().and(hasPreviousCalendarBasePortLocationIds(locationIds))
            )
            ;
    }

    default Specification<ActivityCalendar> hasPreviousCalendarBasePortLocationIds(Integer[] locationIds) {
        if (ArrayUtils.isEmpty(locationIds)) return null;
        return BindableSpecification.where((root, query, cb) -> {
            ParameterExpression<Collection> param = cb.parameter(Collection.class, ActivityCalendarFilterVO.Fields.BASE_PORT_LOCATION_IDS);

            Subquery<VesselUseFeatures> subQuery = query.subquery(VesselUseFeatures.class);
            Root<VesselUseFeatures> vuf = subQuery.from(VesselUseFeatures.class);
            Join<VesselUseFeatures, ActivityCalendar> vufCalendar = Daos.composeJoin(vuf, VesselUseFeatures.Fields.ACTIVITY_CALENDAR, JoinType.INNER);
            subQuery.select(vuf.get(VesselUseFeatures.Fields.ID));
            subQuery.where(
                // Same program
                cb.equal(vufCalendar.get(ActivityCalendar.Fields.PROGRAM), root.get(ActivityCalendar.Fields.PROGRAM)),
                // Same vessel
                cb.equal(vufCalendar.get(ActivityCalendar.Fields.VESSEL), root.get(ActivityCalendar.Fields.VESSEL)),
                // Previous year
                cb.equal(vufCalendar.get(ActivityCalendar.Fields.YEAR), cb.diff(root.get(ActivityCalendar.Fields.YEAR), 1)),
                // Base port location
                vuf.get(VesselUseFeatures.Fields.BASE_PORT_LOCATION).get(IEntity.Fields.ID).in(param)
            );
            return cb.exists(subQuery);
        }).addBind(ActivityCalendarFilterVO.Fields.BASE_PORT_LOCATION_IDS, Arrays.asList(locationIds));
    }

    default Specification<ActivityCalendar> hasDirectSurveyInvestigation(Integer directSurveyInvestigation) {
        if (directSurveyInvestigation == null) return null;
        return BindableSpecification.where((root, query, cb) -> {
            ParameterExpression<Integer> param = cb.parameter(Integer.class, ActivityCalendar.Fields.DIRECT_SURVEY_INVESTIGATION);
            return cb.equal(root.get(ActivityCalendar.Fields.DIRECT_SURVEY_INVESTIGATION), param);
        }).addBind(ActivityCalendar.Fields.DIRECT_SURVEY_INVESTIGATION, directSurveyInvestigation);
    }
    
    default Specification<ActivityCalendar> hasEconomicSurvey(Boolean economicSurvey) {
        if (economicSurvey == null) return null;
        return BindableSpecification.where((root, query, cb) -> {
            ParameterExpression<Boolean> param = cb.parameter(Boolean.class, ActivityCalendar.Fields.ECONOMIC_SURVEY);
            return cb.equal(root.get(ActivityCalendar.Fields.ECONOMIC_SURVEY), param);
        }).addBind(ActivityCalendar.Fields.ECONOMIC_SURVEY, economicSurvey);
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

    default Specification<ActivityCalendar> hasObserverPersonIds(Integer... observerPersonIds) {
        if (ArrayUtils.isEmpty(observerPersonIds)) return null;

        return BindableSpecification.where((root, query, cb) -> {

            ParameterExpression<Collection> parameter = cb.parameter(Collection.class, ActivityCalendarFilterVO.Fields.OBSERVER_PERSON_IDS);

            Subquery<ActivityCalendar> subQuery = query.subquery(ActivityCalendar.class);
            Root<ActivityCalendar> subRoot = subQuery.from(ActivityCalendar.class);
            subQuery.select(subRoot.get(ActivityCalendar.Fields.ID));
            subQuery.where(
                cb.equal(root, subRoot),
                cb.in(Daos.composeJoin(subRoot, ObservedLocation.Fields.OBSERVERS).get(IEntity.Fields.ID))
                    .value(parameter)
            );
            return cb.exists(subQuery);
        }).addBind(ActivityCalendarFilterVO.Fields.OBSERVER_PERSON_IDS, Arrays.asList(observerPersonIds));
    }

}

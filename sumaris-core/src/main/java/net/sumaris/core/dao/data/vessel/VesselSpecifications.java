package net.sumaris.core.dao.data.vessel;

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
import net.sumaris.core.model.data.Vessel;
import net.sumaris.core.model.data.VesselFeatures;
import net.sumaris.core.model.data.VesselRegistrationPeriod;
import net.sumaris.core.model.referential.Status;
import net.sumaris.core.model.referential.VesselType;
import net.sumaris.core.model.referential.location.Location;
import net.sumaris.core.model.referential.location.LocationHierarchy;
import net.sumaris.core.util.StringUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

public interface VesselSpecifications extends RootDataSpecifications<Vessel> {

    String VESSEL_FEATURES_ID_PARAM = "vesselFeaturesId";
    String VESSEL_TYPE_ID_PARAM = "vesselTypeId";
    String STATUS_IDS_PARAM = "statusIds";
    String REGISTRATION_LOCATION_ID_PARAM = "registrationLocationId";
    String BASE_PORT_LOCATION_ID = "basePortLocationId";
    String SEARCH_TEXT_PREFIX_PARAM = "searchTextPrefix";
    String SEARCH_TEXT_ANY_PARAM = "searchTextAny";

    boolean enableRegistrationCodeSearchAsPrefix();

    default ListJoin<Vessel, VesselRegistrationPeriod> composeVrpJoin(Root<Vessel> root) {
        return composeVrpJoin(root, JoinType.LEFT);
    }

    default ListJoin<Vessel, VesselRegistrationPeriod> composeVrpJoin(Root<Vessel> root, JoinType joinType) {
        return Daos.composeJoinList(root, Vessel.Fields.VESSEL_REGISTRATION_PERIODS, joinType);
    }

    default ListJoin<Vessel, VesselFeatures> composeVfJoin(Root<Vessel> root) {
        return composeVfJoin(root, JoinType.LEFT);
    }

    default ListJoin<Vessel, VesselFeatures> composeVfJoin(Root<Vessel> root, JoinType joinType) {
        return Daos.composeJoinList(root, Vessel.Fields.VESSEL_FEATURES, joinType);
    }

    default Specification<Vessel> vesselTypeId(Integer vesselTypeId) {
        if (vesselTypeId == null) return null;
        return BindableSpecification.where((root, query, cb) -> {
                ParameterExpression<Integer> param = cb.parameter(Integer.class, VESSEL_TYPE_ID_PARAM);
                return cb.equal(Daos.composePath(root, StringUtils.doting(Vessel.Fields.VESSEL_TYPE, VesselType.Fields.ID), JoinType.INNER), param);
            })
            .addBind(VESSEL_TYPE_ID_PARAM, vesselTypeId);
    }

    default Specification<Vessel> statusIds(List<Integer> statusIds) {
        if (CollectionUtils.isEmpty(statusIds)) return null;

        return BindableSpecification.where((root, query, cb) -> {
                ParameterExpression<Collection> param = cb.parameter(Collection.class, STATUS_IDS_PARAM);
                return cb.in(root.get(Vessel.Fields.STATUS).get(Status.Fields.ID)).value(param);
            })
            .addBind(STATUS_IDS_PARAM, statusIds);
    }

    default Specification<Vessel> betweenFeaturesDate(Date startDate, Date endDate) {
        if (startDate == null && endDate == null) return null;
        return (root, query, cb) -> {
            ListJoin<Vessel, VesselFeatures> features = composeVfJoin(root);

            // Start + end date
            if (startDate != null && endDate != null) {
                return cb.not(
                    cb.or(
                        cb.lessThan(cb.coalesce(features.get(VesselFeatures.Fields.END_DATE), Daos.DEFAULT_END_DATE_TIME), startDate),
                        cb.greaterThan(features.get(VesselFeatures.Fields.START_DATE), endDate)
                    )
                );
            }

            // Start date only
            else if (startDate != null) {
                return cb.greaterThanOrEqualTo(cb.coalesce(features.get(VesselFeatures.Fields.END_DATE), Daos.DEFAULT_END_DATE_TIME), startDate);
            }

            // End date only
            else {
                return cb.lessThanOrEqualTo(features.get(VesselFeatures.Fields.START_DATE), endDate);
            }
        };
    }

    default Specification<Vessel> betweenRegistrationDate(Date startDate, Date endDate, final boolean onlyWithRegistration) {
        if (startDate == null && endDate == null) return null;
        return (root, query, cb) -> {
            Join<Vessel, VesselRegistrationPeriod> vrp = composeVrpJoin(root, onlyWithRegistration ? JoinType.INNER : JoinType.LEFT);

            // Start + end date
            Predicate vrpDatesPredicate;
            if (startDate != null && endDate != null) {
                // NOT outside the start/end period
                vrpDatesPredicate = cb.not(
                    cb.or(
                        cb.lessThan(cb.coalesce(vrp.get(VesselRegistrationPeriod.Fields.END_DATE), Daos.DEFAULT_END_DATE_TIME), startDate),
                        cb.greaterThan(vrp.get(VesselRegistrationPeriod.Fields.START_DATE), endDate)
                    )
                );
            }

            // Start date only
            else if (startDate != null) {
                // VRP.end_date >= filter.startDate
                vrpDatesPredicate = cb.greaterThanOrEqualTo(cb.coalesce(vrp.get(VesselRegistrationPeriod.Fields.END_DATE), Daos.DEFAULT_END_DATE_TIME), startDate);
            }

            // End date only
            else {
                // VRP.start_date <=> filter.endDate
                vrpDatesPredicate = cb.lessThanOrEqualTo(vrp.get(VesselRegistrationPeriod.Fields.START_DATE), endDate);
            }

            if (onlyWithRegistration || vrp.getJoinType() == JoinType.INNER) return vrpDatesPredicate;

            // Allow without VRP (left outer join)
            return cb.or(
                cb.isNull(vrp.get(VesselRegistrationPeriod.Fields.ID)),
                vrpDatesPredicate
            );
        };
    }

    default Specification<Vessel> vesselFeaturesId(Integer vesselFeatureId) {
        if (vesselFeatureId == null) return null;

        return BindableSpecification.<Vessel>where((root, query, cb) -> {
            ParameterExpression<Integer> param = cb.parameter(Integer.class, VESSEL_FEATURES_ID_PARAM);
            ListJoin<Vessel, VesselFeatures> vf = composeVfJoin(root);
            return cb.equal(vf.get(VesselFeatures.Fields.ID), param);
        })
            .addBind(VESSEL_FEATURES_ID_PARAM, vesselFeatureId);
    }

    default Specification<Vessel> registrationLocationId(Integer registrationLocationId) {
        if (registrationLocationId == null) return null;
        return BindableSpecification.where((root, query, cb) -> {
            ParameterExpression<Integer> param = cb.parameter(Integer.class, REGISTRATION_LOCATION_ID_PARAM);
            Root<LocationHierarchy> lh = query.from(LocationHierarchy.class);

            ListJoin<Vessel, VesselRegistrationPeriod> vrp = Daos.composeJoinList(root, Vessel.Fields.VESSEL_REGISTRATION_PERIODS, JoinType.INNER);

            return cb.and(
                // LH.CHILD_LOCATION_FK = VRP.REGISTRATION_LOCATION_FK
                cb.equal(lh.get(LocationHierarchy.Fields.CHILD_LOCATION),
                    vrp.get(VesselRegistrationPeriod.Fields.REGISTRATION_LOCATION)),

                // AND LH.PARENT_LOCATION_FK = :registrationLocationId
                cb.equal(lh.get(LocationHierarchy.Fields.PARENT_LOCATION).get(Location.Fields.ID), param)
            );
        }).addBind(REGISTRATION_LOCATION_ID_PARAM, registrationLocationId);
    }


    default Specification<Vessel> basePortLocationId(Integer basePortLocationId) {
        if (basePortLocationId == null) return null;
        return BindableSpecification.<Vessel>where((root, query, cb) -> {
            ParameterExpression<Integer> param = cb.parameter(Integer.class, BASE_PORT_LOCATION_ID);
            Root<LocationHierarchy> lh = query.from(LocationHierarchy.class);

            ListJoin<Vessel, VesselFeatures> vf = composeVfJoin(root, JoinType.INNER);

            return cb.and(
                // LH.CHILD_LOCATION_FK = VF.BASE_PORT_LOCATION_FK
                cb.equal(lh.get(LocationHierarchy.Fields.CHILD_LOCATION),
                    vf.get(VesselFeatures.Fields.BASE_PORT_LOCATION)),

                // and LH.PARENT_LOCATION_FK = :basePortLocationId
                cb.equal(lh.get(LocationHierarchy.Fields.PARENT_LOCATION).get(Location.Fields.ID), param)
            );
        }).addBind(BASE_PORT_LOCATION_ID, basePortLocationId);
    }

    default Specification<Vessel> searchText(String[] searchAttributes, String searchText) {

        String searchTextAsPrefix = Daos.getEscapedSearchText(searchText);
        if (searchTextAsPrefix == null) return null;

        // If not defined, search on :
        // - VesselFeatures.exteriorMarking (prefix match - e.g. '<searchText>%')
        // - VesselRegistrationPeriod.registrationCode (prefix match - e.g. '<searchText>%')
        // - VesselFeatures.name (any match - e.g. '%<searchText>%')
        final String[] attributes = searchAttributes != null ? searchAttributes : new String[] {
            // Label
            StringUtils.doting(Vessel.Fields.VESSEL_FEATURES, VesselFeatures.Fields.EXTERIOR_MARKING),
            StringUtils.doting(Vessel.Fields.VESSEL_REGISTRATION_PERIODS, VesselRegistrationPeriod.Fields.REGISTRATION_CODE),
            // Name
            StringUtils.doting(Vessel.Fields.VESSEL_FEATURES, VesselFeatures.Fields.NAME)
        };

        boolean enableRegistrationCodeSearchAsPrefix = enableRegistrationCodeSearchAsPrefix();
        boolean enableAnySearch = !enableRegistrationCodeSearchAsPrefix
            || Arrays.stream(attributes).anyMatch(attr -> attr.endsWith(VesselFeatures.Fields.NAME));
        boolean enablePrefixSearch = enableRegistrationCodeSearchAsPrefix
            && Arrays.stream(attributes).anyMatch(attr -> !attr.endsWith(VesselFeatures.Fields.NAME));

        BindableSpecification<Vessel> specification = BindableSpecification.where((root, query, cb) -> {
            final ParameterExpression<String> prefixParam = cb.parameter(String.class, SEARCH_TEXT_PREFIX_PARAM);
            final ParameterExpression<String> anyParam = cb.parameter(String.class, SEARCH_TEXT_ANY_PARAM);

            Predicate[] predicates = Arrays.stream(attributes).map(attr -> cb.like(
                cb.upper(Daos.composePath(root, attr)),
                (enablePrefixSearch && !attr.endsWith(VesselFeatures.Fields.NAME)) ? prefixParam : anyParam,
                Daos.LIKE_ESCAPE_CHAR)
            ).toArray(Predicate[]::new);

            return cb.or(predicates);
        });

        if (enablePrefixSearch) specification.addBind(SEARCH_TEXT_PREFIX_PARAM, searchTextAsPrefix.toUpperCase());
        if (enableAnySearch) specification.addBind(SEARCH_TEXT_ANY_PARAM, "%" + searchTextAsPrefix.toUpperCase());

        return specification;
    }

}

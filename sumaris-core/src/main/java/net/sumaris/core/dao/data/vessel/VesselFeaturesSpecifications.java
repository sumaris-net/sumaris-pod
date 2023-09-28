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

import net.sumaris.core.dao.data.DataRepository;
import net.sumaris.core.dao.data.DataSpecifications;
import net.sumaris.core.dao.data.IWithVesselSpecifications;
import net.sumaris.core.dao.technical.Daos;
import net.sumaris.core.dao.technical.DatabaseType;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.dao.technical.jpa.BindableSpecification;
import net.sumaris.core.dao.technical.jpa.IFetchOptions;
import net.sumaris.core.model.data.Vessel;
import net.sumaris.core.model.data.VesselFeatures;
import net.sumaris.core.model.data.VesselRegistrationPeriod;
import net.sumaris.core.model.referential.Status;
import net.sumaris.core.model.referential.VesselType;
import net.sumaris.core.model.referential.location.Location;
import net.sumaris.core.model.referential.location.LocationHierarchy;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.data.IDataVO;
import net.sumaris.core.vo.filter.VesselFilterVO;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.NoRepositoryBean;

import javax.persistence.criteria.*;
import java.util.*;

@NoRepositoryBean
public interface VesselFeaturesSpecifications<
    E extends VesselFeatures,
    V extends IDataVO<Integer>,
    F extends VesselFilterVO,
    O extends IFetchOptions
    >
    extends DataSpecifications<Integer, VesselFeatures>,
    DataRepository<E, V, F, O>,
    IWithVesselSpecifications<VesselFeatures> {

    String VESSEL_ID_PARAM = "vesselId";
    String VESSEL_TYPE_ID_PARAM = "vesselTypeId";
    String STATUS_IDS_PARAM = "statusIds";
    String REGISTRATION_LOCATION_ID_PARAM = "registrationLocationId";
    String BASE_PORT_LOCATION_ID = "basePortLocationId";
    String SEARCH_TEXT_PREFIX_PARAM = "searchTextPrefix";
    String SEARCH_TEXT_ANY_PARAM = "searchTextAny";

    String MIN_UPDATE_DATE_PARAM = "minUpdateDate";

    String VRP_PATH = StringUtils.doting(VesselFeatures.Fields.VESSEL, Vessel.Fields.VESSEL_REGISTRATION_PERIODS);

    boolean enableRegistrationCodeSearchAsPrefix();

    default <T> ListJoin<Vessel, VesselRegistrationPeriod> composeVrpJoin(Root<T> root, CriteriaBuilder cb) {
        Join<T, Vessel> vessel = composeVesselJoin(root);
        return composeVrpJoin(vessel, cb, null /*VRP should be filtered by filter's dates*/);
    }

    default Specification<VesselFeatures> vesselId(Integer vesselId) {
        if (vesselId == null) return null;
        return BindableSpecification.where((root, query, cb) -> {
            ParameterExpression<Integer> param = cb.parameter(Integer.class, VESSEL_ID_PARAM);
            return cb.equal(root.get(VesselFeatures.Fields.VESSEL).get(Vessel.Fields.ID), param);
        })
        .addBind(VESSEL_ID_PARAM, vesselId);
    }

    default Specification<VesselFeatures> vesselTypeId(Integer vesselTypeId) {
        if (vesselTypeId == null) return null;
        return BindableSpecification.where((root, query, cb) -> {
                ParameterExpression<Integer> param = cb.parameter(Integer.class, VESSEL_TYPE_ID_PARAM);
                return cb.equal(Daos.composePath(root, StringUtils.doting(VesselFeatures.Fields.VESSEL, Vessel.Fields.VESSEL_TYPE, VesselType.Fields.ID), JoinType.INNER), param);
            })
            .addBind(VESSEL_TYPE_ID_PARAM, vesselTypeId);
    }

    default Specification<VesselFeatures> statusIds(List<Integer> statusIds) {
        if (CollectionUtils.isEmpty(statusIds)) return null;

        return BindableSpecification.where((root, query, cb) -> {
                ParameterExpression<Collection> param = cb.parameter(Collection.class, STATUS_IDS_PARAM);
                return cb.in(Daos.composePath(root,
                        StringUtils.doting(VesselFeatures.Fields.VESSEL, Vessel.Fields.STATUS, Status.Fields.ID)))
                    .value(param);
            })
            .addBind(STATUS_IDS_PARAM, statusIds);
    }

    default Specification<VesselFeatures> betweenFeaturesDate(Date startDate, Date endDate) {
        if (startDate == null && endDate == null) return null;
        return (root, query, cb) -> {

            // Start + end date
            if (startDate != null && endDate != null) {
                return cb.not(
                    cb.or(
                        cb.lessThan(Daos.nvlEndDate(root, cb, VesselFeatures.Fields.END_DATE, getDatabaseType()), startDate),
                        cb.greaterThan(root.get(VesselFeatures.Fields.START_DATE), endDate)
                    )
                );
            }

            // Start date only
            else if (startDate != null) {
                return cb.greaterThanOrEqualTo(Daos.nvlEndDate(root, cb, VesselFeatures.Fields.END_DATE, getDatabaseType()), startDate);
            }

            // End date only
            else {
                return cb.lessThanOrEqualTo(root.get(VesselFeatures.Fields.START_DATE), endDate);
            }
        };
    }

    default Specification<VesselFeatures> betweenRegistrationDate(Date startDate, Date endDate) {
        if (startDate == null && endDate == null) return null;
        return (root, query, cb) -> {

            ListJoin<Vessel, VesselRegistrationPeriod> vrp = composeVrpJoin(root, cb);

            // Start + end date
            if (startDate != null && endDate != null) {
                // NOT outside the start/end period
                return cb.not(
                    cb.or(
                        cb.lessThan(Daos.nvlEndDate(vrp, cb, VesselRegistrationPeriod.Fields.END_DATE, getDatabaseType()), startDate),
                        cb.greaterThan(vrp.get(VesselRegistrationPeriod.Fields.START_DATE), endDate)
                    )
                );
            }

            // Start date only
            else if (startDate != null) {
                // VRP.end_date >= filter.startDate
                return cb.greaterThanOrEqualTo(Daos.nvlEndDate(vrp, cb, VesselRegistrationPeriod.Fields.END_DATE, getDatabaseType()), startDate);
            }

            // End date only
            else {
                // VRP.start_date <=> filter.endDate
                return cb.lessThanOrEqualTo(vrp.get(VesselRegistrationPeriod.Fields.START_DATE), endDate);
            }
        };
    }

    default Specification<VesselFeatures> registrationLocation(Integer registrationLocationId) {
        if (registrationLocationId == null) return null;
        return BindableSpecification.<VesselFeatures>where((root, query, cb) -> {
            ParameterExpression<Integer> param = cb.parameter(Integer.class, REGISTRATION_LOCATION_ID_PARAM);

            Root<LocationHierarchy> lh = query.from(LocationHierarchy.class);

            ListJoin<Vessel, VesselRegistrationPeriod> vrp = composeVrpJoin(root, cb);

            return cb.and(
                // LH.CHILD_LOCATION_FK = VRP.REGISTRATION_LOCATION_FK
                cb.equal(lh.get(LocationHierarchy.Fields.CHILD_LOCATION), vrp.get(VesselRegistrationPeriod.Fields.REGISTRATION_LOCATION)),

                // AND LH.PARENT_LOCATION_FK = registrationLocationId
                cb.equal(Daos.composePath(lh, StringUtils.doting(LocationHierarchy.Fields.PARENT_LOCATION, Location.Fields.ID)), param)
            );
        }).addBind(REGISTRATION_LOCATION_ID_PARAM, registrationLocationId);
    }


    default Specification<VesselFeatures> basePortLocation(Integer basePortLocationId) {
        if (basePortLocationId == null) return null;
        return BindableSpecification.where((root, query, cb) -> {
            ParameterExpression<Integer> param = cb.parameter(Integer.class, BASE_PORT_LOCATION_ID);
            Root<LocationHierarchy> lh = query.from(LocationHierarchy.class);

            return cb.and(
                cb.equal(lh.get(LocationHierarchy.Fields.CHILD_LOCATION), Daos.composePath(root, VesselFeatures.Fields.BASE_PORT_LOCATION)),
                cb.equal(Daos.composePath(lh, StringUtils.doting(LocationHierarchy.Fields.PARENT_LOCATION, Location.Fields.ID)), param)
            );
        }).addBind(BASE_PORT_LOCATION_ID, basePortLocationId);
    }

    default Specification<VesselFeatures> searchText(String[] searchAttributes, String searchText) {

        String searchTextAsPrefix = Daos.getEscapedSearchText(searchText);
        if (searchTextAsPrefix == null) return null;

        // If not defined, search on :
        // - VesselFeatures.exteriorMarking (prefix match - e.g. '<searchText>%')
        // - VesselRegistrationPeriod.registrationCode (prefix match - e.g. '<searchText>%')
        // - VesselFeatures.exteriorMarking (any match - e.g. '%<searchText>%')
        final String[] attributes = searchAttributes != null ? searchAttributes : new String[] {
            // Label
            VesselFeatures.Fields.EXTERIOR_MARKING,
            StringUtils.doting(VesselFeatures.Fields.VESSEL, Vessel.Fields.VESSEL_REGISTRATION_PERIODS, VesselRegistrationPeriod.Fields.REGISTRATION_CODE),
            // Name
            VesselFeatures.Fields.NAME
        };

        boolean enableRegistrationCodeSearchAsPrefix = enableRegistrationCodeSearchAsPrefix();
        boolean enableAnySearch = !enableRegistrationCodeSearchAsPrefix
            || Arrays.stream(attributes).anyMatch(attr -> attr.endsWith(VesselFeatures.Fields.NAME));
        boolean enablePrefixSearch = enableRegistrationCodeSearchAsPrefix
            && Arrays.stream(attributes).anyMatch(attr -> !attr.endsWith(VesselFeatures.Fields.NAME));

        BindableSpecification<VesselFeatures> specification =  BindableSpecification.where((root, query, cb) -> {
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

    default Optional<V> getLastByVesselId(int vesselId, O fetchOptions) {
        return getByVesselIdAndDate(vesselId, null, fetchOptions);
    }

    default Optional<V> getByVesselIdAndDate(int vesselId, Date date, O fetchOptions) {
        F filter = (F)VesselFilterVO.builder()
            .vesselId(vesselId)
            .startDate(date)
            .build();
        List<V> result = findAll((F)filter, 0, 1, VesselFeatures.Fields.START_DATE, SortDirection.DESC, fetchOptions);

        // No result for this date:
        // => retry without date (should return the last features and period)
        if (result.isEmpty() && date != null) {
            filter.setDate(null);
            result = findAll(filter, 0, 1, VesselFeatures.Fields.START_DATE, SortDirection.DESC, fetchOptions);
        }
        if (result.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(result.get(0));
    }

    default Specification<VesselFeatures> newerThan(Date minUpdateDate) {
        if (minUpdateDate == null) return null;
        return BindableSpecification.where((root, query, cb) -> {
            Join<?, Vessel> vesselJoin = Daos.composeJoin(root, VesselFeatures.Fields.VESSEL);
            ParameterExpression<Date> updateDateParam = cb.parameter(Date.class, MIN_UPDATE_DATE_PARAM);
            return cb.or(
                cb.greaterThan(root.get(VesselFeatures.Fields.UPDATE_DATE), updateDateParam),
                cb.greaterThan(vesselJoin.get(Vessel.Fields.UPDATE_DATE), updateDateParam)
            );
        }).addBind(MIN_UPDATE_DATE_PARAM, minUpdateDate);
    }

    DatabaseType getDatabaseType();
}

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
import net.sumaris.core.dao.technical.Daos;
import net.sumaris.core.dao.technical.Pageables;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.dao.technical.jpa.BindableSpecification;
import net.sumaris.core.dao.technical.jpa.IFetchOptions;
import net.sumaris.core.model.data.IDataEntity;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.NoRepositoryBean;

import javax.persistence.criteria.*;
import java.util.*;

@NoRepositoryBean
public interface VesselFeaturesSpecifications<
    E extends IDataEntity<Integer>,
    V extends IDataVO<Integer>,
    F extends VesselFilterVO,
    O extends IFetchOptions
    >
    extends DataSpecifications<VesselFeatures>, DataRepository<E, V, F, O> {

    String VESSEL_ID_PARAM = "vesselId";
    String VESSEL_TYPE_ID_PARAM = "vesselTypeId";
    String STATUS_IDS_PARAM = "statusIds";
    String REGISTRATION_LOCATION_ID_PARAM = "registrationLocationId";
    String BASE_PORT_LOCATION_ID = "basePortLocationId";
    String SEARCH_TEXT_PREFIX_PARAM = "searchTextPrefix";
    String SEARCH_TEXT_ANY_PARAM = "searchTextAny";

    String VRP_PATH = StringUtils.doting(VesselFeatures.Fields.VESSEL, Vessel.Fields.VESSEL_REGISTRATION_PERIODS);

    default Specification<VesselFeatures> vesselId(Integer vesselId) {
        if (vesselId == null) return null;
        return BindableSpecification.where((root, query, criteriaBuilder) -> {
            ParameterExpression<Integer> param = criteriaBuilder.parameter(Integer.class, VESSEL_ID_PARAM);
            return criteriaBuilder.equal(root.get(VesselFeatures.Fields.VESSEL).get(Vessel.Fields.ID), param);
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

    default Expression<Date> nvlFeaturesEndDate(Path<?> root, CriteriaBuilder cb) {
        if (isOracleDatabase()) {
            // When using Oracle (e.g. over a SIH-Agadgio schema): use NVL to allow use of index
            return root.get(VesselFeatures.Fields.NVL_END_DATE);
        }
        return cb.coalesce(root.get(VesselFeatures.Fields.END_DATE), Daos.NVL_END_DATE_TIME);
    }

    default Expression<Date> nvlRegistrationEndDate(Path<VesselRegistrationPeriod> vrp, CriteriaBuilder cb) {
        if (isOracleDatabase()) {
            // When using Oracle (e.g. over a SIH-Agadgio schema): use NVL to allow use of index
            return vrp.get(VesselRegistrationPeriod.Fields.NVL_END_DATE);
        }
        return cb.coalesce(vrp.get(VesselRegistrationPeriod.Fields.END_DATE), Daos.NVL_END_DATE_TIME);
    }
    default Specification<VesselFeatures> betweenFeaturesDate(Date startDate, Date endDate) {
        if (startDate == null && endDate == null) return null;
        return (root, query, cb) -> {

            // Start + end date
            if (startDate != null && endDate != null) {
                return cb.not(
                    cb.or(
                        cb.lessThan(nvlFeaturesEndDate(root, cb), startDate),
                        cb.greaterThan(root.get(VesselFeatures.Fields.START_DATE), endDate)
                    )
                );
            }

            // Start date only
            else if (startDate != null) {
                return cb.greaterThanOrEqualTo(nvlFeaturesEndDate(root, cb), startDate);
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

            Join<?, VesselRegistrationPeriod> vrp = Daos.composeJoin(root, VRP_PATH);

            // Start + end date
            if (startDate != null && endDate != null) {
                return cb.and(
                    cb.equal(vrp.get(VesselRegistrationPeriod.Fields.VESSEL), root.get(VesselFeatures.Fields.VESSEL)),
                    // without VRP
                    //cb.isNull(vrp.get(VesselRegistrationPeriod.Fields.ID)),

                    // or NOT outside the start/end period
                    cb.not(
                        cb.or(
                            cb.lessThan(nvlRegistrationEndDate(vrp, cb), startDate),
                            cb.greaterThan(vrp.get(VesselRegistrationPeriod.Fields.START_DATE), endDate)
                        )
                    )
                );
            }

            // Start date only
            else if (startDate != null) {
                return cb.and(
                    cb.equal(vrp.get(VesselRegistrationPeriod.Fields.VESSEL), root.get(VesselFeatures.Fields.VESSEL)),
                    // without VRP
                    //cb.isNull(vrp.get(VesselRegistrationPeriod.Fields.ID)),
                    // VRP.end_date >= filter.startDate
                    cb.greaterThanOrEqualTo(nvlRegistrationEndDate(vrp, cb), startDate)
                );
            }

            // End date only
            else {
                return cb.and(
                    cb.equal(vrp.get(VesselRegistrationPeriod.Fields.VESSEL), root.get(VesselFeatures.Fields.VESSEL)),
                    // without VRP
                    //cb.isNull(vrp.get(VesselRegistrationPeriod.Fields.ID)),
                    // VRP.start_date <=> filter.endDate
                    cb.lessThanOrEqualTo(vrp.get(VesselRegistrationPeriod.Fields.START_DATE), endDate)
                );
            }
        };
    }

    default Specification<VesselFeatures> registrationLocation(Integer registrationLocationId) {
        if (registrationLocationId == null) return null;
        return BindableSpecification.where((root, query, cb) -> {
            ParameterExpression<Integer> param = cb.parameter(Integer.class, REGISTRATION_LOCATION_ID_PARAM);

            Root<LocationHierarchy> lh = query.from(LocationHierarchy.class);
            Join<?, VesselRegistrationPeriod> vrp = Daos.composeJoin(root, VRP_PATH);

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
            StringUtils.doting(VRP_PATH, VesselRegistrationPeriod.Fields.REGISTRATION_CODE),
            // Name
            VesselFeatures.Fields.NAME
        };

        return BindableSpecification.where((root, query, cb) -> {
            final ParameterExpression<String> prefixParam = cb.parameter(String.class, SEARCH_TEXT_PREFIX_PARAM);

            return cb.or(
                Arrays.stream(attributes).map(attr -> cb.like(
                    cb.upper(Daos.composePath(root, attr)),
                    prefixParam)
                ).toArray(Predicate[]::new)
            );
        })
            .addBind(SEARCH_TEXT_PREFIX_PARAM, searchTextAsPrefix.toUpperCase());
    }

    default Optional<V> getLastByVesselId(int vesselId, O fetchOptions) {
        return getByVesselIdAndDate(vesselId, null, fetchOptions);
    }

    default Optional<V> getByVesselIdAndDate(int vesselId, Date date, O fetchOptions) {
        VesselFilterVO filter = VesselFilterVO.builder()
            .vesselId(vesselId)
            .startDate(date)
            .build();
        Pageable lastVesselPage = Pageables.create(0, 1, VesselFeatures.Fields.START_DATE, SortDirection.DESC);
        Page<V> result = findAll((F)filter, lastVesselPage, fetchOptions);

        // No result for this date:
        // => retry without date (should return the last features and period)
        if (date != null && result.isEmpty()) {
            filter.setDate(null);
            result = findAll((F)filter, lastVesselPage, fetchOptions);
        }
        if (result.isEmpty()) {
            return Optional.empty();
        }
        return result.get().findFirst();
    }

    boolean isOracleDatabase();
}
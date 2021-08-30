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
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.data.VesselVO;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Predicate;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

public interface VesselSpecifications extends RootDataSpecifications<Vessel> {

    String VESSEL_FEATURES_ID_PARAM = "vesselFeaturesId";
    String STATUS_IDS_PARAM = "statusIds";
    String SEARCH_TEXT_PREFIX_PARAM = "searchTextPrefix";
    String SEARCH_TEXT_ANY_PARAM = "searchTextAny";

    default Specification<Vessel> atDate(Date date) {
        return betweenDate(date, date);
    }

    default Specification<Vessel> betweenDate(Date startDate, Date endDate) {
        if (startDate == null && endDate == null) return null;
        return (root, query, cb) -> {
            Join<Vessel, VesselFeatures> features = root.join(Vessel.Fields.VESSEL_FEATURES, JoinType.LEFT);
            Join<Vessel, VesselRegistrationPeriod> vrp = root.join(Vessel.Fields.VESSEL_REGISTRATION_PERIODS, JoinType.LEFT);

            // Start + end date
            if (startDate != null && endDate != null) {
                return cb.and(
                    cb.not(
                        cb.or(
                            cb.lessThan(cb.coalesce(features.get(VesselFeatures.Fields.END_DATE), Daos.NVL_END_DATE_TIME), startDate),
                            cb.greaterThan(features.get(VesselFeatures.Fields.START_DATE), endDate)
                        )
                    ),
                    cb.not(
                        cb.or(
                            cb.isNotNull(vrp.get(VesselRegistrationPeriod.Fields.ID)),
                            cb.lessThan(cb.coalesce(vrp.get(VesselRegistrationPeriod.Fields.END_DATE), Daos.NVL_END_DATE_TIME), startDate),
                            cb.greaterThan(vrp.get(VesselRegistrationPeriod.Fields.START_DATE), endDate)
                        )
                    )
                );
            }

            // Start date only
            else if (startDate != null) {
                return cb.and(
                    cb.not(
                        cb.lessThan(cb.coalesce(features.get(VesselFeatures.Fields.END_DATE), Daos.NVL_END_DATE_TIME), startDate)
                    ),
                    cb.not(
                        cb.not(cb.lessThan(cb.coalesce(vrp.get(VesselRegistrationPeriod.Fields.END_DATE), Daos.NVL_END_DATE_TIME), startDate))
                    )
                );
            }

            // End date only
            else {
                return cb.and(
                    cb.not(cb.greaterThan(features.get(VesselFeatures.Fields.START_DATE), endDate)),
                    cb.not(cb.greaterThan(vrp.get(VesselRegistrationPeriod.Fields.START_DATE), endDate))
                );
            }
        };
    }

    default Specification<Vessel> statusIds(List<Integer> statusIds) {
        if (CollectionUtils.isEmpty(statusIds)) return null;

        return BindableSpecification.where((root, query, cb) -> {
            ParameterExpression<Collection> param = cb.parameter(Collection.class, STATUS_IDS_PARAM);
            return cb.in(root.get(Vessel.Fields.STATUS).get(Status.Fields.ID)).value(param);
        })
        .addBind(STATUS_IDS_PARAM, statusIds);
    }

    default Specification<Vessel> vesselFeatures(Integer vesselFeatureId) {
        if (vesselFeatureId == null) return null;

        return BindableSpecification.where((root, query, cb) -> {
            Join<Vessel, VesselFeatures> featuresJoin = root.join(Vessel.Fields.VESSEL_FEATURES, JoinType.LEFT);
            ParameterExpression<Integer> param = cb.parameter(Integer.class, VESSEL_FEATURES_ID_PARAM);
            return cb.equal(featuresJoin.get(VesselFeatures.Fields.ID), param);
        })
            .addBind(VESSEL_FEATURES_ID_PARAM, vesselFeatureId);
    }

    default Specification<Vessel> searchText(String[] searchAttributes, String searchText) {

        String searchTextAsPrefix = Daos.getEscapedSearchText(searchText);
        if (searchTextAsPrefix == null) return null;

        // If not defined, search on :
        // - vesselFeatures.exteriorMarking (prefix match - e.g. '<searchText>%')
        // - vesselRegistrationPeriod.registrationCode (prefix match - e.g. '<searchText>%')
        // - vesselFeatures.exteriorMarking (any match - e.g. '%<searchText>%')
        final String[] attributes = searchAttributes != null ? searchAttributes : new String[] {
            // Label
            StringUtils.doting(Vessel.Fields.VESSEL_FEATURES, VesselFeatures.Fields.EXTERIOR_MARKING),
            StringUtils.doting(Vessel.Fields.VESSEL_REGISTRATION_PERIODS, VesselRegistrationPeriod.Fields.REGISTRATION_CODE),
            // Name
            StringUtils.doting(Vessel.Fields.VESSEL_FEATURES, VesselFeatures.Fields.NAME)
        };

        boolean enableAnySearch = Arrays.stream(attributes)
            .anyMatch(attr -> attr.endsWith(VesselFeatures.Fields.NAME));
        boolean enablePrefixSearch = Arrays.stream(attributes)
            .anyMatch(attr -> !attr.endsWith(VesselFeatures.Fields.NAME));

        BindableSpecification<Vessel> specification = BindableSpecification.where((root, query, cb) -> {
            final ParameterExpression<String> prefixParam = cb.parameter(String.class, SEARCH_TEXT_PREFIX_PARAM);
            final ParameterExpression<String> anyParam = cb.parameter(String.class, SEARCH_TEXT_ANY_PARAM);

            return cb.or(Arrays.stream(attributes)
                .map(attr -> cb.like(
                    cb.upper(Daos.composePath(root, attr)),
                    attr.endsWith(VesselFeatures.Fields.NAME) ? anyParam : prefixParam)
                ).toArray(Predicate[]::new)
            );
        });

        if (enablePrefixSearch) specification.addBind(SEARCH_TEXT_PREFIX_PARAM, searchTextAsPrefix.toUpperCase());
        if (enableAnySearch) specification.addBind(SEARCH_TEXT_ANY_PARAM, "%" + searchTextAsPrefix.toUpperCase());

        return specification;
    }

    VesselVO save(VesselVO vo, boolean checkUpdateDate);
}

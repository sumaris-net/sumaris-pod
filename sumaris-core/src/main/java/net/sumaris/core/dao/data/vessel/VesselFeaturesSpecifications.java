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

import net.sumaris.core.dao.data.DataSpecifications;
import net.sumaris.core.dao.technical.Daos;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.dao.technical.jpa.BindableSpecification;
import net.sumaris.core.dao.technical.jpa.IFetchOptions;
import net.sumaris.core.model.data.IDataEntity;
import net.sumaris.core.model.data.Vessel;
import net.sumaris.core.model.data.VesselFeatures;
import net.sumaris.core.model.data.VesselRegistrationPeriod;
import net.sumaris.core.model.referential.Status;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.data.IDataVO;
import net.sumaris.core.vo.filter.VesselFilterVO;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Predicate;
import java.util.*;

public interface VesselFeaturesSpecifications<
    E extends IDataEntity<Integer>,
    V extends IDataVO<Integer>,
    F extends VesselFilterVO,
    O extends IFetchOptions
    >
    extends DataSpecifications<VesselFeatures> {

    String VESSEL_ID_PARAM = "vesselId";
    String STATUS_IDS_PARAM = "statusIds";
    String SEARCH_TEXT_PREFIX_PARAM = "searchTextPrefix";
    String SEARCH_TEXT_ANY_PARAM = "searchTextAny";

    default Specification<VesselFeatures> vesselId(Integer vesselId) {
        if (vesselId == null) return null;
        BindableSpecification<VesselFeatures> specification = BindableSpecification.where((root, query, criteriaBuilder) -> {
            ParameterExpression<Integer> param = criteriaBuilder.parameter(Integer.class, VESSEL_ID_PARAM);
            return criteriaBuilder.equal(root.get(VesselFeatures.Fields.VESSEL).get(Vessel.Fields.ID), param);
        });
        specification.addBind(VESSEL_ID_PARAM, vesselId);
        return specification;
    }

    default Specification<VesselFeatures> statusIds(List<Integer> statusIds) {
        if (CollectionUtils.isEmpty(statusIds)) return null;

        return BindableSpecification.where((root, query, cb) -> {
                ParameterExpression<Collection> param = cb.parameter(Collection.class, STATUS_IDS_PARAM);
                return cb.in(root.get(Vessel.Fields.STATUS).get(Status.Fields.ID)).value(param);
            })
            .addBind(STATUS_IDS_PARAM, statusIds);
    }

    default Specification<VesselFeatures> atDate(Date date) {
        return betweenDate(date, null);
    }

    default Specification<VesselFeatures> betweenDate(Date startDate, Date endDate) {
        if (startDate == null && endDate == null) return null;
        return (root, query, cb) -> {

            // Start + end date
            if (startDate != null && endDate != null) {
                return cb.not(
                    cb.or(
                        cb.lessThan(cb.coalesce(root.get(VesselFeatures.Fields.END_DATE), Daos.NVL_END_DATE_TIME), startDate),
                        cb.greaterThan(root.get(VesselFeatures.Fields.START_DATE), endDate)
                    )
                );
            }

            // Start date only
            else if (startDate != null) {
                return cb.or(
                    cb.isNull(root.get(VesselFeatures.Fields.END_DATE)),
                    cb.not(cb.lessThan(root.get(VesselFeatures.Fields.END_DATE), startDate))
                );
            }

            // End date only
            else {
                return cb.not(cb.greaterThan(root.get(VesselFeatures.Fields.START_DATE), endDate));
            }
        };
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

        boolean enableAnySearch = Arrays.asList(attributes).contains(VesselFeatures.Fields.NAME);
        boolean enablePrefixSearch = Arrays.stream(attributes)
            .anyMatch(attr -> !attr.equals(VesselFeatures.Fields.NAME));

        BindableSpecification<VesselFeatures> specification = BindableSpecification.where((root, query, cb) -> {
            final ParameterExpression<String> prefixParam = cb.parameter(String.class, SEARCH_TEXT_PREFIX_PARAM);
            final ParameterExpression<String> anyParam = cb.parameter(String.class, SEARCH_TEXT_ANY_PARAM);

            return cb.or(Arrays.stream(attributes)
                .map(attr -> cb.like(
                    cb.upper(Daos.composePath(root, attr)),
                    attr.equals(VesselFeatures.Fields.NAME) ? anyParam : prefixParam)
                ).toArray(Predicate[]::new)
            );
        });

        if (enablePrefixSearch) specification.addBind(SEARCH_TEXT_PREFIX_PARAM, searchTextAsPrefix.toUpperCase());
        if (enableAnySearch) specification.addBind(SEARCH_TEXT_ANY_PARAM, "%" + searchTextAsPrefix.toUpperCase());

        return specification;
    }

    default Optional<V> getLastByVesselId(int vesselId, O fetchOptions) {
        return getByVesselIdAndDate(vesselId, null, fetchOptions);
    }

    default Optional<V> getByVesselIdAndDate(int vesselId, Date date, O fetchOptions) {
        VesselFilterVO filter = VesselFilterVO.builder()
            .vesselId(vesselId)
            .startDate(date)
            .build();
        Page lastVesselPage = Page.builder()
            .offset(0).size(1)
            .sortBy(VesselFeatures.Fields.START_DATE).sortDirection(SortDirection.DESC)
            .build();
        List<V> result = findAll((F)filter, lastVesselPage, fetchOptions);

        // No result for this date: retry without date (should return the last features and period)
        if (CollectionUtils.isEmpty(result) && date != null) {
            filter.setDate(null);
            result = findAll((F)filter, lastVesselPage, fetchOptions);
        }
        if (CollectionUtils.isEmpty(result)) {
            return Optional.empty();
        }
        return Optional.of(result.get(0));
    }

    List<V> findAll(F filter, Page page, O fetchOptions);
}

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
    String STATUS_IDS_PARAM = "statusIds";
    String SEARCH_TEXT_PREFIX_PARAM = "searchTextPrefix";
    String SEARCH_TEXT_ANY_PARAM = "searchTextAny";

    default Specification<VesselFeatures> vesselId(Integer vesselId) {
        if (vesselId == null) return null;
        return BindableSpecification.where((root, query, criteriaBuilder) -> {
            ParameterExpression<Integer> param = criteriaBuilder.parameter(Integer.class, VESSEL_ID_PARAM);
            return criteriaBuilder.equal(root.get(VesselFeatures.Fields.VESSEL).get(Vessel.Fields.ID), param);
        })
            .addBind(VESSEL_ID_PARAM, vesselId);
    }

    default Specification<VesselFeatures> statusIds(List<Integer> statusIds) {
        if (CollectionUtils.isEmpty(statusIds)) return null;

        return BindableSpecification.where((root, query, cb) -> {
                ParameterExpression<Collection> param = cb.parameter(Collection.class, STATUS_IDS_PARAM);
                return cb.in(root.get(VesselFeatures.Fields.VESSEL).get(Vessel.Fields.STATUS).get(Status.Fields.ID)).value(param);
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

        final String vrpPath = StringUtils.doting(VesselFeatures.Fields.VESSEL, Vessel.Fields.VESSEL_REGISTRATION_PERIODS);

        // If not defined, search on :
        // - VesselFeatures.exteriorMarking (prefix match - e.g. '<searchText>%')
        // - VesselRegistrationPeriod.registrationCode (prefix match - e.g. '<searchText>%')
        // - VesselFeatures.exteriorMarking (any match - e.g. '%<searchText>%')
        final String[] attributes = searchAttributes != null ? searchAttributes : new String[] {
            // Label
            VesselFeatures.Fields.EXTERIOR_MARKING,
            StringUtils.doting(vrpPath, VesselRegistrationPeriod.Fields.REGISTRATION_CODE),
            // Name
            VesselFeatures.Fields.NAME
        };

        boolean enableAnySearch = Arrays.asList(attributes).contains(VesselFeatures.Fields.NAME);
        boolean enablePrefixSearch = Arrays.stream(attributes)
            .anyMatch(attr -> !attr.equals(VesselFeatures.Fields.NAME));

        BindableSpecification<VesselFeatures> specification = BindableSpecification.where((root, query, cb) -> {
            final ParameterExpression<String> prefixParam = cb.parameter(String.class, SEARCH_TEXT_PREFIX_PARAM);
            final ParameterExpression<String> anyParam = cb.parameter(String.class, SEARCH_TEXT_ANY_PARAM);
            final Join<VesselFeatures, Vessel> vessel = root.join(VesselFeatures.Fields.VESSEL, JoinType.INNER);
            final Join<Vessel, VesselRegistrationPeriod> vrp = vessel.join(Vessel.Fields.VESSEL_REGISTRATION_PERIODS, JoinType.LEFT);

            return cb.or(Arrays.stream(attributes)
                .map(attr -> {
                    Path<String> attrPath;
                    if (attr.indexOf('.') != -1) {
                        if (attr.startsWith(vrpPath)) {
                            attrPath = Daos.composePath(vrp, attr.substring(vrpPath.length()+1));
                        }
                        else {
                            attrPath = Daos.composePath(root, attr);
                        }
                    }
                    else {
                        attrPath = root.get(attr);
                    }
                    return cb.like(
                            cb.upper(attrPath),
                            attr.equals(VesselFeatures.Fields.NAME) ? anyParam : prefixParam);
                }).toArray(Predicate[]::new)
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
}

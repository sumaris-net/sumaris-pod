package net.sumaris.core.dao.data.landing;

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
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.dao.technical.jpa.BindableSpecification;
import net.sumaris.core.model.IEntity;
import net.sumaris.core.model.data.Landing;
import net.sumaris.core.vo.data.LandingFetchOptions;
import net.sumaris.core.vo.data.LandingVO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.ParameterExpression;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

public interface LandingSpecifications extends RootDataSpecifications<Landing> {

    String OBSERVED_LOCATION_ID_PARAM = "observedLocationId";
    String TRIP_ID_PARAM = "tripId";
    String TRIP_IDS_PARAM = "tripIds";
    String LOCATION_ID_PARAM = "locationId";
    String LOCATION_IDS_PARAM = "locationIds";
    String VESSEL_ID_PARAM = "vesselId";
    String EXCLUDE_VESSEL_IDS_PARAM = "excludeVesselIds";

    default Specification<Landing> hasObservedLocationId(Integer observedLocationId) {
        if (observedLocationId == null) return null;
        return BindableSpecification.where((root, query, cb) -> {
            ParameterExpression<Integer> param = cb.parameter(Integer.class, OBSERVED_LOCATION_ID_PARAM);
            return cb.equal(root.get(Landing.Fields.OBSERVED_LOCATION).get(IEntity.Fields.ID), param);
        }).addBind(OBSERVED_LOCATION_ID_PARAM, observedLocationId);
    }

    default Specification<Landing> hasTripId(Integer tripId) {
        if (tripId == null) return null;
        return BindableSpecification.where((root, query, cb) -> {
            ParameterExpression<Integer> param = cb.parameter(Integer.class, TRIP_ID_PARAM);
            return cb.equal(root.get(Landing.Fields.TRIP).get(IEntity.Fields.ID), param);
        }).addBind(TRIP_ID_PARAM, tripId);
    }

    default Specification<Landing> hasTripIds(Collection<Integer> tripIds) {
        if (CollectionUtils.isEmpty(tripIds)) return null;
        return BindableSpecification.where((root, query, cb) -> {
            ParameterExpression<Collection> param = cb.parameter(Collection.class, TRIP_IDS_PARAM);
            return cb.in(root.get(Landing.Fields.TRIP).get(IEntity.Fields.ID)).value(param);
        }).addBind(TRIP_IDS_PARAM, tripIds);
    }

    default Specification<Landing> hasLocationId(Integer locationId) {
        if (locationId == null) return null;
        return BindableSpecification.where((root, query, cb) -> {
            ParameterExpression<Integer> param = cb.parameter(Integer.class, LOCATION_ID_PARAM);
            return cb.equal(root.get(Landing.Fields.LOCATION).get(IEntity.Fields.ID), param);
        }).addBind(LOCATION_ID_PARAM, locationId);
    }

    default Specification<Landing> inLocationIds(Integer... locationIds) {
        if (ArrayUtils.isEmpty(locationIds)) return null;
        return BindableSpecification.where((root, query, cb) -> {
            ParameterExpression<Collection> param = cb.parameter(Collection.class, LOCATION_IDS_PARAM);
            return cb.in(root.get(Landing.Fields.LOCATION).get(IEntity.Fields.ID)).value(param);
        }).addBind(LOCATION_IDS_PARAM, Arrays.asList(locationIds));
    }

    default Specification<Landing> hasVesselId(Integer vesselId) {
        if (vesselId == null) return null;
        return BindableSpecification.where((root, query, cb) -> {
            ParameterExpression<Integer> param = cb.parameter(Integer.class, VESSEL_ID_PARAM);
            return cb.equal(root.get(Landing.Fields.VESSEL).get(IEntity.Fields.ID), param);
        }).addBind(VESSEL_ID_PARAM, vesselId);
    }

    default Specification<Landing> hasExcludeVesselIds(Integer... excludeVesselIds) {
        if (ArrayUtils.isEmpty(excludeVesselIds)) return null;
        return hasExcludeVesselIds(Arrays.asList(excludeVesselIds));
    }

    default Specification<Landing> hasExcludeVesselIds(List<Integer> excludeVesselIds) {
        if (CollectionUtils.isNotEmpty(excludeVesselIds)) return null;
        return BindableSpecification.where((root, query, cb) -> {
            ParameterExpression<Collection> param = cb.parameter(Collection.class, EXCLUDE_VESSEL_IDS_PARAM);
            return cb.not(root.get(Landing.Fields.VESSEL).get(IEntity.Fields.ID).in(param));
        }).addBind(EXCLUDE_VESSEL_IDS_PARAM, excludeVesselIds);
    }

    // fixme : not used but could be mixed with TripSpecifications & PhysicalGearSpecifications
    default Specification<Landing> betweenDate(Date startDate, Date endDate) {
        if (startDate == null && endDate == null) return null;
        return (root, query, cb) -> {
            // Start + end date
            if (startDate != null && endDate != null) {
                return cb.and(
                    cb.greaterThanOrEqualTo(root.get(Landing.Fields.DATE_TIME), startDate),
                    cb.lessThanOrEqualTo(root.get(Landing.Fields.DATE_TIME), endDate)
                );
            }
            // Start date
            else if (startDate != null) {
                return cb.greaterThanOrEqualTo(root.get(Landing.Fields.DATE_TIME), startDate);
            }
            // End date
            else {
                return cb.lessThanOrEqualTo(root.get(Landing.Fields.DATE_TIME), endDate);
            }
        };
    }

    List<LandingVO> findAllByObservedLocationId(int observedLocationId, Page page, LandingFetchOptions fetchOptions);

    List<LandingVO> findAllByObservedLocationId(int observedLocationId);

    List<LandingVO> saveAllByObservedLocationId(int observedLocationId, List<LandingVO> sources);

    List<LandingVO> findAllByTripIds(List<Integer> tripIds);
}

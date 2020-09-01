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
import net.sumaris.core.dao.technical.jpa.BindableSpecification;
import net.sumaris.core.dao.technical.model.IEntity;
import net.sumaris.core.model.data.Landing;
import net.sumaris.core.vo.data.LandingVO;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.ParameterExpression;
import java.util.Collection;
import java.util.Date;
import java.util.List;

public interface LandingSpecifications extends RootDataSpecifications<Landing> {

    String OBSERVED_LOCATION_ID_PARAM = "observedLocationId";
    String TRIP_ID_PARAM = "tripId";
    String TRIP_IDS_PARAM = "tripIds";
    String TRIP_IDS_SET_PARAM = "tripIdsSet";
    String LOCATION_ID_PARAM = "locationId";
    String VESSEL_ID_PARAM = "vesselId";
    String EXCLUDE_VESSEL_IDS_PARAM = "excludeVesselIds";
    String EXCLUDE_VESSEL_IDS_SET_PARAM = "excludeVesselIdsSet";

    default Specification<Landing> hasObservedLocationId(Integer observedLocationId) {
        BindableSpecification<Landing> specification = BindableSpecification.where((root, query, criteriaBuilder) -> {
            ParameterExpression<Integer> param = criteriaBuilder.parameter(Integer.class, OBSERVED_LOCATION_ID_PARAM);
            return criteriaBuilder.or(
                criteriaBuilder.isNull(param),
                criteriaBuilder.equal(root.get(Landing.Fields.OBSERVED_LOCATION).get(IEntity.Fields.ID), param)
            );
        });
        specification.addBind(OBSERVED_LOCATION_ID_PARAM, observedLocationId);
        return specification;
    }

    default Specification<Landing> hasTripId(Integer tripId) {
        BindableSpecification<Landing> specification = BindableSpecification.where((root, query, criteriaBuilder) -> {
            ParameterExpression<Integer> param = criteriaBuilder.parameter(Integer.class, TRIP_ID_PARAM);
            return criteriaBuilder.or(
                criteriaBuilder.isNull(param),
                criteriaBuilder.equal(root.get(Landing.Fields.TRIP).get(IEntity.Fields.ID), param)
            );
        });
        specification.addBind(TRIP_ID_PARAM, tripId);
        return specification;
    }

    default Specification<Landing> hasTripIds(Collection<Integer> tripIds) {
        BindableSpecification<Landing> specification = BindableSpecification.where((root, query, criteriaBuilder) -> {
            ParameterExpression<Boolean> setParam = criteriaBuilder.parameter(Boolean.class, TRIP_IDS_SET_PARAM);
            ParameterExpression<Collection> param = criteriaBuilder.parameter(Collection.class, TRIP_IDS_PARAM);
            return criteriaBuilder.or(
                criteriaBuilder.isFalse(setParam),
                criteriaBuilder.in(root.get(Landing.Fields.TRIP).get(IEntity.Fields.ID)).value(param)
            );
        });
        specification.addBind(TRIP_IDS_SET_PARAM, CollectionUtils.isNotEmpty(tripIds));
        specification.addBind(TRIP_IDS_PARAM, CollectionUtils.isEmpty(tripIds) ? null : tripIds);
        return specification;
    }

    default Specification<Landing> hasLocationId(Integer locationId) {
        BindableSpecification<Landing> specification = BindableSpecification.where((root, query, criteriaBuilder) -> {
            ParameterExpression<Integer> param = criteriaBuilder.parameter(Integer.class, LOCATION_ID_PARAM);
            return criteriaBuilder.or(
                criteriaBuilder.isNull(param),
                criteriaBuilder.equal(root.get(Landing.Fields.LOCATION).get(IEntity.Fields.ID), param)
            );
        });
        specification.addBind(LOCATION_ID_PARAM, locationId);
        return specification;
    }

    default Specification<Landing> hasVesselId(Integer vesselId) {
        BindableSpecification<Landing> specification = BindableSpecification.where((root, query, criteriaBuilder) -> {
            ParameterExpression<Integer> param = criteriaBuilder.parameter(Integer.class, VESSEL_ID_PARAM);
            return criteriaBuilder.or(
                criteriaBuilder.isNull(param),
                criteriaBuilder.equal(root.get(Landing.Fields.VESSEL).get(IEntity.Fields.ID), param)
            );
        });
        specification.addBind(VESSEL_ID_PARAM, vesselId);
        return specification;
    }

    default Specification<Landing> hasExcludeVesselIds(List<Integer> excludeVesselIds) {
        BindableSpecification<Landing> specification = BindableSpecification.where((root, query, criteriaBuilder) -> {
            ParameterExpression<Boolean> setParam = criteriaBuilder.parameter(Boolean.class, EXCLUDE_VESSEL_IDS_SET_PARAM);
            ParameterExpression<Collection> param = criteriaBuilder.parameter(Collection.class, EXCLUDE_VESSEL_IDS_PARAM);
            return criteriaBuilder.or(
                criteriaBuilder.isFalse(setParam),
                criteriaBuilder.not(root.get(Landing.Fields.VESSEL).get(IEntity.Fields.ID).in(param))
            );
        });
        specification.addBind(EXCLUDE_VESSEL_IDS_SET_PARAM, CollectionUtils.isNotEmpty(excludeVesselIds));
        specification.addBind(EXCLUDE_VESSEL_IDS_PARAM, CollectionUtils.isEmpty(excludeVesselIds) ? null : excludeVesselIds);
        return specification;
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

    // Not tested
    List<LandingVO> saveAllByObservedLocationId(int observedLocationId, List<LandingVO> sources);

    List<LandingVO> findAllByTripIds(List<Integer> tripIds);
}

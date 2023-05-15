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
import net.sumaris.core.dao.technical.Daos;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.dao.technical.jpa.BindableSpecification;
import net.sumaris.core.model.IEntity;
import net.sumaris.core.model.data.*;
import net.sumaris.core.model.referential.pmfm.PmfmEnum;
import net.sumaris.core.vo.data.LandingFetchOptions;
import net.sumaris.core.vo.data.LandingVO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.*;
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

    String STRATEGY_LABELS = "strategyLabels";
    String SAMPLE_LABELS = "sampleLabels";
    String SAMPLE_TAG_IDS = "sampleTagIds";

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

    default Specification<Landing> betweenDate(Date startDate, Date endDate) {
        if (startDate == null && endDate == null) return null;
        return (root, query, cb) -> {
            // Start + end date
            if (startDate != null && endDate != null) {
                return cb.and(
                    cb.not(cb.lessThan(root.get(Landing.Fields.DATE_TIME), startDate)),
                    cb.not(cb.greaterThan(root.get(Landing.Fields.DATE_TIME), endDate))
                );
            }
            // Start date only
            else if (startDate != null) {
                return cb.greaterThanOrEqualTo(root.get(Landing.Fields.DATE_TIME), startDate);
            }
            // End date only
            else {
                return cb.lessThanOrEqualTo(root.get(Landing.Fields.DATE_TIME), endDate);
            }
        };
    }

    default Specification<Landing> hasStrategyLabels(String[] strategyLabels) {
        if (ArrayUtils.isEmpty(strategyLabels)) return null;

        // Check if pmfm STRATEGY_LABEL has been resolved
        final Integer strategyLabelPmfmId = PmfmEnum.STRATEGY_LABEL.getId();
        if (strategyLabelPmfmId == null || strategyLabelPmfmId.intValue() == -1) {
            return null;
        }

        return BindableSpecification.where((root, query, cb) -> {
            ParameterExpression<Collection> param = cb.parameter(Collection.class, STRATEGY_LABELS);

            // Add distinct, because of left join
            query.distinct(true);

            // Search by Trip -> Operation -> Sample
            ListJoin<Sample, LandingMeasurement> landingMeasurements = Daos.composeJoinList(root, Landing.Fields.LANDING_MEASUREMENTS, JoinType.LEFT);
            return cb.and(
                cb.equal(landingMeasurements.get(LandingMeasurement.Fields.PMFM), strategyLabelPmfmId),
                cb.in(landingMeasurements.get(LandingMeasurement.Fields.ALPHANUMERICAL_VALUE)).value(param)
            );
        })
        .addBind(STRATEGY_LABELS, Arrays.asList(strategyLabels));
    }

    default Specification<Landing> hasSampleLabels(String[] sampleLabels, boolean enableAdagioOptimization) {
        if (ArrayUtils.isEmpty(sampleLabels)) return null;

        return BindableSpecification.where((root, query, cb) -> {
                ParameterExpression<String> param = cb.parameter(String.class, SAMPLE_LABELS);

                // Add distinct, because of left join
                query.distinct(true);

                // Search by Trip -> Operation -> Sample
                Join<Landing, Trip> trip = Daos.composeJoin(root, Landing.Fields.TRIP, JoinType.LEFT);
                ListJoin<Trip, Operation> operations = Daos.composeJoinList(trip, Trip.Fields.OPERATIONS, JoinType.LEFT);
                ListJoin<Operation, Sample> opSamples = Daos.composeJoinList(operations, Operation.Fields.SAMPLES, JoinType.LEFT);
                Predicate searchByUndefinedOperation = cb.and(
                    cb.equal(trip.get(Trip.Fields.DEPARTURE_DATE_TIME), operations.get(Operation.Fields.START_DATE_TIME)),
                    cb.equal(trip.get(Trip.Fields.RETURN_DATE_TIME), operations.get(Operation.Fields.END_DATE_TIME)),
                    cb.in(opSamples.get(Sample.Fields.LABEL)).value(param)
                );

                // When running on an Adagio database, skip searching by Landing -> Sample
                // (because sample are always linked to an undefined operation)
                if (enableAdagioOptimization) return searchByUndefinedOperation;

                // Search by Landing -> Sample
                ListJoin<Landing, Sample> landingSamples = Daos.composeJoinList(root, Landing.Fields.SAMPLES, JoinType.LEFT);
                Predicate searchByLanding = cb.and(
                    cb.in(landingSamples.get(Sample.Fields.LABEL)).value(param)
                );

                return cb.or(
                    searchByUndefinedOperation,
                    searchByLanding
                );
            })
            .addBind(SAMPLE_LABELS, Arrays.asList(sampleLabels));
    }

    default Specification<Landing> hasSampleTagIds(String[] sampleTagIds, boolean enableAdagioOptimization) {
        if (ArrayUtils.isEmpty(sampleTagIds)) return null;

        // Check if pmfm TAG_ID has been resolved
        final Integer tagIdPmfmId = PmfmEnum.TAG_ID.getId();
        if (tagIdPmfmId == null || tagIdPmfmId.intValue() == -1) {
            return null;
        }

        return BindableSpecification.where((root, query, cb) -> {
            ParameterExpression<Collection> param = cb.parameter(Collection.class, SAMPLE_TAG_IDS);

            // Add distinct, because of left join
            query.distinct(true);

            // Search by Trip -> Operation -> Sample
            Join<Landing, Trip> trip = Daos.composeJoin(root, Landing.Fields.TRIP, JoinType.LEFT);
            ListJoin<Trip, Operation> operations = Daos.composeJoinList(trip, Trip.Fields.OPERATIONS, JoinType.LEFT);
            ListJoin<Operation, Sample> opSamples = Daos.composeJoinList(operations, Operation.Fields.SAMPLES, JoinType.LEFT);
            ListJoin<Sample, SampleMeasurement> opSampleMeasurements = Daos.composeJoinList(opSamples, Sample.Fields.MEASUREMENTS, JoinType.LEFT);
            Predicate searchByUndefinedOperation = cb.and(
                cb.equal(trip.get(Trip.Fields.DEPARTURE_DATE_TIME), operations.get(Operation.Fields.START_DATE_TIME)),
                cb.equal(trip.get(Trip.Fields.RETURN_DATE_TIME), operations.get(Operation.Fields.END_DATE_TIME)),
                cb.equal(opSampleMeasurements.get(SampleMeasurement.Fields.PMFM), tagIdPmfmId),
                cb.in(opSampleMeasurements.get(SampleMeasurement.Fields.ALPHANUMERICAL_VALUE)).value(param)
            );

            // When running on an Adagio database, skip searching by Landing -> Sample
            // (because sample are always linked to an undefined operation)
            if (enableAdagioOptimization) return searchByUndefinedOperation;

            // Search by Landing -> Sample
            ListJoin<Landing, Sample> landingSamples = Daos.composeJoinList(root, Landing.Fields.SAMPLES, JoinType.LEFT);
            ListJoin<Sample, SampleMeasurement> landingSampleMeasurements = Daos.composeJoinList(landingSamples, Sample.Fields.MEASUREMENTS, JoinType.LEFT);
            Predicate searchByLanding = cb.and(
                cb.equal(landingSampleMeasurements.get(SampleMeasurement.Fields.PMFM), tagIdPmfmId),
                cb.in(landingSampleMeasurements.get(SampleMeasurement.Fields.ALPHANUMERICAL_VALUE)).value(param)
            );

            return cb.or(
                searchByUndefinedOperation,
                searchByLanding
            );
        })
        .addBind(SAMPLE_TAG_IDS, Arrays.asList(sampleTagIds));
    }

    List<LandingVO> findAllByObservedLocationId(int observedLocationId, Page page, LandingFetchOptions fetchOptions);

    List<LandingVO> findAllByObservedLocationId(int observedLocationId);

    List<LandingVO> saveAllByObservedLocationId(int observedLocationId, List<LandingVO> sources);

    List<LandingVO> findAllByTripIds(List<Integer> tripIds);
}

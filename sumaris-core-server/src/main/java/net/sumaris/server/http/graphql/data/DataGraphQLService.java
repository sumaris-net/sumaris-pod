package net.sumaris.server.http.graphql.data;

/*-
 * #%L
 * SUMARiS:: Server
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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import io.leangen.graphql.annotations.*;
import io.leangen.graphql.generator.mapping.common.MapToListTypeAdapter;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.model.data.Trip;
import net.sumaris.core.service.data.*;
import net.sumaris.core.service.data.sample.SampleService;
import net.sumaris.core.service.referential.PmfmService;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.data.*;
import net.sumaris.core.vo.filter.OperationFilterVO;
import net.sumaris.core.vo.filter.TripFilterVO;
import net.sumaris.core.vo.filter.VesselFilterVO;
import net.sumaris.core.vo.referential.PmfmVO;
import net.sumaris.server.service.technical.ChangesPublisherService;
import net.sumaris.server.service.administration.ImageService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional
public class DataGraphQLService {

    private static final Log log = LogFactory.getLog(DataGraphQLService.class);

    @Autowired
    private VesselService vesselService;

    @Autowired
    private TripService tripService;

    @Autowired
    private SaleService saleService;

    @Autowired
    private OperationService operationService;

    @Autowired
    private VesselPositionService vesselPositionService;

    @Autowired
    private SampleService sampleService;

    @Autowired
    private MeasurementService measurementService;

    @Autowired
    private PmfmService pmfmService;

    @Autowired
    protected PhysicalGearService physicalGearService;

    @Autowired
    private ImageService imageService;

    @Autowired
    private ChangesPublisherService changesPublisherService;

    /* -- Vessel -- */


    @GraphQLQuery(name = "vessels", description = "Search in vessels")
    @Transactional(readOnly = true)
    public List<VesselFeaturesVO> findVesselsByFilter(@GraphQLArgument(name = "filter") VesselFilterVO filter,
                                                      @GraphQLArgument(name = "offset", defaultValue = "0") Integer offset,
                                                      @GraphQLArgument(name = "size", defaultValue = "1000") Integer size,
                                                      @GraphQLArgument(name = "sortBy", defaultValue = VesselFeaturesVO.PROPERTY_EXTERIOR_MARKING) String sort,
                                                      @GraphQLArgument(name = "sortDirection", defaultValue = "asc") String direction
    ) {
        return vesselService.findByFilter(filter, offset, size, sort,
                direction != null ? SortDirection.valueOf(direction.toUpperCase()) : null);
    }

    @GraphQLMutation(name = "saveVessel", description = "Create or update a vessel")
    public VesselFeaturesVO saveVessel(@GraphQLArgument(name = "vessel") VesselFeaturesVO vessel) {
        return vesselService.save(vessel);
    }

    @GraphQLMutation(name = "saveVessels", description = "Create or update many vessels")
    public List<VesselFeaturesVO> saveVessels(@GraphQLArgument(name = "vessels") List<VesselFeaturesVO> vessels) {
        return vesselService.save(vessels);
    }

    @GraphQLMutation(name = "deleteVessel", description = "Delete a vessel (by vessel features id)")
    public void deleteVessel(@GraphQLArgument(name = "id") int id) {
        vesselService.delete(id);
    }

    @GraphQLMutation(name = "deleteVessels", description = "Delete many vessels (by vessel features ids)")
    public void deleteVessels(@GraphQLArgument(name = "ids") List<Integer> ids) {
        vesselService.delete(ids);
    }


    /* -- Trip -- */

    @GraphQLQuery(name = "trips", description = "Search in trips")
    @Transactional(readOnly = true)
    public List<TripVO> findTripsByFilter(@GraphQLArgument(name = "filter") TripFilterVO filter,
                                          @GraphQLArgument(name = "offset", defaultValue = "0") Integer offset,
                                          @GraphQLArgument(name = "size", defaultValue = "1000") Integer size,
                                          @GraphQLArgument(name = "sortBy", defaultValue = TripVO.PROPERTY_DEPARTURE_DATE_TIME) String sort,
                                          @GraphQLArgument(name = "sortDirection", defaultValue = "asc") String direction,
                                          @GraphQLEnvironment() Set<String> fields
                                  ) {
        final List<TripVO> result = tripService.findByFilter(filter, offset, size, sort,
                direction != null ? SortDirection.valueOf(direction.toUpperCase()) : null);

        // Add image if need
        if (hasImageField(fields)) fillImages(result);

        // Add vessel if need
        if (hasVesselFeaturesField(fields)) {
            result.stream().forEach( t -> {
                if (t.getVesselFeatures().getVesselId() != null) {
                    t.setVesselFeatures(vesselService.getByVesselIdAndDate(t.getVesselFeatures().getVesselId(), t.getDepartureDateTime()));
                }
            });
        }

        return result;
    }

    // FOR DEV ONLY: Full access to database model
    //@GraphQLQuery(name = "model_trip", description = "Get a trip, by id")
    //@Transactional(readOnly = true)
    //public Trip getTrip(@GraphQLArgument(name = "id") int id) {
    //    return tripService.get(id, Trip.class);
    //}

    @GraphQLQuery(name = "trip", description = "Get a trip, by id")
    @Transactional(readOnly = true)
    public TripVO getTripById(@GraphQLArgument(name = "id") int id,
                              @GraphQLEnvironment() Set<String> fields) {
        final TripVO result = tripService.get(id);

        // Add image if need
        if (hasImageField(fields)) fillImages(result);

        // Add vessel if need
        if (hasVesselFeaturesField(fields) && result.getVesselFeatures() != null && result.getVesselFeatures().getVesselId() != null) {
            result.setVesselFeatures(vesselService.getByVesselIdAndDate(result.getVesselFeatures().getVesselId(), result.getDepartureDateTime()));
        }

        return result;
    }

    @GraphQLMutation(name = "saveTrip", description = "Create or update a trip")
    public TripVO saveTrip(@GraphQLArgument(name = "trip") TripVO trip, @GraphQLEnvironment() Set<String> fields) {
        final TripVO result = tripService.save(trip);

        // Add image if need
        if (hasImageField(fields)) fillImages(result);

        // Add vessel if need
        if (hasVesselFeaturesField(fields) && result.getVesselFeatures() != null && result.getVesselFeatures().getVesselId() != null) {
            result.setVesselFeatures(vesselService.getByVesselIdAndDate(result.getVesselFeatures().getVesselId(), result.getDepartureDateTime()));
        }

        return result;
    }

    @GraphQLMutation(name = "saveTrips", description = "Create or update many trips")
    public List<TripVO> saveTrips(@GraphQLArgument(name = "trips") List<TripVO> trips, @GraphQLEnvironment() Set<String> fields) {
        final List<TripVO> result = tripService.save(trips);

        // Add image if need
        if (hasImageField(fields)) fillImages(result);

        // Add vessel if need
        if (hasVesselFeaturesField(fields)) {
            result.stream().forEach( t -> {
                if (t.getVesselFeatures().getVesselId() != null) {
                    t.setVesselFeatures(vesselService.getByVesselIdAndDate(t.getVesselFeatures().getVesselId(), t.getDepartureDateTime()));
                }
            });
        }

        return result;
    }

    @GraphQLMutation(name = "deleteTrip", description = "Delete a trip")
    public void deleteTrip(@GraphQLArgument(name = "id") int id) {
        tripService.delete(id);
    }

    @GraphQLMutation(name = "deleteTrips", description = "Delete many trips")
    public void deleteTrips(@GraphQLArgument(name = "ids") List<Integer> ids) {
        tripService.delete(ids);
    }

    @GraphQLSubscription(name = "updateTrip", description = "Subcribe to a trip update")
    public Publisher<TripVO> updateTrip(@GraphQLArgument(name = "tripId") final int tripId,
                                        @GraphQLArgument(name = "interval", defaultValue = "30", description = "Minimum interval to get changes, in seconds.") final Integer minIntervalInSecond) {

        Preconditions.checkArgument(tripId >= 0, "Invalid tripId");
        return changesPublisherService.getPublisher(Trip.class, TripVO.class, tripId, minIntervalInSecond, true);
    }

    @GraphQLQuery(name = "gears", description = "Get operation's gears")
    public List<PhysicalGearVO> getGearsByTrip(@GraphQLContext TripVO trip) {
        return physicalGearService.getPhysicalGearByTripId(trip.getId());
    }

    /* -- Sales -- */

    @GraphQLQuery(name = "sales", description = "Get trip's sales")
    public List<SaleVO> getSalesByTrip(@GraphQLContext TripVO trip) {
        return saleService.getAllByTripId(trip.getId());
    }

    @GraphQLQuery(name = "sale", description = "Get trip's unique sale")
    public SaleVO getUniqueSaleByTrip(@GraphQLContext TripVO trip) {
        List<SaleVO> sales = saleService.getAllByTripId(trip.getId());
        return CollectionUtils.isEmpty(sales) ? null : CollectionUtils.extractSingleton(sales);
    }

    /* -- Operations -- */

    @GraphQLQuery(name = "operations", description = "Get trip's operations")
    @Transactional(readOnly = true)
    public List<OperationVO> getOperationsByTripId(@GraphQLArgument(name = "filter") OperationFilterVO filter,
                                                   @GraphQLArgument(name = "offset", defaultValue = "0") Integer offset,
                                                   @GraphQLArgument(name = "size", defaultValue = "1000") Integer size,
                                                   @GraphQLArgument(name = "sortBy", defaultValue = OperationVO.PROPERTY_START_DATE_TIME) String sort,
                                                   @GraphQLArgument(name = "sortDirection", defaultValue = "asc") String direction) {
        Preconditions.checkNotNull(filter, "Missing filter or filter.tripId");
        Preconditions.checkNotNull(filter.getTripId(), "Missing filter or filter.tripId");
        return operationService.getAllByTripId(filter.getTripId(), offset, size, sort, direction != null ? SortDirection.valueOf(direction.toUpperCase()) : null);
    }

    @GraphQLQuery(name = "operations", description = "Get trip's operations")
    public List<OperationVO> getOperationsByTrip(@GraphQLContext TripVO trip) {
        return operationService.getAllByTripId(trip.getId(), 0, 100, OperationVO.PROPERTY_START_DATE_TIME, SortDirection.ASC);
    }

    @GraphQLQuery(name = "operation", description = "Get an operation")
    @Transactional(readOnly = true)
    public OperationVO getOperation(@GraphQLArgument(name = "id") int id) {
        return operationService.get(id);
    }

    @GraphQLMutation(name = "saveOperations", description = "Save operations")
    public List<OperationVO> saveOperations(@GraphQLArgument(name = "operations") List<OperationVO> operations) {
        return operationService.save(operations);
    }

    @GraphQLMutation(name = "saveOperation", description = "Create or update an operation")
    public OperationVO saveOperation(@GraphQLArgument(name = "operation") OperationVO operation) {
        return operationService.save(operation);
    }

    @GraphQLMutation(name = "deleteOperation", description = "Delete an operation")
    public void deleteOperation(@GraphQLArgument(name = "id") int id) {
        operationService.delete(id);
    }

    @GraphQLMutation(name = "deleteOperations", description = "Delete many operations")
    public void deleteOperations(@GraphQLArgument(name = "ids") List<Integer> ids) {
        operationService.delete(ids);
    }

    /* -- Vessel position -- */

    @GraphQLQuery(name = "positions", description = "Get operation's position")
    public List<VesselPositionVO> getPositionsByOperation(@GraphQLContext OperationVO operation) {
        return vesselPositionService.getAllByOperationId(operation.getId(), 0, 100, VesselPositionVO.PROPERTY_DATE_TIME, SortDirection.ASC);
    }

    /* -- Sample -- */

    @GraphQLQuery(name = "samples", description = "Get operation's samples")
    public List<SampleVO> getSamplesByOperation(@GraphQLContext OperationVO operation) {
        return sampleService.getAllByOperationId(operation.getId());
    }

    /* -- Measurements -- */

    @GraphQLQuery(name = "measurements", description = "Get trip's measurements")
    public List<MeasurementVO> getTripMeasurements(@GraphQLContext TripVO trip) {
        return measurementService.getVesselUseMeasurementsByTripId(trip.getId());
    }

    @GraphQLQuery(name = "measurements", description = "Get operation's measurements")
    public List<MeasurementVO> getOperationMeasurements(@GraphQLContext OperationVO operation) {
        return measurementService.getVesselUseMeasurementsByOperationId(operation.getId());
    }

    @GraphQLQuery(name = "gearMeasurements", description = "Get operation's gear measurements")
    public List<MeasurementVO> getOperationGearUseMeasurements(@GraphQLContext OperationVO operation) {
        return measurementService.getGearUseMeasurementsByOperationId(operation.getId());
    }

    @GraphQLQuery(name = "measurements", description = "Get physical gear measurements")
    public List<MeasurementVO> getPhysicalGearMeasurements(@GraphQLContext PhysicalGearVO physicalGear) {
        return measurementService.getPhysicalGearMeasurements(physicalGear.getId());
    }

    @GraphQLQuery(name = "measurements", description = "Get sample measurements")
    public List<MeasurementVO> getSampleMeasurements(@GraphQLContext SampleVO sample) {
        return measurementService.getSampleMeasurements(sample.getId());
    }

    @GraphQLQuery(name = "measurementValues", description = "Get measurement values (as a key/value map, using pmfmId as key)")
    public Map<Integer, String> getSampleMeasurementValues(@GraphQLContext SampleVO sample) {
        return measurementService.getSampleMeasurementsMap(sample.getId());
    }

    @GraphQLQuery(name = "measurementValues", description = "Get measurement values (as a key/value map, using pmfmId as key)")
    public Map<Integer, String> getBatchMeasurementValues(@GraphQLContext BatchVO sample) {
        Map<Integer, String> map = Maps.newHashMap();
        map.putAll(measurementService.getBatchSortingMeasurementsMap(sample.getId()));
        map.putAll(measurementService.getBatchQuantificationMeasurementsMap(sample.getId()));
        return map;
    }

//    @GraphQLQuery(name = "measurementValues", description = "Get measurement values (as a key/value map, using pmfmId as key)")
//    public List<Map.Entry<Integer, Object>> getBatchMeasurementsValues(@GraphQLContext BatchVO sample) {
//        Map<Integer, Object> map = Maps.newHashMap();
//        map.putAll(measurementService.getBatchSortingMeasurementsMap(sample.getId()));
//        map.putAll(measurementService.getBatchQuantificationMeasurementsMap(sample.getId()));
//        return ImmutableList.copyOf(map.entrySet());
//    }

    // TODO: remove if not used
//    @GraphQLQuery(name = "sortingMeasurementValues", description = "Get sorting measurement values (as a key/value map, using pmfmId as key)")
//    public Map<Integer, Object> getSortingMeasurementsMap(@GraphQLContext BatchVO sample) {
//        return measurementService.getBatchSortingMeasurementsMap(sample.getId());
//    }
//    @GraphQLQuery(name = "quantificationMeasurementValues", description = "Get quantification measurement values (as a key/value map, using pmfmId as key)")
//    public Map<Integer, Object> getQuantificationMeasurementsMap(@GraphQLContext BatchVO sample) {
//        return measurementService.getBatchQuantificationMeasurementsMap(sample.getId());
//    }

    @GraphQLQuery(name = "pmfm", description = "Get measurement's pmfm")
    public PmfmVO getMeasurementPmfm(@GraphQLContext MeasurementVO measurement) {
        return pmfmService.get(measurement.getPmfmId());
    }

    /* -- protected methods -- */

    protected boolean hasImageField(Set<String> fields) {
        return fields.contains(TripVO.PROPERTY_RECORDER_DEPARTMENT + "/" + DepartmentVO.PROPERTY_LOGO) ||
                fields.contains(TripVO.PROPERTY_RECORDER_PERSON + "/" + PersonVO.PROPERTY_AVATAR);
    }

    protected boolean hasVesselFeaturesField(Set<String> fields) {
        return fields.contains(TripVO.PROPERTY_VESSEL_FEATURES + "/" + VesselFeaturesVO.PROPERTY_EXTERIOR_MARKING)
                || fields.contains(TripVO.PROPERTY_VESSEL_FEATURES + "/" + VesselFeaturesVO.PROPERTY_NAME);
    }

    protected List<TripVO> fillImages(final List<TripVO> results) {
        results.forEach(this::fillImages);
        return results;
    }

    protected TripVO fillImages(TripVO result) {
        if (result != null) {

            // Fill avatar on recorder department (if not null)
            imageService.fillLogo(result.getRecorderDepartment());

            // Fill avatar on recorder persons (if not null)
            imageService.fillAvatar(result.getRecorderPerson());
        }

        return result;
    }
}

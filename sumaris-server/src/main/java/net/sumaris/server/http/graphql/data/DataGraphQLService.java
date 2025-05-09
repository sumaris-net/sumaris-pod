/*
 * #%L
 * SUMARiS
 * %%
 * Copyright (C) 2019 SUMARiS Consortium
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

package net.sumaris.server.http.graphql.data;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import io.leangen.graphql.annotations.*;
import io.leangen.graphql.execution.ResolutionEnvironment;
import io.reactivex.rxjava3.core.BackpressureStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.referential.metier.MetierRepository;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.dao.technical.Pageables;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.event.config.ConfigurationReadyEvent;
import net.sumaris.core.event.config.ConfigurationUpdatedEvent;
import net.sumaris.core.exception.UnauthorizedException;
import net.sumaris.core.model.IEntity;
import net.sumaris.core.model.administration.programStrategy.ProgramEnum;
import net.sumaris.core.model.administration.samplingScheme.SamplingStrata;
import net.sumaris.core.model.data.*;
import net.sumaris.core.model.referential.ObjectTypeEnum;
import net.sumaris.core.service.administration.programStrategy.ProgramService;
import net.sumaris.core.service.data.*;
import net.sumaris.core.service.data.activity.ActivityCalendarService;
import net.sumaris.core.service.data.activity.DailyActivityCalendarService;
import net.sumaris.core.service.data.denormalize.DenormalizedBatchService;
import net.sumaris.core.service.data.denormalize.DenormalizedTripResultVO;
import net.sumaris.core.service.data.denormalize.DenormalizedTripService;
import net.sumaris.core.service.data.vessel.VesselService;
import net.sumaris.core.service.referential.ReferentialService;
import net.sumaris.core.service.referential.pmfm.PmfmService;
import net.sumaris.core.util.ArrayUtils;
import net.sumaris.core.util.Beans;
import net.sumaris.core.util.Dates;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.data.*;
import net.sumaris.core.vo.data.activity.*;
import net.sumaris.core.vo.data.aggregatedLanding.AggregatedLandingVO;
import net.sumaris.core.vo.data.batch.*;
import net.sumaris.core.vo.data.sample.SampleFetchOptions;
import net.sumaris.core.vo.data.sample.SampleVO;
import net.sumaris.core.vo.filter.*;
import net.sumaris.core.vo.referential.ReferentialVO;
import net.sumaris.core.vo.referential.metier.MetierVO;
import net.sumaris.core.vo.referential.pmfm.PmfmVO;
import net.sumaris.server.config.SumarisServerConfiguration;
import net.sumaris.server.http.graphql.GraphQLApi;
import net.sumaris.server.http.graphql.GraphQLHelper;
import net.sumaris.server.http.graphql.GraphQLUtils;
import net.sumaris.server.http.security.AuthService;
import net.sumaris.server.http.security.IsSupervisor;
import net.sumaris.server.http.security.IsUser;
import net.sumaris.server.service.administration.DataAccessControlService;
import net.sumaris.server.service.administration.ImageService;
import net.sumaris.server.service.technical.EntityWatchService;
import net.sumaris.server.service.technical.TrashService;
import org.apache.commons.collections4.CollectionUtils;
import org.nuiton.util.TimeLog;
import org.reactivestreams.Publisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.util.*;

@Service
@GraphQLApi
@Transactional
@RequiredArgsConstructor
@Slf4j
public class DataGraphQLService {


    private final TripService tripService;

    private final TrashService trashService;

    private final ObservedLocationService observedLocationService;

    private final OperationService operationService;

    private final OperationGroupService operationGroupService;

    private final LandingService landingService;

    private final AggregatedLandingService aggregatedLandingService;

    private final SaleService saleService;

    private final ExpectedSaleService expectedSaleService;

    private final VesselPositionService vesselPositionService;

    private final SampleService sampleService;

    private final BatchService batchService;

    private final DenormalizedTripService denormalizedTripService;

    private final DenormalizedBatchService denormalizedBatchService;

    private final MeasurementService measurementService;

    private final ActivityCalendarService activityCalendarService;

    private final DailyActivityCalendarService dailyActivityCalendarService;

    private final PmfmService pmfmService;

    protected final PhysicalGearService physicalGearService;

    private final ImageService imageService;

    private final EntityWatchService entityWatchService;

    private final ProductService productService;

    private final PacketService packetService;

    private final FishingAreaService fishingAreaService;

    private final AuthService authService;

    private final VesselService vesselService;

    private final VesselGraphQLService vesselGraphQLService;

    private final DataAccessControlService dataAccessControlService;

    private final ProgramService programService;

    private final MetierRepository metierRepository;

    private final ReferentialService referentialService;

    private final SumarisServerConfiguration configuration;

    private boolean enableImageAttachments = false;

    private final TimeLog timeLog = new TimeLog(DataGraphQLService.class);

    @PostConstruct
    @EventListener({ConfigurationReadyEvent.class, ConfigurationUpdatedEvent.class})
    public void onConfigurationReady() {
        this.enableImageAttachments = configuration.enableDataImages();
    }

    /* -- Trips -- */

    @GraphQLQuery(name = "trips", description = "Search in trips")
    @Transactional(readOnly = true)
    @IsUser
    public List<TripVO> findAllTrips(@GraphQLArgument(name = "filter") TripFilterVO filter,
                                     @GraphQLArgument(name = "offset", defaultValue = "0") Integer offset,
                                     @GraphQLArgument(name = "size", defaultValue = "1000") Integer size,
                                     @GraphQLArgument(name = "sortBy") String sort,
                                     @GraphQLArgument(name = "sortDirection", defaultValue = "desc") String direction,
                                     @GraphQLArgument(name = "trash", defaultValue = "false") Boolean trash,
                                     @GraphQLEnvironment() ResolutionEnvironment env
    ) {
        SortDirection sortDirection = SortDirection.fromString(direction, SortDirection.DESC);

        // Read from trash
        if (trash) {
            // Check user is admin
            dataAccessControlService.checkIsAdmin("Cannot access to trash");

            // Set default sort
            sort = sort != null ? sort : TripVO.Fields.UPDATE_DATE;

            // Call the trash service
            return trashService.findAll(Trip.class.getSimpleName(),
                    Page.builder().offset(offset).size(size).sortBy(sort).sortDirection(sortDirection).build(),
                    TripVO.class);
        }

        filter = fillRootDataFilter(filter, TripFilterVO.class);

        // Exclude trip with observed location
        if (filter.getHasObservedLocation() == null) {
            filter.setHasObservedLocation(false);
        }
        if (filter.getHasScientificCruise() == null) {
            filter.setHasScientificCruise(false);
        }

        // Set default sort
        // Remove default sortBy - fix IMAGINE issue (see app LandingService.fixLandingDates())
        //sort = sort != null ? sort : TripVO.Fields.DEPARTURE_DATE_TIME;

        Set<String> fields = GraphQLUtils.fields(env);

        long now = TimeLog.getTime();
        final List<TripVO> result = tripService.findAll(filter,
                Page.builder().offset(offset).size(size).sortBy(sort).sortDirection(sortDirection).build(),
                getTripFetchOptions(fields));

        // Add additional properties if needed
        fillTrips(result, fields);

        timeLog.log(now, "findAllTrips");


        return result;
    }

    @GraphQLQuery(name = "tripsCount", description = "Get trips count")
    @Transactional(readOnly = true)
    @IsUser
    public long countTrips(@GraphQLArgument(name = "filter") TripFilterVO filter,
                           @GraphQLArgument(name = "trash", defaultValue = "false") Boolean trash) {
        if (trash) {
            // Check user is admin
            dataAccessControlService.checkIsAdmin("Cannot access to trash");

            // Call the trash service
            return trashService.count(Trip.class.getSimpleName());
        }

        filter = fillRootDataFilter(filter, TripFilterVO.class);

        return tripService.countByFilter(filter);
    }

    @GraphQLQuery(name = "trip", description = "Get a trip, by id")
    @Transactional(readOnly = true)
    @IsUser
    public TripVO getTripById(@GraphQLNonNull @GraphQLArgument(name = "id") int id,
                              @GraphQLEnvironment ResolutionEnvironment env) {

        Set<String> fields = GraphQLUtils.fields(env);

        final TripVO result = tripService.get(id, getTripFetchOptions(fields));

        // Check read access
        dataAccessControlService.checkCanRead(result);

        // Add additional properties if needed
        fillTripFields(result, fields);

        return result;
    }

    @GraphQLQuery(name = "landing", description = "Get trip's landing")
    public LandingVO getTripLanding(@GraphQLContext TripVO trip, @GraphQLEnvironment ResolutionEnvironment env) {
        if (trip.getLanding() != null) return trip.getLanding();
        if (trip.getLandingId() == null) return null;

        LandingVO target = landingService.get(trip.getLandingId(), LandingFetchOptions.DEFAULT.toBuilder()
                .withTrip(false)
                .build());

        // Avoid trip to be reload from landing (in GraphQL fragment)
        target.setTrip(trip);
        target.setVesselSnapshot(trip.getVesselSnapshot());

        fillLandingFields(target, GraphQLUtils.fields(env));

        return target;
    }

    @GraphQLMutation(name = "saveTrip", description = "Create or update a trip")
    @IsUser
    public TripVO saveTrip(@GraphQLNonNull @GraphQLArgument(name = "trip") TripVO trip,
                           @GraphQLArgument(name = "options") TripSaveOptions options,
                           // Deprecated attributes
                           @GraphQLArgument(name = "saveOptions", description = "@deprecated Use options") TripSaveOptions saveOptions,
                           @GraphQLArgument(name = "withOperation", defaultValue = "false", description = "@deprecated Use options") Boolean withOperation,
                           // Env
                           @GraphQLEnvironment ResolutionEnvironment env) {

        if (options == null) {
            // For compat prior to 1.7
            if (saveOptions != null) {
                GraphQLHelper.logDeprecatedUse(authService, "saveTrip(TripVO, saveOptions)", "1.7.0");
                options = saveOptions;
            }
            // For compat prior to 1.5
            else if (withOperation != null) {
                GraphQLHelper.logDeprecatedUse(authService, "saveTrip(TripVO, withOperation)", "1.5.0");
                options = TripSaveOptions.builder()
                        .withOperation(withOperation)
                        .build();
            }
        }
        // Make sure user can write
        dataAccessControlService.checkCanWrite(trip);

        // Save
        final TripVO result = tripService.save(trip, options);

        // Add additional properties if needed
        fillTripFields(result, GraphQLUtils.fields(env));

        return result;
    }

    @GraphQLMutation(name = "saveTrips", description = "Create or update many trips")
    @IsUser
    public List<TripVO> saveTrips(@GraphQLNonNull @GraphQLArgument(name = "trips") List<TripVO> trips,
                                  @GraphQLArgument(name = "options") TripSaveOptions options,
                                  // Deprecated
                                  @GraphQLArgument(name = "saveOptions", description = "@deprecated Use options") TripSaveOptions saveOptions, // Deprecated
                                  @GraphQLArgument(name = "withOperation", defaultValue = "false", description = "@deprecated Use options") Boolean withOperation, // Deprecated
                                  // Env
                                  @GraphQLEnvironment ResolutionEnvironment env) {

        if (options == null) {
            // For compat prior to 1.7
            if (saveOptions != null) {
                GraphQLHelper.logDeprecatedUse(authService, "saveTrip(TripVO, saveOptions)", "1.7.0");
                options = saveOptions;
            }
            // For compat prior to 1.5
            else if (withOperation != null) {
                GraphQLHelper.logDeprecatedUse(authService, "saveTrip(TripVO, withOperation)", "1.5.0");
                options = TripSaveOptions.builder()
                        .withOperation(withOperation)
                        .build();
            }
        }
        // Make sure user can write
        dataAccessControlService.checkCanWriteAll(trips);

        // Save
        final List<TripVO> result = tripService.save(trips, options);

        // Add additional properties if needed
        fillTrips(result, GraphQLUtils.fields(env));

        return result;
    }

    @GraphQLMutation(name = "deleteTrip", description = "Delete a trip")
    @IsUser
    public void deleteTrip(@GraphQLArgument(name = "id") int id) {
        tripService.asyncDelete(id);
    }

    @GraphQLMutation(name = "deleteTrips", description = "Delete many trips")
    @IsUser
    public void deleteTrips(@GraphQLNonNull @GraphQLArgument(name = "ids") List<Integer> ids) {
        tripService.delete(ids);
    }

    @GraphQLSubscription(name = "updateTrip", description = "Subscribe to changes on a trip")
    @IsUser
    public Publisher<TripVO> updateTrip(@GraphQLNonNull @GraphQLArgument(name = "id") final int id,
                                        @GraphQLArgument(name = "interval", defaultValue = "30", description = "Minimum interval to find changes, in seconds.") final Integer minIntervalInSecond,
                                        @GraphQLEnvironment() ResolutionEnvironment env) {

        Preconditions.checkArgument(id >= 0, "Invalid id");
        Set<String> fields = GraphQLUtils.fields(env);
        return entityWatchService.watchEntity(Trip.class, TripVO.class, id, minIntervalInSecond, true)
                .toFlowable(BackpressureStrategy.LATEST)
                .map(t -> fillTripFields(t, fields));
    }

    @GraphQLMutation(name = "controlTrip", description = "Control a trip")
    @IsUser
    public TripVO controlTrip(@GraphQLNonNull @GraphQLArgument(name = "trip") TripVO trip, @GraphQLEnvironment ResolutionEnvironment env) {
        final TripVO result = tripService.control(trip);

        // Add additional properties if needed
        fillTripFields(result, GraphQLUtils.fields(env));

        return result;
    }

    @GraphQLMutation(name = "validateTrip", description = "Validate a trip")
    @IsSupervisor
    public TripVO validateTrip(@GraphQLNonNull @GraphQLArgument(name = "trip") TripVO trip, @GraphQLEnvironment ResolutionEnvironment env) {
        final TripVO result = tripService.validate(trip);

        // Add additional properties if needed
        fillTripFields(result, GraphQLUtils.fields(env));

        return result;
    }

    @GraphQLMutation(name = "unvalidateTrip", description = "Unvalidate a trip")
    @IsSupervisor
    public TripVO unvalidateTrip(@GraphQLNonNull @GraphQLArgument(name = "trip") TripVO trip, @GraphQLEnvironment ResolutionEnvironment env) {
        final TripVO result = tripService.unvalidate(trip);

        // Add additional properties if needed
        fillTripFields(result, GraphQLUtils.fields(env));

        return result;
    }

    @GraphQLMutation(name = "qualifyTrip", description = "Qualify a trip")
    @IsSupervisor
    public TripVO qualifyTrip(@GraphQLNonNull @GraphQLArgument(name = "trip") TripVO trip,
                              @GraphQLEnvironment ResolutionEnvironment env) {
        final TripVO result = tripService.qualify(trip);

        // Add additional properties if needed
        fillTripFields(result, GraphQLUtils.fields(env));

        return result;
    }

    @GraphQLQuery(name = "samplingStrata", description = "Get trip's sampling strata")
    public ReferentialVO getTripSamplingStrata(@GraphQLContext TripVO trip) {
        if (trip.getSamplingStrata() != null) return trip.getSamplingStrata();
        if (trip.getSamplingStrataId() == null) return null;

        return this.referentialService.get(SamplingStrata.class.getSimpleName(), trip.getSamplingStrataId());
    }

    /* -- Gears -- */


    @GraphQLQuery(name = "physicalGears", description = "Get physical gears")
    @Transactional(readOnly = true)
    @IsUser
    public List<PhysicalGearVO> findPhysicalGears(@GraphQLArgument(name = "filter") PhysicalGearFilterVO filter,
                                                  @GraphQLArgument(name = "offset", defaultValue = "0") Integer offset,
                                                  @GraphQLArgument(name = "size", defaultValue = "1000") Integer size,
                                                  @GraphQLArgument(name = "sortBy", defaultValue = PhysicalGearVO.Fields.RANK_ORDER) String sort,
                                                  @GraphQLArgument(name = "sortDirection", defaultValue = "asc") String direction,
                                                  @GraphQLEnvironment ResolutionEnvironment env) {
        Preconditions.checkNotNull(filter, "Missing filter");
        Preconditions.checkArgument(filter.getVesselId() != null || filter.getParentGearId() != null
            || ArrayUtils.isNotEmpty(filter.getVesselIds()), "Missing 'filter.vesselId', 'filter.vesselIds' or 'filter.parentGearId'");
        Page page = Page.builder().offset(offset)
                .size(size)
                .sortBy(sort)
                .sortDirection(SortDirection.fromString(direction))
                .build();
        return physicalGearService.findAll(filter, page, getFetchOptions(GraphQLUtils.fields(env)));
    }

    @GraphQLQuery(name = "gears", description = "Get operation's gears")
    public List<PhysicalGearVO> getGearsByTrip(@GraphQLContext TripVO trip) {
        if (trip.getGears() != null) return trip.getGears();
        if (trip.getId() == null) return null;
        return physicalGearService.getAllByTripId(trip.getId(), null);
    }

    @GraphQLQuery(name = "trip", description = "Get physical gear's trip")
    public TripVO getTripByPhysicalGearId(@GraphQLContext PhysicalGearVO physicalGear) {
        if (physicalGear.getTrip() != null) return physicalGear.getTrip();
        if (physicalGear.getTripId() == null) return null;
        return tripService.get(physicalGear.getTripId());
    }

    @GraphQLQuery(name = "physicalGear", description = "Get a physical gear")
    @Transactional(readOnly = true)
    @IsUser
    public PhysicalGearVO getPhysicalGear(@GraphQLArgument(name = "id") int id,
                                          @GraphQLEnvironment() ResolutionEnvironment env) {
        Set<String> fields = GraphQLUtils.fields(env);
        return physicalGearService.get(id, getFetchOptions(fields));
    }

    /* -- Metier -- */

    @GraphQLQuery(name = "metier", description = "Get operation's metier")
    public MetierVO getOperationMetier(@GraphQLContext OperationVO operation) {
        // Already fetch: use it
        if (operation.getMetier() == null || operation.getMetier().getTaxonGroup() != null) {
            return operation.getMetier();
        }
        Integer metierId = operation.getMetier().getId();
        if (metierId == null) return null;

        return metierRepository.get(metierId);
    }

    @GraphQLQuery(name = "metiers", description = "Get trip metiers")
    public List<MetierVO> getTripMetiers(@GraphQLContext TripVO trip) {
        if (trip.getMetiers() != null) {
            return trip.getMetiers();
        }
        return operationGroupService.getMetiersByTripId(trip.getId());
    }


    /* -- Observed location -- */

    @GraphQLQuery(name = "observedLocations", description = "Search in observed locations")
    @Transactional(readOnly = true)
    @IsUser
    public List<ObservedLocationVO> findObservedLocations(@GraphQLArgument(name = "filter") ObservedLocationFilterVO filter,
                                                          @GraphQLArgument(name = "offset", defaultValue = "0") Integer offset,
                                                          @GraphQLArgument(name = "size", defaultValue = "1000") Integer size,
                                                          @GraphQLArgument(name = "sortBy", defaultValue = ObservedLocationVO.Fields.START_DATE_TIME) String sort,
                                                          @GraphQLArgument(name = "sortDirection", defaultValue = "asc") String direction,
                                                          @GraphQLArgument(name = "trash", defaultValue = "false") Boolean trash,
                                                          @GraphQLEnvironment ResolutionEnvironment env
) {
        SortDirection sortDirection = SortDirection.fromString(direction, SortDirection.DESC);

        // Read from trash
        if (trash) {
            // Check user is admin
            dataAccessControlService.checkIsAdmin("Cannot access to trash");

            // Set default sort
            sort = sort != null ? sort : ObservedLocationVO.Fields.UPDATE_DATE;

            // Call the trash service
            return trashService.findAll(ObservedLocation.class.getSimpleName(),
                    Pageables.create(offset, size, sort, sortDirection),
                    ObservedLocationVO.class).getContent();
        }

        filter = fillRootDataFilter(filter, ObservedLocationFilterVO.class);
        Set<String> fields = GraphQLUtils.fields(env);

        long now = TimeLog.getTime();
        final List<ObservedLocationVO> result = observedLocationService.findAll(
                filter,
                offset, size, sort,
                sortDirection,
                getObservedLocationFetchOptions(fields));

        // Add additional properties if needed
        fillObservedLocationsFields(result, fields);

        timeLog.log(now, "findObservedLocations");

        return result;
    }

    @GraphQLQuery(name = "observedLocationsCount", description = "Get total number of observed locations")
    @Transactional(readOnly = true)
    @IsUser
    public long countObservedLocations(@GraphQLArgument(name = "filter") ObservedLocationFilterVO filter,
                                       @GraphQLArgument(name = "trash", defaultValue = "false") Boolean trash) {
        if (trash) {
            // Check user is admin
            dataAccessControlService.checkIsAdmin("Cannot access to trash");

            // Call the trash service
            return trashService.count(ObservedLocation.class.getSimpleName());
        }

        filter = fillRootDataFilter(filter, ObservedLocationFilterVO.class);

        return observedLocationService.countByFilter(filter);
    }

    @GraphQLQuery(name = "observedLocation", description = "Get an observed location, by id")
    @Transactional(readOnly = true)
    @IsUser
    public ObservedLocationVO getObservedLocationById(@GraphQLArgument(name = "id") int id,
                                                      @GraphQLEnvironment ResolutionEnvironment env) {

        Set<String> fields = GraphQLUtils.fields(env);

        final ObservedLocationVO result = observedLocationService.get(id, getObservedLocationFetchOptions(fields));

        // Check read access
        dataAccessControlService.checkCanRead(result);

        // Add additional properties if needed
        fillObservedLocationFields(result, fields);

        return result;
    }

    @GraphQLMutation(name = "saveObservedLocation", description = "Create or update an observed location")
    @IsUser
    public ObservedLocationVO saveObservedLocation(
            @GraphQLArgument(name = "observedLocation") ObservedLocationVO observedLocation,
            @GraphQLArgument(name = "options") ObservedLocationSaveOptions options,
            @GraphQLEnvironment ResolutionEnvironment env) {

        // Make sure user can write
        dataAccessControlService.checkCanWrite(observedLocation);

        // Save
        ObservedLocationVO result = observedLocationService.save(observedLocation, options);

        // Fill expected fields
        fillObservedLocationFields(result, GraphQLUtils.fields(env));

        return result;
    }

    @GraphQLMutation(name = "saveObservedLocations", description = "Create or update many observed locations")
    @IsUser
    public List<ObservedLocationVO> saveObservedLocations(
            @GraphQLArgument(name = "observedLocations") List<ObservedLocationVO> observedLocations,
            @GraphQLArgument(name = "options") ObservedLocationSaveOptions options,
            @GraphQLEnvironment ResolutionEnvironment env) {

        // Make sure user can write
        dataAccessControlService.checkCanWriteAll(observedLocations);

        final List<ObservedLocationVO> result = observedLocationService.save(observedLocations, options);

        // Fill expected fields
        fillObservedLocationsFields(result, GraphQLUtils.fields(env));

        return result;
    }

    @GraphQLMutation(name = "deleteObservedLocation", description = "Delete an observed location")
    @IsUser
    public void deleteObservedLocation(@GraphQLArgument(name = "id") int id) {
        observedLocationService.delete(id);
    }

    @GraphQLMutation(name = "deleteObservedLocations", description = "Delete many observed locations")
    @IsUser
    public void deleteObservedLocations(@GraphQLNonNull @GraphQLArgument(name = "ids") List<Integer> ids) {
        observedLocationService.delete(ids);
    }

    @GraphQLSubscription(name = "updateObservedLocation", description = "Subscribe to changes on an observed location")
    @IsUser
    public Publisher<ObservedLocationVO> updateObservedLocation(@GraphQLArgument(name = "id") final int id,
                                                                @GraphQLArgument(name = "interval", defaultValue = "30", description = "Minimum interval to find changes, in seconds.") final Integer minIntervalInSecond,
                                                                @GraphQLEnvironment() ResolutionEnvironment env) {

        Preconditions.checkArgument(id >= 0, "Invalid id");
        Set<String> fields = GraphQLUtils.fields(env);
        return entityWatchService.watchEntity(ObservedLocation.class, ObservedLocationVO.class, id, minIntervalInSecond, true)
                .toFlowable(BackpressureStrategy.LATEST)
                .map(ol -> fillObservedLocationFields(ol, fields));
    }

    @GraphQLMutation(name = "controlObservedLocation", description = "Control an observed location")
    @IsUser
    public ObservedLocationVO controlObservedLocation(
            @GraphQLArgument(name = "observedLocation") ObservedLocationVO observedLocation,
            @GraphQLArgument(name = "options") DataControlOptions options,
            @GraphQLEnvironment ResolutionEnvironment env) {
        final ObservedLocationVO result = observedLocationService.control(observedLocation, options);

        // Add additional properties if needed
        fillObservedLocationFields(result, GraphQLUtils.fields(env));

        return result;
    }

    @GraphQLMutation(name = "validateObservedLocation", description = "Validate an observed location")
    @IsSupervisor
    public ObservedLocationVO validateObservedLocation(
            @GraphQLArgument(name = "observedLocation") ObservedLocationVO observedLocation,
            @GraphQLArgument(name = "options") DataValidateOptions options,
            @GraphQLEnvironment ResolutionEnvironment env) {
        final ObservedLocationVO result = observedLocationService.validate(observedLocation, options);

        // Add additional properties if needed
        fillObservedLocationFields(result, GraphQLUtils.fields(env));

        return result;
    }

    @GraphQLMutation(name = "unvalidateObservedLocation", description = "Unvalidate an observed location")
    @IsSupervisor
    public ObservedLocationVO unvalidateObservedLocation(
            @GraphQLArgument(name = "observedLocation") ObservedLocationVO observedLocation,
            @GraphQLArgument(name = "options") DataValidateOptions options,
            @GraphQLEnvironment ResolutionEnvironment env) {
        final ObservedLocationVO result = observedLocationService.unvalidate(observedLocation, options);

        // Add additional properties if needed
        fillObservedLocationFields(result, GraphQLUtils.fields(env));

        return result;
    }

    @GraphQLMutation(name = "qualifyObservedLocation", description = "Qualify an observed location")
    @IsSupervisor
    public ObservedLocationVO qualifyObservedLocation(@GraphQLArgument(name = "observedLocation") ObservedLocationVO observedLocation, @GraphQLEnvironment ResolutionEnvironment env) {
        final ObservedLocationVO result = observedLocationService.qualify(observedLocation);

        // Add additional properties if needed
        fillObservedLocationFields(result, GraphQLUtils.fields(env));

        return result;
    }

    @GraphQLQuery(name = "samplingStrata", description = "Get observed location's sampling strata")
    public ReferentialVO getObservedLocationSamplingStrata(@GraphQLContext ObservedLocationVO observedLocationVO) {
        if (observedLocationVO.getSamplingStrata() != null) return observedLocationVO.getSamplingStrata();
        if (observedLocationVO.getSamplingStrataId() == null) return null;

        return this.referentialService.get(SamplingStrata.class.getSimpleName(), observedLocationVO.getSamplingStrataId());
    }

    /* -- Sales -- */

    @GraphQLQuery(name = "sales", description = "Get trip's sales")
    public List<SaleVO> getSalesByTrip(@GraphQLContext TripVO trip) {
        // Optimization: avoid fetching expected sale when not need (fix #IMAGINE-651)
        if (trip.getHasSales() == Boolean.FALSE) return null;

        if (trip.getSales() != null) return trip.getSales();
        if (trip.getId() == null) return null;
        return saleService.getAllByTripId(trip.getId(), null);
    }

    @GraphQLQuery(name = "sales", description = "Get landing's sales")
    public List<SaleVO> getSalesByLanding(@GraphQLContext LandingVO landing) {
        // Optimization: avoid fetching sale when not need
        if (landing.getHasSales() == Boolean.FALSE) return null;

        if (landing.getSales() != null) return landing.getSales();
        if (landing.getId() == null) return null;
        return saleService.getAllByLandingId(landing.getId(), null);
    }

    /* -- Sale -- */
    @GraphQLQuery(name = "sale", description = "Get sale by id")
    public SaleVO getSaleById(@GraphQLArgument(name = "id") int id,
                              @GraphQLEnvironment ResolutionEnvironment env) {

        SaleVO result = saleService.get(id, getSaleFetchOptions(GraphQLHelper.fields(env)));

        // Check read access
        dataAccessControlService.checkCanRead(result);

        return result;
    }

    @GraphQLSubscription(name = "updateSale", description = "Subscribe to changes on an sale")
    @IsUser
    public Publisher<SaleVO> updateSale(@GraphQLNonNull @GraphQLArgument(name = "id") final int id,
                                        @GraphQLArgument(name = "interval", defaultValue = "30", description = "Minimum interval to find changes, in seconds.") final Integer minIntervalInSecond,
                                        @GraphQLEnvironment() ResolutionEnvironment env) {

        Preconditions.checkArgument(id >= 0, "Invalid id");
        Set<String> fields = GraphQLUtils.fields(env);
        return entityWatchService.watchEntity(Sale.class, SaleVO.class, id, minIntervalInSecond, true)
                .toFlowable(BackpressureStrategy.LATEST)
                .map(t -> fillSaleFields(t, fields));
    }

    @GraphQLMutation(name = "saveSales", description = "Create or update many sales")
    @IsUser
    public List<SaleVO> saveSales(@GraphQLNonNull @GraphQLArgument(name = "sales") List<SaleVO> sales,
                                  @GraphQLEnvironment ResolutionEnvironment env) {
        final List<SaleVO> result = saleService.save(sales);

        // Fill expected fields
        fillSalesFields(result, GraphQLUtils.fields(env));

        return result;
    }
    @GraphQLQuery(name = "sale", description = "Get trip's unique sale")
    public SaleVO getUniqueSaleByTrip(@GraphQLContext TripVO trip) {
        // Optimization: avoid fetching sale when not need (fix #IMAGINE-651)
        if (trip.getHasSales() == Boolean.FALSE) return null;

        if (trip.getSale() != null) return trip.getSale();
        if (trip.getId() == null) return null;
        List<SaleVO> sales = saleService.getAllByTripId(trip.getId(), null);
        return CollectionUtils.isEmpty(sales) ? null : CollectionUtils.extractSingleton(sales);
    }

    @GraphQLMutation(name = "deleteSale", description = "Delete a sale")
    @IsUser
    public void deleteSale(@GraphQLArgument(name = "id") int id) {
        saleService.delete(id);
    }

    @GraphQLMutation(name = "deleteSales", description = "Delete many sales")
    @IsUser
    public void deleteSales(@GraphQLNonNull @GraphQLArgument(name = "ids") List<Integer> ids) {
        saleService.delete(ids);
    }

    /* -- Expected Sales -- */

    @GraphQLQuery(name = "expectedSales", description = "Get trip's expected sales")
    public List<ExpectedSaleVO> getExpectedSalesByTrip(@GraphQLContext TripVO trip) {
        // Optimization: avoid fetching expected sale when not need (fix #IMAGINE-651)
        if (trip.getHasExpectedSales() == Boolean.FALSE) return null;

        if (trip.getExpectedSales() != null) return trip.getExpectedSales();
        if (trip.getId() == null) return null;
        return expectedSaleService.getAllByTripId(trip.getId());
    }

    @GraphQLQuery(name = "expectedSale", description = "Get trip's unique expected sale")
    public ExpectedSaleVO getUniqueExpectedSaleByTrip(@GraphQLContext TripVO trip) {
        // Optimization: avoid fetching expected sale when not need (fix #IMAGINE-651)
        if (trip.getHasExpectedSales() == Boolean.FALSE) return null;

        if (trip.getExpectedSale() != null) return trip.getExpectedSale();
        if (trip.getId() == null) return null;
        List<ExpectedSaleVO> expectedSales = expectedSaleService.getAllByTripId(trip.getId());
        return CollectionUtils.isEmpty(expectedSales) ? null : CollectionUtils.extractSingleton(expectedSales);
    }

    /* -- Operations -- */

    @GraphQLQuery(name = "operations", description = "Get trip's operations")
    @Transactional(readOnly = true)
    @IsUser
    public List<OperationVO> findOperationsByTripId(@GraphQLArgument(name = "filter") OperationFilterVO filter,
                                                    @GraphQLArgument(name = "offset", defaultValue = "0") Integer offset,
                                                    @GraphQLArgument(name = "size", defaultValue = "1000") Integer size,
                                                    @GraphQLArgument(name = "sortBy", defaultValue = OperationVO.Fields.START_DATE_TIME) String sort,
                                                    @GraphQLArgument(name = "sortDirection", defaultValue = "asc") String direction) {
        Preconditions.checkNotNull(filter, "Missing filter or filter.tripId");
        Preconditions.checkNotNull(filter.getTripId(), "Missing filter or filter.tripId");
        return operationService.findAllByTripId(filter.getTripId(), offset, size, sort,
                SortDirection.fromString(direction),
                OperationFetchOptions.DEFAULT);
    }

    @GraphQLQuery(name = "operations", description = "Get trip's operations")
    public List<OperationVO> findOperationsByTripId(@GraphQLContext TripVO trip) {
        if (CollectionUtils.isNotEmpty(trip.getOperations())) {
            return trip.getOperations();
        }
        return operationService.findAllByTripId(trip.getId(), OperationFetchOptions.DEFAULT);
    }

    @GraphQLQuery(name = "operations", description = "Search in operations")
    @Transactional(readOnly = true)
    @IsUser
    public List<OperationVO> findOperationsByFilter(@GraphQLArgument(name = "filter") OperationFilterVO filter,
                                                    @GraphQLArgument(name = "offset", defaultValue = "0") Integer offset,
                                                    @GraphQLArgument(name = "size", defaultValue = "1000") Integer size,
                                                    @GraphQLArgument(name = "sortBy") String sort,
                                                    @GraphQLArgument(name = "sortDirection", defaultValue = "desc") String direction,
                                                    @GraphQLEnvironment() ResolutionEnvironment env
    ) {

        Preconditions.checkNotNull(filter, "Missing filter");
        Preconditions.checkArgument(filter.getTripId() != null || filter.getProgramLabel() != null, "Missing filter.programLabel or filter.tripId");

        SortDirection sortDirection = SortDirection.fromString(direction, SortDirection.DESC);
        sort = (sort.equals("endPosition") || sort.equals("startPosition") ? "id" : sort);

        Set<String> fields = GraphQLUtils.fields(env);

        return operationService.findAllByFilter(filter,
                offset, size, sort, sortDirection,
                getOperationFetchOptions(fields));
    }

    @GraphQLQuery(name = "operationsCount", description = "Get operations count")
    @Transactional(readOnly = true)
    @IsUser
    public long countOperations(@GraphQLArgument(name = "filter") OperationFilterVO filter) {
        Preconditions.checkNotNull(filter, "Missing filter");
        Preconditions.checkArgument(filter.getTripId() != null || filter.getProgramLabel() != null, "Missing filter.programLabel or filter.tripId");
        return operationService.countByFilter(filter);
    }

    @GraphQLQuery(name = "operation", description = "Get an operation")
    @Transactional(readOnly = true)
    @IsUser
    public OperationVO getOperation(@GraphQLArgument(name = "id") int id,
                                    @GraphQLEnvironment() ResolutionEnvironment env) {
        Set<String> fields = GraphQLUtils.fields(env);
        return operationService.get(id, getOperationFetchOptions(fields));
    }

    @GraphQLMutation(name = "saveOperations", description = "Create or update many operations")
    @IsUser
    public List<OperationVO> saveOperations(@GraphQLNonNull @GraphQLArgument(name = "operations") List<OperationVO> operations) {
        return operationService.save(operations);
    }

    @GraphQLMutation(name = "saveOperation", description = "Create or update an operation")
    @IsUser
    public OperationVO saveOperation(@GraphQLArgument(name = "operation") OperationVO operation) {
        return operationService.save(operation);
    }

    @GraphQLMutation(name = "deleteOperation", description = "Delete an operation")
    @IsUser
    public void deleteOperation(@GraphQLArgument(name = "id") int id) {
        operationService.delete(id);
    }

    @GraphQLMutation(name = "deleteOperations", description = "Delete many operations")
    @IsUser
    public void deleteOperations(@GraphQLNonNull @GraphQLArgument(name = "ids") List<Integer> ids) {
        operationService.delete(ids);
    }

    @GraphQLSubscription(name = "updateOperation", description = "Subscribe to changes on an operation")
    @IsUser
    public Publisher<OperationVO> updateOperation(@GraphQLArgument(name = "id") final int id,
                                                  @GraphQLArgument(name = "interval", defaultValue = "30", description = "Minimum interval to find changes, in seconds.") final Integer minIntervalInSecond
    ) {

        Preconditions.checkArgument(id >= 0, "Invalid id");
        return entityWatchService.watchEntity(Operation.class, OperationVO.class, id, minIntervalInSecond, true)
                .toFlowable(BackpressureStrategy.LATEST);
    }

    @GraphQLMutation(name = "controlOperation", description = "Control an operation")
    @IsUser
    public OperationVO controlOperation(@GraphQLNonNull @GraphQLArgument(name = "operation") OperationVO operation) {
        return operationService.control(operation);
    }

    /* -- Operation Groups -- */

    @GraphQLQuery(name = "operationGroups", description = "Get trip's operation groups")
    public List<OperationGroupVO> getOperationGroupsByTrip(@GraphQLContext TripVO trip) {
        if (trip.getOperationGroups() != null) return trip.getOperationGroups();
        if (trip.getId() == null) return null;
        return operationGroupService.findAllByTripId(trip.getId(), null);
    }

    @GraphQLQuery(name = "operationGroups", description = "Get trip's operation groups")
    @Transactional(readOnly = true)
    @IsUser
    @Deprecated
    public List<OperationGroupVO> getOperationGroupsByTripId(@GraphQLArgument(name = "filter") OperationFilterVO filter,
                                                             @GraphQLArgument(name = "offset", defaultValue = "0") Integer offset,
                                                             @GraphQLArgument(name = "size", defaultValue = "1000") Integer size,
                                                             @GraphQLArgument(name = "sortBy", defaultValue = OperationGroupVO.Fields.RANK_ORDER_ON_PERIOD) String sort,
                                                             @GraphQLArgument(name = "sortDirection", defaultValue = "asc") String direction) {
        Preconditions.checkNotNull(filter, "Missing tripFilter or tripFilter.tripId");
        Preconditions.checkNotNull(filter.getTripId(), "Missing tripFilter or tripFilter.tripId");
        return operationGroupService.findAllByTripId(filter.getTripId(),
                Page.builder()
                        .offset(offset)
                        .size(size)
                        .sortBy(sort)
                        .sortDirection(SortDirection.fromString(direction))
                        .build(), null);
    }

    /* -- Products -- */

    @GraphQLQuery(name = "products", description = "Get operation group's products")
    public List<ProductVO> getProductsByOperationGroup(@GraphQLContext OperationGroupVO operationGroup) {

        if (operationGroup.getProducts() != null) return operationGroup.getProducts();
        if (operationGroup.getId() == null) return null;
        return productService.getByOperationId(operationGroup.getId());
    }

    @GraphQLQuery(name = "products", description = "Get sale's products")
    public List<ProductVO> getProductsBySale(@GraphQLContext SaleVO sale) {

        if (sale.getProducts() != null) return sale.getProducts();
        if (sale.getId() == null) return null;
        return productService.getBySaleId(sale.getId());
    }

    @GraphQLQuery(name = "products", description = "Get expected sale's products")
    public List<ProductVO> getProductsByExpectedSale(@GraphQLContext ExpectedSaleVO expectedSale) {

        if (expectedSale.getProducts() != null) return expectedSale.getProducts();
        if (expectedSale.getId() == null) return null;
        return productService.getByExpectedSaleId(expectedSale.getId());
    }

    @GraphQLQuery(name = "products", description = "Get landing's products")
    public List<ProductVO> getProductsByLanding(@GraphQLContext LandingVO landing) {
        if (landing.getId() == null) return null;
        return productService.getByLandingId(landing.getId());
    }

    /* -- Packets -- */

    @GraphQLQuery(name = "packets", description = "Get operation group's packets")
    public List<PacketVO> getPacketsByOperationGroup(@GraphQLContext OperationGroupVO operationGroup) {

        if (operationGroup.getPackets() != null) return operationGroup.getPackets();
        if (operationGroup.getId() == null) return null;
        return packetService.getAllByOperationId(operationGroup.getId());
    }


    /* -- Vessel position -- */

    @GraphQLQuery(name = "positions", description = "Get operation's position")
    public List<VesselPositionVO> getPositionsByOperation(@GraphQLContext OperationVO operation) {
        // Avoid a reloading (e.g. when saving)
        if (operation.getPositions() != null) return operation.getPositions();
        if (operation.getId() == null) return null;
        return vesselPositionService.getAllByOperationId(operation.getId(), 0, 100, VesselPositionVO.Fields.DATE_TIME, SortDirection.ASC);
    }

    /* -- Sample -- */

    @GraphQLQuery(name = "samples", description = "Get operation's samples")
    public List<SampleVO> getSamplesByOperation(@GraphQLContext OperationVO operation,
                                                @GraphQLEnvironment ResolutionEnvironment env) {
        // Avoid a reloading (e.g. when saving)
        if (operation.getSamples() != null) return operation.getSamples();
        if (operation.getId() == null) return null;

        SampleFetchOptions fetchOptions = getSampleFetchOptions(GraphQLUtils.fields(env));
        return sampleService.getAllByOperationId(operation.getId(), fetchOptions);
    }

    @GraphQLQuery(name = "samples", description = "Get operation group's samples")
    public List<SampleVO> getSamplesByOperationGroup(@GraphQLContext OperationGroupVO operationGroup,
                                                     @GraphQLEnvironment ResolutionEnvironment env) {
        // Avoid a reloading (e.g. when saving)
        if (operationGroup.getSamples() != null) return operationGroup.getSamples();
        if (operationGroup.getId() == null) return null;

        SampleFetchOptions fetchOptions = getSampleFetchOptions(GraphQLUtils.fields(env));

        return sampleService.getAllByOperationId(operationGroup.getId(), fetchOptions);
    }

    @GraphQLQuery(name = "samples", description = "Get landing's samples")
    public List<SampleVO> getSamplesByLanding(@GraphQLContext LandingVO landing,
                                              @GraphQLEnvironment ResolutionEnvironment env) {
        if (landing.getHasSamples() == Boolean.FALSE) return null;

        // Avoid a reloading (e.g. when saving)
        if (landing.getSamples() != null) return landing.getSamples();
        if (landing.getId() == null) return null;

        SampleFetchOptions fetchOptions = getSampleFetchOptions(GraphQLUtils.fields(env));

        // Get samples by operation if a main undefined operation group exists
        List<SampleVO> samples;
        Optional<Integer> operationId = getMainUndefinedOperationGroupId(landing);
        if (operationId.isPresent()) {
            samples = sampleService.getAllByOperationId(operationId.get(), fetchOptions);
        } else {
            samples = sampleService.getAllByLandingId(landing.getId(), fetchOptions);
        }

        landing.setSamples(samples);
        return samples;
    }

    @GraphQLQuery(name = "samplesCount", description = "Get number of samples")
    public long countSamplesByLanding(@GraphQLContext LandingVO landing) {
        // Avoid a reloading (e.g. when saving)
        if (landing.getSamplesCount() != null) return landing.getSamplesCount();
        if (landing.getId() == null) return 0L;

        // Get samples by operation if a main undefined operation group exists
        SampleFilterVO filter = SampleFilterVO.builder().withTagId(true).build();
        Optional<Integer> operationId = getMainUndefinedOperationGroupId(landing);
        if (operationId.isPresent()) {
            filter.setOperationId(operationId.get());
        } else {
            filter.setLandingId(landing.getId());
        }

        return sampleService.countByFilter(filter);
    }

    @GraphQLQuery(name = "samplesCount", description = "Get total number of samples")
    @Transactional(readOnly = true)
    @IsUser
    public long countSamples(@GraphQLArgument(name = "filter") SampleFilterVO filter) {
        return sampleService.countByFilter(filter);
    }

    /* -- Batch -- */

    @GraphQLQuery(name = "batches", description = "Get operation's batches")
    public List<BatchVO> getBatchesByOperation(@GraphQLContext OperationVO operation,
                                               @GraphQLEnvironment ResolutionEnvironment env) {
        // Avoid a reloading (e.g. when saving): reuse existing VO
        if (operation.getBatches() != null) return operation.getBatches();
        if (operation.getId() == null) return null;

        Set<String> fields = GraphQLUtils.fields(env);

        // Reload, if not exist in VO
        return batchService.getAllByOperationId(operation.getId(), BatchFetchOptions.builder()
                .withRecorderDepartment(fields.contains(StringUtils.slashing(BatchVO.Fields.RECORDER_DEPARTMENT, ReferentialVO.Fields.ID)))
                .withMeasurementValues(fields.contains(BatchVO.Fields.MEASUREMENT_VALUES))
                .withChildrenEntities(false)
                .build());
    }

    /* -- DenormalizedBatch -- */


    @GraphQLQuery(name = "denormalizeTrip", description = "Check if trip has been denormalized or not")
    @Transactional(propagation = Propagation.NOT_SUPPORTED) // Avoid transaction timeout
    @IsUser
    public DenormalizedTripResultVO denormalizeTrip(@GraphQLArgument(name = "id") int tripId) {
        return denormalizedTripService.denormalizeById(tripId);
    }


    @GraphQLQuery(name = "denormalizedBatches", description = "Get denormalized batches")
    @Transactional(readOnly = true)
    @IsUser
    public List<DenormalizedBatchVO> findAllDenormalizedBatches(@GraphQLArgument(name = "filter") DenormalizedBatchesFilterVO filter,
                                                                @GraphQLArgument(name = "offset", defaultValue = "0") Integer offset,
                                                                @GraphQLArgument(name = "size", defaultValue = "1000") Integer size,
                                                                @GraphQLArgument(name = "sortBy") String sort,
                                                                @GraphQLArgument(name = "sortDirection", defaultValue = "desc") String direction,
                                                                @GraphQLEnvironment ResolutionEnvironment env) {
        SortDirection sortDirection = SortDirection.fromString(direction, SortDirection.DESC);
        Set<String> fields = GraphQLUtils.fields(env);

        DenormalizedBatchFetchOptions fetchOptions = DenormalizedBatchFetchOptions.builder()
                .withChildrenEntities(fields.contains(StringUtils.slashing(DenormalizedBatchVO.Fields.SORTING_VALUES, DenormalizedBatchSortingValueVO.Fields.ID)))
                .withMeasurementValues(fields.contains(DenormalizedBatchVO.Fields.MEASUREMENT_VALUES))
                .build();
        return denormalizedBatchService.findAll(filter, offset, size, sort, sortDirection, fetchOptions);
    }

    /* -- Landings -- */

    @GraphQLQuery(name = "landings", description = "Search in landings")
    @Transactional(readOnly = true)
    @IsUser
    public List<LandingVO> findLandings(@GraphQLArgument(name = "filter") LandingFilterVO filter,
                                        @GraphQLArgument(name = "offset", defaultValue = "0") Integer offset,
                                        @GraphQLArgument(name = "size", defaultValue = "1000") Integer size,
                                        @GraphQLArgument(name = "sortBy", defaultValue = LandingVO.Fields.DATE_TIME) String sort,
                                        @GraphQLArgument(name = "sortDirection", defaultValue = "asc") String direction,
                                        @GraphQLArgument(name = "trash", defaultValue = "false") Boolean trash,
                                        @GraphQLEnvironment ResolutionEnvironment env
    ) {
        SortDirection sortDirection = SortDirection.fromString(direction, SortDirection.DESC);

        // Read from trash
        if (trash) {
            // Check user is admin
            dataAccessControlService.checkIsAdmin("Cannot access to trash");

            // Set default sort
            sort = sort != null ? sort : LandingVO.Fields.UPDATE_DATE;

            // Call the trash service
            return trashService.findAll(Landing.class.getSimpleName(),
                Pageables.create(offset, size, sort, sortDirection),
                LandingVO.class).getContent();
        }

        filter = fillRootDataFilter(filter, LandingFilterVO.class);
        Set<String> fields = GraphQLUtils.fields(env);

        long now = TimeLog.getTime();
        final List<LandingVO> result = landingService.findAll(
                filter,
                Page.builder()
                        .offset(offset)
                        .size(size)
                        .sortBy(sort)
                        .sortDirection(SortDirection.fromString(direction))
                        .build(),
                getLandingFetchOptions(fields));

        // Add additional properties if needed
        fillLandingsFields(result, fields);

        timeLog.log(now, "findLandings");

        return result;
    }

    @GraphQLQuery(name = "landingsCount", description = "Get total number of landings")
    @Transactional(readOnly = true)
    @IsUser
    public long countLandings(@GraphQLArgument(name = "filter") LandingFilterVO filter,
                              @GraphQLArgument(name = "trash", defaultValue = "false") Boolean trash) {
        if (trash) {
            // Check user is admin
            dataAccessControlService.checkIsAdmin("Cannot access to trash");

            // Call the trash service
            return trashService.count(Landing.class.getSimpleName());
        }

        filter = fillRootDataFilter(filter, LandingFilterVO.class);

        return landingService.countByFilter(filter);
    }

    @GraphQLQuery(name = "landing", description = "Get a landing, by id")
    @Transactional(readOnly = true)
    @IsUser
    public LandingVO getLanding(@GraphQLArgument(name = "id") int id,
                                @GraphQLEnvironment ResolutionEnvironment env) {
        final LandingVO result = landingService.get(id, getLandingFetchOptions(GraphQLUtils.fields(env)));

        // Add additional properties if needed
        fillLandingFields(result, GraphQLUtils.fields(env));

        return result;
    }

    @GraphQLQuery(name = "trip", description = "Get landing's trip")
    public TripVO getTripByLanding(@GraphQLContext LandingVO landing) {
        if (landing.getTrip() != null) return landing.getTrip(); // Used updated entity, if exists (e.g. when saving)
        if (landing.getTripId() == null) return null;
        TripVO target = tripService.get(landing.getTripId());

        // Avoid landing to be reload from trip (in GraphQL fragment)
        target.setLanding(landing);

        return target;
    }

    @GraphQLMutation(name = "saveLanding", description = "Create or update an landing")
    @IsUser
    public LandingVO saveLanding(@GraphQLArgument(name = "landing") LandingVO landing,
                                 @GraphQLEnvironment ResolutionEnvironment env) {
        final LandingVO result = landingService.save(landing);

        // Fill expected fields
        fillLandingFields(result, GraphQLUtils.fields(env));

        return result;
    }

    @GraphQLMutation(name = "saveLandings", description = "Create or update many landings")
    @IsUser
    public List<LandingVO> saveLandings(@GraphQLNonNull @GraphQLArgument(name = "landings") List<LandingVO> landings,
                                        @GraphQLEnvironment ResolutionEnvironment env) {
        final List<LandingVO> result = landingService.save(landings);

        // Fill expected fields
        fillLandingsFields(result, GraphQLUtils.fields(env));

        return result;
    }

    @GraphQLMutation(name = "deleteLanding", description = "Delete an observed location")
    @IsUser
    public void deleteLanding(@GraphQLArgument(name = "id") int id) {
        landingService.delete(id);
    }

    @GraphQLMutation(name = "deleteLandings", description = "Delete many observed locations")
    @IsUser
    public void deleteLandings(@GraphQLNonNull @GraphQLArgument(name = "ids") List<Integer> ids) {
        landingService.delete(ids);
    }

    @GraphQLSubscription(name = "updateLanding", description = "Subscribe to changes on an landing")
    @IsUser
    public Publisher<LandingVO> updateLanding(@GraphQLArgument(name = "id") final int id,
                                              @GraphQLArgument(name = "interval", defaultValue = "30", description = "Minimum interval to find changes, in seconds.") final Integer minIntervalInSecond,
                                              @GraphQLEnvironment ResolutionEnvironment env) {

        Preconditions.checkArgument(id >= 0, "Invalid id");
        Set<String> fields = GraphQLUtils.fields(env);
        return entityWatchService.watchEntity(Landing.class, LandingVO.class, id, minIntervalInSecond, true)
                .toFlowable(BackpressureStrategy.LATEST)
                .map(l -> fillLandingFields(l, fields));
    }

    @GraphQLMutation(name = "controlLanding", description = "Control a landing")
    @IsUser
    public LandingVO controlLanding(@GraphQLNonNull @GraphQLArgument(name = "landing") LandingVO landing, @GraphQLEnvironment ResolutionEnvironment env) {
        final LandingVO result = landingService.control(landing, DataControlOptions.builder().build());

        // Add additional properties if needed
        fillLandingFields(result, GraphQLUtils.fields(env));

        return result;
    }

    @GraphQLMutation(name = "unvalidateLanding", description = "Unvalidate a landing")
    @IsSupervisor
    public LandingVO unvalidateLanding(
        @GraphQLArgument(name = "landing") LandingVO landing,
        @GraphQLArgument(name = "options") DataValidateOptions options,
        @GraphQLEnvironment ResolutionEnvironment env) {
        final LandingVO result = landingService.unvalidate(landing, options);

        // Add additional properties if needed
        fillLandingFields(result, GraphQLUtils.fields(env));

        return result;
    }

    @GraphQLMutation(name = "qualifyLanding", description = "Qualify a landing")
    @IsSupervisor
    public LandingVO qualifyLanding(@GraphQLArgument(name = "landing") LandingVO landing, @GraphQLEnvironment ResolutionEnvironment env) {
        final LandingVO result = landingService.qualify(landing);

        // Add additional properties if needed
        fillLandingFields(result, GraphQLUtils.fields(env));

        return result;
    }

    /* -- Aggregated landings -- */

    @GraphQLQuery(name = "aggregatedLandings", description = "Find aggregated landings by filter")
    public List<AggregatedLandingVO> findAggregatedLandings(
            @GraphQLArgument(name = "filter") AggregatedLandingFilterVO filter,
            @GraphQLEnvironment ResolutionEnvironment env
    ) {
        List<AggregatedLandingVO> result = aggregatedLandingService.findAll(filter);
        vesselGraphQLService.fillVesselSnapshot(result, GraphQLUtils.fields(env));
        return result;
    }

    @GraphQLMutation(name = "saveAggregatedLandings", description = "Save aggregated landings")
    public List<AggregatedLandingVO> saveAggregatedLandings(
            @GraphQLArgument(name = "filter") AggregatedLandingFilterVO filter,
            @GraphQLArgument(name = "aggregatedLandings") List<AggregatedLandingVO> aggregatedLandings,
            @GraphQLEnvironment ResolutionEnvironment env
    ) {
        List<AggregatedLandingVO> result = aggregatedLandingService.saveAll(filter, aggregatedLandings);

        vesselGraphQLService.fillVesselSnapshot(result, GraphQLUtils.fields(env));

        return result;
    }

    @GraphQLMutation(name = "deleteAggregatedLandings", description = "Delete many aggregated landings")
    public void deleteAggregatedLandings(
            @GraphQLArgument(name = "filter") AggregatedLandingFilterVO filter,
            @GraphQLArgument(name = "vesselIds") List<Integer> vesselIds
    ) {
        aggregatedLandingService.deleteAll(filter, vesselIds);
    }

    /* -- Activity calendar -- */

    @GraphQLQuery(name = "activityCalendars", description = "Search in activity calendars")
    @Transactional(readOnly = true)
    @IsUser
    public List<ActivityCalendarVO> findActivityCalendars(@GraphQLArgument(name = "filter") ActivityCalendarFilterVO filter,
                                                          @GraphQLArgument(name = "offset", defaultValue = "0") Integer offset,
                                                          @GraphQLArgument(name = "size", defaultValue = "1000") Integer size,
                                                          @GraphQLArgument(name = "sortBy", defaultValue = ActivityCalendarVO.Fields.YEAR) String sort,
                                                          @GraphQLArgument(name = "sortDirection", defaultValue = "desc") String direction,
                                                          @GraphQLArgument(name = "trash", defaultValue = "false") Boolean trash,
                                                          @GraphQLEnvironment ResolutionEnvironment env
    ) {
        SortDirection sortDirection = SortDirection.fromString(direction, SortDirection.DESC);

        // Read from trash
        if (trash) {
            // Check user is admin
            dataAccessControlService.checkIsAdmin("Cannot access to trash");

            // Set default sort
            sort = sort != null ? sort : ActivityCalendarVO.Fields.UPDATE_DATE;

            // Call the trash service
            return trashService.findAll(ActivityCalendar.class.getSimpleName(),
                Pageables.create(offset, size, sort, sortDirection),
                ActivityCalendarVO.class).getContent();
        }

        filter = fillActivityCalendarFilter(filter);
        Set<String> fields = GraphQLUtils.fields(env);
        ActivityCalendarFetchOptions fetchOptions = getActivityCalendarFetchOptions(fields);

        long now = TimeLog.getTime();
        final List<ActivityCalendarVO> result = activityCalendarService.findAll(
            filter,
            offset, size, sort,
            sortDirection, fetchOptions);

        // Add additional properties if needed
        fillActivityCalendarsFields(result, fields);

        timeLog.log(now, "findActivityCalendars");

        return result;
    }

    @GraphQLQuery(name = "activityCalendarsCount", description = "Get total number of activity calendars")
    @Transactional(readOnly = true)
    @IsUser
    public long countActivityCalendars(@GraphQLArgument(name = "filter") ActivityCalendarFilterVO filter,
                                       @GraphQLArgument(name = "trash", defaultValue = "false") Boolean trash) {
        if (trash) {
            // Check user is admin
            dataAccessControlService.checkIsAdmin("Cannot access to trash");

            // Call the trash service
            return trashService.count(ActivityCalendar.class.getSimpleName());
        }

        filter = fillActivityCalendarFilter(filter);

        return activityCalendarService.countByFilter(filter);
    }


    @GraphQLQuery(name = "activityCalendar", description = "Get an activity calendar, by id")
    @Transactional(readOnly = true)
    @IsUser
    public ActivityCalendarVO getActivityCalendarById(@GraphQLArgument(name = "id") int id,
                                                      @GraphQLEnvironment ResolutionEnvironment env) {
        final ActivityCalendarVO result = activityCalendarService.get(id,
            getActivityCalendarFetchOptions(GraphQLHelper.fields(env)));

        // Check read access
        dataAccessControlService.checkCanRead(result);

        // Add additional properties if needed
        fillActivityCalendarFields(result, GraphQLUtils.fields(env));

        return result;
    }

    @GraphQLMutation(name = "saveActivityCalendar", description = "Create or update an activity calendar")
    @IsUser
    public ActivityCalendarVO saveActivityCalendar(
        @GraphQLArgument(name = "activityCalendar") ActivityCalendarVO activityCalendar,
        @GraphQLArgument(name = "options") ActivityCalendarSaveOptions options,
        @GraphQLEnvironment ResolutionEnvironment env) {

        // Make sure user can write
        dataAccessControlService.checkCanWrite(activityCalendar);

        // Save
        ActivityCalendarVO result = activityCalendarService.save(activityCalendar,
            ActivityCalendarSaveOptions.defaultIfEmpty(options));

        // Fill expected fields
        fillActivityCalendarFields(result, GraphQLUtils.fields(env));

        return result;
    }

    @GraphQLMutation(name = "saveActivityCalendars", description = "Create or update many activity calendars")
    @IsUser
    public List<ActivityCalendarVO> saveActivityCalendars(
        @GraphQLArgument(name = "activityCalendars") List<ActivityCalendarVO> activityCalendars,
        @GraphQLArgument(name = "options") ActivityCalendarSaveOptions options,
        @GraphQLEnvironment ResolutionEnvironment env) {

        // Make sure user can write
        dataAccessControlService.checkCanWriteAll(activityCalendars);

        final List<ActivityCalendarVO> result = activityCalendarService.save(activityCalendars,
            ActivityCalendarSaveOptions.defaultIfEmpty(options));

        // Fill expected fields
        fillActivityCalendarsFields(result, GraphQLUtils.fields(env));

        return result;
    }

    @GraphQLMutation(name = "deleteActivityCalendar", description = "Delete an activity calendar")
    @IsUser
    public void deleteActivityCalendar(@GraphQLArgument(name = "id") int id) {
        activityCalendarService.delete(id);
    }


    @GraphQLMutation(name = "deleteActivityCalendars", description = "Delete many activity calendar, by ids")
    @IsUser
    public void deleteActivityCalendars(@GraphQLNonNull @GraphQLArgument(name = "ids") List<Integer> ids) {
        activityCalendarService.delete(ids);
    }

    @GraphQLSubscription(name = "updateActivityCalendar", description = "Subscribe to changes on an activity calendar")
    @IsUser
    public Publisher<ActivityCalendarVO> updateActivityCalendar(@GraphQLNonNull @GraphQLArgument(name = "id") final int id,
                                        @GraphQLArgument(name = "interval", defaultValue = "30", description = "Minimum interval to find changes, in seconds.") final Integer minIntervalInSecond,
                                        @GraphQLEnvironment() ResolutionEnvironment env) {

        Preconditions.checkArgument(id >= 0, "Invalid id");
        Set<String> fields = GraphQLUtils.fields(env);
        return entityWatchService.watchEntity(ActivityCalendar.class, ActivityCalendarVO.class, id, minIntervalInSecond, true)
            .toFlowable(BackpressureStrategy.LATEST)
            .map(t -> fillActivityCalendarFields(t, fields));
    }

    @GraphQLMutation(name = "controlActivityCalendar", description = "Control a activityCalendar")
    @IsUser
    public ActivityCalendarVO controlActivityCalendar(@GraphQLNonNull @GraphQLArgument(name = "activityCalendar") ActivityCalendarVO activityCalendar, @GraphQLEnvironment ResolutionEnvironment env) {
        final ActivityCalendarVO result = activityCalendarService.control(activityCalendar);

        // Add additional properties if needed
        fillActivityCalendarFields(result, GraphQLUtils.fields(env));

        return result;
    }

    @GraphQLMutation(name = "validateActivityCalendar", description = "Validate a activityCalendar")
    @IsSupervisor
    public ActivityCalendarVO validateActivityCalendar(@GraphQLNonNull @GraphQLArgument(name = "activityCalendar") ActivityCalendarVO activityCalendar, @GraphQLEnvironment ResolutionEnvironment env) {
        final ActivityCalendarVO result = activityCalendarService.validate(activityCalendar);

        // Add additional properties if needed
        fillActivityCalendarFields(result, GraphQLUtils.fields(env));

        return result;
    }

    @GraphQLMutation(name = "unvalidateActivityCalendar", description = "Unvalidate a activityCalendar")
    @IsSupervisor
    public ActivityCalendarVO unvalidateActivityCalendar(@GraphQLNonNull @GraphQLArgument(name = "activityCalendar") ActivityCalendarVO activityCalendar, @GraphQLEnvironment ResolutionEnvironment env) {
        final ActivityCalendarVO result = activityCalendarService.unvalidate(activityCalendar);

        // Add additional properties if needed
        fillActivityCalendarFields(result, GraphQLUtils.fields(env));

        return result;
    }

    @GraphQLMutation(name = "qualifyActivityCalendar", description = "Qualify a activityCalendar")
    @IsSupervisor
    public ActivityCalendarVO qualifyActivityCalendar(@GraphQLNonNull @GraphQLArgument(name = "activityCalendar") ActivityCalendarVO activityCalendar,
                              @GraphQLEnvironment ResolutionEnvironment env) {
        final ActivityCalendarVO result = activityCalendarService.qualify(activityCalendar);

        // Add additional properties if needed
        fillActivityCalendarFields(result, GraphQLUtils.fields(env));

        return result;
    }

    /* -- Measurements -- */

    // Trip
    @GraphQLQuery(name = "measurements", description = "Get trip's measurements")
    public List<MeasurementVO> getTripVesselUseMeasurements(@GraphQLContext TripVO trip) {
        if (trip.getMeasurements() != null) return trip.getMeasurements();
        if (trip.getId() == null) return null;
        return measurementService.getTripVesselUseMeasurements(trip.getId());
    }

    @GraphQLQuery(name = "measurementValues", description = "Get trip's measurements")
    public Map<Integer, String> getTripVesselUseMeasurementsMap(@GraphQLContext TripVO trip) {
        if (trip.getMeasurementValues() != null) return trip.getMeasurementValues();
        if (trip.getId() == null) return null;
        return measurementService.getTripVesselUseMeasurementsMap(trip.getId());
    }

    // Operation
    @GraphQLQuery(name = "measurements", description = "Get operation's measurements")
    public List<MeasurementVO> getOperationVesselUseMeasurements(@GraphQLContext OperationVO operation,
                                                                 @GraphQLArgument(name = "pmfmIds") List<Integer> pmfmIds) {
        if (operation.getMeasurements() != null) return operation.getMeasurements();
        if (operation.getId() == null) return null;
        return measurementService.getOperationVesselUseMeasurements(operation.getId(), pmfmIds);
    }

    @GraphQLQuery(name = "measurementValues", description = "Get operation's measurements")
    public Map<Integer, String> getOperationVesselUseMeasurementsMap(@GraphQLContext OperationVO operation) {
        if (operation.getMeasurementValues() != null) return operation.getMeasurementValues();
        if (operation.getId() == null) return null;
        return measurementService.getOperationVesselUseMeasurementsMap(operation.getId());
    }

    @GraphQLQuery(name = "gearMeasurements", description = "Get operation's gear measurements")
    public List<MeasurementVO> getOperationGearUseMeasurements(@GraphQLContext OperationVO operation) {
        if (operation.getGearMeasurements() != null) return operation.getGearMeasurements();
        if (operation.getId() == null) return null;
        return measurementService.getOperationGearUseMeasurements(operation.getId());
    }

    @GraphQLQuery(name = "gearMeasurementValues", description = "Get operation's gear measurements")
    public Map<Integer, String> getOperationGearUseMeasurementsMap(@GraphQLContext OperationVO operation) {
        if (operation.getGearMeasurementValues() != null) return operation.getGearMeasurementValues();
        if (operation.getId() == null) return null;
        return measurementService.getOperationGearUseMeasurementsMap(operation.getId());
    }

    // Operation Group
    @GraphQLQuery(name = "measurements", description = "Get operation group's measurements")
    public List<MeasurementVO> getOperationGroupVesselUseMeasurements(@GraphQLContext OperationGroupVO operationGroup) {
        if (operationGroup.getMeasurements() != null) return operationGroup.getMeasurements();
        if (operationGroup.getId() == null) return null;
        return measurementService.getOperationVesselUseMeasurements(operationGroup.getId());
    }

    @GraphQLQuery(name = "measurementValues", description = "Get operation group's measurements")
    public Map<Integer, String> getOperationGroupVesselUseMeasurementsMap(@GraphQLContext OperationGroupVO operationGroup) {
        if (operationGroup.getMeasurementValues() != null) return operationGroup.getMeasurementValues();
        if (operationGroup.getId() == null) return null;
        return measurementService.getOperationVesselUseMeasurementsMap(operationGroup.getId());
    }

    @GraphQLQuery(name = "gearMeasurements", description = "Get operation group's gear measurements")
    public List<MeasurementVO> getOperationGroupGearUseMeasurements(@GraphQLContext OperationGroupVO operationGroup) {
        if (operationGroup.getGearMeasurements() != null) return operationGroup.getGearMeasurements();
        if (operationGroup.getId() == null) return null;
        return measurementService.getOperationGearUseMeasurements(operationGroup.getId());
    }

    @GraphQLQuery(name = "gearMeasurementValues", description = "Get operation group's gear measurements")
    public Map<Integer, String> getOperationGroupGearUseMeasurementsMap(@GraphQLContext OperationGroupVO operationGroup) {
        if (operationGroup.getGearMeasurementValues() != null) return operationGroup.getGearMeasurementValues();
        if (operationGroup.getId() == null) return null;
        return measurementService.getOperationGearUseMeasurementsMap(operationGroup.getId());
    }


    // Fishing area

    @GraphQLQuery(name = "fishingArea", description = "Get trip's fishing area")
    public FishingAreaVO getTripFishingArea(@GraphQLContext TripVO trip) {
        if (trip.getId() == null) return null; // Cannot load
        return fishingAreaService.getByFishingTripId(trip.getId());
    }

    @GraphQLQuery(name = "fishingAreas", description = "Get trip's fishing areas")
    public List<FishingAreaVO> getTripFishingAreas(@GraphQLContext TripVO trip) {
        if (trip.getFishingAreas() != null) {
            // FIXME: after the first save (when id = null), the id is not set
            boolean hasAllIds = trip.getFishingAreas().stream()
                    .map(FishingAreaVO::getId)
                    .noneMatch(Objects::isNull);
            if (hasAllIds) return trip.getFishingAreas();
        }

        if (trip.getId() == null) return null; // Cannot load

        return fishingAreaService.getAllByFishingTripId(trip.getId());
    }

    @GraphQLQuery(name = "fishingAreas", description = "Get operation's fishing areas")
    public List<FishingAreaVO> getOperationFishingAreas(@GraphQLContext OperationVO operation) {
        if (operation.getFishingAreas() != null) {
            // FIXME: after the first save (when id = null), the id is not set
            boolean hasAllIds = operation.getFishingAreas().stream()
                    .map(FishingAreaVO::getId)
                    .noneMatch(Objects::isNull);
            if (hasAllIds) return operation.getFishingAreas();
        }

        if (operation.getId() == null) return null; // Cannot load

        return fishingAreaService.getAllByOperationId(operation.getId());
    }

    @GraphQLQuery(name = "fishingAreas", description = "Get operation group's fishing areas")
    public List<FishingAreaVO> getOperationGroupFishingAreas(@GraphQLContext OperationGroupVO operationGroup) {
        if (operationGroup.getFishingAreas() != null) {
            // FIXME: after the first save (when id = null), the id is not set
            boolean hasAllIds = operationGroup.getFishingAreas().stream()
                    .map(FishingAreaVO::getId)
                    .noneMatch(Objects::isNull);
            if (hasAllIds) return operationGroup.getFishingAreas();
        }

        if (operationGroup.getId() == null) return null;
        return fishingAreaService.getAllByOperationId(operationGroup.getId());
    }

    // Sale
    @GraphQLQuery(name = "measurements", description = "Get sale measurements")
    public List<MeasurementVO> getSaleMeasurements(@GraphQLContext SaleVO sale) {
        if (sale.getMeasurements() != null) return sale.getMeasurements();
        if (sale.getId() == null) return null;
        return measurementService.getSaleMeasurements(sale.getId());
    }

    @GraphQLQuery(name = "measurementValues", description = "Get sale measurement values")
    public Map<Integer, String> getSaleMeasurementsMap(@GraphQLContext SaleVO sale) {
        if (sale.getMeasurementValues() != null) return sale.getMeasurementValues();
        if (sale.getId() == null) return null;
        return measurementService.getSaleMeasurementsMap(sale.getId());
    }

    // ExpectedSale
    @GraphQLQuery(name = "measurements", description = "Get expected sale measurements")
    public List<MeasurementVO> getExpectedSaleMeasurements(@GraphQLContext ExpectedSaleVO expectedSale) {
        if (expectedSale.getMeasurements() != null) return expectedSale.getMeasurements();
        if (expectedSale.getId() == null) return null;
        return measurementService.getExpectedSaleMeasurements(expectedSale.getId());
    }

    @GraphQLQuery(name = "measurementValues", description = "Get expected sale measurement values")
    public Map<Integer, String> getExpectedSaleMeasurementsMap(@GraphQLContext ExpectedSaleVO expectedSale) {
        if (expectedSale.getMeasurementValues() != null) return expectedSale.getMeasurementValues();
        if (expectedSale.getId() == null) return null;
        return measurementService.getExpectedSaleMeasurementsMap(expectedSale.getId());
    }

    // Physical gear
    @GraphQLQuery(name = "measurements", description = "Get physical gear measurements")
    public List<MeasurementVO> getPhysicalGearMeasurements(@GraphQLContext PhysicalGearVO physicalGear) {
        if (physicalGear.getMeasurements() == null) return physicalGear.getMeasurements();
        if (physicalGear.getId() == null) return null;
        return measurementService.getPhysicalGearMeasurements(physicalGear.getId());
    }

    @GraphQLQuery(name = "measurementValues", description = "Get physical gear measurements")
    public Map<Integer, String> getPhysicalGearMeasurementsMap(@GraphQLContext PhysicalGearVO physicalGear) {
        if (physicalGear.getMeasurementValues() != null) return physicalGear.getMeasurementValues();
        if (physicalGear.getId() == null) return null;
        return measurementService.getPhysicalGearMeasurementsMap(physicalGear.getId());
    }

    // Sample
    @GraphQLQuery(name = "measurements", description = "Get sample measurements")
    public List<MeasurementVO> getSampleMeasurements(@GraphQLContext SampleVO sample) {
        if (sample.getMeasurements() != null) return sample.getMeasurements();
        if (sample.getId() == null) return null;
        return measurementService.getSampleMeasurements(sample.getId());
    }

    @GraphQLQuery(name = "measurementValues", description = "Get measurement values (as a key/value map, using pmfmId as key)")
    public Map<Integer, String> getSampleMeasurementValues(@GraphQLContext SampleVO sample) {
        if (sample.getMeasurementValues() != null) return sample.getMeasurementValues();
        if (sample.getId() == null) return null;
        return measurementService.getSampleMeasurementsMap(sample.getId());
    }

    // Batch
    @GraphQLQuery(name = "measurementValues", description = "Get measurement values (as a key/value map, using pmfmId as key)")
    public Map<Integer, String> getBatchMeasurementValues(@GraphQLContext BatchVO batch) {
        if (batch.getMeasurementValues() != null) return batch.getMeasurementValues();
        if (batch.getId() == null) return null;
        Map<Integer, String> map = Maps.newHashMap();
        map.putAll(measurementService.getBatchSortingMeasurementsMap(batch.getId()));
        map.putAll(measurementService.getBatchQuantificationMeasurementsMap(batch.getId()));
        return map;
    }

    @GraphQLQuery(name = "quantificationMeasurements", description = "Get batch quantification measurements")
    public List<QuantificationMeasurementVO> getBatchQuantificationMeasurements(@GraphQLContext BatchVO batch) {
        if (batch.getQuantificationMeasurements() != null) return batch.getQuantificationMeasurements();
        if (batch.getId() == null) return null;
        return measurementService.getBatchQuantificationMeasurements(batch.getId());
    }

    @GraphQLQuery(name = "sortingMeasurements", description = "Get batch sorting measurements")
    public List<MeasurementVO> getBatchSortingMeasurements(@GraphQLContext BatchVO batch) {
        if (batch.getSortingMeasurements() != null) return batch.getSortingMeasurements();
        if (batch.getId() == null) return null;
        return measurementService.getBatchSortingMeasurements(batch.getId());
    }

    // Product
    @GraphQLQuery(name = "measurementValues", description = "Get measurement values (as a key/value map, using pmfmId as key)")
    public Map<Integer, String> getProductMeasurementValues(@GraphQLContext ProductVO product) {
        if (product.getMeasurementValues() != null) return product.getMeasurementValues();
        productService.fillMeasurementsMap(product);
        return product.getMeasurementValues();
    }

    // Observed location
    @GraphQLQuery(name = "measurements", description = "Get measurement values")
    public List<MeasurementVO> getObservedLocationMeasurements(@GraphQLContext ObservedLocationVO observedLocation) {
        if (observedLocation.getMeasurements() != null) return observedLocation.getMeasurements();
        if (observedLocation.getId() == null) return null;
        return measurementService.getObservedLocationMeasurements(observedLocation.getId());
    }

    @GraphQLQuery(name = "measurementValues", description = "Get measurement values (as a key/value map, using pmfmId as key)")
    public Map<Integer, String> getObservedLocationMeasurementsMap(@GraphQLContext ObservedLocationVO observedLocation) {
        if (observedLocation.getMeasurementValues() != null) return observedLocation.getMeasurementValues();
        if (observedLocation.getId() == null) return null;
        return measurementService.getObservedLocationMeasurementsMap(observedLocation.getId());
    }

    // Landing
    @GraphQLQuery(name = "measurementValues", description = "Get measurement values (as a key/value map, using pmfmId as key)")
    public Map<Integer, String> getLandingMeasurementsMap(@GraphQLContext LandingVO landing) {
        if (landing.getMeasurementValues() != null) return landing.getMeasurementValues();
        if (landing.getId() == null) return null;
        Map<Integer, String> result = new HashMap<>();
        Optional.ofNullable(measurementService.getLandingMeasurementsMap(landing.getId())).ifPresent(result::putAll);
        Optional.ofNullable(measurementService.getLandingSurveyMeasurementsMap(landing.getId())).ifPresent(result::putAll);
        return result;
    }

    @GraphQLQuery(name = "measurementValues", description = "Get measurement values (as a key/value map, using pmfmId as key)")
    public Map<Integer, String> getLandingMeasurementsMap(@GraphQLContext LandingVO landing,
                                                          @GraphQLArgument(name = "pmfmIds") List<Integer> pmfmIds) {
        if (landing.getMeasurementValues() != null) return landing.getMeasurementValues();
        if (landing.getId() == null) return null;
        Map<Integer, String> result = new HashMap<>();
        Optional.ofNullable(measurementService.getLandingMeasurementsMap(landing.getId(), pmfmIds)).ifPresent(result::putAll);
        Optional.ofNullable(measurementService.getLandingSurveyMeasurementsMap(landing.getId(), pmfmIds)).ifPresent(result::putAll);
        return result;
    }

    // Activity calendar
    @GraphQLQuery(name = "measurementValues", description = "Get measurement values (as a key/value map, using pmfmId as key)")
    public Map<Integer, String> getActivityCalendarMeasurementsMap(@GraphQLContext ActivityCalendarVO activityCalendar) {
        if (activityCalendar.getMeasurementValues() != null) return activityCalendar.getMeasurementValues();
        if (activityCalendar.getId() == null) return null;
        return measurementService.getActivityCalendarMeasurementsMap(activityCalendar.getId());
    }

    // Daily activity calendar
    @GraphQLQuery(name = "measurementValues", description = "Get measurement values (as a key/value map, using pmfmId as key)")
    public Map<Integer, String> getDailyActivityCalendarMeasurementsMap(@GraphQLContext DailyActivityCalendarVO dailyActivityCalendar) {
        if (dailyActivityCalendar.getMeasurementValues() != null) return dailyActivityCalendar.getMeasurementValues();
        if (dailyActivityCalendar.getId() == null) return null;
        return measurementService.getDailyActivityCalendarMeasurementsMap(dailyActivityCalendar.getId());
    }

    // Measurement pmfm
    @GraphQLQuery(name = "pmfm", description = "Get measurement's pmfm")
    public PmfmVO getMeasurementPmfm(@GraphQLContext MeasurementVO measurement) {
        return pmfmService.get(measurement.getPmfmId(), null);
    }

    // Vessel
    @GraphQLQuery(name = "measurements", description = "Get vessel's physical measurements")
    public List<MeasurementVO> getVesselFeaturesMeasurements(@GraphQLContext VesselSnapshotVO vesselSnapshot) {
        return measurementService.getVesselFeaturesMeasurements(vesselSnapshot.getVesselFeaturesId());
    }

    @GraphQLQuery(name = "measurementValues", description = "Get vessel's physical measurements")
    public Map<Integer, String> getVesselFeaturesMeasurementsMap(@GraphQLContext VesselSnapshotVO vesselSnapshot) {
        if (vesselSnapshot.getMeasurementValues() != null) vesselSnapshot.getMeasurementValues();
        if (vesselSnapshot.getVesselFeaturesId() == null) return null;
        return measurementService.getVesselFeaturesMeasurementsMap(vesselSnapshot.getVesselFeaturesId());
    }

    @GraphQLQuery(name = "measurementValues", description = "Get vessel use features measurements")
    public Map<Integer, String> getVesselUseFeaturesMeasurementsMap(@GraphQLContext VesselUseFeaturesVO vesselUseFeatures) {
        if (vesselUseFeatures.getMeasurementValues() != null) vesselUseFeatures.getMeasurementValues();
        if (vesselUseFeatures.getId() == null) return null;
        return measurementService.getVesselUseFeaturesMeasurementsMap(vesselUseFeatures.getId());
    }

    @GraphQLQuery(name = "measurementValues", description = "Get gear use features measurements")
    public Map<Integer, String> getGearUseFeaturesMeasurementsMap(@GraphQLContext GearUseFeaturesVO gearUseFeatures) {
        if (gearUseFeatures.getMeasurementValues() != null) gearUseFeatures.getMeasurementValues();
        if (gearUseFeatures.getId() == null) return null;
        return measurementService.getGearUseFeaturesMeasurementsMap(gearUseFeatures.getId());
    }

    // Images
    @GraphQLQuery(name = "images", description = "Get sample's images")
    public List<ImageAttachmentVO> getSampleImages(@GraphQLContext SampleVO sample) {
        if (sample.getImages() != null) return sample.getImages();
        if (sample.getId() == null || !this.enableImageAttachments) return null;

        return imageService.getImagesForObject(sample.getId(), ObjectTypeEnum.SAMPLE);
    }

    @GraphQLQuery(name = "images", description = "Get activity calendar's images")
    public List<ImageAttachmentVO> getActivityCalendarImages(@GraphQLContext ActivityCalendarVO activityCalendar) {
        if (activityCalendar.getImages() != null) return activityCalendar.getImages();
        if (activityCalendar.getId() == null || !this.enableImageAttachments) return null;

        return imageService.getImagesForObject(activityCalendar.getId(), ObjectTypeEnum.ACTIVITY_CALENDAR);
    }

    @GraphQLQuery(name = "images", description = "Search filter")
    @IsUser
    public List<ImageAttachmentVO> findImagesByFilter(@GraphQLArgument(name = "filter") ImageAttachmentFilterVO filter,
                                                      @GraphQLArgument(name = "offset", defaultValue = "0") Integer offset,
                                                      @GraphQLArgument(name = "size", defaultValue = "100") Integer size,
                                                      @GraphQLArgument(name = "sortBy") String sort,
                                                      @GraphQLArgument(name = "sortDirection", defaultValue = "desc") String direction) {

        if (!this.enableImageAttachments) return ImmutableList.of();

        filter = ImageAttachmentFilterVO.nullToEmpty(filter);

        // If not an admin, limit to itself images
        if (!authService.isAdmin()) {
            Integer userId = this.authService.getAuthenticatedUserId().orElseThrow(UnauthorizedException::new);
            filter.setRecorderPersonId(userId);
        }

        return imageService.findAllByFilter(filter, Page.builder()
                .offset(offset).size(size).sortBy(sort)
                .sortDirection(SortDirection.fromString(direction, SortDirection.DESC))
                .build(), null);
    }

    @GraphQLQuery(name = "url", description = "Get image url")
    public String getImageUrl(@GraphQLContext ImageAttachmentVO image) {
        if (image.getUrl() != null) return image.getUrl(); // Already fetched
        if (image.getId() == null) return null; // Cannot fetch without id

        return imageService.getImageUrlById(image.getId());
    }

    /* -- protected methods -- */

    protected TripVO fillTripFields(TripVO trip, Set<String> fields) {
        // Add image if need
        fillImages(trip, fields);

        // Add vessel if need
        vesselGraphQLService.fillVesselSnapshot(trip, fields);

        if (fields.contains(StringUtils.slashing(TripVO.Fields.LANDING, LandingVO.Fields.ID))
                || fields.contains(TripVO.Fields.LANDING_ID)
                || fields.contains(TripVO.Fields.OBSERVED_LOCATION_ID)) {
            tripService.fillTripLandingLinks(trip);
        }

        return trip;
    }

    protected List<TripVO> fillTrips(List<TripVO> trips, Set<String> fields) {
        if (CollectionUtils.isEmpty(trips)) return trips;

        // Add image if need
        fillImages(trips, fields);

        // Add vessel if need
        vesselGraphQLService.fillVesselSnapshot(trips, fields);

        // Fill link to parent landing or observed location
        // (e.g. need by ObsDeb)
        if (fields.contains(StringUtils.slashing(TripVO.Fields.LANDING, LandingVO.Fields.ID)) || fields.contains(TripVO.Fields.OBSERVED_LOCATION_ID)) {
            tripService.fillTripsLandingLinks(trips);
        }

        return trips;
    }

    protected ObservedLocationVO fillObservedLocationFields(ObservedLocationVO observedLocation, Set<String> fields) {
        // Add image if need
        fillImages(observedLocation, fields);

        return observedLocation;
    }

    protected List<ObservedLocationVO> fillObservedLocationsFields(List<ObservedLocationVO> observedLocations, Set<String> fields) {
        // Add image if need
        fillImages(observedLocations, fields);

        return observedLocations;
    }

    protected LandingVO fillLandingFields(LandingVO landing, Set<String> fields) {
        // Add image if need
        fillImages(landing, fields);

        // Add vessel if need
        vesselGraphQLService.fillVesselSnapshot(landing, fields);

        // Add landing to child trip, if need (will avoid a reload of the same landing)
        if (landing.getTrip() != null
                && Objects.equals(landing.getTrip().getLandingId(), landing.getId())
                && fields.contains(StringUtils.slashing(LandingVO.Fields.TRIP, TripVO.Fields.LANDING, IEntity.Fields.ID))) {
            landing.getTrip().setLanding(landing);
        }

        return landing;
    }

    protected SaleVO fillSaleFields(SaleVO sale, Set<String> fields) {
        vesselGraphQLService.fillVesselSnapshot(sale, fields);
        fillImages(sale, fields);
        return sale;
    }

    protected List<LandingVO> fillLandingsFields(List<LandingVO> landings, Set<String> fields) {
        // Add image if need
        fillImages(landings, fields);

        // Add vessel if need
        vesselGraphQLService.fillVesselSnapshot(landings, fields);

        return landings;
    }

    protected List<SaleVO> fillSalesFields(List<SaleVO> sales, Set<String> fields) {
        // Add image if need
        fillImages(sales, fields);

        // Add vessel if need
        vesselGraphQLService.fillVesselSnapshot(sales, fields);

        return sales;
    }

    protected ActivityCalendarVO fillActivityCalendarFields(ActivityCalendarVO activityCalendar, Set<String> fields) {
        // Add image if need
        fillImages(activityCalendar, fields);

        // Add vessel if need
        vesselGraphQLService.fillVesselSnapshot(activityCalendar, fields);

        // Add registrationsPeriods if needed
        fillActivityCalendarsVesselRegistrationPeriods(activityCalendar, fields);

        return activityCalendar;
    }

    protected List<ActivityCalendarVO> fillActivityCalendarsFields(List<ActivityCalendarVO> activityCalendars, Set<String> fields) {
        // Add image if need
        fillImages(activityCalendars, fields);

        // Add vessel if need
        vesselGraphQLService.fillVesselSnapshot(activityCalendars, fields);

        // Add vesselRegistrationPeriods if needed
        activityCalendars.forEach((activityCalendar) -> fillActivityCalendarsVesselRegistrationPeriods(activityCalendar, fields));

        return activityCalendars;
    }

    protected ActivityCalendarVO fillActivityCalendarsVesselRegistrationPeriods(ActivityCalendarVO activityCalendar, Set<String> fields) {
        // Add authorized locations (if need)
        if (fields.contains(StringUtils.doting(ActivityCalendarVO.Fields.VESSEL_REGISTRATION_PERIODS))) {

            // get vessel id
            int vesselId =  activityCalendar.getVesselId() != null ? activityCalendar.getVesselId() : activityCalendar.getVesselSnapshot().getId();

            // Load vessel registration periods
            Date startDate = Dates.getFirstDayOfYear(activityCalendar.getYear());
            Date endDate = Dates.getLastSecondOfYear(activityCalendar.getYear());
            List<VesselRegistrationPeriodVO> registrationPeriods = vesselService.findRegistrationPeriodsByFilter(VesselFilterVO.builder()
                            .vesselId(vesselId)
                            .startDate(startDate)
                            .endDate(endDate)
                            .build(),
                    Page.create(0, 50, VesselRegistrationPeriodVO.Fields.START_DATE, SortDirection.ASC)
            );

            Integer[] registrationLocationIds = Beans.<Integer, VesselRegistrationPeriodVO>collectDistinctProperties(registrationPeriods, StringUtils.doting(VesselRegistrationPeriodVO.Fields.REGISTRATION_LOCATION, IEntity.Fields.ID))
                    .toArray(Integer[]::new);

            Set<Integer> authorizedLocationIds = dataAccessControlService.getAuthorizedLocationIds(
                            new Integer[]{activityCalendar.getProgram().getId()},
                            registrationLocationIds).map(ArrayUtils::asSet)
                    .orElseThrow(UnauthorizedException::new) // TODO check if works in App, and if Forbidden is better (in app)
                    ;

            activityCalendar.setVesselRegistrationPeriods(registrationPeriods.stream().map(source -> {
                ActivityCalendarVesselRegistrationPeriodVO target = new ActivityCalendarVesselRegistrationPeriodVO();
                Beans.copyProperties(source, target);
                target.setReadonly(!authorizedLocationIds.contains(source.getRegistrationLocation().getId()));
                return target;
            }).toList());
        }

        return activityCalendar;
    }

    protected boolean hasImageField(Set<String> fields) {
        return fields.contains(StringUtils.slashing(TripVO.Fields.RECORDER_DEPARTMENT, DepartmentVO.Fields.LOGO))
                || fields.contains(StringUtils.slashing(TripVO.Fields.RECORDER_PERSON, PersonVO.Fields.AVATAR));
    }

    protected <T extends IRootDataVO<?>> List<T> fillImages(final List<T> results) {
        results.forEach(this::fillImages);
        return results;
    }

    protected <T extends IRootDataVO<?>> T fillImages(T result) {
        if (result != null) {

            // Fill avatar on recorder department (if not null)
            imageService.fillLogo(result.getRecorderDepartment());

            // Fill avatar on recorder persons (if not null)
            imageService.fillAvatar(result.getRecorderPerson());
        }

        return result;
    }

    protected <T extends IRootDataVO<?>> List<T> fillImages(final List<T> results, Set<String> fields) {
        if (hasImageField(fields)) results.forEach(this::fillImages);
        return results;
    }

    protected <T extends IRootDataVO<?>> T fillImages(T result, Set<String> fields) {
        if (hasImageField(fields)) {
            return fillImages(result);
        }

        return result;
    }

    protected DataFetchOptions getFetchOptions(Set<String> fields) {
        return DataFetchOptions.builder()
                .withObservers(fields.contains(StringUtils.slashing(IWithObserversEntity.Fields.OBSERVERS, IEntity.Fields.ID)))
                .withRecorderDepartment(fields.contains(StringUtils.slashing(IWithRecorderDepartmentEntity.Fields.RECORDER_DEPARTMENT, IEntity.Fields.ID)))
                .withRecorderPerson(fields.contains(StringUtils.slashing(IWithRecorderPersonEntity.Fields.RECORDER_PERSON, IEntity.Fields.ID)))
                .build();
    }


    protected TripFetchOptions getTripFetchOptions(Set<String> fields) {
        return TripFetchOptions.builder()
                .withLocations(fields.contains(StringUtils.slashing(TripVO.Fields.DEPARTURE_LOCATION, IEntity.Fields.ID))
                        || fields.contains(StringUtils.slashing(TripVO.Fields.RETURN_LOCATION, IEntity.Fields.ID))
                )
                .withProgram(fields.contains(StringUtils.slashing(TripVO.Fields.PROGRAM, IEntity.Fields.ID)))
                .withObservers(fields.contains(StringUtils.slashing(IWithObserversEntity.Fields.OBSERVERS, IEntity.Fields.ID)))
                .withRecorderDepartment(fields.contains(StringUtils.slashing(IWithRecorderDepartmentEntity.Fields.RECORDER_DEPARTMENT, IEntity.Fields.ID)))
                .withRecorderPerson(fields.contains(StringUtils.slashing(IWithRecorderPersonEntity.Fields.RECORDER_PERSON, IEntity.Fields.ID)))
                .withGears(fields.contains(StringUtils.slashing(TripVO.Fields.GEARS, IEntity.Fields.ID)))
                .withLanding(fields.contains(StringUtils.slashing(TripVO.Fields.LANDING_ID))
                        || fields.contains(StringUtils.slashing(TripVO.Fields.LANDING, IEntity.Fields.ID))
                )
                .withSales(fields.contains(StringUtils.slashing(TripVO.Fields.SALE, IEntity.Fields.ID))
                        || fields.contains(StringUtils.slashing(TripVO.Fields.SALES, IEntity.Fields.ID))
                )
                .withExpectedSales(fields.contains(StringUtils.slashing(TripVO.Fields.EXPECTED_SALE, IEntity.Fields.ID))
                        || fields.contains(StringUtils.slashing(TripVO.Fields.EXPECTED_SALES, IEntity.Fields.ID))
                )
                .withSamplingStrata(fields.contains(StringUtils.slashing(TripVO.Fields.SAMPLING_STRATA, IEntity.Fields.ID)))
                .build();
    }


    protected LandingFetchOptions getLandingFetchOptions(Set<String> fields) {
        boolean withSales = fields.contains(StringUtils.slashing(LandingVO.Fields.SALES, IEntity.Fields.ID));
        boolean withSaleIds = !withSales && fields.contains(LandingVO.Fields.SALE_IDS);
        boolean withTrip = fields.contains(StringUtils.slashing(LandingVO.Fields.TRIP, IEntity.Fields.ID));
        boolean withTripSale = withTrip && fields.contains(StringUtils.slashing(LandingVO.Fields.TRIP, TripVO.Fields.SALE, IEntity.Fields.ID))
                || fields.contains(StringUtils.slashing(LandingVO.Fields.TRIP, TripVO.Fields.SALES, IEntity.Fields.ID));
        boolean withTripExpectedSale = withTrip && fields.contains(StringUtils.slashing(LandingVO.Fields.TRIP, TripVO.Fields.EXPECTED_SALE, IEntity.Fields.ID))
                || fields.contains(StringUtils.slashing(LandingVO.Fields.TRIP, TripVO.Fields.EXPECTED_SALES, IEntity.Fields.ID));
        boolean withChildrenEntities = withTrip
                || fields.contains(StringUtils.slashing(LandingVO.Fields.VESSEL_SNAPSHOT, IEntity.Fields.ID));

        SampleFetchOptions sampleFetchOptions = getSampleFetchOptions(fields, LandingVO.Fields.SAMPLES);
        // Avoid to fetch recorder department here
        sampleFetchOptions.setWithRecorderDepartment(false);

        return LandingFetchOptions.builder()
            .withSales(withSales)
            .withSaleIds(withSaleIds)
            .withTrip(withTrip)
            .withTripSales(withTripSale)
            .withTripExpectedSales(withTripExpectedSale)
            .withChildrenEntities(withChildrenEntities)
            .sampleFetchOptions(sampleFetchOptions)
            .build();
    }

    protected OperationFetchOptions getOperationFetchOptions(Set<String> fields) {
        return OperationFetchOptions.builder()
                .withRecorderDepartment(fields.contains(StringUtils.slashing(IWithRecorderDepartmentEntity.Fields.RECORDER_DEPARTMENT, IEntity.Fields.ID)))
                .withRecorderPerson(fields.contains(StringUtils.slashing(IWithRecorderPersonEntity.Fields.RECORDER_PERSON, IEntity.Fields.ID)))
                .withTrip(fields.contains(StringUtils.slashing(IWithTripEntity.Fields.TRIP, IEntity.Fields.ID)))
                .withParentOperation(fields.contains(StringUtils.slashing(OperationVO.Fields.PARENT_OPERATION, IEntity.Fields.ID)))
                .withChildOperation(fields.contains(StringUtils.slashing(OperationVO.Fields.CHILD_OPERATION, IEntity.Fields.ID)))
                .build();
    }

    protected SampleFetchOptions getSampleFetchOptions(Set<String> fields) {
        return SampleFetchOptions.builder()

                .withRecorderDepartment(fields.contains(StringUtils.slashing(SampleVO.Fields.RECORDER_DEPARTMENT, IEntity.Fields.ID)))
                .withMeasurementValues(fields.contains(SampleVO.Fields.MEASUREMENT_VALUES))

                // Enable images only if enable in Pod configuration
                .withImages(this.enableImageAttachments && fields.contains(StringUtils.slashing(SampleVO.Fields.IMAGES, IEntity.Fields.ID)))
                .build();
    }

    protected SampleFetchOptions getSampleFetchOptions(Set<String> fields, String samplePath) {
        return SampleFetchOptions.builder()

                .withRecorderDepartment(fields.contains(StringUtils.slashing(samplePath, SampleVO.Fields.RECORDER_DEPARTMENT, IEntity.Fields.ID)))
                .withMeasurementValues(fields.contains(StringUtils.slashing(samplePath, SampleVO.Fields.MEASUREMENT_VALUES)))

                // Enable images only if enable in Pod configuration
                .withImages(this.enableImageAttachments && fields.contains(StringUtils.slashing(samplePath, SampleVO.Fields.IMAGES, IEntity.Fields.ID)))
                .build();
    }


    protected ObservedLocationFetchOptions getObservedLocationFetchOptions(Set<String> fields) {
        return ObservedLocationFetchOptions.builder()
            .withLocations(fields.contains(StringUtils.slashing(ObservedLocationVO.Fields.LOCATION, IEntity.Fields.ID)))
            .withProgram(fields.contains(StringUtils.slashing(ObservedLocationVO.Fields.PROGRAM, IEntity.Fields.ID)))
            .withObservers(fields.contains(StringUtils.slashing(IWithObserversEntity.Fields.OBSERVERS, IEntity.Fields.ID)))
            .withRecorderDepartment(fields.contains(StringUtils.slashing(IWithRecorderDepartmentEntity.Fields.RECORDER_DEPARTMENT, IEntity.Fields.ID)))
            .withRecorderPerson(fields.contains(StringUtils.slashing(IWithRecorderPersonEntity.Fields.RECORDER_PERSON, IEntity.Fields.ID)))
            .withLandings(fields.contains(StringUtils.slashing(ObservedLocationVO.Fields.LANDINGS, IEntity.Fields.ID)))
            .withSamplingStrata(fields.contains(StringUtils.slashing(ObservedLocationVO.Fields.SAMPLING_STRATA, IEntity.Fields.ID)))
            .build();
    }

    protected SaleFetchOptions getSaleFetchOptions(Set<String> fields) {
        return SaleFetchOptions.builder()
                .withProgram(fields.contains(StringUtils.slashing(SaleVO.Fields.PROGRAM, IEntity.Fields.ID)))
                .withVesselSnapshot(fields.contains(StringUtils.slashing(SaleVO.Fields.VESSEL_SNAPSHOT, IEntity.Fields.ID)))
                .withRecorderDepartment(fields.contains(StringUtils.slashing(IWithRecorderDepartmentEntity.Fields.RECORDER_DEPARTMENT, IEntity.Fields.ID)))
                .withRecorderPerson(fields.contains(StringUtils.slashing(IWithRecorderPersonEntity.Fields.RECORDER_PERSON, IEntity.Fields.ID)))
                .withMeasurementValues(fields.contains(SaleVO.Fields.MEASUREMENT_VALUES))
                .withFishingAreas(fields.contains(StringUtils.slashing(SaleVO.Fields.FISHING_AREAS, IEntity.Fields.ID)))
                .withProducts(fields.contains(StringUtils.slashing(SaleVO.Fields.PRODUCTS, IEntity.Fields.ID)))
                .withBatches(fields.contains(StringUtils.slashing(SaleVO.Fields.BATCHES, IEntity.Fields.ID)))
                .build();
    }

    protected ActivityCalendarFetchOptions getActivityCalendarFetchOptions(Set<String> fields) {
        return ActivityCalendarFetchOptions.builder()
            .withProgram(fields.contains(StringUtils.slashing(ActivityCalendarVO.Fields.PROGRAM, IEntity.Fields.ID)))
            .withRecorderDepartment(fields.contains(StringUtils.slashing(IWithRecorderDepartmentEntity.Fields.RECORDER_DEPARTMENT, IEntity.Fields.ID)))
            .withRecorderPerson(fields.contains(StringUtils.slashing(IWithRecorderPersonEntity.Fields.RECORDER_PERSON, IEntity.Fields.ID)))
            .withMeasurementValues(fields.contains(ActivityCalendarVO.Fields.MEASUREMENT_VALUES))
            .withImages(this.enableImageAttachments && fields.contains(StringUtils.slashing(ActivityCalendarVO.Fields.IMAGES, IEntity.Fields.ID)))
            .withChildrenEntities(fields.contains(StringUtils.slashing(ActivityCalendarVO.Fields.VESSEL_USE_FEATURES, IEntity.Fields.ID))
                || fields.contains(StringUtils.slashing(ActivityCalendarVO.Fields.GEAR_USE_FEATURES, IEntity.Fields.ID)))
            .build();
    }

    /**
     * Restrict to self data and/or department data
     *
     */
    protected <F extends IRootDataFilter> F fillRootDataFilter(F filter, Class<F> filterClass) {
        filter = Beans.nullToEmpty(filter, filterClass);

        // Replace programLabel by ID
        if (StringUtils.isNotBlank(filter.getProgramLabel()) && ArrayUtils.isEmpty(filter.getProgramIds())) {
            // Use optional, to avoid error when programLabel not found (e.g. when changing pod in the App settings)
            Integer[] programIds = this.programService.findIdByLabel(filter.getProgramLabel())
                .map(programId -> new Integer[]{programId})
                .orElse(DataAccessControlService.NO_ACCESS_FAKE_IDS);
            filter.setProgramIds(programIds);
            filter.setProgramLabel(null);
        }

        // Admin: restrict only on programs
        if (authService.isAdmin()) {
            Integer[] authorizedProgramIds = dataAccessControlService.getAllAuthorizedProgramIds(filter.getProgramIds())
                    .orElse(DataAccessControlService.NO_ACCESS_FAKE_IDS);
            filter.setProgramIds(authorizedProgramIds);
            return filter;
        }

        // Restrict to self data and/or department data
        PersonVO user = authService.getAuthenticatedUser().orElse(null);

        // Guest: hide all (Should never occur, because of @IsUser security annotation)
        if (user == null) {
            filter.setRecorderPersonId(DataAccessControlService.NO_ACCESS_FAKE_ID);
            return filter;
        }

        // Limit program access
        Integer[] programIds = dataAccessControlService.getAuthorizedProgramIdsByUserId(user.getId(), filter.getProgramIds())
                // No access
                .orElse(DataAccessControlService.NO_ACCESS_FAKE_IDS);
        filter.setProgramIds(programIds);

        // Stop here if user has no access
        if (programIds == DataAccessControlService.NO_ACCESS_FAKE_IDS) return filter;

        // Limit on own data
        if (!dataAccessControlService.canUserAccessNotSelfData()) {
            // Limit data access to self data
            filter.setRecorderPersonId(user.getId());
            return filter;
        }

        // Limit data access to user's department
        Integer depId = user.getDepartment().getId();
        if (!dataAccessControlService.canDepartmentAccessNotSelfData(depId)) {
            filter.setRecorderDepartmentId(depId);
        }

        return filter;
    }

    protected ActivityCalendarFilterVO fillActivityCalendarFilter(ActivityCalendarFilterVO filter) {

        filter = Beans.nullToEmpty(filter, ActivityCalendarFilterVO.class);

        // Save original state of some properties
        boolean hasRecorderPersonId = filter.getRecorderPersonId() != null;
        boolean hasRecorderDepartmentId = filter.getRecorderDepartmentId() != null;

        // Fill default program
        if (ArrayUtils.isEmpty(filter.getProgramIds()) && StringUtils.isEmpty(filter.getProgramLabel())
            && ProgramEnum.SIH_ACTIFLOT.getId() != -1) {
            filter.setProgramLabel(ProgramEnum.SIH_ACTIFLOT.getLabel());
        }

        // Default rules
        this.fillRootDataFilter(filter, ActivityCalendarFilterVO.class);

        // Limit to user program's locations
        if (filter.getProgramIds() != DataAccessControlService.NO_ACCESS_FAKE_IDS) {

            // Get authorized location ids
            Integer[] locationIds = dataAccessControlService.getAuthorizedLocationIds(
                filter.getProgramIds(),
                ArrayUtils.concat(filter.getRegistrationLocationId(), filter.getRegistrationLocationIds()))
                .orElse(DataAccessControlService.NO_ACCESS_FAKE_IDS);

            // Has access to some locations
            if (locationIds != DataAccessControlService.NO_ACCESS_FAKE_IDS) {
                filter.setRegistrationLocationId(null);
                filter.setRegistrationLocationIds(locationIds);
                // Reset the recorder limitation, if was not set by the user himself
                if (!hasRecorderPersonId) filter.setRecorderPersonId(null);
                if (!hasRecorderDepartmentId) filter.setRecorderDepartmentId(null);
            }
            // No access to any location
            else {
                // Convert filter to use a fake recorder person id.
                // (optimize the query, using a simple predicate)
                filter.setRegistrationLocationId(null);
                filter.setRegistrationLocationIds(null);
                filter.setRecorderPersonId(DataAccessControlService.NO_ACCESS_FAKE_ID);
                filter.setRecorderDepartmentId(null);
            }
        }

        return filter;
    }

    private Optional<Integer> getMainUndefinedOperationGroupId(LandingVO landing) {
        return Optional.ofNullable(landing.getTripId())
                .flatMap(operationGroupService::getMainUndefinedOperationGroupId);
    }
}

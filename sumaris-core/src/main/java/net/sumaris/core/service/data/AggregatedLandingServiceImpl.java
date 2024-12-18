package net.sumaris.core.service.data;

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

import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.dao.administration.programStrategy.ProgramRepository;
import net.sumaris.core.dao.data.MeasurementDao;
import net.sumaris.core.dao.data.operation.OperationGroupRepository;
import net.sumaris.core.dao.referential.metier.MetierRepository;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.service.data.vessel.VesselSnapshotService;
import net.sumaris.core.util.*;
import net.sumaris.core.vo.data.*;
import net.sumaris.core.vo.data.aggregatedLanding.AggregatedLandingVO;
import net.sumaris.core.vo.data.aggregatedLanding.VesselActivityVO;
import net.sumaris.core.vo.filter.AggregatedLandingFilterVO;
import net.sumaris.core.vo.filter.LandingFilterVO;
import net.sumaris.core.vo.filter.ObservedLocationFilterVO;
import net.sumaris.core.vo.filter.OperationGroupFilterVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import net.sumaris.core.vo.referential.metier.MetierVO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Service("aggregatedLandingService")
@Slf4j
public class AggregatedLandingServiceImpl implements AggregatedLandingService {

    private static final LandingFetchOptions defaultLandingFetchOptions = LandingFetchOptions.builder()
        .withVesselSnapshot(true)
        .withRecorderDepartment(true)
        .withRecorderPerson(true)
        .withChildrenEntities(false)
        .build();

    private final LandingService landingService;
    private final TripService tripService;
    private final ObservedLocationService observedLocationService;
    private final OperationGroupRepository operationGroupRepository;
    private final MeasurementDao measurementDao;
    private final MetierRepository metierRepository;
    private final VesselSnapshotService vesselSnapshotService;
    private final ProgramRepository programRepository;
    private final TimeZone dbTimeZone;
    private final boolean enableVesselActivityDateCheck;

    public AggregatedLandingServiceImpl(SumarisConfiguration configuration,
                                        LandingService landingService,
                                        TripService tripService,
                                        ObservedLocationService observedLocationService,
                                        OperationGroupRepository operationGroupRepository,
                                        MeasurementDao measurementDao,
                                        MetierRepository metierRepository,
                                        VesselSnapshotService vesselSnapshotService,
                                        ProgramRepository programRepository) {
        this.landingService = landingService;
        this.tripService = tripService;
        this.observedLocationService = observedLocationService;
        this.operationGroupRepository = operationGroupRepository;
        this.measurementDao = measurementDao;
        this.metierRepository = metierRepository;
        this.vesselSnapshotService = vesselSnapshotService;
        this.programRepository = programRepository;
        this.dbTimeZone = configuration.getDbTimezone();
        this.enableVesselActivityDateCheck = this.dbTimeZone.equals(configuration.getTimezone());
        if (!this.enableVesselActivityDateCheck) {
            log.warn("Disabling aggregated landing dates check, because server and DB have NOT the same time zone.");
        }
    }

    @Override
    public List<AggregatedLandingVO> findAll(AggregatedLandingFilterVO filter) {

        long start = System.currentTimeMillis();

        Preconditions.checkNotNull(filter);
        Preconditions.checkNotNull(filter.getStartDate());
        Preconditions.checkNotNull(filter.getEndDate());
        Preconditions.checkNotNull(filter.getLocationId());
        Preconditions.checkArgument(StringUtils.isNotBlank(filter.getProgramLabel()));

        final Date startDate = Dates.resetTime(filter.getStartDate());
        final Date endDate = Dates.lastSecondOfTheDay(filter.getEndDate());

        List<AggregatedLandingVO> result = new ArrayList<>();

        // Get observations
        List<ObservedLocationVO> observedLocations = observedLocationService.findAll(
            ObservedLocationFilterVO.builder()
                .programLabel(filter.getProgramLabel())
                .locationId(filter.getLocationId())
                .startDate(startDate)
                .endDate(endDate)
                .build(),
            0, 1000, null, null,
            ObservedLocationFetchOptions.copy(defaultLandingFetchOptions));

        ConcurrentHashMap<VesselSnapshotVO, Map<Date, List<LandingVO>>> landingsByBateByVessel = new ConcurrentHashMap<>();
        observedLocations
            //.parallelStream() // Warning: Can cause concurrent exceptions on landingsByBateByVessel.computeIfAbsent
            .forEach(observedLocation -> {

            List<LandingVO> landings = landingService.findAll(LandingFilterVO.builder().observedLocationId(observedLocation.getId()).build(),
                null, defaultLandingFetchOptions);

            landings.stream()
                .filter(landing -> landing.getId() != null)
                .forEach(landing -> {
                    if (landing.getDateTime() == null)
                        throw new SumarisTechnicalException(String.format("The landing date is missing for landing id=%s", landing.getId()));

                    landingsByBateByVessel
                        .computeIfAbsent(landing.getVesselSnapshot(), x -> new HashMap<>())
                        .computeIfAbsent(Dates.resetTime(landing.getDateTime()), x -> new ArrayList<>())
                        .add(landing);
                });
        });

        // Build aggregated landings
        landingsByBateByVessel.keySet().forEach(vessel -> {

            AggregatedLandingVO aggregatedLanding = new AggregatedLandingVO();
            result.add(aggregatedLanding);
            aggregatedLanding.setObservedLocationId(filter.getObservedLocationId());
            aggregatedLanding.setVesselSnapshot(vessel);
//            aggregatedLanding.setId(vessel.getId());
            Map<Date, List<LandingVO>> landingsByDate = landingsByBateByVessel.get(vessel);

            // Iterate days
            landingsByDate.keySet().forEach(date -> landingsByDate.get(date).forEach(landing -> {

                VesselActivityVO activity = new VesselActivityVO();
                activity.setDate(date);
                activity.setRankOrder(landing.getRankOrder());
                activity.setComments(landing.getComments());

                // Add parent links
                activity.setObservedLocationId(landing.getObservedLocationId());
                activity.setLandingId(landing.getId());

                // Add measurements
                loadLandingMeasurements(landing);
                activity.setMeasurementValues(landing.getMeasurementValues());

                // Add metier activity
                if (landing.getTripId() != null) {
                    activity.setTripId(landing.getTripId());

                    // Get trip's metier
                    List<MetierVO> metiers = operationGroupRepository.getMetiersByTripId(landing.getTripId());
                    metiers.forEach(metier -> activity.getMetiers().add(metier));
                }

                // Add this activity
                aggregatedLanding.getVesselActivities().add(activity);

            }));

        });

        if (log.isDebugEnabled()) {
            log.debug(String.format("findAll done in %s ms", System.currentTimeMillis() - start));
        }

        return result;
    }

    @Override
    public List<AggregatedLandingVO> saveAll(AggregatedLandingFilterVO filter, List<AggregatedLandingVO> aggregatedLandings) {

        long startTime = System.currentTimeMillis();
        if (log.isDebugEnabled()) log.debug("Saving aggregated landings...");

        Preconditions.checkNotNull(filter);
        Preconditions.checkNotNull(filter.getObservedLocationId());
        Preconditions.checkNotNull(aggregatedLandings);
        // Check all aggregated landings have a vessel
        aggregatedLandings.forEach(aggregatedLanding -> {
            Preconditions.checkNotNull(aggregatedLanding.getVesselSnapshot());
            Preconditions.checkNotNull(aggregatedLanding.getVesselSnapshot().getVesselId());
        });

        // Check all activity have date without time (if DB TZ = Server TZ)
        if (this.enableVesselActivityDateCheck) {
            aggregatedLandings.forEach(aggregatedLanding -> aggregatedLanding.getVesselActivities()
                .forEach(activity -> {
                    Date expectedDate = Dates.resetTime(activity.getDate(), dbTimeZone);
                    Preconditions.checkArgument(
                        activity.getDate().equals(expectedDate),
                        String.format("Invalid date. Expected %s - Actual %s", expectedDate, activity.getDate())
                    );
                }));
        }

        // Load VesselSnapshot Entity
        aggregatedLandings.parallelStream()
            .forEach(aggregatedLanding -> {
                aggregatedLanding.setObservedLocationId(filter.getObservedLocationId());
                aggregatedLanding.setVesselSnapshot(vesselSnapshotService.getByIdAndDate(aggregatedLanding.getVesselSnapshot().getVesselId(), null));
            });

        // Collect aggregated landings by date
        Map<Date, Multimap<Integer, VesselActivityVO>> aggregatedLandingsByDate = new HashMap<>();
        aggregatedLandings
            .forEach(aggregatedLanding -> aggregatedLanding.getVesselActivities()
                .forEach(activity -> aggregatedLandingsByDate
                    .computeIfAbsent(activity.getDate(), x -> ArrayListMultimap.create())
                    .put(aggregatedLanding.getVesselSnapshot().getVesselId(), activity)));

        // Get parent observed location
        ObservedLocationVO parent = observedLocationService.get(filter.getObservedLocationId());
        // Get existing observations
        final Date startDate = Dates.resetTime(filter.getStartDate(), this.dbTimeZone);
        final Date endDate = Dates.lastSecondOfTheDay(Dates.resetTime(filter.getEndDate(), this.dbTimeZone));
        List<ObservedLocationVO> observedLocations = observedLocationService.findAll(
            ObservedLocationFilterVO.builder()
                .programLabel(filter.getProgramLabel())
                .locationId(filter.getLocationId())
                .startDate(startDate)
                .endDate(endDate)
                .build(),
            0, 1000, null, null,
            ObservedLocationFetchOptions.copy(defaultLandingFetchOptions));

        // Create observed location if missing
        Set<Date> existingDates = observedLocations.stream().map(ObservedLocationVO::getStartDateTime)
            .map(Dates::resetTime)
            .collect(Collectors.toSet());
        if (observedLocations.size() != existingDates.size()) {
            throw new SumarisTechnicalException("There are several observations on same day. This is not implemented for now.");
        }
        Set<Integer> observationIdsToCheck = new HashSet<>();
        Map<Date, ObservedLocationVO> observedLocationByDate = observedLocations.stream()
            .collect(Collectors.toMap(
                ol -> Dates.resetTime(ol.getStartDateTime()),
                ol -> ol
            ));
        aggregatedLandingsByDate.keySet().forEach(date -> {
            if (!observedLocationByDate.containsKey(date)) {
                observedLocationByDate.put(date, createObservedLocation(parent, date, filter.getProgramLabel()));
            }
        });

        // Iterate over dates
        aggregatedLandingsByDate.keySet().forEach(date -> {
            Multimap<Integer, VesselActivityVO> activityByVesselId = aggregatedLandingsByDate.get(date);
            ObservedLocationVO observedLocation = observedLocationByDate.remove(date);
            ListMultimap<Integer, LandingVO> landingsByVesselId = ArrayListMultimap.create();//Beans.splitByNotUniqueProperty(existingLandings, LandingVO.Fields.VESSEL_SNAPSHOT);
            getLandingsByObservedLocationId(observedLocation.getId())
                .forEach(landing -> landingsByVesselId.put(landing.getVesselSnapshot().getVesselId(), landing));

            // Iterate over vessel's activities for the date
            activityByVesselId.keySet().forEach(vesselId -> {
                Collection<VesselActivityVO> activities = activityByVesselId.get(vesselId);
                List<LandingVO> landingsToSave = new ArrayList<>(landingsByVesselId.removeAll(vesselId));
                List<Integer> landingIdsToRemove = Beans.collectIds(landingsToSave);
                AtomicBoolean landingsDirty = new AtomicBoolean(false);
                // Part 1 : Landings
                activities.forEach(activity -> {
                    if (createOrUpdateLandings(observedLocation, landingsToSave, landingIdsToRemove, vesselId, activity))
                        landingsDirty.set(true);
                });
                if (landingsDirty.get()) {
                    // Save all landings
                    landingsToSave.forEach(landing -> saveLanding(observedLocation, landing));
                    // Add the observed location to check list
                    observationIdsToCheck.add(observedLocation.getId());
                }
                if (!landingIdsToRemove.isEmpty()) {
                    // Delete remaining landings
                    landingService.delete(landingIdsToRemove);

                    // Add the observed location to check list
                    observationIdsToCheck.add(observedLocation.getId());
                }

                // Part 2 : Trips
                activities.forEach(activity -> {
                    LandingVO landing = landingsToSave.stream().filter(landingVO -> Objects.equals(landingVO.getRankOrder(), activity.getRankOrder())).findFirst().orElseThrow(IllegalArgumentException::new);
                    activity.setLandingId(landing.getId());
                    activity.setObservedLocationId(landing.getObservedLocationId());

                    if (createOrUpdateTrip(observedLocation, landing, vesselId, activity)) {
                        // Add the observed location to check list
                        observationIdsToCheck.add(observedLocation.getId());
                    }
                });
            });
            // Delete remaining landings
            if (!landingsByVesselId.isEmpty()) {
                landingService.delete(Beans.collectIds(landingsByVesselId.values()));
            }
        });

        if (!observedLocationByDate.isEmpty()) {
            // some observed locations remains, delete them
            observedLocationByDate.values().forEach(observedLocation -> deleteObservedLocationWithLandings(observedLocation.getId(), null));
        }
        // Check emptiness of observations
        if (!observationIdsToCheck.isEmpty()) {
            updateOrDeleteEmptyObservedLocations(observationIdsToCheck);
        }

        if (log.isDebugEnabled()) {
            log.debug("Saving aggregated landings [OK] in {}", TimeUtils.printDurationFrom(startTime));
        }

        return aggregatedLandings;
    }

    @Override
    public void deleteAll(AggregatedLandingFilterVO filter, List<Integer> vesselIds) {

        long start = System.currentTimeMillis();

        Preconditions.checkNotNull(filter);
        Preconditions.checkNotNull(filter.getObservedLocationId()); // not really used
        Preconditions.checkNotNull(filter.getStartDate());
        Preconditions.checkNotNull(filter.getEndDate());
        Preconditions.checkNotNull(filter.getLocationId());
        Preconditions.checkArgument(StringUtils.isNotBlank(filter.getProgramLabel()));

        if (CollectionUtils.isEmpty(vesselIds)) {
            return;
        }

        // Get existing observations
        final Date startDate = Dates.resetTime(filter.getStartDate());
        final Date endDate = Dates.lastSecondOfTheDay(filter.getEndDate());
        List<ObservedLocationVO> observedLocations = observedLocationService.findAll(
            ObservedLocationFilterVO.builder()
                .programLabel(filter.getProgramLabel())
                .locationId(filter.getLocationId())
                .startDate(startDate)
                .endDate(endDate)
                .build(),
            0, 1000, null, null,
            ObservedLocationFetchOptions.copy(defaultLandingFetchOptions));

        observedLocations.forEach(observedLocation -> vesselIds.forEach(vesselId -> {
            landingService.deleteAllByFilter(LandingFilterVO.builder()
                .observedLocationId(observedLocation.getId())
                .vesselId(vesselId)
                .build());
        }));

        updateOrDeleteEmptyObservedLocations(Beans.collectIds(observedLocations));

        if (log.isDebugEnabled()) {
            log.debug(String.format("Deleting done in %s ms", System.currentTimeMillis() - start));
        }
    }

    /* protected methods */

    private List<LandingVO> getLandingsByObservedLocationId(int observedLocationId) {
        return getLandingsByObservedLocationId(observedLocationId, defaultLandingFetchOptions);
    }

    private List<LandingVO> getLandingsByObservedLocationId(int observedLocationId, LandingFetchOptions fetchOptions) {
        return landingService.findAll(
            LandingFilterVO.builder().observedLocationId(observedLocationId).build(),
            null,
            fetchOptions);
    }

    private LandingVO saveLanding(@Nonnull ObservedLocationVO parent, @Nonnull LandingVO landing) {

        // Set default value for recorder department and person
        DataBeans.setDefaultRecorderDepartment(landing, parent.getRecorderDepartment());
        DataBeans.setDefaultRecorderPerson(landing, parent.getRecorderPerson());

        // Make sure to return a valid landing VO
        if (landing.getObservedLocationId() == null) {
            landing.setObservedLocationId(parent.getId());
        }

        return landingService.save(landing);
    }

    private void updateOrDeleteEmptyObservedLocations(Collection<Integer> observationIds) {

        if (CollectionUtils.isEmpty(observationIds)) return;

        observationIds.forEach(observationId -> {
            // clean if no landing at all
            List<LandingVO> landings = getLandingsByObservedLocationId(observationId);

            if (CollectionUtils.isEmpty(landings)) {

                // No landing, delete observation
                observedLocationService.delete(observationId);
                if (log.isDebugEnabled()) {
                    log.debug(String.format("Delete observation (id=%s) without landing", observationId));
                }

            } else if (landings.stream().allMatch(this::isLandingFullyEmpty)) {

                // All landings are empty, delete observation
                deleteObservedLocationWithLandings(observationId, landings);

            } else {

                // Just Update
                ObservedLocationVO observedLocation = observedLocationService.get(observationId);
                observedLocationService.save(observedLocation, null);
            }
        });
    }

    private void deleteObservedLocationWithLandings(int observedLocationId, List<LandingVO> landings) {

        // Get landings if not provided
        if (landings == null) {
            landings = getLandingsByObservedLocationId(observedLocationId);
        }
        // Delete landings
        landingService.delete(Beans.collectIds(landings));

        // Delete observed location
        observedLocationService.delete(observedLocationId);
        if (log.isDebugEnabled()) {
            log.debug(String.format("Delete observation (id=%s) with empty landings", observedLocationId));
        }
    }

    private ObservedLocationVO createObservedLocation(ObservedLocationVO parent, Date date, String programLabel) {
        ObservedLocationVO observedLocation = new ObservedLocationVO();
        observedLocation.setStartDateTime(date);
        observedLocation.setEndDateTime(Dates.lastSecondOfTheDay(date));
        observedLocation.setLocation(parent.getLocation());
        observedLocation.setProgram(programRepository.getByLabel(programLabel));
        observedLocation.setObservers(Beans.getSet(parent.getObservers()));
        observedLocation.setRecorderDepartment(parent.getRecorderDepartment());
        observedLocation.setRecorderPerson(parent.getRecorderPerson());
        if (log.isDebugEnabled()) {
            log.debug(String.format("Create observed location from %s to %s, program: %s, location: %s",
                observedLocation.getStartDateTime(), observedLocation.getEndDateTime(), observedLocation.getProgram().getLabel(), observedLocation.getLocation().getLabel()));
        }
        return observedLocationService.save(observedLocation, null);
    }

    private boolean createOrUpdateLandings(ObservedLocationVO observedLocation, List<LandingVO> landings, List<Integer> landingIdsToRemove, Integer vesselId, VesselActivityVO activity) {
        boolean landingsDirty = false;

        LandingVO landing = landings.stream().filter(landingVO -> Objects.equals(landingVO.getRankOrder(), activity.getRankOrder())).findFirst().orElse(null);

        // Get measurements
        if (hasMeasurements(activity)) {
            // If a measurement exists

            if (landing == null) {
                landing = createLanding(observedLocation, vesselId, activity);
                landing.setMeasurementValues(Maps.newHashMap(activity.getMeasurementValues()));
                landings.add(landing);
                landingsDirty = true;
                if (log.isDebugEnabled()) {
                    log.debug(String.format("Add landing on observation (id=%s, date=%s) for vessel %s, comment '%s'",
                        observedLocation.getId(), observedLocation.getStartDateTime(), landing.getVesselSnapshot().getVesselId(), landing.getComments()));
                }

            } else {

                // Remove from list to remove
                landingIdsToRemove.remove(landing.getId());

                // Load its measurements
                loadLandingMeasurements(landing);

//                if (log.isDebugEnabled()) {
//                    log.debug(String.format("activity comment:'%s', measurements: %s", activity.getComments(), activity.getMeasurementValues()));
//                    log.debug(String.format("landing comment:'%s', measurements: %s", landing.getComments(), landing.getMeasurementValues()));
//                }

                if (!containsMeasurements(landing, activity)) {
                    landing.getMeasurementValues().putAll(activity.getMeasurementValues());
                    landingsDirty = true;
                    if (log.isDebugEnabled()) {
                        log.debug(String.format("Update landing on observation (id=%s, date=%s) for vessel %s, measurements %s",
                            observedLocation.getId(), observedLocation.getStartDateTime(), landing.getVesselSnapshot().getVesselId(), activity.getMeasurementValues()));
                    }
                }
                if (!Objects.equals(activity.getComments(), landing.getComments())) {
                    landing.setComments(activity.getComments());
                    landingsDirty = true;
                    if (log.isDebugEnabled()) {
                        log.debug(String.format("Update landing on observation (id=%s, date=%s) for vessel %s, comment '%s'",
                            observedLocation.getId(), observedLocation.getStartDateTime(), landing.getVesselSnapshot().getVesselId(), activity.getComments()));
                    }

                }
            }

        } else if (landing != null) {

            if (isLandingEmpty(landing)) {

                landings.remove(landing);
                landingsDirty = true;
                if (log.isDebugEnabled()) {
                    log.debug(String.format("Remove landing on observation (id=%s, date=%s) for vessel %s",
                        observedLocation.getId(), observedLocation.getStartDateTime(), landing.getVesselSnapshot().getVesselId()));
                }

            } else {

                if (hasMeasurements(landing)) {

                    throw new SumarisTechnicalException("Should not happens. The activity measurements is mandatory.");

//                    landing.setMeasurementValues(new HashMap<>());
//                    landing.setComments(null);
//                    saved = true;
//                    if (log.isDebugEnabled()) {
//                        log.debug(String.format("Update landing on observation (id=%s, date=%s) for vessel %s, situation and comment removed",
//                            observedLocation.getId(), observedLocation.getStartDateTime(), landing.getVesselSnapshot().getId()));
//                    }

                }
            }
        }

        return landingsDirty;
    }

    private boolean containsMeasurements(IWithMeasurementValues source, IWithMeasurementValues target) {
        if (MapUtils.isEmpty(source.getMeasurementValues()))
            return false;

        if (MapUtils.isEmpty(target.getMeasurementValues())) {
            log.warn("Cannot check if the source measurement value contains specified values, because target measurement values are empty.");
            return false;
        }

        if (source.getMeasurementValues() instanceof IdentityHashMap || target.getMeasurementValues() instanceof IdentityHashMap) {
            throw new IllegalArgumentException("Cannot compare IdentityHashMap's");
        }

        return target.getMeasurementValues().entrySet().stream()
            .allMatch(entry -> source.getMeasurementValues().containsKey(entry.getKey()) && source.getMeasurementValues().get(entry.getKey()).equals(entry.getValue()));
    }

    private boolean createOrUpdateTrip(ObservedLocationVO observedLocation, LandingVO landing, Integer vesselId, VesselActivityVO activity) {

        Preconditions.checkNotNull(landing, "The landing should already exists.");
        Integer tripId = landing.getTrip() != null ? landing.getTrip().getId() : landing.getTripId();

        boolean tripDirty = false;

        if (!activity.getMetiers().isEmpty()) {
            // At least 1 metier is active, create or update existing trip
            TripVO trip;
            if (tripId == null) {
                // Create new trip
                trip = createTrip(observedLocation, vesselId, activity);
                tripDirty = true;
                if (log.isDebugEnabled()) {
                    log.debug(String.format("Add trip on observation (id=%s) for vessel %s",
                        observedLocation.getId(), vesselId));
                }
            } else {
                // Load it
                trip = loadTrip(tripId);
            }

            // Collect metiers to remove
            List<Integer> activityMetierIds = Beans.collectIds(activity.getMetiers());
            List<Integer> metierIdsToRemove = trip.getMetiers() == null ? null :
                trip.getMetiers().stream()
                    .map(MetierVO::getId)
                    .filter(metierId -> !activityMetierIds.contains(metierId))
                    .collect(Collectors.toList());

            // Iterate over missing metier to add
            for (ReferentialVO metierRef : activity.getMetiers()) {
                MetierVO metier = metierRepository.get(metierRef.getId());
                // Add missing metier
                if (!trip.getMetiers().contains(metier)) {
                    trip.getMetiers().add(metier);
                    tripDirty = true;
                    if (log.isDebugEnabled()) {
                        log.debug(String.format("Add Metier %s (id=%s) on trip (id=%s)", metier.getLabel() + " - " + metier.getName(), metier.getId(), tripId));
                    }
                }

                // TODO: Create operation only if needed
//                // Get operation group
//                OperationGroupVO operationGroup = trip.getOperationGroups().stream().filter(operationGroupVO -> operationGroupVO.getMetier().equals(metier)).findFirst().orElse(null);
//
//                // Add missing operation group
//                if (operationGroup == null) {
//                    int maxRankOrder = trip.getOperationGroups().stream().map(OperationGroupVO::getRankOrderOnPeriod).max(Integer::compareTo).orElse(0);
//                    operationGroup = createOperationGroup(metier, maxRankOrder + 1);
//                    operationGroup.setTripId(tripId);
//                    trip.getOperationGroups().add(operationGroup);
//                    tripDirty = true;
//                    if (log.isDebugEnabled()) {
//                        log.debug(String.format("Add operation on trip (id=%s) for metier '%s' (id=%s)", tripId, metier.getLabel() + " - " + metier.getName(), metier.getId()));
//                    }
//                }
            }

            // Remove remaining metiers
            if (CollectionUtils.isNotEmpty(metierIdsToRemove)) {
                trip.getMetiers().removeIf(metier -> metierIdsToRemove.contains(metier.getId()));
                trip.getOperationGroups().removeIf(operationGroup -> metierIdsToRemove.contains(operationGroup.getMetier().getId()));
                tripDirty = true;
            }

            if (tripDirty) {
                // Save trip
                if (trip.getLanding() == null) {
                    trip.setLanding(landing);
                }
                trip.setLandingId(landing.getId());
                TripVO savedTrip = saveTrip(trip);
                activity.setTripId(savedTrip.getId());
                if (log.isDebugEnabled()) {
                    log.debug(String.format("Trip (id=%s) with landing (id=%s) successfully saved", savedTrip.getId(), savedTrip.getLanding().getId()));
                }
            }

            return tripDirty;

        } else if (tripId != null) {

            // Delete the whole trip
            tripService.delete(tripId);
            if (log.isDebugEnabled()) {
                log.debug(String.format("Trip (id=%s) successfully deleted", tripId));
            }
            activity.setTripId(null);
            return true;
        }

        return false;
    }

    private TripVO saveTrip(TripVO trip) {

        Preconditions.checkNotNull(trip);
        Preconditions.checkNotNull(trip.getLanding());

        // Trip itself (landing updated by tripService)
        TripVO savedTrip = tripService.save(
            trip,
            TripSaveOptions.builder().withLanding(true).build() // TODO check if needed, landing should be already created
        );

        // Save metiers
        operationGroupRepository.saveMetiersByTripId(savedTrip.getId(), trip.getMetiers());

        // TODO: save operation groups (if default needed)

        return savedTrip;
    }

    private OperationGroupVO createOperationGroup(MetierVO metier, int rankOrder) {
        OperationGroupVO operationGroup = new OperationGroupVO();
        operationGroup.setMetier(metier);
        operationGroup.setRankOrderOnPeriod(rankOrder);
        PhysicalGearVO physicalGear = new PhysicalGearVO();
        physicalGear.setGear(metier.getGear());
        physicalGear.setRankOrder(rankOrder);
        return operationGroup;
    }

    private TripVO loadTrip(int tripId) {
        //noinspection UnnecessaryBoxing
        TripVO trip = tripService.get(Integer.valueOf(tripId));
        // Load metiers and operation groups
        if (CollectionUtils.isEmpty(trip.getMetiers())) {
            trip.setMetiers(operationGroupRepository.getMetiersByTripId(tripId));
        }
        if (CollectionUtils.isEmpty(trip.getOperationGroups())) {
            trip.setOperationGroups(operationGroupRepository.findAll(
                OperationGroupFilterVO.builder().tripId(tripId).onlyDefined(true).build()
            ));
        }

        return trip;
    }

    private TripVO createTrip(ObservedLocationVO observedLocation, Integer vesselId, VesselActivityVO activity) {
        VesselSnapshotVO vessel = new VesselSnapshotVO();
        vessel.setVesselId(vesselId);
        TripVO trip = new TripVO();
        trip.setObservedLocationId(observedLocation.getId());
        trip.setProgram(observedLocation.getProgram());
        trip.setVesselSnapshot(vessel);
        // add minutes to startDate to prevent natural Id integrity violation
        trip.setDepartureDateTime(Dates.addMinutes(activity.getDate(), activity.getRankOrder() - 1));
        trip.setReturnDateTime(Dates.lastSecondOfTheDay(activity.getDate()));
        trip.setDepartureLocation(observedLocation.getLocation());
        trip.setReturnLocation(observedLocation.getLocation());
        trip.setObservers(Beans.getSet(observedLocation.getObservers()));
        trip.setRecorderDepartment(observedLocation.getRecorderDepartment());
        trip.setRecorderPerson(observedLocation.getRecorderPerson());
        trip.setMetiers(new ArrayList<>());
        trip.setOperationGroups(new ArrayList<>());
        return trip;
    }

    private boolean hasMeasurements(IWithMeasurementValues entity) {
        return MapUtils.isNotEmpty(entity.getMeasurementValues()) && entity.getMeasurementValues().values().stream().noneMatch(StringUtils::isBlank);
    }

    private boolean isLandingEmpty(LandingVO landing) {
        return landing.getTripId() == null
            && landing.getTrip() == null;
    }

    private boolean isLandingFullyEmpty(LandingVO landing) {
        if (!isLandingEmpty(landing))
            return false;

        // Load its measurements
        loadLandingMeasurements(landing);

        return !hasMeasurements(landing);
    }

    private void loadLandingMeasurements(LandingVO landing) {
        if (landing.getMeasurementValues() == null) {
            Map<Integer, String> result = new HashMap<>();
            Optional.ofNullable(measurementDao.getLandingMeasurementsMap(landing.getId())).ifPresent(result::putAll);
            Optional.ofNullable(measurementDao.getLandingSurveyMeasurementsMap(landing.getId())).ifPresent(result::putAll);
            landing.setMeasurementValues(result);
        }
    }

    private LandingVO createLanding(ObservedLocationVO parent, Integer vesselId, VesselActivityVO activity) {
        VesselSnapshotVO vessel = new VesselSnapshotVO();
        vessel.setVesselId(vesselId);
        LandingVO landing = new LandingVO();
        landing.setVesselSnapshot(vessel);
        landing.setObservedLocation(parent);
        landing.setProgram(parent.getProgram());
        landing.setDateTime(parent.getStartDateTime());
        landing.setLocation(parent.getLocation());
        landing.setRankOrder(activity.getRankOrder());
        landing.setComments(activity.getComments());
        landing.setRecorderDepartment(parent.getRecorderDepartment());
        landing.setRecorderPerson(parent.getRecorderPerson());
        return landing;
    }

}

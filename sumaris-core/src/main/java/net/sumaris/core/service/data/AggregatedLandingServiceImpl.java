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
import net.sumaris.core.dao.administration.programStrategy.ProgramDao;
import net.sumaris.core.dao.data.*;
import net.sumaris.core.dao.referential.metier.MetierRepository;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.util.Beans;
import net.sumaris.core.util.DataBeans;
import net.sumaris.core.util.Dates;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.data.*;
import net.sumaris.core.vo.data.aggregatedLanding.AggregatedLandingVO;
import net.sumaris.core.vo.data.aggregatedLanding.VesselActivityVO;
import net.sumaris.core.vo.filter.AggregatedLandingFilterVO;
import net.sumaris.core.vo.filter.LandingFilterVO;
import net.sumaris.core.vo.filter.ObservedLocationFilterVO;
import net.sumaris.core.vo.referential.MetierVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Service("aggregatedLandingService")
public class AggregatedLandingServiceImpl implements AggregatedLandingService {

    private static final Logger log = LoggerFactory.getLogger(AggregatedLandingServiceImpl.class);

    private static final DataFetchOptions defaultFetchOption = DataFetchOptions.builder()
        .withRecorderDepartment(true)
        .withRecorderPerson(true)
        .withObservers(false)
        .withChildrenEntities(false)
        .build();

    private final LandingRepository landingRepository;
    private final TripRepository tripRepository;
    private final ObservedLocationDao observedLocationDao;
    private final OperationGroupDao operationGroupDao;
    private final MeasurementDao measurementDao;
    private final MetierRepository metierRepository;
    private final VesselService vesselService;
    private final ProgramDao programDao;

    public AggregatedLandingServiceImpl(LandingRepository landingRepository,
                                        TripRepository tripRepository,
                                        ObservedLocationDao observedLocationDao,
                                        OperationGroupDao operationGroupDao,
                                        MeasurementDao measurementDao,
                                        MetierRepository metierRepository,
                                        VesselService vesselService,
                                        ProgramDao programDao) {
        this.landingRepository = landingRepository;
        this.tripRepository = tripRepository;
        this.observedLocationDao = observedLocationDao;
        this.operationGroupDao = operationGroupDao;
        this.measurementDao = measurementDao;
        this.metierRepository = metierRepository;
        this.vesselService = vesselService;
        this.programDao = programDao;
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
        List<ObservedLocationVO> observedLocations = observedLocationDao.findByFilter(
            ObservedLocationFilterVO.builder()
                .programLabel(filter.getProgramLabel())
                .locationId(filter.getLocationId())
                .startDate(startDate)
                .endDate(endDate)
                .build(),
            0, 1000, null, null,
            defaultFetchOption);

        ConcurrentHashMap<VesselSnapshotVO, Map<Date, List<LandingVO>>> landingsByBateByVessel = new ConcurrentHashMap<>();
        observedLocations.parallelStream().forEach(observedLocation -> {

            List<LandingVO> landings = getLandings(observedLocation.getId());

            landings.stream()
                .filter(landing -> landing.getId() != null)
                .forEach(landing -> {
                    if (landing.getDateTime() == null)
                        throw new SumarisTechnicalException(String.format("The landing date is missing for landing id=%s", landing.getId()));

                    Map<Date, List<LandingVO>> landingsByDate = landingsByBateByVessel.computeIfAbsent(landing.getVesselSnapshot(), x -> new HashMap<>());
                    landingsByDate.computeIfAbsent(Dates.resetTime(landing.getDateTime()), x -> new ArrayList<>()).add(landing);
                });
        });

        // Build aggregated landings
        landingsByBateByVessel.keySet().forEach(vessel -> {

            AggregatedLandingVO aggregatedLanding = new AggregatedLandingVO();
            result.add(aggregatedLanding);
            aggregatedLanding.setVesselSnapshot(vessel);
            aggregatedLanding.setId(vessel.getId());
            Map<Date, List<LandingVO>> landingsByDate = landingsByBateByVessel.get(vessel);

            // Iterate days
            landingsByDate.keySet().forEach(date -> landingsByDate.get(date).forEach(landing -> {

                VesselActivityVO activity = new VesselActivityVO();
                activity.setDate(date);
                activity.setRankOrder(landing.getRankOrderOnVessel());
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
                    List<MetierVO> metiers = operationGroupDao.getMetiersByTripId(landing.getTripId());
                    metiers.forEach(metier -> activity.getMetiers().add(metier));

                    // Get operation metier
//                    List<OperationGroupVO> operationGroups = operationGroupDao.getAllByTripId(landing.getTripId());
//                    operationGroups.forEach(operationGroup -> {
//
//                        activity.getMetiers().add(operationGroup.getMetier());
//
//                        // Set comments from operation
//                        if (StringUtils.isNotBlank(operationGroup.getComments())) {
//                            if (StringUtils.isBlank(activity.getComments())) {
//                                activity.setComments(operationGroup.getComments());
//                            } else if (!Objects.equals(activity.getComments(), operationGroup.getComments())) {
//                                activity.setComments(activity.getComments() + "\n" + operationGroup.getComments());
//                            }
//                        }
//                    });
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
    public List<AggregatedLandingVO> saveAllByObservedLocationId(AggregatedLandingFilterVO filter, List<AggregatedLandingVO> aggregatedLandings) {

        long start = System.currentTimeMillis();

        Preconditions.checkNotNull(filter);
        Preconditions.checkNotNull(filter.getObservedLocationId());
        Preconditions.checkNotNull(aggregatedLandings);
        // Check all aggregated landings have a vessel
        aggregatedLandings.forEach(aggregatedLanding -> {
            Preconditions.checkNotNull(aggregatedLanding.getVesselSnapshot());
            Preconditions.checkNotNull(aggregatedLanding.getVesselSnapshot().getId());
        });
        // Check all activity have date without time
        aggregatedLandings.forEach(aggregatedLanding -> aggregatedLanding.getVesselActivities().forEach(activity -> {
            Preconditions.checkArgument(activity.getDate().equals(Dates.resetTime(activity.getDate())));
        }));

        // Load VesselSnapshot Entity
        aggregatedLandings.parallelStream().forEach(aggregatedLanding -> aggregatedLanding.setVesselSnapshot(vesselService.getSnapshotByIdAndDate(aggregatedLanding.getVesselSnapshot().getId(), null)));

        // Collect aggregated landings by date
        Map<Date, Multimap<Integer, VesselActivityVO>> aggregatedLandingsByDate = new HashMap<>();
        aggregatedLandings.forEach(aggregatedLanding -> aggregatedLanding.getVesselActivities().forEach(activity -> {
            Multimap<Integer, VesselActivityVO> activityByVessel = aggregatedLandingsByDate.computeIfAbsent(activity.getDate(), x -> ArrayListMultimap.create());
            activityByVessel.put(aggregatedLanding.getVesselSnapshot().getId(), activity);
        }));

        // Get parent observed location
        ObservedLocationVO parent = observedLocationDao.get(filter.getObservedLocationId());
        // Get existing observations
        final Date startDate = Dates.resetTime(filter.getStartDate());
        final Date endDate = Dates.lastSecondOfTheDay(filter.getEndDate());
        List<ObservedLocationVO> observedLocations = observedLocationDao.findByFilter(
            ObservedLocationFilterVO.builder()
                .programLabel(filter.getProgramLabel())
                .locationId(filter.getLocationId())
                .startDate(startDate)
                .endDate(endDate)
                .build(),
            0, 1000, null, null,
            defaultFetchOption);

        // Create observed location if missing
        Set<Date> existingDates = observedLocations.stream().map(ObservedLocationVO::getStartDateTime).map(Dates::resetTime).collect(Collectors.toSet());
        if (observedLocations.size() != existingDates.size()) {
            throw new SumarisTechnicalException("There are several observations on same day. This is not implemented for now.");
        }
        Set<Integer> observationIdsToCheck = new HashSet<>();
        Map<Date, ObservedLocationVO> observedLocationByDate = new HashMap<>();
        observedLocations.forEach(observedLocation -> observedLocationByDate.put(Dates.resetTime(observedLocation.getStartDateTime()), observedLocation));
        aggregatedLandingsByDate.keySet().forEach(date -> {
            if (!observedLocationByDate.containsKey(date)) {
                observedLocationByDate.put(date, createObservedLocation(parent, date, filter.getProgramLabel()));
            }
        });

        // Iterate over dates
        aggregatedLandingsByDate.keySet().forEach(date -> {
            Multimap<Integer, VesselActivityVO> activityByVesselId = aggregatedLandingsByDate.get(date);
            ObservedLocationVO observedLocation = observedLocationByDate.get(date);
            ListMultimap<Integer, LandingVO> landingsByVesselId = ArrayListMultimap.create();//Beans.splitByNotUniqueProperty(existingLandings, LandingVO.Fields.VESSEL_SNAPSHOT);
            List<LandingVO> existingLandings = getLandings(observedLocation.getId());
            existingLandings.forEach(landing -> landingsByVesselId.put(landing.getVesselSnapshot().getId(), landing));

            // Iterate over vessel's activities for the date
            activityByVesselId.keySet().forEach(vesselId -> {
                Collection<VesselActivityVO> activities = activityByVesselId.get(vesselId);
                List<LandingVO> landingsToSave = landingsByVesselId.get(vesselId);
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
                    landingRepository.deleteByIdIn(landingIdsToRemove);
                    // Add the observed location to check list
                    observationIdsToCheck.add(observedLocation.getId());
                }

                // Part 2 : Trips
                activities.forEach(activity -> {
                    if (createOrUpdateTrips(observedLocation, landingsByVesselId.get(vesselId), vesselId, activity)) {
                        // Add the observed location to check list
                        observationIdsToCheck.add(observedLocation.getId());
                    }
                });
            });
        });

        // Check emptiness of observations
        if (!observationIdsToCheck.isEmpty()) {
            updateOrDeleteEmptyObservedLocations(observationIdsToCheck);
        }

        if (log.isDebugEnabled()) {
            log.debug(String.format("Saving done in %s ms", System.currentTimeMillis() - start));
        }

        return aggregatedLandings;
    }

    /* protected methods */

    private List<LandingVO> getLandings(int observedLocationId) {
        return landingRepository.findAll(
            LandingFilterVO.builder().observedLocationId(observedLocationId).build(),
            defaultFetchOption);
    }

    private void saveLanding(@Nonnull ObservedLocationVO parent, @Nonnull LandingVO landing) {

        // Set default value for recorder department and person
        DataBeans.setDefaultRecorderDepartment(landing, parent.getRecorderDepartment());
        DataBeans.setDefaultRecorderPerson(landing, parent.getRecorderPerson());

        LandingVO savedLanding = landingRepository.save(landing);
        if (landing.getMeasurementValues() != null)
            measurementDao.saveLandingMeasurementsMap(savedLanding.getId(), landing.getMeasurementValues());

    }

    private void updateOrDeleteEmptyObservedLocations(Set<Integer> observationIds) {

        if (CollectionUtils.isEmpty(observationIds)) return;

        observationIds.forEach(observationId -> {
            // clean if no landing at all
            List<LandingVO> landings = getLandings(observationId);

            if (CollectionUtils.isEmpty(landings)) {

                // No landing, delete observation
                observedLocationDao.delete(observationId);
                if (log.isDebugEnabled()) {
                    log.debug(String.format("Delete observation (id=%s) without landing", observationId));
                }

            } else if (landings.stream().allMatch(this::isLandingFullyEmpty)) {

                // All landings are empty, delete observation
                observedLocationDao.delete(observationId);
                if (log.isDebugEnabled()) {
                    log.debug(String.format("Delete observation (id=%s) with empty landings", observationId));
                }

            } else {

                // Just Update
                ObservedLocationVO observedLocation = observedLocationDao.get(observationId);
                observedLocationDao.save(observedLocation);
            }
        });
    }

    private ObservedLocationVO createObservedLocation(ObservedLocationVO parent, Date date, String programLabel) {
        ObservedLocationVO observedLocation = new ObservedLocationVO();
        observedLocation.setStartDateTime(date);
        observedLocation.setEndDateTime(Dates.lastSecondOfTheDay(date));
        observedLocation.setLocation(parent.getLocation());
        observedLocation.setProgram(programDao.getByLabel(programLabel));
        observedLocation.setObservers(Beans.getSet(parent.getObservers()));
        observedLocation.setRecorderDepartment(parent.getRecorderDepartment());
        observedLocation.setRecorderPerson(parent.getRecorderPerson());
        if (log.isDebugEnabled()) {
            log.debug(String.format("Create observed location from %s to %s, program: %s, location: %s",
                observedLocation.getStartDateTime(), observedLocation.getEndDateTime(), observedLocation.getProgram().getLabel(), observedLocation.getLocation().getLabel()));
        }
        return observedLocationDao.save(observedLocation);
    }

    private boolean createOrUpdateLandings(ObservedLocationVO observedLocation, List<LandingVO> landings, List<Integer> landingIdsToRemove, Integer vesselId, VesselActivityVO activity) {
        boolean landingsDirty = false;

        LandingVO landing = landings.stream().filter(landingVO -> Objects.equals(landingVO.getRankOrderOnVessel(), activity.getRankOrder())).findFirst().orElse(null);

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
                        observedLocation.getId(), observedLocation.getStartDateTime(), landing.getVesselSnapshot().getId(), landing.getComments()));
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
                            observedLocation.getId(), observedLocation.getStartDateTime(), landing.getVesselSnapshot().getId(), activity.getMeasurementValues()));
                    }
                }
                if (!Objects.equals(activity.getComments(), landing.getComments())) {
                    landing.setComments(activity.getComments());
                    landingsDirty = true;
                    if (log.isDebugEnabled()) {
                        log.debug(String.format("Update landing on observation (id=%s, date=%s) for vessel %s, comment '%s'",
                            observedLocation.getId(), observedLocation.getStartDateTime(), landing.getVesselSnapshot().getId(), activity.getComments()));
                    }

                }
            }

        } else if (landing != null) {

            if (isLandingEmpty(landing)) {

                landings.remove(landing);
                landingsDirty = true;
                if (log.isDebugEnabled()) {
                    log.debug(String.format("Remove landing on observation (id=%s, date=%s) for vessel %s",
                        observedLocation.getId(), observedLocation.getStartDateTime(), landing.getVesselSnapshot().getId()));
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

    private boolean createOrUpdateTrips(ObservedLocationVO observedLocation, List<LandingVO> landings, Integer vesselId, VesselActivityVO activity) {

        LandingVO landing = landings.stream().filter(landingVO -> Objects.equals(landingVO.getRankOrderOnVessel(), activity.getRankOrder())).findFirst().orElse(null);
        Preconditions.checkNotNull(landing, "The landing should already exists.");
        Integer tripId = landing.getTrip() != null ? landing.getTrip().getId() : landing.getTripId();
        // Check if trip ids corresponds (both null is ok)
        Preconditions.checkArgument(Objects.equals(tripId, activity.getTripId()),
            String.format("Landing tripId (%s) and activity tripId (%s) must corresponds", tripId, activity.getTripId()));

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
                if (trip.getLandingId() == null)
                    trip.setLandingId(landing.getId());
                TripVO savedTrip = saveTrip(trip);
                if (activity.getTripId() == null)
                    activity.setTripId(savedTrip.getId());
                if (log.isDebugEnabled()) {
                    log.debug(String.format("Trip (id=%s) with landing (id=%s) successfully saved", savedTrip.getId(), savedTrip.getLandingId()));
                }
                return true;
            }

        } else if (tripId != null) {

            // Delete the whole trip
            tripRepository.deleteById(tripId);
            if (log.isDebugEnabled()) {
                log.debug(String.format("Trip (id=%s) successfully deleted", tripId));
            }

            return true;
        }

        return false;
    }

    private TripVO saveTrip(TripVO trip) {

        Preconditions.checkNotNull(trip);
        Preconditions.checkNotNull(trip.getLandingId());

        // Trip itself
        TripVO savedTrip = tripRepository.save(trip);

        // Update landing
        LandingVO landing = landingRepository.get(savedTrip.getLandingId());
        landing.setTripId(savedTrip.getId());
        landingRepository.save(landing);

        // Save metiers
        operationGroupDao.saveMetiersByTripId(savedTrip.getId(), trip.getMetiers());

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
        operationGroup.setPhysicalGear(physicalGear);
        return operationGroup;
    }

    private TripVO loadTrip(int tripId) {
        //noinspection UnnecessaryBoxing
        TripVO trip = tripRepository.get(Integer.valueOf(tripId));
        // Load metiers and operation groups
        if (CollectionUtils.isEmpty(trip.getMetiers())) {
            trip.setMetiers(operationGroupDao.getMetiersByTripId(tripId));
        }
        if (CollectionUtils.isEmpty(trip.getOperationGroups())) {
            trip.setOperationGroups(operationGroupDao.getAllByTripId(tripId));
        }

        return trip;
    }

    private TripVO createTrip(ObservedLocationVO observedLocation, Integer vesselId, VesselActivityVO activity) {
        VesselSnapshotVO vessel = new VesselSnapshotVO();
        vessel.setId(vesselId);
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
            && landing.getTrip() == null
            && CollectionUtils.isEmpty(landing.getSamples());
    }

    private boolean isLandingFullyEmpty(LandingVO landing) {
        if (!isLandingEmpty(landing))
            return false;

        // Load its measurements
        loadLandingMeasurements(landing);

        return !hasMeasurements(landing);
    }

    private void loadLandingMeasurements(LandingVO landing) {
        if (landing.getMeasurementValues() == null)
            landing.setMeasurementValues(measurementDao.getLandingMeasurementsMap(landing.getId()));
    }

    private LandingVO createLanding(ObservedLocationVO parent, Integer vesselId, VesselActivityVO activity) {
        VesselSnapshotVO vessel = new VesselSnapshotVO();
        vessel.setId(vesselId);
        LandingVO landing = new LandingVO();
        landing.setVesselSnapshot(vessel);
        landing.setObservedLocation(parent);
        landing.setProgram(parent.getProgram());
        landing.setDateTime(parent.getStartDateTime());
        landing.setLocation(parent.getLocation());
        landing.setRankOrderOnVessel(activity.getRankOrder());
        landing.setComments(activity.getComments());
        landing.setRecorderDepartment(parent.getRecorderDepartment());
        landing.setRecorderPerson(parent.getRecorderPerson());
        return landing;
    }

}

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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import lombok.NonNull;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.dao.data.landing.LandingRepository;
import net.sumaris.core.dao.data.MeasurementDao;
import net.sumaris.core.dao.data.observedLocation.ObservedLocationRepository;
import net.sumaris.core.dao.data.trip.TripRepository;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.dao.technical.model.IValueObject;
import net.sumaris.core.event.config.ConfigurationEvent;
import net.sumaris.core.event.config.ConfigurationReadyEvent;
import net.sumaris.core.event.config.ConfigurationUpdatedEvent;
import net.sumaris.core.event.entity.EntityDeleteEvent;
import net.sumaris.core.event.entity.EntityInsertEvent;
import net.sumaris.core.event.entity.EntityUpdateEvent;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.model.data.Landing;
import net.sumaris.core.model.data.Trip;
import net.sumaris.core.model.data.VesselUseMeasurement;
import net.sumaris.core.model.referential.SaleType;
import net.sumaris.core.model.referential.SaleTypeEnum;
import net.sumaris.core.service.referential.ReferentialService;
import net.sumaris.core.service.referential.pmfm.PmfmService;
import net.sumaris.core.util.Beans;
import net.sumaris.core.util.DataBeans;
import net.sumaris.core.util.Dates;
import net.sumaris.core.vo.data.*;
import net.sumaris.core.vo.filter.TripFilterVO;
import net.sumaris.core.vo.referential.MetierVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service("tripService")
public class TripServiceImpl implements TripService {

    private static final Logger log = LoggerFactory.getLogger(TripServiceImpl.class);

    @Autowired
    private SumarisConfiguration configuration;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private SaleService saleService;

    @Autowired
    private OperationService operationService;

    @Autowired
    private OperationGroupService operationGroupService;

    @Autowired
    private PhysicalGearService physicalGearService;

    @Autowired
    private MeasurementDao measurementDao;

    @Autowired
    private ApplicationEventPublisher publisher;

    @Autowired
    private PmfmService pmfmService;

    @Autowired
    private LandingRepository landingRepository;

    @Autowired
    private ObservedLocationRepository observedLocationRepository;

    @Autowired
    private ReferentialService referentialService;

    @Autowired
    private FishingAreaService fishingAreaService;

    @Override
    public List<TripVO> getAllTrips(int offset, int size) {
        return findByFilter(null, offset, size, null, null, DataFetchOptions.builder().build());
    }

    @Override
    public List<TripVO> findByFilter(TripFilterVO filter, int offset, int size) {
        return findByFilter(filter, offset, size, null, null, DataFetchOptions.builder().build());
    }

    @Override
    public List<TripVO> findByFilter(TripFilterVO filter, int offset, int size, String sortAttribute,
                                     SortDirection sortDirection, DataFetchOptions fieldOptions) {
        return tripRepository.findAll(filter != null ? filter : TripFilterVO.builder().build(), offset, size, sortAttribute, sortDirection, fieldOptions).getContent();
    }

    @Override
    public Long countByFilter(TripFilterVO filter) {
        return tripRepository.count(filter);
    }

    @Override
    public TripVO get(int id) {
        return get(id, DataFetchOptions.builder().build());
    }

    @Override
    public TripVO get(int id, @NonNull DataFetchOptions fetchOptions) {
        TripVO target = tripRepository.get(id);

        // Fetch children (disabled by default)
        if (fetchOptions.isWithChildrenEntities()) {

            target.setGears(physicalGearService.getAllByTripId(id, fetchOptions));
            target.setSales(saleService.getAllByTripId(id, fetchOptions));

            // Fill link to landing, if any
            fillTripLandingLinks(target);

            // Operation groups
            if (target.getLanding() != null) {
                target.setOperationGroups(operationGroupService.getAllByTripId(id, fetchOptions));
                target.setMetiers(operationGroupService.getMetiersByTripId(id));
            }

            // Operations
            else {
                target.setOperations(operationService.getAllByTripId(id, fetchOptions));
            }

        }

        // Measurements
        if (fetchOptions.isWithMeasurementValues()) {
            target.setMeasurements(measurementDao.getTripVesselUseMeasurements(id));
        }

        return target;
    }

    @Override
    public void fillTripLandingLinks(TripVO target) {
        Preconditions.checkNotNull(target);
        Preconditions.checkNotNull(target.getId());
        Landing landing = landingRepository.getByTripId(target.getId());
        if (landing != null) {
            target.setLanding(landingRepository.toVO(landing, DataFetchOptions.builder().withRecorderDepartment(false).withObservers(false).build()));
            if (landing.getObservedLocation() != null) {
                target.setObservedLocationId(landing.getObservedLocation().getId());
            }
        }
    }

    @Override
    public void fillTripsLandingLinks(List<TripVO> targets) {

        List<Integer> tripsIds = targets.stream()
            .map(TripVO::getId)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        List<LandingVO> landings = landingRepository.findAllByTripIds(tripsIds);
        final Multimap<Integer, LandingVO> landingsByTripId = Beans.splitByNotUniqueProperty(landings, LandingVO.Fields.TRIP_ID);
        targets.forEach(target -> {
            Collection<LandingVO> tripLandings = landingsByTripId.get(target.getId());
            LandingVO tripLanding = CollectionUtils.size(tripLandings) == 1 ? tripLandings.iterator().next() : null;
            if (tripLanding != null) {
                target.setLanding(tripLanding);
                if (tripLanding.getObservedLocation() != null) {
                    target.setObservedLocationId(tripLanding.getObservedLocation().getId());
                }
            }
        });
    }

    @Override
    public TripVO save(final TripVO source, TripSaveOptions saveOptions) {
        Preconditions.checkNotNull(source);
        Preconditions.checkNotNull(source.getProgram(), "Missing program");
        Preconditions.checkArgument(source.getProgram().getId() != null || source.getProgram().getLabel() != null, "Missing program.id or program.label");
        Preconditions.checkNotNull(source.getDepartureDateTime(), "Missing departureDateTime");
        Preconditions.checkNotNull(source.getDepartureLocation(), "Missing departureLocation");
        Preconditions.checkNotNull(source.getDepartureLocation().getId(), "Missing departureLocation.id");
        Preconditions.checkNotNull(source.getRecorderDepartment(), "Missing recorderDepartment");
        Preconditions.checkNotNull(source.getRecorderDepartment().getId(), "Missing recorderDepartment.id");
        Preconditions.checkNotNull(source.getVesselSnapshot(), "Missing vesselSnapshot");
        Preconditions.checkNotNull(source.getVesselSnapshot().getId(), "Missing vesselSnapshot.id");
        Preconditions.checkArgument(Objects.isNull(source.getSale()) || CollectionUtils.isEmpty(source.getSales()), "Must not have both 'sales' and 'sale' attributes");

        // Init save options with default values if not provided
        final TripSaveOptions finalSaveOptions = Optional.ofNullable(saveOptions).orElse(TripSaveOptions.builder().build());

        // Reset control date
        source.setControlDate(null);

        boolean isNew = source.getId() == null;

        // Update undefined operations (=metiers) on existing trip, dates can be changed
        if (!isNew) {
            operationGroupService.updateUndefinedOperationDates(source.getId(), source.getDepartureDateTime(), source.getReturnDateTime());
        }

        // Save
        TripVO result = tripRepository.save(source);

        // Save or update parent entity
        saveParent(result, finalSaveOptions);

        // Save measurements
        if (source.getMeasurementValues() != null) {
            measurementDao.saveTripMeasurementsMap(source.getId(), source.getMeasurementValues());
        } else {
            List<MeasurementVO> measurements = Beans.getList(source.getMeasurements());
            measurements.forEach(m -> fillDefaultProperties(result, m));
            measurements = measurementDao.saveTripVesselUseMeasurements(result.getId(), measurements);
            result.setMeasurements(measurements);
        }

        // Save metier
        List<MetierVO> metiers = Beans.getList(source.getMetiers());
        metiers = operationGroupService.saveMetiersByTripId(result.getId(), metiers);
        result.setMetiers(metiers);

        // Save fishing area
        FishingAreaVO savedFishingArea = fishingAreaService.saveByFishingTripId(result.getId(), source.getFishingArea());
        result.setFishingArea(savedFishingArea);

        // Save physical gears
        List<PhysicalGearVO> physicalGears = Beans.getList(source.getGears());
        physicalGears.forEach(physicalGear -> {
            fillDefaultProperties(result, physicalGear);
            if (finalSaveOptions.getWithOperationGroup()) {
                fillPhysicalGearMeasurementsFromOperationGroups(physicalGear, source.getOperationGroups());
            }
        });
        physicalGears = physicalGearService.save(result.getId(), physicalGears);
        result.setGears(physicalGears);

        // Save operations (only if asked)
        if (finalSaveOptions.getWithOperation()) {
            List<OperationVO> operations = Beans.getList(source.getOperations());
            fillOperationPhysicalGears(operations, physicalGears);
            operations = operationService.saveAllByTripId(result.getId(), operations);
            result.setOperations(operations);
        }

        // Save operation groups (only if asked)
        if (finalSaveOptions.getWithOperationGroup()) {
            List<OperationGroupVO> operationGroups = Beans.getList(source.getOperationGroups());
            fillOperationGroupPhysicalGears(operationGroups, physicalGears);
            operationGroups = operationGroupService.saveAllByTripId(result.getId(), operationGroups);
            result.setOperationGroups(operationGroups);
        }

        // Save sales
        if (CollectionUtils.isNotEmpty(source.getSales())) {
            List<SaleVO> sales = Beans.getList(source.getSales());
            sales.forEach(sale -> fillDefaultProperties(result, sale));
            // fill sale products but only if there is only one sale
            if (sales.size() == 1)
                fillSaleProducts(result, sales.get(0));
            sales = saleService.saveAllByTripId(result.getId(), sales);
            result.setSales(sales);
        } else if (source.getSale() != null) {
            SaleVO sale = source.getSale();
            fillDefaultProperties(result, sale);
            fillSaleProducts(result, sale);
            List<SaleVO> sales = saleService.saveAllByTripId(result.getId(), ImmutableList.of(sale));
            result.setSale(sales.get(0));
        } else {
            // Remove all
            saleService.saveAllByTripId(result.getId(), ImmutableList.of());
        }

        // Publish event
        if (isNew) {
            publisher.publishEvent(new EntityInsertEvent(result.getId(), Trip.class.getSimpleName(), result));
        } else {
            publisher.publishEvent(new EntityUpdateEvent(result.getId(), Trip.class.getSimpleName(), result));
        }

        return result;
    }

    private void fillOperationPhysicalGears(List<OperationVO> sources, List<PhysicalGearVO> physicalGears) {
        // Find operations with a physical gear, that have NO id, BUT a rankOrder
        List<OperationVO> sourcesToFill = sources.stream()
                .filter(source -> (source.getPhysicalGearId() == null || source.getPhysicalGearId() < 0)
                        && source.getPhysicalGear() != null
                        && (source.getPhysicalGear().getId() == null || source.getPhysicalGear().getId() < 0)
                        && source.getPhysicalGear().getRankOrder() != null)
                .collect(Collectors.toList());

        // Replace physical gears by exact trip's VO
        if (CollectionUtils.isNotEmpty(sourcesToFill)){
            // Split gears by rankOrder
            Multimap<Integer, PhysicalGearVO> gearsByRankOrder = Beans.splitByNotUniqueProperty(physicalGears, PhysicalGearVO.Fields.RANK_ORDER);

            sourcesToFill.forEach(operation -> {
                Collection<PhysicalGearVO> matches = gearsByRankOrder.get(operation.getPhysicalGear().getRankOrder());
                PhysicalGearVO match = CollectionUtils.isNotEmpty(matches) ? matches.iterator().next() : null;
                if (match == null) {
                    throw new SumarisTechnicalException(String.format("Operation {startDateTime: '%s'} use an unknown PhysicalGear. Physical gears with {rankOrder: %s} not found in trip's gear.",
                            Dates.toISODateTimeString(operation.getStartDateTime()),
                            operation.getPhysicalGear().getRankOrder()));
                }
                operation.setPhysicalGear(match);
            });
        }
    }

    private void fillOperationGroupPhysicalGears(List<OperationGroupVO> sources, List<PhysicalGearVO> physicalGears) {
        Map<Integer, PhysicalGearVO> physicalGearsById = Beans.splitById(physicalGears);
        Multimap<Integer, PhysicalGearVO> physicalGearsByGearId = Beans.splitByNotUniqueProperty(physicalGears, PhysicalGearVO.Fields.GEAR + "." + ReferentialVO.Fields.ID);

        // Affect physical gears from savedTrip.getGears, because new oG can have a physicalGear with null id
        sources.forEach(source -> {
            PhysicalGearVO physicalGear = source.getPhysicalGear();
            if (physicalGear == null) {
                if (source.getPhysicalGearId() != null) {
                    physicalGear = physicalGearsById.get(source.getPhysicalGearId());
                }
            } else if (physicalGear.getId() == null && physicalGear.getGear() != null && physicalGear.getGear().getId() != null) {
                // case of new operation group with unsaved physical gear
                // try to find it with trip's gears
                Collection<PhysicalGearVO> matches = physicalGearsByGearId.get(physicalGear.getGear().getId());
                if (CollectionUtils.isNotEmpty(matches)) {
                    physicalGear = matches.iterator().next();
                }
            }

            // Assert PhysicalGear
            Preconditions.checkNotNull(physicalGear, "OperationGroup has no valid PhysicalGear");
            source.setPhysicalGear(physicalGear);
        });
    }

    private void saveParent(TripVO trip, TripSaveOptions saveOptions) {

        if (saveOptions.getWithLanding()) {
            // Landing
            Preconditions.checkNotNull(trip.getLanding(), "The Landing object must be created first");
            boolean createLanding = false;
            if (trip.getLanding().getId() != null) {

                // update update_date on landing
                LandingVO landing = landingRepository.get(trip.getLanding().getId());

                if (landing.getTripId() == null) {
                    landing.setTripId(trip.getId());
                }
                landing.setDateTime(trip.getReturnDateTime());
                landing.setObservers(Beans.getSet(trip.getObservers()));

                landingRepository.save(landing);

            } else {

                // a landing have to be created
                createLanding = true;

            }

            // ObservedLocation
            if (trip.getObservedLocationId() != null) {

                // update update_date on observed_location
                ObservedLocationVO observedLocation = observedLocationRepository.get(trip.getObservedLocationId());
                observedLocationRepository.save(observedLocation);

                if (createLanding) {

                    LandingVO landing = new LandingVO();

                    landing.setObservedLocationId(observedLocation.getId());
                    landing.setTripId(trip.getId());
                    landing.setProgram(observedLocation.getProgram());
                    landing.setLocation(observedLocation.getLocation());
                    landing.setVesselSnapshot(trip.getVesselSnapshot());
                    landing.setRankOrderOnVessel(trip.getLanding().getRankOrderOnVessel());
                    landing.setDateTime(trip.getReturnDateTime());
                    landing.setObservers(Beans.getSet(trip.getObservers()));
                    landing.setRecorderDepartment(observedLocation.getRecorderDepartment());

                    LandingVO savedLanding = landingRepository.save(landing);
                    trip.setLanding(savedLanding);
                }

            }
        }
    }

    private void fillPhysicalGearMeasurementsFromOperationGroups(PhysicalGearVO physicalGear, List<OperationGroupVO> operationGroups) {
        if (CollectionUtils.isEmpty(operationGroups)) return;
        OperationGroupVO operationGroup;
        if (physicalGear.getId() != null) {
            // Find with id
            operationGroup = operationGroups.stream()
                .filter(og -> og.getPhysicalGear() != null ? physicalGear.getId().equals(og.getPhysicalGear().getId()) : physicalGear.getId().equals(og.getPhysicalGearId()))
                .findFirst().orElseThrow(() -> new SumarisTechnicalException(String.format("OperationGroup with PhysicalGear#%s not found", physicalGear.getId())));

        } else {

            Preconditions.checkNotNull(physicalGear.getGear(), "This PhysicalGear should have a Gear");
            Preconditions.checkNotNull(physicalGear.getGear().getId(), "This PhysicalGear should have a valid Gear");
            Preconditions.checkNotNull(physicalGear.getRankOrder(), "This PhysicalGear should have a rank order");

            // Find with gear and rank order
            operationGroup = operationGroups.stream()
                .filter(og -> og.getPhysicalGear() != null && og.getPhysicalGear().getGear() != null
                    && physicalGear.getGear().getId().equals(og.getPhysicalGear().getGear().getId())
                    && physicalGear.getRankOrder().equals(og.getPhysicalGear().getRankOrder()))
                .findFirst().orElseThrow(() -> new SumarisTechnicalException(
                    String.format("Operation with PhysicalGear.gear#%s and PhysicalGear.rankOrder#%s not found in OperationGroups",
                        physicalGear.getGear().getId(), physicalGear.getRankOrder()))
                );
        }

        // Get gear physical measurement from operation group
        Map<Integer, String> gearPhysicalMeasurements = Maps.newLinkedHashMap();
        operationGroup.getMeasurementValues().forEach((pmfmId, value) -> {
            // TODO: find a better way (not using PMFM label) to determine if a measurement is for physical gear
            // Maybe using matrix=GEAR ?
            if (pmfmService.isGearPhysicalPmfm(pmfmId))
                gearPhysicalMeasurements.putIfAbsent(pmfmId, value);
        });
        // Affect measurement values
        physicalGear.setMeasurementValues(gearPhysicalMeasurements);
    }

    @Override
    public List<TripVO> save(List<TripVO> trips, TripSaveOptions saveOptions) {
        Preconditions.checkNotNull(trips);

        return trips.stream()
            .map(t -> save(t, saveOptions))
            .collect(Collectors.toList());
    }

    @Override
    public void delete(int id) {

        IValueObject eventData = configuration.enableEntityTrash() ?
                get(id, DataFetchOptions.builder().withChildrenEntities(true).build()) :
                null;

        // Remove link LANDING->TRIP
        Landing landing = landingRepository.getByTripId(id);
        if (landing != null) {
            landing.setTrip(null);
            landingRepository.save(landing);
        }

        // Apply deletion
        tripRepository.deleteById(id);

        // Publish delete event
        publisher.publishEvent(new EntityDeleteEvent(id, Trip.class.getSimpleName(), eventData));
    }

    @Override
    public void delete(List<Integer> ids) {
        Preconditions.checkNotNull(ids);
        ids.stream()
            .filter(Objects::nonNull)
            .forEach(this::delete);
    }

    @Override
    public TripVO control(TripVO trip) {
        Preconditions.checkNotNull(trip);
        Preconditions.checkNotNull(trip.getId());
        Preconditions.checkArgument(trip.getControlDate() == null);

        return tripRepository.control(trip);
    }

    @Override
    public TripVO validate(TripVO trip) {
        Preconditions.checkNotNull(trip);
        Preconditions.checkNotNull(trip.getId());
        Preconditions.checkNotNull(trip.getControlDate());
        Preconditions.checkArgument(trip.getValidationDate() == null);

        return tripRepository.validate(trip);
    }

    @Override
    public TripVO unvalidate(TripVO trip) {
        Preconditions.checkNotNull(trip);
        Preconditions.checkNotNull(trip.getId());
        Preconditions.checkNotNull(trip.getControlDate());
        Preconditions.checkNotNull(trip.getValidationDate());

        return tripRepository.unvalidate(trip);
    }

    @Override
    public TripVO qualify(TripVO trip) {
        Preconditions.checkNotNull(trip);
        Preconditions.checkNotNull(trip.getId());
        Preconditions.checkNotNull(trip.getControlDate());
        Preconditions.checkNotNull(trip.getValidationDate());
        Preconditions.checkNotNull(trip.getQualityFlagId());

        return tripRepository.qualify(trip);
    }

    /* protected methods */

    void fillDefaultProperties(TripVO parent, SaleVO sale) {
        if (sale == null) return;

        // Set default values from parent
        DataBeans.setDefaultRecorderDepartment(sale, parent.getRecorderDepartment());
        DataBeans.setDefaultRecorderPerson(sale, parent.getRecorderPerson());
        DataBeans.setDefaultVesselFeatures(sale, parent.getVesselSnapshot());

        if (sale.getStartDateTime() == null) {
            sale.setStartDateTime(parent.getReturnDateTime());
        }
        if (sale.getSaleLocation() == null || sale.getSaleLocation().getId() == null) {
            sale.setSaleLocation(parent.getReturnLocation());
        }
        if (sale.getSaleType() == null || sale.getSaleType().getId() == null) {
            sale.setSaleType(referentialService.findByUniqueLabel(SaleType.class.getSimpleName(), SaleTypeEnum.OTHER.getLabel()));
        }

        sale.setTripId(parent.getId());
    }

    void fillDefaultProperties(TripVO parent, PhysicalGearVO gear) {
        if (gear == null) return;

        // Copy program
        gear.setProgram(parent.getProgram());

        // Copy recorder department from the parent trip
        DataBeans.setDefaultRecorderDepartment(gear, parent.getRecorderDepartment());
        DataBeans.setDefaultRecorderPerson(gear, parent.getRecorderPerson());

        gear.setTripId(parent.getId());
    }

    void fillDefaultProperties(TripVO parent, MeasurementVO measurement) {
        if (measurement == null) return;

        // Set default value for recorder department and person
        DataBeans.setDefaultRecorderDepartment(measurement, parent.getRecorderDepartment());
        DataBeans.setDefaultRecorderPerson(measurement, parent.getRecorderPerson());

        measurement.setEntityName(VesselUseMeasurement.class.getSimpleName());
    }

    void fillSaleProducts(TripVO parent, SaleVO sale) {

        // Fill sale products list with parent trip products, from operation group's product and packets
        if (CollectionUtils.isNotEmpty(parent.getOperationGroups())) {
            List<ProductVO> saleProducts = new ArrayList<>();
            parent.getOperationGroups().forEach(operationGroup -> {
                operationGroup.getProducts().forEach(product -> saleProducts.addAll(product.getSaleProducts()));
                operationGroup.getPackets().forEach(packet -> saleProducts.addAll(
                    // Applying packet id to product.batchId
                    packet.getSaleProducts().stream().peek(saleProduct -> saleProduct.setBatchId(packet.getId())).collect(Collectors.toList())
                ));
            });
            sale.setProducts(saleProducts);
        }

    }
}

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
import net.sumaris.core.dao.data.LandingRepository;
import net.sumaris.core.dao.data.MeasurementDao;
import net.sumaris.core.dao.data.ObservedLocationDao;
import net.sumaris.core.dao.data.TripRepository;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.event.DataEntityCreatedEvent;
import net.sumaris.core.event.DataEntityUpdatedEvent;
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
import net.sumaris.core.vo.data.*;
import net.sumaris.core.vo.filter.TripFilterVO;
import net.sumaris.core.vo.referential.MetierVO;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service("tripService")
public class TripServiceImpl implements TripService {

    private static final Logger log = LoggerFactory.getLogger(TripServiceImpl.class);

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
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private PmfmService pmfmService;

    @Autowired
    private LandingRepository landingRepository;

    @Autowired
    private ObservedLocationDao observedLocationDao;

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
        return tripRepository.findAll(filter, offset, size, sortAttribute, sortDirection, fieldOptions)
            .stream().collect(Collectors.toList());
    }

    @Override
    public Long countByFilter(TripFilterVO filter) {
        return tripRepository.count(filter);
    }

    @Override
    public TripVO get(int tripId) {
        return tripRepository.get(Integer.valueOf(tripId));
    }

    @Override
    public void fillTripLandingLinks(TripVO target) {
        Preconditions.checkNotNull(target);
        Preconditions.checkNotNull(target.getId());
        Landing landing = landingRepository.getByTripId(target.getId());
        if (landing != null) {
            target.setLandingId(landing.getId());
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
                target.setLandingId(tripLanding.getId());
                if (tripLanding.getObservedLocation() != null) {
                    target.setObservedLocationId(tripLanding.getObservedLocation().getId());
                }
            }
        });
    }

    @Override
    public TripVO save(final TripVO source, boolean withOperation, boolean withOperationGroup) {
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

        // Reset control date
        source.setControlDate(null);

        boolean isNew = source.getId() == null;

        // Update undefined operations (=metiers) on existing trip, dates can be changed
        if (!isNew) {
            operationGroupService.updateUndefinedOperationDates(source.getId(), source.getDepartureDateTime(), source.getReturnDateTime());
        }

        // Save
        TripVO savedTrip = tripRepository.save(source);

        // Save or update parent entity
        saveParent(savedTrip);

        // Save measurements
        if (source.getMeasurementValues() != null) {
            measurementDao.saveTripMeasurementsMap(source.getId(), source.getMeasurementValues());
        } else {
            List<MeasurementVO> measurements = Beans.getList(source.getMeasurements());
            measurements.forEach(m -> fillDefaultProperties(savedTrip, m));
            measurements = measurementDao.saveTripVesselUseMeasurements(savedTrip.getId(), measurements);
            savedTrip.setMeasurements(measurements);
        }

        // Save metier
        List<MetierVO> metiers = Beans.getList(source.getMetiers());
        metiers = operationGroupService.saveMetiersByTripId(savedTrip.getId(), metiers);
        savedTrip.setMetiers(metiers);

        // Save fishing area
        FishingAreaVO savedFishingArea = fishingAreaService.saveByFishingTripId(savedTrip.getId(), source.getFishingArea());
        savedTrip.setFishingArea(savedFishingArea);

        // Save physical gears
        List<PhysicalGearVO> physicalGears = Beans.getList(source.getGears());
        physicalGears.forEach(physicalGear -> {
            fillDefaultProperties(savedTrip, physicalGear);
            if (withOperationGroup) {
                fillPhysicalGearMeasurementsFromOperationGroups(physicalGear, source.getOperationGroups());
            }
        });
        physicalGears = physicalGearService.save(savedTrip.getId(), physicalGears);
        savedTrip.setGears(physicalGears);

        // Save operations (only if asked)
        if (withOperation) {
            List<OperationVO> operations = Beans.getList(source.getOperations());
            operations = operationService.saveAllByTripId(savedTrip.getId(), operations);
            savedTrip.setOperations(operations);
        }

        // Save operation groups (only if asked)
        if (withOperationGroup) {
            List<OperationGroupVO> operationGroups = Beans.getList(source.getOperationGroups());

            // Affect physical gears from savedTrip.getGears, because new oG can have a physicalGear with null id
            for (OperationGroupVO operationGroup : operationGroups) {

                if (operationGroup.getPhysicalGear() == null) {
                    if (operationGroup.getPhysicalGearId() != null) {
                        operationGroup.setPhysicalGear(
                            savedTrip.getGears().stream()
                                .filter(physicalGear -> physicalGear.getId().equals(operationGroup.getPhysicalGearId()))
                                .findFirst().orElse(null)
                        );
                    } else {
                        throw new SumarisTechnicalException("OperationGroup has no PhysicalGear");
                    }
                } else if (operationGroup.getPhysicalGear().getId() == null) {
                    // case of new operation group with unsaved physical gear
                    // try to find it with trip's gears
                    operationGroup.setPhysicalGear(
                        savedTrip.getGears().stream()
                            .filter(physicalGear -> physicalGear.getGear().getId().equals(operationGroup.getPhysicalGear().getGear().getId()))
                            .findFirst().orElse(null)
                    );
                }

                // Assert PhysicalGear
                if (operationGroup.getPhysicalGear() == null || operationGroup.getPhysicalGear().getId() == null) {
                    throw new SumarisTechnicalException("OperationGroup has no valid PhysicalGear");
                }
            }

            operationGroups = operationGroupService.saveAllByTripId(savedTrip.getId(), operationGroups);
            savedTrip.setOperationGroups(operationGroups);
        }

        // Save sales
        if (CollectionUtils.isNotEmpty(source.getSales())) {
            List<SaleVO> sales = Beans.getList(source.getSales());
            sales.forEach(sale -> fillDefaultProperties(savedTrip, sale));
            // fill sale products but only if there is only one sale
            if (sales.size() == 1)
                fillSaleProducts(savedTrip, sales.get(0));
            sales = saleService.saveAllByTripId(savedTrip.getId(), sales);
            savedTrip.setSales(sales);
        } else if (source.getSale() != null) {
            SaleVO sale = source.getSale();
            fillDefaultProperties(savedTrip, sale);
            fillSaleProducts(savedTrip, sale);
            List<SaleVO> sales = saleService.saveAllByTripId(savedTrip.getId(), ImmutableList.of(sale));
            savedTrip.setSale(sales.get(0));
        } else {
            // Remove all
            saleService.saveAllByTripId(savedTrip.getId(), ImmutableList.of());
        }

        // Emit event
        if (isNew) {
            eventPublisher.publishEvent(new DataEntityCreatedEvent(Trip.class.getSimpleName(), savedTrip));
        } else {
            eventPublisher.publishEvent(new DataEntityUpdatedEvent(Trip.class.getSimpleName(), savedTrip));
        }

        return savedTrip;
    }

    private void saveParent(TripVO trip) {

        // Landing
        boolean createLanding = false;
        if (trip.getLandingId() != null) {

            // update update_date on landing
            LandingVO landing = landingRepository.get(trip.getLandingId());

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
            ObservedLocationVO observedLocation = observedLocationDao.get(trip.getObservedLocationId());
            observedLocationDao.save(observedLocation);

            if (createLanding) {

                LandingVO landing = new LandingVO();

                landing.setObservedLocationId(observedLocation.getId());
                landing.setTripId(trip.getId());
                landing.setProgram(observedLocation.getProgram());
                landing.setLocation(observedLocation.getLocation());
                landing.setVesselSnapshot(trip.getVesselSnapshot());
                landing.setDateTime(trip.getReturnDateTime());
                landing.setObservers(Beans.getSet(trip.getObservers()));
                landing.setRecorderDepartment(observedLocation.getRecorderDepartment());

                LandingVO savedLanding = landingRepository.save(landing);
                trip.setLandingId(savedLanding.getId());
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
            if (pmfmService.isGearPhysicalPmfm(pmfmId))
                gearPhysicalMeasurements.putIfAbsent(pmfmId, value);
        });
        // Affect measurement values
        physicalGear.setMeasurementValues(gearPhysicalMeasurements);
    }

    @Override
    public List<TripVO> save(List<TripVO> trips, boolean withOperation, boolean withOperationGroup) {
        Preconditions.checkNotNull(trips);

        return trips.stream()
            .map(t -> save(t, withOperation, withOperationGroup))
            .collect(Collectors.toList());
    }

    @Override
    public void delete(int id) {
        tripRepository.deleteById(id);
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

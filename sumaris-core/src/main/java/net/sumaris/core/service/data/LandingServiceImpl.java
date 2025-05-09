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
import com.google.common.collect.Lists;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.data.MeasurementDao;
import net.sumaris.core.dao.data.landing.LandingRepository;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.event.config.ConfigurationEvent;
import net.sumaris.core.event.config.ConfigurationReadyEvent;
import net.sumaris.core.event.config.ConfigurationUpdatedEvent;
import net.sumaris.core.event.entity.EntityDeleteEvent;
import net.sumaris.core.event.entity.EntityInsertEvent;
import net.sumaris.core.event.entity.EntityUpdateEvent;
import net.sumaris.core.model.data.*;
import net.sumaris.core.model.referential.pmfm.MatrixEnum;
import net.sumaris.core.service.data.vessel.VesselSnapshotService;
import net.sumaris.core.service.referential.pmfm.PmfmService;
import net.sumaris.core.util.Beans;
import net.sumaris.core.util.DataBeans;
import net.sumaris.core.util.Dates;
import net.sumaris.core.vo.data.*;
import net.sumaris.core.vo.data.sample.SampleVO;
import net.sumaris.core.vo.filter.LandingFilterVO;
import net.sumaris.core.vo.filter.TripFilterVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service("landingService")
@RequiredArgsConstructor
@Slf4j
public class LandingServiceImpl implements LandingService {

    protected final LandingRepository repository;

    protected final TripService tripService;

    protected final VesselSnapshotService vesselSnapshotService;

    protected final MeasurementDao measurementDao;

    protected final SampleService sampleService;

    protected final SaleService saleService;

    protected final OperationGroupService operationGroupService;

    protected final PmfmService pmfmService;

    private final ApplicationEventPublisher publisher;

    private boolean enableTrash = false;

    @EventListener({ConfigurationReadyEvent.class, ConfigurationUpdatedEvent.class})
    public void onConfigurationReady(ConfigurationEvent event) {
        this.enableTrash = event.getConfiguration().enableEntityTrash();
    }

    @Override
    public List<LandingVO> findAll(@Nullable LandingFilterVO filter, @Nullable Page page, LandingFetchOptions fetchOptions) {
       filter = LandingFilterVO.nullToEmpty(filter);
       List<LandingVO> landings = repository.findAll(filter, page, fetchOptions);

       // Fill vessel snapshots
       if (fetchOptions == null || fetchOptions.isWithVesselSnapshot()) fillVesselSnapshots(landings);

       // Fill sale ids
       if (fetchOptions != null && fetchOptions.isWithSaleIds()) fillSaleIds(landings);

       return landings;
    }

    @Override
    public Long countByFilter(LandingFilterVO filter) {
        return repository.count(filter);
    }

    @Override
    public LandingVO get(Integer id) {
        return get(id, LandingFetchOptions.DEFAULT);
    }

    @Override
    public LandingVO get(Integer id, @NonNull LandingFetchOptions fetchOptions) {
        LandingVO target = repository.get(id, fetchOptions);

        // Fill vessel snapshot
        if (fetchOptions.isWithVesselSnapshot() || fetchOptions.isWithChildrenEntities()) fillVesselSnapshot(target);

        // Fetch children (disabled by default)
        if (fetchOptions.isWithChildrenEntities()) {

            Integer mainUndefinedOperationGroupId = null;
            if (target.getTripId() != null && fetchOptions.isWithTrip()) {
                TripFetchOptions tripFetchOptions = TripFetchOptions.builder()
                    .withChildrenEntities(true) // Need to fetch operation group (fishing areas, metier)
                    .withSales(false) // Never used when fetching from a landing
                    .withExpectedSales(false) // Never used when fetching from a landing - fix IMAGINE-651
                    .build();
                TripVO trip = tripService.get(target.getTripId(), tripFetchOptions);
                target.setTrip(trip);

                // Optimization: avoid fetching sale and expected sale (fix #IMAGINE-651)
                trip.setHasSales(false);
                trip.setHasExpectedSales(false);

                // Get the main undefined operation group
                mainUndefinedOperationGroupId = operationGroupService.getMainUndefinedOperationGroupId(target.getTripId()).orElse(null);
            }

            // Get samples by operation if a main undefined operation group exists
            if (mainUndefinedOperationGroupId != null) {
                target.setSamples(sampleService.getAllByOperationId(mainUndefinedOperationGroupId, fetchOptions.getSampleFetchOptions()));
            } else {
                target.setSamples(sampleService.getAllByLandingId(id, fetchOptions.getSampleFetchOptions()));
            }
        }

        // Measurements
        if (fetchOptions.isWithMeasurementValues()) {
            target.setMeasurements(measurementDao.getLandingMeasurements(id));
        }

        // Sale ids
        if (fetchOptions.isWithSaleIds()) fillSaleIds(target);

        return target;
    }

    public void fillVesselSnapshot(LandingVO target) {
        if (target.getVesselId() != null && target.getVesselSnapshot() == null) {
            target.setVesselSnapshot(vesselSnapshotService.getByIdAndDate(target.getVesselId(), Dates.resetTime(target.getVesselDateTime())));
        }
    }

    public void fillVesselSnapshots(List<LandingVO> target) {
        target.parallelStream().forEach(this::fillVesselSnapshot);
    }


    public void fillSaleIds(LandingVO target) {
        if (target.getSales() != null) {
            target.setSaleIds(Beans.collectIds(target.getSales()));
        }
        else {
            target.setSaleIds(Beans.getList(saleService.getAllIdByLandingId(target.getId())));
        }
    }

    public void fillSaleIds(List<LandingVO> target) {
        target.parallelStream().forEach(this::fillSaleIds);
    }

    @Override
    public LandingVO save(final LandingVO source) {
        checkCanSave(source);

        // Reset control date
        source.setControlDate(null);

        boolean isNew = source.getId() == null;

        // Save
        LandingVO target = repository.save(source);

        // Save children entities (measurement, etc.)
        saveChildrenEntities(target);

        // Publish event
        if (isNew) {
            publisher.publishEvent(new EntityInsertEvent(target.getId(), Landing.class.getSimpleName(), target));
        } else {
            publisher.publishEvent(new EntityUpdateEvent(target.getId(), Landing.class.getSimpleName(), target));
        }

        return target;
    }

    @Override
    public List<LandingVO> save(List<LandingVO> landings) {
        Preconditions.checkNotNull(landings);

        return landings.stream()
            .map(this::save)
            .collect(Collectors.toList());
    }

    @Override
    public List<LandingVO> saveAllByObservedLocationId(int observedLocationId, List<LandingVO> sources) {
        // Check operation validity
        sources.forEach(this::checkCanSave);

        // Save entities
        List<LandingVO> result = repository.saveAllByObservedLocationId(observedLocationId, sources);

        // Save children entities
        result.forEach(this::saveChildrenEntities);

        return result;
    }

    @Override
    public void deleteAllByObservedLocationId(int observedLocationId) {
        repository.findAllIdsByObservedLocationId(observedLocationId)
                .forEach(this::delete);
    }

    @Override
    public void deleteAllByFilter(@NonNull LandingFilterVO filter) {
        List<LandingVO> landingsToDelete = findAll(filter, null, LandingFetchOptions.MINIMAL);
        landingsToDelete.stream().map(LandingVO::getId).forEach(this::delete);
    }

    @Override
    public void delete(int id) {
        log.info("Delete Landing#{} {trash: {}}", id, enableTrash);

        // Create events (before deletion, to be able to join VO)
        LandingVO eventData = enableTrash ? get(id, LandingFetchOptions.FULL_GRAPH) : null;
        Integer tripId = eventData != null ? eventData.getTripId() : null;

        // Delete linked trip
        // WARN: use delete by trip id (if possible) to fix Oracle on an Adagio schema - see #IMAGINE-602 and #IMAGINE-589
        if (tripId != null) {
            tripService.delete(tripId);
        }
        else {
            tripService.deleteAllByLandingId(id);
        }

        // Delete linked samples
        sampleService.deleteAllByLandingId(id);

        measurementDao.deleteMeasurements(LandingMeasurement.class, Landing.class, ImmutableList.of(id));
        measurementDao.deleteMeasurements(SurveyMeasurement.class, Landing.class, ImmutableList.of(id));

        // Delete landing
        repository.deleteByIds(ImmutableList.of(id));

        // Publish events
        publisher.publishEvent(new EntityDeleteEvent(id, Landing.class.getSimpleName(), eventData));
    }

    @Override
    public void delete(List<Integer> ids) {
        Preconditions.checkNotNull(ids);
        ids.stream()
            .filter(Objects::nonNull)
            .forEach(this::delete);
    }

    @Override
    public LandingVO control(@NonNull LandingVO landing, DataControlOptions options) {
        Preconditions.checkNotNull(landing.getId());
        Preconditions.checkArgument(landing.getValidationDate() == null);

        landing = repository.control(landing);

        // Also control Trip
        if (landing.getTripId() != null && options != null && options.getWithChildren()) {
            tripService.findAll(TripFilterVO.builder()
                                    .tripId(landing.getTripId())
                                    .dataQualityStatus(new DataQualityStatusEnum[]{DataQualityStatusEnum.MODIFIED, DataQualityStatusEnum.CONTROLLED})
                                    .build(),
                            Page.builder().offset(0).size(1000).build(),
                            TripFetchOptions.MINIMAL)
                    .forEach(tripService::control);
        }

        return landing;
    }

    @Override
    public LandingVO validate(@NonNull LandingVO landing, DataValidateOptions options) {
        Preconditions.checkNotNull(landing.getId());
        Preconditions.checkNotNull(landing.getControlDate());
        Preconditions.checkArgument(landing.getValidationDate() == null);

        options = DataValidateOptions.defaultIfEmpty(options);

        landing = repository.validate(landing);

        // Also validate trip
        if (landing.getTripId() != null && options.getWithChildren()) {
            tripService.findAll(TripFilterVO.builder()
                                    .tripId(landing.getTripId())
                                    .dataQualityStatus(new DataQualityStatusEnum[]{DataQualityStatusEnum.CONTROLLED})
                                    .build(),
                            Page.builder().offset(0).size(1000).build(),
                            TripFetchOptions.MINIMAL)
                    .forEach(tripService::validate);
        }

        return landing;
    }

    @Override
    public LandingVO unvalidate(@NonNull LandingVO landing, DataValidateOptions options) {
        Preconditions.checkNotNull(landing.getId());
        Preconditions.checkNotNull(landing.getControlDate());
        Preconditions.checkNotNull(landing.getValidationDate());

        landing = repository.unValidate(landing);

        options = DataValidateOptions.defaultIfEmpty(options);

        // Also unvalidate trip
        if (landing.getTripId() != null && options.getWithChildren()) {
            tripService.findAll(TripFilterVO.builder()
                                    .tripId(landing.getTripId())
                                    .dataQualityStatus(new DataQualityStatusEnum[]{DataQualityStatusEnum.VALIDATED, DataQualityStatusEnum.QUALIFIED})
                                    .build(),
                            Page.builder().offset(0).size(1000).build(),
                            TripFetchOptions.MINIMAL)
                    .forEach(tripService::unvalidate);
        }


        return landing;
    }

    @Override
    public LandingVO qualify(LandingVO landing) {
        Preconditions.checkNotNull(landing);
        Preconditions.checkNotNull(landing.getId());
        Preconditions.checkNotNull(landing.getControlDate());
        Preconditions.checkNotNull(landing.getValidationDate());

        landing = repository.qualify(landing);

        // Publish event
        publisher.publishEvent(new EntityUpdateEvent(landing.getId(), Landing.class.getSimpleName(), landing));

        return landing;
    }

    /* -- protected methods -- */

    protected void saveChildrenEntities(final LandingVO source) {

        // Save measurements
        if (source.getMeasurementValues() != null) {
            // Split survey and landing measurements
            Map<Integer, String> surveyMeasurementMap = Beans.filterMap(source.getMeasurementValues(), pmfmId -> pmfmService.isSurveyPmfm(pmfmId));
            Map<Integer, String> landingMeasurementMap = Beans.filterMap(source.getMeasurementValues(), pmfmId -> !surveyMeasurementMap.containsKey(pmfmId));
            measurementDao.saveLandingMeasurementsMap(source.getId(), landingMeasurementMap);
            measurementDao.saveLandingSurveyMeasurementsMap(source.getId(), surveyMeasurementMap);
        } else {
            // Split survey and landing measurements

            List<MeasurementVO> surveyMeasurements = Beans.filterCollection(source.getMeasurements(), measurementVO -> pmfmService.isSurveyPmfm(measurementVO.getPmfmId()));
            List<Integer> surveyPmfmIds = surveyMeasurements.stream().map(MeasurementVO::getPmfmId).collect(Collectors.toList());
            List<MeasurementVO> landingMeasurements = Beans.filterCollection(source.getMeasurements(), measurementVO -> !surveyPmfmIds.contains(measurementVO.getPmfmId()));

            landingMeasurements.forEach(m -> fillDefaultProperties(source, m, LandingMeasurement.class));
            landingMeasurements = measurementDao.saveLandingMeasurements(source.getId(), landingMeasurements);
            surveyMeasurements.forEach(m -> fillDefaultProperties(source, m, SurveyMeasurement.class));
            surveyMeasurements = measurementDao.saveLandingSurveyMeasurements(source.getId(), surveyMeasurements);

            source.setMeasurements(ListUtils.union(landingMeasurements, surveyMeasurements));
        }

        // Save trip
        Integer mainUndefinedOperationGroupId = null;
        TripVO trip = source.getTrip();
        if (trip != null) {
            // Prepare landing to save
            trip.setLandingId(source.getId());
            trip.setLanding(null);

            fillDefaultProperties(source, trip);

            // Save the landed trip
            boolean hasSales = trip.getSale() != null || trip.getSales() != null;
            boolean hasExpectedSales = trip.getExpectedSale() != null || trip.getExpectedSales() != null;
            TripSaveOptions tripSaveOptions = TripSaveOptions.LANDED_TRIP.toBuilder()
                .withSales(hasSales)
                .withExpectedSales(hasExpectedSales)
                .build();
            TripVO savedTrip = tripService.save(trip, tripSaveOptions);

            // Optimization: avoid fetching expected sale (fix #IMAGINE-651)
            savedTrip.setHasSales(hasSales);
            savedTrip.setHasExpectedSales(hasExpectedSales);

            // Update the source landing
            source.setTripId(savedTrip.getId());
            source.setTrip(savedTrip);

            // Get the main undefined operation group
            mainUndefinedOperationGroupId = operationGroupService.getMainUndefinedOperationGroupId(savedTrip.getId()).orElse(null);
        }

        // Save samples
        {
            List<SampleVO> samples = getSamplesAsList(source);
            samples.forEach(s -> fillDefaultProperties(source, s));

            // Save samples by operation if a main undefined operation group exists
            if (mainUndefinedOperationGroupId != null) {
                samples = sampleService.saveByOperationId(mainUndefinedOperationGroupId, samples);
            } else {
                samples = sampleService.saveByLandingId(source.getId(), samples);
            }

            // Prepare saved samples (e.g. to be used as graphQL query response)
            samples.forEach(sample -> {
                // Set parentId (instead of parent object)
                if (sample.getParentId() == null && sample.getParent() != null) {
                    sample.setParentId(sample.getParent().getId());
                }
                // Remove link parent/children
                sample.setParent(null);
                sample.setChildren(null);

                // landingId can have been deleted by saveByOperationId()
                // TODO: review this with Ludo
                // FIX IMAGINE issue, on samples table (when saving entities, sample.equals() always return false, because of landingId=null in received SampleVO)
                sample.setLandingId(source.getId());
            });

            source.setSamples(samples);
        }

        // Save sales
        if (source.getSales() != null) {
            source.getSales().forEach((SaleVO sale) -> fillDefaultProperties(source, sale));
            source.setSales(saleService.saveAllByLandingId(source.getId(), source.getSales()));
        }
        else {
            source.setHasSales(false);
        }
    }

    protected void checkCanSave(final LandingVO source) {
        Preconditions.checkNotNull(source);
        Preconditions.checkNotNull(source.getProgram(), "Missing program");
        Preconditions.checkArgument(source.getProgram().getId() != null || source.getProgram().getLabel() != null, "Missing program.id or program.label");
        Preconditions.checkNotNull(source.getDateTime(), "Missing dateTime");
        Preconditions.checkNotNull(source.getLocation(), "Missing location");
        Preconditions.checkNotNull(source.getLocation().getId(), "Missing location.id");
        Preconditions.checkNotNull(source.getRecorderDepartment(), "Missing recorderDepartment");
        Preconditions.checkNotNull(source.getRecorderDepartment().getId(), "Missing recorderDepartment.id");
    }

    protected void fillDefaultProperties(LandingVO parent, MeasurementVO measurement, Class<? extends IMeasurementEntity> measurementClass) {
        if (measurement == null) return;

        // Set default value for recorder department and person
        DataBeans.setDefaultRecorderDepartment(measurement, parent.getRecorderDepartment());
        DataBeans.setDefaultRecorderPerson(measurement, parent.getRecorderPerson());

        measurement.setEntityName(measurementClass.getSimpleName());
    }

    protected void fillDefaultProperties(LandingVO parent, SampleVO sample) {
        if (sample == null) return;

        // Copy recorder department from the parent
        DataBeans.setDefaultRecorderDepartment(sample, parent.getRecorderDepartment());

        // Fill matrix
        if (sample.getMatrix() == null || sample.getMatrix().getId() == null) {
            ReferentialVO matrix = new ReferentialVO();
            matrix.setId(MatrixEnum.INDIVIDUAL.getId());
            sample.setMatrix(matrix);
        }

        // Fill sample (use operation end date time)
        if (sample.getSampleDate() == null) {
            sample.setSampleDate(parent.getDateTime());
        }

        sample.setLandingId(parent.getId());
    }

    protected void fillDefaultProperties(LandingVO parent, TripVO trip) {
        if (trip == null) return;

        DataBeans.setDefaultRecorderDepartment(trip, parent.getRecorderDepartment());
        DataBeans.setDefaultRecorderPerson(trip, parent.getRecorderPerson());

        if (trip.getProgram() == null) {
            trip.setProgram(parent.getProgram());
        }
        if (trip.getVesselSnapshot() == null) {
            trip.setVesselSnapshot(parent.getVesselSnapshot());
        }
        if (trip.getVesselId() == null) {
            trip.setVesselId(parent.getVesselId());
        }
    }

    protected void fillDefaultProperties(LandingVO parent, SaleVO sale) {
        if (sale == null) return;

        // Copy recorder department from the parent
        DataBeans.setDefaultRecorderDepartment(sale, parent.getRecorderDepartment());
        DataBeans.setDefaultRecorderPerson(sale, parent.getRecorderPerson());

        if (sale.getProgram() == null) {
            sale.setProgram(parent.getProgram());
        }
        if (sale.getVesselSnapshot() == null) {
            sale.setVesselSnapshot(parent.getVesselSnapshot());
        }
        if (sale.getVesselId() == null) {
            sale.setVesselId(parent.getVesselId());
        }
        if (sale.getSaleLocation() == null) {
            sale.setSaleLocation(parent.getLocation());
        }

        sale.setLandingId(parent.getId());
    }

    /**
     * Get all samples, in the sample tree parent/children
     *
     * @param parent
     * @return
     */
    protected List<SampleVO> getSamplesAsList(final LandingVO parent) {
        final List<SampleVO> result = Lists.newArrayList();
        if (CollectionUtils.isNotEmpty(parent.getSamples())) {
            parent.getSamples().forEach(sample -> {
                fillDefaultProperties(parent, sample);
                sampleService.treeToList(sample, result);
            });
        }
        return result;
    }
}

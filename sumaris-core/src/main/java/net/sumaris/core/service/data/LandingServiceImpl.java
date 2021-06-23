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
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.data.MeasurementDao;
import net.sumaris.core.dao.data.landing.LandingRepository;
import net.sumaris.core.dao.data.trip.TripRepository;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.event.config.ConfigurationEvent;
import net.sumaris.core.event.config.ConfigurationReadyEvent;
import net.sumaris.core.event.config.ConfigurationUpdatedEvent;
import net.sumaris.core.event.entity.EntityDeleteEvent;
import net.sumaris.core.event.entity.EntityInsertEvent;
import net.sumaris.core.event.entity.EntityUpdateEvent;
import net.sumaris.core.model.data.*;
import net.sumaris.core.model.referential.pmfm.MatrixEnum;
import net.sumaris.core.util.Beans;
import net.sumaris.core.util.DataBeans;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.data.*;
import net.sumaris.core.vo.data.sample.SampleVO;
import net.sumaris.core.vo.filter.LandingFilterVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service("landingService")
@Slf4j
public class LandingServiceImpl implements LandingService {

    @Autowired
    protected LandingRepository landingRepository;

    @Autowired
    protected TripService tripService;

    @Autowired
    protected TripRepository tripRepository;

    @Autowired
    protected MeasurementDao measurementDao;

    @Autowired
    protected SampleService sampleService;

    @Autowired
    private ApplicationEventPublisher publisher;

    private boolean enableTrash = false;

    @EventListener({ConfigurationReadyEvent.class, ConfigurationUpdatedEvent.class})
    public void onConfigurationReady(ConfigurationEvent event) {
        this.enableTrash = event.getConfiguration().enableEntityTrash();
    }

    @Override
    public List<LandingVO> findAll(LandingFilterVO filter, Page page, DataFetchOptions fetchOptions) {

        filter = LandingFilterVO.nullToEmpty(filter);

        if (page != null) {

            // FIXME LP: Sorting by 'vessel' must sort by registration code
            /*if (Landing.Fields.VESSEL.equals(page.getSortBy())) {
                page.setSortBy(
                    StringUtils.doting(Landing.Fields.VESSEL, Vessel.Fields.VESSEL_REGISTRATION_PERIODS, VesselRegistrationPeriod.Fields.REGISTRATION_CODE)
                );
            }*/

            return landingRepository.findAll(filter, page, fetchOptions);

        } else {

            return landingRepository.findAll(filter, fetchOptions);
        }

    }

    @Override
    public Long countByFilter(LandingFilterVO filter) {
        return landingRepository.count(filter);
    }

    @Override
    public LandingVO get(Integer landingId) {
        return landingRepository.get(landingId);
    }

    @Override
    public LandingVO save(final LandingVO source) {
        checkCanSave(source);

        // Reset control date
        source.setControlDate(null);

        boolean isNew = source.getId() == null;

        // Save
        LandingVO savedLanding = landingRepository.save(source);

        // Save children entities (measurement, etc.)
        saveChildrenEntities(savedLanding);

        // Publish event
        if (isNew) {
            publisher.publishEvent(new EntityInsertEvent(savedLanding.getId(), Landing.class.getSimpleName(), savedLanding));
        } else {
            publisher.publishEvent(new EntityUpdateEvent(savedLanding.getId(), Landing.class.getSimpleName(), savedLanding));
        }

        return savedLanding;
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
        List<LandingVO> result = landingRepository.saveAllByObservedLocationId(observedLocationId, sources);

        // Save children entities
        result.forEach(this::saveChildrenEntities);

        return result;
    }

    @Override
    public void delete(int id) {

        // Create events (before deletion, to be able to join VO)
        LandingVO deletedVO = null;
        Integer tripId = null;
        if (enableTrash) {
            deletedVO = get(id);
            tripId = deletedVO.getTripId();
            if (tripId != null) {
                deletedVO.setTrip(tripRepository.get(tripId)); // TODO full VO loading
            }
        }

        if (tripId != null) {
            tripRepository.deleteById(tripId);
        }

        // Apply deletion
        landingRepository.deleteById(id);

        // Publish events
        publisher.publishEvent(new EntityDeleteEvent(id, Trip.class.getSimpleName(), deletedVO));
    }

    @Override
    public void delete(List<Integer> ids) {
        Preconditions.checkNotNull(ids);
        ids.stream()
            .filter(Objects::nonNull)
            .forEach(this::delete);
    }

    @Override
    public LandingVO control(LandingVO landing) {
        Preconditions.checkNotNull(landing);
        Preconditions.checkNotNull(landing.getId());
        Preconditions.checkArgument(landing.getControlDate() == null);

        return landingRepository.control(landing);
    }

    @Override
    public LandingVO validate(LandingVO landing) {
        Preconditions.checkNotNull(landing);
        Preconditions.checkNotNull(landing.getId());
        Preconditions.checkNotNull(landing.getControlDate());
        Preconditions.checkArgument(landing.getValidationDate() == null);

        return landingRepository.validate(landing);
    }

    @Override
    public LandingVO unvalidate(LandingVO landing) {
        Preconditions.checkNotNull(landing);
        Preconditions.checkNotNull(landing.getId());
        Preconditions.checkNotNull(landing.getControlDate());
        Preconditions.checkNotNull(landing.getValidationDate());

        return landingRepository.unValidate(landing);
    }

    /* -- protected methods -- */

    protected void saveChildrenEntities(final LandingVO source) {

        // Save measurements
        if (source.getMeasurementValues() != null) {
            measurementDao.saveLandingMeasurementsMap(source.getId(), source.getMeasurementValues());
        } else {
            List<MeasurementVO> measurements = Beans.getList(source.getMeasurements());
            measurements.forEach(m -> fillDefaultProperties(source, m));
            measurements = measurementDao.saveLandingMeasurements(source.getId(), measurements);
            source.setMeasurements(measurements);
        }

        // Save samples
        {
            List<SampleVO> samples = getSamplesAsList(source);
            samples.forEach(s -> fillDefaultProperties(source, s));
            samples = sampleService.saveByLandingId(source.getId(), samples);

            // Prepare saved samples (e.g. to be used as graphQL query response)
            samples.forEach(sample -> {
                // Set parentId (instead of parent object)
                if (sample.getParentId() == null && sample.getParent() != null) {
                    sample.setParentId(sample.getParent().getId());
                }
                // Remove link parent/children
                sample.setParent(null);
                sample.setChildren(null);
            });

            source.setSamples(samples);
        }

        // Save trip
        TripVO trip = source.getTrip();
        if (trip != null) {
            // Prepare landing to save
            trip.setLandingId(source.getId());
            trip.setLanding(null);

            TripVO savedTrip = tripService.save(source.getTrip(), TripSaveOptions.builder()
                    .withLanding(false)
                    .withOperation(false)
                    .withOperationGroup(true)
                    .build());

            source.setTrip(savedTrip);
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

    protected void fillDefaultProperties(LandingVO parent, MeasurementVO measurement) {
        if (measurement == null) return;

        // Set default value for recorder department and person
        DataBeans.setDefaultRecorderDepartment(measurement, parent.getRecorderDepartment());
        DataBeans.setDefaultRecorderPerson(measurement, parent.getRecorderPerson());

        measurement.setEntityName(LandingMeasurement.class.getSimpleName());
    }

    protected void fillDefaultProperties(LandingVO parent, SampleVO sample) {
        if (sample == null) return;

        // Copy recorder department from the parent
        if (sample.getRecorderDepartment() == null || sample.getRecorderDepartment().getId() == null) {
            sample.setRecorderDepartment(parent.getRecorderDepartment());
        }

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

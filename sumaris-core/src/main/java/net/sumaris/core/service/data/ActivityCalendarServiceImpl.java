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
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.dao.data.MeasurementDao;
import net.sumaris.core.dao.data.activity.ActivityCalendarRepository;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.event.config.ConfigurationReadyEvent;
import net.sumaris.core.event.config.ConfigurationUpdatedEvent;
import net.sumaris.core.event.entity.EntityDeleteEvent;
import net.sumaris.core.event.entity.EntityInsertEvent;
import net.sumaris.core.event.entity.EntityUpdateEvent;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.model.data.ActivityCalendar;
import net.sumaris.core.model.data.SurveyMeasurement;
import net.sumaris.core.service.data.vessel.VesselSnapshotService;
import net.sumaris.core.service.referential.ReferentialService;
import net.sumaris.core.service.referential.pmfm.PmfmService;
import net.sumaris.core.util.DataBeans;
import net.sumaris.core.util.Dates;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.data.*;
import net.sumaris.core.vo.data.activity.ActivityCalendarFetchOptions;
import net.sumaris.core.vo.data.activity.ActivityCalendarVO;
import net.sumaris.core.vo.data.aggregatedLanding.VesselActivityVO;
import net.sumaris.core.vo.filter.ActivityCalendarFilterVO;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor
@Service("activityCalendarService")
@Slf4j
public class ActivityCalendarServiceImpl implements ActivityCalendarService {

    private final SumarisConfiguration configuration;
    private final ActivityCalendarRepository repository;
    private final MeasurementDao measurementDao;
    private final ApplicationEventPublisher publisher;
    private final PmfmService pmfmService;
    private final ReferentialService referentialService;
    private final FishingAreaService fishingAreaService;
    private final VesselSnapshotService vesselSnapshotService;
    private boolean enableTrash = false;

    @PostConstruct
    @EventListener({ConfigurationReadyEvent.class, ConfigurationUpdatedEvent.class})
    public void onConfigurationReady() {
        this.enableTrash = configuration.enableEntityTrash();
    }

    @Override
    public List<ActivityCalendarVO> findAll(ActivityCalendarFilterVO filter, int offset, int size, String sortAttribute,
                                            SortDirection sortDirection, ActivityCalendarFetchOptions fetchOptions) {
        return repository.findAll(ActivityCalendarFilterVO.nullToEmpty(filter), offset, size, sortAttribute, sortDirection, fetchOptions);
    }

    @Override
    public List<ActivityCalendarVO> findAll(@Nullable ActivityCalendarFilterVO filter, @Nullable Page page, ActivityCalendarFetchOptions fetchOptions) {
        return repository.findAll(ActivityCalendarFilterVO.nullToEmpty(filter), page, fetchOptions);
    }

    @Override
    public long countByFilter(ActivityCalendarFilterVO filter) {
        return repository.count(filter);
    }

    @Override
    public ActivityCalendarVO get(int id) {
        return get(id, ActivityCalendarFetchOptions.DEFAULT);
    }

    @Override
    public ActivityCalendarVO get(int id, @NonNull ActivityCalendarFetchOptions fetchOptions) {
        ActivityCalendarVO target = repository.get(id);

        // Vessel snapshot
        if (fetchOptions.isWithVesselSnapshot()) {
            fillVesselSnapshot(target);
        }

        // Measurements
        if (fetchOptions.isWithMeasurementValues()) {
            target.setMeasurementValues(measurementDao.getActivityCalendarMeasurementsMap(id));
        }

        return target;
    }

    @Override
    public int getProgramIdById(int id) {
        return repository.getProgramIdById(id);
    }

    public void fillVesselSnapshot(ActivityCalendarVO target) {
        Integer year = target.getYear();
        if (year != null && target.getVesselId() != null && target.getVesselSnapshot() == null) {

            try {
                String dateStr = String.format("%s-01-01", StringUtils.leftPad(year.toString(), 4, "0"));
                Date vesselDate = Dates.resetTime(Dates.parseDate(dateStr, "yyyy-MM-dd"));
                target.setVesselSnapshot(vesselSnapshotService.getByIdAndDate(target.getVesselId(), vesselDate));
            } catch (ParseException e) {
                throw new SumarisTechnicalException(e.getMessage(), e);
            }
        }
    }

    public void fillVesselSnapshots(List<ActivityCalendarVO> target) {
        target.parallelStream().forEach(this::fillVesselSnapshot);
    }

    @Override
    public ActivityCalendarVO save(final ActivityCalendarVO source) {
        checkCanSave(source);

        // Reset control date
        source.setControlDate(null);

        boolean isNew = source.getId() == null;

        // Save
        ActivityCalendarVO target = repository.save(source);

        // Avoid sequence configuration mistake (see AllocationSize)
        Preconditions.checkArgument(target.getId() != null && target.getId() >= 0, "Invalid ActivityCalendar.id. Make sure your sequence has been well configured");

        if (source.getMeasurementValues() != null) {
            measurementDao.saveActivityCalendarMeasurementsMap(source.getId(), source.getMeasurementValues());
        }


        // Publish event
        if (isNew) {
            publisher.publishEvent(new EntityInsertEvent(target.getId(), ActivityCalendar.class.getSimpleName(), target));
        } else {
            publisher.publishEvent(new EntityUpdateEvent(target.getId(), ActivityCalendar.class.getSimpleName(), target));
        }

        return target;
    }

    @Override
    public List<ActivityCalendarVO> save(@NonNull List<ActivityCalendarVO> sources) {
        return sources.stream()
            .map(this::save)
            .toList();
    }

    @Override
    public void delete(int id) {
        log.info("Delete ActivityCalendar#{} {trash: {}}", id, enableTrash);

        // Construct the event data
        // (should be done before deletion, to be able to get the VO)
        ActivityCalendarVO eventData = enableTrash ?
            get(id, ActivityCalendarFetchOptions.FULL_GRAPH) :
            null;

        // Apply deletion
        repository.deleteById(id);

        // Publish delete event
        publisher.publishEvent(new EntityDeleteEvent(id, ActivityCalendar.class.getSimpleName(), eventData));
    }

    @Override
    public void delete(List<Integer> ids) {
        Preconditions.checkNotNull(ids);
        ids.stream()
            .filter(Objects::nonNull)
            .forEach(this::delete);
    }

    @Override
    public CompletableFuture<Boolean> asyncDelete(int id) {
        try {
            // Call self, to be sure to have a transaction
            this.delete(id);
            return CompletableFuture.completedFuture(Boolean.TRUE);
        } catch (Exception e) {
            log.warn(String.format("Error while deleting activityCalendar {id: %s}: %s", id, e.getMessage()), e);
            return CompletableFuture.completedFuture(Boolean.FALSE);
        }
    }

    @Override
    public CompletableFuture<Boolean> asyncDelete(List<Integer> ids) {
        try {
            // Call self, to be sure to have a transaction
            this.delete(ids);
            return CompletableFuture.completedFuture(Boolean.TRUE);
        } catch (Exception e) {
            log.warn(String.format("Error while deleting activityCalendar {ids: %s}: %s", ids, e.getMessage()), e);
            return CompletableFuture.completedFuture(Boolean.FALSE);
        }
    }

    @Override
    public ActivityCalendarVO control(ActivityCalendarVO activityCalendar) {
        Preconditions.checkNotNull(activityCalendar);
        Preconditions.checkNotNull(activityCalendar.getId());
        Preconditions.checkArgument(activityCalendar.getControlDate() == null);

        ActivityCalendarVO result = repository.control(activityCalendar);

        // Publish event
        publisher.publishEvent(new EntityUpdateEvent(result.getId(), ActivityCalendar.class.getSimpleName(), result));

        return result;
    }

    @Override
    public ActivityCalendarVO validate(ActivityCalendarVO activityCalendar) {
        Preconditions.checkNotNull(activityCalendar);
        Preconditions.checkNotNull(activityCalendar.getId());
        Preconditions.checkNotNull(activityCalendar.getControlDate());
        Preconditions.checkArgument(activityCalendar.getValidationDate() == null);

        ActivityCalendarVO result = repository.validate(activityCalendar);

        // Publish event
        publisher.publishEvent(new EntityUpdateEvent(result.getId(), ActivityCalendar.class.getSimpleName(), result));

        return result;
    }

    @Override
    public ActivityCalendarVO unvalidate(ActivityCalendarVO activityCalendar) {
        Preconditions.checkNotNull(activityCalendar);
        Preconditions.checkNotNull(activityCalendar.getId());
        Preconditions.checkNotNull(activityCalendar.getControlDate());
        Preconditions.checkNotNull(activityCalendar.getValidationDate());

        ActivityCalendarVO result = repository.unValidate(activityCalendar);

        // Publish event
        publisher.publishEvent(new EntityUpdateEvent(result.getId(), ActivityCalendar.class.getSimpleName(), result));

        return result;
    }

    @Override
    public ActivityCalendarVO qualify(ActivityCalendarVO activityCalendar) {
        Preconditions.checkNotNull(activityCalendar);
        Preconditions.checkNotNull(activityCalendar.getId());
        Preconditions.checkNotNull(activityCalendar.getControlDate());
        Preconditions.checkNotNull(activityCalendar.getValidationDate());
        Preconditions.checkNotNull(activityCalendar.getQualityFlagId());

        ActivityCalendarVO result = repository.qualify(activityCalendar);

        // Publish event
        publisher.publishEvent(new EntityUpdateEvent(result.getId(), ActivityCalendar.class.getSimpleName(), result));

        return result;
    }


    /* -- protected methods -- */

    protected void checkCanSave(ActivityCalendarVO source) {
        Preconditions.checkNotNull(source);
        Preconditions.checkArgument(source.getId() == null || source.getId() >= 0, "Cannot save a activityCalendar with a local id: " + source.getId());
        Preconditions.checkNotNull(source.getProgram(), "Missing program");
        Preconditions.checkArgument(source.getProgram().getId() != null || source.getProgram().getLabel() != null, "Missing program.id or program.label");
        Preconditions.checkNotNull(source.getYear(), "Missing year");
        Preconditions.checkNotNull(source.getDirectSurveyInvestigation(), "Missing directSurveyInvestigation");
        Preconditions.checkNotNull(source.getRecorderDepartment(), "Missing recorderDepartment");
        Preconditions.checkNotNull(source.getRecorderDepartment().getId(), "Missing recorderDepartment.id");
        Preconditions.checkNotNull(source.getVesselSnapshot(), "Missing vesselSnapshot");
        Preconditions.checkNotNull(source.getVesselSnapshot().getVesselId(), "Missing vesselSnapshot.id");
    }

    protected void fillDefaultProperties(ActivityCalendarVO parent, VesselActivityVO vesselActivity) {
        if (vesselActivity == null) return;

        // Set default values from parent
        // TODO BLA remove this ?
        //DataBeans.setDefaultRecorderDepartment(vesselActivity, parent.getRecorderDepartment());
        //DataBeans.setDefaultRecorderPerson(vesselActivity, parent.getRecorderPerson());
        //DataBeans.setDefaultVesselSnapshot(vesselActivity, parent.getVesselSnapshot());

       vesselActivity.setActivityCalendarId(parent.getId());
    }


    protected void fillDefaultProperties(ActivityCalendarVO parent, MeasurementVO measurement) {
        if (measurement == null) return;

        // Set default value for recorder department and person
        DataBeans.setDefaultRecorderDepartment(measurement, parent.getRecorderDepartment());
        DataBeans.setDefaultRecorderPerson(measurement, parent.getRecorderPerson());

        measurement.setEntityName(SurveyMeasurement.class.getSimpleName());
    }

}

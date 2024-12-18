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

package net.sumaris.core.service.data.activity;

import com.google.common.base.Preconditions;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.dao.data.ImageAttachmentRepository;
import net.sumaris.core.dao.data.MeasurementDao;
import net.sumaris.core.dao.data.activity.ActivityCalendarRepository;
import net.sumaris.core.dao.data.vessel.GearPhysicalFeaturesRepository;
import net.sumaris.core.dao.data.vessel.GearUseFeaturesRepository;
import net.sumaris.core.dao.data.vessel.VesselUseFeaturesRepository;
import net.sumaris.core.dao.technical.Daos;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.event.config.ConfigurationReadyEvent;
import net.sumaris.core.event.config.ConfigurationUpdatedEvent;
import net.sumaris.core.event.entity.EntityDeleteEvent;
import net.sumaris.core.event.entity.EntityInsertEvent;
import net.sumaris.core.event.entity.EntityUpdateEvent;
import net.sumaris.core.model.data.ActivityCalendar;
import net.sumaris.core.model.referential.ObjectTypeEnum;
import net.sumaris.core.service.data.vessel.VesselSnapshotService;
import net.sumaris.core.util.Beans;
import net.sumaris.core.util.DataBeans;
import net.sumaris.core.util.Dates;
import net.sumaris.core.vo.data.*;
import net.sumaris.core.vo.data.activity.ActivityCalendarFetchOptions;
import net.sumaris.core.vo.data.activity.ActivityCalendarVO;
import net.sumaris.core.vo.filter.ActivityCalendarFilterVO;
import net.sumaris.core.vo.filter.GearPhysicalFeaturesFilterVO;
import net.sumaris.core.vo.filter.GearUseFeaturesFilterVO;
import net.sumaris.core.vo.filter.VesselUseFeaturesFilterVO;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor
@Service("activityCalendarService")
@Slf4j
public class ActivityCalendarServiceImpl implements ActivityCalendarService {

    private final SumarisConfiguration configuration;
    private final ActivityCalendarRepository repository;
    private final MeasurementDao measurementDao;
    private final ApplicationEventPublisher publisher;
    private final VesselSnapshotService vesselSnapshotService;
    private final VesselUseFeaturesRepository vesselUseFeaturesRepository;
    private final GearUseFeaturesRepository gearUseFeaturesRepository;
    private final GearPhysicalFeaturesRepository gearPhysicalFeaturesRepository;
    private boolean enableImageAttachments;

    @Autowired
    private ImageAttachmentRepository imageAttachmentRepository;


    private boolean enableTrash = false;
    @PostConstruct
    @EventListener({ConfigurationReadyEvent.class, ConfigurationUpdatedEvent.class})
    public void onConfigurationReady() {
        this.enableTrash = configuration.enableEntityTrash();
        this.enableImageAttachments = configuration.enableDataImages();
    }

    @Override
    public List<ActivityCalendarVO> findAll(ActivityCalendarFilterVO filter, int offset, int size, String sortAttribute,
                                            SortDirection sortDirection, ActivityCalendarFetchOptions fetchOptions) {
        List<ActivityCalendarVO> result = repository.findAll(ActivityCalendarFilterVO.nullToEmpty(filter), offset, size, sortAttribute, sortDirection, fetchOptions);

        fillVOs(result, fetchOptions);

        return result;
    }

    @Override
    public List<ActivityCalendarVO> findAll(@Nullable ActivityCalendarFilterVO filter, @Nullable Page page, ActivityCalendarFetchOptions fetchOptions) {
        List<ActivityCalendarVO> result = repository.findAll(ActivityCalendarFilterVO.nullToEmpty(filter), page, fetchOptions);

        fillVOs(result, fetchOptions);

        return result;
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
        ActivityCalendarVO target = repository.get(id, fetchOptions);

        // Vessel snapshot
        if (fetchOptions.isWithVesselSnapshot()) {
            fillVesselSnapshot(target);
        }

        // Measurements
        if (fetchOptions.isWithMeasurementValues()) {
            target.setMeasurementValues(measurementDao.getActivityCalendarMeasurementsMap(id));
        }

        // Load features
        if (fetchOptions.isWithChildrenEntities()) {
            fillChildrenEntities(target, fetchOptions);
        }

        return target;
    }

    @Override
    public int getProgramIdById(int id) {
        return repository.getProgramIdById(id);
    }

    public void fillVOs(List<ActivityCalendarVO> targets, ActivityCalendarFetchOptions fetchOptions) {

        if (fetchOptions.isWithVesselSnapshot()) {
            fillVesselSnapshots(targets);
        }

        // Measurements
        if (fetchOptions.isWithMeasurementValues()) {
            targets.forEach(target -> target.setMeasurementValues(measurementDao.getActivityCalendarMeasurementsMap(target.getId())));
        }

        // Load features
        if (fetchOptions.isWithChildrenEntities()) {
            fillChildrenEntities(targets, fetchOptions);
        }
    }


    public void fillVesselSnapshot(ActivityCalendarVO target) {
        Integer year = target.getYear();
        if (year != null && target.getVesselId() != null && target.getVesselSnapshot() == null) {
            target.setVesselSnapshot(vesselSnapshotService.getByIdAndDate(target.getVesselId(), Dates.resetTime(Dates.getFirstDayOfYear(year))));
        }
    }

    public void fillVesselSnapshots(List<ActivityCalendarVO> targets) {
        targets.parallelStream().forEach(this::fillVesselSnapshot);
    }


    public void fillChildrenEntities(ActivityCalendarVO target, ActivityCalendarFetchOptions fetchOptions) {
        DataFetchOptions childrenFetchOptions = DataFetchOptions.copy(fetchOptions);

        // Vessel use features
        {
            List<VesselUseFeaturesVO> vesselUseFeatures = vesselUseFeaturesRepository.findAll(VesselUseFeaturesFilterVO.builder()
                    .activityCalendarId(target.getId())
                    .build(), childrenFetchOptions);
            target.setVesselUseFeatures(vesselUseFeatures);
        }

        // Gear use features
        {
            List<GearUseFeaturesVO> gearUseFeatures = gearUseFeaturesRepository.findAll(GearUseFeaturesFilterVO.builder()
                    .activityCalendarId(target.getId())
                    .build(), childrenFetchOptions);
            target.setGearUseFeatures(gearUseFeatures);
        }

        // Gear physical features
        {
            List<GearPhysicalFeaturesVO> gearPhysicalFeatures = gearPhysicalFeaturesRepository.findAll(GearPhysicalFeaturesFilterVO.builder()
                    .activityCalendarId(target.getId())
                    .build(), childrenFetchOptions);
            target.setGearPhysicalFeatures(gearPhysicalFeatures);
        }
    }

    public void fillChildrenEntities(List<ActivityCalendarVO> targets, ActivityCalendarFetchOptions fetchOptions) {
        targets.parallelStream().forEach(target -> this.fillChildrenEntities(target, fetchOptions));
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

        // Save children entities (measurement, etc.)
        saveChildrenEntities(target);

        // Publish event
        if (isNew) {
            publisher.publishEvent(new EntityInsertEvent(target.getId(), ActivityCalendar.class.getSimpleName(), target));
        } else {
            publisher.publishEvent(new EntityUpdateEvent(target.getId(), ActivityCalendar.class.getSimpleName(), target));
        }

        // Save images
        if (enableImageAttachments) saveImageAttachments(target);

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

    @Override
    public void copyPreviousYearCommentsByIds(@NonNull List<Integer> ids) {
        log.debug("Copying previous year comments of {} activity calendar(s)...", ids.size());

        int count = Daos.streamByChunk(ids, 500) // Chunk of 500 items, because of IN operator
                .mapToInt(repository::copyPreviousYearCommentsByIds)
                .sum();
        log.debug("Copying previous year comments of {} activity calendar(s) [OK] - {} updates", ids.size(), count);
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

        Integer vesselId = source.getVesselId() != null ? source.getVesselId() : (source.getVesselSnapshot() != null ? source.getVesselSnapshot().getVesselId() : null);
        Preconditions.checkNotNull(vesselId, "Missing vesselId or vesselSnapshot.id");
    }


    protected void saveChildrenEntities(final ActivityCalendarVO source) {

        // Save measurements
        if (source.getMeasurementValues() != null) {
            measurementDao.saveActivityCalendarMeasurementsMap(source.getId(), source.getMeasurementValues());
        }

        // Save vessel use features
        {
            List<VesselUseFeaturesVO> vesselUseFeatures = Beans.getList(source.getVesselUseFeatures());
            vesselUseFeatures.forEach(vuf -> fillDefaultProperties(source, vuf));
            vesselUseFeatures = vesselUseFeaturesRepository.saveAllByActivityCalendarId(source.getId(), vesselUseFeatures);
            source.setVesselUseFeatures(vesselUseFeatures);
        }

        // Save gear use features
        {
            List<GearUseFeaturesVO> gearUseFeatures = Beans.getList(source.getGearUseFeatures());
            gearUseFeatures.forEach(guf -> fillDefaultProperties(source, guf));
            gearUseFeatures = gearUseFeaturesRepository.saveAllByActivityCalendarId(source.getId(), gearUseFeatures);
            source.setGearUseFeatures(gearUseFeatures);
        }

        // Save gear physical features
        {
            List<GearPhysicalFeaturesVO> gearPhysicalFeatures = Beans.getList(source.getGearPhysicalFeatures());
            gearPhysicalFeatures.forEach(guf -> fillDefaultProperties(source, guf));
            gearPhysicalFeatures = gearPhysicalFeaturesRepository.saveAllByActivityCalendarId(source.getId(), gearPhysicalFeatures);
            source.setGearPhysicalFeatures(gearPhysicalFeatures);
        }

    }

    protected void fillDefaultProperties(ActivityCalendarVO parent, IUseFeaturesVO source) {
        if (source == null) return;

        // Same program
        source.setProgram(parent.getProgram());

        // Same vessel
        if (parent.getVesselId() != null) {
            source.setVesselId(parent.getVesselId());
        }
        else if (parent.getVesselSnapshot() != null) {
            source.setVesselId(parent.getVesselSnapshot().getId());
        }

        // Set default recorder department/person
        if (source.getRecorderDepartmentId() == null && parent.getRecorderDepartment() != null) {
            source.setRecorderDepartmentId(parent.getRecorderDepartment().getId());
        }
        if (source.getRecorderPersonId() == null && parent.getRecorderPerson() != null) {
            source.setRecorderPersonId(parent.getRecorderPerson().getId());
        }

        // Link to parent
        if (source instanceof VesselUseFeaturesVO vuf) {
            vuf.setActivityCalendarId(parent.getId());
        }
        else if (source instanceof GearUseFeaturesVO guf) {
            guf.setActivityCalendarId(parent.getId());
        }
        else if (source instanceof GearPhysicalFeaturesVO gpf) {
            gpf.setActivityCalendarId(parent.getId());
        }
    }

    private void saveImageAttachments(ActivityCalendarVO activityCalendar) {
        List<Integer> existingIdsToRemove = imageAttachmentRepository.getIdsFromObject(activityCalendar.getId(), ObjectTypeEnum.ACTIVITY_CALENDAR.getId());
        Beans.getStream(activityCalendar.getImages())
                .filter(Objects::nonNull)
                .forEach(image -> {
                    boolean exists = existingIdsToRemove.remove(image.getId());

                    // Update only, when images already exists, and no content changes
                    if (exists && image.getContent() == null) {
                        // Update comments only
                        log.debug("Update Image#{} comments", image.getId());
                        imageAttachmentRepository.updateComments(image.getId(), image.getComments());
                    }
                    else {
                        // Fill defaults
                        fillDefaultProperties(image, activityCalendar);

                        // Save
                        imageAttachmentRepository.save(image);
                    }
                });

        // Remove
        if (CollectionUtils.isNotEmpty(existingIdsToRemove)) {
            imageAttachmentRepository.deleteAllByIdInBatch(existingIdsToRemove);
        }
    }

    protected void fillDefaultProperties(ImageAttachmentVO image, ActivityCalendarVO parent) {
        if (image == null) return;

        // Set default recorder department
        DataBeans.setDefaultRecorderDepartment(image, parent.getRecorderDepartment());

        // Fill date
        if (image.getDateTime() == null) {
            image.setDateTime(parent.getCreationDate()); //TODO BLA CHECK THIS
        }

        // Link to activityCalendar
        image.setObjectId(parent.getId());
        image.setObjectTypeId(ObjectTypeEnum.ACTIVITY_CALENDAR.getId());
    }
}

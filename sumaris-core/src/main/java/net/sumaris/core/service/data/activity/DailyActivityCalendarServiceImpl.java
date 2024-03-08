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
import net.sumaris.core.dao.data.MeasurementDao;
import net.sumaris.core.dao.data.activity.DailyActivityCalendarRepository;
import net.sumaris.core.dao.data.activity.DailyActivityCalendarRepository;
import net.sumaris.core.dao.data.vessel.GearUseFeaturesRepository;
import net.sumaris.core.dao.data.vessel.VesselUseFeaturesRepository;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.event.config.ConfigurationReadyEvent;
import net.sumaris.core.event.config.ConfigurationUpdatedEvent;
import net.sumaris.core.event.entity.EntityDeleteEvent;
import net.sumaris.core.event.entity.EntityInsertEvent;
import net.sumaris.core.event.entity.EntityUpdateEvent;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.model.data.DailyActivityCalendar;
import net.sumaris.core.service.data.vessel.VesselSnapshotService;
import net.sumaris.core.util.Beans;
import net.sumaris.core.util.Dates;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.data.DataFetchOptions;
import net.sumaris.core.vo.data.GearUseFeaturesVO;
import net.sumaris.core.vo.data.IUseFeaturesVO;
import net.sumaris.core.vo.data.VesselUseFeaturesVO;
import net.sumaris.core.vo.data.activity.DailyActivityCalendarFetchOptions;
import net.sumaris.core.vo.data.activity.DailyActivityCalendarVO;
import net.sumaris.core.vo.filter.DailyActivityCalendarFilterVO;
import net.sumaris.core.vo.filter.GearUseFeaturesFilterVO;
import net.sumaris.core.vo.filter.VesselUseFeaturesFilterVO;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor
@Service("dailyActivityCalendarService")
@Slf4j
public class DailyActivityCalendarServiceImpl implements DailyActivityCalendarService {

    private final SumarisConfiguration configuration;
    private final DailyActivityCalendarRepository repository;
    private final MeasurementDao measurementDao;
    private final ApplicationEventPublisher publisher;
    private final VesselSnapshotService vesselSnapshotService;
    private final VesselUseFeaturesRepository vesselUseFeaturesRepository;
    private final GearUseFeaturesRepository gearUseFeaturesRepository;

    private boolean enableTrash = false;

    @PostConstruct
    @EventListener({ConfigurationReadyEvent.class, ConfigurationUpdatedEvent.class})
    public void onConfigurationReady() {
        this.enableTrash = configuration.enableEntityTrash();
    }

    @Override
    public List<DailyActivityCalendarVO> findAll(DailyActivityCalendarFilterVO filter, int offset, int size, String sortAttribute,
                                            SortDirection sortDirection, DailyActivityCalendarFetchOptions fetchOptions) {
        return repository.findAll(DailyActivityCalendarFilterVO.nullToEmpty(filter), offset, size, sortAttribute, sortDirection, fetchOptions);
    }

    @Override
    public List<DailyActivityCalendarVO> findAll(@Nullable DailyActivityCalendarFilterVO filter, @Nullable Page page, DailyActivityCalendarFetchOptions fetchOptions) {
        return repository.findAll(DailyActivityCalendarFilterVO.nullToEmpty(filter), page, fetchOptions);
    }

    @Override
    public long countByFilter(DailyActivityCalendarFilterVO filter) {
        return repository.count(filter);
    }

    @Override
    public DailyActivityCalendarVO get(int id) {
        return get(id, DailyActivityCalendarFetchOptions.DEFAULT);
    }

    @Override
    public DailyActivityCalendarVO get(int id, @NonNull DailyActivityCalendarFetchOptions fetchOptions) {
        DailyActivityCalendarVO target = repository.get(id, fetchOptions);

        // Vessel snapshot
        if (fetchOptions.isWithVesselSnapshot()) {
            fillVesselSnapshot(target);
        }

        // Measurements
        if (fetchOptions.isWithMeasurementValues()) {
            target.setMeasurementValues(measurementDao.getDailyActivityCalendarMeasurementsMap(id));
        }

        // Load features
        if (fetchOptions.isWithChildrenEntities()) {
            DataFetchOptions childrenFetchOptions = DataFetchOptions.copy(fetchOptions);

            // Vessel use features
            {
                List<VesselUseFeaturesVO> vesselUseFeatures = vesselUseFeaturesRepository.findAll(VesselUseFeaturesFilterVO.builder()
                    .dailyActivityCalendarId(id)
                    .build(), childrenFetchOptions);
                target.setVesselUseFeatures(vesselUseFeatures);
            }

            // Gear use features
            {
                List<GearUseFeaturesVO> gearUseFeatures = gearUseFeaturesRepository.findAll(GearUseFeaturesFilterVO.builder()
                    .dailyActivityCalendarId(id)
                    .build(), childrenFetchOptions);
                target.setGearUseFeatures(gearUseFeatures);
            }
        }

        return target;
    }

    @Override
    public int getProgramIdById(int id) {
        return repository.getProgramIdById(id);
    }

    public void fillVesselSnapshot(DailyActivityCalendarVO target) {
        if (target.getVesselId() != null && target.getVesselSnapshot() == null) {
            target.setVesselSnapshot(vesselSnapshotService.getByIdAndDate(target.getVesselId(), Dates.resetTime(target.getVesselDateTime())));
        }
    }

    public void fillVesselSnapshots(List<DailyActivityCalendarVO> target) {
        target.parallelStream().forEach(this::fillVesselSnapshot);
    }

    @Override
    public DailyActivityCalendarVO save(final DailyActivityCalendarVO source) {
        checkCanSave(source);

        // Reset control date
        source.setControlDate(null);

        boolean isNew = source.getId() == null;

        // Save
        DailyActivityCalendarVO target = repository.save(source);

        // Avoid sequence configuration mistake (see AllocationSize)
        Preconditions.checkArgument(target.getId() != null && target.getId() >= 0, "Invalid DailyActivityCalendar.id. Make sure your sequence has been well configured");

        // Save children entities (measurement, etc.)
        saveChildrenEntities(target);

        // Publish event
        if (isNew) {
            publisher.publishEvent(new EntityInsertEvent(target.getId(), DailyActivityCalendar.class.getSimpleName(), target));
        } else {
            publisher.publishEvent(new EntityUpdateEvent(target.getId(), DailyActivityCalendar.class.getSimpleName(), target));
        }

        return target;
    }

    @Override
    public List<DailyActivityCalendarVO> save(@NonNull List<DailyActivityCalendarVO> sources) {
        return sources.stream()
            .map(this::save)
            .toList();
    }

    @Override
    public void delete(int id) {
        log.info("Delete DailyActivityCalendar#{} {trash: {}}", id, enableTrash);

        // Construct the event data
        // (should be done before deletion, to be able to get the VO)
        DailyActivityCalendarVO eventData = enableTrash ?
            get(id, DailyActivityCalendarFetchOptions.FULL_GRAPH) :
            null;

        // Apply deletion
        repository.deleteById(id);

        // Publish delete event
        publisher.publishEvent(new EntityDeleteEvent(id, DailyActivityCalendar.class.getSimpleName(), eventData));
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
            log.warn(String.format("Error while deleting dailyActivityCalendar {id: %s}: %s", id, e.getMessage()), e);
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
            log.warn(String.format("Error while deleting dailyActivityCalendar {ids: %s}: %s", ids, e.getMessage()), e);
            return CompletableFuture.completedFuture(Boolean.FALSE);
        }
    }

    @Override
    public DailyActivityCalendarVO control(DailyActivityCalendarVO dailyActivityCalendar) {
        Preconditions.checkNotNull(dailyActivityCalendar);
        Preconditions.checkNotNull(dailyActivityCalendar.getId());
        Preconditions.checkArgument(dailyActivityCalendar.getControlDate() == null);

        DailyActivityCalendarVO result = repository.control(dailyActivityCalendar);

        // Publish event
        publisher.publishEvent(new EntityUpdateEvent(result.getId(), DailyActivityCalendar.class.getSimpleName(), result));

        return result;
    }

    @Override
    public DailyActivityCalendarVO validate(DailyActivityCalendarVO dailyActivityCalendar) {
        Preconditions.checkNotNull(dailyActivityCalendar);
        Preconditions.checkNotNull(dailyActivityCalendar.getId());
        Preconditions.checkNotNull(dailyActivityCalendar.getControlDate());
        Preconditions.checkArgument(dailyActivityCalendar.getValidationDate() == null);

        DailyActivityCalendarVO result = repository.validate(dailyActivityCalendar);

        // Publish event
        publisher.publishEvent(new EntityUpdateEvent(result.getId(), DailyActivityCalendar.class.getSimpleName(), result));

        return result;
    }

    @Override
    public DailyActivityCalendarVO unvalidate(DailyActivityCalendarVO dailyActivityCalendar) {
        Preconditions.checkNotNull(dailyActivityCalendar);
        Preconditions.checkNotNull(dailyActivityCalendar.getId());
        Preconditions.checkNotNull(dailyActivityCalendar.getControlDate());
        Preconditions.checkNotNull(dailyActivityCalendar.getValidationDate());

        DailyActivityCalendarVO result = repository.unValidate(dailyActivityCalendar);

        // Publish event
        publisher.publishEvent(new EntityUpdateEvent(result.getId(), DailyActivityCalendar.class.getSimpleName(), result));

        return result;
    }

    @Override
    public DailyActivityCalendarVO qualify(DailyActivityCalendarVO dailyActivityCalendar) {
        Preconditions.checkNotNull(dailyActivityCalendar);
        Preconditions.checkNotNull(dailyActivityCalendar.getId());
        Preconditions.checkNotNull(dailyActivityCalendar.getControlDate());
        Preconditions.checkNotNull(dailyActivityCalendar.getValidationDate());
        Preconditions.checkNotNull(dailyActivityCalendar.getQualityFlagId());

        DailyActivityCalendarVO result = repository.qualify(dailyActivityCalendar);

        // Publish event
        publisher.publishEvent(new EntityUpdateEvent(result.getId(), DailyActivityCalendar.class.getSimpleName(), result));

        return result;
    }


    /* -- protected methods -- */

    protected void checkCanSave(DailyActivityCalendarVO source) {
        Preconditions.checkNotNull(source);
        Preconditions.checkArgument(source.getId() == null || source.getId() >= 0, "Cannot save a dailyActivityCalendar with a local id: " + source.getId());
        Preconditions.checkNotNull(source.getProgram(), "Missing program");
        Preconditions.checkArgument(source.getProgram().getId() != null || source.getProgram().getLabel() != null, "Missing program.id or program.label");
        Preconditions.checkNotNull(source.getStartDate(), "Missing startDate");
        Preconditions.checkNotNull(source.getEndDate(), "Missing endDate");
        Preconditions.checkNotNull(source.getRecorderDepartment(), "Missing recorderDepartment");
        Preconditions.checkNotNull(source.getRecorderDepartment().getId(), "Missing recorderDepartment.id");
        Preconditions.checkNotNull(source.getVesselSnapshot(), "Missing vesselSnapshot");
        Preconditions.checkNotNull(source.getVesselSnapshot().getVesselId(), "Missing vesselSnapshot.id");
    }


    protected void saveChildrenEntities(final DailyActivityCalendarVO source) {

        // Save measurements
        if (source.getMeasurementValues() != null) {
            measurementDao.saveDailyActivityCalendarMeasurementsMap(source.getId(), source.getMeasurementValues());
        }

        // Save vessel use features
        {
            List<VesselUseFeaturesVO> vesselUseFeatures = Beans.getList(source.getVesselUseFeatures());
            vesselUseFeatures.forEach(vuf -> fillDefaultProperties(source, vuf));
            vesselUseFeatures = vesselUseFeaturesRepository.saveAllByDailyActivityCalendarId(source.getId(), vesselUseFeatures);
            source.setVesselUseFeatures(vesselUseFeatures);
        }

        // Save gear use features
        {
            List<GearUseFeaturesVO> gearUseFeatures = Beans.getList(source.getGearUseFeatures());
            gearUseFeatures.forEach(guf -> fillDefaultProperties(source, guf));
            gearUseFeatures = gearUseFeaturesRepository.saveAllByDailyActivityCalendarId(source.getId(), gearUseFeatures);
            source.setGearUseFeatures(gearUseFeatures);
        }

    }

    protected void fillDefaultProperties(DailyActivityCalendarVO parent, IUseFeaturesVO source) {
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

        if (source instanceof VesselUseFeaturesVO vuf) {
            vuf.setDailyActivityCalendarId(parent.getId());
        }
        else if (source instanceof GearUseFeaturesVO guf) {
            guf.setDailyActivityCalendarId(parent.getId());
        }
    }

}

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
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.data.MeasurementDao;
import net.sumaris.core.dao.data.VesselPositionDao;
import net.sumaris.core.dao.data.operation.OperationRepository;
import net.sumaris.core.dao.technical.Pageables;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.event.config.ConfigurationEvent;
import net.sumaris.core.event.config.ConfigurationReadyEvent;
import net.sumaris.core.event.config.ConfigurationUpdatedEvent;
import net.sumaris.core.event.entity.EntityDeleteEvent;
import net.sumaris.core.model.data.GearUseMeasurement;
import net.sumaris.core.model.data.IMeasurementEntity;
import net.sumaris.core.model.data.Operation;
import net.sumaris.core.model.data.VesselUseMeasurement;
import net.sumaris.core.model.referential.pmfm.MatrixEnum;
import net.sumaris.core.util.Beans;
import net.sumaris.core.vo.data.*;
import net.sumaris.core.vo.data.batch.BatchVO;
import net.sumaris.core.vo.data.sample.SampleVO;
import net.sumaris.core.vo.filter.OperationFilterVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service("operationService")
@Slf4j
public class OperationServiceImpl implements OperationService {

    @Autowired
    protected OperationRepository operationRepository;

    @Autowired
    protected VesselPositionDao vesselPositionDao;

    @Autowired
    protected MeasurementDao measurementDao;

    @Autowired
    protected SampleService sampleService;

    @Autowired
    protected BatchService batchService;

    @Autowired
    protected FishingAreaService fishingAreaService;

    @Autowired
    private ApplicationEventPublisher publisher;

    private boolean enableTrash = false;

    @EventListener({ConfigurationReadyEvent.class, ConfigurationUpdatedEvent.class})
    protected void onConfigurationReady(ConfigurationEvent event) {
        this.enableTrash = event.getConfiguration().enableEntityTrash();
    }


    @Override
    public List<OperationVO> findAllByTripId(int tripId, @NonNull OperationFetchOptions fetchOptions) {
        return operationRepository.findAllVO(operationRepository.hasTripId(tripId), fetchOptions);
    }

    @Override
    public List<OperationVO> findAllByTripId(int tripId,
                                             int offset, int size, String sortAttribute, SortDirection sortDirection,
                                             @NonNull OperationFetchOptions fetchOptions) {
        return operationRepository.findAllVO(operationRepository.hasTripId(tripId),
                Pageables.create(offset, size, sortAttribute, sortDirection),
                fetchOptions).getContent();
    }

    @Override
    public List<OperationVO> findAllByFilter(OperationFilterVO filter, int offset, int size, String sortAttribute, SortDirection sortDirection,
                                             @NonNull OperationFetchOptions fetchOptions) {
        return operationRepository.findAll(OperationFilterVO.nullToEmpty(filter), offset, size, sortAttribute, sortDirection, fetchOptions);
    }

    @Override
    public Long countByTripId(int tripId) {
        return operationRepository.count(OperationFilterVO.builder().tripId(tripId).build());
    }

    @Override
    public Long countByFilter(OperationFilterVO filter) {
        return operationRepository.count(filter);
    }

    @Override
    public List<OperationVO> saveAllByTripId(int tripId, List<OperationVO> sources) {
        // Check operation validity
        sources.forEach(this::checkCanSave);

        // Save entities
        List<OperationVO> result = operationRepository.saveAllByTripId(tripId, sources);

        // Save children entities
        result.forEach(this::saveChildrenEntities);

        return result;
    }

    @Override
    public OperationVO get(int operationId) {
        return operationRepository.get(operationId);
    }

    @Override
    public OperationVO get(int operationId, OperationFetchOptions o) {
        return operationRepository.get(operationId, o);
    }


    @Override
    public int getProgramIdById(int id) {
        return operationRepository.getProgramIdById(id);
    }

    @Override
    public OperationVO save(final OperationVO source) {
        // Check operation validity
        checkCanSave(source);

        // Save the entity
        operationRepository.save(source);

        // Save linked entities
        saveChildrenEntities(source);

        return source;
    }

    @Override
    public OperationVO control(OperationVO source) {
        return operationRepository.control(source);
    }

    @Override
    public List<OperationVO> save(List<OperationVO> operations) {
        Preconditions.checkNotNull(operations);

        return operations.stream()
                .map(this::save)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(int id) {
        // Construct the event data
        // (should be done before deletion, to be able to get the VO)
        OperationVO eventData = enableTrash ? get(id) : null;

        // Apply deletion
        operationRepository.deleteById(id);

        // Emit the event

        EntityDeleteEvent event = new EntityDeleteEvent(id, Operation.class.getSimpleName(), eventData);
        publisher.publishEvent(event);
    }

    @Override
    public void delete(List<Integer> ids) {
        Preconditions.checkNotNull(ids);
        ids.stream()
                .filter(Objects::nonNull)
                .forEach(this::delete);
    }

    /* -- protected methods -- */

    protected void checkCanSave(final OperationVO source) {
        Preconditions.checkNotNull(source);
        Preconditions.checkNotNull(source.getStartDateTime(), "Missing startDateTime");
        Preconditions.checkNotNull(source.getEndDateTime(), "Missing endDateTime");
        Preconditions.checkNotNull(source.getRecorderDepartment(), "Missing recorderDepartment");
        Preconditions.checkNotNull(source.getRecorderDepartment().getId(), "Missing recorderDepartment.id");


        // Reset control date
        source.setControlDate(null);
    }

    protected void saveChildrenEntities(final OperationVO source) {

        // Save positions
        {
            List<VesselPositionVO> positions = Beans.getList(source.getPositions());
            positions.forEach(m -> fillDefaultProperties(source, m));
            positions = vesselPositionDao.saveByOperationId(source.getId(), positions);
            source.setPositions(positions);
        }

        // Save measurements (vessel use measurement)
        {
            if (source.getMeasurementValues() != null) {
                measurementDao.saveOperationVesselUseMeasurementsMap(source.getId(), source.getMeasurementValues());
            } else {
                List<MeasurementVO> measurements = Beans.getList(source.getMeasurements());
                measurements.forEach(m -> fillDefaultProperties(source, m, VesselUseMeasurement.class));

                // TODO: dispatch measurement by GEAR/NOT GEAR
                measurements = measurementDao.saveOperationVesselUseMeasurements(source.getId(), measurements);
                source.setMeasurements(measurements);
            }
        }

        // Save gear measurements (gear use measurement)
        {
            if (source.getGearMeasurementValues() != null) {
                measurementDao.saveOperationGearUseMeasurementsMap(source.getId(), source.getGearMeasurementValues());
            } else {
                List<MeasurementVO> measurements = Beans.getList(source.getGearMeasurements());
                measurements.forEach(m -> fillDefaultProperties(source, m, GearUseMeasurement.class));
                measurements = measurementDao.saveOperationGearUseMeasurements(source.getId(), measurements);
                source.setGearMeasurements(measurements);
            }
        }

        // Save samples
        {
            List<SampleVO> samples = getSamplesAsList(source);
            samples.forEach(s -> fillDefaultProperties(source, s));
            samples = sampleService.saveByOperationId(source.getId(), samples);

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

        // Save batches
        {

            List<BatchVO> batches = getAllBatches(source);
            batches.forEach(b -> fillDefaultProperties(source, b));
            batches = batchService.saveAllByOperationId(source.getId(), batches);

            // Transform saved batches into flat list (e.g. to be used as graphQL query response)
            batches.forEach(batch -> {
                // Set parentId (instead of parent object)
                if (batch.getParentId() == null && batch.getParent() != null) {
                    batch.setParentId(batch.getParent().getId());
                }
                // Remove link parent/children
                batch.setParent(null);
                batch.setChildren(null);
            });

            source.setCatchBatch(null);
            source.setBatches(batches);
        }

        // Save Fishing areas
        {
            if (source.getFishingAreas() != null) {

                source.getFishingAreas().forEach(fishingArea -> fillDefaultProperties(source, fishingArea));
                fishingAreaService.saveAllByOperationId(source.getId(), source.getFishingAreas());
            }
        }

    }

    protected void fillDefaultProperties(OperationVO parent, VesselPositionVO position) {
        if (position == null) return;

        // Copy recorder department from the parent
        if (position.getRecorderDepartment() == null || position.getRecorderDepartment().getId() == null) {
            position.setRecorderDepartment(parent.getRecorderDepartment());
        }

        position.setOperationId(parent.getId());
    }

    protected void fillDefaultProperties(OperationVO parent, MeasurementVO measurement, Class<? extends IMeasurementEntity> entityClass) {
        if (measurement == null) return;

        // Copy recorder department from the parent
        if (measurement.getRecorderDepartment() == null || measurement.getRecorderDepartment().getId() == null) {
            measurement.setRecorderDepartment(parent.getRecorderDepartment());
        }

        measurement.setEntityName(entityClass.getSimpleName());
    }

    protected void fillDefaultProperties(OperationVO parent, BatchVO batch) {
        if (batch == null) return;

        // Copy recorder department from the parent
        if (batch.getRecorderDepartment() == null || batch.getRecorderDepartment().getId() == null) {
            batch.setRecorderDepartment(parent.getRecorderDepartment());
        }

        batch.setOperationId(parent.getId());
    }

    protected void fillDefaultProperties(BatchVO parent, BatchVO batch) {
        if (batch == null) return;

        // Copy recorder department from the parent
        if (batch.getRecorderDepartment() == null || batch.getRecorderDepartment().getId() == null) {
            batch.setRecorderDepartment(parent.getRecorderDepartment());
        }

        if (parent.getId() == null) {
            // Need to be the parent object, when parent has not id yet (see issue #2)
            batch.setParent(parent);
        } else {
            batch.setParentId(parent.getId());
        }
        batch.setOperationId(parent.getOperationId());
    }

    protected void fillDefaultProperties(OperationVO parent, SampleVO sample) {
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
            Date sampleDate = parent.getEndDateTime() != null ? parent.getEndDateTime() : parent.getFishingEndDateTime();
            sample.setSampleDate(sampleDate);
        }

        sample.setOperationId(parent.getId());
    }

    protected void fillDefaultProperties(OperationVO parent, FishingAreaVO fishingArea) {

        fishingArea.setOperationId(parent.getId());
    }

    protected List<BatchVO> getAllBatches(OperationVO operation) {
        BatchVO catchBatch = operation.getCatchBatch();
        fillDefaultProperties(operation, catchBatch);
        List<BatchVO> result = Lists.newArrayList();
        addAllBatchesToList(catchBatch, result);
        return result;
    }

    protected void addAllBatchesToList(final BatchVO batch, final List<BatchVO> result) {
        if (batch == null) return;

        // Add the batch itself
        if (!result.contains(batch)) result.add(batch);

        // Process children
        if (CollectionUtils.isNotEmpty(batch.getChildren())) {
            // Recursive call
            batch.getChildren().forEach(child -> {
                fillDefaultProperties(batch, child);
                addAllBatchesToList(child, result);
            });
        }
    }

    /**
     * Get all samples, in the sample tree parent/children
     *
     * @param parent
     * @return
     */
    protected List<SampleVO> getSamplesAsList(final OperationVO parent) {
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

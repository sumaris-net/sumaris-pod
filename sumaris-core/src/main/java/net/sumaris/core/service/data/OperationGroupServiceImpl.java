package net.sumaris.core.service.data;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.dao.data.MeasurementDao;
import net.sumaris.core.dao.data.OperationGroupDao;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.model.data.GearUseMeasurement;
import net.sumaris.core.model.data.IMeasurementEntity;
import net.sumaris.core.model.data.VesselUseMeasurement;
import net.sumaris.core.util.Beans;
import net.sumaris.core.vo.data.BatchVO;
import net.sumaris.core.vo.data.MeasurementVO;
import net.sumaris.core.vo.data.OperationGroupVO;
import net.sumaris.core.vo.referential.MetierVO;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author peck7 on 28/11/2019.
 */
@Service("operationGroupService")
public class OperationGroupServiceImpl implements OperationGroupService {

    @Autowired
    protected SumarisConfiguration config;

    @Autowired
    private OperationGroupDao operationGroupDao;

    @Autowired
    protected MeasurementDao measurementDao;

    @Autowired
    protected BatchService batchService;

    @Autowired
    protected PhysicalGearService physicalGearService;

    @Override
    public List<MetierVO> getMetiersByTripId(int tripId) {
        return operationGroupDao.getMetiersByTripId(tripId);
    }

    @Override
    public List<MetierVO> saveMetiersByTripId(int tripId, List<MetierVO> metiers) {

        Preconditions.checkNotNull(metiers);

        return operationGroupDao.saveMetiersByTripId(tripId, metiers);
    }

    @Override
    public void updateUndefinedOperationDates(int tripId, Date startDate, Date endDate) {

        Preconditions.checkNotNull(startDate);
        Preconditions.checkNotNull(endDate);

        operationGroupDao.updateUndefinedOperationDates(tripId, startDate, endDate);
    }

    @Override
    public List<OperationGroupVO> getAllByTripId(int tripId, int offset, int size, String sortAttribute, SortDirection sortDirection) {
        return operationGroupDao.getAllByTripId(tripId, offset, size, sortAttribute, sortDirection);
    }

    @Override
    public OperationGroupVO get(int id) {
        return operationGroupDao.get(id);
    }

    @Override
    public OperationGroupVO save(OperationGroupVO source) {

        Preconditions.checkNotNull(source);
        Preconditions.checkNotNull(source.getRecorderDepartment(), "Missing recorderDepartment");
        Preconditions.checkNotNull(source.getRecorderDepartment().getId(), "Missing recorderDepartment.id");

        // Default properties
        if (source.getQualityFlagId() == null) {
            source.setQualityFlagId(config.getDefaultQualityFlagId());
        }

        OperationGroupVO savedOperationGroup = operationGroupDao.save(source);

        // Save measurements (vessel use measurement)
        {
            if (source.getMeasurementValues() != null) {
                measurementDao.saveOperationVesselUseMeasurementsMap(savedOperationGroup.getId(), source.getMeasurementValues());
            } else {
                List<MeasurementVO> measurements = Beans.getList(source.getMeasurements());
                measurements.forEach(m -> fillDefaultProperties(savedOperationGroup, m, VesselUseMeasurement.class));
                measurements = measurementDao.saveOperationVesselUseMeasurements(savedOperationGroup.getId(), measurements);
                savedOperationGroup.setMeasurements(measurements);
            }
        }

        // Save gear measurements (gear use measurement)
        {
            if (source.getGearMeasurementValues() != null) {
                measurementDao.saveOperationGearUseMeasurementsMap(savedOperationGroup.getId(), source.getGearMeasurementValues());
            } else {
                List<MeasurementVO> measurements = Beans.getList(source.getGearMeasurements());
                measurements.forEach(m -> fillDefaultProperties(savedOperationGroup, m, GearUseMeasurement.class));
                measurements = measurementDao.saveOperationGearUseMeasurements(savedOperationGroup.getId(), measurements);
                savedOperationGroup.setGearMeasurements(measurements);
            }
        }

        // Save gear measurements (gear physical measurement)
        {
            physicalGearService.save(savedOperationGroup.getTripId(), ImmutableList.of(savedOperationGroup.getPhysicalGear()));
        }

        // Save batches
        {
            List<BatchVO> batches = getAllBatches(savedOperationGroup);
            batches.forEach(b -> fillDefaultProperties(savedOperationGroup, b));
            batches = batchService.saveByOperationId(savedOperationGroup.getId(), batches);

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

            savedOperationGroup.setCatchBatch(null);
            savedOperationGroup.setBatches(batches);
        }

        // TODO save products

        return savedOperationGroup;

    }

    @Override
    public List<OperationGroupVO> save(List<OperationGroupVO> operationGroups) {
        Preconditions.checkNotNull(operationGroups);

        return operationGroups.stream()
            .map(this::save)
            .collect(Collectors.toList());
    }

    @Override
    public List<OperationGroupVO> saveAllByTripId(int tripId, List<OperationGroupVO> operationGroups) {
        Preconditions.checkNotNull(operationGroups);

        // affect tripId
        operationGroups.forEach(operationGroup -> operationGroup.setTripId(tripId));

        return save(operationGroups);
    }

    @Override
    public void delete(int id) {
        operationGroupDao.delete(id);
    }

    @Override
    public void delete(List<Integer> ids) {
        Preconditions.checkNotNull(ids);
        ids.stream()
            .filter(Objects::nonNull)
            .forEach(this::delete);
    }

    /* protected methods */

    protected List<BatchVO> getAllBatches(OperationGroupVO operation) {
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

    protected void fillDefaultProperties(OperationGroupVO parent, MeasurementVO measurement, Class<? extends IMeasurementEntity> entityClass) {
        if (measurement == null) return;

        // Copy recorder department from the parent
        if (measurement.getRecorderDepartment() == null || measurement.getRecorderDepartment().getId() == null) {
            measurement.setRecorderDepartment(parent.getRecorderDepartment());
        }

        measurement.setEntityName(entityClass.getSimpleName());
    }

    protected void fillDefaultProperties(OperationGroupVO parent, BatchVO batch) {
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

}

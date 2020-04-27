package net.sumaris.core.service.data;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.dao.data.MeasurementDao;
import net.sumaris.core.dao.data.OperationGroupDao;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.model.data.GearUseMeasurement;
import net.sumaris.core.model.data.IMeasurementEntity;
import net.sumaris.core.model.data.VesselUseMeasurement;
import net.sumaris.core.service.referential.pmfm.PmfmService;
import net.sumaris.core.util.Beans;
import net.sumaris.core.vo.data.BatchVO;
import net.sumaris.core.vo.data.MeasurementVO;
import net.sumaris.core.vo.data.OperationGroupVO;
import net.sumaris.core.vo.referential.MetierVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;
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

    @Autowired
    protected PmfmService pmfmService;

    @Autowired
    protected ProductService productService;

    @Autowired
    protected PacketService packetService;

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
    public List<OperationGroupVO> getAllByTripId(int tripId) {
        return operationGroupDao.getAllByTripId(tripId);
    }

    @Override
    public OperationGroupVO get(int id) {
        return operationGroupDao.get(id);
    }

    @Override
    public OperationGroupVO save(final OperationGroupVO source) {

        // Check operation group validity
        checkOperationGroup(source);

        // Save entity
        operationGroupDao.save(source);

        // Save linked entities
        saveChildrenEntities(source);

        return source;
    }

    @Override
    public List<OperationGroupVO> save(List<OperationGroupVO> operationGroups) {
        Preconditions.checkNotNull(operationGroups);

        return operationGroups.stream()
            .map(this::save)
            .collect(Collectors.toList());
    }

    @Override
    public List<OperationGroupVO> saveAllByTripId(int tripId, List<OperationGroupVO> sources) {
        Preconditions.checkNotNull(sources);
        sources.forEach(this::checkOperationGroup);

        // Save entities
        List<OperationGroupVO> saved = operationGroupDao.saveAllByTripId(tripId, sources);

        // Save children entities
        saved.forEach(this::saveChildrenEntities);

        return saved;
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

    protected void checkOperationGroup(final OperationGroupVO source) {
        Preconditions.checkNotNull(source);
        Preconditions.checkNotNull(source.getRecorderDepartment(), "Missing recorderDepartment");
        Preconditions.checkNotNull(source.getRecorderDepartment().getId(), "Missing recorderDepartment.id");
    }

    protected void saveChildrenEntities(final OperationGroupVO source) {


        // Dispatch measurements from measurementValues (which should contains all measurements)
        {
            Map<Integer, String> vesselUseMeasurements = Maps.newLinkedHashMap();
            Map<Integer, String> gearUseMeasurements = Maps.newLinkedHashMap();
            source.getMeasurementValues().forEach((pmfmId, value) -> {
                if (pmfmService.isVesselUsePmfm(pmfmId)) {
                    vesselUseMeasurements.putIfAbsent(pmfmId, value);
                } else if (pmfmService.isGearUsePmfm(pmfmId)) {
                    gearUseMeasurements.putIfAbsent(pmfmId, value);
                }
            });

            // Re-affect to correct map
            source.setMeasurementValues(vesselUseMeasurements);
            source.setGearMeasurementValues(gearUseMeasurements);
        }

        // Save measurements (vessel use measurement)
        {
            if (source.getMeasurementValues() != null) {
                measurementDao.saveOperationVesselUseMeasurementsMap(source.getId(), source.getMeasurementValues());
            } else {
                List<MeasurementVO> measurements = Beans.getList(source.getMeasurements());
                measurements.forEach(m -> fillDefaultProperties(source, m, VesselUseMeasurement.class));
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

        // Save products
        productService.saveByOperationId(source.getId(), source.getProducts());

        // Save packets
        packetService.saveByOperationId(source.getId(), source.getPackets());

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

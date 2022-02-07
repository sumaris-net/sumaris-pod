package net.sumaris.core.service.data;

/*-
 * #%L
 * SUMARiS:: Core
 * %%
 * Copyright (C) 2018 - 2020 SUMARiS Consortium
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
import com.google.common.collect.Maps;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.dao.data.MeasurementDao;
import net.sumaris.core.dao.data.operation.OperationGroupRepository;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.model.data.GearUseMeasurement;
import net.sumaris.core.model.data.IMeasurementEntity;
import net.sumaris.core.model.data.VesselUseMeasurement;
import net.sumaris.core.model.referential.pmfm.MatrixEnum;
import net.sumaris.core.service.referential.pmfm.PmfmService;
import net.sumaris.core.util.Beans;
import net.sumaris.core.vo.data.*;
import net.sumaris.core.vo.data.batch.BatchVO;
import net.sumaris.core.vo.data.sample.SampleVO;
import net.sumaris.core.vo.filter.OperationGroupFilterVO;
import net.sumaris.core.vo.referential.MetierVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author peck7 on 28/11/2019.
 */
@Service("operationGroupService")
@Slf4j
public class OperationGroupServiceImpl implements OperationGroupService {

    @Autowired
    protected SumarisConfiguration config;

    @Autowired
    private OperationGroupRepository operationGroupRepository;

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

    @Autowired
    protected FishingAreaService fishingAreaService;

    @Autowired
    protected SampleService sampleService;

    @Override
    public List<MetierVO> getMetiersByTripId(int tripId) {
        return operationGroupRepository.getMetiersByTripId(tripId);
    }

    @Override
    public List<MetierVO> saveMetiersByTripId(int tripId, List<MetierVO> metiers) {

        Preconditions.checkNotNull(metiers);

        return operationGroupRepository.saveMetiersByTripId(tripId, metiers);
    }

    @Override
    public OperationGroupVO getMainUndefinedOperationGroup(int tripId) {
        return operationGroupRepository.getMainUndefinedOperationGroup(tripId);
    }

    @Override
    public void updateUndefinedOperationDates(int tripId, @NonNull Date startDate, @NonNull Date endDate) {

        operationGroupRepository.updateUndefinedOperationDates(tripId, startDate, endDate);
    }

    public List<OperationGroupVO> findAllByTripId(int tripId, Page page) {
        return operationGroupRepository.findAll(
            OperationGroupFilterVO.builder().tripId(tripId).onlyDefined(true).build(),
            page,
            null);
    }

    @Override
    public List<OperationGroupVO> findAllByTripId(int tripId, Page page, DataFetchOptions fetchOptions) {
        List<OperationGroupVO> result = operationGroupRepository.findAll(
            OperationGroupFilterVO.builder().tripId(tripId).onlyDefined(true).build(),
            page,
            fetchOptions);

        // Fetch packets (because it cannot be called from repo)
        if (fetchOptions != null && fetchOptions.isWithChildrenEntities()) {
            result.stream()
                .filter(o -> o.getId() != null)
                .forEach(o -> o.setPackets(packetService.getAllByOperationId(o.getId())));
        }

        return result;
    }

    @Override
    public List<OperationGroupVO> findAllByTripId(int tripId, DataFetchOptions fetchOptions) {
        return findAllByTripId(tripId, null, fetchOptions);
    }

    @Override
    public OperationGroupVO get(int id) {
        return operationGroupRepository.get(id);
    }

    @Override
    public List<OperationGroupVO> saveAllByTripId(int tripId, List<OperationGroupVO> sources) {
        Preconditions.checkNotNull(sources);
        sources.forEach(this::checkOperationGroup);

        // Save entities
        List<OperationGroupVO> saved = operationGroupRepository.saveAllByTripId(tripId, sources);

        // Save children entities
        saved.forEach(this::saveChildrenEntities);

        return saved;
    }

    @Override
    public void delete(int id) {
        operationGroupRepository.deleteById(id);
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
                if (pmfmService.isGearPmfm(pmfmId)) {
                    gearUseMeasurements.putIfAbsent(pmfmId, value);
                }
                else {
                    vesselUseMeasurements.putIfAbsent(pmfmId, value);
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
        {
            List<ProductVO> products = Beans.getList(source.getProducts());
            products.forEach(product -> fillDefaultProperties(source, product));
            products = productService.saveByOperationId(source.getId(), products);

            // Prepare saved samples (e.g. to be used as graphQL query response)
            products.forEach(product -> {
                // Set parentId (instead of parent object)
                if (product.getOperationId() == null && product.getOperation() != null) {
                    product.setOperationId(product.getOperation().getId());
                }
                // Remove link parent/children
                product.setOperation(null);
            });

            source.setProducts(products);
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

        // Save packets
        if (source.getPackets() != null) {

            // Save packets
            List<PacketVO> savedPackets = packetService.saveByOperationId(source.getId(), source.getPackets());

            // Re-affect saved packets to source
            source.setPackets(savedPackets);
        }

        // Save fishing areas
        if (source.getFishingAreas() != null) {

            source.getFishingAreas().forEach(fishingArea -> fillDefaultProperties(source, fishingArea));
            fishingAreaService.saveAllByOperationId(source.getId(), source.getFishingAreas());
        }
    }

    private void fillDefaultProperties(OperationGroupVO source, FishingAreaVO fishingArea) {

        fishingArea.setOperationId(source.getId());
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

    protected void fillDefaultProperties(OperationGroupVO parent, ProductVO product) {
        if (product == null) return;

        // Copy recorder department from the parent
        if (product.getRecorderDepartment() == null || product.getRecorderDepartment().getId() == null) {
            product.setRecorderDepartment(parent.getRecorderDepartment());
        }

        product.setOperationId(parent.getId());
    }

    protected void fillDefaultProperties(OperationGroupVO parent, PacketVO packet) {
        if (packet == null) return;

        // Copy recorder department from the parent
        if (packet.getRecorderDepartment() == null || packet.getRecorderDepartment().getId() == null) {
            packet.setRecorderDepartment(parent.getRecorderDepartment());
        }

        packet.setOperationId(parent.getId());
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

    protected void fillDefaultProperties(OperationGroupVO parent, SampleVO sample) {
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

        sample.setOperationId(parent.getId());
    }

    /**
     * Get all samples, in the sample tree parent/children
     *
     */
    protected List<SampleVO> getSamplesAsList(final OperationGroupVO parent) {
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

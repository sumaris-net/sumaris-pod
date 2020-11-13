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
import com.google.common.collect.Maps;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.dao.data.MeasurementDao;
import net.sumaris.core.dao.data.operation.OperationGroupRepository;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.model.data.GearUseMeasurement;
import net.sumaris.core.model.data.IMeasurementEntity;
import net.sumaris.core.model.data.VesselUseMeasurement;
import net.sumaris.core.service.referential.pmfm.PmfmService;
import net.sumaris.core.util.Beans;
import net.sumaris.core.vo.data.*;
import net.sumaris.core.vo.data.batch.BatchVO;
import net.sumaris.core.vo.filter.OperationGroupFilterVO;
import net.sumaris.core.vo.referential.MetierVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * @author peck7 on 28/11/2019.
 */
@Service("operationGroupService")
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
    public void updateUndefinedOperationDates(int tripId, Date startDate, Date endDate) {

        Preconditions.checkNotNull(startDate);
        Preconditions.checkNotNull(endDate);

        operationGroupRepository.updateUndefinedOperationDates(tripId, startDate, endDate);
    }

    @Override
    public List<OperationGroupVO> getAllByTripId(int tripId, int offset, int size, String sortAttribute, SortDirection sortDirection) {
        return operationGroupRepository.findAll(
            OperationGroupFilterVO.builder().tripId(tripId).onlyDefined(true).build(),
            offset,
            size,
            sortAttribute,
            sortDirection,
            null).getContent();
    }

    @Override
    public List<OperationGroupVO> getAllByTripId(int tripId, DataFetchOptions fetchOptions) {
        List<OperationGroupVO> result = operationGroupRepository.findAll(OperationGroupFilterVO.builder().tripId(tripId).onlyDefined(true).build(), fetchOptions);

        if (fetchOptions != null && fetchOptions.isWithChildrenEntities()) {
            // Fetch packets (because it cannot be called from repo)
            result.stream()
                    .filter(o -> o.getId() != null)
                    .forEach(o -> o.setPackets(packetService.getAllByOperationId(o.getId())));
        }
        return result;
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
        if (source.getProducts() != null) source.getProducts().forEach(product -> fillDefaultProperties(source, product));
        productService.saveByOperationId(source.getId(), source.getProducts());

        // Save packets
        if (source.getPackets() != null) {

            // Keep saleProducts before save packets
            Map<Integer, List<ProductVO>> saleProductsByPacketRankOrder = new HashMap<>();
            source.getPackets().forEach(packet -> {
                fillDefaultProperties(source, packet);
                saleProductsByPacketRankOrder.put(packet.getRankOrder(), packet.getSaleProducts());
            });

            // Save packets
            List<PacketVO> savedPackets = packetService.saveByOperationId(source.getId(), source.getPackets());

            // Restore saleProducts
            savedPackets.forEach(savedPacket -> savedPacket.setSaleProducts(saleProductsByPacketRankOrder.get(savedPacket.getRankOrder())));

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

}

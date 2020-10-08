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
import net.sumaris.core.dao.data.BatchDao;
import net.sumaris.core.dao.data.MeasurementDao;
import net.sumaris.core.dao.referential.ReferentialDao;
import net.sumaris.core.dao.technical.Daos;
import net.sumaris.core.exception.DataNotFoundException;
import net.sumaris.core.event.config.ConfigurationEvent;
import net.sumaris.core.event.config.ConfigurationReadyEvent;
import net.sumaris.core.event.config.ConfigurationUpdatedEvent;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.model.data.BatchQuantificationMeasurement;
import net.sumaris.core.model.data.BatchSortingMeasurement;
import net.sumaris.core.model.data.IMeasurementEntity;
import net.sumaris.core.model.referential.pmfm.PmfmEnum;
import net.sumaris.core.model.referential.pmfm.QualitativeValue;
import net.sumaris.core.model.referential.pmfm.QualitativeValueEnum;
import net.sumaris.core.util.Beans;
import net.sumaris.core.vo.data.BatchVO;
import net.sumaris.core.vo.data.MeasurementVO;
import net.sumaris.core.vo.data.PacketCompositionVO;
import net.sumaris.core.vo.data.PacketVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author peck7 on 09/04/2020.
 */
@Service("packetService")
public class PacketServiceImpl implements PacketService {

    private static final Logger logger = LoggerFactory.getLogger(PacketServiceImpl.class);
    private Integer calculatedWeightPmfmId;
    private Integer measuredWeightPmfmId;
    private Integer estimatedRatioPmfmId;
    private Integer sortingPmfmId;
    private static final String RATIO_SEPARATOR = " ";

    @Autowired
    private BatchDao batchDao;

    @Autowired
    private MeasurementDao measurementDao;

    @Autowired
    private ReferentialDao referentialDao;

    @EventListener({ConfigurationReadyEvent.class, ConfigurationUpdatedEvent.class})
    protected void onConfigurationReady(ConfigurationEvent event) {
        // Init pmfm ids
        this.calculatedWeightPmfmId = PmfmEnum.BATCH_CALCULATED_WEIGHT.getId();
        this.measuredWeightPmfmId = PmfmEnum.BATCH_MEASURED_WEIGHT.getId();
        this.estimatedRatioPmfmId = PmfmEnum.BATCH_ESTIMATED_RATIO.getId();
        this.sortingPmfmId = PmfmEnum.BATCH_SORTING.getId();
    }

    @Override
    public List<PacketVO> getAllByOperationId(int operationId) {

        BatchVO catchBatch = batchDao.getRootByOperationId(operationId, true);
        if (catchBatch == null)
            return null;

        return toPackets(catchBatch);
    }

    @Override
    public List<PacketVO> saveByOperationId(int operationId, List<PacketVO> sources) {

        checkPackets(sources);

        // Batches list to save
        List<BatchVO> batches = new ArrayList<>();

        // Get catch batch
        BatchVO rootBatch = batchDao.getRootByOperationId(operationId, false);

        if (rootBatch == null) {

            if (sources.isEmpty()) {

                // nothing to save
                return sources;

            } else {

                // Create new root batch
                rootBatch = new BatchVO();
                rootBatch.setRankOrder(0);
                rootBatch.setLabel(BatchDao.DEFAULT_ROOT_BATCH_LABEL);
                rootBatch.setOperationId(operationId);
            }

        } else {

            if (sources.isEmpty()) {

                // Root batch exists but no packet to save = delete this batch
                batchDao.saveByOperationId(operationId, batches);
                return sources;

            }
        }

        batches.add(rootBatch);

        // Convert Packets to Batches
        batches.addAll(toBatchVOs(sources, rootBatch));

        // Save Batches
        List<BatchVO> savedBatches = batchDao.saveByOperationId(operationId, batches);

        // Save measurements
        savedBatches.forEach(savedBatch -> {

            // Sorting measurement
            {
                List<MeasurementVO> measurements = Beans.getList(savedBatch.getSortingMeasurements());
                measurements.forEach(m -> fillDefaultProperties(savedBatch, m, BatchSortingMeasurement.class));
                measurements = measurementDao.saveBatchSortingMeasurements(savedBatch.getId(), measurements);
                savedBatch.setSortingMeasurements(measurements);
            }

            // Quantification measurement
            {
                List<MeasurementVO> measurements = Beans.getList(savedBatch.getQuantificationMeasurements());
                measurements.forEach(m -> fillDefaultProperties(savedBatch, m, BatchQuantificationMeasurement.class));
                measurements = measurementDao.saveBatchQuantificationMeasurements(savedBatch.getId(), measurements);
                savedBatch.setQuantificationMeasurements(measurements);
            }

        });

        // Return optimistic version of saved beans
        return toPackets(batchDao.toTree(savedBatches));
        // Or load completely
//        return getAllByOperationId(operationId);
    }

    private void checkPackets(List<PacketVO> sources) {
        Preconditions.checkNotNull(sources);
        sources.forEach(this::checkPacket);
    }

    private void checkPacket(PacketVO source) {
        Preconditions.checkNotNull(source);
        Preconditions.checkNotNull(source.getNumber());
        Preconditions.checkArgument(source.getNumber() > 0);
        Preconditions.checkNotNull(source.getRankOrder());
        Preconditions.checkNotNull(source.getWeight());
        Preconditions.checkArgument(CollectionUtils.isNotEmpty(source.getSampledWeights()));
        Preconditions.checkArgument(CollectionUtils.isNotEmpty(source.getComposition()));
        Preconditions.checkNotNull(source.getRecorderDepartment(), "Missing recorderDepartment");
        Preconditions.checkNotNull(source.getRecorderDepartment().getId(), "Missing recorderDepartment.id");
    }

    private List<BatchVO> toBatchVOs(List<PacketVO> sources, BatchVO rootBatch) {
        return sources.stream().flatMap(packet -> toBatchVO(packet, rootBatch).stream()).collect(Collectors.toList());
    }

    private List<BatchVO> toBatchVO(PacketVO source, BatchVO rootBatch) {

        List<BatchVO> result = new ArrayList<>();

        // create sorting batch
        BatchVO target = new BatchVO();
        Beans.copyProperties(source, target);

        // Default values
        target.setExhaustiveInventory(false);

        // Affect parent
        target.setParent(rootBatch);

        // assert some copy todo remove when ok
        if (source.getRecorderDepartment() != null) {
            Preconditions.checkNotNull(target.getRecorderDepartment());
        }
        if (source.getRecorderPerson() != null) {
            Preconditions.checkNotNull(target.getRecorderPerson());
        }

        // Label fixme this is SIH labels
        target.setLabel(source.getRankOrder().toString());

        List<Double> sampledWeights = source.getSampledWeights().stream().filter(Objects::nonNull).collect(Collectors.toList());
        int sampledPacketCount = sampledWeights.size();
        // Individual count = number of sampled packets
        target.setIndividualCount(sampledPacketCount); // in SIH, individual_count is stored in subgroup_count

        Double averagePacketWeight = Daos.roundValue(sampledWeights.stream()
            .mapToDouble(Number::doubleValue)
            .average()
            .orElse(0d));

        // Reference weight
        Double refWeight = sampledPacketCount * averagePacketWeight;
        Double totalWeight = source.getNumber() * averagePacketWeight;
        // update source value
        source.setWeight(totalWeight);

        // Ratio
        target.setSamplingRatioText(String.format("%s/%s", refWeight, totalWeight).replaceAll(",", "."));
        target.setSamplingRatio(refWeight / totalWeight);

        // Prepare measurements
        MeasurementVO sortingMeasurement = null;
        MeasurementVO refWeightMeasurement = null;
        List<MeasurementVO> packetWeightMeasurements = new ArrayList<>();
        if (target.getId() != null) {

            // Sorting Measurement
            sortingMeasurement = measurementDao.getBatchSortingMeasurements(target.getId()).stream()
                .filter(measurementVO -> measurementVO.getPmfmId() == sortingPmfmId)
                .findFirst()
                .orElse(null);

            // Quantification measurements
            List<BatchQuantificationMeasurement> qms = measurementDao.getMeasurementEntitiesByParentId(
                BatchQuantificationMeasurement.class,
                BatchQuantificationMeasurement.Fields.BATCH,
                source.getId(),
                BatchQuantificationMeasurement.Fields.SUBGROUP_NUMBER
            );
            refWeightMeasurement = qms.stream()
                .filter(m -> m.getIsReferenceQuantification() && m.getPmfm().getId().equals(measuredWeightPmfmId))
                .findFirst()
                .map(m -> measurementDao.toMeasurementVO(m))
                .orElse(null);
            packetWeightMeasurements = qms.stream()
                .filter(m -> !m.getIsReferenceQuantification() && m.getPmfm().getId().equals(measuredWeightPmfmId) && m.getSubgroupNumber() != null)
                .sorted(Comparator.comparingInt(BatchQuantificationMeasurement::getSubgroupNumber))
                .map(m -> measurementDao.toMeasurementVO(m))
                .collect(Collectors.toList());

        }

        // Create or update Sorting measurement (bulk as default)
        {
            if (sortingMeasurement == null) {
                sortingMeasurement = createMeasurement(BatchSortingMeasurement.class, sortingPmfmId);
            }
            ReferentialVO qv = referentialDao.findByUniqueLabel(QualitativeValue.class.getSimpleName(), QualitativeValueEnum.SORTING_BULK.getLabel())
                .orElseThrow(() -> new DataNotFoundException(String.format("The qualitative value with label %s was not found", QualitativeValueEnum.SORTING_BULK.getLabel())));
            sortingMeasurement.setQualitativeValue(qv);
            target.setSortingMeasurements(Collections.singletonList(sortingMeasurement));
        }

        // Create or update Quantification measurements
        {
            if (refWeightMeasurement == null) {
                refWeightMeasurement = createMeasurement(BatchQuantificationMeasurement.class, measuredWeightPmfmId);
            }
            refWeightMeasurement.setNumericalValue(refWeight);

            List<MeasurementVO> newPacketWeightMeasurements = new ArrayList<>();
            for (int i = 0; i < sampledWeights.size(); i++) {
                MeasurementVO measurement = Beans.safeGet(packetWeightMeasurements, i);
                if (measurement == null) {
                    measurement = createMeasurement(BatchQuantificationMeasurement.class, measuredWeightPmfmId);
                }
                measurement.setNumericalValue(sampledWeights.get(i));
                newPacketWeightMeasurements.add(measurement);
            }

            // Ordered measurements
            List<MeasurementVO> quantificationMeasurements = new ArrayList<>();
            quantificationMeasurements.add(refWeightMeasurement); // will be reference
            quantificationMeasurements.addAll(newPacketWeightMeasurements);
            target.setQuantificationMeasurements(quantificationMeasurements);

        }

        // Add this sorting batch
        result.add(target);

        // Add composition converted to batchVO
        result.addAll(toBatchVO(averagePacketWeight, source.getComposition(), target));

        return result;
    }

    private List<BatchVO> toBatchVO(Double averagePacketWeight, List<PacketCompositionVO> compositions, BatchVO parentBatch) {
        return compositions.stream()
            .map(composition -> toBatchVO(averagePacketWeight, composition, parentBatch))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    private BatchVO toBatchVO(Double averagePacketWeight, PacketCompositionVO source, BatchVO parentBatch) {

        Preconditions.checkNotNull(source);
        Preconditions.checkNotNull(source.getRankOrder());
        Preconditions.checkNotNull(source.getTaxonGroup());

        if (CollectionUtils.isEmpty(source.getRatios()))
            return null; // Consider the composition to be deleted later

        // create sorting batch
        BatchVO target = new BatchVO();
        Beans.copyProperties(source, target);

        // Default values
        target.setExhaustiveInventory(false);

        // Affect parent
        target.setParent(parentBatch);

        // Label fixme this is SIH labels
        target.setLabel(String.format("%s.%s", parentBatch.getLabel(), source.getRankOrder().toString()));

        // TaxonGroup fixme maybe already affected by copyProperties
        target.setTaxonGroup(source.getTaxonGroup());

        // Measurements
        MeasurementVO sortingMeasurement = null;
        MeasurementVO weightMeasurement = null;
        MeasurementVO ratioMeasurement = null;

        if (target.getId() != null) {

            // Sorting Measurement
            sortingMeasurement = measurementDao.getBatchSortingMeasurements(target.getId()).stream()
                .filter(measurementVO -> measurementVO.getPmfmId() == sortingPmfmId)
                .findFirst()
                .orElse(null);

            List<MeasurementVO> qms = measurementDao.getBatchQuantificationMeasurements(target.getId());
            weightMeasurement = qms.stream()
                .filter(measurementVO -> measurementVO.getPmfmId() == calculatedWeightPmfmId)
                .findFirst()
                .orElse(null);
            ratioMeasurement = qms.stream()
                .filter(measurementVO -> measurementVO.getPmfmId() == estimatedRatioPmfmId)
                .findFirst()
                .orElse(null);

        }

        // Create or update Sorting measurement (bulk as default)
        {
            if (sortingMeasurement == null) {
                sortingMeasurement = createMeasurement(BatchSortingMeasurement.class, sortingPmfmId);
            }
            ReferentialVO qv = referentialDao.findByUniqueLabel(QualitativeValue.class.getSimpleName(), QualitativeValueEnum.SORTING_BULK.getLabel())
                .orElseThrow(() -> new DataNotFoundException(String.format("The qualitative value with label %s was not found", QualitativeValueEnum.SORTING_BULK.getLabel())));
            sortingMeasurement.setQualitativeValue(qv);
            target.setSortingMeasurements(Collections.singletonList(sortingMeasurement));
        }

        // Create or update Quantification measurements
        {
            if (weightMeasurement == null) {
                weightMeasurement = createMeasurement(BatchQuantificationMeasurement.class, calculatedWeightPmfmId);
            }
            double averageRatio = source.getRatios().stream().filter(Objects::nonNull).mapToDouble(Number::doubleValue).average().orElse(0);
            Double calculatedWeight = averageRatio / 100 * averagePacketWeight;
            weightMeasurement.setNumericalValue(calculatedWeight);

            if (ratioMeasurement == null) {
                ratioMeasurement = createMeasurement(BatchQuantificationMeasurement.class, estimatedRatioPmfmId);
            }
            ratioMeasurement.setAlphanumericalValue(
                source.getRatios().stream().filter(Objects::nonNull).map(Object::toString).collect(Collectors.joining(RATIO_SEPARATOR))
            );

            // Ordered measurements
            List<MeasurementVO> quantificationMeasurements = new ArrayList<>();
            quantificationMeasurements.add(weightMeasurement); // will be reference
            quantificationMeasurements.add(ratioMeasurement);
            target.setQuantificationMeasurements(quantificationMeasurements);

        }

        return target;
    }

    private MeasurementVO createMeasurement(Class<?> entityClass, int pmfmId) {
        MeasurementVO measurement = new MeasurementVO();
        measurement.setEntityName(entityClass.getSimpleName());
        measurement.setPmfmId(pmfmId);
        return measurement;
    }

    private List<PacketVO> toPackets(BatchVO catchBatch) {
        return catchBatch.getChildren().stream().map(this::toPacketVO).collect(Collectors.toList());
    }

    protected PacketVO toPacketVO(BatchVO source) {
        if (source == null) return null;

        PacketVO target = new PacketVO();
        Beans.copyProperties(source, target);

        // assert
        Preconditions.checkNotNull(target.getOperationId());

        // Get quantification measurements entities
        List<BatchQuantificationMeasurement> qms = measurementDao.getMeasurementEntitiesByParentId(
            BatchQuantificationMeasurement.class,
            BatchQuantificationMeasurement.Fields.BATCH,
            source.getId(),
            BatchQuantificationMeasurement.Fields.SUBGROUP_NUMBER
        );

        // reference weight
        Double referenceWeight = qms.stream()
            .filter(m -> m.getIsReferenceQuantification() && m.getPmfm().getId().equals(measuredWeightPmfmId))
            .findFirst()
            .map(BatchQuantificationMeasurement::getNumericalValue)
            .map(Daos::roundValue)
            .orElse(null);

        if (referenceWeight == null) {
            throw new SumarisTechnicalException("Packet average weight cannot be null");
        }

        // sampled weights
        target.setSampledWeights(
            qms.stream()
                .filter(m -> !m.getIsReferenceQuantification() && m.getPmfm().getId().equals(measuredWeightPmfmId) && m.getSubgroupNumber() != null)
                .map(BatchQuantificationMeasurement::getNumericalValue)
                .map(Daos::roundValue)
                .collect(Collectors.toList())
        );

        // weight
        target.setWeight(Daos.roundValue(computeTotalWeightFromSamplingRatio(source, referenceWeight)));

        // number
        Double averageWeight = referenceWeight / target.getSampledWeights().size();
        target.setNumber(Daos.roundValue(target.getWeight() / averageWeight).intValue());

        // composition
        target.setComposition(source.getChildren().stream()
            .map(this::toPacketCompositionVO)
            .filter(Objects::nonNull)
            .collect(Collectors.toList())
        );

        return target;
    }

    protected PacketCompositionVO toPacketCompositionVO(BatchVO source) {
        if (source == null
            || source.getTaxonGroup() == null)
            return null;

        PacketCompositionVO target = new PacketCompositionVO();

        target.setId(source.getId());
        target.setRankOrder(source.getRankOrder());
        target.setTaxonGroup(source.getTaxonGroup());

        List<MeasurementVO> measurements = measurementDao.getBatchQuantificationMeasurements(source.getId());

        target.setRatios(measurements.stream()
            .filter(measurementVO -> measurementVO.getPmfmId() == estimatedRatioPmfmId)
            .findFirst()
            .map(MeasurementVO::getAlphanumericalValue)
            .map(ratios -> Arrays.stream(ratios.split(RATIO_SEPARATOR)).map(Integer::valueOf).collect(Collectors.toList()))
            .orElse(null)
        );

        return target;
    }

    protected Double computeTotalWeightFromSamplingRatio(BatchVO sb, Double referenceWeight) {
        Double totalWeight = null;
        String startStr = referenceWeight.toString().replace(',', '.') + "/";
        if (sb.getSamplingRatioText() != null && sb.getSamplingRatioText().startsWith(startStr)) {
            String weightStr = sb.getSamplingRatioText().substring(startStr.length());
            if (!weightStr.isEmpty()) {
                totalWeight = Double.parseDouble(weightStr);
            }
        }
        // Or compute total weight using the samplingRatio - bad precision ;(
        else if (sb.getSamplingRatio() != null) {
            totalWeight = referenceWeight / sb.getSamplingRatio();
        }

        return totalWeight;
    }

    protected void fillDefaultProperties(BatchVO parent, MeasurementVO measurement, Class<? extends IMeasurementEntity> entityClass) {
        if (measurement == null) return;

        // Copy recorder department from the parent
        if (measurement.getRecorderDepartment() == null || measurement.getRecorderDepartment().getId() == null) {
            measurement.setRecorderDepartment(parent.getRecorderDepartment());
        }

        measurement.setEntityName(entityClass.getSimpleName());
    }

}

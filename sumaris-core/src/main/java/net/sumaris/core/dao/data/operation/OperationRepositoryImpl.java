package net.sumaris.core.dao.data.operation;

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

import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.data.DataRepositoryImpl;
import net.sumaris.core.dao.data.MeasurementDao;
import net.sumaris.core.dao.data.VesselPositionDao;
import net.sumaris.core.dao.data.batch.BatchRepository;
import net.sumaris.core.dao.data.fishingArea.FishingAreaRepository;
import net.sumaris.core.dao.data.physicalGear.PhysicalGearRepository;
import net.sumaris.core.dao.data.sample.SampleRepository;
import net.sumaris.core.dao.data.trip.TripRepository;
import net.sumaris.core.dao.referential.metier.MetierRepository;
import net.sumaris.core.dao.technical.Daos;
import net.sumaris.core.model.data.Operation;
import net.sumaris.core.model.data.PhysicalGear;
import net.sumaris.core.model.data.Trip;
import net.sumaris.core.model.referential.QualityFlag;
import net.sumaris.core.model.referential.metier.Metier;
import net.sumaris.core.util.Beans;
import net.sumaris.core.util.Dates;
import net.sumaris.core.vo.data.DataFetchOptions;
import net.sumaris.core.vo.data.OperationFetchOptions;
import net.sumaris.core.vo.data.OperationVO;
import net.sumaris.core.vo.data.TripFetchOptions;
import net.sumaris.core.vo.data.batch.BatchFetchOptions;
import net.sumaris.core.vo.data.sample.SampleFetchOptions;
import net.sumaris.core.vo.filter.OperationFilterVO;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author peck7 on 01/09/2020.
 */
@Slf4j
public class OperationRepositoryImpl
        extends DataRepositoryImpl<Operation, OperationVO, OperationFilterVO, OperationFetchOptions>
        implements OperationSpecifications {

    @Autowired
    private PhysicalGearRepository physicalGearRepository;

    @Autowired
    private MetierRepository metierRepository;

    @Autowired
    private MeasurementDao measurementDao;

    @Autowired
    private SampleRepository sampleRepository;

    @Autowired
    private FishingAreaRepository fishingAreaRepository;

    @Autowired
    private BatchRepository batchRepository;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    protected VesselPositionDao vesselPositionDao;


    protected OperationRepositoryImpl(EntityManager entityManager) {
        super(Operation.class, OperationVO.class, entityManager);
        setLockForUpdate(true);
    }

    @Override
    public void toVO(Operation source, OperationVO target, OperationFetchOptions fetchOptions, boolean copyIfNull) {
        super.toVO(source, target, fetchOptions, copyIfNull);

        // Trip
        if (source.getTrip() != null) {
            target.setTripId(source.getTrip().getId());
            if (fetchOptions != null && fetchOptions.isWithTrip()){
                // We use MINIMAL fetch, because only root attributes are usually expected by the APP
                target.setTrip(tripRepository.toVO(source.getTrip(), TripFetchOptions.MINIMAL));
            }
        }

        // Physical gear
        if (source.getPhysicalGear() != null) {
            target.setPhysicalGearId(source.getPhysicalGear().getId());
            target.setPhysicalGear(physicalGearRepository.toVO(source.getPhysicalGear(), DataFetchOptions.builder()
                    .withRecorderDepartment(false)
                    .withRecorderPerson(false)
                    .build()));
        }

        // Métier
        if (source.getMetier() != null) {
            target.setMetier(metierRepository.toVO(source.getMetier()));
        }

        // Load children entities (not loaded by default)
        Integer operationId = source.getId();
        if (fetchOptions != null && fetchOptions.isWithChildrenEntities() && operationId != null) {

            // Positions
            target.setPositions(vesselPositionDao.getAllByOperationId(operationId));

            // Fishing Areas
            target.setFishingAreas(fishingAreaRepository.findAllVO(fishingAreaRepository.hasOperationId(operationId)));

            // Batches
            target.setBatches(batchRepository.findAllVO(batchRepository.hasOperationId(operationId),
                    BatchFetchOptions.builder()
                            .withChildrenEntities(false) // Use flat list, not a tree
                            .withRecorderDepartment(false)
                            .withMeasurementValues(true)
                            .build()));

            // Samples
            target.setSamples(sampleRepository.findAllVO(sampleRepository.hasOperationId(operationId),
                    SampleFetchOptions.builder()
                            .withChildrenEntities(false) // Use flat list, not a tree
                            .withRecorderDepartment(false)
                            .withMeasurementValues(true)
                            .build()));
        }

        // Measurements
        if (fetchOptions != null && fetchOptions.isWithMeasurementValues() && operationId != null) {
            target.setMeasurements(measurementDao.getOperationVesselUseMeasurements(operationId));
            target.setGearMeasurements(measurementDao.getOperationGearUseMeasurements(operationId));
        }

        // ParentOperation
        if (source.getParentOperation() != null) {
            target.setParentOperationId(source.getParentOperation().getId());
            if (fetchOptions != null && fetchOptions.isWithParentOperation()) {
                fetchOptions = OperationFetchOptions.clone(fetchOptions);
                fetchOptions.setWithParentOperation(false);
                fetchOptions.setWithChildOperation(false);
                target.setParentOperation(toVO(source.getParentOperation(), fetchOptions));
            }
        }

        // ChildOperation
        else if (source.getChildOperation() != null) {
            target.setChildOperationId(source.getChildOperation().getId());
            if (fetchOptions != null && fetchOptions.isWithChildOperation()) {
                fetchOptions = OperationFetchOptions.clone(fetchOptions);
                fetchOptions.setWithParentOperation(false);
                fetchOptions.setWithChildOperation(false);
                target.setChildOperation(toVO(source.getChildOperation(), fetchOptions));
            }
        }
    }

    @Override
    public List<OperationVO> saveAllByTripId(int tripId, List<OperationVO> operations) {

        // Load parent entity
        Trip parent = getById(Trip.class, tripId);

        // Remember existing entities
        final List<Integer> sourcesIdsToRemove = Beans.collectIds(Beans.getList(parent.getOperations()));

        // Save each operation
        List<OperationVO> result = operations.stream().map(source -> {
            source.setTripId(tripId);
            if (source.getId() != null) {
                sourcesIdsToRemove.remove(source.getId());
            }
            return save(source);
        }).collect(Collectors.toList());

        // Remove unused entities
        if (CollectionUtils.isNotEmpty(sourcesIdsToRemove)) {
            sourcesIdsToRemove.forEach(this::deleteById);
        }

        // Update the parent entity
        Daos.replaceEntities(parent.getOperations(),
                result,
                (vo) -> getReference(Operation.class, vo.getId()));

        return result;
    }

    @Override
    public void toEntity(OperationVO source, Operation target, boolean copyIfNull) {
        super.toEntity(source, target, copyIfNull);

        // Trip
        Integer tripId = source.getTripId() != null ? source.getTripId() : (source.getTrip() != null ? source.getTrip().getId() : null);
        if (copyIfNull || (tripId != null)) {
            if (tripId == null) {
                target.setTrip(null);
            } else {
                // Use get() and NOT load(), because trip object will be used later (for physicalGears)
                target.setTrip(getById(Trip.class, tripId));
            }
        }

        // Métier
        if (copyIfNull || source.getMetier() != null) {
            if (source.getMetier() == null || source.getMetier().getId() == null) {
                target.setMetier(null);
            } else {
                target.setMetier(getReference(Metier.class, source.getMetier().getId()));
            }
        }

        // Physical gear
        {
            // Read physical gear id
            Integer physicalGearId = source.getPhysicalGearId() != null
                    ? source.getPhysicalGearId()
                    : source.getPhysicalGear() != null ? source.getPhysicalGear().getId() : null;

            // If not found, try using the rankOrder
            if (physicalGearId == null && source.getPhysicalGear() != null && source.getPhysicalGear().getRankOrder() != null && target.getTrip() != null) {
                Integer rankOrder = source.getPhysicalGear().getRankOrder();
                physicalGearId = target.getTrip().getGears()
                        .stream()
                        .filter(g -> rankOrder != null && Objects.equals(g.getRankOrder(), rankOrder))
                        .map(PhysicalGear::getId)
                        .findFirst().orElse(null);
                if (physicalGearId == null) {
                    throw new DataIntegrityViolationException(
                            String.format("Operation {starDateTime: '%s'} use a unknown PhysicalGear. PhysicalGear with {rankOrder: %s} not found in gears Trip.",
                                    Dates.toISODateTimeString(source.getStartDateTime()),
                                    source.getPhysicalGear().getRankOrder()
                            ));
                }
                source.setPhysicalGearId(physicalGearId);
                source.setPhysicalGear(null);
            }

            if (copyIfNull || physicalGearId != null) {
                if (physicalGearId == null) {
                    target.setPhysicalGear(null);
                } else {
                    target.setPhysicalGear(getReference(PhysicalGear.class, physicalGearId));
                }
            }
        }

        // Parent Operation
        Integer parentOperationId = source.getParentOperationId() != null ? source.getParentOperationId() : (source.getParentOperation() != null ? source.getParentOperation().getId() : null);
        if (copyIfNull || parentOperationId != null) {
            if (parentOperationId == null) {
                target.setParentOperation(null);
            } else {
                target.setParentOperation(getReference(Operation.class, parentOperationId));
            }
        }

        // Child Operation
        Integer childOperationId = source.getChildOperationId() != null ? source.getChildOperationId() : (source.getChildOperation() != null ? source.getChildOperation().getId() : null);
        if (copyIfNull || childOperationId != null) {
            if (childOperationId == null) {
                target.setChildOperation(null);
            } else {
                target.setChildOperation(getReference(Operation.class, childOperationId));
            }
        }

        //Quality Flag
        Integer qualityFlag = source.getQualityFlagId();
        if (qualityFlag != null) {
            target.setQualityFlag(getReference(QualityFlag.class, qualityFlag));
        } else {
            target.setQualityFlag(getReference(QualityFlag.class, getConfig().getDefaultQualityFlagId()));
        }
    }

    @Override
    protected Specification<Operation> toSpecification(OperationFilterVO filter, OperationFetchOptions fetchOptions) {
        return super.toSpecification(filter, fetchOptions)
            .and(distinct())
            .and(excludeOperationGroup())
            .and(hasTripId(filter.getTripId()))
            .and(hasProgramLabel(filter.getProgramLabel()))
            .and(hasVesselId(filter.getVesselId()))
            .and(excludedIds(filter.getExcludedIds()))
            .and(includedIds(filter.getIncludedIds()))
            .and(excludeChildOperation(filter.getExcludeChildOperation()))
            .and(hasNoChildOperation(filter.getHasNoChildOperation()))
            .and(isBetweenDates(filter.getStartDate(), filter.getEndDate()))
            .and(inGearIds(filter.getGearIds()))
            .and(inPhysicalGearIds(filter.getPhysicalGearIds()))
            .and(inTaxonGroupLabels(filter.getTaxonGroupLabels()))
            .and(hasQualityFlagIds(filter.getQualityFlagIds()))
            .and(inDataQualityStatus(filter.getDataQualityStatus()));
    }

    @Override
    protected void onAfterSaveEntity(OperationVO vo, Operation savedEntity, boolean isNew) {
        super.onAfterSaveEntity(vo, savedEntity, isNew);

        if (vo.getParentOperation() == null && vo.getParentOperationId() != null) {
            vo.setParentOperation(this.get(vo.getParentOperationId()));
        }
    }
}

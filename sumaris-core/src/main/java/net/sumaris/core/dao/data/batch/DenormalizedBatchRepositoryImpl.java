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

package net.sumaris.core.dao.data.batch;

import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.dao.referential.pmfm.ParameterRepository;
import net.sumaris.core.dao.referential.pmfm.PmfmRepository;
import net.sumaris.core.dao.technical.jpa.SumarisJpaRepositoryImpl;
import net.sumaris.core.dao.technical.model.TreeNodeEntities;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.model.data.DenormalizedBatch;
import net.sumaris.core.model.data.Operation;
import net.sumaris.core.model.data.Sale;
import net.sumaris.core.model.referential.QualityFlag;
import net.sumaris.core.model.referential.QualityFlagEnum;
import net.sumaris.core.model.referential.pmfm.PmfmEnum;
import net.sumaris.core.model.referential.pmfm.QualitativeValueEnum;
import net.sumaris.core.model.referential.pmfm.UnitEnum;
import net.sumaris.core.util.Beans;
import net.sumaris.core.util.Numbers;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.data.MeasurementVO;
import net.sumaris.core.vo.data.batch.*;
import net.sumaris.core.vo.data.QuantificationMeasurementVO;
import net.sumaris.core.vo.referential.ParameterVO;
import net.sumaris.core.vo.referential.PmfmVO;
import net.sumaris.core.vo.referential.PmfmValueType;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.mutable.MutableShort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

import javax.annotation.Nonnull;
import javax.persistence.EntityManager;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author peck7 on 09/06/2020.
 */
@Slf4j
public class DenormalizedBatchRepositoryImpl
    extends SumarisJpaRepositoryImpl<DenormalizedBatch, Integer, DenormalizedBatchVO>
    implements DenormalizedBatchRepositoryExtend<DenormalizedBatchVO> {


    private final SumarisConfiguration config;
    private final PmfmRepository pmfmRepository;
    private final ParameterRepository parameterRepository;

    @Autowired
    @Lazy
    private DenormalizedBatchRepository self;

    @Autowired
    public DenormalizedBatchRepositoryImpl(EntityManager entityManager,
                                           SumarisConfiguration config,
                                           PmfmRepository pmfmRepository,
                                           ParameterRepository parameterRepository) {
        super(DenormalizedBatch.class, entityManager);
        this.config = config;
        this.pmfmRepository = pmfmRepository;
        this.parameterRepository = parameterRepository;
    }

    @Override
    public Class<DenormalizedBatchVO> getVOClass() {
        return DenormalizedBatchVO.class;
    }

    @Override
    public void toVO(DenormalizedBatch source, DenormalizedBatchVO target, boolean copyIfNull) {
        super.toVO(source, target, copyIfNull);

        Integer operationId = source.getOperation() != null ? source.getOperation().getId() : null;
        if (copyIfNull || source.getOperation() != null) {
            target.setOperationId(operationId);
        }
    }

    @Override
    public void toEntity(DenormalizedBatchVO source, DenormalizedBatch target, boolean copyIfNull) {
        // Always same id
        target.setId(source.getId());

        super.toEntity(source, target, copyIfNull);

        /*if (copyIfNull || source.getQualityFlagId() != null) {
            if (source.getQualityFlagId() == null) {
                target.setQualityFlag(load(QualityFlag.class, config.getDefaultQualityFlagId()));
            } else {
                target.setQualityFlag(load(QualityFlag.class, source.getQualityFlagId()));
            }
        }*/

        // Quality flag
        if (copyIfNull || source.getQualityFlagId() != null) {
            if (source.getQualityFlagId() == null) {
                target.setQualityFlag(load(QualityFlag.class, config.getDefaultQualityFlagId()));
            } else {
                target.setQualityFlag(load(QualityFlag.class, source.getQualityFlagId()));
            }
        }

        // Parent operation
        Integer operationId = source.getOperationId() != null ? source.getOperationId() : (source.getOperation() != null ? source.getOperation().getId() : null);
        source.setOperationId(operationId);
        if (copyIfNull || (operationId != null)) {
            if (operationId == null) {
                target.setOperation(null);
            } else {
                target.setOperation(load(Operation.class, operationId));
            }
        }

        // Parent sale
        Integer saleId = source.getSaleId() != null ? source.getSaleId() : (source.getSale() != null ? source.getSale().getId() : null);
        source.setSaleId(saleId);
        if (copyIfNull || (saleId != null)) {
            if (saleId == null) {
                target.setSale(null);
            } else {
                target.setSale(load(Sale.class, saleId));
            }
        }

    }

    @Override
    public List<DenormalizedBatchVO> saveAllByOperationId(int operationId, @Nonnull List<DenormalizedBatchVO> sources) {

        // Set parent link
        sources.forEach(b -> b.setOperationId(operationId));

        // Get existing fishing areas
        Set<Integer> existingIds = self.getAllIdsByOperationId(operationId);

        // Save
        sources.forEach(b -> {
            save(b);
            existingIds.remove(b.getId());
        });

        // Delete remaining objects
        existingIds.forEach(this::deleteById);

        return sources;
    }

    @Override
    public List<DenormalizedBatchVO> saveAllBySaleId(int saleId, @Nonnull List<DenormalizedBatchVO> sources) {

        // Set parent link
        sources.forEach(b -> b.setSaleId(saleId));

        // Get existing fishing areas
        Set<Integer> existingIds = self.getAllIdsBySaleId(saleId);

        // Save
        sources.forEach(b -> {
            save(b);
            existingIds.remove(b.getId());
        });

        // Delete remaining objects
        existingIds.forEach(this::deleteById);

        return sources;
    }

    @Override
    public List<DenormalizedBatchVO> denormalized(BatchVO catchBatch) {

        boolean trace = log.isTraceEnabled();
        long now = System.currentTimeMillis();
        final MutableShort flatRankOrder = new MutableShort(0);

        List<DenormalizedBatchVO> result = TreeNodeEntities.<BatchVO, DenormalizedBatchVO>streamAllAndMap(catchBatch, (source, parent) -> {
            TempDenormalizedBatchVO target = toTempVO(source);

            // Add to parent's children
            if (parent != null) parent.addChildren(target);

            // Depth level
            if (parent == null) {
                target.setTreeLevel((short)1); // First level
            }
            else {
                target.setTreeLevel((short)(parent.getTreeLevel() + 1));
                // Inherit taxon group
                if (target.getInheritedTaxonGroup() == null && parent.getInheritedTaxonGroup() != null) {
                    target.setInheritedTaxonGroup(parent.getInheritedTaxonGroup());
                }
                // Inherit taxon name
                if (target.getInheritedTaxonName() == null && parent.getInheritedTaxonName() != null) {
                    target.setInheritedTaxonName(parent.getInheritedTaxonName());
                }
                // Exhaustive inventory
                if (parent.getExhaustiveInventory() != null && target.getExhaustiveInventory() == null)  {
                    target.setExhaustiveInventory(parent.getExhaustiveInventory());
                }
                // Inherit location
                if (parent.getLocationId() != null) {
                    target.setLocationId(parent.getLocationId());
                }

                // Inherit quality flag (keep the worst value)
                if (parent.getQualityFlagId() > target.getQualityFlagId()) {
                    target.setQualityFlagId(parent.getQualityFlagId());
                }

                // If current quality is out of stats
                if (target.getQualityFlagId() >= QualityFlagEnum.OUT_STATS.getId()) {
                    // Force both parent and current parent exhaustive inventory to FALSE
                    parent.setExhaustiveInventory(Boolean.FALSE);
                    target.setExhaustiveInventory(Boolean.FALSE);
                }

                // Inherit sorting values
                Beans.getStream(parent.getSortingValues()).forEach(svSource -> {
                    DenormalizedBatchSortingValueVO svTarget = new DenormalizedBatchSortingValueVO();
                    Beans.copyProperties(svSource, svTarget);
                    svTarget.setIsInherited(true);
                    svTarget.setRankOrder(svSource.getRankOrder() / 10);
                    target.addSortingValue(svTarget);
                });


            }

         return target;
        })
        // Sort
        .sorted(Comparator.comparing(DenormalizedBatches::computeFlatOrder))
        .map(target -> {
            // Compute flat rank order
            flatRankOrder.increment();
            target.setFlatRankOrder(flatRankOrder.getValue());

            // Compute tree indent (run once, on the root batch)
            if (target.getParent() == null) computeTreeIndent(target);

            return target;
        })
        .collect(Collectors.toList());

        // Compute indirect values
        computeIndirectValues(result);

        // Elevate weight
        computeElevatedValues(result);

        // Log
        if (trace) {
            log.trace("Successfully denormalized batches (in {}ms):\n{}",
                    System.currentTimeMillis() - now,
                    DenormalizedBatches.dumpAsString(result, true, true));
        }

        return result;
    }

    public DenormalizedBatch toEntity(DenormalizedBatchVO vo) {
        Preconditions.checkNotNull(vo);
        DenormalizedBatch entity = null;

        entity = createEntity();

        toEntity(vo, entity, true);

        return entity;
    }

    @Override
    public <S extends DenormalizedBatch> S save(S entity) {
        this.getSession().saveOrUpdate(entity);
        return entity;
    }

    /* -- protected methods -- */

    protected void computeIndirectValues(List<DenormalizedBatchVO> batches) {

        List<TempDenormalizedBatchVO> revertBatches = batches.stream()
                .map(target -> (TempDenormalizedBatchVO)target)
                // Reverse order (start from leaf)
                .sorted(Collections.reverseOrder(Comparator.comparing(DenormalizedBatchVO::getFlatRankOrder)))
                .collect(Collectors.toList());

        // Compute indirect values (from children to parent)
        MutableInt changedBatchCount = new MutableInt(0);
        MutableInt loopCounter = new MutableInt(0);
        do {
            loopCounter.increment();
            log.debug("Computing indirect values... (pass #{})", loopCounter);

            changedBatchCount.setValue(0);

            revertBatches.forEach(batch -> {
                boolean changed = false;

                // Indirect weight
                Double indirectWeight = computeIndirectWeight(batch);
                changed = changed || !Objects.equals(indirectWeight, batch.getIndirectWeight());
                batch.setIndirectWeight(indirectWeight);

                // Indirect individual count
                Integer indirectIndividualCount = computeIndirectIndividualCount(batch);
                changed = changed || !Objects.equals(indirectIndividualCount, batch.getIndirectIndividualCount());
                batch.setIndirectIndividualCount(indirectIndividualCount);


                // Contextual weight
                //Double contextWeight = computeContextWeight(target);
                //changed = changed || !Objects.equals(contextWeight, target.getContextWeight());
                //target.setContextWeight(contextWeight);

                // Compute Round weight weight
                //Double sumChildRoundWeight = computeSumChildRoundWeight(target);
                //changed = changed || !Objects.equals(sumChildRoundWeight, target.getSumChildRoundWeight());

                // Compute RTP weight
                //Double sumChildRtpWeight = computeSumChildRTPWeight(target);
                //changed = changed || !Objects.equals(sumChildRtpWeight, target.getSumChildRTPWeight());

                if (changed) changedBatchCount.increment();
            });

            log.trace("Computing indirect values... (pass #{}) [OK] - {} changes", loopCounter, changedBatchCount);
        }

        // Continue while changes has been applied on tree
        while (changedBatchCount.intValue() > 0);
    }

    /**
     * Compute elevated values
     */
    protected void computeElevatedValues(List<DenormalizedBatchVO> batches) {
        MutableInt changesCount = new MutableInt(0);

        log.debug("Computing elevated values...");
        batches.stream().map(target -> (TempDenormalizedBatchVO)target)
            .forEach(target -> {
                boolean changed = false;

                Double elevateFactor = target.getElevateFactor();
                if (elevateFactor == null) {
                    elevateFactor = 1d;
                    if (target.getParent() != null) {
                        elevateFactor *= ((TempDenormalizedBatchVO)target.getParent()).getElevateFactor();
                    }
                }
                // Remember it, for children
                target.setElevateFactor(elevateFactor);

                Double weight = target.getWeight() != null ? target.getWeight() : target.getIndirectWeight();
                if (weight != null) {
                    Double elevateWeight = weight * elevateFactor;
                    changed = changed || !Objects.equals(elevateWeight, target.getElevateWeight());
                    target.setElevateWeight(elevateWeight);
                }

                Integer individualCount = target.getIndividualCount() != null ? target.getIndividualCount() : target.getIndirectIndividualCount();
                if (individualCount != null) {
                    Integer elevateIndividualCount = new Double(individualCount * elevateFactor).intValue();
                    changed = changed || !Objects.equals(elevateIndividualCount, target.getElevateIndividualCount());
                    target.setElevateIndividualCount(elevateIndividualCount);
                }

                if (changed) changesCount.increment();
            });

        log.trace("Computing elevated values... [OK] - {} changes", changesCount);
    }

    protected void computeTreeIndent(DenormalizedBatchVO target) {
        computeTreeIndent(target, "", true);
    }

    protected void computeTreeIndent(DenormalizedBatchVO target, String inheritedTreeIndent, boolean isLast) {
        if (target.getParent() == null) {
            target.setTreeIndent("-");
        } else {
            target.setTreeIndent(inheritedTreeIndent + (isLast? "|_" : "|-"));
        }

        List<DenormalizedBatchVO> children = target.getChildren();
        if (CollectionUtils.isNotEmpty(children)) {
            String childrenTreeIndent = inheritedTreeIndent + (isLast? "  " : "|  ");
            for (int i = 0; i < children.size(); i++) {
                computeTreeIndent(children.get(i), childrenTreeIndent, i == children.size() - 1);
            }
        }
    }

    protected DenormalizedBatchVO toVO(BatchVO source) {
        DenormalizedBatchVO target = new DenormalizedBatchVO();
        toVO(source, target, true);
        return target;
    }

    protected TempDenormalizedBatchVO toTempVO(BatchVO source) {
        TempDenormalizedBatchVO target = new TempDenormalizedBatchVO();
        toVO(source, target, true);
        return target;
    }

    protected void toVO(BatchVO source, DenormalizedBatchVO target, boolean copyIfNull) {
        Beans.copyProperties(source, target);

        // Quality flag (default value)
        if (target.getQualityFlagId() == null) {
            target.setQualityFlagId(QualityFlagEnum.NOT_QUALIFED.getId());
        }

        // Init sorting value rank order (must be high, to let inherited values BEFORE current )
        MutableInt sortingValueRankOrder = new MutableInt( 10000);

        if (source.getMeasurementValues() != null) {
            Set<Integer> pmfmIds = source.getMeasurementValues().keySet();

            // Init rankOrder with a very high value, inherited values must be BEFORE current values
            pmfmIds.stream()
                    .forEach(pmfmId -> {
                        String valStr = source.getMeasurementValues().get(pmfmId);
                        if (StringUtils.isBlank(valStr)) return; // Skip

                        // Weight
                        if (this.isWeightPmfm(pmfmId)) {
                            Double weight = Double.parseDouble(valStr);
                            target.setWeight(weight);
                            target.setWeightMethodId(getPmfmMethodId(pmfmId));
                        }

                        // landing / discard
                        else {
                            if (pmfmId == PmfmEnum.DISCARD_OR_LANDING.getId()) {
                                Integer qvId = Integer.parseInt(valStr);
                                target.setIsLanding(Objects.equals(qvId, QualitativeValueEnum.LANDING.getId()));
                                target.setIsDiscard(Objects.equals(qvId, QualitativeValueEnum.DISCARD.getId()));
                            }

                            // Any other sorting value
                            sortingValueRankOrder.increment();
                            DenormalizedBatchSortingValueVO sv = toSortingValueVO(pmfmId, valStr, sortingValueRankOrder.intValue());
                            if (sv == null) {
                                throw new SumarisTechnicalException(String.format("Unable to convert sorting value, in batch {id: %s, label: '%s'}. Pmfm {id: %s}",
                                        target.getId(), target.getLabel(), pmfmId));
                            }
                            target.addSortingValue(sv);
                        }
                    });

        }
        else {
            // weight;
            if (source.getQuantificationMeasurements() != null) {
                source.getQuantificationMeasurements().stream()
                        .filter(QuantificationMeasurementVO::getIsReferenceQuantification)
                        .forEach(m -> {
                            Double weight = m.getNumericalValue();
                            if (weight != null) {
                                target.setWeight(weight);
                                target.setWeightMethodId(getPmfmMethodId(m.getPmfmId()));
                            }
                        });
            }

            if (source.getSortingMeasurements() != null) {
                source.getSortingMeasurements().forEach(m -> {
                    int pmfmId = m.getPmfmId();
                    if (pmfmId == PmfmEnum.DISCARD_OR_LANDING.getId()) {
                        Integer qvId = m.getQualitativeValue() != null ? m.getQualitativeValue().getId() : null;
                        target.setIsLanding(Objects.equals(qvId, QualitativeValueEnum.LANDING.getId()));
                        target.setIsDiscard(Objects.equals(qvId, QualitativeValueEnum.DISCARD.getId()));
                    }

                    // Any other sorting value
                    DenormalizedBatchSortingValueVO sv = toSortingValueVO(pmfmId, m, sortingValueRankOrder.intValue() + m.getRankOrder());
                    if (sv == null) {
                        throw new SumarisTechnicalException(String.format("Unable to convert sorting value, in batch {id: %s, label: '%s'}. Pmfm {id: %s}",
                                target.getId(), target.getLabel(), pmfmId));
                    }
                    target.addSortingValue(sv);
                });
            }
        }

        // Sorting value text
        if (copyIfNull || CollectionUtils.isNotEmpty(target.getSortingValues())) {
            String sortingValuesText = computeSortingValuesText(target.getSortingValues());
            target.setSortingValuesText(sortingValuesText);
        }

        // Taxon group
        if (copyIfNull || source.getTaxonGroup() != null) {
            target.setTaxonGroup(source.getTaxonGroup());
            target.setInheritedTaxonGroup(target.getTaxonGroup());
        }

        // Taxon name
        if (copyIfNull || source.getTaxonName() != null) {
            target.setTaxonName(source.getTaxonName());
            target.setInheritedTaxonName(source.getTaxonName());
        }

        // Link to parent: operation or sale
        if (copyIfNull || source.getOperationId() != null) {
            target.setOperationId(source.getOperationId());
        }
        if (copyIfNull || source.getSaleId() != null) {
            target.setOperationId(source.getSaleId());
        }

    }

    protected boolean isWeightPmfm(int pmfmId) {
        return pmfmRepository.hasLabelSuffix(pmfmId, "WEIGHT");
    }

    protected PmfmVO getPmfm(int pmfmId) {
        return pmfmRepository.get(pmfmId);
    }


    protected Integer getPmfmMethodId(int pmfmId) {
        PmfmVO pmfm = pmfmRepository.get(pmfmId);
        return pmfm != null ? pmfm.getMethodId() : null;
    }

    protected Double computeSumChildContextWeight(TempDenormalizedBatchVO batch) {

        // Cannot compute indirect weight, when:
        // - No children
        // - Not exhaustive
        if (!DenormalizedBatches.isExhaustiveInventory(batch)
                || !batch.hasChildren()) return null;

        try {
            // Child batch is a sampling batch
            if (DenormalizedBatches.isParentOfSamplingBatch(batch)) {
                return null;
            }

            return Beans.getStream(batch.getChildren())
                    .mapToDouble(child -> {
                        if (child.getWeight() != null) return child.getWeight();
                        if (child.getIndirectWeight() != null) return child.getIndirectWeight();
                        if (child.hasChildren()) {
                            Double sumChildContextWeight = computeSumChildContextWeight((TempDenormalizedBatchVO)child);
                            if (sumChildContextWeight != null) {
                                return sumChildContextWeight;
                            }
                        }
                        throw new SumarisTechnicalException(String.format("Cannot compute sum child context weight,"
                                + " because some child batch has no weight {id: %s, label: '%s'}", child.getId(), child.getLabel()));
                    }).sum();
        }
        catch(SumarisTechnicalException e) {
            log.trace(e.getMessage());
            return null;
        }
    }

    protected Double computeIndirectWeight(TempDenormalizedBatchVO batch) {
        // Already computed: skip
        if (batch.getIndirectWeight() != null) return batch.getIndirectWeight();

        // Sampling batch
        if (DenormalizedBatches.isSamplingBatch(batch)) {
            Double samplingWeight = computeSamplingWeightAndRatio(batch, false);
            if (samplingWeight != null) return samplingWeight;
            // Continue if not set
        }

        // Child batch is a sampling batch
        if (DenormalizedBatches.isParentOfSamplingBatch(batch)) {
            return computeParentSamplingWeight(batch, false);
        }

        return computeSumChildrenWeight(batch);
    }

    protected Double computeSumChildrenWeight(DenormalizedBatchVO batch) {
        // Cannot compute children sum, when:
        // - Not exhaustive inventory
        // - No children
        if (!DenormalizedBatches.isExhaustiveInventory(batch)
                || !batch.hasChildren()) {
            return null;
        }

        try {
            return Beans.getStream(batch.getChildren())
                    .map(child -> (TempDenormalizedBatchVO)child)
                    .mapToDouble(child -> {
                        if (child.getWeight() != null) return child.getWeight();
                        if (child.getIndirectWeight() != null) return child.getIndirectWeight();
                        if (child.hasChildren()) {
                            Double indirectWeight = computeIndirectWeight(child);
                            if (indirectWeight != null) {
                                return indirectWeight;
                            }
                        }
                        throw new SumarisTechnicalException(String.format("Cannot compute indirect weight,"
                                + " because some child batch has no weight {id: %s, label: '%s'}", child.getId(), child.getLabel()));
                    }).sum();
        }
        catch(SumarisTechnicalException e) {
            log.trace(e.getMessage());
            return null;
        }
    }


    protected Integer computeIndirectIndividualCount(TempDenormalizedBatchVO batch) {
        // Already computed: skip
        if (batch.getIndirectIndividualCount() != null) return batch.getIndirectIndividualCount();

        // Cannot compute when:
        // - Not exhaustive inventory
        // - No children
        if (!DenormalizedBatches.isExhaustiveInventory(batch)
                || !batch.hasChildren()) {
            return null;
        }

        try {
            return Beans.getStream(batch.getChildren())
                    .map(child -> (TempDenormalizedBatchVO)child)
                    .mapToInt(child -> {
                        if (child.getIndividualCount() != null) return child.getIndividualCount();
                        if (child.hasChildren()) {
                            Integer indirectIndividualCount = computeIndirectIndividualCount(child);
                            if (indirectIndividualCount != null) {
                                return indirectIndividualCount;
                            }
                        }
                        throw new SumarisTechnicalException(String.format("Cannot compute indirect individual count,"
                                + " because some child batch has no individual count {id: %s, label: '%s'}", child.getId(), child.getLabel()));
                    }).sum();
        }
        catch(SumarisTechnicalException e) {
            log.trace(e.getMessage());
            return null;
        }
    }

    protected Double computeSamplingWeightAndRatio(TempDenormalizedBatchVO batch, boolean checkArgument) {
        if (checkArgument)
            Preconditions.checkArgument(DenormalizedBatches.isSamplingBatch(batch));

        Double samplingWeight = null;
        Double samplingRatio = null;
        Double elevateFactor = null;

        if (batch.getSamplingRatio() != null) {
            samplingRatio = batch.getSamplingRatio();
            elevateFactor = 1 / samplingRatio;

            // Try to use the sampling ratio text (more accuracy)
            if (StringUtils.isNotBlank(batch.getSamplingRatioText()) && batch.getSamplingRatioText().contains("/")) {
                String[] parts = batch.getSamplingRatioText().split("/", 2);
                try {
                    samplingRatio = Double.parseDouble(parts[0]) / Double.parseDouble(parts[1]);
                    elevateFactor = Double.parseDouble(parts[1]) / Double.parseDouble(parts[0]);
                } catch (Exception e) {
                    log.warn(String.format("Cannot parse samplingRatioText on batch {id: %s, label: '%s', saplingRatioText: '%s'} : %s",
                            batch.getId(),
                            batch.getLabel(),
                            batch.getSamplingRatioText(),
                            e.getMessage()));
                }
            }
        }
        else if (batch.getParent().getWeight() != null && batch.getWeight() != null) {
            samplingRatio = batch.getWeight() / batch.getParent().getWeight();
            elevateFactor = batch.getParent().getWeight() / batch.getWeight();
        }

        else if (batch.getParent().getWeight() != null && batch.hasChildren()) {
            samplingWeight = computeSumChildrenWeight(batch);
            if (samplingWeight != null) {
                samplingRatio = samplingWeight / batch.getParent().getWeight();
                elevateFactor = batch.getParent().getWeight() / samplingWeight;
            }
        }

        if (samplingRatio == null || elevateFactor == null)
            throw new SumarisTechnicalException(String.format("Invalid fraction batch {id: %s, label: '%s'}: cannot get or compute the sampling ratio",
                    batch.getId(), batch.getLabel()));

        // Remember values
        batch.setSamplingRatio(samplingRatio);
        batch.setElevateFactor(elevateFactor);

        if (samplingWeight == null) {
            if (batch.getWeight() != null) {
                samplingWeight = batch.getWeight();
            } else if (batch.hasParent() && batch.getParent().getWeight() != null) {
                samplingWeight = batch.getParent().getWeight() * samplingRatio;
            }
        }

        return samplingWeight;
    }

    protected Double computeParentSamplingWeight(TempDenormalizedBatchVO parent, boolean checkArgument) {
        if (checkArgument) Preconditions.checkArgument(DenormalizedBatches.isParentOfSamplingBatch(parent));

        // Use reference weight, if any
        if (parent.getWeight() != null) {
            return parent.getWeight();
        }

        TempDenormalizedBatchVO batch = (TempDenormalizedBatchVO)CollectionUtils.extractSingleton(parent.getChildren());

        Double parentWeight = null;
        Double samplingRatio = null;
        Double elevateFactor = null;
        if (batch.getSamplingRatio() != null) {
            samplingRatio = batch.getSamplingRatio();
            elevateFactor = 1 / samplingRatio;

            // Try to use the sampling ratio text (more accuracy)
            if (StringUtils.isNotBlank(batch.getSamplingRatioText()) && batch.getSamplingRatioText().contains("/")) {
                String[] parts = batch.getSamplingRatioText().split("/", 2);
                try {
                    Double shouldBeSamplingWeight = Double.parseDouble(parts[0]);
                    Double shouldBeParentWeight = Double.parseDouble(parts[1]);
                    // If ratio text use the sampling weight, we have the parent weight
                    if (Objects.equals(shouldBeSamplingWeight, batch.getWeight())
                        || Objects.equals(shouldBeSamplingWeight, batch.getIndirectWeight())) {
                        parentWeight = shouldBeParentWeight;
                    }
                    samplingRatio = shouldBeSamplingWeight / shouldBeParentWeight;
                    elevateFactor = shouldBeParentWeight / shouldBeSamplingWeight;
                } catch (Exception e) {
                    log.warn(String.format("Cannot parse samplingRatioText on batch {id: %s, label: '%s', saplingRatioText: '%s'} : %s",
                            batch.getId(),
                            batch.getLabel(),
                            batch.getSamplingRatioText(),
                            e.getMessage()));
                }
            }
        }

        if (samplingRatio == null)
            throw new SumarisTechnicalException(String.format("Invalid fraction batch {id: %s, label: '%s'}: cannot get or compute the sampling ratio",
                    batch.getId(), batch.getLabel()));

        if (parentWeight == null) {
            if (batch.getWeight() != null) {
                parentWeight = batch.getWeight() * elevateFactor;
            } else if (batch.getIndirectWeight() != null) {
                parentWeight = batch.getIndirectWeight() * elevateFactor;
            }
        }

        return parentWeight;
    }

    protected Double computeElevateWeight(TempDenormalizedBatchVO batch) {

        if (DenormalizedBatches.isSamplingBatch(batch)) {
            Double samplingRatio = null;
            Double elevateFactor = null;
            if (batch.getSamplingRatio() != null) {
                samplingRatio = batch.getSamplingRatio();
                elevateFactor = 1 / samplingRatio;

                // Try to use the sampling ratio text (more accuracy)
                if (StringUtils.isNotBlank(batch.getSamplingRatioText()) && batch.getSamplingRatioText().contains("/")) {
                    String[] parts = batch.getSamplingRatioText().split("/", 2);
                    try {
                        samplingRatio = Double.parseDouble(parts[0]) / Double.parseDouble(parts[1]);
                        elevateFactor = Double.parseDouble(parts[1]) / Double.parseDouble(parts[0]);
                    } catch (Exception e) {
                        log.warn(String.format("Cannot parse samplingRatioText on batch {id: %s, label: '%s', saplingRatioText: '%s'} : %s",
                                batch.getId(),
                                batch.getLabel(),
                                batch.getSamplingRatioText(),
                                e.getMessage()));
                    }

                }
            }
            else if (batch.getParent().getWeight() != null && batch.getWeight() != null) {
                samplingRatio = batch.getWeight() / batch.getParent().getWeight();
                elevateFactor = batch.getParent().getWeight() / batch.getWeight();
            }

            if (samplingRatio == null || elevateFactor == null)
                throw new SumarisTechnicalException(String.format("Invalid fraction batch {id: %s, label: '%s'}: cannot get or compute the sampling ratio",
                    batch.getId(), batch.getLabel()));

            Double elevateWeight = null;
            if (batch.getWeight() != null) {
                elevateWeight = batch.getWeight() * elevateFactor;
            }
            else if (batch.getIndirectWeight() != null) {
                elevateWeight = batch.getIndirectWeight() * elevateFactor;
            }
            else if (batch.hasParent() && batch.getParent().getWeight() != null) {
                elevateWeight = batch.getParent().getWeight() * samplingRatio;
            }

            if (elevateWeight == null) {
                throw new SumarisTechnicalException(String.format("Invalid fraction batch {id: %s, label: '%s'}: missing weight, indirect weight, or parent weight ", batch.getId(), batch.getLabel()));
            }

            return elevateWeight;
        }

        return null;
    }

    protected DenormalizedBatchSortingValueVO toSortingValueVO(int pmfmId,
                                                               String valStr,
                                                               int rankOrder) {
        PmfmVO pmfm = getPmfm(pmfmId);
        PmfmValueType pmfmType = PmfmValueType.fromPmfm(pmfm);
        ParameterVO parameter = parameterRepository.get(pmfm.getParameterId());

        DenormalizedBatchSortingValueVO target = new DenormalizedBatchSortingValueVO();
        target.setRankOrder(rankOrder);
        target.setIsInherited(false);
        target.setPmfmId(pmfmId);
        target.setPmfm(pmfm);
        target.setParameter(parameter);
        target.setUnit(ReferentialVO.builder()
                .id(pmfm.getUnitId())
                .label(pmfm.getUnitLabel())
                .build());

        // Value
        if (pmfmType == PmfmValueType.DOUBLE || pmfmType == PmfmValueType.INTEGER) {
            target.setNumericalValue(Double.parseDouble(valStr));
        }
        else if (pmfmType == PmfmValueType.BOOLEAN) {
            target.setNumericalValue("true".equalsIgnoreCase(valStr) || "1".equals(valStr) ? 1d : 0d);
        }
        else if (pmfmType == PmfmValueType.STRING || pmfmType == PmfmValueType.DATE) {
            target.setAlphanumericalValue(valStr);
        }
        else if (pmfmType == PmfmValueType.QUALITATIVE_VALUE) {
            Integer qvId = Integer.parseInt(valStr);
            ReferentialVO qv = Beans.getStream(pmfm.getQualitativeValues())
                    .filter(item -> qvId.equals(item.getId()))
                    .findFirst().orElse(ReferentialVO.builder()
                    .id(Integer.parseInt(valStr))
                    .label("?")
                    .build());
            target.setQualitativeValue(qv);
        }
        else {
            return null;
        }


        return target;
    }

    protected DenormalizedBatchSortingValueVO toSortingValueVO(int pmfmId,
                                                               MeasurementVO measurement,
                                                               int rankOrder) {
        PmfmVO pmfm = getPmfm(pmfmId);
        ParameterVO parameter = parameterRepository.get(pmfm.getParameterId());

        DenormalizedBatchSortingValueVO target = new DenormalizedBatchSortingValueVO();
        target.setRankOrder(rankOrder);
        target.setIsInherited(false);
        target.setPmfmId(pmfmId);
        target.setPmfm(pmfm);
        target.setParameter(parameter);
        target.setUnit(ReferentialVO.builder()
                .id(pmfm.getUnitId())
                .label(pmfm.getUnitLabel())
                .build());

        // Value
        target.setNumericalValue(measurement.getNumericalValue());
        target.setAlphanumericalValue(measurement.getAlphanumericalValue());
        target.setQualitativeValue(measurement.getQualitativeValue());

        return target;
    }

    protected String computeSortingValuesText(Collection<DenormalizedBatchSortingValueVO> sources) {
        return Beans.getStream(sources)
                .map(this::getSortingValueText)
                .collect(Collectors.joining(", "));
    }

    protected String getSortingValueText(DenormalizedBatchSortingValueVO source) {

        StringBuffer result = new StringBuffer();

        // Parameter
        String parameterName = source.getParameter().getName();
        if (parameterName.matches(".*\\([A-Z]+\\)")) {
            parameterName = parameterName.replaceAll(".*\\(([A-Z]+)\\)", "$1=");
            result.append(parameterName);
        }

        // Value
        if (source.getNumericalValue() != null) {
            result.append(
                    Numbers.format(source.getNumericalValue(), source.getPmfm().getMaximumNumberDecimals())
            );

            // Unit label (=symbol)
            if (source.getUnit() != null && !Objects.equals(source.getUnit().getId(), UnitEnum.NONE.getId())) {
                result.append(" ").append(source.getUnit().getLabel());
            }
        }
        else if (source.getAlphanumericalValue() != null) {
            result.append(source.getAlphanumericalValue());
        }
        else if (source.getQualitativeValue() != null) {
            ReferentialVO qv = source.getQualitativeValue();
            result.append(qv.getLabel());
            //result.append(" - ").append(qv.getName());
        }


        return result.toString();
    }
}

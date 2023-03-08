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
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.data.batch.BatchRepository;
import net.sumaris.core.dao.data.batch.DenormalizedBatchRepository;
import net.sumaris.core.dao.data.batch.InvalidSamplingBatchException;
import net.sumaris.core.event.config.ConfigurationEvent;
import net.sumaris.core.event.config.ConfigurationReadyEvent;
import net.sumaris.core.event.config.ConfigurationUpdatedEvent;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.model.TreeNodeEntities;
import net.sumaris.core.model.administration.programStrategy.ProgramPropertyEnum;
import net.sumaris.core.model.referential.QualityFlagEnum;
import net.sumaris.core.model.referential.StatusEnum;
import net.sumaris.core.model.referential.location.LocationLevels;
import net.sumaris.core.model.referential.pmfm.MethodEnum;
import net.sumaris.core.model.referential.pmfm.ParameterEnum;
import net.sumaris.core.model.referential.pmfm.QualitativeValueEnum;
import net.sumaris.core.model.referential.pmfm.UnitEnum;
import net.sumaris.core.model.referential.taxon.TaxonGroupTypeEnum;
import net.sumaris.core.service.administration.programStrategy.ProgramService;
import net.sumaris.core.service.referential.conversion.RoundWeightConversionService;
import net.sumaris.core.service.referential.conversion.WeightLengthConversionService;
import net.sumaris.core.service.referential.taxon.TaxonGroupService;
import net.sumaris.core.util.Beans;
import net.sumaris.core.util.Dates;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.util.TimeUtils;
import net.sumaris.core.vo.administration.programStrategy.ProgramFetchOptions;
import net.sumaris.core.vo.administration.programStrategy.ProgramVO;
import net.sumaris.core.vo.administration.programStrategy.Programs;
import net.sumaris.core.vo.data.batch.*;
import net.sumaris.core.vo.filter.ReferentialFilterVO;
import net.sumaris.core.vo.referential.TaxonGroupVO;
import net.sumaris.core.vo.referential.conversion.RoundWeightConversionFilterVO;
import net.sumaris.core.vo.referential.conversion.RoundWeightConversionVO;
import net.sumaris.core.vo.referential.conversion.WeightLengthConversionFilterVO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.mutable.MutableShort;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service("denormalizedBatchService")
@RequiredArgsConstructor
@Slf4j
public class DenormalizedBatchServiceImpl implements DenormalizedBatchService {

    protected final DenormalizedBatchRepository denormalizedBatchRepository;

    protected final BatchRepository batchRepository;

    protected final ProgramService programService;

    protected final OperationService operationService;

    protected final SaleService saleService;

    protected final TaxonGroupService taxonGroupService;

    protected final WeightLengthConversionService weightLengthConversionService;

    protected final RoundWeightConversionService roundWeightConversionService;

    @EventListener({ConfigurationReadyEvent.class, ConfigurationUpdatedEvent.class})
    public void onConfigurationReady(ConfigurationEvent event) {
        // Check useful enumerations
        if (ParameterEnum.SEX.getId() < 0)
            log.warn("ParameterEnum.SEX not defined. Will not be able to run denormalization with RTP weight enabled");
        if (QualitativeValueEnum.SEX_UNSEXED.getId() < 0)
            log.warn("QualitativeValueEnum.SEX_UNSEXED not defined. Will not be able to run denormalization with RTP weight enabled");
        if (Beans.getStream(LocationLevels.getStatisticalRectangleLevelIds())
            .anyMatch(id -> id < 0))
            log.warn("LocationLevelEnum not defined for statistical rectangle levels. Will not be able to run denormalization with RTP weight enabled");
    }

    @Override
    public List<DenormalizedBatchVO> denormalize(@NonNull BatchVO catchBatch, @NonNull final DenormalizedBatchOptions options) {
        if (options.isEnableRtpWeight()) {
            // Check useful enumerations
            Preconditions.checkArgument(!(ParameterEnum.SEX.getId() < 0), "ParameterEnum.SEX not defined. Cannot compute RTP weight");
            Preconditions.checkArgument(!(QualitativeValueEnum.SEX_UNSEXED.getId() < 0), "QualitativeValueEnum.SEX_UNSEXED not defined. Cannot compute RTP weight");
            Preconditions.checkArgument(!Beans.getStream(LocationLevels.getStatisticalRectangleLevelIds()).anyMatch(id -> id < 0),
                "LocationLevelEnum not defined for statistical rectangle levels. Cannot compute RTP weight");

            // Check options
            Preconditions.checkNotNull(options.getRoundWeightCountryLocationId(), "Required options.roundWeightCountryLocationId when RTP weight enabled");
            Preconditions.checkNotNull(options.getDefaultLandingDressingId(), "Required options.defaultLandingDressingId when RTP weight enabled");
            Preconditions.checkNotNull(options.getDefaultDiscardDressingId(), "Required options.defaultDiscardDressingId when RTP weight enabled");
            Preconditions.checkNotNull(options.getDefaultLandingPreservationId(), "Required options.defaultLandingPreservationId when RTP weight enabled");
            Preconditions.checkNotNull(options.getDefaultDiscardDressingId(), "Required options.defaultDiscardDressingId when RTP weight enabled");
        }

        boolean trace = log.isTraceEnabled();
        long startTime = System.currentTimeMillis();
        final MutableShort flatRankOrder = new MutableShort(0);

        List<DenormalizedBatchVO> result = TreeNodeEntities.<BatchVO, DenormalizedBatchVO>streamAllAndMap(catchBatch, (source, parent) -> {
                TempDenormalizedBatchVO target = createTempVO(source);

                // Add to parent's children
                if (parent != null) parent.addChildren(target);

                // Depth level
                if (parent == null) {
                    target.setTreeLevel((short) 1); // First level
                    if (target.getIsLanding() == null) target.setIsLanding(false);
                    if (target.getIsDiscard() == null) target.setIsDiscard(false);
                } else {
                    target.setTreeLevel((short) (parent.getTreeLevel() + 1));
                    // Inherit taxon group
                    if (target.getInheritedTaxonGroup() == null && parent.getInheritedTaxonGroup() != null) {
                        target.setInheritedTaxonGroup(parent.getInheritedTaxonGroup());
                    }
                    // Inherit taxon name
                    if (target.getInheritedTaxonName() == null && parent.getInheritedTaxonName() != null) {
                        target.setInheritedTaxonName(parent.getInheritedTaxonName());
                    }
                    // Exhaustive inventory
                    if (target.getExhaustiveInventory() == null) {
                        // Always true, when:
                        // - taxon name is defined
                        // - taxon group is defined and taxon Name disable (in options)
                        if (target.getInheritedTaxonName() != null) {
                            target.setExhaustiveInventory(Boolean.TRUE);
                        } else if (target.getInheritedTaxonGroup() != null && !options.isEnableTaxonName()) {
                            target.setExhaustiveInventory(Boolean.TRUE);
                        } else if (parent.getExhaustiveInventory() != null) {
                            target.setExhaustiveInventory(parent.getExhaustiveInventory());
                        }
                    }
                    // Inherit location
                    if (parent.getLocationId() != null) {
                        target.setLocationId(parent.getLocationId());
                    }
                    // Inherit landing / discard
                    if (target.getIsLanding() == null) {
                        target.setIsLanding(parent.getIsLanding());
                    }
                    if (target.getIsDiscard() == null) {
                        target.setIsDiscard(parent.getIsDiscard());
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

        // If only the catch batch
        if (CollectionUtils.size(result) == 1) {
            DenormalizedBatchVO target = result.get(0);
            target.setElevateWeight(target.getWeight());
        } else {
            // Compute RTP weight from length (if enabled)
            if (options.isEnableRtpWeight()) {
                computeRtpWeights(result, options);
            }

            // Compute indirect values
            computeIndirectValues(result, options);

            // Elevate weight
            computeElevatedValues(result, options);
        }

        // Log
        if (trace) {
            log.trace("Successfully denormalized batches, in {}:\n{}",
                TimeUtils.printDurationFrom(startTime),
                DenormalizedBatches.dumpAsString(result, true, true));
        } else {
            log.debug("Successfully denormalized batches, in {}", TimeUtils.printDurationFrom(startTime));
        }

        return result;
    }

    @Override
    public List<DenormalizedBatchVO> denormalizeAndSaveByOperationId(int operationId, @Nullable DenormalizedBatchOptions options) {
        BatchVO catchBatch = batchRepository.getCatchBatchByOperationId(operationId, BatchFetchOptions.builder()
            .withChildrenEntities(true)
            .withMeasurementValues(true)
            .withRecorderDepartment(false)
            .build());
        if (catchBatch == null) return null;

        long startTime = System.currentTimeMillis();
        log.debug("Denormalize batches of operation {id: {}}...", operationId);

        // Compute options, for the operation's program
        if (options == null) {
            int programId = operationService.getProgramIdById(operationId);
            options = createOptionsByProgramId(programId);
        }

        // Denormalize batches
        List<DenormalizedBatchVO> batches = denormalize(catchBatch, options);

        // Save denormalized batches
        batches = denormalizedBatchRepository.saveAllByOperationId(operationId, batches);

        log.debug("Denormalize batches of operation {id: {}} [OK] in {}", operationId, TimeUtils.printDurationFrom(startTime));
        return batches;
    }

    @Override
    public List<DenormalizedBatchVO> denormalizeAndSaveBySaleId(int saleId, @Nullable DenormalizedBatchOptions options) {
        BatchVO catchBatch = batchRepository.getCatchBatchBySaleId(saleId, BatchFetchOptions.builder()
            .withChildrenEntities(true)
            .withMeasurementValues(true)
            .withRecorderDepartment(false)
            .build());
        if (catchBatch == null) return null;

        long startTime = System.currentTimeMillis();
        log.debug("Denormalize batches of sale {id: {}}...", saleId);

        // Compute options, for the sale's program
        if (options == null) {
            int programId = saleService.getProgramIdById(saleId);
            options = createOptionsByProgramId(programId);
        }

        // Denormalize batches
        List<DenormalizedBatchVO> denormalizedBatches = denormalize(catchBatch, options);

        // Save denormalized batches
        List<DenormalizedBatchVO> result = denormalizedBatchRepository.saveAllBySaleId(saleId, denormalizedBatches);

        log.debug("Denormalize batches of sale {id: {}} [OK] in {}", saleId, TimeUtils.printDurationFrom(startTime));
        return result;
    }

    @Override
    public DenormalizedBatchOptions createOptionsByProgramId(int programId) {

        ProgramVO program = programService.get(programId, ProgramFetchOptions.builder()
            .withProperties(true)
            .withLocations(false)
            .withStrategies(false)
            .build());

        return createOptionsByProgram(program);
    }

    @Override
    public DenormalizedBatchOptions createOptionsByProgramLabel(String programLabel) {

        ProgramVO program = programService.getByLabel(programLabel, ProgramFetchOptions.builder()
            .withProperties(true)
            .withLocations(false)
            .withStrategies(false)
            .build());

        return createOptionsByProgram(program);
    }

    /* -- protected methods -- */

    protected DenormalizedBatchOptions createOptionsByProgram(@NonNull ProgramVO program) {
        Preconditions.checkNotNull(program.getProperties());

        // Get ids of taxon group without weight
        String taxonGroupsNoWeight = Optional.ofNullable(Programs.getProperty(program, ProgramPropertyEnum.TRIP_BATCH_TAXON_GROUPS_NO_WEIGHT)).orElse("");
        List<Integer> taxonGroupIdsNoWeight = Arrays.stream(taxonGroupsNoWeight.split(","))
            .map(String::trim)
            .map(label -> taxonGroupService.findAllByFilter(ReferentialFilterVO.builder()
                .label(label)
                .levelIds(new Integer[]{TaxonGroupTypeEnum.FAO.getId()})
                .statusIds(new Integer[]{StatusEnum.ENABLE.getId()})
                .build()).stream().findFirst())
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(TaxonGroupVO::getId)
            .toList();


        return DenormalizedBatchOptions.builder()
            .taxonGroupIdsNoWeight(taxonGroupIdsNoWeight)
            .enableTaxonName(Programs.getPropertyAsBoolean(program, ProgramPropertyEnum.TRIP_BATCH_TAXON_NAME_ENABLE))
            .enableTaxonGroup(Programs.getPropertyAsBoolean(program, ProgramPropertyEnum.TRIP_BATCH_TAXON_GROUP_ENABLE))
            .enableRtpWeight(Programs.getPropertyAsBoolean(program, ProgramPropertyEnum.TRIP_BATCH_LENGTH_WEIGHT_CONVERSION_ENABLE))
            .roundWeightCountryLocationId(Programs.getPropertyAsInteger(program, ProgramPropertyEnum.TRIP_BATCH_ROUND_WEIGHT_CONVERSION_COUNTRY_ID))
            .build();
    }

    protected void computeRtpWeights(List<DenormalizedBatchVO> batches, DenormalizedBatchOptions options) {
        // Select leafs
        List<TempDenormalizedBatchVO> leafBatches = batches.stream()
            .map(target -> (TempDenormalizedBatchVO) target)
            .filter(target -> !target.hasChildren())
            .toList();

        int maxRtpWeightDiffPct = options.getMaxRtpWeightDiffPct();

        // For each leaf, try to compute a RTP weight
        leafBatches.forEach(batch -> computeRtpWeight(batch, options)
            .ifPresent(rtpWeight -> {
                // Apply to the batch
                batch.setRtpWeight(rtpWeight);

                // Check diff with existing weight
                if (batch.getWeight() != null && maxRtpWeightDiffPct > 0) {

                    // Compute diff between two weights
                    double errorPct = DenormalizedBatches.computeWeightDiffPercent(rtpWeight, batch.getWeight());

                    // If delta > max % => warn
                    if (errorPct > maxRtpWeightDiffPct) {

                        // Replace weight, if it was a RTP (should be wrong computation in the App ?)
                        if (Objects.equals(batch.getWeightMethodId(), MethodEnum.CALCULATED_WEIGHT_LENGTH.getId())) {
                            log.warn("Batch {} has a invalid RTP weight (computed: {}, actual: {}, delta: {}%). Fixing the RTP weight using the computed value.",
                                batch.getLabel(),
                                batch.getRtpWeight(),
                                batch.getWeight(),
                                errorPct
                            );
                            batch.setWeight(rtpWeight);
                        }
                        else {
                            log.warn("Batch {} has a invalid weight (RTP: {}, actual: {} => delta: {}%)",
                                batch.getLabel(),
                                batch.getRtpWeight(),
                                batch.getWeight(),
                                errorPct
                            );
                        }
                    }
                }
            })
        );
    }
    protected void computeIndirectValues(List<DenormalizedBatchVO> batches, DenormalizedBatchOptions options) {

        List<TempDenormalizedBatchVO> revertBatches = batches.stream()
            .map(target -> (TempDenormalizedBatchVO) target)
            // Reverse order (start from leaf)
            .sorted(Collections.reverseOrder(Comparator.comparing(DenormalizedBatchVO::getFlatRankOrder, Short::compareTo)))
            .toList();

        MutableInt changesCount = new MutableInt(0);
        MutableInt loopCounter = new MutableInt(0);
        do {
            changesCount.setValue(0);
            loopCounter.increment();
            log.debug("Computing indirect values (pass #{}) ...", loopCounter);

            // For each (leaf -> root)
            revertBatches.forEach(batch -> {
                boolean changed = false;

                log.trace("- {}", batch.getLabel());

                // Indirect weight
                Double indirectWeight = computeIndirectWeight(batch, options);
                changed = changed || !Objects.equals(indirectWeight, batch.getIndirectWeight());
                batch.setIndirectWeight(indirectWeight);

                // Indirect RTP weight from length (if enabled)
                if (options.isEnableRtpWeight()) {
                    Double indirectRtpWeight = computeIndirectRtpWeight(batch, options);
                    changed = changed || !Objects.equals(indirectRtpWeight, batch.getIndirectRtpWeight());
                    batch.setIndirectRtpWeight(indirectRtpWeight);
                }

                // Indirect individual count
                BigDecimal indirectIndividualCount = computeIndirectIndividualCount(batch);
                changed = changed || !Objects.equals(indirectIndividualCount, batch.getIndirectIndividualCountDecimal());
                batch.setIndirectIndividualCountDecimal(indirectIndividualCount);

                // Contextual weight
                //Double contextWeight = computeContextWeight(target);
                //changed = changed || !Objects.equals(contextWeight, target.getContextWeight());
                //target.setContextWeight(contextWeight);

                if (changed) changesCount.increment();
            });

            log.trace("Computing indirect values (pass #{}) [OK] - {} changes", loopCounter, changesCount);
        }

        // Continue while changes has been applied on tree
        while (changesCount.intValue() > 0);
    }

    /**
     * Compute elevated values
     */
    protected void computeElevatedValues(List<DenormalizedBatchVO> batches, DenormalizedBatchOptions options) {
        MutableInt changesCount = new MutableInt(0);
        MutableInt loopCounter = new MutableInt(0);

        do {
            changesCount.setValue(0);
            loopCounter.increment();
            log.debug("Computing elevated values (pass #{}) ...", loopCounter);

            // For each (root -> leaf)
            batches.stream()
                .map(target -> (TempDenormalizedBatchVO) target)
                .forEach(target -> {
                    boolean changed = false;

                    log.trace("{} {}", target.getTreeIndent(), target.getLabel());
                    BigDecimal elevateFactor = target.getSamplingFactor();
                    if (elevateFactor == null) {
                        elevateFactor = new BigDecimal(1);
                    }
                    if (target.getParent() != null) {
                        elevateFactor = elevateFactor.multiply(((TempDenormalizedBatchVO) target.getParent()).getElevateFactor());
                    }
                    target.setElevateFactor(elevateFactor); // Remember for children

                    Double weight = target.getWeight() != null ? target.getWeight() : target.getIndirectWeight();
                    if (weight != null) {
                        Double elevateWeight = elevateFactor.multiply(new BigDecimal(weight)).doubleValue();
                        changed = changed || !Objects.equals(elevateWeight, target.getElevateWeight());
                        target.setElevateWeight(elevateWeight);
                    }

                    if (options.isEnableRtpWeight()) {
                        Double rtpWeight = target.getRtpWeight() != null ? target.getRtpWeight() : target.getIndirectRtpWeight();
                        if (rtpWeight != null) {
                            Double elevateRtpWeight = elevateFactor.multiply(new BigDecimal(rtpWeight)).doubleValue();
                            changed = changed || !Objects.equals(elevateRtpWeight, target.getElevateRtpWeight());
                            target.setElevateRtpWeight(elevateRtpWeight);
                        }
                    }

                    BigDecimal individualCount = target.getIndividualCount() != null ? new BigDecimal(target.getIndividualCount()) : target.getIndirectIndividualCountDecimal();
                    if (individualCount != null) {
                        Integer elevateIndividualCount = individualCount.multiply(elevateFactor)
                            .divide(new BigDecimal(1), 0, RoundingMode.HALF_UP) // Round to half up
                            .intValue();
                        changed = changed || !Objects.equals(elevateIndividualCount, target.getElevateIndividualCount());
                        target.setElevateIndividualCount(elevateIndividualCount);

                        // Set final indirect individualCount (rounded to int, from decimal value)
                        if (target.getIndirectIndividualCountDecimal() != null) {
                            target.setIndirectIndividualCount(target.getIndirectIndividualCountDecimal()
                                .divide(new BigDecimal(1), 0, RoundingMode.HALF_UP) // Round to half up
                                .intValue());
                        }
                    }

                    if (changed) changesCount.increment();
                });

            log.trace("Computing elevated values (pass #{}) [OK] - {} changes", loopCounter, changesCount);
        } while (changesCount.intValue() > 0);
    }

    protected Optional<Double> computeRtpWeight(TempDenormalizedBatchVO batch, DenormalizedBatchOptions options) {
        // Already computed: skip
        if (batch.getRtpWeight() != null) return Optional.of(batch.getRtpWeight());

        // No individual count: skip
        // TODO: should we use '1' as default individual count ?
        if (batch.getIndividualCount() == null) return Optional.empty();

        // No taxon: skip
        Integer referenceTaxonId = batch.getTaxonName() != null
            ? batch.getTaxonName().getReferenceTaxonId()
            : (batch.getInheritedTaxonName() != null ? batch.getInheritedTaxonName().getId() : null);
        if (referenceTaxonId == null) return Optional.empty();

        return Beans.getStream(batch.getSortingValues())
            // Filter on length measure
            .filter(sv -> sv.getNumericalValue() != null
                && sv.getParameter() != null && sv.getParameter().getId() != null
                && weightLengthConversionService.isWeightLengthParameter(sv.getParameter().getId())
            )
            .map(measure -> {
                // Get the individual's sex (or not sexed as default)
                Integer sexId = DenormalizedBatches.getSexId(batch).orElse(QualitativeValueEnum.SEX_UNSEXED.getId());
                // Get length precision (default = 1 - see Allegro mantis #8330)
                Double lengthPrecision = measure.getPmfm().getPrecision() != null ? measure.getPmfm().getPrecision() : 1d;

                return weightLengthConversionService.loadFirstByFilter(WeightLengthConversionFilterVO.builder()
                        .month(options.getMonth())
                        .year(options.getYear())
                        .referenceTaxonIds(new Integer[]{referenceTaxonId})
                        .childLocationIds(options.getFishingAreaLocationIds())
                        .lengthPmfmIds(new Integer[]{measure.getPmfmId()})
                        .sexIds(sexId >= 0 ? new Integer[]{sexId} : null)
                        .build())
                    .map(conversion -> weightLengthConversionService.computedWeight(conversion,
                            measure.getNumericalValue(), // Length
                            measure.getUnit().getLabel(),
                            lengthPrecision,
                            batch.getIndividualCount(),
                            UnitEnum.KG.getLabel(),
                            6 // = mg precision
                        )
                    );
            })
            .filter(Optional::isPresent)
            .map(Optional::get)
            .findFirst()
            // Convert alive RTP weight into dressing/preservation weight
            .flatMap(rtpAliveWeight -> convertAliveWeight(batch, options, rtpAliveWeight))
            .map(BigDecimal::doubleValue);
    }

    protected Double computeIndirectWeight(TempDenormalizedBatchVO batch,
                                           DenormalizedBatchOptions options) {
        return computeIndirectWeight(batch, options,
            DenormalizedBatchVO::getWeight,
            DenormalizedBatchVO::getIndirectWeight
        );
    }

    protected Double computeIndirectRtpWeight(TempDenormalizedBatchVO batch,
                                              DenormalizedBatchOptions options) {
        return computeIndirectWeight(batch, options,
            TempDenormalizedBatchVO::getRtpWeight,
            TempDenormalizedBatchVO::getIndirectRtpWeight
        );
    }

    protected Double computeIndirectWeight(TempDenormalizedBatchVO batch,
                                           DenormalizedBatchOptions options,
                                           Function<TempDenormalizedBatchVO, Double> weightGetter,
                                           Function<TempDenormalizedBatchVO, Double> indirectWeightGetter) {
        // Already computed: skip
        Double indirectWeight = indirectWeightGetter.apply(batch);
        if (indirectWeight != null) return indirectWeight;

        // Sampling batch
        if (DenormalizedBatches.isSamplingBatch(batch)) {
            try {
                Double samplingWeight = computeSamplingWeightAndRatio(batch, false,
                    options, weightGetter, indirectWeightGetter);
                if (samplingWeight != null) return samplingWeight;
            } catch (InvalidSamplingBatchException e) {
                // May be not a sampling batch ? (e.g. a species batch)
                indirectWeight = computeSumChildrenWeight(batch, options,
                    weightGetter, indirectWeightGetter);
                if (indirectWeight != null) return indirectWeight;
                throw e;
            }
            // Invalid sampling batch: Continue if not set
        }

        // Child batch is a sampling batch
        if (DenormalizedBatches.isParentOfSamplingBatch(batch)) {
            return computeParentSamplingWeight(batch, false, weightGetter, indirectWeightGetter);
        }

        // Leaf batch: use the current weight, if any
        if (!batch.hasChildren()) {
            return weightGetter.apply(batch);
        }

        indirectWeight = computeSumChildrenWeight(batch, options, weightGetter, indirectWeightGetter);
        return indirectWeight;
    }

    protected Double computeSamplingWeightAndRatio(TempDenormalizedBatchVO batch,
                                                   boolean checkArgument,
                                                   DenormalizedBatchOptions options,
                                                   Function<TempDenormalizedBatchVO, Double> weightGetter,
                                                   Function<TempDenormalizedBatchVO, Double> indirectWeightGetter) {
        if (checkArgument)
            Preconditions.checkArgument(DenormalizedBatches.isSamplingBatch(batch));

        TempDenormalizedBatchVO parent = (TempDenormalizedBatchVO) batch.getParent();
        boolean parentExhaustiveInventory = DenormalizedBatches.isExhaustiveInventory(parent);
        Double samplingWeight = null;
        Double samplingRatio = null;
        BigDecimal samplingFactor = null;
        final int scale = 12;

        if (batch.getSamplingRatio() != null) {
            samplingRatio = batch.getSamplingRatio();
            samplingFactor = new BigDecimal(1).divide(new BigDecimal(samplingRatio), scale, RoundingMode.HALF_UP);

            // Try to restore sampling ratio from text (more accuracy)
            if (StringUtils.isNotBlank(batch.getSamplingRatioText()) && batch.getSamplingRatioText().contains("/")) {
                String[] parts = batch.getSamplingRatioText().split("/", 2);
                try {
                    double d0 = Double.parseDouble(parts[0]);
                    double d1 = Double.parseDouble(parts[1]);
                    samplingRatio = d0 / d1;
                    samplingFactor = new BigDecimal(d1).divide(new BigDecimal(d0), scale, RoundingMode.HALF_UP);
                } catch (Exception e) {
                    log.warn("Cannot parse samplingRatioText on batch {id: {}}, label: '{}', saplingRatioText: '{}'} : {}",
                        batch.getId(),
                        batch.getLabel(),
                        batch.getSamplingRatioText(),
                        e.getMessage());
                }
            }
        } else if (parentExhaustiveInventory && parent.getWeight() != null && batch.getWeight() != null) {
            samplingRatio = batch.getWeight() / parent.getWeight();
            samplingFactor = new BigDecimal(parent.getWeight()).divide(new BigDecimal(batch.getWeight()), scale, RoundingMode.HALF_UP);
        } else if (parentExhaustiveInventory && parent.getWeight() != null && batch.hasChildren()) {
            samplingWeight = computeSumChildrenWeight(batch, options, weightGetter, indirectWeightGetter);
            if (samplingWeight != null) {
                samplingRatio = samplingWeight / parent.getWeight();
                samplingFactor = new BigDecimal(parent.getWeight()).divide(new BigDecimal(samplingWeight), scale, RoundingMode.HALF_UP);
            }
        } else if ((!parentExhaustiveInventory || parent.getWeight() == null) && batch.hasChildren()) {
            samplingWeight = computeSumChildrenWeight(batch, options, weightGetter, indirectWeightGetter);
            if (samplingWeight != null) {
                samplingRatio = 1d;
                samplingFactor = new BigDecimal(1);
            }
        }

        // case of taxon group no weight: compute the ratio by individual count !
        else if (parent.getInheritedTaxonGroup() != null && CollectionUtils.isNotEmpty(options.getTaxonGroupIdsNoWeight())
            && options.getTaxonGroupIdsNoWeight().contains(parent.getInheritedTaxonGroup().getId())) {
            // TODO
            log.warn("Batch {} - TODO compute samplingRatio, using individualCount (Taxon group no weight)", batch.getLabel());
        }

        if (samplingRatio == null || samplingFactor == null) {
            // Use default value (samplingRatio=1) if:
            // - batch has no children
            // - batch is parent of a sampling batch
            if (CollectionUtils.isEmpty(batch.getChildren())) {
                samplingRatio = 1d;
                samplingFactor = new BigDecimal(1);
            } else {
                throw new InvalidSamplingBatchException(String.format("Invalid sampling batch {id: %s, label: '%s'}: cannot get or compute the sampling ratio",
                    batch.getId(), batch.getLabel()));
            }
        }

        // Remember values
        batch.setSamplingRatio(samplingRatio);
        batch.setSamplingFactor(samplingFactor);

        // Find the weight of current sampling batch
        if (samplingWeight == null) {
            Double weight = weightGetter.apply(batch);
            if (weight != null) {
                samplingWeight = weight;
            } else if (parentExhaustiveInventory) {
                Double parentWeight = weightGetter.apply(parent);
                if (parentWeight != null) {
                    samplingWeight = parentWeight * samplingRatio;
                }
            }
        }

        return samplingWeight;
    }


    protected Double computeParentSamplingWeight(TempDenormalizedBatchVO parent,
                                                 boolean checkArgument,
                                                 Function<TempDenormalizedBatchVO, Double> weightGetter,
                                                 Function<TempDenormalizedBatchVO, Double> indirectWeightGetter
    ) {
        if (checkArgument) Preconditions.checkArgument(DenormalizedBatches.isParentOfSamplingBatch(parent));

        // Use reference weight, if any
        Double parentWeight = weightGetter.apply(parent);
        if (parentWeight != null) return parentWeight;

        TempDenormalizedBatchVO samplingBatch = (TempDenormalizedBatchVO) CollectionUtils.extractSingleton(parent.getChildren());
        Double samplingWeight = weightGetter.apply(samplingBatch);
        Double samplingIndirectWeight = indirectWeightGetter.apply(samplingBatch);

        Double samplingRatio = null;
        Double elevateFactor = null;
        if (samplingBatch.getSamplingRatio() != null) {
            samplingRatio = samplingBatch.getSamplingRatio();
            elevateFactor = 1 / samplingRatio;


            // Try to use the sampling ratio text (more accuracy)
            if (StringUtils.isNotBlank(samplingBatch.getSamplingRatioText()) && samplingBatch.getSamplingRatioText().contains("/")) {
                String[] parts = samplingBatch.getSamplingRatioText().split("/", 2);
                try {
                    Double shouldBeSamplingWeight = Double.parseDouble(parts[0]);
                    Double shouldBeParentWeight = Double.parseDouble(parts[1]);
                    // If ratio text use the sampling weight, we have the parent weight
                    if (Objects.equals(shouldBeSamplingWeight, samplingWeight)
                        || Objects.equals(shouldBeSamplingWeight, samplingIndirectWeight)) {
                        parentWeight = shouldBeParentWeight;
                    }
                    samplingRatio = shouldBeSamplingWeight / shouldBeParentWeight;
                    elevateFactor = shouldBeParentWeight / shouldBeSamplingWeight;
                } catch (Exception e) {
                    log.warn(String.format("Cannot parse samplingRatioText on batch {id: %s, label: '%s', saplingRatioText: '%s'} : %s",
                        samplingBatch.getId(),
                        samplingBatch.getLabel(),
                        samplingBatch.getSamplingRatioText(),
                        e.getMessage()));
                }
            }
        }

        if (samplingRatio == null)
            throw new SumarisTechnicalException(String.format("Invalid fraction batch {id: %s, label: '%s'}: cannot get or compute the sampling ratio",
                samplingBatch.getId(), samplingBatch.getLabel()));

        if (parentWeight == null) {
            if (samplingWeight != null) {
                parentWeight = samplingWeight * elevateFactor;
            } else if (samplingIndirectWeight != null) {
                parentWeight = samplingIndirectWeight * elevateFactor;
            }
        }

        return parentWeight;
    }

    protected Double computeSumChildrenWeight(DenormalizedBatchVO batch,
                                              DenormalizedBatchOptions options,
                                              Function<TempDenormalizedBatchVO, Double> weightGetter,
                                              Function<TempDenormalizedBatchVO, Double> indirectWeightGetter) {
        // Cannot compute children sum, when:
        // - Not exhaustive inventory
        // - No children
        if (!DenormalizedBatches.isExhaustiveInventory(batch)
            || !batch.hasChildren()) {
            return null;
        }

        try {
            return Beans.getStream(batch.getChildren())
                .map(child -> (TempDenormalizedBatchVO) child)
                .mapToDouble(child -> {
                    // Use child weight, if any
                    Double weight = weightGetter.apply(child);
                    if (weight != null) return weight;

                    // Compute indirect weight
                    Double indirectWeight = computeIndirectWeight(child, options, weightGetter, indirectWeightGetter);
                    if (indirectWeight != null) return indirectWeight;

                    // Stop here, because we cannot sum all children's weight
                    throw new SumarisTechnicalException(String.format("Cannot compute indirect weight,"
                        + " because some child batch has no weight {id: %s, label: '%s'}", child.getId(), child.getLabel()));
                }).sum();
        } catch (SumarisTechnicalException e) {
            log.trace(e.getMessage());
            return null;
        }
    }

    protected BigDecimal computeIndirectIndividualCount(TempDenormalizedBatchVO batch) {
        // Already computed: skip
        if (batch.getIndirectIndividualCountDecimal() != null) return batch.getIndirectIndividualCountDecimal();

        // Cannot compute when:
        // - Not exhaustive inventory
        // - No children
        if (!DenormalizedBatches.isExhaustiveInventory(batch)
            || !batch.hasChildren()) {
            return null;
        }

        try {
            return Beans.getStream(batch.getChildren())
                .map(child -> (TempDenormalizedBatchVO) child)
                .map(child -> {
                    if (child.getIndividualCount() != null) {
                        BigDecimal samplingFactor = Optional.ofNullable(child.getSamplingFactor()).orElse(new BigDecimal(1));
                        return samplingFactor.multiply(new BigDecimal(child.getIndividualCount()));
                    }
                    if (child.hasChildren()) {
                        BigDecimal indirectIndividualCount = computeIndirectIndividualCount(child);
                        if (indirectIndividualCount != null) {
                            return indirectIndividualCount;
                        }
                    }
                    throw new SumarisTechnicalException(String.format("Cannot compute indirect individual count,"
                        + " because some child batch has no individual count {id: %s, label: '%s'}", child.getId(), child.getLabel()));
                }).reduce(new BigDecimal(0), BigDecimal::add);
        } catch (SumarisTechnicalException e) {
            log.trace(e.getMessage());
            return null;
        }
    }

    protected void computeTreeIndent(DenormalizedBatchVO target) {
        computeTreeIndent(target, "", true);
    }

    protected void computeTreeIndent(DenormalizedBatchVO target, String inheritedTreeIndent, boolean isLast) {
        if (target.getParent() == null) {
            target.setTreeIndent("-");
        } else {
            target.setTreeIndent(inheritedTreeIndent + (isLast ? "|_" : "|-"));
        }

        List<DenormalizedBatchVO> children = target.getChildren();
        if (CollectionUtils.isNotEmpty(children)) {
            String childrenTreeIndent = inheritedTreeIndent + (isLast ? "  " : "|  ");
            for (int i = 0; i < children.size(); i++) {
                computeTreeIndent(children.get(i), childrenTreeIndent, i == children.size() - 1);
            }
        }
    }

    protected TempDenormalizedBatchVO createTempVO(BatchVO source) {
        TempDenormalizedBatchVO target = new TempDenormalizedBatchVO();
        denormalizedBatchRepository.copy(source, target, true);
        return target;
    }

    /**
     * Convert an alive weight into batch's dressing and preservation.
     * If dressing/preservation are whole/fresh, then return unchanged weight
     * @param batch
     * @param aliveWeight
     * @return
     */
    protected Optional<BigDecimal> convertAliveWeight(TempDenormalizedBatchVO batch,
                                                      DenormalizedBatchOptions options,
                                                      BigDecimal aliveWeight) {
        return DenormalizedBatches.getTaxonGroupId(batch)
            .flatMap(taxonGroupId -> {
                // Get dressing, or 'WHL - Whole' by default
                int dressingId = DenormalizedBatches.getDressingId(batch)
                    .orElseGet(() -> batch.getIsLanding()
                        ? options.getDefaultLandingDressingId()
                        : batch.getIsDiscard() ? options.getDefaultDiscardDressingId()
                        : QualitativeValueEnum.DRESSING_WHOLE.getId());

                // Get preservation, or 'FRE - Fresh' by default
                int preservationId = DenormalizedBatches.getPreservationId(batch)
                    .orElseGet(() -> batch.getIsLanding()
                        ? options.getDefaultLandingPreservationId()
                        : batch.getIsDiscard() ? options.getDefaultDiscardPreservationId()
                        : QualitativeValueEnum.PRESERVATION_FRESH.getId());

                boolean isAlive = dressingId == QualitativeValueEnum.DRESSING_WHOLE.getId()
                    && preservationId == QualitativeValueEnum.PRESERVATION_FRESH.getId();
                if (isAlive) return Optional.of(aliveWeight); // Not need to convert

                // Find the best conversion coefficient
                Optional<RoundWeightConversionVO> conversion = roundWeightConversionService.findFirstByFilter(RoundWeightConversionFilterVO.builder()
                    .taxonGroupIds(new Integer[]{taxonGroupId})
                    .dressingIds(new Integer[]{dressingId})
                    .preservingIds(new Integer[]{preservationId})
                    .locationIds(new Integer[]{options.getRoundWeightCountryLocationId()})
                    .date(Dates.resetTime(options.getDateTime())) // Reset time need to for cache key stability
                    .build());

                if (conversion.isEmpty()) {
                    log.warn("No RoundWeightConversion found for {taxonGroupId: {}, dressingId: {}, preservationId: {}, locationId: {}}",
                        taxonGroupId,
                        dressingId,
                        preservationId,
                        options.getRoundWeightCountryLocationId());
                    return Optional.empty();
                }

                // Apply inverse conversion (alive weight / conversion coefficient)
                BigDecimal convertedWeight = aliveWeight.divide(new BigDecimal(conversion.get().getConversionCoefficient()),
                    // Keep same precision
                    aliveWeight.scale(), RoundingMode.HALF_UP);

                return Optional.of(convertedWeight);
            });
    }
}

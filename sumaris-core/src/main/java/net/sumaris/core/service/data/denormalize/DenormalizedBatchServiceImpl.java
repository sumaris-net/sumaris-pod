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

package net.sumaris.core.service.data.denormalize;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
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
import net.sumaris.core.model.IEntity;
import net.sumaris.core.model.TreeNodeEntities;
import net.sumaris.core.model.administration.programStrategy.ProgramPropertyEnum;
import net.sumaris.core.model.annotation.EntityEnums;
import net.sumaris.core.model.referential.QualityFlags;
import net.sumaris.core.model.referential.StatusEnum;
import net.sumaris.core.model.referential.location.LocationLevels;
import net.sumaris.core.model.referential.pmfm.MethodEnum;
import net.sumaris.core.model.referential.pmfm.ParameterEnum;
import net.sumaris.core.model.referential.pmfm.QualitativeValueEnum;
import net.sumaris.core.model.referential.pmfm.UnitEnum;
import net.sumaris.core.model.referential.taxon.TaxonGroupTypeEnum;
import net.sumaris.core.service.administration.programStrategy.ProgramService;
import net.sumaris.core.service.data.OperationService;
import net.sumaris.core.service.data.SaleService;
import net.sumaris.core.service.referential.conversion.RoundWeightConversionService;
import net.sumaris.core.service.referential.conversion.WeightLengthConversionService;
import net.sumaris.core.service.referential.taxon.TaxonGroupService;
import net.sumaris.core.util.Beans;
import net.sumaris.core.util.Numbers;
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
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.mutable.MutableDouble;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.mutable.MutableShort;
import org.nuiton.i18n.I18n;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;

@Service("denormalizedBatchService")
@RequiredArgsConstructor
@Slf4j
public class DenormalizedBatchServiceImpl implements DenormalizedBatchService {

    public static final int INTERMEDIATE_DECIMAL_SCALE = 24; // intermediate scale can be maximized
    public static final int WEIGHT_DECIMAL_SCALE = 6; // grams precision

    protected final DenormalizedBatchRepository denormalizedBatchRepository;

    protected final BatchRepository batchRepository;

    protected final ProgramService programService;

    protected final OperationService operationService;

    protected final SaleService saleService;

    protected final TaxonGroupService taxonGroupService;

    protected final WeightLengthConversionService weightLengthConversionService;

    protected final RoundWeightConversionService roundWeightConversionService;

    private boolean canEnableRtpWeight = true;

    @EventListener({ConfigurationReadyEvent.class, ConfigurationUpdatedEvent.class})
    public void onConfigurationReady(ConfigurationEvent event) {
        // Check useful enumerations
        try {
            checkBaseEnumerations();
        } catch (Exception e) {
            log.warn(e.getMessage());
        }

        // Check is can enable RTP
        {
            boolean enableRtp = true;
            List<String> errorMessages = Lists.newArrayList();

            // Check enumerations used for RTP
            try {
                checkRtpEnumerations();
            } catch (Exception e) {
                enableRtp = false;
                errorMessages.add(e.getMessage());
            }

            // Check statistical rectangle level ids
            if (Beans.getStream(LocationLevels.getStatisticalRectangleLevelIds()).anyMatch(id -> id < 0)) {
                enableRtp = false;
                errorMessages.add(I18n.t("sumaris.error.missingSomeRectangleLocationLevel"));

            }
            if (this.canEnableRtpWeight != enableRtp) {
                this.canEnableRtpWeight = enableRtp;
                if (!enableRtp) log.warn(I18n.t("sumaris.error.denormalization.batch.cannotEnableRtpWeight", "\n" + Joiner.on("\n\t- ").join(errorMessages)));
            }
        }
    }

    @Override
    public List<DenormalizedBatchVO> denormalize(@NonNull BatchVO catchBatch, @NonNull final DenormalizedBatchOptions options) {
        if (options.isEnableRtpWeight()) {
            Preconditions.checkArgument(canEnableRtpWeight, I18n.t("sumaris.error.denormalization.batch.cannotEnableRtpWeight", "See startup error"));

            // Check options
            Preconditions.checkNotNull(options.getDateTime(), "Required options.dateTime when RTP weight enabled");
            Preconditions.checkNotNull(options.getAliveWeightCountryLocationId(), "Required options.roundWeightCountryLocationId when RTP weight enabled");
            Preconditions.checkNotNull(options.getDefaultLandingDressingId(), "Required options.defaultLandingDressingId when RTP weight enabled");
            Preconditions.checkNotNull(options.getDefaultDiscardDressingId(), "Required options.defaultDiscardDressingId when RTP weight enabled");
            Preconditions.checkNotNull(options.getDefaultLandingPreservationId(), "Required options.defaultLandingPreservationId when RTP weight enabled");
            Preconditions.checkNotNull(options.getDefaultDiscardDressingId(), "Required options.defaultDiscardDressingId when RTP weight enabled");

        }

        long startTime = System.currentTimeMillis();
        final MutableShort flatRankOrder = new MutableShort(0);

        List<DenormalizedBatchVO> result = TreeNodeEntities.<BatchVO, DenormalizedBatchVO>streamAllAndMap(catchBatch, (source, p) -> {
                TempDenormalizedBatchVO target = createTempVO(source);
                TempDenormalizedBatchVO parent = p != null ? (TempDenormalizedBatchVO)p : null;
                boolean isLeaf = parent != null && source.isLeaf(); // Do not use 'target', because children are added later

                // Add to parent's children
                if (parent != null) parent.addChildren(target);

                // Copy parent's values
                computeInheritedValues(target, parent, options);

                // Compute Alive weight, on leaf batch
                // (to be able to compute indirect alive weight later)
                if (isLeaf && options.isEnableAliveWeight() && options.getAliveWeightCountryLocationId() != null) {
                    computeAliveWeightFactor(target, options, true)
                        .ifPresent(target::setAliveWeightFactor);
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
            .toList();

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

            // compute elevate factors
            computeElevateFactor(result, options);

            // Elevate weight
            computeElevatedValues(result, options);

            // Indirect elevated values
            computeIndirectElevatedValues(result, options);

        }

        // Log
        if (log.isDebugEnabled()) {
            log.debug("Batches denormalization succeed, in {}:\n{}",
                TimeUtils.printDurationFrom(startTime),
                DenormalizedBatches.dumpAsString(result, true, true));
            //log.debug("Batches denormalization succeed, in {}", TimeUtils.printDurationFrom(startTime));
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
        log.debug("Batches denormalization of operation {id: {}}...", operationId);

        // Compute options, for the operation's program
        if (options == null) {
            int programId = operationService.getProgramIdById(operationId);
            options = createOptionsByProgramId(programId);
        }

        // Denormalize batches
        List<DenormalizedBatchVO> denormalizedBatches = denormalize(catchBatch, options);

        // Save denormalized batches
        denormalizedBatches = denormalizedBatchRepository.saveAllByOperationId(operationId, denormalizedBatches);

        log.debug("Batches denormalization of operation {id: {}} [OK] in {}", operationId, TimeUtils.printDurationFrom(startTime));
        return denormalizedBatches;
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
        Integer[] taxonGroupIdsNoWeight = Arrays.stream(taxonGroupsNoWeight.split(","))
            .map(String::trim)
            .map(label -> taxonGroupService.findAllByFilter(ReferentialFilterVO.builder()
                .label(label)
                .levelIds(new Integer[]{TaxonGroupTypeEnum.FAO.getId()})
                .statusIds(new Integer[]{StatusEnum.ENABLE.getId()})
                .build()).stream().findFirst())
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(TaxonGroupVO::getId)
            .toArray(Integer[]::new);

        Integer roundWeightConversionCountryId = Programs.getPropertyAsInteger(program, ProgramPropertyEnum.TRIP_BATCH_ROUND_WEIGHT_CONVERSION_COUNTRY_ID);

        if (roundWeightConversionCountryId == null || roundWeightConversionCountryId < 0) {
            log.warn("Missing or invalid value for program property '{}'. Will not be able to compute alive weights, in batch denormalization!", ProgramPropertyEnum.TRIP_BATCH_ROUND_WEIGHT_CONVERSION_COUNTRY_ID.getKey());
        }

        return DenormalizedBatchOptions.builder()
            .taxonGroupIdsNoWeight(taxonGroupIdsNoWeight)
            .enableTaxonName(Programs.getPropertyAsBoolean(program, ProgramPropertyEnum.TRIP_BATCH_TAXON_NAME_ENABLE))
            .enableTaxonGroup(Programs.getPropertyAsBoolean(program, ProgramPropertyEnum.TRIP_BATCH_TAXON_GROUP_ENABLE))
            .enableRtpWeight(canEnableRtpWeight && Programs.getPropertyAsBoolean(program, ProgramPropertyEnum.TRIP_BATCH_LENGTH_WEIGHT_CONVERSION_ENABLE))
            .aliveWeightCountryLocationId(roundWeightConversionCountryId)
            .build();
    }

    protected void computeInheritedValues(TempDenormalizedBatchVO target,
                                          TempDenormalizedBatchVO parent,
                                          DenormalizedBatchOptions options) {
        // Special case for catch batch
        if (parent == null) {
            target.setTreeLevel((short) 1); // First level
            if (target.getIsLanding() == null) target.setIsLanding(false);
            if (target.getIsDiscard() == null) target.setIsDiscard(false);
            return;
        }

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

        // Inherit quality flag
        if (parent.getQualityFlagId() != null) {
            if (target.getQualityFlagId() == null) {
                target.setQualityFlagId(parent.getQualityFlagId());
            }
            // Keep the worst value, if current has a value
            else {
                target.setQualityFlagId(QualityFlags.worst(parent.getQualityFlagId(), target.getQualityFlagId()));
            }
        }

        // If current quality is invalid
        if (QualityFlags.isInvalid(target.getQualityFlagId())) {
            // Force both parent and current parent exhaustive inventory to FALSE
            // NOTE Allegro:
            //   mantis Allegro #12951 - remontée des poids selon le niveau de qualité
            //   Si un des lots fils (direct ou indirect) est invalide
            //   (c'est à dire si le code du niveau de qualité appartient à la liste des niveaux invalides)
            //   alors il faut considérer que l'inventaire exhaustif est non.
            //   Le but est de stopper la remontée des poids calculés
            //   s'il y a au moins un lot invalide parmi les fils, isExhaustive = false
            parent.setExhaustiveInventory(Boolean.FALSE);
            target.setExhaustiveInventory(Boolean.FALSE);
        }

        // Inherit sorting values
        Beans.getStream(parent.getSortingValues())
            .forEach(svSource -> {
                // Make sure sorting value not already exists
                Beans.getStream(target.getSortingValues())
                    .filter(svTarget -> Objects.equals(svTarget.getPmfmId(), svSource.getPmfmId()))
                    .findFirst()
                    .ifPresentOrElse(svTarget -> {
                            Beans.copyProperties(svSource, svTarget, IEntity.Fields.ID);
                            svTarget.setIsInherited(true);
                            svTarget.setRankOrder(svSource.getRankOrder() / 10);
                        },
                        () -> {
                            DenormalizedBatchSortingValueVO svTarget = new DenormalizedBatchSortingValueVO();
                            Beans.copyProperties(svSource, svTarget, IEntity.Fields.ID);
                            svTarget.setIsInherited(true);
                            svTarget.setRankOrder(svSource.getRankOrder() / 10);
                            target.addSortingValue(svTarget);
                        });
            });
    }

    protected void computeRtpWeights(List<DenormalizedBatchVO> batches, DenormalizedBatchOptions options) {
        // Select leafs
        List<TempDenormalizedBatchVO> leafBatches = batches.stream()
            .map(target -> (TempDenormalizedBatchVO) target)
            .filter(target -> !target.hasChildren())
            .toList();

        int maxRtpWeightDiffPct = options.getMaxRtpWeightDiffPct();
        log.debug("Computing RTP weights on leafs...");

        // For each leaf, try to compute a RTP weight
        leafBatches.forEach(batch -> {
            log.trace("- {}", batch.getLabel());

            computeRtpContextWeight(batch, options)
                .ifPresent(rtpContextWeight -> {
                    // Apply to the batch
                    batch.setRtpContextWeight(rtpContextWeight);

                    // Check diff with existing weight
                    if (batch.getWeight() != null && maxRtpWeightDiffPct > 0) {

                        // Compute diff between two weights
                        double errorPct = DenormalizedBatches.computeWeightDiffPercent(rtpContextWeight, batch.getWeight());

                        // If delta > max % => warn
                        if (errorPct > maxRtpWeightDiffPct) {

                            // Replace weight, if it was a RTP (should be wrong computation in the App ?)
                            if (Objects.equals(batch.getWeightMethodId(), MethodEnum.CALCULATED_WEIGHT_LENGTH.getId())) {
                                log.warn("Batch {} has a invalid RTP weight (computed: {}, actual: {}, delta: {}%). Fixing the RTP weight using the computed value.",
                                    batch.getLabel(),
                                    batch.getRtpContextWeight(),
                                    batch.getWeight(),
                                    errorPct
                                );
                                batch.setWeight(rtpContextWeight);
                            } else {
                                log.warn("Batch {} has a invalid weight (RTP: {}, actual: {} => delta: {}%)",
                                    batch.getLabel(),
                                    batch.getRtpContextWeight(),
                                    batch.getWeight(),
                                    errorPct
                                );
                            }
                        }
                    }
                });
            }
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
        int maxLoop = 1; // TODO check this
        do {
            changesCount.setValue(0);
            loopCounter.increment();
            log.debug("Computing indirect values (pass #{}) ...", loopCounter);

            // For each (leaf -> root)
            revertBatches.forEach(batch -> {
                boolean changed = false;

                log.trace("- {}", batch.getLabel());

                // Indirect context weight
                Double indirectContextWeight = computeIndirectContextWeight(batch, options);
                changed = changed || !Objects.equals(indirectContextWeight, batch.getIndirectContextWeight());
                batch.setIndirectContextWeight(indirectContextWeight);

                // Indirect RTP weight from length (if enabled)
                if (options.isEnableRtpWeight()) {
                    Double indirectRtpContextWeight = computeIndirectRtpContextWeight(batch, options);
                    changed = changed || !Objects.equals(indirectRtpContextWeight, batch.getIndirectRtpContextWeight());
                    batch.setIndirectRtpContextWeight(indirectRtpContextWeight);
                }

                // Indirect individual count
                BigDecimal indirectIndividualCount = computeIndirectIndividualCount(batch);
                changed = changed || !Objects.equals(indirectIndividualCount, batch.getIndirectIndividualCountDecimal());
                batch.setIndirectIndividualCountDecimal(indirectIndividualCount);

                // Compute alive weight factor
                if (options.isEnableAliveWeight() && options.getAliveWeightCountryLocationId() != null) {
                    if (batch.isLeaf()) {
                        Double aliveWeightFactor = computeAliveWeightFactor(batch, options, batch.isLeaf()).orElse(null);
                        changed = changed || !Objects.equals(aliveWeightFactor, batch.getAliveWeightFactor());
                        batch.setAliveWeightFactor(aliveWeightFactor);
                    } else {
                        Double aliveWeightFactor = computeIndirectAliveWeightFactor(batch, options);
                        changed = changed || !Objects.equals(aliveWeightFactor, batch.getAliveWeightFactor());
                        batch.setAliveWeightFactor(aliveWeightFactor);
                    }
                }
                else {
                    batch.setAliveWeightFactor(1d);
                }

                if (changed) changesCount.increment();
            });

            log.trace("Computing indirect values (pass #{}) [OK] - {} changes", loopCounter, changesCount);
        }

        // Continue while changes has been applied on tree
        while (changesCount.intValue() > 0 && loopCounter.intValue() < maxLoop);
    }

    /**
     * Compute elevation factors
     */
    protected void computeElevateFactor(List<DenormalizedBatchVO> batches, DenormalizedBatchOptions options) {
        log.debug("Computing elevation factors...");

        // For each (root -> leaf)
        batches.stream()
            .map(target -> (TempDenormalizedBatchVO) target)
            .forEach(target -> {
                TempDenormalizedBatchVO parent = (TempDenormalizedBatchVO) target.getParent();

                log.trace("{} {}", target.getTreeIndent(), target.getLabel());

                BigDecimal samplingFactor = target.getSamplingFactor() != null ? target.getSamplingFactor() : new BigDecimal(1);

                // Elevate context factor (=samplingFactor x parent value)
                BigDecimal elevateContextFactor = samplingFactor;
                if (parent != null) {
                    elevateContextFactor = elevateContextFactor.multiply(parent.getElevateContextFactor());
                }
                target.setElevateContextFactor(elevateContextFactor);

                // Taxon elevation factor (=samplingFactor x parent value - BUT not if parent has no taxonGroup/taxonName)
                if (target.hasTaxonGroup() || target.hasTaxonName()) {
                    BigDecimal taxonElevateFactor = samplingFactor;
                    // Apply parent factor (only if has taxonGroup)
                    if (parent != null && (parent.hasTaxonName() || parent.hasTaxonName())) {
                        taxonElevateFactor = taxonElevateFactor.multiply(parent.getTaxonElevateFactor());
                    }
                    target.setTaxonElevateFactor(taxonElevateFactor);
                }

                // Elevation factor (alive weight) = elevateContextFactor x aliveWeightFactor
                if (target.getAliveWeightFactor() != null) {
                    BigDecimal elevateFactor = elevateContextFactor.multiply(new BigDecimal(target.getAliveWeightFactor()));
                    target.setElevateFactor(elevateFactor);
                }
                else {
                    target.setElevateFactor(elevateContextFactor);
                }
            });
    }

    /**
     * Compute elevated values
     */
    protected void computeElevatedValues(List<DenormalizedBatchVO> batches, DenormalizedBatchOptions options) {
        MutableInt changesCount = new MutableInt(0);
        MutableInt loopCounter = new MutableInt(0);
        int maxLoop = 1; // TODO check this

        do {
            changesCount.setValue(0);
            loopCounter.increment();
            log.debug("Computing elevated values (pass #{}) ...", loopCounter);

            // For each (root -> leaf)
            batches.stream()
                .map(batch -> (TempDenormalizedBatchVO) batch)
                .forEach(batch -> {
                    boolean changed = false;
                    log.trace("{} {}", batch.getTreeIndent(), batch.getLabel());

                    // Base weight
                    BigDecimal contextWeight = Numbers.firstNotNullAsBigDecimal(batch.getWeight(), batch.getIndirectContextWeight());

                    if (contextWeight != null) {
                        // Elevate contextual weight
                        {
                            Double elevateContextWeight = contextWeight.multiply(batch.getElevateContextFactor())
                                .divide(new BigDecimal(1), WEIGHT_DECIMAL_SCALE, RoundingMode.HALF_UP)
                                .doubleValue();
                            changed = changed || !Objects.equals(elevateContextWeight, batch.getElevateContextWeight());
                            batch.setElevateContextWeight(elevateContextWeight);
                        }

                        // Taxon elevate context weight
                        if (batch.getTaxonElevateFactor() != null) {
                            Double taxonElevateContextWeight = contextWeight.multiply(batch.getTaxonElevateFactor()).doubleValue();
                            changed = changed || !Objects.equals(taxonElevateContextWeight, batch.getTaxonElevateContextWeight());
                            batch.setTaxonElevateContextWeight(taxonElevateContextWeight);
                        }

                        // Elevate weight (alive weight)
                        {
                            Double elevateWeight = contextWeight.multiply(batch.getElevateFactor())
                                .divide(new BigDecimal(1), WEIGHT_DECIMAL_SCALE, RoundingMode.HALF_UP)
                                .doubleValue();
                            changed = changed || !Objects.equals(elevateWeight, batch.getElevateWeight());
                            batch.setElevateWeight(elevateWeight);
                        }
                    }

                    if (options.isEnableRtpWeight()) {

                        BigDecimal rtpContextWeight = Numbers.firstNotNullAsBigDecimal(batch.getRtpContextWeight(), batch.getIndirectRtpContextWeight());
                        if (rtpContextWeight != null) {
                            // Elevate RTP context weight
                            {
                                Double elevateRtpContextWeight = rtpContextWeight.multiply(batch.getElevateContextFactor())
                                    .divide(new BigDecimal(1), WEIGHT_DECIMAL_SCALE, RoundingMode.HALF_UP)
                                    .doubleValue();
                                changed = changed || !Objects.equals(elevateRtpContextWeight, batch.getElevateRtpContextWeight());
                                batch.setElevateRtpContextWeight(elevateRtpContextWeight);
                            }

                            // Indirect RTP weight (from indirect RTP context weight, converted to alive)
                            {
                                BigDecimal aliveWeightFactor = Numbers.firstNotNullAsBigDecimal(batch.getAliveWeightFactor(), new BigDecimal(1));
                                Double indirectRtpWeight = rtpContextWeight.multiply(aliveWeightFactor)
                                    .divide(new BigDecimal(1), WEIGHT_DECIMAL_SCALE, RoundingMode.HALF_UP)
                                    .doubleValue();
                                changed = changed || !Objects.equals(indirectRtpWeight, batch.getIndirectRtpWeight());
                                batch.setIndirectRtpWeight(indirectRtpWeight);
                            }

                            // Elevate RTP weight
                            {
                                Double elevateRtpWeight = rtpContextWeight.multiply(batch.getElevateFactor())
                                    .divide(new BigDecimal(1), WEIGHT_DECIMAL_SCALE, RoundingMode.HALF_UP)
                                    .doubleValue();
                                changed = changed || !Objects.equals(elevateRtpWeight, batch.getElevateRtpWeight());
                                batch.setElevateRtpWeight(elevateRtpWeight);
                            }
                        }
                    }

                    BigDecimal individualCount = batch.getIndividualCount() != null ? new BigDecimal(batch.getIndividualCount()) : batch.getIndirectIndividualCountDecimal();
                    if (individualCount != null) {
                        // Taxon elevate individual count
                        if (batch.getTaxonElevateContextWeight() != null && batch.getTaxonElevateFactor() != null) {
                            Integer taxonElevateIndividualCount = individualCount.multiply(batch.getTaxonElevateFactor())
                                .divide(new BigDecimal(1), 0, RoundingMode.HALF_UP) // Round to half up
                                .intValue();
                            changed = changed || !Objects.equals(taxonElevateIndividualCount, batch.getTaxonElevateIndividualCount());
                            batch.setTaxonElevateIndividualCount(taxonElevateIndividualCount);
                        }

                        // Elevate individual count
                        Integer elevateIndividualCount = individualCount.multiply(batch.getElevateFactor())
                            .divide(new BigDecimal(1), 0, RoundingMode.HALF_UP) // Round to half up
                            .intValue();
                        changed = changed || !Objects.equals(elevateIndividualCount, batch.getElevateIndividualCount());
                        batch.setElevateIndividualCount(elevateIndividualCount);

                        // Set indirect individualCount
                        if (batch.getIndirectIndividualCountDecimal() != null) {
                            batch.setIndirectIndividualCount(batch.getIndirectIndividualCountDecimal()
                                .divide(new BigDecimal(1), 0, RoundingMode.HALF_UP) // Round to half up
                                .intValue());
                        }
                    }

                    if (changed) {
                        //log.trace("{} {} - changes!", target.getTreeIndent(), target.getLabel());
                        changesCount.increment();
                    }
                });

            log.trace("Computing elevated values (pass #{}) [OK] - {} changes", loopCounter, changesCount);
        } while (changesCount.intValue() > 0 && loopCounter.intValue() < maxLoop);
    }

    protected void computeIndirectElevatedValues(List<DenormalizedBatchVO> batches, DenormalizedBatchOptions options) {

        List<TempDenormalizedBatchVO> revertBatches = batches.stream()
            .map(target -> (TempDenormalizedBatchVO) target)
            // Reverse order (start from leaf)
            .sorted(Collections.reverseOrder(Comparator.comparing(DenormalizedBatchVO::getFlatRankOrder, Short::compareTo)))
            .toList();

        MutableInt changesCount = new MutableInt(0);
        MutableInt loopCounter = new MutableInt(0);
        int maxLoop = 1; // TODO check this value

        do {
            changesCount.setValue(0);
            loopCounter.increment();
            log.debug("Computing indirect elevated values (pass #{}) ...", loopCounter);

            // For each (leaf -> root)
            revertBatches
                .forEach(batch -> {
                boolean changed = false;

                log.trace("- {}", batch.getLabel());

                if (batch.getElevateWeight() == null) {
                    // No context weight to elevate: so use children elevate weight
                    Double indirectElevateWeight = computeIndirectElevateWeight(batch, options);
                    changed = changed || !Objects.equals(indirectElevateWeight, batch.getIndirectElevateWeight())
                        || !Objects.equals(indirectElevateWeight, batch.getElevateWeight());
                    batch.setIndirectElevateWeight(indirectElevateWeight);
                    batch.setElevateWeight(indirectElevateWeight);
                }

                if (options.isEnableRtpWeight() && batch.getElevateRtpWeight() == null) {
                    Double indirectElevateRtpWeight = computeIndirectElevateRtpWeight(batch, options);
                    changed = changed || !Objects.equals(indirectElevateRtpWeight, batch.getIndirectRtpElevateWeight())
                        || !Objects.equals(indirectElevateRtpWeight, batch.getElevateRtpWeight());
                    batch.setIndirectRtpElevateWeight(indirectElevateRtpWeight);
                    batch.setElevateRtpWeight(indirectElevateRtpWeight);
                }

                // Check weight = 0 AND individual
                boolean zeroWeightWithIndividual = batch.getElevateWeight() != null && batch.getElevateWeight() == 0d
                    && batch.getElevateIndividualCount() != null && batch.getElevateIndividualCount() > 0;
                if (zeroWeightWithIndividual) {
                    String message = String.format("Invalid batch {id: %s, label: '%s'}: elevateWeight=0 but elevateIndividualCount > 0",
                        batch.getId(), batch.getLabel());
                    if (options.isAllowZeroWeightWithIndividual()) log.warn(message);
                    else throw new InvalidSamplingBatchException(message);
                }

                if (changed) {
                    //log.trace("{} {} - changes!", target.getTreeIndent(), target.getLabel());
                    changesCount.increment();
                }
            });

            log.trace("Computing indirect elevated values (pass #{}) [OK] - {} changes", loopCounter, changesCount);
        } while (changesCount.intValue() > 0 && loopCounter.intValue() < maxLoop);
    }

    protected Optional<Double> computeRtpContextWeight(TempDenormalizedBatchVO batch, DenormalizedBatchOptions options) {
        // Already computed: skip
        if (batch.getRtpContextWeight() != null) return Optional.of(batch.getRtpContextWeight());

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
                        .statusIds(new Integer[]{StatusEnum.ENABLE.getId()})
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
            .flatMap(rtpAliveWeight -> convertAliveWeightToContext(batch, options, rtpAliveWeight))
            .map(BigDecimal::doubleValue);
    }

    protected Double computeIndirectContextWeight(TempDenormalizedBatchVO batch,
                                                  DenormalizedBatchOptions options) {
        return computeIndirectWeight(batch, options,
            DenormalizedBatchVO::getWeight,
            DenormalizedBatchVO::getIndirectContextWeight,
            true,
            TempDenormalizedBatchVO::getAliveWeightFactor
        );
    }

    protected Double computeIndirectRtpContextWeight(TempDenormalizedBatchVO batch,
                                                     DenormalizedBatchOptions options) {
        return computeIndirectWeight(batch, options,
            TempDenormalizedBatchVO::getRtpContextWeight,
            TempDenormalizedBatchVO::getIndirectRtpContextWeight,
            true,
            TempDenormalizedBatchVO::getAliveWeightFactor
        );
    }

    protected Double computeIndirectElevateWeight(TempDenormalizedBatchVO batch,
                                                  DenormalizedBatchOptions options) {
        return computeIndirectWeight(batch, options,
            TempDenormalizedBatchVO::getElevateWeight,
            TempDenormalizedBatchVO::getIndirectElevateWeight,
            false,
            null // Skip control on same dressing/preservation
        );
    }

    protected Double computeIndirectElevateRtpWeight(TempDenormalizedBatchVO batch,
                                                     DenormalizedBatchOptions options) {
        return computeIndirectWeight(batch, options,
            TempDenormalizedBatchVO::getElevateRtpWeight,
            TempDenormalizedBatchVO::getIndirectRtpElevateWeight,
            false,
            null // Skip control on same dressing/preservation
        );
    }

    protected Double computeIndirectWeight(TempDenormalizedBatchVO batch,
                                           DenormalizedBatchOptions options,
                                           Function<TempDenormalizedBatchVO, Double> weightGetter,
                                           Function<TempDenormalizedBatchVO, Double> indirectWeightGetter,
                                           boolean applySamplingRatio,
                                           @Nullable Function<TempDenormalizedBatchVO, Double> aliveWeightFactorGetter) {
        // Already computed: skip
        Double indirectWeight = indirectWeightGetter.apply(batch);
        if (indirectWeight != null) return indirectWeight;

        if (applySamplingRatio) {
            // Sampling batch
            if (DenormalizedBatches.isSamplingBatch(batch)) {
                try {
                    Double samplingWeight = computeSamplingWeightAndRatio(batch, false,
                        options, weightGetter, indirectWeightGetter, aliveWeightFactorGetter);
                    if (samplingWeight != null) return samplingWeight;
                } catch (InvalidSamplingBatchException e) {
                    // May be not a sampling batch ? (e.g. a species batch)
                    indirectWeight = computeSumChildrenWeight(batch, options,
                        weightGetter, indirectWeightGetter, true, aliveWeightFactorGetter);
                    if (indirectWeight != null) return indirectWeight;
                    throw e;
                }
                // Invalid sampling batch: Continue if not set
            }

            // Child batch is a sampling batch
            if (DenormalizedBatches.isParentOfSamplingBatch(batch)) {
                return computeParentSamplingWeight(batch, false, weightGetter, indirectWeightGetter);
            }
        }

        // Has children (not a leaf batch)
        if (batch.hasChildren()) {
            // Compute sum from children's weight
            return computeSumChildrenWeight(batch, options, weightGetter, indirectWeightGetter, applySamplingRatio, aliveWeightFactorGetter);
        }
        // Leaf batch: use the current weight as default
        else {
            return weightGetter.apply(batch);
        }
    }

    protected Double computeSamplingWeightAndRatio(TempDenormalizedBatchVO batch,
                                                   boolean checkArgument,
                                                   DenormalizedBatchOptions options,
                                                   Function<TempDenormalizedBatchVO, Double> weightGetter,
                                                   Function<TempDenormalizedBatchVO, Double> indirectWeightGetter,
                                                   Function<TempDenormalizedBatchVO, Double> aliveWeightFactorGetter) {
        if (checkArgument) Preconditions.checkArgument(DenormalizedBatches.isSamplingBatch(batch));

        TempDenormalizedBatchVO parent = (TempDenormalizedBatchVO) batch.getParent();
        boolean parentExhaustiveInventory = DenormalizedBatches.isExhaustiveInventory(parent);
        Double parentWeight = weightGetter.apply(parent);
        Double weight = weightGetter.apply(batch);
        Double samplingWeight = null;
        Double samplingRatio = batch.getSamplingRatio();
        BigDecimal samplingFactor = null;
        final int scale = INTERMEDIATE_DECIMAL_SCALE;

        // Ignore invalid value
        if (samplingRatio != null && (Double.isNaN(samplingRatio) || Double.isInfinite(batch.getSamplingRatio()))) {
            samplingRatio = null;
        }

        if (samplingRatio != null) {
            samplingFactor = samplingRatio <= 0
                ? new BigDecimal(0)
                : new BigDecimal(1).divide(new BigDecimal(samplingRatio), scale, RoundingMode.HALF_UP);

            // Try to restore sampling ratio from text (more accuracy)
            if (samplingRatio > 0 && StringUtils.isNotBlank(batch.getSamplingRatioText()) && batch.getSamplingRatioText().contains("/")) {
                String[] parts = batch.getSamplingRatioText().split("/", 2);
                try {
                    BigDecimal shouldBeSamplingWeight = new BigDecimal(parts[0]);
                    BigDecimal shouldBeParentWeight = new BigDecimal(parts[1]);
                    samplingRatio = shouldBeParentWeight.doubleValue() <= 0
                        ? 0d
                        : shouldBeSamplingWeight.divide(shouldBeParentWeight, scale, RoundingMode.HALF_UP).doubleValue();
                    samplingFactor = shouldBeSamplingWeight.doubleValue() <= 0
                        ? new BigDecimal(0)
                        : shouldBeParentWeight.divide(shouldBeSamplingWeight, scale, RoundingMode.HALF_UP);
                } catch (Exception e) {
                    log.warn("Cannot parse samplingRatioText on batch {id: {}, label: '{}', saplingRatioText: '{}'} : {}",
                        batch.getId(),
                        batch.getLabel(),
                        batch.getSamplingRatioText(),
                        e.getMessage());
                }
            }
        } else if (parentExhaustiveInventory && parentWeight != null && weight != null) {
            if (weight > parentWeight) {
                throw new InvalidSamplingBatchException(String.format("Invalid batch weight {id: %s, label: '%s', weight: %s}. Should be <= %s kg (parent weight)",
                    batch.getId(), batch.getLabel(), weight,
                    parentWeight));
            }
            if (parentWeight <= 0) {
                samplingRatio = 0d;
                samplingFactor = new BigDecimal(0);
            }
            else {
                samplingRatio = new BigDecimal(weight)
                    .divide(new BigDecimal(parentWeight), scale, RoundingMode.HALF_UP)
                    .doubleValue();
                samplingFactor = weight == 0d
                    ? new BigDecimal(0)
                    : new BigDecimal(parentWeight).divide(new BigDecimal(weight), scale, RoundingMode.HALF_UP);
            }
        } else if (parentExhaustiveInventory && parentWeight != null && batch.hasChildren()) {
            samplingWeight = computeSumChildrenWeight(batch, options, weightGetter, indirectWeightGetter, true, aliveWeightFactorGetter);
            if (samplingWeight != null) {
                if (parentWeight <= 0d || samplingWeight <= 0d) {
                    samplingRatio = 0d;
                    samplingFactor = new BigDecimal(0);
                }
                else {
                    samplingRatio = new BigDecimal(samplingWeight).divide(new BigDecimal(parentWeight), scale, RoundingMode.HALF_UP).doubleValue();
                    samplingFactor = new BigDecimal(parentWeight).divide(new BigDecimal(samplingWeight), scale, RoundingMode.HALF_UP);
                }
            }
        } else if ((!parentExhaustiveInventory || parentWeight == null) && batch.hasChildren()) {
            samplingWeight = computeSumChildrenWeight(batch, options, weightGetter, indirectWeightGetter, true, aliveWeightFactorGetter);
            if (samplingWeight != null) {
                samplingRatio = 1d;
                samplingFactor = new BigDecimal(1);
            }
        }

        // When taxon group without weight: compute the simpling ratio by individual count
        else if (parent.getTaxonGroupId() != null
            && ArrayUtils.isNotEmpty(options.getTaxonGroupIdsNoWeight())
            && ArrayUtils.contains(options.getTaxonGroupIdsNoWeight(), parent.getInheritedTaxonGroup().getId())) {
            // TODO
            log.warn("Batch {label: '{}'} - TODO try to compute samplingRatio using individualCount parent/child (taxon group no weight)", batch.getLabel());
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
            if (weight != null) {
                samplingWeight = weight;
            } else if (parentExhaustiveInventory) {
                if (parentWeight != null) {
                    samplingWeight = new BigDecimal(parentWeight).multiply(new BigDecimal(samplingRatio))
                        .divide(new BigDecimal(1), WEIGHT_DECIMAL_SCALE, RoundingMode.HALF_UP)
                        .doubleValue();
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
        int scale = INTERMEDIATE_DECIMAL_SCALE;

        Double samplingRatio = null;
        BigDecimal samplingFactor = null;
        if (samplingBatch.getSamplingRatio() != null) {
            samplingRatio = samplingBatch.getSamplingRatio();
            samplingFactor = samplingRatio <= 0d
                ? new BigDecimal(0)
                : new BigDecimal(1).divide(new BigDecimal(samplingRatio), scale, RoundingMode.HALF_UP);

            // Try to use the sampling ratio text (more accuracy)
            if (samplingRatio > 0 && StringUtils.isNotBlank(samplingBatch.getSamplingRatioText()) && samplingBatch.getSamplingRatioText().contains("/")) {
                String[] parts = samplingBatch.getSamplingRatioText().split("/", 2);
                try {
                    BigDecimal shouldBeSamplingWeight = new BigDecimal(parts[0]);
                    BigDecimal shouldBeParentWeight = new BigDecimal(parts[1]);
                    // If ratio text use the sampling weight, we have the parent weight
                    if (Objects.equals(shouldBeSamplingWeight, samplingWeight)
                        || Objects.equals(shouldBeSamplingWeight, samplingIndirectWeight)) {
                        parentWeight = shouldBeParentWeight.doubleValue();
                    }
                    samplingRatio = shouldBeParentWeight.doubleValue() <= 0d
                        ? 0
                        : shouldBeSamplingWeight.divide(shouldBeParentWeight, scale, RoundingMode.HALF_UP).doubleValue();
                    samplingFactor = shouldBeSamplingWeight.doubleValue() <= 0
                        ? new BigDecimal(0)
                        : shouldBeParentWeight.divide(shouldBeSamplingWeight, scale, RoundingMode.HALF_UP);
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
                parentWeight = new BigDecimal(samplingWeight).multiply(samplingFactor)
                    .divide(new BigDecimal(1), WEIGHT_DECIMAL_SCALE, RoundingMode.HALF_UP)
                    .doubleValue();
            } else if (samplingIndirectWeight != null) {
                parentWeight = new BigDecimal(samplingIndirectWeight).multiply(samplingFactor)
                    .divide(new BigDecimal(1), WEIGHT_DECIMAL_SCALE, RoundingMode.HALF_UP)
                    .doubleValue();
            }
        }

        return parentWeight;
    }

    protected Double computeSumChildrenWeight(@NonNull DenormalizedBatchVO batch,
                                              @NonNull DenormalizedBatchOptions options,
                                              @NonNull Function<TempDenormalizedBatchVO, Double> weightGetter,
                                              @NonNull Function<TempDenormalizedBatchVO, Double> indirectWeightGetter,
                                              boolean applySamplingRatio,
                                              @Nullable Function<TempDenormalizedBatchVO, Double> aliveWeightFactorGetter) {
        // Cannot compute children sum, when:
        // - Not exhaustive inventory
        // - No children
        if (!DenormalizedBatches.isExhaustiveInventory(batch)
            || !batch.hasChildren()) {
            return null;
        }

        // We track children alive weight factor. If not SAME => cannot compute sum
        MutableDouble childrenAliveWeightFactor = new MutableDouble(-1d);

        try {
            return Beans.getStream(batch.getChildren())
                .map(child -> (TempDenormalizedBatchVO) child)
                .mapToDouble(child -> {
                    if (aliveWeightFactorGetter != null) {
                        Double aliveWeightFactor = aliveWeightFactorGetter.apply(child);
                        if (aliveWeightFactor == null) aliveWeightFactor = 1d;
                        // Not set: update and continue
                        if (childrenAliveWeightFactor.doubleValue() == -1d) {
                            childrenAliveWeightFactor.setValue(aliveWeightFactor);
                        }
                        // Control that all children have alive weight factor. If not, we cannot compute sum.
                        else if (!Objects.equals(childrenAliveWeightFactor.getValue(), aliveWeightFactor)) {
                            // Stop here, because we cannot sum all children's weight
                            throw new SumarisTechnicalException(String.format("No indirect weight"
                                + " (a child has a different dressing/preservation: {id: %s, label: '%s'})", child.getId(), child.getLabel()));
                        }
                    }
                    // Use child weight, if any
                    Double weight = weightGetter.apply(child);
                    if (weight != null) return weight;

                    // Compute indirect weight
                    Double indirectWeight = computeIndirectWeight(child, options, weightGetter, indirectWeightGetter, applySamplingRatio, aliveWeightFactorGetter);
                    if (indirectWeight != null) return indirectWeight;

                    // Stop here, because we cannot sum all children's weight
                    throw new SumarisTechnicalException(String.format("No indirect weight"
                        + " (a child has no weight {id: %s, label: '%s'})", child.getId(), child.getLabel()));
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
                    throw new SumarisTechnicalException(String.format("No indirect individual count,"
                        + " (some child batch has no individual count {id: %s, label: '%s'})", child.getId(), child.getLabel()));
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
     * @param options
     * @param aliveWeight
     * @return
     */
    protected Optional<BigDecimal> convertAliveWeightToContext(TempDenormalizedBatchVO batch,
                                                               DenormalizedBatchOptions options,
                                                               BigDecimal aliveWeight) {
        // Apply inverse conversion (alive weight / conversion factor)
        return computeAliveWeightFactor(batch, options, batch.isLeaf())
            .map(conversionFactor -> {

                // Check conversion is not negative or zero (should never occur)
                if (conversionFactor <= 0) throw new SumarisTechnicalException("Invalid round weight conversion. Coefficient should be > 0");

                // No conversion: skip
                if (conversionFactor == 1d) return aliveWeight;

                // Apply inverse conversion
                return aliveWeight.divide(new BigDecimal(conversionFactor), WEIGHT_DECIMAL_SCALE, RoundingMode.HALF_UP);
            });
    }

    protected Optional<Double> computeAliveWeightFactor(TempDenormalizedBatchVO batch,
                                                        DenormalizedBatchOptions options,
                                                        boolean applyDressingAndPreservationDefaults) {

        if (batch.getAliveWeightFactor() != null) return Optional.of(batch.getAliveWeightFactor());

        Integer taxonGroupId = batch.getTaxonGroupId();
        if (taxonGroupId == null) return Optional.empty();

        // No weight for this species:
        if (ArrayUtils.isNotEmpty(options.getTaxonGroupIdsNoWeight())
            && ArrayUtils.contains(options.getTaxonGroupIdsNoWeight(), taxonGroupId)) {
            return Optional.empty();
        }

        // Get dressing, or 'WHL - Whole' by default
        Integer dressingId = DenormalizedBatches.getDressingId(batch)
            .orElseGet(() -> {
                // Apply defaults
                if (applyDressingAndPreservationDefaults) {
                    return Boolean.TRUE.equals(batch.getIsLanding())
                        ? options.getDefaultLandingDressingId()
                        : Boolean.TRUE.equals(batch.getIsDiscard())
                            ? options.getDefaultDiscardDressingId()
                            : QualitativeValueEnum.DRESSING_WHOLE.getId();
                }
                return null;
            });

        // Get preservation, or 'FRE - Fresh' by default
        Integer preservationId = DenormalizedBatches.getPreservationId(batch)
            .orElseGet(() -> {
                // Apply default preservation
                if (applyDressingAndPreservationDefaults) {
                    return Boolean.TRUE.equals(batch.getIsLanding())
                        ? options.getDefaultLandingPreservationId()
                        : Boolean.TRUE.equals(batch.getIsDiscard())
                            ? options.getDefaultDiscardPreservationId()
                            : QualitativeValueEnum.PRESERVATION_FRESH.getId();
                }
                return null;
            });

        // Skip (e.g. if enableDefaultsDressingAndPreservation is 'false')
        if (dressingId == null || preservationId == null) return Optional.empty();

        // Find the best conversion coefficient
        Optional<RoundWeightConversionVO> conversion = roundWeightConversionService.findFirstByFilter(RoundWeightConversionFilterVO.builder()
            .taxonGroupIds(new Integer[]{taxonGroupId})
            .dressingIds(new Integer[]{dressingId})
            .preservingIds(new Integer[]{preservationId})
            .locationIds(new Integer[]{options.getAliveWeightCountryLocationId()})
            .date(options.getDay())
            .statusIds(new Integer[]{StatusEnum.ENABLE.getId()})
            .build());

        if (conversion.isEmpty()) {
            log.warn("No RoundWeightConversion found for {taxonGroupId: {}, dressingId: {}, preservationId: {}, locationId: {}}",
                taxonGroupId,
                dressingId,
                preservationId,
                options.getAliveWeightCountryLocationId());
        }


        return conversion.map(RoundWeightConversionVO::getConversionCoefficient);
    }

    protected Double computeIndirectAliveWeightFactor(TempDenormalizedBatchVO batch, DenormalizedBatchOptions options) {
        if (batch.getAliveWeightFactor() != null) return batch.getAliveWeightFactor();

        // No taxon group, or no child => no indirect value
        if (!batch.hasTaxonGroup() || !batch.hasChildren() || !DenormalizedBatches.isExhaustiveInventory(batch)) return null;

        // Collect all children factors
        List<Double> childAliveFactors = batch.getChildren()
            .stream()
            .map(child -> computeIndirectAliveWeightFactor((TempDenormalizedBatchVO) child, options))
            .toList();

        // Peek the first value (should be not null)
        Double firstAliveFactor = childAliveFactors.get(0);
        if (firstAliveFactor == null) return null;

        // Check same value on each child. If not: return empty (cannot compute indirect value)
        boolean alwaysSameValue = childAliveFactors.size() == 1 || !childAliveFactors.stream().anyMatch(value -> !firstAliveFactor.equals(value));
        if (!alwaysSameValue) {
            log.trace("No indirect alive weight. No unique value found in children");
            return null;
        }

        return firstAliveFactor;
    }

    private void checkBaseEnumerations() {
        EntityEnums.checkResolved(
            QualitativeValueEnum.LANDING,
            QualitativeValueEnum.DISCARD
        );
    }
    private void checkRtpEnumerations() {
        EntityEnums.checkResolved(
            ParameterEnum.SEX,
            QualitativeValueEnum.SEX_UNSEXED,
            QualitativeValueEnum.DRESSING_WHOLE,
            QualitativeValueEnum.DRESSING_GUTTED,
            QualitativeValueEnum.PRESERVATION_FRESH);
    }
}

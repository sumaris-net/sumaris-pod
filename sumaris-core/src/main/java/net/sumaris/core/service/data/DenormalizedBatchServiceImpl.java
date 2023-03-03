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
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.config.SumarisConfigurationOption;
import net.sumaris.core.dao.data.batch.BatchRepository;
import net.sumaris.core.dao.data.batch.DenormalizedBatchRepository;
import net.sumaris.core.dao.data.batch.InvalidSamplingBatchException;
import net.sumaris.core.model.TreeNodeEntities;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.model.referential.QualityFlagEnum;
import net.sumaris.core.model.referential.StatusEnum;
import net.sumaris.core.model.referential.taxon.TaxonGroupTypeEnum;
import net.sumaris.core.service.administration.programStrategy.ProgramService;
import net.sumaris.core.service.referential.taxon.TaxonGroupService;
import net.sumaris.core.util.Beans;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.util.TimeUtils;
import net.sumaris.core.vo.administration.programStrategy.ProgramFetchOptions;
import net.sumaris.core.vo.administration.programStrategy.ProgramVO;
import net.sumaris.core.vo.administration.programStrategy.Programs;
import net.sumaris.core.vo.data.batch.*;
import net.sumaris.core.vo.filter.ReferentialFilterVO;
import net.sumaris.core.vo.referential.TaxonGroupVO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.mutable.MutableShort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service("denormalizedBatchService")
@Slf4j
public class DenormalizedBatchServiceImpl implements DenormalizedBatchService {

	@Autowired
	protected DenormalizedBatchRepository denormalizedBatchRepository;

	@Autowired
	protected BatchRepository batchRepository;

	@Autowired
	protected ProgramService programService;

	@Autowired
	protected OperationService operationService;

	@Autowired
	protected SaleService saleService;

	@Autowired
	protected TaxonGroupService taxonGroupService;

	@Override
	public List<DenormalizedBatchVO> denormalize(@NonNull BatchVO catchBatch, @NonNull final DenormalizedBatchOptions options) {

		boolean trace = log.isTraceEnabled();
		long startTime = System.currentTimeMillis();
		final MutableShort flatRankOrder = new MutableShort(0);

		List<DenormalizedBatchVO> result = TreeNodeEntities.<BatchVO, DenormalizedBatchVO>streamAllAndMap(catchBatch, (source, parent) -> {
			TempDenormalizedBatchVO target = createTempVO(source);

			// Add to parent's children
			if (parent != null) parent.addChildren(target);

			// Depth level
			if (parent == null) {
				target.setTreeLevel((short)1); // First level
				if (target.getIsLanding() == null) target.setIsLanding(false);
				if (target.getIsDiscard() == null) target.setIsDiscard(false);
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
				if (target.getExhaustiveInventory() == null) {
					// Always true, when:
					// - taxon name is defined
					// - taxon group is defined and taxon Name disable (in options)
					if (target.getInheritedTaxonName() != null) {
						target.setExhaustiveInventory(Boolean.TRUE);
					}
					else if (target.getInheritedTaxonGroup() != null && !options.isEnableTaxonName()) {
						target.setExhaustiveInventory(Boolean.TRUE);
					}
					else if (parent.getExhaustiveInventory() != null) {
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

		if (CollectionUtils.size(result) == 1) {
			DenormalizedBatchVO target = result.get(0);
			target.setElevateWeight(target.getWeight());
		}
		else {
			// Compute indirect values
			computeIndirectValues(result);

			// Elevate weight
			computeElevatedValues(result);
		}

		// Log
		if (trace) {
			log.trace("Successfully denormalized batches, in {}:\n{}",
				TimeUtils.printDurationFrom(startTime),
				DenormalizedBatches.dumpAsString(result, true, true));
		}
		else {
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

		String taxonGroupsNoWeight = Programs.getProperty(program, SumarisConfigurationOption.BATCH_TAXON_GROUP_LABELS_NO_WEIGHT);
		List<Integer> taxonGroupIdsNoWeight = Arrays.stream(taxonGroupsNoWeight.split(","))
				.map(String::trim)
				.map(label -> taxonGroupService.findAllByFilter(ReferentialFilterVO.builder()
						.label(label)
						.levelIds(new Integer[]{TaxonGroupTypeEnum.FAO.getId()})
						.statusIds(new Integer[]{ StatusEnum.ENABLE.getId() })
						.build()).stream().findFirst().orElse(null))
				.filter(Objects::nonNull)
				.map(TaxonGroupVO::getId)
				.collect(Collectors.toList());

		return DenormalizedBatchOptions.builder()
				.enableTaxonName(Programs.getPropertyAsBoolean(program, SumarisConfigurationOption.ENABLE_BATCH_TAXON_NAME))
				.enableTaxonGroup(Programs.getPropertyAsBoolean(program, SumarisConfigurationOption.ENABLE_BATCH_TAXON_GROUP))
				.taxonGroupIdsNoWeight(taxonGroupIdsNoWeight)
				.build();
	}

	protected void computeIndirectValues(List<DenormalizedBatchVO> batches) {

		List<TempDenormalizedBatchVO> revertBatches = batches.stream()
			.map(target -> (TempDenormalizedBatchVO)target)
			// Reverse order (start from leaf)
			.sorted(Collections.reverseOrder(Comparator.comparing(DenormalizedBatchVO::getFlatRankOrder)))
			.collect(Collectors.toList());

		// Compute indirect values (from children to parent)
		MutableInt changesCount = new MutableInt(0);
		MutableInt loopCounter = new MutableInt(0);
		do {
			changesCount.setValue(0);
			loopCounter.increment();
			log.debug("Computing indirect values (pass #{}) ...", loopCounter);

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
	protected void computeElevatedValues(List<DenormalizedBatchVO> batches) {
		MutableInt changesCount = new MutableInt(0);
		MutableInt loopCounter = new MutableInt(0);

		do {
			changesCount.setValue(0);
			loopCounter.increment();
			log.debug("Computing elevated values (pass #{}) ...", loopCounter);

			batches.stream()
				.map(target -> (TempDenormalizedBatchVO) target)
				.forEach(target -> {
					boolean changed = false;

					log.trace("{} {}", target.getTreeIndent(), target.getLabel());
					BigDecimal elevateFactor = target.getElevateFactor();
					if (elevateFactor == null) {
						elevateFactor = new BigDecimal(1);
						if (target.getParent() != null) {
							elevateFactor = elevateFactor.multiply(((TempDenormalizedBatchVO) target.getParent()).getElevateFactor());
						}
					}
					// Remember it, for children
					target.setElevateFactor(elevateFactor);

					Double weight = target.getWeight() != null ? target.getWeight() : target.getIndirectWeight();
					if (weight != null) {
						Double elevateWeight = elevateFactor.multiply(new BigDecimal(weight)).doubleValue();
						changed = changed || !Objects.equals(elevateWeight, target.getElevateWeight());
						target.setElevateWeight(elevateWeight);
					}

					Integer individualCount = target.getIndividualCount() != null ? target.getIndividualCount() : target.getIndirectIndividualCount();
					if (individualCount != null) {
						Integer elevateIndividualCount = new BigDecimal(individualCount).multiply(elevateFactor).intValue();
						changed = changed || !Objects.equals(elevateIndividualCount, target.getElevateIndividualCount());
						target.setElevateIndividualCount(elevateIndividualCount);
					}

					if (changed) changesCount.increment();
				});

			log.trace("Computing elevated values (pass #{}) [OK] - {} changes", loopCounter, changesCount);
		} while (changesCount.intValue() > 0);

	}

	protected Double computeIndirectWeight(TempDenormalizedBatchVO batch) {
		// Already computed: skip
		if (batch.getIndirectWeight() != null) return batch.getIndirectWeight();

		// Sampling batch
		if (DenormalizedBatches.isSamplingBatch(batch)) {
			try {
				Double samplingWeight = computeSamplingWeightAndRatio(batch, false);
				if (samplingWeight != null) return samplingWeight;
			}
			catch (InvalidSamplingBatchException e) {
				// May be not a sampling batch ? (e.g. a species batch)
				Double indirectWeight = computeSumChildrenWeight(batch);
				if (indirectWeight != null) return indirectWeight;
				throw e;
			}
			// Invalid sampling batch: Continue if not set
		}

		// Child batch is a sampling batch
		if (DenormalizedBatches.isParentOfSamplingBatch(batch)) {
			return computeParentSamplingWeight(batch, false);
		}

		Double indirectWeight = computeSumChildrenWeight(batch);
		return indirectWeight;
	}


	protected Double computeSamplingWeightAndRatio(TempDenormalizedBatchVO batch, boolean checkArgument) {
		if (checkArgument)
			Preconditions.checkArgument(DenormalizedBatches.isSamplingBatch(batch));

		DenormalizedBatchVO parent = batch.getParent();
		boolean parentExhaustiveInventory = DenormalizedBatches.isExhaustiveInventory(parent);
		Double samplingWeight = null;
		Double samplingRatio = null;
		BigDecimal elevateFactor = null;

		if (batch.getSamplingRatio() != null) {
			samplingRatio = batch.getSamplingRatio();
			elevateFactor = new BigDecimal(1).divide(new BigDecimal(samplingRatio));

			// Try to use the sampling ratio text (more accuracy)
			if (StringUtils.isNotBlank(batch.getSamplingRatioText()) && batch.getSamplingRatioText().contains("/")) {
				String[] parts = batch.getSamplingRatioText().split("/", 2);
				try {
					double d0 = Double.parseDouble(parts[0]);
					double d1 = Double.parseDouble(parts[1]);
					samplingRatio = d0 / d1;
					elevateFactor = new BigDecimal(d1).divide(new BigDecimal(d0));
				} catch (Exception e) {
					log.warn("Cannot parse samplingRatioText on batch {id: {}}, label: '{}', saplingRatioText: '{}'} : {}",
						batch.getId(),
						batch.getLabel(),
						batch.getSamplingRatioText(),
						e.getMessage());
				}
			}
		}
		else if (parentExhaustiveInventory && parent.getWeight() != null && batch.getWeight() != null) {
			samplingRatio = batch.getWeight() / parent.getWeight();
			elevateFactor = new BigDecimal(parent.getWeight()).divide(new BigDecimal(batch.getWeight()));
		}

		else if (parentExhaustiveInventory && parent.getWeight() != null && batch.hasChildren()) {
			samplingWeight = computeSumChildrenWeight(batch);
			if (samplingWeight != null) {
				samplingRatio = samplingWeight / parent.getWeight();
				elevateFactor = new BigDecimal(parent.getWeight()).divide(new BigDecimal(samplingWeight));
			}
		}

		else if ((!parentExhaustiveInventory || parent.getWeight() == null) && batch.hasChildren()) {
			samplingWeight = computeSumChildrenWeight(batch);
			if (samplingWeight != null) {
				samplingRatio = 1d;
				elevateFactor = new BigDecimal(1);
			}
		}

		if (samplingRatio == null || elevateFactor == null) {
			// Use default value (samplingRatio=1) if:
			// - batch has no children
			// - batch is parent of a sampling batch
			if (CollectionUtils.isEmpty(batch.getChildren())) {
				samplingRatio = 1d;
				elevateFactor = new BigDecimal(1);
			}
			else {
				throw new InvalidSamplingBatchException(String.format("Invalid sampling batch {id: %s, label: '%s'}: cannot get or compute the sampling ratio",
					batch.getId(), batch.getLabel()));
			}
		}

		// Remember values
		batch.setSamplingRatio(samplingRatio);
		batch.setElevateFactor(elevateFactor);

		if (samplingWeight == null) {
			if (batch.getWeight() != null) {
				samplingWeight = batch.getWeight();
			} else if (parentExhaustiveInventory && parent.getWeight() != null) {
				samplingWeight = parent.getWeight() * samplingRatio;
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
				.map(child -> (TempDenormalizedBatchVO) child)
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

	protected TempDenormalizedBatchVO createTempVO(BatchVO source) {
		TempDenormalizedBatchVO target = new TempDenormalizedBatchVO();
		denormalizedBatchRepository.copy(source, target, true);
		return target;
	}
}

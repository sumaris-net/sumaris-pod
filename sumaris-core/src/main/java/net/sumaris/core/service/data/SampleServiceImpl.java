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
import com.google.common.collect.ImmutableList;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.data.ImageAttachmentRepository;
import net.sumaris.core.dao.data.MeasurementDao;
import net.sumaris.core.dao.data.sample.SampleAdagioRepository;
import net.sumaris.core.dao.data.sample.SampleRepository;
import net.sumaris.core.event.config.ConfigurationEvent;
import net.sumaris.core.event.config.ConfigurationReadyEvent;
import net.sumaris.core.event.config.ConfigurationUpdatedEvent;
import net.sumaris.core.exception.NotUniqueException;
import net.sumaris.core.model.IWithFlagsValueObject;
import net.sumaris.core.model.data.IMeasurementEntity;
import net.sumaris.core.model.data.SampleMeasurement;
import net.sumaris.core.model.referential.ObjectTypeEnum;
import net.sumaris.core.model.referential.pmfm.MatrixEnum;
import net.sumaris.core.model.referential.pmfm.PmfmEnum;
import net.sumaris.core.service.administration.programStrategy.ProgramService;
import net.sumaris.core.util.Beans;
import net.sumaris.core.util.DataBeans;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.ValueObjectFlags;
import net.sumaris.core.vo.data.ImageAttachmentVO;
import net.sumaris.core.vo.data.MeasurementVO;
import net.sumaris.core.vo.data.sample.SampleFetchOptions;
import net.sumaris.core.vo.data.sample.SampleVO;
import net.sumaris.core.vo.filter.SampleFilterVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service("sampleService")
@RequiredArgsConstructor
@Slf4j
public class SampleServiceImpl implements SampleService {

	public static final String FAKE_NULL_TAG_ID = "NULL";

	protected final SampleRepository sampleRepository;

	protected final Optional<SampleAdagioRepository> sampleAdagioRepository;

	protected final MeasurementDao measurementDao;

	protected final ProgramService programService;

	protected final ImageAttachmentRepository imageAttachmentRepository;

	protected boolean enableSampleUniqueTag = false;
	protected boolean enableAdagioOptimization = false;

	@EventListener({ConfigurationReadyEvent.class, ConfigurationUpdatedEvent.class})
	public void onConfigurationReady(ConfigurationEvent event) {
		this.enableSampleUniqueTag = event.getConfiguration().enableSampleUniqueTag();
		this.enableAdagioOptimization = event.getConfiguration().enableAdagioOptimization();
	}

	@Override
	public Long countByFilter(SampleFilterVO filter) {
		return sampleRepository.count(filter);
	}

	@Override
	public Long countByLandingId(int landingId) {
		return sampleRepository.count(SampleFilterVO.builder().landingId(landingId).build());
	}

	@Override
	public List<SampleVO> getAllByOperationId(int operationId) {
		return sampleRepository.findAll(SampleFilterVO.builder().operationId(operationId).build());
	}

	@Override
	public List<SampleVO> getAllByOperationId(int operationId, SampleFetchOptions fetchOptions) {
		return sampleRepository.findAll(SampleFilterVO.builder().operationId(operationId).build(),
				fetchOptions);
	}

	@Override
	public List<SampleVO> getAllByLandingId(int landingId) {
		return sampleRepository.findAll(SampleFilterVO.builder().landingId(landingId).build());
	}

	@Override
	public List<SampleVO> getAllByLandingId(int landingId, SampleFetchOptions fetchOptions) {
		return sampleRepository.findAll(SampleFilterVO.builder().landingId(landingId).build(),
				fetchOptions);
	}

	@Override
	public SampleVO get(int id) {
		return sampleRepository.get(id);
	}

	@Override
	public SampleVO get(int id, SampleFetchOptions fetchOptions) {
		return sampleRepository.get(id, fetchOptions);
	}

	@Override
	public List<SampleVO> saveByOperationId(int operationId, List<SampleVO> sources) {

		List<SampleVO> result = sampleRepository.saveByOperationId(operationId, sources);

		// Excluded samples with same hash (= unchanged - not need to save children)
		List<SampleVO> changes = IWithFlagsValueObject.collectMissingFlag(result, ValueObjectFlags.SAME_HASH);

		if (CollectionUtils.isEmpty(changes)) return result; // No changes: skip

		// Check all samples have the same programId
		// /!\ should be the full list, not only changes
		int programId = extractSingleProgramId(result);

		// Check all sample's tag ids are unique
		if (this.enableSampleUniqueTag) {
			// Check inside the input list
			// /!\ should be the full list, not only changes
			checkSamplesUniqueTagInList(result);

			// Check inside the program
			checkSamplesUniqueTagInProgram(programId, changes);
		}

		// Save measurements
		changes.forEach(this::saveMeasurements);

		return result;
	}

	@Override
	public List<SampleVO> saveByLandingId(int landingId, List<SampleVO> sources) {

		List<SampleVO> result = sampleRepository.saveByLandingId(landingId, sources);

		// Excluded samples with same hash (= unchanged - not need to save children)
		List<SampleVO> changes = IWithFlagsValueObject.collectMissingFlag(result, ValueObjectFlags.SAME_HASH);

		if (CollectionUtils.isEmpty(changes)) return result; // No changes: skip

		// Check all sample's have the same program
		// /!\ should be done in the full list, not only changes
		int programId = extractSingleProgramId(result);

		// Check all sample's tag ids are uniques
		if (this.enableSampleUniqueTag) {
			// Check inside the input list
			// /!\ should be the full list, not only changes
			checkSamplesUniqueTagInList(result);

			// Check inside the program
			checkSamplesUniqueTagInProgram(programId, changes);
		}

		// Save measurements
		changes.forEach(this::saveMeasurements);

		// Save images
		changes.forEach(this::saveImageAttachments);

		return result;
	}

	@Override
	public SampleVO save(SampleVO sample) {
		Preconditions.checkNotNull(sample);
		Preconditions.checkArgument((sample.getOperation() != null && sample.getOperation().getId() != null)
				|| sample.getOperationId() != null
				|| (sample.getLanding() != null && sample.getLanding().getId() != null)
				|| sample.getLandingId() != null,
				"Missing sample.operation or sample.operationId, or sample.landing.id or sample.landingId");
		Preconditions.checkNotNull(sample.getRecorderDepartment(), "Missing sample.recorderDepartment");
		Preconditions.checkNotNull(sample.getRecorderDepartment().getId(), "Missing sample.recorderDepartment.id");

		SampleVO result = sampleRepository.save(sample);

		// Excluded samples with same hash (= unchanged - not need to save children)
		if (result.hasNotFlag(ValueObjectFlags.SAME_HASH)) {
			// Check tag is unique, inside the program
			if (enableSampleUniqueTag) checkSamplesUniqueTagInProgram(sample);

			// Save measurements
			saveMeasurements(result);

			// Save images
			saveImageAttachments(result);
		}

		return result;
	}

	@Override
	public List<SampleVO> save(List<SampleVO> sales) {
		Preconditions.checkNotNull(sales);

		return sales.stream()
				.map(this::save)
				.collect(Collectors.toList());
	}

	@Override
	public void delete(int id) {
		sampleRepository.deleteById(id);
	}

	@Override
	public void delete(List<Integer> ids) {
		Preconditions.checkNotNull(ids);
		ids.stream()
				.filter(Objects::nonNull)
				.forEach(this::delete);
	}

	@Override
	public void deleteAllByLandingId(int landingId) {
		sampleRepository.deleteByLandingId(landingId);
	}

	/**
	 * Transform a samples (with children) into a falt list, sorted with parent always before children
	 * @param sample
	 * @param result
	 */
	@Override
	public void treeToList(final SampleVO sample, final List<SampleVO> result) {
		if (sample == null) return;

		// Add current to list
		if (!result.contains(sample)) result.add(sample);

		// Process children
		if (CollectionUtils.isNotEmpty(sample.getChildren())) {
			// Recursive call
			sample.getChildren().forEach(child -> {
				fillDefaultProperties(sample, child);
				// Link to parent
				child.setParent(sample);
				treeToList(child, result);
			});
		}
	}

	/* -- protected methods -- */

	protected void saveMeasurements(SampleVO sample) {

		// Save measurements
		if (sample.getMeasurementValues() != null) {
			measurementDao.saveSampleMeasurementsMap(sample.getId(), sample.getMeasurementValues());
		} else {
			List<MeasurementVO> measurements = Beans.getList(sample.getMeasurements());
			measurements.forEach(m -> fillDefaultProperties(sample, m, SampleMeasurement.class));
			measurements = measurementDao.saveSampleMeasurements(sample.getId(), measurements);
			sample.setMeasurements(measurements);
		}
	}


	private void saveImageAttachments(SampleVO sample) {
		List<Integer> existingIdsToRemove = imageAttachmentRepository.getIdsFromObject(sample.getId(), ObjectTypeEnum.SAMPLE.getId());
		Beans.getStream(sample.getImages())
			.filter(Objects::nonNull)
			.forEach(image -> {
				boolean exists = existingIdsToRemove.remove(image.getId());

				// Skip image when already saved, and no content
				if (exists && image.getContent() == null) {
					log.debug("Skipping save of an existing image (content not set)");
				}
				else {
					// Fill defaults
					fillDefaultProperties(image, sample);

					// Save
					imageAttachmentRepository.save(image);
				}
			});

		// Remove
		if (CollectionUtils.isNotEmpty(existingIdsToRemove)) {
			imageAttachmentRepository.deleteAllByIdInBatch(existingIdsToRemove);
		}
	}


	/**
	 * Check all tag_id are unique in the input list (and NOT in the DB table)
	 * @param samples
	 */
	private void checkSamplesUniqueTagInList(List<SampleVO> samples) {
		// /!\ Use a default value, because sample's TAG_ID can be null
		Set<String> duplicatedTagIds = Beans.splitByNotUniqueProperty(samples, SampleVO.GetterFields.TAG_ID, FAKE_NULL_TAG_ID)
				.asMap().entrySet().stream()
				// Filter more than one sample for the same tag id
				.filter(e -> e.getValue().size() > 1
					// And exclude null value
					&& !FAKE_NULL_TAG_ID.equals(e.getKey()))
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());

		if (CollectionUtils.isNotEmpty(duplicatedTagIds)) {
			// Get suffix only
			List<String> duplicatedTagIdSuffixes = Beans.transformCollection(duplicatedTagIds, this::getTagIdSuffix);

			throw new NotUniqueException("Duplicated tag_id (in list)", duplicatedTagIdSuffixes);
		}
	}

	private void checkSamplesUniqueTagInProgram(SampleVO sample) {
		int programId = extractProgramId(sample);
		this.checkSamplesUniqueTagInProgram(programId, ImmutableList.of(sample));
	}
	private void checkSamplesUniqueTagInProgram(int programId, List<SampleVO> samples) {

		List<Integer> sampleIds = Beans.collectIds(samples);
		Set<String> tagIds = Beans.transformCollection(samples, SampleVO::getTagId)
				.stream()
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());

		// Check if tagIds are uniques by program
		if (CollectionUtils.isNotEmpty(tagIds) && PmfmEnum.TAG_ID.getId() != -1) {
			Set<String> duplicatedTagIds;
			if (this.enableAdagioOptimization && sampleAdagioRepository.isPresent()) {
				String programLabel = programService.get(programId).getLabel();
				duplicatedTagIds = sampleAdagioRepository.get().getDuplicatedTagIdsByProgramLabel(
						programLabel,
						PmfmEnum.TAG_ID.getId(),
						tagIds,
						sampleIds);
			}
			else {
				duplicatedTagIds = sampleRepository.getDuplicatedTagIdsByProgramId(
									programId,
									PmfmEnum.TAG_ID.getId(),
									tagIds,
									sampleIds);
			}

			if (CollectionUtils.isNotEmpty(duplicatedTagIds)) {
				// Get suffix only
				List<String> duplicatedTagIdSuffixes = Beans.transformCollection(duplicatedTagIds, this::getTagIdSuffix);

				throw new NotUniqueException("Duplicated tag_id (in program)", duplicatedTagIdSuffixes);
			}
		}
	}

	/**
	 * Get last part of the tag_id (characters after the last separator).
	 * <p>'22PLEUPLA002-0349' will return '0349'</p>
	 *
	 * @param tagId
	 * @return
	 */
	public String getTagIdSuffix(@NonNull String tagId) {
		return StringUtils.defaultIfBlank(StringUtils.getSuffixOrDefault(tagId, "-", null), tagId);
	}

	public int extractSingleProgramId(@NonNull List<SampleVO> samples) {
		Preconditions.checkArgument(samples.size() > 0, "Required a not empty samples list");
		int programId = extractProgramId(samples.get(0));

		// Check all have the same program id
		Preconditions.checkArgument(samples.stream()
						.filter(s -> extractProgramId(s) != programId)
						.findFirst().isEmpty(),
				"All samples should have the same 'program.id'");

		return programId;
	}

	public int extractProgramId(@NonNull SampleVO sample) {
		Preconditions.checkNotNull(sample.getProgram(), "Missing required argument 'sample.program'");
		Preconditions.checkNotNull(sample.getProgram().getId(), "Missing required argument 'sample.program.id'");
		return sample.getProgram().getId();
	}

	protected void fillDefaultProperties(SampleVO parent, MeasurementVO measurement, Class<? extends IMeasurementEntity> entityClass) {
		if (measurement == null) return;

		// Copy recorder department from the parent
		if (measurement.getRecorderDepartment() == null || measurement.getRecorderDepartment().getId() == null) {
			measurement.setRecorderDepartment(parent.getRecorderDepartment());
		}

		measurement.setEntityName(entityClass.getSimpleName());
	}

	protected void fillDefaultProperties(SampleVO parent, SampleVO sample) {
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

		// Fill sample (use operation end date time)
		if (sample.getSampleDate() == null) {
			sample.setSampleDate(parent.getSampleDate());
		}

		sample.setParentId(parent.getId());
		sample.setOperationId(parent.getOperationId());
	}

	protected void fillDefaultProperties(ImageAttachmentVO image, SampleVO parent) {
		if (image == null) return;

		// Set default recorder department
		DataBeans.setDefaultRecorderDepartment(image, parent.getRecorderDepartment());

		// Fill date
		if (image.getDateTime() == null) {
			image.setDateTime(parent.getSampleDate());
		}

		// Link to sample
		image.setObjectId(parent.getId());
		image.setObjectTypeId(ObjectTypeEnum.SAMPLE.getId());
	}
}

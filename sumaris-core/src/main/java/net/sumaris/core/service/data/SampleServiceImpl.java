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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.dao.data.ImageAttachmentRepository;
import net.sumaris.core.dao.data.MeasurementDao;
import net.sumaris.core.dao.data.sample.SampleRepository;
import net.sumaris.core.exception.NotUniqueException;
import net.sumaris.core.model.data.IMeasurementEntity;
import net.sumaris.core.model.data.SampleMeasurement;
import net.sumaris.core.model.referential.ObjectTypeEnum;
import net.sumaris.core.model.referential.pmfm.MatrixEnum;
import net.sumaris.core.model.referential.pmfm.PmfmEnum;
import net.sumaris.core.util.Beans;
import net.sumaris.core.util.DataBeans;
import net.sumaris.core.vo.ValueObjectFlags;
import net.sumaris.core.vo.data.ImageAttachmentVO;
import net.sumaris.core.vo.data.MeasurementVO;
import net.sumaris.core.vo.data.sample.SampleFetchOptions;
import net.sumaris.core.vo.data.sample.SampleVO;
import net.sumaris.core.vo.filter.SampleFilterVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service("sampleService")
@RequiredArgsConstructor
@Slf4j
public class SampleServiceImpl implements SampleService {

	private final SumarisConfiguration configuration;

	protected final SampleRepository sampleRepository;

	protected final MeasurementDao measurementDao;

	protected final ImageAttachmentRepository imageAttachmentRepository;

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
		List<SampleVO> changes = result.stream()
				.filter(sample -> sample.hasNotFlag(ValueObjectFlags.SAME_HASH))
				.collect(Collectors.toList());

		if (CollectionUtils.isNotEmpty(changes)) {
			// Save measurements
			boolean enableSampleUniqueTag = configuration.enableSampleUniqueTag();
			changes.forEach(sample -> saveMeasurements(sample, enableSampleUniqueTag));

		}

		return result;
	}

	@Override
	public List<SampleVO> saveByLandingId(int landingId, List<SampleVO> sources) {

		List<SampleVO> result = sampleRepository.saveByLandingId(landingId, sources);

		// Excluded samples with same hash (= unchanged - not need to save children)
		List<SampleVO> changes = result.stream()
			.filter(sample -> sample.hasNotFlag(ValueObjectFlags.SAME_HASH))
			.collect(Collectors.toList());

		if (CollectionUtils.isNotEmpty(changes)) {
			// Save measurements
			boolean enableSampleUniqueTag = configuration.enableSampleUniqueTag();
			changes.forEach(sample -> saveMeasurements(sample, enableSampleUniqueTag));

			// Save images
			changes.forEach(this::saveImageAttachments);
		}

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
			// Save measurements
			saveMeasurements(result, configuration.enableSampleUniqueTag());

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

	protected void saveMeasurements(SampleVO sample, boolean enableSampleUniqueTag) {
		// Make sure sample tag is unique
		if (enableSampleUniqueTag) checkSampleUniqueTag(sample);

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
		sample.getImages()
			.stream().filter(Objects::nonNull)
			.forEach(image -> {
				// Fill defaults
				fillDefaultProperties(image, sample);

				// Save
				imageAttachmentRepository.save(image);
			});
	}

	private void checkSampleUniqueTag(SampleVO savedSample) {
		String savedSampleTagId = null;

		// Get tag_id measurement
		if (savedSample.getMeasurementValues() != null) {
			savedSampleTagId = savedSample.getMeasurementValues().get(PmfmEnum.TAG_ID.getId());
		} else if (savedSample.getMeasurements() != null) {
			savedSampleTagId = savedSample.getMeasurements().stream()
					.filter(m -> PmfmEnum.TAG_ID.getId().equals(m.getPmfmId()))
					.map(MeasurementVO::getAlphanumericalValue)
					.findFirst().orElse(null);
		}

		// Check if tag_id is unique by program
		if (savedSampleTagId != null) {
			long count = sampleRepository.findAll(SampleFilterVO.builder()
					.programLabel(savedSample.getProgram().getLabel()).tagId(savedSampleTagId).build())
					.stream()
					.filter(s -> savedSample.getId() == null || !Objects.equals(s.getId(), savedSample.getId()))
					.count();
			if (count > 0) {
				throw new NotUniqueException(String.format("Sample tag measurement '%s' already exists", savedSampleTagId));
			}
		}
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

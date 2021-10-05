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
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.dao.data.MeasurementDao;
import net.sumaris.core.dao.data.sample.SampleRepository;
import net.sumaris.core.exception.NotUniqueException;
import net.sumaris.core.model.data.IMeasurementEntity;
import net.sumaris.core.model.data.SampleMeasurement;
import net.sumaris.core.model.referential.pmfm.MatrixEnum;
import net.sumaris.core.model.referential.pmfm.PmfmEnum;
import net.sumaris.core.util.Beans;
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
@Slf4j
public class SampleServiceImpl implements SampleService {

	@Autowired
	private SumarisConfiguration configuration;

	@Autowired
	protected SampleRepository sampleRepository;

	@Autowired
	protected MeasurementDao measurementDao;

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
	public SampleVO get(int saleId) {
		return sampleRepository.get(saleId);
	}

	@Override
	public List<SampleVO> saveByOperationId(int operationId, List<SampleVO> sources) {

		List<SampleVO> result = sampleRepository.saveByOperationId(operationId, sources);

		// Save measurements
		saveMeasurements(result);

		return result;
	}

	@Override
	public List<SampleVO> saveByLandingId(int landingId, List<SampleVO> sources) {

		List<SampleVO> result = sampleRepository.saveByLandingId(landingId, sources);

		// Save measurements
		saveMeasurements(result);

		return result;
	}

	@Override
	public SampleVO save(SampleVO sample) {
		Preconditions.checkNotNull(sample);
		Preconditions.checkArgument((sample.getOperation() != null && sample.getOperation().getId() != null) || sample.getOperationId() != null, "Missing sample.operation or sample.operationId");
		Preconditions.checkNotNull(sample.getRecorderDepartment(), "Missing sample.recorderDepartment");
		Preconditions.checkNotNull(sample.getRecorderDepartment().getId(), "Missing sample.recorderDepartment.id");

		return sampleRepository.save(sample);
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

		// Add the batch itself
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

	protected void saveMeasurements(List<SampleVO> result) {
		result.forEach(savedSample -> {
			checkUniqueTag(savedSample);
			if (savedSample.getMeasurementValues() != null) {
				measurementDao.saveSampleMeasurementsMap(savedSample.getId(), savedSample.getMeasurementValues());
			}
			else {
				List<MeasurementVO> measurements = Beans.getList(savedSample.getMeasurements());
				measurements.forEach(m -> fillDefaultProperties(savedSample, m, SampleMeasurement.class));
				measurements = measurementDao.saveSampleMeasurements(savedSample.getId(), measurements);
				savedSample.setMeasurements(measurements);
			}
		});
	}

	private void checkUniqueTag(SampleVO savedSample) {
		if (!configuration.enableSampleUniqueTag()) return;
		String savedSampleTagId = null;

		// Get tag_id measurement
		if (savedSample.getMeasurementValues() != null) {
			savedSampleTagId = savedSample.getMeasurementValues().get(PmfmEnum.TAG_ID.getId());
		} else if (savedSample.getMeasurements() != null) {
			savedSampleTagId = savedSample.getMeasurements().stream()
					.filter(m -> m.getId() == PmfmEnum.TAG_ID.getId())
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
}

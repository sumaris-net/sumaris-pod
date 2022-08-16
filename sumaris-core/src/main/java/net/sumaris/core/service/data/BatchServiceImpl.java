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
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import lombok.NonNull;
import net.sumaris.core.dao.data.batch.BatchRepository;
import net.sumaris.core.dao.data.MeasurementDao;
import net.sumaris.core.event.entity.EntityUpdateEvent;
import net.sumaris.core.model.data.Batch;
import net.sumaris.core.model.data.BatchQuantificationMeasurement;
import net.sumaris.core.model.data.BatchSortingMeasurement;
import net.sumaris.core.model.data.IMeasurementEntity;
import net.sumaris.core.service.referential.pmfm.PmfmService;
import net.sumaris.core.util.Beans;
import net.sumaris.core.vo.data.batch.BatchFetchOptions;
import net.sumaris.core.vo.data.batch.BatchFilterVO;
import net.sumaris.core.vo.data.batch.BatchVO;
import net.sumaris.core.vo.data.MeasurementVO;
import net.sumaris.core.vo.data.QuantificationMeasurementVO;
import net.sumaris.core.vo.data.sample.SampleVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service("batchService")
@Slf4j
public class BatchServiceImpl implements BatchService {

	@Autowired
	protected BatchRepository batchRepository;

	@Autowired
	protected MeasurementDao measurementDao;

	@Autowired
	protected PmfmService pmfmService;

	@Autowired
	private ApplicationEventPublisher publisher;

	@Override
	public List<BatchVO> getAllByOperationId(int operationId) {
		return getAllByOperationId(operationId, BatchFetchOptions.DEFAULT);
	}

	@Override
	public List<BatchVO> getAllByOperationId(int operationId, @NonNull BatchFetchOptions fetchOptions) {
		return batchRepository.findAll(BatchFilterVO.builder()
						.operationId(operationId).build(),
				fetchOptions);
	}

	@Override
	public BatchVO get(int id) {
		return batchRepository.get(id);
	}

	@Override
	public List<BatchVO> saveAllByOperationId(int operationId, List<BatchVO> sources) {

		List<BatchVO> result = batchRepository.saveAllByOperationId(operationId, sources);

		// Save measurements
		saveMeasurements(result);

		// Emit update event, on the catch batch
		result.stream()
				// Find the catch batch
				.filter(b -> b.getParent() == null && b.getParentId() == null)
				.findFirst()
				// Transform to event
				.map(catchBatch -> new EntityUpdateEvent(catchBatch.getId(), Batch.class.getSimpleName(), catchBatch))
				// Publish
				.ifPresent(publisher::publishEvent);

		return result;
	}

	@Override
	public BatchVO save(BatchVO batch) {
		Preconditions.checkNotNull(batch);
		Preconditions.checkArgument((batch.getOperation() != null && batch.getOperation().getId() != null) || batch.getOperationId() != null, "Missing batch.operation or batch.operationId");
		Preconditions.checkNotNull(batch.getRecorderDepartment(), "Missing batch.recorderDepartment");
		Preconditions.checkNotNull(batch.getRecorderDepartment().getId(), "Missing batch.recorderDepartment.id");

		return batchRepository.save(batch);
	}

	@Override
	public List<BatchVO> save(List<BatchVO> sources) {
		Preconditions.checkNotNull(sources);

		return sources.stream()
				.map(this::save)
				.collect(Collectors.toList());
	}

	@Override
	public void delete(int id) {
		batchRepository.deleteById(id);
	}

	@Override
	public void delete(@NonNull List<Integer> ids) {
		ids.stream()
				.filter(Objects::nonNull)
				.forEach(this::delete);
	}

	@Override
	public List<BatchVO> toFlatList(final BatchVO catchBatch) {
		return batchRepository.toFlatList(catchBatch);
	}


	/* -- protected methods -- */

	protected void saveMeasurements(List<BatchVO> result){

		// Save measurements
		result.forEach(savedBatch -> {

			// If only one maps: distinguish each item
			if (savedBatch.getMeasurementValues() != null) {

				Map<Integer, String> quantificationMeasurements = Maps.newLinkedHashMap();
				Map<Integer, String> sortingMeasurements = Maps.newLinkedHashMap();

				savedBatch.getMeasurementValues().forEach((pmfmId, value) -> {
					if (pmfmService.isWeightPmfm(pmfmId)) {
						quantificationMeasurements.putIfAbsent(pmfmId, value);
					}
					else {
						if (sortingMeasurements.containsKey(pmfmId)) {
							log.warn(String.format("Duplicate measurement width {pmfmId: %s} on batch {id: %s}", pmfmId, savedBatch.getId()));
						}
						else {
							sortingMeasurements.putIfAbsent(pmfmId, value);
						}
					}
				});
				measurementDao.saveBatchSortingMeasurementsMap(savedBatch.getId(), sortingMeasurements);
				measurementDao.saveBatchQuantificationMeasurementsMap(savedBatch.getId(), quantificationMeasurements);
			}
			else {
				// Sorting measurement
				{
					List<MeasurementVO> measurements = Beans.getList(savedBatch.getSortingMeasurements());
					measurements.forEach(m -> fillDefaultProperties(savedBatch, m, BatchSortingMeasurement.class));
					measurements = measurementDao.saveBatchSortingMeasurements(savedBatch.getId(), measurements);
					savedBatch.setSortingMeasurements(measurements);
				}

				// Quantification measurement
				{
					List<QuantificationMeasurementVO> measurements = Beans.getList(savedBatch.getQuantificationMeasurements());
					measurements.forEach(m -> fillDefaultProperties(savedBatch, m, BatchQuantificationMeasurement.class));
					measurements = measurementDao.saveBatchQuantificationMeasurements(savedBatch.getId(), measurements);
					savedBatch.setQuantificationMeasurements(measurements);
				}
			}
		});
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

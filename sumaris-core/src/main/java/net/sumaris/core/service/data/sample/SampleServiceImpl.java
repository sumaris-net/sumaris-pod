package net.sumaris.core.service.data.sample;

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
import net.sumaris.core.dao.data.MeasurementDao;
import net.sumaris.core.dao.data.sample.SampleDao;
import net.sumaris.core.dao.technical.Beans;
import net.sumaris.core.model.data.Operation;
import net.sumaris.core.model.data.VesselPosition;
import net.sumaris.core.model.data.measure.IMeasurementEntity;
import net.sumaris.core.model.data.measure.VesselUseMeasurement;
import net.sumaris.core.model.data.sample.SampleMeasurement;
import net.sumaris.core.vo.data.MeasurementVO;
import net.sumaris.core.vo.data.OperationVO;
import net.sumaris.core.vo.data.SampleVO;
import net.sumaris.core.vo.data.VesselPositionVO;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service("sampleService")
public class SampleServiceImpl implements SampleService {

	private static final Log log = LogFactory.getLog(SampleServiceImpl.class);

	@Autowired
	protected SampleDao sampleDao;

	@Autowired
	protected MeasurementDao measurementDao;

	@Override
	public List<SampleVO> getAllByOperationId(int tripId) {
		return sampleDao.getAllByOperationId(tripId);
	}

	@Override
	public SampleVO get(int saleId) {
		return sampleDao.get(saleId);
	}

	@Override
	public List<SampleVO> saveByOperationId(int operationId, List<SampleVO> sources) {

		List<SampleVO> result = sampleDao.saveByOperationId(operationId, sources);

		// Save measurements
		result.stream().forEach(savedSample -> {
			if (savedSample.getMeasurementsMap() != null) {
				measurementDao.saveSampleMeasurementsMap(savedSample.getId(), savedSample.getMeasurementsMap());
			}
			else {
				List<MeasurementVO> measurements = Beans.getList(savedSample.getMeasurements());
				measurements.forEach(m -> fillDefaultProperties(savedSample, m, SampleMeasurement.class));
				measurements = measurementDao.saveSampleMeasurements(savedSample.getId(), measurements);
				savedSample.setMeasurements(measurements);
			}
		});

		return result;
	}

	@Override
	public SampleVO save(SampleVO sample) {
		Preconditions.checkNotNull(sample);
		Preconditions.checkArgument((sample.getOperation() != null && sample.getOperation().getId() != null) || sample.getOperationId() != null, "Missing sample.operation or sample.operationId");
		Preconditions.checkNotNull(sample.getRecorderDepartment(), "Missing sample.recorderDepartment");
		Preconditions.checkNotNull(sample.getRecorderDepartment().getId(), "Missing sample.recorderDepartment.id");

		return sampleDao.save(sample);
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
		sampleDao.delete(id);
	}

	@Override
	public void delete(List<Integer> ids) {
		Preconditions.checkNotNull(ids);
		ids.stream()
				.filter(Objects::nonNull)
				.forEach(this::delete);
	}

	/* -- protected methods -- */

	protected void fillDefaultProperties(SampleVO parent, MeasurementVO measurement, Class<? extends IMeasurementEntity> entityClass) {
		if (measurement == null) return;

		// Copy recorder department from the parent
		if (measurement.getRecorderDepartment() == null || measurement.getRecorderDepartment().getId() == null) {
			measurement.setRecorderDepartment(parent.getRecorderDepartment());
		}

		measurement.setOperationId(parent.getId());
		measurement.setEntityName(entityClass.getSimpleName());
	}
}

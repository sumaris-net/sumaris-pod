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
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.dao.data.MeasurementDao;
import net.sumaris.core.dao.data.OperationDao;
import net.sumaris.core.dao.data.VesselPositionDao;
import net.sumaris.core.dao.technical.Beans;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.model.data.GearUseMeasurement;
import net.sumaris.core.model.data.IMeasurementEntity;
import net.sumaris.core.model.data.VesselUseMeasurement;
import net.sumaris.core.vo.data.MeasurementVO;
import net.sumaris.core.vo.data.OperationVO;
import net.sumaris.core.vo.data.PhysicalGearVO;
import net.sumaris.core.vo.data.VesselPositionVO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service("operationService")
public class OperationServiceImpl implements OperationService {

	private static final Log log = LogFactory.getLog(OperationServiceImpl.class);

	@Autowired
	protected SumarisConfiguration config;

	@Autowired
	protected OperationDao operationDao;

	@Autowired
	protected VesselPositionDao vesselPositionDao;

	@Autowired
	protected MeasurementDao measurementDao;


	@Override
	public List<OperationVO> getAllByTripId(int tripId, int offset, int size, String sortAttribute,
                                     SortDirection sortDirection) {
		return operationDao.getAllByTripId(tripId, offset, size, sortAttribute, sortDirection);
	}

	@Override
	public OperationVO get(int operationId) {
		return operationDao.get(operationId);
	}

	@Override
	public OperationVO save(final OperationVO source) {
		Preconditions.checkNotNull(source);
		Preconditions.checkNotNull(source.getStartDateTime(), "Missing startDateTime");
		Preconditions.checkNotNull(source.getEndDateTime(), "Missing endDateTime");
		Preconditions.checkNotNull(source.getRecorderDepartment(), "Missing recorderDepartment");
		Preconditions.checkNotNull(source.getRecorderDepartment().getId(), "Missing recorderDepartment.id");

		// Default properties
		if (source.getQualityFlagId() == null) {
			source.setQualityFlagId(config.getDefaultQualityFlagId());
		}

		OperationVO savedOperation = operationDao.save(source);

		// Save positions
		{
			List<VesselPositionVO> positions = Beans.getList(source.getPositions());
			positions.forEach(m -> fillDefaultProperties(savedOperation, m));
			positions = vesselPositionDao.saveByOperationId(savedOperation.getId(), positions);
			savedOperation.setPositions(positions);
		}

		// Save measurements (vessel use measurement)
		{
			List<MeasurementVO> measurements = Beans.getList(source.getMeasurements());
			measurements.forEach(m -> fillDefaultProperties(savedOperation, m, VesselUseMeasurement.class));
			measurements = measurementDao.saveVesselUseMeasurementsByOperationId(savedOperation.getId(), measurements);
			savedOperation.setMeasurements(measurements);
		}

		// Save gear measurements (gear use measurement)
		{
			List<MeasurementVO> measurements = Beans.getList(source.getGearMeasurements());
			measurements.forEach(m -> fillDefaultProperties(savedOperation, m, GearUseMeasurement.class));
			measurements = measurementDao.saveGearUseMeasurementsByOperationId(savedOperation.getId(), measurements);
			savedOperation.setGearMeasurements(measurements);
		}

		return savedOperation;
	}

	@Override
	public List<OperationVO> save(List<OperationVO> operations) {
		Preconditions.checkNotNull(operations);

		return operations.stream()
				.map(this::save)
				.collect(Collectors.toList());
	}

	@Override
	public void delete(int id) {
		operationDao.delete(id);
	}

	@Override
	public void delete(List<Integer> ids) {
		Preconditions.checkNotNull(ids);
		ids.stream()
				.filter(Objects::nonNull)
				.forEach(this::delete);
	}

	/* -- protected methods -- */

	protected void fillDefaultProperties(OperationVO parent, VesselPositionVO position) {
		if (position == null) return;

		// Copy recorder department from the parent
		if (position.getRecorderDepartment() == null || position.getRecorderDepartment().getId() == null) {
			position.setRecorderDepartment(parent.getRecorderDepartment());
		}

		position.setOperationId(parent.getId());
	}

	protected void fillDefaultProperties(OperationVO parent, MeasurementVO measurement, Class<? extends IMeasurementEntity> entityClass) {
		if (measurement == null) return;

		// Copy recorder department from the parent
		if (measurement.getRecorderDepartment() == null || measurement.getRecorderDepartment().getId() == null) {
			measurement.setRecorderDepartment(parent.getRecorderDepartment());
		}

		measurement.setPhysicalGearId(parent.getId());
		measurement.setEntityName(entityClass.getSimpleName());
	}
}

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
import net.sumaris.core.dao.data.MeasurementDao;
import net.sumaris.core.dao.data.physicalGear.PhysicalGearRepository;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.model.data.IMeasurementEntity;
import net.sumaris.core.model.data.PhysicalGearMeasurement;
import net.sumaris.core.util.Beans;
import net.sumaris.core.vo.ValueObjectFlags;
import net.sumaris.core.vo.data.DataFetchOptions;
import net.sumaris.core.vo.data.MeasurementVO;
import net.sumaris.core.vo.data.PhysicalGearVO;
import net.sumaris.core.vo.filter.PhysicalGearFilterVO;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service("physicalGearService")
@Slf4j
public class PhysicalGearServiceImpl implements PhysicalGearService {

	@Autowired
	protected PhysicalGearRepository physicalGearRepository;

	@Autowired
	protected MeasurementDao measurementDao;

	@Override
	public List<PhysicalGearVO> findAll(PhysicalGearFilterVO filter, Page page, DataFetchOptions options) {
		return physicalGearRepository.findAll(filter != null ? filter : new PhysicalGearFilterVO(), page, options);
	}

	@Override
	public List<PhysicalGearVO> getAllByTripId(int tripId, DataFetchOptions options) {
		return physicalGearRepository.findAllVO(physicalGearRepository.hasTripId(tripId), options);
	}

	@Override
	public List<PhysicalGearVO> saveAllByTripId(int tripId, List<PhysicalGearVO> sources) {
		return saveAllByTripId(tripId, sources, null);
	}

	@Override
	public List<PhysicalGearVO> saveAllByTripId(int tripId, List<PhysicalGearVO> sources, List<Integer> idsToRemove) {
		List<PhysicalGearVO> result = physicalGearRepository.saveAllByTripId(tripId, sources, idsToRemove);

		// Save measurements
		saveMeasurements(result);

		return result;
	}

	@Override
	public void delete(List<Integer> ids) {
		Preconditions.checkNotNull(ids);
		ids.stream()
				.filter(Objects::nonNull)
				.forEach(this::delete);
	}

	@Override
	public void delete(int id) {
		// Apply deletion
		physicalGearRepository.deleteById(id);
	}

	@Override
	public PhysicalGearVO get(int physicalGearId) {
		return physicalGearRepository.get(physicalGearId);
	}

	@Override
	public PhysicalGearVO get(int physicalGearId, DataFetchOptions o) {
		return physicalGearRepository.get(physicalGearId, o);
	}

	@Override
	public void treeToList(final PhysicalGearVO source, final List<PhysicalGearVO> result) {
		if (source == null) return;

		// Add current to list
		if (!result.contains(source)) result.add(source);

		// Process children
		if (CollectionUtils.isNotEmpty(source.getChildren())) {
			// Recursive call
			source.getChildren().forEach(child -> {
				fillDefaultProperties(source, child);
				// Link to parent
				child.setParent(source);
				treeToList(child, result);
			});
		}
	}

	/* -- protected functions -- */

	protected void saveMeasurements(List<PhysicalGearVO> result) {
		result.stream()
		// Exclude already unchanged entities
		.filter(source -> source.hasNotFlag(ValueObjectFlags.SAME_HASH))
			// Save measurements
		.forEach(source -> {
			if (source.getMeasurementValues() != null) {
				measurementDao.savePhysicalGearMeasurementsMap(source.getId(), source.getMeasurementValues());
			}
			else {
				List<MeasurementVO> measurements = Beans.getList(source.getMeasurements());
				measurements.forEach(m -> fillDefaultProperties(source, m, PhysicalGearMeasurement.class));
				measurements = measurementDao.savePhysicalGearMeasurements(source.getId(), measurements);
				source.setMeasurements(measurements);
			}
		});
	}

	protected void fillDefaultProperties(PhysicalGearVO parent, MeasurementVO measurement, Class<? extends IMeasurementEntity> entityClass) {
		if (measurement == null) return;

		// Copy recorder department from the parent
		if (measurement.getRecorderDepartment() == null || measurement.getRecorderDepartment().getId() == null) {
			measurement.setRecorderDepartment(parent.getRecorderDepartment());
		}
		// Copy recorder person from the parent
		if (measurement.getRecorderPerson() == null || measurement.getRecorderPerson().getId() == null) {
			measurement.setRecorderPerson(parent.getRecorderPerson());
		}

		measurement.setEntityName(entityClass.getSimpleName());
	}

	protected void fillDefaultProperties(PhysicalGearVO parent, PhysicalGearVO source) {
		if (source == null) return;

		// Copy recorder department from the parent
		if (source.getRecorderDepartment() == null || source.getRecorderDepartment().getId() == null) {
			source.setRecorderDepartment(parent.getRecorderDepartment());
		}

		source.setProgram(parent.getProgram());
		source.setParentId(parent.getId());
		source.setTripId(parent.getTripId());
	}
}

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
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.dao.data.MeasurementDao;
import net.sumaris.core.dao.data.PhysicalGearDao;
import net.sumaris.core.dao.technical.Beans;
import net.sumaris.core.model.data.PhysicalGearMeasurement;
import net.sumaris.core.model.data.VesselUseMeasurement;
import net.sumaris.core.vo.data.MeasurementVO;
import net.sumaris.core.vo.data.PhysicalGearVO;
import net.sumaris.core.vo.data.TripVO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("physicalGearService")
public class PhysicalGearServiceImpl implements PhysicalGearService {

	private static final Log log = LogFactory.getLog(PhysicalGearServiceImpl.class);

	@Autowired
	protected SumarisConfiguration config;

	@Autowired
	protected PhysicalGearDao physicalGearDao;

	@Autowired
	protected MeasurementDao measurementDao;

	@Override
	public List<PhysicalGearVO> getPhysicalGearByTripId(int tripId) {
		return physicalGearDao.getPhysicalGearByTripId(tripId);
	}

	@Override
	public List<PhysicalGearVO> save(int tripId, List<PhysicalGearVO> gears) {
		return physicalGearDao.save(tripId, gears);
	}

	protected PhysicalGearVO save(PhysicalGearVO source) {
		Preconditions.checkNotNull(source);
		Preconditions.checkNotNull(source.getGear(), "Missing gear");
		Preconditions.checkNotNull(source.getGear().getId(), "Missing gear.id");

		// Default properties
		if (source.getQualityFlagId() == null) {
			source.setQualityFlagId(config.getDefaultQualityFlagId());
		}

		PhysicalGearVO savedGear = physicalGearDao.save(source);

		// Save measurements
		List<MeasurementVO> measurements = Beans.getList(source.getMeasurements());
		source.getMeasurements().forEach(m -> fillDefaultProperties(savedGear, m));
		measurements = measurementDao.savePhysicalGearMeasurementByPhysicalGearId(savedGear.getId(), measurements);
		savedGear.setMeasurements(measurements);

		return savedGear;
	}

	void fillDefaultProperties(PhysicalGearVO parent, MeasurementVO measurement) {
		if (measurement == null) return;

		// Copy recorder department from the parent
		if (measurement.getRecorderDepartment() == null || measurement.getRecorderDepartment().getId() == null) {
			measurement.setRecorderDepartment(parent.getRecorderDepartment());
		}
		// Copy recorder person from the parent
		if (measurement.getRecorderPerson() == null || measurement.getRecorderPerson().getId() == null) {
			measurement.setRecorderPerson(parent.getRecorderPerson());
		}

		measurement.setPhysicalGearId(parent.getId());
		measurement.setEntityName(PhysicalGearMeasurement.class.getSimpleName());
	}
}

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
import net.sumaris.core.dao.data.MeasurementDao;
import net.sumaris.core.dao.data.VesselDao;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.model.data.VesselFeatures;
import net.sumaris.core.model.data.VesselPhysicalMeasurement;
import net.sumaris.core.util.Beans;
import net.sumaris.core.util.DataBeans;
import net.sumaris.core.vo.data.MeasurementVO;
import net.sumaris.core.vo.data.VesselFeaturesVO;
import net.sumaris.core.vo.data.VesselSnapshotVO;
import net.sumaris.core.vo.data.VesselRegistrationVO;
import net.sumaris.core.vo.filter.VesselFilterVO;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service("vesselService")
public class VesselServiceImpl implements VesselService {

	private static final Logger log = LoggerFactory.getLogger(VesselServiceImpl.class);

	@Autowired
	protected VesselDao vesselDao;

	@Autowired
	protected MeasurementDao measurementDao;


	@Override
	public List<VesselSnapshotVO> findSnapshotByFilter(VesselFilterVO filter, int offset, int size, String sortAttribute, SortDirection sortDirection) {
		return vesselDao.findByFilter(filter, offset, size, sortAttribute, sortDirection);
	}

	@Override
	public List<VesselSnapshotVO> getSnapshotByVesselId(int vesselId, int offset, int size, String sortAttribute, SortDirection sortDirection) {
		return vesselDao.getByVesselId(vesselId, offset, size, sortAttribute, sortDirection);
	}

	@Override
	public List<VesselRegistrationVO> getRegistrationsByVesselId(int vesselId, int offset, int size, String sortAttribute, SortDirection sortDirection) {
		return vesselDao.getRegistrationsByVesselId(vesselId, offset, size, sortAttribute, sortDirection);
	}

	@Override
	public VesselSnapshotVO getSnapshotByIdAndDate(int vesselId, Date date) {
		return vesselDao.getSnapshotByIdAndDate(vesselId, date);
	}

	@Override
	public VesselFeaturesVO save(VesselFeaturesVO source) {
		Preconditions.checkNotNull(source);
		Preconditions.checkNotNull(source.getRecorderDepartment(), "Missing recorderDepartment");
		Preconditions.checkNotNull(source.getRecorderDepartment().getId(), "Missing recorderDepartment.id");

		Preconditions.checkNotNull(source.getBasePortLocation().getId(), "Missing basePortLocation.id");
		Preconditions.checkArgument(source.getVesselId() != null && source.getVesselType() != null, "Missing vesselId or vesselTypeId");

		Preconditions.checkNotNull(source.getStartDate(), "Missing start date");
		Preconditions.checkArgument(StringUtils.isNotBlank(source.getExteriorMarking()), "Missing exterior marking");
		Preconditions.checkArgument(StringUtils.isNotBlank(source.getRegistrationCode()), "Missing registration code");
		Preconditions.checkNotNull(source.getRegistrationLocation().getId(), "Missing registration location");

		VesselFeaturesVO savedVesselFeatures = vesselDao.save(source);

		// Save measurements
		if (savedVesselFeatures.getMeasurementValues() != null) {
			measurementDao.saveVesselPhysicalMeasurementsMap(savedVesselFeatures.getId(), savedVesselFeatures.getMeasurementValues());
		}
		else {
			List<MeasurementVO> measurements = Beans.getList(savedVesselFeatures.getMeasurements());
			measurements.forEach(m -> fillDefaultProperties(savedVesselFeatures, m));
			measurements = measurementDao.saveVesselPhysicalMeasurements(savedVesselFeatures.getId(), measurements);
			savedVesselFeatures.setMeasurements(measurements);
		}

		return savedVesselFeatures;
	}

	@Override
	public List<VesselFeaturesVO> save(List<VesselFeaturesVO> sources) {
		Preconditions.checkNotNull(sources);

		return sources.stream()
				.map(this::save)
				.collect(Collectors.toList());
	}

	@Override
	public void delete(int id) {
		vesselDao.delete(id);
	}

	@Override
	public void delete(List<Integer> ids) {
		Preconditions.checkNotNull(ids);
		ids.stream()
				.filter(Objects::nonNull)
				.forEach(this::delete);
	}

	/* protected methods */

	void fillDefaultProperties(VesselFeaturesVO parent, MeasurementVO measurement) {
		if (measurement == null) return;

		// Set default value for recorder department and person
		DataBeans.setDefaultRecorderDepartment(measurement, parent.getRecorderDepartment());
		DataBeans.setDefaultRecorderPerson(measurement, parent.getRecorderPerson());

		measurement.setEntityName(VesselPhysicalMeasurement.class.getSimpleName());
	}

}

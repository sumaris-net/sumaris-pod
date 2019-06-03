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
import net.sumaris.core.dao.data.LandingRepository;
import net.sumaris.core.dao.data.MeasurementDao;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.model.data.LandingMeasurement;
import net.sumaris.core.util.Beans;
import net.sumaris.core.util.DataBeans;
import net.sumaris.core.vo.data.DataFetchOptions;
import net.sumaris.core.vo.data.LandingVO;
import net.sumaris.core.vo.data.MeasurementVO;
import net.sumaris.core.vo.filter.LandingFilterVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service("landingService")
public class LandingServiceImpl implements LandingService {

	private static final Logger log = LoggerFactory.getLogger(LandingServiceImpl.class);

	@Autowired
	protected LandingRepository landingRepository;

	@Autowired
	protected MeasurementDao measurementDao;

	@Override
	public List<LandingVO> getAll(int offset, int size) {
		return findAll(null, offset, size, null, null, null);
	}

	@Override
	public List<LandingVO> findAll(LandingFilterVO filter, int offset, int size) {
		return findAll(filter, offset, size, null, null, null);
	}

	@Override
	public List<LandingVO> findAll(LandingFilterVO filter, int offset, int size, String sortAttribute,
                                   SortDirection sortDirection, DataFetchOptions fetchOptions) {

		return landingRepository.findAll(filter, offset, size, sortAttribute, sortDirection, fetchOptions)
				.stream().collect(Collectors.toList());
	}

	@Override
	public Long countByFilter(LandingFilterVO filter) {
		return landingRepository.count(filter);
	}

	@Override
	public LandingVO get(int landingId) {
		return landingRepository.get(landingId);
	}

	@Override
	public LandingVO save(final LandingVO source) {
		Preconditions.checkNotNull(source);
		Preconditions.checkNotNull(source.getProgram(), "Missing program");
		Preconditions.checkArgument(source.getProgram().getId() != null || source.getProgram().getLabel() != null, "Missing program.id or program.label");
		Preconditions.checkNotNull(source.getDateTime(), "Missing dateTime");
		Preconditions.checkNotNull(source.getLocation(), "Missing location");
		Preconditions.checkNotNull(source.getLocation().getId(), "Missing location.id");
		Preconditions.checkNotNull(source.getRecorderDepartment(), "Missing recorderDepartment");
		Preconditions.checkNotNull(source.getRecorderDepartment().getId(), "Missing recorderDepartment.id");

		// Reset control date
		source.setControlDate(null);

		// Save
		LandingVO savedLanding = landingRepository.save(source);

		// Save measurements
		if (savedLanding.getMeasurementValues() != null) {
			measurementDao.saveLandingMeasurementsMap(savedLanding.getId(), savedLanding.getMeasurementValues());
		}
		else {
			List<MeasurementVO> measurements = Beans.getList(savedLanding.getMeasurements());
			measurements.forEach(m -> fillDefaultProperties(savedLanding, m));
			measurements = measurementDao.saveLandingMeasurements(savedLanding.getId(), measurements);
			savedLanding.setMeasurements(measurements);
		}

		return savedLanding;
	}

	@Override
	public List<LandingVO> save(List<LandingVO> landings) {
		Preconditions.checkNotNull(landings);

		return landings.stream()
				.map(this::save)
				.collect(Collectors.toList());
	}

	@Override
	public void delete(int id) {
		landingRepository.deleteById(id);
	}

	@Override
	public void delete(List<Integer> ids) {
		Preconditions.checkNotNull(ids);
		ids.stream()
				.filter(Objects::nonNull)
				.forEach(this::delete);
	}

	@Override
	public LandingVO control(LandingVO landing) {
		Preconditions.checkNotNull(landing);
		Preconditions.checkNotNull(landing.getId());
		Preconditions.checkArgument(landing.getControlDate() == null);

		return landingRepository.control(landing);
	}

	@Override
	public LandingVO validate(LandingVO landing) {
		Preconditions.checkNotNull(landing);
		Preconditions.checkNotNull(landing.getId());
		Preconditions.checkNotNull(landing.getControlDate());
		Preconditions.checkArgument(landing.getValidationDate() == null);

		return landingRepository.validate(landing);
	}

	@Override
	public LandingVO unvalidate(LandingVO landing) {
		Preconditions.checkNotNull(landing);
		Preconditions.checkNotNull(landing.getId());
		Preconditions.checkNotNull(landing.getControlDate());
		Preconditions.checkNotNull(landing.getValidationDate());

		return landingRepository.unvalidate(landing);
	}

	/* protected methods */

	void fillDefaultProperties(LandingVO parent, MeasurementVO measurement) {
		if (measurement == null) return;

		// Set default value for recorder department and person
		DataBeans.setDefaultRecorderDepartment(measurement, parent.getRecorderDepartment());
		DataBeans.setDefaultRecorderPerson(measurement, parent.getRecorderPerson());

		measurement.setEntityName(LandingMeasurement.class.getSimpleName());
	}
}

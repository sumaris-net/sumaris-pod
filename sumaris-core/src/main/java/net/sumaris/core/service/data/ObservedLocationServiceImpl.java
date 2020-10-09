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
import net.sumaris.core.dao.data.observedLocation.ObservedLocationRepository;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.model.data.ObservedLocationMeasurement;
import net.sumaris.core.util.Beans;
import net.sumaris.core.util.DataBeans;
import net.sumaris.core.vo.data.DataFetchOptions;
import net.sumaris.core.vo.data.MeasurementVO;
import net.sumaris.core.vo.data.ObservedLocationVO;
import net.sumaris.core.vo.filter.ObservedLocationFilterVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service("observedLocationService")
public class ObservedLocationServiceImpl implements ObservedLocationService {

	private static final Logger log = LoggerFactory.getLogger(ObservedLocationServiceImpl.class);

	@Autowired
	protected ObservedLocationRepository observedLocationRepository;

	@Autowired
	protected MeasurementDao measurementDao;


	@Override
	public List<ObservedLocationVO> getAll(int offset, int size) {
		return findByFilter(null, offset, size, null, null, null);
	}

	@Override
	public List<ObservedLocationVO> findByFilter(ObservedLocationFilterVO filter, int offset, int size) {
		return findByFilter(filter, offset, size, null, null, null);
	}

	@Override
	public List<ObservedLocationVO> findByFilter(ObservedLocationFilterVO filter, int offset, int size, String sortAttribute,
                                     SortDirection sortDirection, DataFetchOptions fetchOptions) {
		if (filter == null) {
			return observedLocationRepository.findAll(offset, size, sortAttribute, sortDirection, fetchOptions).getContent();
		}

		return observedLocationRepository.findAll(filter, offset, size, sortAttribute, sortDirection, fetchOptions).getContent();
	}

	@Override
	public Long countByFilter(ObservedLocationFilterVO filter) {
		return observedLocationRepository.count(filter);
	}

	@Override
	public ObservedLocationVO get(int observedLocationId) {
		return observedLocationRepository.get(observedLocationId);
	}

	@Override
	public ObservedLocationVO save(final ObservedLocationVO source, final boolean withObservedVessel) {
		Preconditions.checkNotNull(source);
		Preconditions.checkNotNull(source.getProgram(), "Missing program");
		Preconditions.checkArgument(source.getProgram().getId() != null || source.getProgram().getLabel() != null, "Missing program.id or program.label");
		Preconditions.checkNotNull(source.getStartDateTime(), "Missing startDateTime");
		Preconditions.checkNotNull(source.getLocation(), "Missing location");
		Preconditions.checkNotNull(source.getLocation().getId(), "Missing location.id");
		Preconditions.checkNotNull(source.getRecorderDepartment(), "Missing recorderDepartment");
		Preconditions.checkNotNull(source.getRecorderDepartment().getId(), "Missing recorderDepartment.id");

		// Reset control date
		source.setControlDate(null);

		// Save
		ObservedLocationVO savedObservedLocation = observedLocationRepository.save(source);

		// Save measurements
		if (savedObservedLocation.getMeasurementValues() != null) {
			measurementDao.saveObservedLocationMeasurementsMap(savedObservedLocation.getId(), savedObservedLocation.getMeasurementValues());
		}
		else {
			List<MeasurementVO> measurements = Beans.getList(savedObservedLocation.getMeasurements());
			measurements.forEach(m -> fillDefaultProperties(savedObservedLocation, m));
			measurements = measurementDao.saveObservedLocationMeasurements(savedObservedLocation.getId(), measurements);
			savedObservedLocation.setMeasurements(measurements);
		}

		return savedObservedLocation;
	}

	@Override
	public List<ObservedLocationVO> save(List<ObservedLocationVO> observedLocations, final boolean withObservedVessel) {
		Preconditions.checkNotNull(observedLocations);

		return observedLocations.stream()
				.map(t -> save(t, withObservedVessel))
				.collect(Collectors.toList());
	}

	@Override
	public void delete(int id) {
		observedLocationRepository.deleteById(id);
	}

	@Override
	public void delete(List<Integer> ids) {
		Preconditions.checkNotNull(ids);
		ids.stream()
				.filter(Objects::nonNull)
				.forEach(this::delete);
	}

	@Override
	public ObservedLocationVO control(ObservedLocationVO observedLocation) {
		Preconditions.checkNotNull(observedLocation);
		Preconditions.checkNotNull(observedLocation.getId());
		Preconditions.checkArgument(observedLocation.getControlDate() == null);

		return observedLocationRepository.control(observedLocation);
	}

	@Override
	public ObservedLocationVO validate(ObservedLocationVO observedLocation) {
		Preconditions.checkNotNull(observedLocation);
		Preconditions.checkNotNull(observedLocation.getId());
		Preconditions.checkNotNull(observedLocation.getControlDate());
		Preconditions.checkArgument(observedLocation.getValidationDate() == null);

		return observedLocationRepository.validateNoSave(observedLocation); // todo no save !
	}

	@Override
	public ObservedLocationVO unvalidate(ObservedLocationVO observedLocation) {
		Preconditions.checkNotNull(observedLocation);
		Preconditions.checkNotNull(observedLocation.getId());
		Preconditions.checkNotNull(observedLocation.getControlDate());
		Preconditions.checkNotNull(observedLocation.getValidationDate());

		return observedLocationRepository.unvalidateNoSave(observedLocation); // todo no save
	}

	/* protected methods */

	void fillDefaultProperties(ObservedLocationVO parent, MeasurementVO measurement) {
		if (measurement == null) return;

		// Set default value for recorder department and person
		DataBeans.setDefaultRecorderDepartment(measurement, parent.getRecorderDepartment());
		DataBeans.setDefaultRecorderPerson(measurement, parent.getRecorderPerson());

		measurement.setEntityName(ObservedLocationMeasurement.class.getSimpleName());
	}
}

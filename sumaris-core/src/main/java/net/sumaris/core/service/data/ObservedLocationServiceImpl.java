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
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.data.MeasurementDao;
import net.sumaris.core.dao.data.observedLocation.ObservedLocationRepository;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.event.entity.EntityInsertEvent;
import net.sumaris.core.event.entity.EntityUpdateEvent;
import net.sumaris.core.model.administration.programStrategy.ProgramPropertyEnum;
import net.sumaris.core.model.data.DataQualityStatusEnum;
import net.sumaris.core.model.data.ObservedLocation;
import net.sumaris.core.model.data.ObservedLocationMeasurement;
import net.sumaris.core.service.administration.programStrategy.ProgramService;
import net.sumaris.core.util.Beans;
import net.sumaris.core.util.DataBeans;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.administration.programStrategy.ProgramVO;
import net.sumaris.core.vo.data.*;
import net.sumaris.core.vo.filter.LandingFilterVO;
import net.sumaris.core.vo.filter.ObservedLocationFilterVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service("observedLocationService")
@RequiredArgsConstructor
@Slf4j
public class ObservedLocationServiceImpl implements ObservedLocationService {


	protected final ObservedLocationRepository observedLocationRepository;

	protected final MeasurementDao measurementDao;

	protected final ProgramService programService;

	protected final LandingService landingService;

	protected final LandingService landingService;

	private final ApplicationEventPublisher publisher;

	@Override
	public List<ObservedLocationVO> findAll(ObservedLocationFilterVO filter, int offset, int size) {
		return findAll(filter, offset, size, null, null, null);
	}

	@Override
	public List<ObservedLocationVO> findAll(ObservedLocationFilterVO filter, int offset, int size, String sortAttribute,
											SortDirection sortDirection, DataFetchOptions fetchOptions) {
		return observedLocationRepository.findAll(ObservedLocationFilterVO.nullToEmpty(filter), offset, size, sortAttribute, sortDirection, fetchOptions);
	}

	@Override
	public List<ObservedLocationVO> findAll(ObservedLocationFilterVO filter, Page page, DataFetchOptions fetchOptions) {
		return observedLocationRepository.findAll(ObservedLocationFilterVO.nullToEmpty(filter), page, fetchOptions);
	}

	@Override
	public Long count(ObservedLocationFilterVO filter) {
		filter = ObservedLocationFilterVO.nullToEmpty(filter);
		return observedLocationRepository.count(filter);
	}

	@Override
	public ObservedLocationVO get(int observedLocationId) {
		return observedLocationRepository.get(observedLocationId);
	}

	@Override
	public ObservedLocationVO save(final ObservedLocationVO source, ObservedLocationSaveOptions options) {
		checkCanSave(source);

		// Init options, if empty
		options = ObservedLocationSaveOptions.defaultIfEmpty(options);

		// Reset control date
		source.setControlDate(null);

		boolean isNew = source.getId() == null;

		// Save
		ObservedLocationVO result = observedLocationRepository.save(source);

		// Save measurements
		if (result.getMeasurementValues() != null) {
			measurementDao.saveObservedLocationMeasurementsMap(result.getId(), result.getMeasurementValues());
		}
		else {
			List<MeasurementVO> measurements = Beans.getList(result.getMeasurements());
			measurements.forEach(m -> fillDefaultProperties(result, m));
			measurements = measurementDao.saveObservedLocationMeasurements(result.getId(), measurements);
			result.setMeasurements(measurements);
		}

		// Save landings (only if asked)
		if (options.getWithLanding()) {
			List<LandingVO> landings = Beans.getList(source.getLandings());
			fillDefaultProperties(result, landings);
			List<LandingVO> savedLandings = landingService.saveAllByObservedLocationId(result.getId(), landings);
			result.setLandings(savedLandings);
		}

		// Publish event
		if (isNew) {
			publisher.publishEvent(new EntityInsertEvent(result.getId(), ObservedLocation.class.getSimpleName(), result));
		} else {
			publisher.publishEvent(new EntityUpdateEvent(result.getId(), ObservedLocation.class.getSimpleName(), result));
		}

		return result;
	}

	@Override
	public List<ObservedLocationVO> save(List<ObservedLocationVO> observedLocations, ObservedLocationSaveOptions options) {
		Preconditions.checkNotNull(observedLocations);

		return observedLocations.stream()
				.map(t -> save(t, options))
				.collect(Collectors.toList());
	}

	@Override
	public void delete(int id) {

		// Delete linked landings
		landingService.deleteAllByObservedLocationId(id);

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
	public ObservedLocationVO control(@NonNull ObservedLocationVO observedLocation, DataControlOptions options) {
		Preconditions.checkNotNull(observedLocation.getId());
		Preconditions.checkArgument(observedLocation.getValidationDate() == null);

		DataControlOptions controlOptions = DataControlOptions.defaultIfEmpty(options);

		observedLocation = observedLocationRepository.control(observedLocation);

		if (controlOptions.getWithChildren()) {
			// Get if observedLocation has a meta program
			String programLabel = observedLocation.getProgram().getLabel();
			String subProgramLabel = programService.getPropertyValueByProgramLabel(
					programLabel,
					ProgramPropertyEnum.OBSERVED_LOCATION_AGGREGATED_LANDINGS_PROGRAM);

			// Control sub observed locations
			if (StringUtils.isNoneBlank(subProgramLabel)) {
				findAll(ObservedLocationFilterVO.builder()
								.programLabel(subProgramLabel)
								.startDate(observedLocation.getStartDateTime())
								.endDate(observedLocation.getEndDateTime())
								.dataQualityStatus(new DataQualityStatusEnum[]{DataQualityStatusEnum.MODIFIED, DataQualityStatusEnum.CONTROLLED})
								.build(), Page.builder().offset(0).size(1000).build(),
						DataFetchOptions.MINIMAL)
						.forEach((i) -> this.control(i, controlOptions));
			}

			// Control children landings
			else {
				landingService.findAll(LandingFilterVO.builder()
										.observedLocationId(observedLocation.getId())
										.dataQualityStatus(new DataQualityStatusEnum[]{DataQualityStatusEnum.MODIFIED, DataQualityStatusEnum.CONTROLLED})
										.build(),
								Page.builder().offset(0).size(1000).build(),
								LandingFetchOptions.MINIMAL)
						.forEach((i) -> landingService.control(i, controlOptions));
			}
		}

		// Publish event
		publisher.publishEvent(new EntityUpdateEvent(result.getId(), ObservedLocation.class.getSimpleName(), observedLocation));

		return observedLocation;
	}

	@Override
	public ObservedLocationVO validate(@NonNull ObservedLocationVO observedLocation, DataValidateOptions options) {
		Preconditions.checkNotNull(observedLocation.getId());
		Preconditions.checkNotNull(observedLocation.getControlDate());
		Preconditions.checkArgument(observedLocation.getValidationDate() == null);

		DataValidateOptions validateOptions = DataValidateOptions.defaultIfEmpty(options);

		observedLocation = observedLocationRepository.validate(observedLocation);

		if (validateOptions.getWithChildren()) {
			// Get if observedLocation has a meta program
			String programLabel = observedLocation.getProgram().getLabel();
			String subProgramLabel = programService.getPropertyValueByProgramLabel(
					programLabel,
					ProgramPropertyEnum.OBSERVED_LOCATION_AGGREGATED_LANDINGS_PROGRAM);

			// Validate sub observed locations
			if (StringUtils.isNoneBlank(subProgramLabel)) {
				findAll(ObservedLocationFilterVO.builder()
								.programLabel(subProgramLabel)
								.startDate(observedLocation.getStartDateTime())
								.endDate(observedLocation.getEndDateTime())
								.dataQualityStatus(new DataQualityStatusEnum[]{DataQualityStatusEnum.CONTROLLED})
								.build(), Page.builder().offset(0).size(1000).build(),
						DataFetchOptions.MINIMAL)
						.forEach((i) -> this.validate(i, validateOptions));
			}

			// Validate children landings
			else {
				landingService.findAll(LandingFilterVO.builder()
										.observedLocationId(observedLocation.getId())
										.dataQualityStatus(new DataQualityStatusEnum[]{DataQualityStatusEnum.CONTROLLED})
										.build(),
								Page.builder().offset(0).size(1000).build(),
								LandingFetchOptions.MINIMAL)
						.forEach((i) -> landingService.validate(i, validateOptions));
			}
		}

		// Publish event
		publisher.publishEvent(new EntityUpdateEvent(result.getId(), ObservedLocation.class.getSimpleName(), observedLocation));

		return observedLocation;
	}

	@Override
	public ObservedLocationVO unvalidate(@NonNull ObservedLocationVO observedLocation, DataValidateOptions options) {
		Preconditions.checkNotNull(observedLocation.getId());
		Preconditions.checkNotNull(observedLocation.getControlDate());
		Preconditions.checkNotNull(observedLocation.getValidationDate());

		DataValidateOptions validateOptions = DataValidateOptions.defaultIfEmpty(options);

		observedLocation = observedLocationRepository.unValidate(observedLocation);

		if (validateOptions.getWithChildren()) {
			String programLabel = observedLocation.getProgram().getLabel();

			// Get if observedLocation has a meta program
			String subProgramLabel = programService.getPropertyValueByProgramLabel(
					programLabel,
					ProgramPropertyEnum.OBSERVED_LOCATION_AGGREGATED_LANDINGS_PROGRAM);

			// Unvalidate sub observed locations
			if (StringUtils.isNoneBlank(subProgramLabel)) {
				findAll(ObservedLocationFilterVO.builder()
								.programLabel(subProgramLabel)
								.startDate(observedLocation.getStartDateTime())
								.endDate(observedLocation.getEndDateTime())
								.dataQualityStatus(new DataQualityStatusEnum[]{DataQualityStatusEnum.VALIDATED, DataQualityStatusEnum.QUALIFIED})
								.build(), Page.builder().offset(0).size(1000).build(),
						DataFetchOptions.MINIMAL)
						.forEach((i) -> this.unvalidate(i, validateOptions));
			}

			// Unvalidate children landings
			else {
				landingService.findAll(LandingFilterVO.builder()
										.observedLocationId(observedLocation.getId())
										.dataQualityStatus(new DataQualityStatusEnum[]{DataQualityStatusEnum.VALIDATED, DataQualityStatusEnum.QUALIFIED})
										.build(),
								Page.builder().offset(0).size(1000).build(),
								LandingFetchOptions.MINIMAL)
						.forEach(l -> landingService.unvalidate(l, validateOptions));
			}
		}


		// Publish event
		publisher.publishEvent(new EntityUpdateEvent(result.getId(), ObservedLocation.class.getSimpleName(), observedLocation));

		return observedLocation;
	}

	@Override
	public ObservedLocationVO qualify(ObservedLocationVO observedLocation) {
		Preconditions.checkNotNull(observedLocation);
		Preconditions.checkNotNull(observedLocation.getId());
		Preconditions.checkNotNull(observedLocation.getControlDate());
		Preconditions.checkNotNull(observedLocation.getValidationDate());

		observedLocation = observedLocationRepository.qualify(observedLocation);

		// Publish event
		publisher.publishEvent(new EntityUpdateEvent(result.getId(), ObservedLocation.class.getSimpleName(), observedLocation));

		return observedLocation;
	}

	/* -- protected methods -- */

	protected void checkCanSave(ObservedLocationVO source) {
		Preconditions.checkNotNull(source);
		Preconditions.checkNotNull(source.getProgram(), "Missing program");
		Preconditions.checkArgument(source.getProgram().getId() != null || source.getProgram().getLabel() != null, "Missing program.id or program.label");
		Preconditions.checkNotNull(source.getStartDateTime(), "Missing startDateTime");
		Preconditions.checkNotNull(source.getLocation(), "Missing location");
		Preconditions.checkNotNull(source.getLocation().getId(), "Missing location.id");
		Preconditions.checkNotNull(source.getRecorderDepartment(), "Missing recorderDepartment");
		Preconditions.checkNotNull(source.getRecorderDepartment().getId(), "Missing recorderDepartment.id");
	}

	protected void fillDefaultProperties(ObservedLocationVO parent, MeasurementVO measurement) {
		if (measurement == null) return;

		// Set default value for recorder department and person
		DataBeans.setDefaultRecorderDepartment(measurement, parent.getRecorderDepartment());
		DataBeans.setDefaultRecorderPerson(measurement, parent.getRecorderPerson());

		measurement.setEntityName(ObservedLocationMeasurement.class.getSimpleName());
	}

	protected void fillDefaultProperties(ObservedLocationVO parent, List<LandingVO> sources) {
		// Program
		ProgramVO program = new ProgramVO();
		program.setId(parent.getProgram().getId());

		// Date/Time
		Date defaultLandingDateTime = parent.getStartDateTime() != null ? parent.getStartDateTime() : parent.getEndDateTime();

		// Apply to all sources
		Beans.getStream(sources).forEach(source -> {
			source.setProgram(program);
			if (source.getDateTime() == null) source.setDateTime(defaultLandingDateTime);
			source.setLocation(parent.getLocation());
			DataBeans.setDefaultRecorderDepartment(source, parent.getRecorderDepartment());
		});
	}
}

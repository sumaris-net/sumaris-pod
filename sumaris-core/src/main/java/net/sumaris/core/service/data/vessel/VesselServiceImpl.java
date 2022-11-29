package net.sumaris.core.service.data.vessel;

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
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.data.MeasurementDao;
import net.sumaris.core.dao.data.vessel.VesselFeaturesRepository;
import net.sumaris.core.dao.data.vessel.VesselRegistrationPeriodRepository;
import net.sumaris.core.dao.data.vessel.VesselRepository;
import net.sumaris.core.dao.data.vessel.VesselSnapshotRepository;
import net.sumaris.core.model.data.VesselPhysicalMeasurement;
import net.sumaris.core.model.referential.StatusEnum;
import net.sumaris.core.util.Beans;
import net.sumaris.core.util.DataBeans;
import net.sumaris.core.vo.data.*;
import net.sumaris.core.vo.data.vessel.VesselFetchOptions;
import net.sumaris.core.vo.filter.VesselFilterVO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service("vesselService2")
@Slf4j
public class VesselServiceImpl implements VesselService {

	@Autowired
	protected VesselRepository vesselRepository;

	@Autowired
	protected VesselSnapshotRepository vesselSnapshotRepository;

	@Autowired
	protected VesselFeaturesRepository vesselFeaturesRepository;

	@Autowired
	protected VesselRegistrationPeriodRepository vesselRegistrationPeriodRepository;

	@Autowired
	protected MeasurementDao measurementDao;

	@Override
	public List<VesselSnapshotVO> findAllSnapshots(@NonNull VesselFilterVO filter,
												   net.sumaris.core.dao.technical.Page page,
												   VesselFetchOptions fetchOptions) {
		return vesselSnapshotRepository.findAll(filter, page, fetchOptions);
	}

	@Override
	public Long countSnapshotsByFilter(@NonNull VesselFilterVO filter) {
		return vesselSnapshotRepository.count(filter);
	}

	@Override
	public List<VesselVO> findAll(@NonNull VesselFilterVO filter,
								  @NonNull net.sumaris.core.dao.technical.Page page,
								  @NonNull VesselFetchOptions fetchOptions) {

		// If expected a date: use today
		boolean needDate = fetchOptions.isWithVesselFeatures() || fetchOptions.isWithVesselRegistrationPeriod();
		if (needDate && filter.getStartDate() == null && filter.getEndDate() == null) {
			filter.setDate(new Date());
		}

		return vesselRepository.findAll(
			filter,
			page,
			fetchOptions);
	}

	@Override
	public Long countByFilter(@NonNull VesselFilterVO filter) {
		return vesselRepository.count(filter);
	}

	@Override
	public VesselVO get(int id) {
		return vesselRepository.get(id);
	}

	@Override
	public VesselSnapshotVO getSnapshotByIdAndDate(int vesselId, Date date) {
		return vesselSnapshotRepository.getByVesselIdAndDate(vesselId, date, VesselFetchOptions.DEFAULT)
			.orElseGet(() -> {
				VesselSnapshotVO unknownVessel = new VesselSnapshotVO();
				unknownVessel.setId(vesselId);
				unknownVessel.setName("Unknown vessel " + vesselId); // TODO remove string
				return unknownVessel;
			});
	}

	@Override
	public Page<VesselFeaturesVO> getFeaturesByVesselId(int vesselId, Pageable pageable, DataFetchOptions fetchOptions) {
		return vesselFeaturesRepository
			.findAll(VesselFilterVO.builder().vesselId(vesselId).build(), pageable, fetchOptions);
	}

	@Override
	public Page<VesselRegistrationPeriodVO> getRegistrationPeriodsByVesselId(int vesselId, Pageable pageable) {
		return vesselRegistrationPeriodRepository.findAll(
			VesselFilterVO.builder().vesselId(vesselId).build(),
			pageable);
	}

	@Override
	public void replaceTemporaryVessel(List<Integer> temporaryVesselIds, int targetVesselId) {
		if (CollectionUtils.isEmpty(temporaryVesselIds)) {
			log.warn("No temporary vessel ids provided");
			return;
		}

		// Load and check vessels
		temporaryVesselIds.forEach(temporaryVesselId -> {
			VesselVO temporaryVessel = get(temporaryVesselId);
			Preconditions.checkNotNull(temporaryVessel, "Temporary vessel (id={}) should exists", temporaryVesselId);
			Preconditions.checkArgument(Objects.equals(temporaryVessel.getStatusId(), StatusEnum.TEMPORARY.getId()), "Vessel (id={}) to replace must be temporary", temporaryVesselId);
		});
		VesselVO validVessel = get(targetVesselId);
		Preconditions.checkNotNull(validVessel);
		Preconditions.checkArgument(Objects.equals(validVessel.getStatusId(), StatusEnum.ENABLE.getId()), "Replacement vessel must be enabled");

		temporaryVesselIds.forEach(temporaryVesselId -> {
			log.info("Vessel replacement from (id={}) to (id={})", temporaryVesselId, targetVesselId);

			int nbTripsUpdated = vesselRepository.updateTrips(temporaryVesselId, targetVesselId);
			log.info("nb trips updated: {}", nbTripsUpdated);
			int nbLandingsUpdated = vesselRepository.updateLandings(temporaryVesselId, targetVesselId);
			log.info("nb landings updated: {}", nbLandingsUpdated);
			int nbSalesUpdated = vesselRepository.updateSales(temporaryVesselId, targetVesselId);
			log.info("nb sales updated: {}", nbSalesUpdated);

			// Delete temporary vessel
			delete(temporaryVesselId);
		});

	}

	@Override
	public List<VesselVO> save(List<VesselVO> sources) {
		Preconditions.checkNotNull(sources);

		// special case if a vessel is saved twice: it means a features or registration change
		if (sources.size() == 2 && Objects.equals(sources.get(0).getId(), sources.get(1).getId())) {
			List<VesselVO> result = new ArrayList<>();
			// save the first vo normally
			result.add(save(sources.get(0)));
			// save the second without checking update date
			result.add(save(sources.get(1), false));
			return result;
		}

		return sources.stream()
			.map(this::save)
			.collect(Collectors.toList());
	}

	@Override
	public VesselVO save(VesselVO source) {
		return save(source, true);
	}

	@Override
	public void delete(int id) {

		// TODO: check if there is data on this vessel, and throw exception
		vesselRepository.deleteById(id);
	}

	@Override
	public void delete(List<Integer> ids) {
		Preconditions.checkNotNull(ids);

		ids.stream()
			.filter(Objects::nonNull)
			.forEach(this::delete);
	}

	/* protected methods */

    protected VesselVO save(VesselVO source, boolean checkUpdateDate) {
        Preconditions.checkNotNull(source);
        Preconditions.checkNotNull(source.getRecorderDepartment(), "Missing recorderDepartment");
        Preconditions.checkNotNull(source.getRecorderDepartment().getId(), "Missing recorderDepartment.id");
        Preconditions.checkNotNull(source.getVesselType(), "Missing vesselId or vesselTypeId");

        if (source.getVesselFeatures() != null) {
			Preconditions.checkNotNull(source.getVesselFeatures().getBasePortLocation(), "Missing features basePortLocation");
			Preconditions.checkNotNull(source.getVesselFeatures().getBasePortLocation().getId(), "Missing features basePortLocation.id");
            Preconditions.checkNotNull(source.getVesselFeatures().getStartDate(), "Missing features start date");
            Preconditions.checkArgument(StringUtils.isNotBlank(source.getVesselFeatures().getExteriorMarking()), "Missing features exterior marking");
        }

        if (source.getVesselRegistrationPeriod() != null) {
            Preconditions.checkArgument(StringUtils.isNotBlank(source.getVesselRegistrationPeriod().getRegistrationCode()), "Missing registration code");
            Preconditions.checkNotNull(source.getVesselRegistrationPeriod().getRegistrationLocation().getId(), "Missing registration location");
        }

        VesselVO savedVessel = vesselRepository.save(source, checkUpdateDate, true);

        if (savedVessel.getVesselFeatures() != null) {
            VesselFeaturesVO vesselFeatures = savedVessel.getVesselFeatures();
            vesselFeatures.setVessel(savedVessel);
            VesselFeaturesVO savedVesselFeatures = vesselFeaturesRepository.save(vesselFeatures, checkUpdateDate, true);

            // Save measurements
            if (savedVesselFeatures.getMeasurementValues() != null) {
                measurementDao.saveVesselPhysicalMeasurementsMap(savedVesselFeatures.getId(), savedVesselFeatures.getMeasurementValues());
            } else {
                List<MeasurementVO> measurements = Beans.getList(savedVesselFeatures.getMeasurements());
                measurements.forEach(m -> fillDefaultProperties(savedVesselFeatures, m));
                measurements = measurementDao.saveVesselPhysicalMeasurements(savedVesselFeatures.getId(), measurements);
                savedVesselFeatures.setMeasurements(measurements);
            }
        }

		if (savedVessel.getVesselRegistrationPeriod() != null) {
			VesselRegistrationPeriodVO vesselRegistrationPeriod = savedVessel.getVesselRegistrationPeriod();
			vesselRegistrationPeriod.setVessel(savedVessel);

			vesselRegistrationPeriodRepository.save(vesselRegistrationPeriod, checkUpdateDate, true);
		}

        return savedVessel;
    }

	protected void fillDefaultProperties(VesselFeaturesVO parent, MeasurementVO measurement) {
		if (measurement == null) return;

		// Set default value for recorder department and person
		DataBeans.setDefaultRecorderDepartment(measurement, parent.getRecorderDepartment());
		DataBeans.setDefaultRecorderPerson(measurement, parent.getRecorderPerson());

		measurement.setEntityName(VesselPhysicalMeasurement.class.getSimpleName());
	}
}

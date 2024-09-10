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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.data.MeasurementDao;
import net.sumaris.core.dao.data.vessel.*;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.model.administration.programStrategy.ProgramEnum;
import net.sumaris.core.model.data.VesselPhysicalMeasurement;
import net.sumaris.core.model.referential.StatusEnum;
import net.sumaris.core.util.Beans;
import net.sumaris.core.util.DataBeans;
import net.sumaris.core.vo.data.*;
import net.sumaris.core.vo.data.vessel.VesselFetchOptions;
import net.sumaris.core.vo.data.vessel.VesselOwnerVO;
import net.sumaris.core.vo.filter.VesselFilterVO;
import net.sumaris.core.vo.filter.VesselOwnerFilterVO;
import net.sumaris.core.vo.filter.VesselRegistrationFilterVO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service("vesselService")
@RequiredArgsConstructor
@Slf4j
public class VesselServiceImpl implements VesselService {

	protected final VesselRepository vesselRepository;

	protected final VesselFeaturesRepository vesselFeaturesRepository;

	protected final VesselRegistrationPeriodRepository vesselRegistrationPeriodRepository;

	protected final VesselOwnerRepository vesselOwnerRepository;

	protected final VesselOwnerPeriodRepository vesselOwnerPeriodRepository;

	protected final MeasurementDao measurementDao;

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
	public long countByFilter(@NonNull VesselFilterVO filter) {
		return vesselRepository.count(filter);
	}

	@Override
	public VesselVO get(int id) {
		return vesselRepository.get(id);
	}

	@Override
	public List<VesselFeaturesVO> findFeaturesByVesselId(int vesselId, Page page, DataFetchOptions fetchOptions) {
		return vesselFeaturesRepository
			.findAll(VesselFilterVO.builder().vesselId(vesselId).build(), page, fetchOptions);
	}

	@Override
	public long countFeaturesByVesselId(int vesselId) {
		return vesselFeaturesRepository
				.count(VesselFilterVO.builder().vesselId(vesselId).build());
	}

	@Override
	public List<VesselRegistrationPeriodVO> findRegistrationPeriodsByVesselId(int vesselId, Page page) {
		return vesselRegistrationPeriodRepository.findAll(
			VesselRegistrationFilterVO.builder()
					.vesselId(vesselId)
					.build(),
			page);
	}

	@Override
	public long countRegistrationPeriodsByVesselId(int vesselId) {
		return vesselRegistrationPeriodRepository.count(
				VesselRegistrationFilterVO.builder()
						.vesselId(vesselId)
						.build());
	}


	@Override
	public List<VesselRegistrationPeriodVO> findRegistrationPeriodsByFilter(VesselRegistrationFilterVO filter, Page page) {
		return vesselRegistrationPeriodRepository.findAll(filter, page);
	}

	@Override
	public List<VesselOwnerPeriodVO> findOwnerPeriodsByFilter(VesselOwnerFilterVO filter, Page page) {
		return vesselOwnerPeriodRepository.findAll(filter, page);
	}

	public VesselOwnerVO getVesselOwner(int id) {
		return vesselOwnerRepository.get(id);
	}

	@Override
	public List<VesselOwnerPeriodVO> findOwnerPeriodsByVesselId(int vesselId, Page page) {
		return vesselOwnerPeriodRepository.findAll(
				VesselOwnerFilterVO.builder()
						.vesselId(vesselId)
						.programLabel(ProgramEnum.SIH.getLabel())
						.build(),
				page);
	}

	@Override
	public long countOwnerPeriodsByVesselId(int vesselId) {
		return vesselOwnerPeriodRepository.count(
				VesselOwnerFilterVO.builder()
						.vesselId(vesselId)
						.programLabel(ProgramEnum.SIH.getLabel())
						.build());
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

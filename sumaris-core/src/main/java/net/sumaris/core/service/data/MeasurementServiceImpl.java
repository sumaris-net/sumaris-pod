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


import net.sumaris.core.dao.data.MeasurementDao;
import net.sumaris.core.vo.data.MeasurementVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service("measurementService")
public class MeasurementServiceImpl implements MeasurementService {

	private static final Logger log = LoggerFactory.getLogger(MeasurementServiceImpl.class);

	@Autowired
	protected MeasurementDao measurementDao;

    @Override
    public List<MeasurementVO> getTripVesselUseMeasurements(int tripId) {
        return measurementDao.getTripVesselUseMeasurements(tripId);
    }

	@Override
	public Map<Integer, String> getTripVesselUseMeasurementsMap(int tripId) {
		return measurementDao.getTripVesselUseMeasurementsMap(tripId);
	}

	@Override
	public List<MeasurementVO> getPhysicalGearMeasurements(int physicalGearId) {
		return measurementDao.getPhysicalGearMeasurements(physicalGearId);
	}

	@Override
	public Map<Integer, String> getPhysicalGearMeasurementsMap(int physicalGearId) {
		return measurementDao.getPhysicalGearMeasurementsMap(physicalGearId);
	}

	@Override
	public List<MeasurementVO> getOperationVesselUseMeasurements(int operationId) {
		return measurementDao.getOperationVesselUseMeasurements(operationId);
	}

	@Override
	public List<MeasurementVO> getOperationGearUseMeasurements(int operationId) {
		return measurementDao.getOperationGearUseMeasurements(operationId);
	}

	@Override
	public Map<Integer, String> getOperationGearUseMeasurementsMap(int operationId) {
		return measurementDao.getOperationGearUseMeasurementsMap(operationId);
	}

	@Override
	public Map<Integer, String> getOperationVesselUseMeasurementsMap(int operationId) {
		return measurementDao.getOperationVesselUseMeasurementsMap(operationId);
	}

	@Override
	public List<MeasurementVO> getSampleMeasurements(int sampleId) {
		return measurementDao.getSampleMeasurements(sampleId);
	}

	@Override
	public Map<Integer, String> getSampleMeasurementsMap(int sampleId) {
		return measurementDao.getSampleMeasurementsMap(sampleId);
	}

	@Override
	public Map<Integer, String> getBatchSortingMeasurementsMap(int batchId) {
		return measurementDao.getBatchSortingMeasurementsMap(batchId);
	}

	@Override
	public List<MeasurementVO> getBatchSortingMeasurements(int batchId) {
		return measurementDao.getBatchSortingMeasurements(batchId);
	}

	@Override
	public Map<Integer, String> getBatchQuantificationMeasurementsMap(int batchId) {
		return measurementDao.getBatchQuantificationMeasurementsMap(batchId);
	}

	@Override
	public List<MeasurementVO> getBatchQuantificationMeasurements(int batchId) {
		return measurementDao.getBatchQuantificationMeasurements(batchId);
	}

	@Override
	public Map<Integer, String> getProductSortingMeasurementsMap(int productId) {
		return measurementDao.getProductSortingMeasurementsMap(productId);
	}

	@Override
	public Map<Integer, String> getProductQuantificationMeasurementsMap(int productId) {
		return measurementDao.getProductQuantificationMeasurementsMap(productId);
	}

	@Override
	public List<MeasurementVO> getObservedLocationMeasurements(int observedLocationId) {
		return measurementDao.getObservedLocationMeasurements(observedLocationId);
	}

	@Override
	public Map<Integer, String> getObservedLocationMeasurementsMap(int observedLocationId) {
		return measurementDao.getObservedLocationMeasurementsMap(observedLocationId);
	}

	@Override
	public List<MeasurementVO> getSaleMeasurements(int saleId) {
		return measurementDao.getSaleMeasurements(saleId);
	}

	@Override
	public Map<Integer, String> getSaleMeasurementsMap(int saleId) {
		return measurementDao.getSaleMeasurementsMap(saleId);
	}

	@Override
	public Map<Integer, String> getLandingMeasurementsMap(int landingId) {
		return measurementDao.getLandingMeasurementsMap(landingId);
	}

	@Override
	public List<MeasurementVO> getVesselFeaturesMeasurements(int vesselFeaturesId) {
		return measurementDao.getVesselFeaturesMeasurements(vesselFeaturesId);
	}

	@Override
	public Map<Integer, String> getVesselFeaturesMeasurementsMap(int vesselFeaturesId) {
		return measurementDao.getVesselFeaturesMeasurementsMap(vesselFeaturesId);
	}
}

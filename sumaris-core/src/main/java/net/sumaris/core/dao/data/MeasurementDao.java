package net.sumaris.core.dao.data;

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


import net.sumaris.core.model.data.IDataEntity;
import net.sumaris.core.model.data.IMeasurementEntity;
import net.sumaris.core.vo.data.MeasurementVO;

import java.util.List;
import java.util.Map;

public interface MeasurementDao {


    <T extends IMeasurementEntity> MeasurementVO toMeasurementVO(T measurement);

    <T extends IMeasurementEntity> List<MeasurementVO> saveMeasurements(
            Class<? extends IMeasurementEntity> entityClass,
            List<MeasurementVO> sources,
            List<T> target,
            IDataEntity<?> parent);

    // Trip
    List<MeasurementVO> getTripVesselUseMeasurements(int tripId);
    Map<Integer, String> getTripVesselUseMeasurementsMap(int tripId);
    List<MeasurementVO> saveTripVesselUseMeasurements(int tripId, List<MeasurementVO> sources);
    Map<Integer, String> saveTripMeasurementsMap(int tripId, Map<Integer, String> sources);

    // Physical gear
    List<MeasurementVO> getPhysicalGearMeasurements(int physicalGearId);
    Map<Integer, String> getPhysicalGearMeasurementsMap(int physicalGearId);
    List<MeasurementVO> savePhysicalGearMeasurements(int physicalGearId, List<MeasurementVO> sources);
    Map<Integer, String> savePhysicalGearMeasurementsMap(int physicalGearId, Map<Integer, String> sources);

    // Operation
    List<MeasurementVO> getOperationVesselUseMeasurements(int operationId);
    List<MeasurementVO> getOperationGearUseMeasurements(int operationId);
    Map<Integer, String> getOperationVesselUseMeasurementsMap(int operationId);
    Map<Integer, String> getOperationGearUseMeasurementsMap(int operationId);
    List<MeasurementVO> saveOperationVesselUseMeasurements(int operationId, List<MeasurementVO> sources);
    List<MeasurementVO> saveOperationGearUseMeasurements(int operationId, List<MeasurementVO> sources);
    Map<Integer, String> saveOperationVesselUseMeasurementsMap(int operationId, Map<Integer, String> sources);
    Map<Integer, String> saveOperationGearUseMeasurementsMap(int operationId, Map<Integer, String> sources);

    // Observed location
    List<MeasurementVO> getObservedLocationMeasurements(int observedLocationId);
    Map<Integer, String> getObservedLocationMeasurementsMap(int observedLocationId);
    List<MeasurementVO> saveObservedLocationMeasurements(int observedLocationId, List<MeasurementVO> sources);
    Map<Integer, String> saveObservedLocationMeasurementsMap(final int observedLocationId, Map<Integer, String> sources);

    // Sale
    List<MeasurementVO> saveSaleMeasurements(int saleId, List<MeasurementVO> sources);
    Map<Integer, String> saveSaleMeasurementsMap(final int saleId, Map<Integer, String> sources);

    // Landing
    List<MeasurementVO> saveLandingMeasurements(int landingId, List<MeasurementVO> sources);
    Map<Integer, String> saveLandingMeasurementsMap(final int landingId, Map<Integer, String> sources);

    // Sample
    List<MeasurementVO> getSampleMeasurements(int sampleId);
    Map<Integer, String> getSampleMeasurementsMap(int sampleId);
    List<MeasurementVO> saveSampleMeasurements(int sampleId, List<MeasurementVO> sources);
    Map<Integer, String> saveSampleMeasurementsMap(final int sampleId, Map<Integer, String> sources);

    // Batch
    Map<Integer, String> getBatchSortingMeasurementsMap(int batchId);
    Map<Integer, String> getBatchQuantificationMeasurementsMap(int batchId);
    List<MeasurementVO> saveBatchSortingMeasurements(int sampleId, List<MeasurementVO> sources);
    List<MeasurementVO> saveBatchQuantificationMeasurements(int sampleId, List<MeasurementVO> sources);
    Map<Integer, String> saveBatchSortingMeasurementsMap(final int batchId, Map<Integer, String> sources);
    Map<Integer, String> saveBatchQuantificationMeasurementsMap(final int batchId, Map<Integer, String> sources);

    // Vessel
    List<MeasurementVO> getVesselFeaturesMeasurements(int vesselFeaturesId);
    Map<Integer, String> getVesselFeaturesMeasurementsMap(int observedLocationId);
}

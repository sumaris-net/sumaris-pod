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


import net.sumaris.core.model.data.IMeasurementEntity;
import net.sumaris.core.vo.data.MeasurementVO;

import java.util.List;
import java.util.Map;

public interface MeasurementDao {

    List<MeasurementVO> getVesselUseMeasurementsByTripId(int tripId);

    List<MeasurementVO> getPhysicalGearMeasurements(int physicalGearId);

    List<MeasurementVO> getVesselUseMeasurementsByOperationId(int operationId);

    List<MeasurementVO> getGearUseMeasurementsByOperationId(int operationId);

    List<MeasurementVO> getSampleMeasurements(int sampleId);

    Map<Integer, String> getSampleMeasurementsMap(int sampleId);

    Map<Integer, String> getBatchSortingMeasurementsMap(int batchId);

    Map<Integer, String> getBatchQuantificationMeasurementsMap(int batchId);


    <T extends IMeasurementEntity> MeasurementVO toMeasurementVO(T measurement);

    <T extends IMeasurementEntity> List<MeasurementVO> saveMeasurements(
            Class<? extends IMeasurementEntity> entityClass,
            List<MeasurementVO> sources,
            List<T> target);

    List<MeasurementVO> saveVesselUseMeasurementsByTripId(int tripId, List<MeasurementVO> sources);

    List<MeasurementVO> savePhysicalGearMeasurementByPhysicalGearId(int physicalGearId, List<MeasurementVO> sources);

    List<MeasurementVO> saveVesselUseMeasurementsByOperationId(int operationId, List<MeasurementVO> sources);

    List<MeasurementVO> saveGearUseMeasurementsByOperationId(int operationId, List<MeasurementVO> sources);

    // Observed location
    List<MeasurementVO> saveObservedLocationMeasurements(int observedLocationId, List<MeasurementVO> sources);
    Map<Integer, String> saveObservedLocationMeasurementsMap(final int observedLocationId, Map<Integer, String> sources);

    // Sale
    List<MeasurementVO> saveSaleMeasurements(int saleId, List<MeasurementVO> sources);
    Map<Integer, String> saveSaleMeasurementsMap(final int saleId, Map<Integer, String> sources);

    // Sample
    List<MeasurementVO> saveSampleMeasurements(int sampleId, List<MeasurementVO> sources);

    Map<Integer, String> saveSampleMeasurementsMap(final int sampleId, Map<Integer, String> sources);

    // Batch
    List<MeasurementVO> saveBatchSortingMeasurements(int sampleId, List<MeasurementVO> sources);

    List<MeasurementVO> saveBatchQuantificationMeasurements(int sampleId, List<MeasurementVO> sources);

    Map<Integer, String> saveBatchSortingMeasurementsMap(final int batchId, Map<Integer, String> sources);

    Map<Integer, String> saveBatchQuantificationMeasurementsMap(final int batchId, Map<Integer, String> sources);
}

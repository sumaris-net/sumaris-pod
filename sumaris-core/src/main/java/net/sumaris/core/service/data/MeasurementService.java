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


import net.sumaris.core.vo.data.MeasurementVO;
import net.sumaris.core.vo.data.QuantificationMeasurementVO;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * @author BLA
 * 
 *    Service in charge of measurements
 * 
 */
@Transactional
public interface MeasurementService {

    @Transactional(readOnly = true)
    List<MeasurementVO> getTripVesselUseMeasurements(int tripId);

    @Transactional(readOnly = true)
    Map<Integer, String> getTripVesselUseMeasurementsMap(int tripId);

    @Transactional(readOnly = true)
    List<MeasurementVO> getPhysicalGearMeasurements(int physicalGearId);

    @Transactional(readOnly = true)
    Map<Integer, String> getPhysicalGearMeasurementsMap(int physicalGearId);

    @Transactional(readOnly = true)
    List<MeasurementVO> getOperationVesselUseMeasurements(int operationId);

    @Transactional(readOnly = true)
    List<MeasurementVO> getOperationVesselUseMeasurements(int operationId, List<Integer> pmfmIds);

    @Transactional(readOnly = true)
    List<MeasurementVO> getOperationGearUseMeasurements(int operationId);

    @Transactional(readOnly = true)
    Map<Integer, String> getOperationVesselUseMeasurementsMap(int operationId);

    @Transactional(readOnly = true)
    Map<Integer, String> getOperationGearUseMeasurementsMap(int operationId);

    @Transactional(readOnly = true)
    List<MeasurementVO> getSampleMeasurements(int sampleId);

    @Transactional(readOnly = true)
    Map<Integer, String> getSampleMeasurementsMap(int sampleId);

    @Transactional(readOnly = true)
    Map<Integer, String> getBatchSortingMeasurementsMap(int batchId);

    @Transactional(readOnly = true)
    List<MeasurementVO> getBatchSortingMeasurements(int batchId);

    @Transactional(readOnly = true)
    Map<Integer, String> getBatchQuantificationMeasurementsMap(int batchId);

    @Transactional(readOnly = true)
    List<QuantificationMeasurementVO> getBatchQuantificationMeasurements(int batchId);

    @Transactional(readOnly = true)
    Map<Integer, String> getProductSortingMeasurementsMap(int productId);

    @Transactional(readOnly = true)
    Map<Integer, String> getProductQuantificationMeasurementsMap(int productId);

    @Transactional(readOnly = true)
    List<MeasurementVO> getObservedLocationMeasurements(int observedLocationId);

    @Transactional(readOnly = true)
    Map<Integer, String> getObservedLocationMeasurementsMap(int observedLocationId);

    @Transactional(readOnly = true)
    List<MeasurementVO> getSaleMeasurements(int saleId);

    @Transactional(readOnly = true)
    Map<Integer, String> getSaleMeasurementsMap(int saleId);

    @Transactional(readOnly = true)
    List<MeasurementVO> getExpectedSaleMeasurements(int saleId);

    @Transactional(readOnly = true)
    Map<Integer, String> getExpectedSaleMeasurementsMap(int saleId);

    @Transactional(readOnly = true)
    List<MeasurementVO> getVesselFeaturesMeasurements(int vesselFeaturesId);

    @Transactional(readOnly = true)
    Map<Integer, String> getVesselFeaturesMeasurementsMap(int vesselFeaturesId);

    @Transactional(readOnly = true)
    Map<Integer, String> getVesselUseFeaturesMeasurementsMap(int vesselUseFeaturesId);

    @Transactional(readOnly = true)
    Map<Integer, String> getGearUseFeaturesMeasurementsMap(int gearUseFeaturesId);

    @Transactional(readOnly = true)
    Map<Integer, String> getLandingMeasurementsMap(int landingId);

    @Transactional(readOnly = true)
    Map<Integer, String> getLandingMeasurementsMap(int landingId, List<Integer> pmfmIds);

    @Transactional(readOnly = true)
    Map<Integer, String> getLandingSurveyMeasurementsMap(int landingId);

    @Transactional(readOnly = true)
    Map<Integer, String> getLandingSurveyMeasurementsMap(int landingId, List<Integer> pmfmIds);

}

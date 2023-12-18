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

import net.sumaris.core.model.IEntity;
import net.sumaris.core.model.data.IMeasurementEntity;
import net.sumaris.core.vo.data.MeasurementVO;
import net.sumaris.core.vo.data.QuantificationMeasurementVO;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface MeasurementDao {

    char MEASUREMENTS_MAP_VALUE_SEPARATOR = '|';
    char MEASUREMENTS_MAP_PRECISION_PREFIX = '~';
    char MEASUREMENTS_MAP_QUALITY_FLAG_PREFIX = 'Q';

    <T extends IMeasurementEntity, V extends MeasurementVO> V toMeasurementVO(T measurement, Class<? extends V> voClass);

    <T extends IMeasurementEntity, V extends MeasurementVO> List<V> saveMeasurements(
            Class<? extends IMeasurementEntity> entityClass,
            List<V> sources,
            List<T> target,
            IEntity<?> parent);

    <ID extends Serializable, T extends IMeasurementEntity> void deleteMeasurements(
        final Class<T> targetClass,
        final Class<? extends IEntity<ID>> parentClass,
        final Collection<ID> parentIds);

    <T extends IMeasurementEntity> List<T> getMeasurementEntitiesByParentId(Class<T> entityClass,
                                                                     String parentPropertyName,
                                                                     int parentId,
                                                                     String sortByPropertyName);

    <T extends IMeasurementEntity> Map<Integer, Collection<T>> getMeasurementEntitiesByParentIds(Class<T> entityClass,
                                                                                                 String parentPropertyName,
                                                                                                 Collection<Integer> parentIds,
                                                                                                 String sortByPropertyName);

    <T extends IMeasurementEntity> Map<Integer, Map<Integer, String>> getMeasurementsMapByParentIds(Class<T> entityClass,
                                                                                                    String parentPropertyName,
                                                                                                    Collection<Integer> parentIds,
                                                                                                    String sortByPropertyName);
    // Trip
    List<MeasurementVO> getTripVesselUseMeasurements(int tripId);
    Map<Integer, String> getTripVesselUseMeasurementsMap(int tripId);
    List<MeasurementVO> saveTripVesselUseMeasurements(int tripId, List<MeasurementVO> sources);
    Map<Integer, String> saveTripMeasurementsMap(int tripId, Map<Integer, String> sources);
    Map<Integer, Map<Integer, String>> getTripsVesselUseMeasurementsMap(Collection<Integer> tripIds);

    // Physical gear
    List<MeasurementVO> getPhysicalGearMeasurements(int physicalGearId);
    Map<Integer, String> getPhysicalGearMeasurementsMap(int physicalGearId);
    List<MeasurementVO> savePhysicalGearMeasurements(int physicalGearId, List<MeasurementVO> sources);
    Map<Integer, String> savePhysicalGearMeasurementsMap(int physicalGearId, Map<Integer, String> sources);

    // Operation
    List<MeasurementVO> getOperationVesselUseMeasurements(int operationId);

    List<MeasurementVO> getOperationVesselUseMeasurements(int operationId, List<Integer> pmfmIds);

    List<MeasurementVO> getOperationGearUseMeasurements(int operationId);
    Map<Integer, String> getOperationVesselUseMeasurementsMap(int operationId);
    Map<Integer, String> getOperationGearUseMeasurementsMap(int operationId);
    Map<Integer, Map<Integer, String>> getOperationsVesselUseMeasurementsMap(Collection<Integer> operationIds);
    Map<Integer, Map<Integer, String>> getOperationsGearUseMeasurementsMap(Collection<Integer> operationIds);

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
    List<MeasurementVO> getSaleMeasurements(int saleId);
    Map<Integer, String> getSaleMeasurementsMap(int saleId);
    List<MeasurementVO> saveSaleMeasurements(int saleId, List<MeasurementVO> sources);
    Map<Integer, String> saveSaleMeasurementsMap(int saleId, Map<Integer, String> sources);

    // Expected Sale
    List<MeasurementVO> getExpectedSaleMeasurements(int expectedSaleId);
    Map<Integer, String> getExpectedSaleMeasurementsMap(int expectedSaleId);
    List<MeasurementVO> saveExpectedSaleMeasurements(int expectedSaleId, List<MeasurementVO> sources);
    Map<Integer, String> saveExpectedSaleMeasurementsMap(int expectedSaleId, Map<Integer, String> sources);

    // Landing
    List<MeasurementVO> saveLandingMeasurements(int landingId, List<MeasurementVO> sources);
    Map<Integer, String> saveLandingMeasurementsMap(final int landingId, Map<Integer, String> sources);
    List<MeasurementVO> getLandingMeasurements(int landingId);
    Map<Integer, String> getLandingMeasurementsMap(int landingId);

    Map<Integer, String> getLandingMeasurementsMap(int landingId, List<Integer> pmfmIds);

    // Landing (Survey measurement)
    List<MeasurementVO> saveLandingSurveyMeasurements(int landingId, List<MeasurementVO> sources);
    Map<Integer, String> saveLandingSurveyMeasurementsMap(final int landingId, Map<Integer, String> sources);
    List<MeasurementVO> getLandingSurveyMeasurements(int landingId);
    Map<Integer, String> getLandingSurveyMeasurementsMap(int landingId);

    Map<Integer, String> getLandingSurveyMeasurementsMap(int landingId, List<Integer> pmfmIds);

    // Sample
    List<MeasurementVO> getSampleMeasurements(int sampleId);
    Map<Integer, String> getSampleMeasurementsMap(int sampleId);
    List<MeasurementVO> saveSampleMeasurements(int sampleId, List<MeasurementVO> sources);
    Map<Integer, String> saveSampleMeasurementsMap(final int sampleId, Map<Integer, String> sources);

    // Batch
    List<MeasurementVO> getBatchSortingMeasurements(int batchId);
    List<QuantificationMeasurementVO> getBatchQuantificationMeasurements(int batchId);
    Map<Integer, String> getBatchSortingMeasurementsMap(int batchId);
    Map<Integer, String> getBatchQuantificationMeasurementsMap(int batchId);
    Map<Integer, Map<Integer, String>> getBatchesSortingMeasurementsMap(Collection<Integer> ids);
    Map<Integer, Map<Integer, String>> getBatchesQuantificationMeasurementsMap(Collection<Integer> ids);
    List<MeasurementVO> saveBatchSortingMeasurements(int batchId, List<MeasurementVO> sources);
    List<QuantificationMeasurementVO> saveBatchQuantificationMeasurements(int batchId, List<QuantificationMeasurementVO> sources);
    Map<Integer, String> saveBatchSortingMeasurementsMap(final int batchId, Map<Integer, String> sources);
    Map<Integer, String> saveBatchQuantificationMeasurementsMap(final int batchId, Map<Integer, String> sources);

    // Product
    Map<Integer, String> getProductSortingMeasurementsMap(int productId);
    Map<Integer, String> getProductQuantificationMeasurementsMap(int productId);
    List<MeasurementVO> saveProductSortingMeasurements(int productId, List<MeasurementVO> sources);
    List<MeasurementVO> saveProductQuantificationMeasurements(int productId, List<MeasurementVO> sources);
    Map<Integer, String> saveProductSortingMeasurementsMap(final int productId, Map<Integer, String> sources);
    Map<Integer, String> saveProductQuantificationMeasurementsMap(final int productId, Map<Integer, String> sources);

    // Vessel
    List<MeasurementVO> getVesselFeaturesMeasurements(int vesselFeaturesId);
    Map<Integer, String> getVesselFeaturesMeasurementsMap(int vesselFeaturesId);
    List<MeasurementVO> saveVesselPhysicalMeasurements(int vesselFeaturesId, List<MeasurementVO> sources);
    Map<Integer, String> saveVesselPhysicalMeasurementsMap(final int vesselFeaturesId, Map<Integer, String> sources);

    // Activity calendar
    List<MeasurementVO> saveActivityCalendarMeasurements(int activityCalendarId, List<MeasurementVO> sources);
    Map<Integer, String> saveActivityCalendarMeasurementsMap(final int activityCalendarId, Map<Integer, String> sources);
    List<MeasurementVO> getActivityCalendarMeasurements(int activityCalendarId);
    Map<Integer, String> getActivityCalendarMeasurementsMap(int activityCalendarId);

    // Utils
    <T extends IMeasurementEntity> Map<Integer, String> toMeasurementsMap(Collection<T> sources);

}

/*
 * #%L
 * SUMARiS
 * %%
 * Copyright (C) 2019 SUMARiS Consortium
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

package net.sumaris.core.service.data;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import net.sumaris.core.dao.DatabaseFixtures;
import net.sumaris.core.model.administration.programStrategy.AcquisitionLevelEnum;
import net.sumaris.core.model.data.PhysicalGear;
import net.sumaris.core.model.referential.pmfm.PmfmEnum;
import net.sumaris.core.model.referential.pmfm.QualitativeValueEnum;
import net.sumaris.core.service.referential.pmfm.PmfmService;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.data.*;
import net.sumaris.core.vo.data.batch.BatchVO;
import net.sumaris.core.vo.data.sample.SampleVO;
import net.sumaris.core.vo.referential.LocationVO;
import net.sumaris.core.vo.referential.MetierVO;
import net.sumaris.core.vo.referential.PmfmVO;
import net.sumaris.core.vo.referential.ReferentialVO;

import java.util.Calendar;
import java.util.List;

public class DataTestUtils {

    public static TripVO createTrip(DatabaseFixtures fixtures,
                                    PmfmService pmfmService) {
        TripVO vo = new TripVO();

        vo.setProgram(fixtures.getDefaultProgram());

        // Vessel
        VesselSnapshotVO vessel = new VesselSnapshotVO();
        vessel.setVesselId(fixtures.getVesselId(0));
        vo.setVesselSnapshot(vessel);

        // Set dates
        Calendar date = Calendar.getInstance();
        date.add(Calendar.HOUR, -12);
        vo.setDepartureDateTime(date.getTime());

        date.add(Calendar.DAY_OF_YEAR, 15);
        vo.setReturnDateTime(date.getTime());

        // Locations
        LocationVO departureLocation = new LocationVO();
        departureLocation.setId(fixtures.getLocationPortId(0));
        vo.setDepartureLocation(departureLocation);

        LocationVO returnLocation = new LocationVO();
        returnLocation.setId(fixtures.getLocationPortId(0));
        vo.setReturnLocation(returnLocation);

        // Recorder
        DepartmentVO recorderDepartment = new DepartmentVO();
        recorderDepartment.setId(fixtures.getDepartmentId(0));
        vo.setRecorderDepartment(recorderDepartment);

        // Gear
        ReferentialVO gear = new ReferentialVO();
        gear.setEntityName(PhysicalGear.class.getSimpleName());
        gear.setId(fixtures.getGearId(0));

        // Physical gear
        PhysicalGearVO physicalGear = new PhysicalGearVO();
        physicalGear.setGear(gear);
        physicalGear.setRankOrder(1);
        physicalGear.setRecorderDepartment(vo.getRecorderDepartment());
        physicalGear.setProgram(vo.getProgram());
        vo.setGears(List.of(physicalGear));

        // Observers
        PersonVO observer1 = new PersonVO();
        observer1.setId(fixtures.getPersonId(0));
        PersonVO observer2 = new PersonVO();
        observer2.setId(fixtures.getPersonId(1));
        vo.setObservers(ImmutableSet.of(observer1, observer2));

        return vo;
    }

    public static OperationVO createOperation(DatabaseFixtures fixtures,
                                              PmfmService pmfmService,
                                              TripVO parent) {
        OperationVO vo = new OperationVO();
        vo.setTripId(parent.getId());

        // Set dates
        Calendar date = Calendar.getInstance();
        date.setTime(parent.getDepartureDateTime());
        date.add(Calendar.HOUR, 1);
        vo.setStartDateTime(date.getTime());

        date.add(Calendar.MINUTE, 5);
        vo.setFishingStartDateTime(date.getTime());

        date.add(Calendar.MINUTE, 20);
        vo.setFishingEndDateTime(date.getTime());

        date.add(Calendar.MINUTE, 5);
        vo.setEndDateTime(date.getTime());

        // Positions
        VesselPositionVO startPos = new VesselPositionVO();
        startPos.setDateTime(vo.getFishingStartDateTime());
        startPos.setLatitude(10.0);
        startPos.setLongitude(0.0);

        VesselPositionVO endPos = new VesselPositionVO();
        endPos.setDateTime(vo.getFishingEndDateTime());
        endPos.setLatitude(10.01);
        endPos.setLongitude(0.01);

        vo.setPositions(Lists.newArrayList(startPos, endPos));

        // Recorder
        DepartmentVO recorderDepartment = new DepartmentVO();
        recorderDepartment.setId(fixtures.getDepartmentId(0));
        vo.setRecorderDepartment(recorderDepartment);

        // Physical gear
        vo.setPhysicalGearId(1);

        // Metier
        vo.setMetier(createMetierVO(fixtures.getMetierIdForOTB(0)));

        // Measurements (= vessel use measurements)
        PmfmVO bottomDepthPmfm = pmfmService.getByLabel("BOTTOM_DEPTH_M", null);
        MeasurementVO meas1 = new MeasurementVO();
        meas1.setNumericalValue(15.0);
        meas1.setPmfmId(bottomDepthPmfm.getId());
        meas1.setRankOrder((short)1);

        vo.setMeasurements(ImmutableList.of(meas1));

        List<SampleVO> samples = Lists.newArrayList();
        vo.setSamples(samples);

        // Sample / Survival tests
        {
            SampleVO sample = new SampleVO();
            sample.setTaxonGroup(createReferentialVO(fixtures.getTaxonGroupFAOId(0)));
            date.add(Calendar.MINUTE, 5);
            sample.setSampleDate(date.getTime());
            sample.setRankOrder(1);
            sample.setLabel(AcquisitionLevelEnum.SAMPLE.getLabel() + "#1");
            sample.setMatrix(createReferentialVO(fixtures.getMatrixIdForIndividual()));
            sample.setComments("A survival test sample #1");
            sample.setProgram(fixtures.getDefaultProgram());

            // Measurements (as map)
            sample.setMeasurementValues(
                    ImmutableMap.<Integer, String>builder()
                            .put(60, "155")
                            .put(80, "185")
                            .put(fixtures.getPmfmSampleTagId(), "TAG-1")
                            .build());
            samples.add(sample);

            // Individual monitoring, as children
            List<SampleVO> children = Lists.newArrayList();
            sample.setChildren(children);
            {
                SampleVO childSample = new SampleVO();
                childSample.setTaxonGroup(createReferentialVO(fixtures.getTaxonGroupFAOId(0)));
                childSample.setRankOrder(1);
                childSample.setLabel(AcquisitionLevelEnum.INDIVIDUAL_MONITORING.getLabel() + "#1");
                childSample.setMatrix(createReferentialVO(fixtures.getMatrixIdForIndividual()));
                childSample.setComments("A individual monitoring test sample #1");

                // Measurements (as map)
                childSample.setMeasurementValues(
                        ImmutableMap.<Integer, String>builder()
                                .put(fixtures.getPmfmSampleIsDead(), "0")
                                .build());
                children.add(childSample);
            }
        }

        // Batch / catch
        {
            BatchVO catchBatch = createBatchTree(fixtures, 0);
            vo.setCatchBatch(catchBatch);
        }

        return vo;
    }

    public static BatchVO createBatchTree(DatabaseFixtures fixtures, int index) {
        return switch (index) {
            case 0 -> createSumarisBatchTree(fixtures);
            case 1 -> createAdapBatchTree(fixtures);
            default -> throw new IllegalArgumentException("Invalid index: " + index);
        };
    }

    public static BatchVO createSumarisBatchTree(DatabaseFixtures fixtures) {
        // Batch / catch
        BatchVO catchBatch = new BatchVO();
        catchBatch.setLabel("batch #1");
        catchBatch.setRankOrder(1);
        catchBatch.setComments("Catch batch on OPE #1");

        // Measurements (as map)
        catchBatch.setMeasurementValues(
                ImmutableMap.<Integer, String>builder()
                        .put(60, "155")
                        .put(80, "185")
                        .build());

        // Children
        List<BatchVO> children = Lists.newArrayList();
        {
            BatchVO batch = new BatchVO();
            batch.setLabel("batch #1.1");
            batch.setRankOrder(1);
            batch.setComments("Batch 1.1 on OPE #1");
            batch.setTaxonGroup(createReferentialVO(fixtures.getTaxonGroupFAOId(0)));

            // Measurements (as map)
            batch.setMeasurementValues(
                    ImmutableMap.<Integer, String>builder()
                            .put(60, "155") // TODO: change this
                            .put(80, "185")
                            .build());
            children.add(batch);
        }
        catchBatch.setChildren(children);

        return catchBatch;
    }

    public static BatchVO createAdapBatchTree(DatabaseFixtures fixtures) {
        // Batch / catch
        BatchVO catchBatch = new BatchVO();
        catchBatch.setLabel(AcquisitionLevelEnum.CATCH_BATCH.getLabel());
        catchBatch.setRankOrder(1);
        catchBatch.setComments("Catch batch on OPE #1");
        catchBatch.setExhaustiveInventory(Boolean.FALSE);

        // Species batch #1 (NEP - Langoustine)
        {
            BatchVO speciesBatch = new BatchVO();
            catchBatch.addChildren(speciesBatch);
            speciesBatch.setLabel(AcquisitionLevelEnum.SORTING_BATCH.getLabel() + "#1");
            speciesBatch.setRankOrder(1);
            speciesBatch.setTaxonGroup(createReferentialVO(1131, "NEP"));
            speciesBatch.setChildren(Lists.newArrayList());
            speciesBatch.setExhaustiveInventory(Boolean.TRUE);

            // Landing (weight=100kg)
            {
                BatchVO lanBatch = new BatchVO();
                speciesBatch.addChildren(lanBatch);
                lanBatch.setLabel(speciesBatch.getLabel() + ".LAN");
                lanBatch.setRankOrder(1);
                lanBatch.setExhaustiveInventory(Boolean.TRUE);
                lanBatch.setMeasurementValues(
                        ImmutableMap.<Integer, String>builder()
                                .put(PmfmEnum.BATCH_MEASURED_WEIGHT.getId(), "100") // Total weight
                                .put(PmfmEnum.DISCARD_OR_LANDING.getId(), String.valueOf(QualitativeValueEnum.LANDING.getId()))
                                .build());

                // Landing > Sampling batch (weight=50kg)
                {
                    BatchVO samplingBatch = new BatchVO();
                    lanBatch.addChildren(samplingBatch);
                    samplingBatch.setLabel(lanBatch.getLabel() + "%");
                    samplingBatch.setRankOrder(1);
                    samplingBatch.setMeasurementValues(
                            ImmutableMap.<Integer, String>builder()
                                    .put(PmfmEnum.BATCH_MEASURED_WEIGHT.getId(), "50") // Sample weight
                                    .build());

                    // Landing > % > 7cm (1 indiv)
                    {
                        BatchVO lengthBatch = new BatchVO();
                        samplingBatch.addChildren(lengthBatch);
                        lengthBatch.setLabel(AcquisitionLevelEnum.SORTING_BATCH_INDIVIDUAL.getLabel() + "#1");
                        lengthBatch.setRankOrder(1);
                        lengthBatch.setIndividualCount(1);
                        lengthBatch.setMeasurementValues(
                                ImmutableMap.<Integer, String>builder()
                                        .put(PmfmEnum.LENGTH_CARAPACE_CM.getId(), "7") // Total length
                                        .build());
                    }

                    // Landing > % > 8cm (1 indiv)
                    {
                        BatchVO lengthBatch = new BatchVO();
                        samplingBatch.addChildren(lengthBatch);
                        lengthBatch.setLabel(AcquisitionLevelEnum.SORTING_BATCH_INDIVIDUAL.getLabel() + "#2");
                        lengthBatch.setRankOrder(2);
                        lengthBatch.setIndividualCount(1);
                        lengthBatch.setMeasurementValues(
                                ImmutableMap.<Integer, String>builder()
                                        .put(PmfmEnum.LENGTH_CARAPACE_CM.getId(), "8") // Total length
                                        .build());
                    }
                }
            }

            // Discard (weight=20kg)
            {
                BatchVO disBatch = new BatchVO();
                speciesBatch.addChildren(disBatch);
                disBatch.setLabel(speciesBatch.getLabel() + ".DIS");
                disBatch.setRankOrder(2);
                disBatch.setExhaustiveInventory(Boolean.TRUE);
                disBatch.setMeasurementValues(
                        ImmutableMap.<Integer, String>builder()
                                .put(PmfmEnum.BATCH_MEASURED_WEIGHT.getId(), "20") // Total weight
                                .put(PmfmEnum.DISCARD_OR_LANDING.getId(), String.valueOf(QualitativeValueEnum.DISCARD.getId()))
                                .build());
                disBatch.setChildren(Lists.newArrayList());

                // Discard > Sampling batch (ratio=50%)
                {
                    BatchVO samplingBatch = new BatchVO();
                    disBatch.addChildren(samplingBatch);
                    samplingBatch.setLabel(disBatch.getLabel() + "%");
                    samplingBatch.setRankOrder(1);
                    samplingBatch.setSamplingRatio(0.5);
                    samplingBatch.setSamplingRatioText("50%");

                    // Landing > % > 5cm (2 indiv)
                    {
                        BatchVO lengthBatch = new BatchVO();
                        samplingBatch.addChildren(lengthBatch);
                        lengthBatch.setLabel(AcquisitionLevelEnum.SORTING_BATCH_INDIVIDUAL.getLabel() + "#3");
                        lengthBatch.setRankOrder(1);
                        lengthBatch.setIndividualCount(2);
                        lengthBatch.setMeasurementValues(
                                ImmutableMap.<Integer, String>builder()
                                        .put(PmfmEnum.LENGTH_CARAPACE_CM.getId(), "5") // Total length
                                        .build());
                    }
                }
            }

        }

        // Species batch #2 (GAD - Eglefin)
        {
            BatchVO speciesBatch = new BatchVO();
            catchBatch.addChildren(speciesBatch);
            speciesBatch.setLabel("SORTING_BATCH#2");
            speciesBatch.setRankOrder(2);
            speciesBatch.setTaxonGroup(createReferentialVO(1152, "GAD"));
            speciesBatch.setExhaustiveInventory(Boolean.TRUE);

            // Landing
            {
                BatchVO lanBatch = new BatchVO();
                speciesBatch.addChildren(lanBatch);
                lanBatch.setLabel(speciesBatch.getLabel() + ".LAN");
                lanBatch.setRankOrder(1);
                lanBatch.setExhaustiveInventory(Boolean.TRUE);
                // Measurements (as map)
                lanBatch.setMeasurementValues(
                        ImmutableMap.<Integer, String>builder()
                                .put(PmfmEnum.BATCH_MEASURED_WEIGHT.getId(), "50") // Total weight
                                .put(PmfmEnum.DISCARD_OR_LANDING.getId(), String.valueOf(QualitativeValueEnum.LANDING.getId()))
                                .build());
            }

            // Discard
            {
                BatchVO disBatch = new BatchVO();
                speciesBatch.addChildren(disBatch);
                disBatch.setLabel(speciesBatch.getLabel() + ".DIS");
                disBatch.setRankOrder(2);
                disBatch.setExhaustiveInventory(Boolean.TRUE);
                // Measurements (as map)
                disBatch.setMeasurementValues(
                        ImmutableMap.<Integer, String>builder()
                                .put(PmfmEnum.BATCH_MEASURED_WEIGHT.getId(), "10") // Total weight
                                .put(PmfmEnum.DISCARD_OR_LANDING.getId(), String.valueOf(QualitativeValueEnum.DISCARD.getId()))
                                .build());

            }
        }

        // Species batch #3 (MNZ - Baudroie / Pour RTP)
        {
            BatchVO speciesBatch = new BatchVO();
            catchBatch.addChildren(speciesBatch);
            speciesBatch.setLabel(AcquisitionLevelEnum.SORTING_BATCH.getLabel() + "#3");
            speciesBatch.setRankOrder(3);
            speciesBatch.setTaxonGroup(createReferentialVO(1132, "MNZ"));
            speciesBatch.setExhaustiveInventory(Boolean.TRUE);

            // Landing (10kg)
            {
                BatchVO lanBatch = new BatchVO();
                speciesBatch.addChildren(lanBatch);
                lanBatch.setLabel(speciesBatch.getLabel() + ".LAN");
                lanBatch.setRankOrder(1);
                lanBatch.setExhaustiveInventory(Boolean.TRUE);
                // Measurements (as map)
                lanBatch.setMeasurementValues(
                        ImmutableMap.<Integer, String>builder()
                                .put(PmfmEnum.BATCH_MEASURED_WEIGHT.getId(), "10") // Total weight
                                .put(PmfmEnum.DISCARD_OR_LANDING.getId(), String.valueOf(QualitativeValueEnum.LANDING.getId()))
                                .build());

                // Discard > Sampling batch (ratio=50%)
                {
                    BatchVO samplingBatch = new BatchVO();
                    lanBatch.addChildren(samplingBatch);
                    samplingBatch.setLabel(lanBatch.getLabel() + "%");
                    samplingBatch.setRankOrder(1);
                    //samplingBatch.setSamplingRatio(0.5);
                    //samplingBatch.setSamplingRatioText("50%");

                    // Landing > % > 30cm (1 indiv)
                    {
                        BatchVO lengthBatch = new BatchVO();
                        samplingBatch.addChildren(lengthBatch);
                        lengthBatch.setLabel(AcquisitionLevelEnum.SORTING_BATCH_INDIVIDUAL.getLabel() + "#4");
                        lengthBatch.setRankOrder(1);
                        lengthBatch.setIndividualCount(1);
                        lengthBatch.setMeasurementValues(
                                ImmutableMap.<Integer, String>builder()
                                        .put(PmfmEnum.BATCH_CALCULATED_WEIGHT.getId(), "0.681") // 780g
                                        .put(PmfmEnum.LENGTH_TOTAL_CM.getId(), "30") // Total length
                                        .build());
                    }
                    // Landing > % > 35cm (1 indiv)
                    {
                        BatchVO lengthBatch = new BatchVO();
                        samplingBatch.addChildren(lengthBatch);
                        lengthBatch.setLabel(AcquisitionLevelEnum.SORTING_BATCH_INDIVIDUAL.getLabel() + "#5");
                        lengthBatch.setRankOrder(2);
                        lengthBatch.setIndividualCount(1);
                        lengthBatch.setMeasurementValues(
                                ImmutableMap.<Integer, String>builder()
                                        .put(PmfmEnum.BATCH_CALCULATED_WEIGHT.getId(), "0.780") // 780g
                                        .put(PmfmEnum.LENGTH_TOTAL_CM.getId(), "35") // Total length
                                        .build());
                    }
                }
            }
        }

        return catchBatch;
    }

    public static ReferentialVO createReferentialVO(int id) {
        return createReferentialVO(id, "?");
    }

    public static ReferentialVO createReferentialVO(int id, String label) {
        ReferentialVO result = new ReferentialVO();
        result.setId(id);
        result.setLabel(label);
        return result;
    }

    public static MetierVO createMetierVO(int id) {
        MetierVO result = new MetierVO();
        result.setId(id);
        return result;
    }

    public static ActivityCalendarVO createActivityCalendar(DatabaseFixtures fixtures,
                                                            PmfmService pmfmService, int year) {
        ActivityCalendarVO vo = new ActivityCalendarVO();

        vo.setProgram(fixtures.getActivityCalendarProgram());

        // Vessel
        VesselSnapshotVO vessel = new VesselSnapshotVO();
        vessel.setVesselId(fixtures.getVesselId(0));
        vo.setVesselSnapshot(vessel);

        // Other properties
        vo.setYear(year);
        vo.setDirectSurveyInvestigation(Boolean.TRUE);

        // Recorder
        DepartmentVO recorderDepartment = new DepartmentVO();
        recorderDepartment.setId(fixtures.getDepartmentId(0));
        vo.setRecorderDepartment(recorderDepartment);

        return vo;
    }
}

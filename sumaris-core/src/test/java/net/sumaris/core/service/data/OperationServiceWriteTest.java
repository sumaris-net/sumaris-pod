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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import net.sumaris.core.dao.DatabaseFixtures;
import net.sumaris.core.dao.DatabaseResource;
import net.sumaris.core.model.administration.programStrategy.AcquisitionLevelEnum;
import net.sumaris.core.service.AbstractServiceTest;
import net.sumaris.core.service.data.batch.BatchService;
import net.sumaris.core.service.data.sample.SampleService;
import net.sumaris.core.service.referential.PmfmService;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.data.*;
import net.sumaris.core.vo.referential.LocationVO;
import net.sumaris.core.vo.referential.PmfmVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class OperationServiceWriteTest extends AbstractServiceTest {

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.writeDb();

    @Autowired
    private TripService tripService;

    @Autowired
    private OperationService service;

    @Autowired
    private MeasurementService measurementService;

    @Autowired
    private SampleService sampleService;

    @Autowired
    private BatchService batchService;

    @Autowired
    private PmfmService pmfmService;


    private TripVO parent;

    private DatabaseFixtures fixtures;

    @Before
    public void setUp() {
        this.parent = tripService.get(dbResource.getFixtures().getTripId(0));
        Assume.assumeNotNull(this.parent);
        this.fixtures = dbResource.getFixtures();
    }

    @Test
    public void get() {
        OperationVO vo = service.get(1);
        Assert.assertNotNull(vo);
        Assert.assertNotNull(vo.getId());
    }

    @Test
    public void save() {
        OperationVO vo = createOperation();
        int batchCount = countBatches(vo);
        int sampleCount = countSamples(vo);

        // Save
        OperationVO savedVo = service.save(vo);
        Assert.assertNotNull(savedVo.getId());
        Assert.assertNotNull(savedVo.getQualityFlagId());

        // Full reload
        OperationVO reloadedVo = service.get(savedVo.getId());
        Assert.assertNotNull(reloadedVo);

        // Check vessel position: Should NOT be loaded in VO
        Assert.assertEquals(0, CollectionUtils.size(reloadedVo.getPositions()));

        // Check measurements
        {
            // Should NOT be loaded in VO
            Assert.assertEquals(0, CollectionUtils.size(reloadedVo.getMeasurements()));
            List<MeasurementVO> reloadMeasurements = measurementService.getVesselUseMeasurementsByOperationId(savedVo.getId());
            Assert.assertEquals(CollectionUtils.size(savedVo.getMeasurements()), CollectionUtils.size(reloadMeasurements));
        }

        // Check samples
        {
            // Should NOT be loaded in OperationVO
            Assert.assertEquals(0, CollectionUtils.size(reloadedVo.getSamples()));
            List<SampleVO> reloadSamples = sampleService.getAllByOperationId(savedVo.getId());
            Assert.assertEquals(sampleCount, CollectionUtils.size(reloadSamples));
        }

        // Check batches
        {
            // Should NOT be loaded in OperationVO
            Assert.assertEquals(0, CollectionUtils.size(reloadedVo.getCatchBatch()));
            List<BatchVO> reloadBatches = batchService.getAllByOperationId(savedVo.getId());
            Assert.assertEquals(batchCount, CollectionUtils.size(reloadBatches));
        }
    }

    @Test
    public void delete() {
        service.delete(fixtures.getTripId(0));
    }


    @Test
    public void deleteAfterCreate() {
        OperationVO savedVO = null;
        try {
            savedVO = service.save(createOperation());
            Assume.assumeNotNull(savedVO);
            Assume.assumeNotNull(savedVO.getId());
        }
        catch(Exception e) {
            Assume.assumeNoException(e);
        }

        service.delete(savedVO.getId());
    }


    /* -- Protected -- */

    protected OperationVO createOperation() {
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
        vo.setMetier(createReferentialVO(fixtures.getMetierIdForOTB(0)));

        // Measurements (= vessel use measurements)
        PmfmVO bottomDepthPmfm = pmfmService.getByLabel("BOTTOM_DEPTH_M");
        MeasurementVO meas1 = new MeasurementVO();
        meas1.setNumericalValue(15.0);
        meas1.setPmfmId(bottomDepthPmfm.getId());
        meas1.setRankOrder(1);

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
            sample.setLabel(AcquisitionLevelEnum.SURVIVAL_TEST.label + "#1");
            sample.setMatrix(createReferentialVO(fixtures.getMatrixIdForIndividual()));
            sample.setComments("A survival test sample #1");

            // Measurements (as map)
            sample.setMeasurementValues(
                    ImmutableMap.<Integer, String>builder()
                            .put(60, "155")
                            .put(80, "185")
                            .put(dbResource.getFixtures().getPmfmSampleTagId(), "TAG-1")
                            .build());
            samples.add(sample);

            // Individual monitoring, as children
            List<SampleVO> children = Lists.newArrayList();
            sample.setChildren(children);
            {
                SampleVO childSample = new SampleVO();
                childSample.setTaxonGroup(createReferentialVO(fixtures.getTaxonGroupFAOId(0)));
                childSample.setRankOrder(1);
                childSample.setLabel(AcquisitionLevelEnum.INDIVIDUAL_MONITORING.label + "#1");
                childSample.setMatrix(createReferentialVO(fixtures.getMatrixIdForIndividual()));
                childSample.setComments("A individual monitoring test sample #1");

                // Measurements (as map)
                childSample.setMeasurementValues(
                        ImmutableMap.<Integer, String>builder()
                                .put(100, "0")
                                .build());
                children.add(childSample);
            }
        }

        // Batch / catch
        {
            BatchVO catchBatch = new BatchVO();
            catchBatch.setLabel("batch #1");
            catchBatch.setRankOrder(1);
            catchBatch.setComments("Catch batch on OPE #1");

            // Measurements (as map)
            catchBatch.setSortingMeasurementValues(
                    ImmutableMap.<Integer, String>builder()
                            .put(60, "155")
                            .put(80, "185")
                            .build());

            vo.setCatchBatch(catchBatch);

            // Children
            List<BatchVO> children = Lists.newArrayList();
            {
                BatchVO batch = new BatchVO();
                batch.setLabel("batch #1.1");
                batch.setRankOrder(1);
                batch.setComments("Batch 1.1 on OPE #1");
                batch.setTaxonGroup(createReferentialVO(fixtures.getTaxonGroupFAOId(0)));

                // Measurements (as map)
                batch.setSortingMeasurementValues(
                        ImmutableMap.<Integer, String>builder()
                                .put(60, "155") // TODO: change this
                                .put(80, "185")
                                .build());
                children.add(batch);
            }
            catchBatch.setChildren(children);
        }

        return vo;
    }

    protected int countBatches(OperationVO vo) {
        if (vo.getCatchBatch() == null) return 0;
        return countBatches(vo.getCatchBatch().getChildren()) + 1;
    }

    protected int countBatches(List<BatchVO> vos) {
        if (CollectionUtils.isEmpty(vos)) return 0;

        int count = CollectionUtils.size(vos);
        for (BatchVO b : vos) {
            count += countBatches(b.getChildren());
        }
        return count;
    }

    protected int countSamples(OperationVO vo) {
        return countSamples(vo.getSamples());
    }

    protected int countSamples(List<SampleVO> vos) {
        if (CollectionUtils.isEmpty(vos)) return 0;

        int count = CollectionUtils.size(vos);
        for (SampleVO b : vos) {
            count += countSamples(b.getChildren());
        }
        return count;
    }
}

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
import net.sumaris.core.model.data.sample.Sample;
import net.sumaris.core.service.AbstractServiceTest;
import net.sumaris.core.service.data.sample.SampleService;
import net.sumaris.core.service.referential.PmfmService;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.data.*;
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
        PmfmVO bottomDepthPmfm = pmfmService.getByLabel("FISHING_DEPTH_M");
        MeasurementVO meas1 = new MeasurementVO();
        meas1.setNumericalValue(15.0);
        meas1.setPmfmId(bottomDepthPmfm.getId());
        meas1.setRankOrder(1);

        vo.setMeasurements(ImmutableList.of(meas1));

        // Sample / Survival tests
        {
            SampleVO sample = new SampleVO();
            sample.setTaxonGroup(createReferentialVO(fixtures.getTaxonGroupFAO(0)));
            date.add(Calendar.MINUTE, 5);
            sample.setSampleDate(date.getTime());
            sample.setLabel("Survival test #1");

            sample.setMatrix(createReferentialVO(fixtures.getMatrixIdForIndividual()));
            sample.setRankOrder(1);
            sample.setComments("A survival test sample #1");

            // Measurements (as map)
            sample.setMeasurementsMap(
                ImmutableMap.<Integer, Object>builder()
                    .put(60, new Integer(155))
                    .put(80, new Integer(185))
                    .build());

            vo.setSamples(ImmutableList.of(sample));
        }

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
            // Should NOT be loaded in VO
            Assert.assertEquals(0, CollectionUtils.size(reloadedVo.getSamples()));
            List<SampleVO> reloadSamples = sampleService.getAllByOperationId(savedVo.getId());
            Assert.assertEquals(CollectionUtils.size(savedVo.getSamples()), CollectionUtils.size(reloadSamples));
        }
    }

    @Test
    public void delete() {
        service.delete(fixtures.getTripId(0));
    }
}

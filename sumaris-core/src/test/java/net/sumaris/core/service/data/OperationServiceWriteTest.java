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
import com.google.common.collect.Lists;
import net.sumaris.core.dao.DatabaseResource;
import net.sumaris.core.service.AbstractServiceTest;
import net.sumaris.core.service.referential.PmfmService;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.data.MeasurementVO;
import net.sumaris.core.vo.data.OperationVO;
import net.sumaris.core.vo.data.TripVO;
import net.sumaris.core.vo.data.VesselPositionVO;
import net.sumaris.core.vo.referential.PmfmVO;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Calendar;

public class OperationServiceWriteTest extends AbstractServiceTest {

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.writeDb();

    @Autowired
    private TripService tripService;

    @Autowired
    private OperationService service;

    @Autowired
    private PmfmService pmfmService;


    private TripVO parent;

    @Before
    public void setUp() {
        this.parent = tripService.get(dbResource.getFixtures().getTripId(0));
        Assume.assumeNotNull(this.parent);
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
        recorderDepartment.setId(dbResource.getFixtures().getDepartmentId(0));
        vo.setRecorderDepartment(recorderDepartment);

        // Physical gear
        vo.setPhysicalGearId(1);

        // Measurements
        PmfmVO bottomDepthPmfm = pmfmService.getByLabel("FISHING_DEPTH_M");
        MeasurementVO meas1 = new MeasurementVO();
        meas1.setNumericalValue(15.0);
        meas1.setPmfmId(bottomDepthPmfm.getId());

        vo.setMeasurements(ImmutableList.of(meas1));

        // Save
        OperationVO savedVo = service.save(vo);
        Assert.assertNotNull(savedVo.getId());
        Assert.assertNotNull(savedVo.getQualityFlagId());

        // Full reload
        OperationVO reloadedVo = service.get(savedVo.getId());
        Assert.assertNotNull(reloadedVo);
        Assert.assertEquals(vo.getPositions().size(), CollectionUtils.size(reloadedVo.getPositions()));
        Assert.assertEquals(vo.getMeasurements().size(), CollectionUtils.size(reloadedVo.getMeasurements()));

    }

    @Test
    public void delete() {
        service.delete(dbResource.getFixtures().getTripId(0));
    }
}

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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import net.sumaris.core.dao.DatabaseResource;
import net.sumaris.core.model.referential.pmfm.PmfmEnum;
import net.sumaris.core.service.AbstractServiceTest;
import net.sumaris.core.service.referential.pmfm.PmfmService;
import net.sumaris.core.vo.data.GearUseFeaturesVO;
import net.sumaris.core.vo.data.VesselUseFeaturesVO;
import net.sumaris.core.vo.data.activity.ActivityCalendarFetchOptions;
import net.sumaris.core.vo.data.activity.ActivityCalendarVO;
import net.sumaris.core.vo.data.aggregatedLanding.VesselActivityVO;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class ActivityCalendarServiceWriteTest extends AbstractServiceTest{

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.writeDb();

    @Autowired
    private ActivityCalendarService service;

    @Autowired
    private PmfmService pmfmService;


    @Test
    public void save() {
        ActivityCalendarVO vo = createActivityCalendar(2023, 0);
        ActivityCalendarVO savedVO = service.save(vo);

        Assert.assertNotNull(savedVO);
        Assert.assertNotNull(savedVO.getId());

        // Reload and check
        ActivityCalendarVO reloadedVO = service.get(savedVO.getId());
        Assert.assertNotNull(reloadedVO);

    }

    @Test
    public void saveWithFeatures() {
        int expectedMonth = 12;
        // Create a activityCalendar, with an physical gear
        ActivityCalendarVO activityCalendar = createActivityCalendar(2023, expectedMonth);

        ActivityCalendarVO savedVO = service.save(activityCalendar);

        Assert.assertNotNull(savedVO);
        Assert.assertNotNull(savedVO.getId());


        ActivityCalendarVO reloadedVO = service.get(activityCalendar.getId(), ActivityCalendarFetchOptions.FULL_GRAPH);
        Assert.assertNotNull(reloadedVO.getVesselUseFeatures());
        Assert.assertEquals(expectedMonth, reloadedVO.getVesselUseFeatures().size());

        Assert.assertNotNull(reloadedVO.getGearUseFeatures());
        Assert.assertEquals(expectedMonth, reloadedVO.getGearUseFeatures().size());

//
//        // Reload and check
//        List<OperationVO> savedOperations = operationService.findAllByActivityCalendarId(savedVO.getId(), OperationFetchOptions.DEFAULT);
//        Assert.assertNotNull(savedOperations);
//        Assert.assertEquals(1, savedOperations.size());
//
//        OperationVO saveOperation = savedOperations.get(0);
//        Assert.assertNotNull(saveOperation.getPhysicalGear());
//        Assert.assertNotNull(saveOperation.getPhysicalGear().getId());
//
//        Assert.assertEquals("Operation's physical gear id should be equals to activityCalendar's physical gear",
//                savedVO.getGears().get(0).getId(), saveOperation.getPhysicalGear().getId());

    }

    @Test
    public void delete() {
        ActivityCalendarVO savedVO = null;
        try {
            // Create activityCalendar
            savedVO = service.save(createActivityCalendar(2023));
            Assume.assumeNotNull(savedVO);

            // TODO create activities

        }
        catch(Exception e) {
            Assume.assumeNoException(e);
        }

        if (savedVO != null) {
            service.asyncDelete(savedVO.getId());
        }
    }

    /* -- Protected -- */

    protected ActivityCalendarVO createActivityCalendar(int year, int monthCount) {
        ActivityCalendarVO calendar = DataTestUtils.createActivityCalendar(fixtures, year);

        // Create features
        List<VesselUseFeaturesVO> vesselUseFeatures = Lists.newArrayList();
        calendar.setVesselUseFeatures(vesselUseFeatures);

        List<GearUseFeaturesVO> gearUseFeatures = Lists.newArrayList();
        calendar.setGearUseFeatures(gearUseFeatures);

        for (int i = 0; i < monthCount; i++) {
            // VUF
            VesselUseFeaturesVO vuf = createVesselUseFeatures(year, i+1);
            vuf.setProgram(calendar.getProgram());
            vuf.setVesselId(calendar.getVesselId());
            vesselUseFeatures.add(vuf);

            // GUF
            GearUseFeaturesVO guf = createGearUseFeatures(year, i+1);
            guf.setProgram(calendar.getProgram());
            guf.setVesselId(calendar.getVesselId());
            gearUseFeatures.add(guf);
        }

        return calendar;
    }

    protected VesselUseFeaturesVO createVesselUseFeatures(int year, int month) {
        VesselUseFeaturesVO vo = DataTestUtils.createActivityCalendarVesselUseFeatures(fixtures, year, month);

        vo.setMeasurementValues(ImmutableMap.of(
            PmfmEnum.NB_FISHERMEN.getId(), "2",
            PmfmEnum.DURATION_AT_SEA_DAYS.getId(), "20",
            PmfmEnum.FISHING_DURATION_DAYS.getId(), "20"
        ));

        return vo;
    }

    protected GearUseFeaturesVO createGearUseFeatures(int year, int month) {
        GearUseFeaturesVO vo = DataTestUtils.createActivityCalendarGearUseFeatures(fixtures, year, month);

        vo.setMeasurementValues(ImmutableMap.of(
            PmfmEnum.NB_FISHERMEN.getId(), "2",
            PmfmEnum.DURATION_AT_SEA_DAYS.getId(), "20",
            PmfmEnum.FISHING_DURATION_DAYS.getId(), "20"
        ));

        return vo;
    }
}

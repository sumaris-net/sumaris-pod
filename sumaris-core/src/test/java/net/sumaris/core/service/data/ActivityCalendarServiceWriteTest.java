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
import net.sumaris.core.dao.DatabaseResource;
import net.sumaris.core.model.referential.pmfm.PmfmEnum;
import net.sumaris.core.service.AbstractServiceTest;
import net.sumaris.core.service.referential.pmfm.PmfmService;
import net.sumaris.core.vo.data.FishingAreaVO;
import net.sumaris.core.vo.data.GearUseFeaturesVO;
import net.sumaris.core.vo.data.VesselUseFeaturesVO;
import net.sumaris.core.vo.data.activity.ActivityCalendarFetchOptions;
import net.sumaris.core.vo.data.activity.ActivityCalendarVO;
import net.sumaris.core.vo.data.aggregatedLanding.VesselActivityVO;
import net.sumaris.core.vo.referential.LocationVO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
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

        // Check VUF
        Assert.assertNotNull(reloadedVO.getVesselUseFeatures());
        Assert.assertEquals(expectedMonth, reloadedVO.getVesselUseFeatures().size());
        reloadedVO.getVesselUseFeatures().forEach(vuf -> {
            Assert.assertNotNull(vuf);
            Assert.assertNotNull(vuf.getActivityCalendarId());
            Assert.assertNotNull(vuf.getVesselId());
            Assert.assertNotNull(vuf.getStartDate());
            Assert.assertNotNull(vuf.getEndDate());
            Assert.assertNotNull(vuf.getProgram());
            Assert.assertNotNull(vuf.getProgram().getId());
            Assert.assertNotNull(vuf.getIsActive());

            Assert.assertEquals(3, MapUtils.size(vuf.getMeasurementValues()));
        });

        // Check GUF
        Assert.assertNotNull(reloadedVO.getGearUseFeatures());
        Assert.assertEquals(expectedMonth, reloadedVO.getGearUseFeatures().size());
        reloadedVO.getGearUseFeatures().forEach(guf -> {
            Assert.assertNotNull(guf);
            Assert.assertNotNull(guf.getActivityCalendarId());
            Assert.assertNotNull(guf.getVesselId());
            Assert.assertNotNull(guf.getStartDate());
            Assert.assertNotNull(guf.getEndDate());
            Assert.assertNotNull(guf.getProgram());
            Assert.assertNotNull(guf.getProgram().getId());
            Assert.assertNotNull(guf.getMetier());

            // FIXME fetch origins
            //Assert.assertNotNull(guf.getOrigins());

            Assert.assertNull(guf.getGear());
            Assert.assertNull(guf.getOtherGear());

            Assert.assertNotNull(guf.getFishingAreas());
            Assert.assertEquals(1, CollectionUtils.size(guf.getFishingAreas()));

            guf.getFishingAreas().forEach(fa -> {
                Assert.assertNotNull(fa);
                Assert.assertNotNull(fa.getId());
                Assert.assertNotNull(fa.getLocation());
                Assert.assertNotNull(fa.getLocation().getId());
            });
        });
    }

    @Test
    public void delete() {
        // Create an activity calendar
        ActivityCalendarVO savedVO = null;
        try {
            savedVO = service.save(createActivityCalendar(2023, 1));
            Assume.assumeNotNull(savedVO);
        }
        catch(Exception e) {
            Assume.assumeNoException(e);
        }

        // Then delete it
        if (savedVO != null) {
            service.asyncDelete(savedVO.getId());
        }
    }

    /* -- Protected -- */

    protected ActivityCalendarVO createActivityCalendar(int year, int monthCount) {
        ActivityCalendarVO calendar = DataTestUtils.createActivityCalendar(fixtures, year);

        calendar.setMeasurementValues(ImmutableMap.of(
            PmfmEnum.SURVEY_QUALIFICATION.getId(), "591", // Directe
            PmfmEnum.SURVEY_RELIABILITY.getId(), "600" // Fiable
        ));

        // Create features
        List<VesselUseFeaturesVO> vesselUseFeatures = Lists.newArrayList();
        calendar.setVesselUseFeatures(vesselUseFeatures);

        List<GearUseFeaturesVO> gearUseFeatures = Lists.newArrayList();
        calendar.setGearUseFeatures(gearUseFeatures);

        for (int i = 0; i < monthCount; i++) {
            // VUF
            VesselUseFeaturesVO vuf = createVesselUseFeatures(year, i+1);
            vesselUseFeatures.add(vuf);

            // GUF
            GearUseFeaturesVO guf = createGearUseFeatures(year, i+1);
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

        FishingAreaVO fa = new FishingAreaVO();
        LocationVO rectangle = new LocationVO();
        rectangle.setId(fixtures.getRectangleId(month % 5));
        fa.setLocation(rectangle);

        vo.setFishingAreas(ImmutableList.of(fa));

        return vo;
    }
}

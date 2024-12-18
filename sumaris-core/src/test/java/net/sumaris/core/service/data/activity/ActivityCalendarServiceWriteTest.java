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

package net.sumaris.core.service.data.activity;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import net.sumaris.core.dao.DatabaseResource;
import net.sumaris.core.model.administration.programStrategy.AcquisitionLevelEnum;
import net.sumaris.core.model.referential.pmfm.PmfmEnum;
import net.sumaris.core.model.referential.pmfm.QualitativeValueEnum;
import net.sumaris.core.service.AbstractServiceTest;
import net.sumaris.core.service.data.DataTestUtils;
import net.sumaris.core.util.Beans;
import net.sumaris.core.util.Dates;
import net.sumaris.core.vo.data.DataOriginVO;
import net.sumaris.core.vo.data.FishingAreaVO;
import net.sumaris.core.vo.data.GearUseFeaturesVO;
import net.sumaris.core.vo.data.VesselUseFeaturesVO;
import net.sumaris.core.vo.data.activity.ActivityCalendarFetchOptions;
import net.sumaris.core.vo.data.activity.ActivityCalendarVO;
import net.sumaris.core.vo.referential.location.LocationVO;
import net.sumaris.core.vo.referential.metier.MetierVO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.mutable.MutableInt;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.List;

public class ActivityCalendarServiceWriteTest extends AbstractServiceTest{

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.writeDb();

    @Autowired
    private ActivityCalendarService service;

    @Test
    public void save() {
        ActivityCalendarVO vo = createActivityCalendar(2020, 0);
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
        ActivityCalendarVO source = createActivityCalendar(2021, expectedMonth);

        ActivityCalendarVO savedVO = service.save(source);

        Assert.assertNotNull(savedVO);
        Assert.assertNotNull(savedVO.getId());


        ActivityCalendarVO reloadedVO = service.get(source.getId(), ActivityCalendarFetchOptions.FULL_GRAPH);

        // Check VUF and GUF
        assertValidVesselUseFeatures(reloadedVO, expectedMonth);
        assertValidGearUseFeatures(reloadedVO, expectedMonth);
    }

    @Test
    public void saveAndUpdateOrigins() {
        // Create a activityCalendar, with an physical gear
        ActivityCalendarVO source = createActivityCalendar(2022, 1);
        source = service.save(source);

        Assert.assertNotNull(source);
        Assert.assertNotNull(source.getId());

        // Keep only one guf, then update data origin
        GearUseFeaturesVO guf = source.getGearUseFeatures().get(0);
        DataOriginVO origin = DataOriginVO.builder()
            .program(fixtures.getDefaultProgram())
            .acquisitionLevel(AcquisitionLevelEnum.OPERATION.getLabel())
            .gearUseFeaturesId(source.getId())
            .build();
        guf.setDataOrigins(ImmutableList.of(origin));
        source.setGearUseFeatures(ImmutableList.of(guf));

        // Save and reload
        service.save(source);

        ActivityCalendarVO reloadedVO = service.get(source.getId(), ActivityCalendarFetchOptions.FULL_GRAPH);

        Assert.assertNotNull(reloadedVO);
        Assert.assertNotNull(reloadedVO.getId());

        // Check GUF origin
        Assert.assertNotNull(reloadedVO.getGearUseFeatures());
        Assert.assertEquals(1, reloadedVO.getGearUseFeatures().size());

        GearUseFeaturesVO reloadedGuf = reloadedVO.getGearUseFeatures().get(0);
        Assert.assertNotNull(reloadedGuf);
        Assert.assertNotNull(reloadedGuf.getDataOrigins());
        Assert.assertEquals(1, reloadedGuf.getDataOrigins().size());
        Assert.assertNotNull(reloadedGuf.getDataOrigins().get(0));
        Assert.assertEquals(fixtures.getDefaultProgram().getId(), reloadedGuf.getDataOrigins().get(0).getProgramId());

        Assert.assertNotNull(reloadedGuf.getDataOrigins().get(0).getProgram());
        Assert.assertEquals(fixtures.getDefaultProgram().getId(), reloadedGuf.getDataOrigins().get(0).getProgram().getId());
    }

    @Test
    public void saveUnchanged() {
        int activityCalendarId = 1;

        // Fetch and re-save (to force hash computation)
        ActivityCalendarVO data = service.get(activityCalendarId, ActivityCalendarFetchOptions.FULL_GRAPH);
        data = service.save(data);

        Date maxUpdateDate = Dates.max(
                Beans.getStream(data.getVesselUseFeatures()).map(VesselUseFeaturesVO::getUpdateDate).max(Dates::compare).orElse(null),
                Beans.getStream(data.getGearUseFeatures()).map(GearUseFeaturesVO::getUpdateDate).max(Dates::compare).orElse(null)
        );
        VesselUseFeaturesVO updatedVuf = Beans.getList(data.getVesselUseFeatures()).get(0);
        Assume.assumeNotNull("No VUF in ActivityCalendar#" + activityCalendarId, updatedVuf);
        Assume.assumeNotNull("Missing VUF basePortLocation", updatedVuf.getBasePortLocation());

        // Update first VUF basePortLocation
        LocationVO newBasePortLocation = new LocationVO();
        newBasePortLocation.setId(fixtures.getLocationPortId(1));
        Assume.assumeFalse("VUF basePortLocation should NOT be Location#" + newBasePortLocation.getId(), updatedVuf.getBasePortLocation().getId().equals(newBasePortLocation.getId()));

        updatedVuf.setBasePortLocation(newBasePortLocation);

        // Save with the updated VUF
        ActivityCalendarVO savedData = service.save(data);
        Date savedMaxUpdateDate = Dates.max(
                Beans.getStream(savedData.getVesselUseFeatures())
                        .filter(vuf -> !vuf.getId().equals(updatedVuf.getId()))
                        .map(VesselUseFeaturesVO::getUpdateDate).max(Dates::compare).orElse(null),
                Beans.getStream(savedData.getGearUseFeatures()).map(GearUseFeaturesVO::getUpdateDate).max(Dates::compare).orElse(null)
        );
        Assert.assertEquals("Only updated VesselUseFeatures/gearUseFeatures should have been saved", maxUpdateDate, savedMaxUpdateDate);

        // Ensure saved vuf update date is updated
        VesselUseFeaturesVO savedUpdatedVuf = Beans.getList(savedData.getVesselUseFeatures()).get(0);
        Assert.assertTrue(savedUpdatedVuf.getUpdateDate().after(savedMaxUpdateDate));
    }

    @Test
    public void delete() {
        // Create an activity calendar
        ActivityCalendarVO savedVO = null;
        try {
            savedVO = service.save(createActivityCalendar(2000, 1));
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

    @Test
    public void fixSaveUniqueConstraint(){
        ActivityCalendarVO calendar = createActivityCalendar(2001, 1);

        // Save
        calendar = service.save(calendar);

        // Get metier 1
        GearUseFeaturesVO guf1 = calendar.getGearUseFeatures().get(0);
        guf1.setId(null); // Clean id to simulate a cut/paste
        guf1.setUpdateDate(null);
        guf1.setRankOrder((short)2); // change order
        MetierVO metier1 = guf1.getMetier();
        int metierId1 = metier1.getId();
        FishingAreaVO fa1 = guf1.getFishingAreas().get(0);
        fa1.setId(null);

        // Create metier 2
        int metierId2 = fixtures.getMetierIdForFPO(0);
        Assume.assumeTrue("Should use distinct metier for this test", metierId1 !=  metierId2);
        GearUseFeaturesVO guf2 = new GearUseFeaturesVO();
        Beans.copyProperties(guf1, guf2,
            // Exclude some properties
            GearUseFeaturesVO.Fields.ID, GearUseFeaturesVO.Fields.UPDATE_DATE, GearUseFeaturesVO.Fields.METIER, GearUseFeaturesVO.Fields.RANK_ORDER);
        guf2.setRankOrder((short)1);
        MetierVO metier2 = new MetierVO();
        metier2.setId(metierId2);
        guf2.setMetier(metier2);
        FishingAreaVO fa2 = createFishingArea(2);
        guf2.setFishingAreas(ImmutableList.of(fa2));

        // Inverse metier  order (metier 2 before metier 1)
        calendar.setGearUseFeatures(
            ImmutableList.of(guf2, guf1)
        );

        service.save(calendar);

    }

    /* -- Protected -- */

    protected ActivityCalendarVO createActivityCalendar(int year, int monthCount) {
        ActivityCalendarVO calendar = DataTestUtils.createActivityCalendar(fixtures, year);

        calendar.setMeasurementValues(ImmutableMap.of(
            PmfmEnum.SURVEY_QUALIFICATION.getId(), QualitativeValueEnum.SURVEY_QUALIFICATION_DIRECT.getId().toString(), // Directe
            PmfmEnum.SURVEY_RELIABILITY.getId(), "600" // Fiable
        ));

        // Create features
        List<VesselUseFeaturesVO> vesselUseFeatures = Lists.newArrayList();
        calendar.setVesselUseFeatures(vesselUseFeatures);

        List<GearUseFeaturesVO> gearUseFeatures = Lists.newArrayList();
        calendar.setGearUseFeatures(gearUseFeatures);

        // Init origins
        DataOriginVO[] origins = new DataOriginVO[]{
            DataOriginVO.builder().program(fixtures.getActivityCalendarProgram()).build(), // Année N - 1
            DataOriginVO.builder().program(fixtures.getActivityCalendarPredocProgram()).build(), // Déclaratif/prédoc
            DataOriginVO.builder().build() // No program = Enquêteur
        };

        for (int i = 0; i < monthCount; i++) {
            // VUF
            VesselUseFeaturesVO vuf = createVesselUseFeatures(year, i);
            vesselUseFeatures.add(vuf);

            // GUF
            GearUseFeaturesVO guf = createGearUseFeatures(year, i);
            guf.setDataOrigins(ImmutableList.of(origins[i % 3]));
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

        FishingAreaVO fa = createFishingArea(month);

        vo.setFishingAreas(ImmutableList.of(fa));

        return vo;
    }

    protected FishingAreaVO createFishingArea(int month) {
        FishingAreaVO fa = new FishingAreaVO();
        LocationVO rectangle = new LocationVO();
        rectangle.setId(fixtures.getRectangleId(month % 5));
        fa.setLocation(rectangle);
        return fa;
    }

    protected void assertValidVesselUseFeatures(ActivityCalendarVO vo, int expectedMonth) {
        // Check VUF
        Assert.assertNotNull(vo.getVesselUseFeatures());
        Assert.assertEquals(expectedMonth, vo.getVesselUseFeatures().size());
        vo.getVesselUseFeatures().forEach(vuf -> {
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
    }
    protected void assertValidGearUseFeatures(ActivityCalendarVO vo, int expectedMonth) {

        MutableInt counter = new MutableInt(0);
        Assert.assertNotNull(vo.getGearUseFeatures());
        Assert.assertEquals(expectedMonth, vo.getGearUseFeatures().size());
        vo.getGearUseFeatures().forEach(guf -> {
            Assert.assertNotNull(guf);
            Assert.assertNotNull(guf.getActivityCalendarId());
            Assert.assertNotNull(guf.getVesselId());
            Assert.assertNotNull(guf.getStartDate());
            Assert.assertNotNull(guf.getEndDate());
            Assert.assertNotNull(guf.getProgram());
            Assert.assertNotNull(guf.getProgram().getId());
            Assert.assertNotNull(guf.getMetier());

            Assert.assertNull(guf.getGear());
            Assert.assertNull(guf.getOtherGear());

            // Heck fishing areas
            Assert.assertNotNull(guf.getFishingAreas());
            Assert.assertEquals(1, CollectionUtils.size(guf.getFishingAreas()));
            guf.getFishingAreas().forEach(fa -> {
                Assert.assertNotNull(fa);
                Assert.assertNotNull(fa.getId());
                Assert.assertNotNull(fa.getLocation());
                Assert.assertNotNull(fa.getLocation().getId());
            });

            // Check origins
            boolean surveyOrigin = counter.intValue() % 3 == 2;
            if (!surveyOrigin) {
                Assert.assertEquals(1, CollectionUtils.size(guf.getDataOrigins()));
                Assert.assertNotNull(guf.getDataOrigins().get(0).getProgramId());
                Assert.assertNotNull(guf.getDataOrigins().get(0).getProgram());
                Assert.assertNotNull(guf.getDataOrigins().get(0).getProgram().getId());
            }
            else {
                Assert.assertEquals(0, CollectionUtils.size(guf.getDataOrigins()));
            }

            counter.increment();
        });
    }
}

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
import net.sumaris.core.model.referential.pmfm.PmfmEnum;
import net.sumaris.core.service.AbstractServiceTest;
import net.sumaris.core.service.data.DataTestUtils;
import net.sumaris.core.util.Dates;
import net.sumaris.core.vo.data.FishingAreaVO;
import net.sumaris.core.vo.data.GearUseFeaturesVO;
import net.sumaris.core.vo.data.VesselUseFeaturesVO;
import net.sumaris.core.vo.data.activity.DailyActivityCalendarFetchOptions;
import net.sumaris.core.vo.data.activity.DailyActivityCalendarVO;
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

import java.text.ParseException;
import java.util.Date;
import java.util.List;

public class DailyActivityCalendarServiceWriteTest extends AbstractServiceTest{

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.writeDb();

    @Autowired
    private DailyActivityCalendarService service;

    @Test
    public void save() {
        DailyActivityCalendarVO vo = createDailyActivityCalendar(new Date(), 0);
        DailyActivityCalendarVO savedVO = service.save(vo);

        Assert.assertNotNull(savedVO);
        Assert.assertNotNull(savedVO.getId());

        // Reload and check
        DailyActivityCalendarVO reloadedVO = service.get(savedVO.getId());
        Assert.assertNotNull(reloadedVO);

    }

    @Test
    public void saveWithFeatures() {
        int dayCount = 7;
        // Create a dailyActivityCalendar, with an physical gear
        DailyActivityCalendarVO source = createDailyActivityCalendar(new Date(), dayCount);

        DailyActivityCalendarVO savedVO = service.save(source);

        Assert.assertNotNull(savedVO);
        Assert.assertNotNull(savedVO.getId());


        DailyActivityCalendarVO reloadedVO = service.get(source.getId(), DailyActivityCalendarFetchOptions.FULL_GRAPH);

        // Check VUF and GUF
        assertValidVesselUseFeatures(reloadedVO, dayCount);
        assertValidGearUseFeatures(reloadedVO, dayCount);
    }

    @Test
    public void saveAndUpdate() {
        // Create a dailyActivityCalendar, with an physical gear
        DailyActivityCalendarVO source = createDailyActivityCalendar(new Date(), 1);
        source = service.save(source);

        Assert.assertNotNull(source);
        Assert.assertNotNull(source.getId());

        // Keep only one guf, then update metier
        GearUseFeaturesVO guf = source.getGearUseFeatures().get(0);

        // Update the metier
        MetierVO metier = new MetierVO();
        Integer newMetierId = fixtures.getMetierIdForFPO(0);
        metier.setId(newMetierId);
        guf.setMetier(metier);
        source.setGearUseFeatures(ImmutableList.of(guf));

        // Save and reload
        service.save(source);

        DailyActivityCalendarVO reloadedVO = service.get(source.getId(), DailyActivityCalendarFetchOptions.FULL_GRAPH);

        Assert.assertNotNull(reloadedVO);
        Assert.assertNotNull(reloadedVO.getId());

        // Check metier
        Assert.assertNotNull(reloadedVO.getGearUseFeatures());
        Assert.assertEquals(1, reloadedVO.getGearUseFeatures().size());

        GearUseFeaturesVO reloadedGuf = reloadedVO.getGearUseFeatures().get(0);
        Assert.assertNotNull(reloadedGuf);
        Assert.assertNotNull(reloadedGuf.getMetier());
        Assert.assertEquals(newMetierId, reloadedGuf.getMetier().getId());
    }

    @Test
    public void delete() throws ParseException {

        Date startDate = Dates.parseDate("01/01/2022", "dd/MM/yyyy");
        // Create an activity calendar
        DailyActivityCalendarVO savedVO = null;
        try {
            savedVO = service.save(createDailyActivityCalendar(startDate, 1));
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

    protected DailyActivityCalendarVO createDailyActivityCalendar(Date startDate, int dayCount) {
        DailyActivityCalendarVO calendar = DataTestUtils.createDailyActivityCalendar(fixtures, startDate, dayCount);

        calendar.setMeasurementValues(ImmutableMap.of(
            // TODO: add ObsDeb pmfm (SURVEY_MEASUREMENT) if any
            //PmfmEnum.SURVEY_QUALIFICATION.getId(), "591", // Directe
            //PmfmEnum.SURVEY_RELIABILITY.getId(), "600" // Fiable
        ));

        // Create features
        List<VesselUseFeaturesVO> vesselUseFeatures = Lists.newArrayList();
        calendar.setVesselUseFeatures(vesselUseFeatures);

        List<GearUseFeaturesVO> gearUseFeatures = Lists.newArrayList();
        calendar.setGearUseFeatures(gearUseFeatures);

        for (int i = 0; i < dayCount; i++) {
            // VUF
            VesselUseFeaturesVO vuf = createVesselUseFeatures(startDate, i+1);
            vesselUseFeatures.add(vuf);

            // GUF
            GearUseFeaturesVO guf = createGearUseFeatures(startDate, i+1);
            gearUseFeatures.add(guf);
        }

        return calendar;
    }

    protected VesselUseFeaturesVO createVesselUseFeatures(Date date, int index) {
        VesselUseFeaturesVO vo = DataTestUtils.createVesselUseFeatures(fixtures,
            fixtures.getDailyActivityCalendarProgram(),
            Dates.resetTime(date), Dates.lastSecondOfTheDay(date));

        vo.setMeasurementValues(ImmutableMap.of(
            // TODO review (should use ObsDEB pmfms, not ACTIFLOT pmfms)
            PmfmEnum.NB_FISHERMEN.getId(), "2",
            PmfmEnum.DURATION_AT_SEA_DAYS.getId(), "20",
            PmfmEnum.FISHING_DURATION_DAYS.getId(), "20"
        ));

        return vo;
    }

    protected GearUseFeaturesVO createGearUseFeatures(Date date, int index) {
        GearUseFeaturesVO vo = DataTestUtils.createGearUseFeatures(fixtures,
            fixtures.getDailyActivityCalendarProgram(),
            Dates.resetTime(date), Dates.lastSecondOfTheDay(date));

        FishingAreaVO fa = new FishingAreaVO();
        LocationVO rectangle = new LocationVO();
        rectangle.setId(fixtures.getRectangleId(index % 5));
        fa.setLocation(rectangle);

        vo.setFishingAreas(ImmutableList.of(fa));

        return vo;
    }

    protected void assertValidVesselUseFeatures(DailyActivityCalendarVO vo, int expectedMonth) {
        // Check VUF
        Assert.assertNotNull(vo.getVesselUseFeatures());
        Assert.assertEquals(expectedMonth, vo.getVesselUseFeatures().size());
        vo.getVesselUseFeatures().forEach(vuf -> {
            Assert.assertNotNull(vuf);
            Assert.assertNotNull(vuf.getDailyActivityCalendarId());
            Assert.assertNotNull(vuf.getVesselId());
            Assert.assertNotNull(vuf.getStartDate());
            Assert.assertNotNull(vuf.getEndDate());
            Assert.assertNotNull(vuf.getProgram());
            Assert.assertNotNull(vuf.getProgram().getId());
            Assert.assertNotNull(vuf.getIsActive());

            Assert.assertEquals(3, MapUtils.size(vuf.getMeasurementValues()));
        });
    }
    protected void assertValidGearUseFeatures(DailyActivityCalendarVO vo, int expectedSize) {

        MutableInt counter = new MutableInt(0);
        Assert.assertNotNull(vo.getGearUseFeatures());
        Assert.assertEquals(expectedSize, vo.getGearUseFeatures().size());
        vo.getGearUseFeatures().forEach(guf -> {
            Assert.assertNotNull(guf);
            Assert.assertNotNull(guf.getDailyActivityCalendarId());
            Assert.assertNotNull(guf.getVesselId());
            Assert.assertNotNull(guf.getStartDate());
            Assert.assertNotNull(guf.getEndDate());
            Assert.assertNotNull(guf.getProgram());
            Assert.assertNotNull(guf.getProgram().getId());
            Assert.assertNotNull(guf.getMetier());
            Assert.assertNotNull(guf.getMetier().getId());

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

            counter.increment();
        });
    }
}

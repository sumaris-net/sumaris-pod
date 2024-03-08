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

import net.sumaris.core.dao.DatabaseResource;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.model.data.DataQualityStatusEnum;
import net.sumaris.core.service.AbstractServiceTest;
import net.sumaris.core.service.administration.programStrategy.ProgramService;
import net.sumaris.core.util.Dates;
import net.sumaris.core.vo.administration.programStrategy.ProgramVO;
import net.sumaris.core.vo.data.activity.ActivityCalendarFetchOptions;
import net.sumaris.core.vo.data.activity.ActivityCalendarVO;
import net.sumaris.core.vo.filter.ActivityCalendarFilterVO;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;
import java.util.Date;
import java.util.List;

public class ActivityCalendarServiceReadTest extends AbstractServiceTest{

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.readDb();

    @Autowired
    private ActivityCalendarService service;

    @Autowired
    private ProgramService programService;

    @Test
    public void findAllByDates() throws ParseException {

        // Period start/end
        Date middleYearDay = Dates.parseDate("2023-03-03", "yyyy-MM-dd");
        assertFindAll(ActivityCalendarFilterVO.builder()
                .startDate(Dates.resetTime(middleYearDay))
                .endDate(Dates.lastSecondOfTheDay(middleYearDay))
                .build(),
            1);

        assertFindAll(ActivityCalendarFilterVO.builder()
                .startDate(Dates.parseDate("2023-01-01", "yyyy-MM-dd"))
                .endDate(Dates.parseDate("2023-12-31", "yyyy-MM-dd"))
                .build(),
            1);

        // startDate only
        assertFindAll(ActivityCalendarFilterVO.builder()
                .startDate(Dates.parseDate("2023-03-03", "yyyy-MM-dd"))
                .build(),
            1);

        assertFindAll(ActivityCalendarFilterVO.builder()
                .startDate(Dates.parseDate("2017-03-03", "yyyy-MM-dd"))
                .build(),
            1);

        assertFindAll(ActivityCalendarFilterVO.builder()
                .startDate(Dates.parseDate("2026-01-01", "yyyy-MM-dd"))
                .build(),
            0);

        // endDate only
        assertFindAll(ActivityCalendarFilterVO.builder()
                .endDate(Dates.parseDate("2023-01-01", "yyyy-MM-dd"))
                .build(),
            1);

        assertFindAll(ActivityCalendarFilterVO.builder()
                .endDate(Dates.parseDate("2023-12-31", "yyyy-MM-dd"))
                .build(),
            1);

        Date endDate = Dates.parseDate("2022-12-31", "yyyy-MM-dd");
        assertFindAll(ActivityCalendarFilterVO.builder()
                .endDate(endDate)
                .build(),
            0);
    }

    @Test
    public void findAllByVessel() {

        assertFindAll(ActivityCalendarFilterVO.builder()
                .vesselId(1)
                .build(),
            1);

        assertFindAll(ActivityCalendarFilterVO.builder()
                .vesselId(999)
                .build(),
            0);
    }

    @Test
    public void findAllByProgram() {

        ProgramVO program = programService.getByLabel("SIH-ACTIFLOT");
        Assume.assumeNotNull(program);

        assertFindAll(ActivityCalendarFilterVO.builder()
                .programLabel(program.getLabel())
                .build(),
            1);

        assertFindAll(ActivityCalendarFilterVO.builder()
                .programLabel("FAKE")
                .build(),
            0);


        assertFindAll(ActivityCalendarFilterVO.builder()
                .programIds(new Integer[]{program.getId()})
                .build(),
            1);

        assertFindAll(ActivityCalendarFilterVO.builder()
                .programIds(new Integer[]{-999})
                .build(),
            0);
    }

    @Test
    public void findAllByRecorder() {

        // By recorder department
        assertFindAll(ActivityCalendarFilterVO.builder()
            .recorderDepartmentId(3)
            .build(),
            1);

        assertFindAll(ActivityCalendarFilterVO.builder()
                .recorderDepartmentId(1)
                .build(),
            0);

        // By recorder person
        assertFindAll(ActivityCalendarFilterVO.builder()
            .recorderPersonId(2)
            .build(),
            1);

        assertFindAll(ActivityCalendarFilterVO.builder()
                .recorderPersonId(1)
                .build(),
            0);
    }

    @Test
    public void findAllByQualityStatus() {

        assertFindAll(ActivityCalendarFilterVO.builder()
                .dataQualityStatus(new DataQualityStatusEnum[]{DataQualityStatusEnum.MODIFIED})
                .build(),
            1);

        assertFindAll(ActivityCalendarFilterVO.builder()
                .dataQualityStatus(new DataQualityStatusEnum[]{DataQualityStatusEnum.CONTROLLED})
                .build(),
            0);

        assertFindAll(ActivityCalendarFilterVO.builder()
                .dataQualityStatus(new DataQualityStatusEnum[]{DataQualityStatusEnum.VALIDATED})
                .build(),
            0);

        assertFindAll(ActivityCalendarFilterVO.builder()
                .dataQualityStatus(new DataQualityStatusEnum[]{DataQualityStatusEnum.QUALIFIED})
                .build(),
            0);
    }

    /**
     * /!\ Test need for trash : when deleted a activityCalendar, should fetch all the activityCalendar, and sub-entities
     */
    @Test
    public void getFullGraph() {

        Integer id = fixtures.getActivityCalendarId(0);
        ActivityCalendarVO calendar = service.get(id, ActivityCalendarFetchOptions.FULL_GRAPH);
        Assert.assertNotNull(calendar);
        Assert.assertNotNull(calendar.getVesselSnapshot());
        Assert.assertNotNull(calendar.getVesselSnapshot().getExteriorMarking());

        // FIXME
        //Assert.assertTrue(CollectionUtils.isNotEmpty(calendar.getVesselActivities()));

        // January
        {
            // TODO

        }

        // February
        {
            // TODO

        }

    }

    private void assertFindAll(ActivityCalendarFilterVO filter, int expectedSize) {
        List<ActivityCalendarVO> activityCalendars = service.findAll(filter, Page.builder().offset(0).size(100).build(), null);
        Assert.assertNotNull(activityCalendars);
        Assert.assertEquals(expectedSize, activityCalendars.size());
    }

}

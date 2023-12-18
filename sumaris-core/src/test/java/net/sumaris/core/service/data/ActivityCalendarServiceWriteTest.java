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
import net.sumaris.core.service.referential.pmfm.PmfmService;
import net.sumaris.core.vo.data.*;
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
        ActivityCalendarVO vo = createActivityCalendar(2023);
        ActivityCalendarVO savedVO = service.save(vo);

        Assert.assertNotNull(savedVO);
        Assert.assertNotNull(savedVO.getId());

        // Reload and check
        ActivityCalendarVO reloadedVO = service.get(savedVO.getId());
        Assert.assertNotNull(reloadedVO);

    }

    @Test
    public void saveWithActivities() {
        // Create a activityCalendar, with an physical gear
        ActivityCalendarVO activityCalendar = createActivityCalendar(2023);

        List<VesselActivityVO> activities = Lists.newArrayList();

        // january
        {
            VesselActivityVO month = createVesselActivity(1);
            activities.add(month);
        }
        // february
        {
            VesselActivityVO month = createVesselActivity(2);
            activities.add(month);
        }

        activityCalendar.setVesselActivities(activities);

        ActivityCalendarVO savedVO = service.save(activityCalendar);

        Assert.assertNotNull(savedVO);
        Assert.assertNotNull(savedVO.getId());


        ActivityCalendarVO reloadedVO = service.get(activityCalendar.getId(), ActivityCalendarFetchOptions.FULL_GRAPH);
        Assert.assertNotNull(reloadedVO.getVesselActivities());
        Assert.assertEquals(2, reloadedVO.getVesselActivities().size());

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

    protected ActivityCalendarVO createActivityCalendar(int year) {
        return DataTestUtils.createActivityCalendar(fixtures, pmfmService, year);
    }

    protected VesselActivityVO createVesselActivity(int month) {
        VesselActivityVO vo = new VesselActivityVO();
        // TODO
        return vo;
    }
}

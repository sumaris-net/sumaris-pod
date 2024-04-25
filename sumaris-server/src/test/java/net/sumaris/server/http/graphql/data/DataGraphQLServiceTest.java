package net.sumaris.server.http.graphql.data;

/*-
 * #%L
 * Sumaris3 Core :: Server
 * %%
 * Copyright (C) 2017 - 2020 Ifremer
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
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.vo.data.activity.ActivityCalendarVO;
import net.sumaris.core.vo.filter.ActivityCalendarFilterVO;
import net.sumaris.server.DatabaseFixtures;
import net.sumaris.server.DatabaseResource;
import net.sumaris.server.http.graphql.AbstractGraphQLServiceTest;
import org.junit.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;

@Slf4j
public class DataGraphQLServiceTest extends AbstractGraphQLServiceTest {

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.writeDb();

    @Autowired
    protected DatabaseFixtures fixtures;

    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @Test
    public void findAllActivityCalendars() {

        Integer activityCalendarProgramId = fixtures.getActivityCalendarProgram().getId();
        Integer[] registrationLocationIds = fixtures.getRegistrationLocationIdsForSharedActivityCalendar();
        Assume.assumeTrue(registrationLocationIds.length == 2);

        // First user
        {
            Assume.assumeTrue(authenticate("debut.calendrier@ifremer.fr", "demo"));

            ActivityCalendarFilterVO filter = ActivityCalendarFilterVO.builder()
                .programIds(new Integer[]{activityCalendarProgramId})
                .registrationLocationId(registrationLocationIds[0])
                .build();
            ArrayList<ActivityCalendarVO> list = getResponse("activityCalendars", ArrayList.class, ActivityCalendarVO.class, asObjectNode(
                ImmutableMap.<String, Object>builder()
                    .put("filter", filter)
                    .build()));
            Assert.assertEquals(1, list.size());
        }

        // Second user
        {
            Assume.assumeTrue(authenticate("fin.calendrier@ifremer.fr", "demo"));

            ActivityCalendarFilterVO filter = ActivityCalendarFilterVO.builder()
                .registrationLocationId(registrationLocationIds[1])
                .programIds(new Integer[]{activityCalendarProgramId})
                .build();
            ArrayList<ActivityCalendarVO> list = getResponse("activityCalendars", ArrayList.class, ActivityCalendarVO.class, asObjectNode(
                ImmutableMap.<String, Object>builder()
                    .put("filter", filter)
                    .build()));
            Assert.assertNotNull(list);
            Assert.assertEquals(1, list.size());
        }
    }

}

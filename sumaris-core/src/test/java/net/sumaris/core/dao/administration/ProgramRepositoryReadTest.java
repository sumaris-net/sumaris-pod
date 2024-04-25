package net.sumaris.core.dao.administration;

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

import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.AbstractDaoTest;
import net.sumaris.core.dao.DatabaseResource;
import net.sumaris.core.dao.administration.programStrategy.ProgramRepository;
import org.junit.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.Assert.*;

@Slf4j
public class ProgramRepositoryReadTest extends AbstractDaoTest {

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.readDb();

    @Autowired
    private ProgramRepository repository;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        setCommitOnTearDown(false); // this is need because of delete test
    }

    @Test
    public void getReadableProgramIdsByUserId() {

        // user demo
        {
            List<Integer> ids = repository.getReadableProgramIdsByUserId(2);
            assertNotNull(ids);
            assertTrue(ids.size() > 0);

            assertTrue(ids.contains(40));
            assertTrue(ids.contains(60));

        }

        // user inactive
        {
            List<Integer> ids = repository.getReadableProgramIdsByUserId(4);
            assertNotNull(ids);
            assertEquals(0, ids.size());
        }

    }

    @Test
    public void getProgramLocationIdsByUserId() {

        // Viewer (all locations)
        {
            int userId = 7;
            List<Integer> programIds = repository.getReadableProgramIdsByUserId(userId);
            programIds.forEach(programId -> {
                List<Integer> locationIds = repository.getProgramLocationIdsByUserId(userId, programId);
                assertNotNull(locationIds);
                assertFalse(locationIds.isEmpty());
                assertTrue(locationIds.contains(null));
            });
        }

        // Activity calendar
        {
            // ACTIFLOT - Observer with right on BR - Brest
            {
                int userId = 11;
                List<Integer> programIds = repository.getReadableProgramIdsByUserId(userId);
                Assume.assumeNotNull(programIds);
                Assume.assumeTrue(programIds.contains(fixtures.getActivityCalendarProgram().getId()));

                List<Integer> locationIds = repository.getProgramLocationIdsByUserId(userId, fixtures.getActivityCalendarProgram().getId());
                assertNotNull(locationIds);
                assertFalse(locationIds.isEmpty());
                assertTrue(locationIds.contains(43)); // BR - Brest
            }

            // ACTIFLOT - Observer with right on BL - Boulogne
            {
                int userId = 12;
                List<Integer> programIds = repository.getReadableProgramIdsByUserId(userId);
                Assume.assumeNotNull(programIds);
                Assume.assumeTrue(programIds.contains(fixtures.getActivityCalendarProgram().getId()));

                List<Integer> locationIds = repository.getProgramLocationIdsByUserId(userId, fixtures.getActivityCalendarProgram().getId());
                assertNotNull(locationIds);
                assertFalse(locationIds.isEmpty());

                assertTrue(locationIds.contains(44)); // BL - Boulogne
            }
        }
    }

//    @Test
//    public void getAllPrivilegesByUserId() {
//        ProgramVO program = repository.getByLabel("SIH-OBSBIO");
//        assertNotNull(program);
//
//        List<ReferentialVO> privileges = repository.getAllPrivilegesByUserId(
//                program.getId(),
//                fixtures.getPersonId(1));
//        assertNotNull(privileges);
//        assertTrue(privileges.size() > 0);
//    }
}

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
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.Assert.*;

@Slf4j
public class ProgramRepositoryReadTest extends AbstractDaoTest{

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
    public void getProgramIdByUserId() {

        // user demo
        {
            List<Integer> ids = repository.getProgramIdsByUserId(2);
            assertNotNull(ids);
            assertTrue(ids.size() > 0);

            assertTrue(ids.contains(40));
            assertTrue(ids.contains(60));

        }

        // user inactive
        {
            List<Integer> ids = repository.getProgramIdsByUserId(4);
            assertNotNull(ids);
            assertEquals(0, ids.size());
        }

    }

}

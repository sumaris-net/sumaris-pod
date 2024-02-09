package net.sumaris.core.dao.data;

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

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.AbstractDaoTest;
import net.sumaris.core.dao.DatabaseResource;
import net.sumaris.core.dao.data.landing.LandingRepository;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.vo.data.LandingFetchOptions;
import net.sumaris.core.vo.data.LandingVO;
import net.sumaris.core.vo.data.OperationFetchOptions;
import net.sumaris.core.vo.data.OperationVO;
import net.sumaris.core.vo.filter.LandingFilterVO;
import net.sumaris.core.vo.filter.OperationFilterVO;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@Slf4j
public class LandingRepositoryReadTest extends AbstractDaoTest {

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.readDb();

    @Autowired
    private LandingRepository repository;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        setCommitOnTearDown(false); // this is need because of delete test
    }

    @Test
    public void findAll() {

        // by observed location
        {
            LandingFilterVO filter = LandingFilterVO.builder().observedLocationId(1).build();
            //filter.setProgramLabel("ADAP-MER");

            List<LandingVO> vos = repository.findAll(filter);
            assertNotNull(vos);
            assertTrue(vos.size() > 0);
        }

        // by program
        {
            LandingFilterVO filter = LandingFilterVO.builder().programLabel("ADAP-CONTROLE").build();

            List<LandingVO> vos = repository.findAll(filter);
            assertNotNull(vos);
            assertTrue(vos.size() > 0);
        }

    }

    @Test
    public void findAllWithVesselIds() {
        assertFindAll(LandingFilterVO.builder()
            .vesselIds(new Integer[]{fixtures.getVesselId(1)})
            .build(), 1);

        assertFindAll(LandingFilterVO.builder()
            .vesselIds(new Integer[]{fixtures.getScientificVesselId()})
            .build(), 0);
    }

    protected List<LandingVO> assertFindAll(@NonNull LandingFilterVO filter, int expectedSize) {
        List<LandingVO> vos = repository.findAll(filter, 0, 100, OperationVO.Fields.ID, SortDirection.ASC,
            LandingFetchOptions.DEFAULT);
        Assert.assertNotNull(vos);
        Assert.assertEquals(expectedSize, vos.size());
        return vos;
    }
}

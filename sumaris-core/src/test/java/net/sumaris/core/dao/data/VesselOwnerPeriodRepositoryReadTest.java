package net.sumaris.core.dao.data;

/*-
 * #%L
 * SUMARiS:: Core
 * %%
 * Copyright (C) 2018 - 2019 SUMARiS Consortium
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
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.AbstractDaoTest;
import net.sumaris.core.dao.DatabaseResource;
import net.sumaris.core.dao.data.vessel.VesselOwnerPeriodRepository;
import net.sumaris.core.dao.data.vessel.VesselRepository;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.model.administration.programStrategy.ProgramEnum;
import net.sumaris.core.model.data.VesselFeatures;
import net.sumaris.core.model.referential.StatusEnum;
import net.sumaris.core.model.referential.VesselTypeEnum;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.data.VesselOwnerPeriodVO;
import net.sumaris.core.vo.data.VesselVO;
import net.sumaris.core.vo.filter.VesselFilterVO;
import net.sumaris.core.vo.filter.VesselOwnerFilterVO;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.List;

/**
 * @author peck7 on 06/11/2019.
 */
@Slf4j
public class VesselOwnerPeriodRepositoryReadTest extends AbstractDaoTest {

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.readDb();

    @Autowired
    private VesselOwnerPeriodRepository repository;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        setCommitOnTearDown(false); // this is need because of delete test
    }

    @Test
    public void findByFilter() {
        Integer vesselId = fixtures.getVesselId(0);

        Date now = new Date();
        VesselOwnerFilterVO filter = VesselOwnerFilterVO.builder()
            .vesselId(vesselId)
            .programLabel(ProgramEnum.SIH.getLabel())
            .startDate(now).endDate(now)
            .build();
        List<VesselOwnerPeriodVO> result = repository.findAll(filter, Page.create(0, 10, VesselOwnerPeriodVO.Fields.START_DATE, SortDirection.ASC));

        Assert.assertNotNull(result);
        Assert.assertEquals(2, result.size());
        VesselOwnerPeriodVO period1 = result.get(0);
        Assert.assertEquals(vesselId, period1.getId().getVesselId());
        Assert.assertNotNull(period1.getStartDate());
        Assert.assertNotNull(period1.getVesselId());
        Assert.assertNotNull(period1.getVesselOwner());
        Assert.assertEquals("19720111", period1.getVesselOwner().getRegistrationCode());
        Assert.assertEquals("DUPOND", period1.getVesselOwner().getLastName());
        Assert.assertNotNull(period1.getVesselOwner().getCity());
        Assert.assertNotNull(period1.getVesselOwner().getCountryLocation());
        Assert.assertNotNull(period1.getVesselOwner().getProgram());

        VesselOwnerPeriodVO period2 = result.get(1);
        Assert.assertEquals(vesselId, period2.getId().getVesselId());
        Assert.assertNotNull(period2.getStartDate());
        Assert.assertNotNull(period2.getVesselId());
        Assert.assertNotNull(period2.getVesselOwner());
        Assert.assertEquals("SPR6950", period2.getVesselOwner().getRegistrationCode());
        Assert.assertEquals("NOTRE DAME DE PARIS", period2.getVesselOwner().getLastName());
        Assert.assertNotNull(period2.getVesselOwner().getCity());
        Assert.assertNotNull(period2.getVesselOwner().getCountryLocation());
        Assert.assertNotNull(period2.getVesselOwner().getProgram());
    }

    @Test
    public void findByFilter_withProgram() {
        Integer vesselId = fixtures.getVesselId(0);

        // Get invalid program label
        {
            VesselOwnerFilterVO filter = VesselOwnerFilterVO.builder()
                    .vesselId(vesselId)
                    .programLabel("FAKE")
                    .build();
            List<VesselOwnerPeriodVO> result = repository.findAll(filter, Page.create(0, 10, VesselOwnerPeriodVO.Fields.START_DATE, SortDirection.ASC));
            Assert.assertNotNull(result);
            Assert.assertEquals(0, result.size());
        }

        // Get valid program label
        {
            VesselOwnerFilterVO filter = VesselOwnerFilterVO.builder()
                    .vesselId(vesselId)
                    .programLabel(ProgramEnum.SIH.getLabel())
                    .build();
            List<VesselOwnerPeriodVO> result = repository.findAll(filter, Page.create(0, 10, VesselOwnerPeriodVO.Fields.START_DATE, SortDirection.ASC));
            Assert.assertNotNull(result);
            Assert.assertFalse(result.isEmpty());
        }


        // Get invalid program ids
        {
            VesselOwnerFilterVO filter = VesselOwnerFilterVO.builder()
                    .vesselId(vesselId)
                    .programIds(new Integer[]{-9999})
                    .build();
            List<VesselOwnerPeriodVO> result = repository.findAll(filter, Page.create(0, 10, VesselOwnerPeriodVO.Fields.START_DATE, SortDirection.ASC));
            Assert.assertNotNull(result);
            Assert.assertTrue(result.isEmpty());
        }

        // Get valid program ids
        {
            VesselOwnerFilterVO filter = VesselOwnerFilterVO.builder()
                    .vesselId(vesselId)
                    .programIds(new Integer[]{ProgramEnum.SIH.getId()})
                    .build();
            List<VesselOwnerPeriodVO> result = repository.findAll(filter, Page.create(0, 10, VesselOwnerPeriodVO.Fields.START_DATE, SortDirection.ASC));
            Assert.assertNotNull(result);
            Assert.assertFalse(result.isEmpty());
        }

    }


    @Test
    public void findByFilter_withDate() {
        Integer vesselId = fixtures.getVesselId(0);

        VesselOwnerFilterVO filter = VesselOwnerFilterVO.builder()
                .vesselId(vesselId)
                .programLabel("FAKE")
                .build();
        List<VesselOwnerPeriodVO> result = repository.findAll(filter, Page.create(0, 10, VesselOwnerPeriodVO.Fields.START_DATE, SortDirection.ASC));
        Assert.assertNotNull(result);
        Assert.assertEquals(0, result.size());

    }
}

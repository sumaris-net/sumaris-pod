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

import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.AbstractDaoTest;
import net.sumaris.core.dao.DatabaseResource;
import net.sumaris.core.dao.data.vessel.VesselOwnerPeriodRepository;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.util.Dates;
import net.sumaris.core.vo.data.VesselOwnerPeriodVO;
import net.sumaris.core.vo.filter.VesselFilterVO;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.List;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 * @since 2.9.19
 */
@Slf4j
public class VesselOwnerPeriodRepositoryWriteTest extends AbstractDaoTest {

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.writeDb();

    @Autowired
    private VesselOwnerPeriodRepository repository;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        setCommitOnTearDown(false); // this is need because of delete test
    }

    @Test
    public void save() {
        Date startDate = Dates.resetTime(new Date());
        Integer vesselId = fixtures.getVesselId(1); // Vessel 2
        Integer vesselOwnerId = fixtures.getVesselOwnerId(0); // Vessel owner 1

        VesselOwnerPeriodVO period = new VesselOwnerPeriodVO();
        period.setVesselId(vesselId);
        period.setVesselOwnerId(vesselOwnerId);
        period.setStartDate(startDate);
        period.setEndDate(null);
        VesselOwnerPeriodVO savedPeriod = repository.save(period);

        // Check if can fetch
        {
            List<VesselOwnerPeriodVO> reloadPeriods = repository.findAll(
                    VesselFilterVO.builder()
                            .vesselId(vesselId)
                            .build(),
                    Page.create(0, 10, VesselOwnerPeriodVO.Fields.START_DATE, SortDirection.DESC)
            );
            Assert.assertNotNull(reloadPeriods);
            VesselOwnerPeriodVO lastPeriod = reloadPeriods.get(0);
            Assert.assertEquals(period, lastPeriod);
        }
    }

    @Test
    public void delete() {
        Integer vesselId = fixtures.getVesselId(0); // Vessel 1
        Integer vesselOwnerId = fixtures.getVesselOwnerId(0); // Vessel Owner 1
        List<VesselOwnerPeriodVO> periods = repository.findAll(VesselFilterVO.builder()
                .vesselId(vesselId)
                .vesselOwnerId(vesselOwnerId)
                .build(), Page.create(0, 1, VesselOwnerPeriodVO.Fields.START_DATE, SortDirection.ASC));
        Assert.assertTrue(CollectionUtils.isNotEmpty(periods));
        VesselOwnerPeriodVO firstPeriod = CollectionUtils.extractSingleton(periods);
        Assert.assertNotNull(firstPeriod.getId());
        repository.deleteById(firstPeriod.getId());
    }
}

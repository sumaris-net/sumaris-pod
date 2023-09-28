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

package net.sumaris.core.service.data.vessel;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.DatabaseFixtures;
import net.sumaris.core.dao.DatabaseResource;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.model.administration.programStrategy.ProgramEnum;
import net.sumaris.core.model.data.Vessel;
import net.sumaris.core.model.data.VesselRegistrationPeriod;
import net.sumaris.core.model.referential.StatusEnum;
import net.sumaris.core.service.AbstractServiceTest;
import net.sumaris.core.util.Dates;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.data.VesselSnapshotVO;
import net.sumaris.core.vo.data.VesselVO;
import net.sumaris.core.vo.data.vessel.VesselFetchOptions;
import net.sumaris.core.vo.filter.VesselFilterVO;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import java.util.Date;
import java.util.List;
import java.util.Set;

@Slf4j
@ActiveProfiles("hsqldb")
public class VesselServiceReadTest extends AbstractServiceTest{

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.readDb();

    @Autowired
    protected DatabaseFixtures fixtures;

    @Autowired
    private VesselService service;

    @Autowired
    private VesselSnapshotService vesselSnapshotService;

    @Test
    public void countAll() {

        VesselFilterVO filter = VesselFilterVO.builder()
            .programLabel(ProgramEnum.SIH.getLabel())
            .statusIds(ImmutableList.of(StatusEnum.ENABLE.getId()))
            .searchAttributes(new String[]{
                StringUtils.doting(Vessel.Fields.VESSEL_REGISTRATION_PERIODS, VesselRegistrationPeriod.Fields.REGISTRATION_CODE)
            })
            .searchText(fixtures.getVesselRegistrationCode(0))
            .build();

        // Start + End date
        {
            filter.setStartDate(new Date());
            filter.setEndDate(Dates.fromISODateTimeString("2025-01-01T00:00:00.000Z"));

            long now = System.currentTimeMillis();
            long count = service.countByFilter(filter);
            log.info("[start/end dates] vesselCount: {} - responseTime: {}ms", count, System.currentTimeMillis() - now);
            Assert.assertEquals(1L, count);

            // Reset filter dates
            filter.setStartDate(null);
            filter.setEndDate(null);
        }
        // Start date only
        {
            // Valid date (today)
            filter.setDate(new Date());
            long now = System.currentTimeMillis();
            long count = service.countByFilter(filter);
            log.info("[startDate only] vesselCount: {} - responseTime: {}ms", count, System.currentTimeMillis() - now);
            Assert.assertEquals(1L, count);

            // Invalid date ( > year 2100 - see NVL condition in vessel query specification)
            filter.setDate(Dates.fromISODateTimeString("1980-01-01T00:00:00.000Z"));
            now = System.currentTimeMillis();
            count = service.countByFilter(filter);
            log.info("[startDate only] vesselCount: {} - responseTime: {}ms", count, System.currentTimeMillis() - now);
            Assert.assertEquals(0L, count);
        }
    }

    @Test
    public void findAll() {

        VesselFilterVO filter = VesselFilterVO.builder()
            .programLabel(ProgramEnum.SIH.getLabel())
            .build();

        filter.setStatusIds(ImmutableList.of(StatusEnum.ENABLE.getId()));

        filter.setSearchAttributes(new String[]{
            Vessel.Fields.VESSEL_REGISTRATION_PERIODS + "." + VesselRegistrationPeriod.Fields.REGISTRATION_CODE
        });
        filter.setSearchText("FRA000851751");

        net.sumaris.core.dao.technical.Page page = net.sumaris.core.dao.technical.Page.builder()
            .offset(0).size(10)
            .sortBy(StringUtils.doting(Vessel.Fields.VESSEL_REGISTRATION_PERIODS, VesselRegistrationPeriod.Fields.REGISTRATION_CODE))
            .sortDirection(SortDirection.ASC)
            .build();

        List<VesselVO> result = service.findAll(filter, page, VesselFetchOptions.DEFAULT);
        Assert.assertNotNull(result);
        Assert.assertFalse(result.isEmpty());

        AssertVessel.assertAllValid(result);

        // Check no duplication
        AssertVessel.assertUniqueIds(result);
    }


    @Test
    public void findSnapshotByFilter() {

        VesselFilterVO filter = VesselFilterVO.builder()
            .programLabel(ProgramEnum.SIH.getLabel())
            .build();

        filter.setStatusIds(ImmutableList.of(StatusEnum.ENABLE.getId()));
        filter.setDate(new Date());

        filter.setSearchAttributes(new String[]{
            VesselRegistrationPeriod.Fields.REGISTRATION_CODE
        });
        filter.setSearchText("FRA000851*");

        net.sumaris.core.dao.technical.Page page = net.sumaris.core.dao.technical.Page.builder()
            .offset(0).size(10)
            .sortBy(VesselRegistrationPeriod.Fields.REGISTRATION_CODE)
            .sortDirection(SortDirection.ASC)
            .build();

        List<VesselSnapshotVO> result = vesselSnapshotService.findAll(filter, page, VesselFetchOptions.DEFAULT);
        Assert.assertNotNull(result);
        Assert.assertFalse(result.isEmpty());

        // Check no duplicate
        final Set<Integer> ids = Sets.newHashSet();
        for (VesselSnapshotVO vessel: result) {

            AssertVessel.assertValid(vessel);

            Assert.assertFalse("Duplicated vessel id=" + vessel.getId(), ids.contains(vessel.getId()));
            ids.add(vessel.getId());
        }
    }

    @Test
    public void get() {

        VesselVO result = service.get(fixtures.getVesselId(0));

        AssertVessel.assertValid(result);
    }

    /* -- protected -- */


}

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
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.DatabaseFixtures;
import net.sumaris.core.dao.DatabaseResource;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.model.administration.programStrategy.ProgramEnum;
import net.sumaris.core.model.data.Vessel;
import net.sumaris.core.model.data.VesselRegistrationPeriod;
import net.sumaris.core.model.referential.StatusEnum;
import net.sumaris.core.service.AbstractServiceTest;
import net.sumaris.core.service.data.vessel.VesselService2;
import net.sumaris.core.util.Dates;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.data.DataFetchOptions;
import net.sumaris.core.vo.data.VesselVO;
import net.sumaris.core.vo.filter.VesselFilterVO;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import java.util.Date;
import java.util.List;

@Slf4j
@ActiveProfiles("hsqldb")
public class VesselServiceReadTest extends AbstractServiceTest{

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.readDb();

    @Autowired
    protected DatabaseFixtures fixtures;

    @Autowired
    private VesselService2 service;

    @Test
    public void countAll() {

        VesselFilterVO filter = VesselFilterVO.builder()
            .programLabel(ProgramEnum.SIH.getLabel())
            .build();

        filter.setStatusIds(ImmutableList.of(StatusEnum.ENABLE.getId()));

        filter.setSearchAttributes(new String[]{
            StringUtils.doting(Vessel.Fields.VESSEL_REGISTRATION_PERIODS, VesselRegistrationPeriod.Fields.REGISTRATION_CODE)
        });
        filter.setSearchText(fixtures.getVesselRegistrationCode(0));

        // Start + End date
        {
            filter.setStartDate(new Date());
            filter.setEndDate(Dates.fromISODateTimeString("2025-01-01T00:00:00.000Z"));

            long now = System.currentTimeMillis();
            Long count = service.countByFilter(filter);
            Assert.assertNotNull(count);
            log.info("[start/end dates] vesselCount: {} - responseTime: {}ms", count, System.currentTimeMillis() - now);
            Assert.assertEquals(1, count.intValue());

            // Reset filter dates
            filter.setStartDate(null);
            filter.setEndDate(null);
        }
        // Start date only
        {
            // Valid date (today)
            filter.setDate(new Date());
            long now = System.currentTimeMillis();
            Long count = service.countByFilter(filter);
            Assert.assertNotNull(count);
            log.info("[startDate only] vesselCount: {} - responseTime: {}ms", count, System.currentTimeMillis() - now);
            Assert.assertEquals(1, count.intValue());

            // Invalid date ( > year 2100 - see NVL condition in vessel query specification)
            filter.setDate(Dates.fromISODateTimeString("1980-01-01T00:00:00.000Z"));
            now = System.currentTimeMillis();
            count = service.countByFilter(filter);
            Assert.assertNotNull(count);
            log.info("[startDate only] vesselCount: {} - responseTime: {}ms", count, System.currentTimeMillis() - now);
            Assert.assertEquals(0, count.intValue());
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
        filter.setSearchText("CN851751");

        List<VesselVO> vessels = service.findAll(filter, Page.builder().size(10).build(), DataFetchOptions.DEFAULT);
        Assert.assertNotNull(vessels);
        Assert.assertTrue(vessels.size() > 0);
    }
}

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
import net.sumaris.core.dao.DatabaseResource;
import net.sumaris.core.model.administration.programStrategy.ProgramEnum;
import net.sumaris.core.model.data.Vessel;
import net.sumaris.core.model.data.VesselFeatures;
import net.sumaris.core.model.data.VesselRegistrationPeriod;
import net.sumaris.core.model.referential.StatusEnum;
import net.sumaris.core.model.referential.VesselTypeEnum;
import net.sumaris.core.service.AbstractServiceTest;
import net.sumaris.core.util.Dates;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.data.VesselSnapshotVO;
import net.sumaris.core.vo.data.VesselVO;
import net.sumaris.core.vo.data.vessel.VesselFetchOptions;
import net.sumaris.core.vo.filter.VesselFilterVO;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.Date;
import java.util.List;

@Ignore("Use only SIH Oracle database")
@ActiveProfiles("oracle")
@TestPropertySource(locations = "classpath:application-oracle.properties")
@Slf4j
public class VesselServiceReadOracleTest extends AbstractServiceTest{

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.writeDb("oracle");
    
    @Autowired
    private VesselService service;

    @Autowired
    private VesselSnapshotService vesselSnapshotService;

    @Test
    public void countAll() {

        VesselFilterVO baseFilter = VesselFilterVO.builder()
            .programLabel(ProgramEnum.SIH.getLabel())
            .statusIds(ImmutableList.of(StatusEnum.ENABLE.getId()))
            .searchAttributes(new String[] {
                StringUtils.doting(Vessel.Fields.VESSEL_REGISTRATION_PERIODS, VesselRegistrationPeriod.Fields.REGISTRATION_CODE)
            })
            .searchText("851751")
            .build();

        // Start + End date
        {
            VesselFilterVO filter = baseFilter.clone();
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
            VesselFilterVO filter = baseFilter.clone();
            // Valid date (today)
            filter.setStartDate(new Date());
            long now = System.currentTimeMillis();
            long count = service.countByFilter(filter);
            log.info("[startDate only] vesselCount: {} - responseTime: {}ms", count, System.currentTimeMillis() - now);
            Assert.assertEquals(1L, count);

            // Invalid date ( > year 2100 - see NVL condition in vessel query specification)
            filter.setDate(Dates.fromISODateTimeString("2101-01-01T00:00:00.000Z"));
            now = System.currentTimeMillis();
            count = service.countByFilter(filter);
            log.info("[startDate only] vesselCount: {} - responseTime: {}ms", count, System.currentTimeMillis() - now);
            Assert.assertEquals(0L, count);
        }
    }

    @Test
    public void findAll() {

        VesselFilterVO baseFilter = VesselFilterVO.builder()
            .programLabel(ProgramEnum.SIH.getLabel())
            .statusIds(ImmutableList.of(StatusEnum.ENABLE.getId()))
            .build();

        net.sumaris.core.dao.technical.Page page = net.sumaris.core.dao.technical.Page.builder()
            .offset(0).size(10).build();

        // Find by registration code
        {
            VesselFilterVO filter = baseFilter.clone();
            filter.setSearchAttributes(new String[] {
                StringUtils.doting(Vessel.Fields.VESSEL_REGISTRATION_PERIODS, VesselRegistrationPeriod.Fields.REGISTRATION_CODE)
            });
            filter.setSearchText("851751");

            List<VesselVO> result = service.findAll(filter, page, VesselFetchOptions.DEFAULT);
            Assert.assertNotNull(result);
            Assert.assertFalse(result.isEmpty());
            AssertVessel.assertAllValid(result);
        }

        // Find by name
        {
            VesselFilterVO filter = baseFilter.clone();
            filter.setVesselTypeId(VesselTypeEnum.SCIENTIFIC_RESEARCH_VESSEL.getId());
            filter.setSearchAttributes(new String[] {
                StringUtils.doting(Vessel.Fields.VESSEL_FEATURES, VesselFeatures.Fields.NAME)
            });
            filter.setSearchText("ANTEA");

            List<VesselVO> result = service.findAll(filter, page, VesselFetchOptions.DEFAULT);

            Assert.assertNotNull(result);
            Assert.assertFalse(result.isEmpty());
            AssertVessel.assertAllValid(result);
            AssertVessel.assertUniqueIds(result);
        }
    }

    @Test
    public void findAllSnapshot() {

        VesselFilterVO baseFilter = VesselFilterVO.builder()
            .programLabel(ProgramEnum.SIH.getLabel())
            .statusIds(ImmutableList.of(StatusEnum.ENABLE.getId()))
            .startDate(new Date())
            .build();

        AssertVesselSpecification assertSpec = AssertVesselSpecification.builder()
                .requiredExteriorMarking(false)
                .requiredBasePortLocation(false)
                .build();

        net.sumaris.core.dao.technical.Page page = net.sumaris.core.dao.technical.Page.builder()
            .offset(0).size(10).build();

        // Find by registration code
        {
            VesselFilterVO filter = baseFilter.clone();
            filter.setSearchAttributes(new String[] {
                VesselRegistrationPeriod.Fields.REGISTRATION_CODE
            });
            filter.setSearchText("851*");

            // First execution
            long startTime1 = System.currentTimeMillis();
            List<VesselSnapshotVO> result1 = vesselSnapshotService.findAll(filter, page, VesselFetchOptions.DEFAULT);
            long duration1 = System.currentTimeMillis() - startTime1;
            Assert.assertNotNull(result1);
            Assert.assertFalse(result1.isEmpty());
            AssertVessel.assertAllValid(result1, assertSpec);

            // Second execution, to test cache
            long startTime2 = System.currentTimeMillis();
            List<VesselSnapshotVO> result2 = vesselSnapshotService.findAll(filter, page, VesselFetchOptions.DEFAULT);
            long duration2 = System.currentTimeMillis() - startTime2;
            Assert.assertNotNull(result2);
            Assert.assertEquals(result1.size(), result2.size());

            // Make sure cache is more efficient
            Assert.assertTrue(duration2 < duration1);
            log.info("Cached query: x{} more efficient", Math.round(duration1 / duration2));
        }
    }

    @Test
    public void countSnapshotsByFilterByRegistrationLocationId() {

        Date today = new Date();
        VesselFilterVO filter = VesselFilterVO.builder()
            .programLabel(ProgramEnum.SIH.getLabel())
            .statusIds(ImmutableList.of(StatusEnum.ENABLE.getId()))
            .startDate(today)
            .searchAttributes(new String[] {
                VesselRegistrationPeriod.Fields.REGISTRATION_CODE
            })
            .searchText("851751")
            .build();

        // Count FRA vessels
        filter.setRegistrationLocationId(12 /*= FRA country*/);
        Long countFra = vesselSnapshotService.countByFilter(filter);
        log.info("FRA vessel count:{}", countFra);

        // Count NLD vessels
        filter.setRegistrationLocationId(30 /*= NLD*/);
        Long countNld = vesselSnapshotService.countByFilter(filter);
        log.info("NLD vessel count={}", countNld);

        Assert.assertTrue(countFra > countNld);

    }
}

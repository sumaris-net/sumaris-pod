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
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.model.IEntity;
import net.sumaris.core.model.administration.programStrategy.ProgramEnum;
import net.sumaris.core.model.data.Vessel;
import net.sumaris.core.model.data.VesselFeatures;
import net.sumaris.core.model.data.VesselRegistrationPeriod;
import net.sumaris.core.model.referential.StatusEnum;
import net.sumaris.core.service.AbstractServiceTest;
import net.sumaris.core.util.Dates;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.util.elasticsearch.ElasticsearchResource;
import net.sumaris.core.vo.data.VesselRegistrationPeriodVO;
import net.sumaris.core.vo.data.VesselSnapshotVO;
import net.sumaris.core.vo.data.VesselVO;
import net.sumaris.core.vo.data.vessel.VesselFetchOptions;
import net.sumaris.core.vo.filter.VesselFilterVO;
import net.sumaris.core.vo.filter.VesselRegistrationFilterVO;
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

    @ClassRule
    public static final ElasticsearchResource nodeResource = new ElasticsearchResource();

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
        VesselFilterVO filter;
        Page page;
        String searchAttribute;

        // Search on registration code
        {
            searchAttribute = StringUtils.doting(Vessel.Fields.VESSEL_REGISTRATION_PERIODS, VesselRegistrationPeriod.Fields.REGISTRATION_CODE);
            filter = createFilterBuilder(searchAttribute)
                .searchText("851751")
                .build();
            page = createPage(searchAttribute);

            List<VesselVO> result = service.findAll(filter, page, VesselFetchOptions.DEFAULT);
            Assert.assertNotNull(result);
            Assert.assertFalse(result.isEmpty());
            AssertVessel.assertAllValid(result);
            AssertVessel.assertUniqueIds(result);
        }

        // Search on international registration code
        {
            searchAttribute = StringUtils.doting(Vessel.Fields.VESSEL_REGISTRATION_PERIODS, VesselRegistrationPeriod.Fields.INT_REGISTRATION_CODE);
            filter = createFilterBuilder(searchAttribute)
                .searchText("FRA000851*")
                .build();
            page = createPage(searchAttribute);

            List<VesselVO> result = service.findAll(filter, page, VesselFetchOptions.DEFAULT);
            Assert.assertNotNull(result);
            Assert.assertFalse(result.isEmpty());
            AssertVessel.assertAllValid(result);
            AssertVessel.assertUniqueIds(result);
        }

        // Search on name
        {
            searchAttribute = StringUtils.doting(Vessel.Fields.VESSEL_FEATURES, VesselFeatures.Fields.NAME);
            filter = createFilterBuilder(searchAttribute)
                .searchText("nav")
                .build();
            page = createPage(searchAttribute);

            List<VesselVO> result = service.findAll(filter, page, VesselFetchOptions.DEFAULT);
            Assert.assertNotNull(result);
            Assert.assertFalse(result.isEmpty());
            AssertVessel.assertAllValid(result);
            AssertVessel.assertUniqueIds(result);
        }
    }


    @Test
    public void findSnapshotByFilter() {

        VesselFilterVO filter;
        Page page;

        {

            filter = createFilterBuilder()
                .searchAttributes(new String[]{VesselRegistrationPeriod.Fields.REGISTRATION_CODE})
                .searchText("FRA000851*")
                .build();
            filter.setDate(new Date());
            page = createPage(VesselRegistrationPeriod.Fields.REGISTRATION_CODE);

            List<VesselSnapshotVO> result = vesselSnapshotService.findAll(filter, page, VesselFetchOptions.DEFAULT);
            Assert.assertNotNull(result);
            Assert.assertFalse(result.isEmpty());
            AssertVessel.assertAllValid(result);
            AssertVessel.assertNoDuplicate(result);
        }
    }

    @Test
    public void get() {

        VesselVO result = service.get(fixtures.getVesselId(0));

        AssertVessel.assertValid(result);
    }

    @Test
    public void findRegistrationPeriodsByFilter() {

        int year = 2023;
        Date startDate = Dates.getFirstDayOfYear(year);
        Date endDate = Dates.getLastSecondOfYear(year);
        int vesselId = fixtures.getVesselWithManyRegistrationLocations();
        VesselRegistrationFilterVO filter = VesselRegistrationFilterVO.builder()
                .vesselId(vesselId)
                .startDate(startDate)
                .endDate(endDate)
                .build();
        List<VesselRegistrationPeriodVO> result = service.findRegistrationPeriodsByFilter(filter, null);
        Assert.assertNotNull(result);
        Assert.assertEquals(2, result.size());

        Assert.assertEquals(1, result.stream().filter((period) -> period.getEndDate() == null).count());
        Assert.assertEquals(1, result.stream().filter((period) -> period.getEndDate() != null).count());
    }

    /* -- protected -- */

    private VesselFilterVO.VesselFilterVOBuilder createFilterBuilder(String... searchAttributes) {
        Date now = new Date();
        return VesselFilterVO.builder()
            .programLabel(ProgramEnum.SIH.getLabel())
            .statusIds(ImmutableList.of(StatusEnum.ENABLE.getId(), StatusEnum.TEMPORARY.getId()))
            .searchAttributes(searchAttributes)
            .startDate(now)
            .endDate(now)
            ;
    }


    private Page createPage(String sortBy) {
        return Page.create(0, 100, sortBy, SortDirection.ASC);
    }

}

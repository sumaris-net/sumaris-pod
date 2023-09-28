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
import net.sumaris.core.dao.DatabaseResource;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.model.administration.programStrategy.ProgramEnum;
import net.sumaris.core.model.data.VesselRegistrationPeriod;
import net.sumaris.core.model.referential.StatusEnum;
import net.sumaris.core.model.referential.VesselTypeEnum;
import net.sumaris.core.service.AbstractServiceTest;
import net.sumaris.core.util.elasticsearch.ElasticsearchResource;
import net.sumaris.core.vo.data.VesselSnapshotVO;
import net.sumaris.core.vo.data.vessel.VesselFetchOptions;
import net.sumaris.core.vo.filter.VesselFilterVO;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.List;


public class VesselSnapshotReadTest extends AbstractServiceTest{

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.readDb();

    @ClassRule
    public static final ElasticsearchResource nodeResource = new ElasticsearchResource();

    @Autowired
    private VesselSnapshotService service;

    @Before
    public void setup() {
        // Index vessels
        service.indexVesselSnapshots(createFilterBuilder().build());
    }

    @Test
    public void findAll() {
        String searchAttribute;
        VesselFilterVO filter;
        Page page;

        // Search on name
        {
            searchAttribute = VesselSnapshotVO.Fields.NAME;
            filter = createFilterBuilder(searchAttribute)
                .searchText("navire")
                .build();
            page = createPage(searchAttribute);

            List<VesselSnapshotVO> result = service.findAll(filter, page, VesselFetchOptions.DEFAULT);

            Assert.assertNotNull(result);
            Assert.assertFalse(result.isEmpty());
            AssertVessel.assertAllValid(result);
            AssertVessel.assertNoDuplicate(result);
        }

        // Search on registration code
        {
            searchAttribute = VesselSnapshotVO.Fields.REGISTRATION_CODE;
            filter = createFilterBuilder(searchAttribute)
                .searchText("851*")
                .build();
            page = createPage(searchAttribute);

            List<VesselSnapshotVO> result = service.findAll(filter, page, VesselFetchOptions.DEFAULT);

            Assert.assertNotNull(result);
            Assert.assertFalse(result.isEmpty());
            AssertVessel.assertAllValid(result);
            AssertVessel.assertNoDuplicate(result);
            AssertVessel.assertAnyMatch(result, v -> "851751".equals(v.getRegistrationCode()));
        }

        // Search on international registration code
        {
            searchAttribute = VesselSnapshotVO.Fields.INT_REGISTRATION_CODE;
            filter = createFilterBuilder(searchAttribute)
                .searchText("FRA000851*")
                .build();
            page = createPage(searchAttribute);

            List<VesselSnapshotVO> result = service.findAll(filter, page, VesselFetchOptions.DEFAULT);

            Assert.assertNotNull(result);
            Assert.assertFalse(result.isEmpty());
            AssertVessel.assertAllValid(result);
            AssertVessel.assertNoDuplicate(result);
            AssertVessel.assertAnyMatch(result, v -> "FRA000851751".equals(v.getIntRegistrationCode()));
        }

        // Search on name + registration code
        {
            filter = createFilterBuilder(VesselSnapshotVO.Fields.NAME, VesselSnapshotVO.Fields.REGISTRATION_CODE)
                .searchText("nav*")
                .build();
            page = createPage(VesselSnapshotVO.Fields.REGISTRATION_CODE);

            List<VesselSnapshotVO> result = service.findAll(filter, page, VesselFetchOptions.DEFAULT);

            Assert.assertNotNull(result);
            Assert.assertFalse(result.isEmpty());
            AssertVessel.assertAllValid(result);
            AssertVessel.assertNoDuplicate(result);
            AssertVessel.assertAnyMatch(result, v -> "Navire 1".equals(v.getName()));

            filter.setSearchText("851*");
            result = service.findAll(filter, page, VesselFetchOptions.DEFAULT);

            Assert.assertNotNull(result);
            Assert.assertFalse(result.isEmpty());
            AssertVessel.assertAllValid(result);
            AssertVessel.assertNoDuplicate(result);
            AssertVessel.assertNoneMatch(result, v -> "123456".equalsIgnoreCase(v.getRegistrationCode()));
        }
    }

    /* -- internal functions -- */

    private VesselFilterVO.VesselFilterVOBuilder createFilterBuilder(String... searchAttributes) {
        Date now = new Date();
        return VesselFilterVO.builder()
            .programLabel(ProgramEnum.SIH.getLabel())
            .statusIds(ImmutableList.of(StatusEnum.ENABLE.getId(), StatusEnum.TEMPORARY.getId()))
            .searchAttributes(searchAttributes)
            .startDate(now)
            .endDate(now)
            .vesselTypeIds(new Integer[]{VesselTypeEnum.FISHING_VESSEL.getId(), VesselTypeEnum.SCIENTIFIC_RESEARCH_VESSEL.getId()})
            ;
    }

    private Page createPage(String sortBy) {
        return Page.create(0, 100, sortBy, SortDirection.ASC);
    }
}

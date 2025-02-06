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
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.AbstractDaoTest;
import net.sumaris.core.dao.data.vessel.IVesselSnapshotSpecifications;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.model.administration.programStrategy.ProgramEnum;
import net.sumaris.core.model.referential.StatusEnum;
import net.sumaris.core.model.referential.VesselTypeEnum;
import net.sumaris.core.vo.data.VesselSnapshotVO;
import net.sumaris.core.vo.data.vessel.VesselFetchOptions;
import net.sumaris.core.vo.filter.VesselFilterVO;
import org.junit.Assert;
import org.junit.Test;

import java.util.Date;
import java.util.List;

/**
 * @author peck7 on 06/11/2019.
 */
@Slf4j
public abstract class VesselSnapshotRepositoryAbstractReadTest<T extends IVesselSnapshotSpecifications> extends AbstractDaoTest {

    private T repository;


    protected void setUp(T repository) throws Exception{
        super.setUp();
        this.repository = repository;
    }

    @Test
    public void count() {
        Long count = repository.count(VesselFilterVO.builder().build());

        Assert.assertNotNull(count);
        Assert.assertEquals(4L, count.longValue());
    }

    @Test
    public void findByFilter_defaultSearchAttributes() {
        VesselFetchOptions fetchOptions = VesselFetchOptions.builder()
            .withBasePortLocation(true)
            .build();

        // Search on registration code, int registration code, and name
        List<VesselSnapshotVO> result = repository.findAll(
            createFilterBuilder()
            .searchText("CN851")
            .build(),
            createPage(),
            fetchOptions);

        assertVessels(2, result);
    }

    public void findByFilter_registrationCode() {
        VesselFetchOptions fetchOptions = VesselFetchOptions.builder()
            .withBasePortLocation(true)
            .build();

        // Search on registration code
        {
            List<VesselSnapshotVO> result = repository.findAll(
                createFilterBuilder(VesselSnapshotVO.Fields.REGISTRATION_CODE)
                    .searchText("851")
                    .build(),
                createPage(VesselSnapshotVO.Fields.REGISTRATION_CODE),
                fetchOptions);

            assertVessels(1, result);
        }
    }

    @Test
    public void findByFilter_name() {
        VesselFetchOptions fetchOptions = VesselFetchOptions.builder()
            .withBasePortLocation(true)
            .build();
        String searchAttribute = VesselSnapshotVO.Fields.NAME;

        // Search on name, without wildcard
        {
            List<VesselSnapshotVO> result = repository.findAll(
                createFilterBuilder(searchAttribute)
                    // WARN: should test a sub-part of a word
                    .searchText("nav")
                    .build(),
                createPage(searchAttribute),
                fetchOptions);

            assertVessels(1, result);
        }

        // Search on name, with wildcard
        {
            List<VesselSnapshotVO> result = repository.findAll(
                createFilterBuilder(searchAttribute)
                    .searchText("?avir*1")
                    .build(),
                createPage(searchAttribute),
                fetchOptions);

            assertVessels(1, result);
        }

        // Search on name, with many words
        {
            List<VesselSnapshotVO> result = repository.findAll(
                createFilterBuilder(searchAttribute)
                    .searchText("nav* 1")
                    .build(),
                createPage(searchAttribute),
                fetchOptions);

            assertVessels(1, result);
        }
    }

    @Test
    public void findByFilter_otherCriteria() {
        VesselFetchOptions fetchOptions = VesselFetchOptions.builder()
            .withBasePortLocation(true)
            .build();
        Date now = new Date();

        // Search on vessel type
        {
            List<VesselSnapshotVO> result = repository.findAll(
                VesselFilterVO.builder()
                    .vesselTypeId(VesselTypeEnum.SCIENTIFIC_RESEARCH_VESSEL.getId())
                    .build(),
                createPage(),
                fetchOptions);
            Assert.assertNotNull(result);
            Assert.assertEquals(1, result.size());
        }

        // Search on vessel id
        {
            List<VesselSnapshotVO> result = repository.findAll(
                VesselFilterVO.builder()
                    .startDate(now)
                    .endDate(now)
                    .vesselId(1)
                    .build(),
                createPage(),
                fetchOptions);

            assertVessels(1, result);
        }
    }


    /* -- protected function -- */

    protected void assertVessels(int expectedSize, List<VesselSnapshotVO> result) {
        Assert.assertNotNull(result);
        Assert.assertEquals(expectedSize, result.size());

        // Vessel 1
        if (expectedSize >= 1) {
            VesselSnapshotVO vessel1 = result.get(0);
            assertVesselId(1, vessel1);

            // Check vessel features
            Assert.assertEquals("CN851751", vessel1.getExteriorMarking());
            Assert.assertNotNull(vessel1.getBasePortLocation());
            Assert.assertEquals(10, vessel1.getBasePortLocation().getId().intValue());

            // Check vessel registration period
            Assert.assertEquals("851751", vessel1.getRegistrationCode());
            Assert.assertEquals("FRA000851751", vessel1.getIntRegistrationCode());
            Assert.assertNotNull(vessel1.getRegistrationLocation());
            Assert.assertEquals(1, vessel1.getRegistrationLocation().getId().intValue());
        }

        // Vessel 2
        if (expectedSize >= 2) {
            VesselSnapshotVO vessel2 = result.get(1);
            assertVesselId(2, vessel2);

            // Check vessel features
            Assert.assertEquals("CN851769", vessel2.getExteriorMarking());
            Assert.assertNotNull(vessel2.getBasePortLocation());
            Assert.assertEquals(10, vessel2.getBasePortLocation().getId().intValue());

            // Should have no vessel registration period
            Assert.assertNull("Vessel 2 should have no vessel registration period and no registrationCode", vessel2.getRegistrationCode());
            Assert.assertNull("Vessel 2 should have no vessel registration period and no intRegistrationCode", vessel2.getIntRegistrationCode());
            Assert.assertNull("Vessel 2 should have no vessel registration period and no registrationLocation", vessel2.getRegistrationLocation());
        }
    }

    protected VesselFilterVO.VesselFilterVOBuilder createFilterBuilder(String... searchAttributes) {
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

    protected Page createPage() {
        return Page.create(0, 100, VesselSnapshotVO.Fields.VESSEL_FEATURES_ID, SortDirection.ASC);
    }

    protected Page createPage(@NonNull String sortBy) {
        return Page.create(0, 100, sortBy, SortDirection.ASC);
    }

    protected void assertVesselId(int vesselId, VesselSnapshotVO vesselSnapshot) {
        Assert.assertNotNull(vesselSnapshot);
        Assert.assertNotNull(vesselSnapshot.getVesselId()); // Should be the VesselFeatures.id
        Assert.assertNotNull(vesselSnapshot.getVesselId());
        Assert.assertEquals(vesselId, vesselSnapshot.getVesselId().intValue());
    }

}

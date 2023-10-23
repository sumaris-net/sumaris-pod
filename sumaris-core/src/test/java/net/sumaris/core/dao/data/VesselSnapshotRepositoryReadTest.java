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
import net.sumaris.core.dao.data.vessel.VesselSnapshotRepository;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.model.referential.StatusEnum;
import net.sumaris.core.model.referential.VesselTypeEnum;
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

/**
 * @author peck7 on 06/11/2019.
 */
@Slf4j
public class VesselSnapshotRepositoryReadTest extends AbstractDaoTest {

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.readDb();

    @Autowired
    private VesselSnapshotRepository repository;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        setCommitOnTearDown(false); // this is need because of delete test
    }

    @Test
    public void findByFilter() {
        Date now = new Date();
        VesselFilterVO filter = VesselFilterVO.builder()
            .vesselTypeId(VesselTypeEnum.FISHING_VESSEL.getId())
            .statusIds(ImmutableList.of(StatusEnum.ENABLE.getId(), StatusEnum.TEMPORARY.getId()))
            .searchText("CN851")
            .startDate(now)
            .endDate(now)
            .build();
        VesselFetchOptions fetchOptions = VesselFetchOptions.builder()
            .withBasePortLocation(true)
            .build();

        List<VesselSnapshotVO> result = repository.findAll(filter, 0, 10, VesselSnapshotVO.Fields.ID, SortDirection.ASC, fetchOptions);

        Assert.assertNotNull(result);
        Assert.assertEquals(2, result.size());
        VesselSnapshotVO vessel1 = result.get(0);
        Assert.assertEquals(2, vessel1.getId().intValue()); // Vessel features ID
        Assert.assertEquals(1, vessel1.getVesselId().intValue());
        Assert.assertEquals("CN851751", vessel1.getExteriorMarking());
        Assert.assertEquals("851751", vessel1.getRegistrationCode());
        Assert.assertEquals("FRA000851751", vessel1.getIntRegistrationCode());
        Assert.assertNotNull(vessel1.getBasePortLocation());
        Assert.assertEquals(10, vessel1.getBasePortLocation().getId().intValue());
        Assert.assertNotNull(vessel1.getRegistrationLocation());
        Assert.assertEquals(1, vessel1.getRegistrationLocation().getId().intValue());
        VesselSnapshotVO vessel2 = result.get(1);
        Assert.assertEquals(3, vessel2.getId().intValue());
        Assert.assertEquals(2, vessel2.getVesselId().intValue());
        Assert.assertEquals("CN851769", vessel2.getExteriorMarking());
        Assert.assertNull(vessel2.getRegistrationCode());
        Assert.assertNotNull(vessel2.getBasePortLocation());
        Assert.assertEquals(10, vessel2.getBasePortLocation().getId().intValue());
        Assert.assertNull(vessel2.getRegistrationLocation());
    }
}

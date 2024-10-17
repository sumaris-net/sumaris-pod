package net.sumaris.core.service.data;

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

import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.DatabaseResource;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.model.data.IWithVesselSnapshotEntity;
import net.sumaris.core.model.data.Landing;
import net.sumaris.core.service.AbstractServiceTest;
import net.sumaris.core.service.data.vessel.VesselSnapshotService;
import net.sumaris.core.util.Dates;
import net.sumaris.core.vo.data.LandingVO;
import net.sumaris.core.vo.data.VesselSnapshotVO;
import net.sumaris.core.vo.filter.LandingFilterVO;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.stream.Collectors;

@Ignore("Use only on SFA Oracle database")
@ActiveProfiles("oracle")
@TestPropertySource(locations = "classpath:application-oracle.properties")
@Slf4j
public class LandingServiceReadOracleTest extends AbstractServiceTest {

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.writeDb("oracle");

    @Autowired
    private LandingService service;

    @Autowired
    private VesselSnapshotService vesselSnapshotService;

    @Test
    public void findAllSortByVesselRegistrationCode() {

        LandingFilterVO filter = LandingFilterVO.builder().observedLocationId(22133).build();
        // standard sort
        {
            List<LandingVO> vos = service.findAll(filter, Page.builder().size(10).sortBy(Landing.Fields.ID).build(), null);
            Assert.assertNotNull(vos);
            Assert.assertEquals(10, vos.size());
            logVesselRegistrationCode(vos);
        }
        // vessel sort
        {
            List<LandingVO> vos = service.findAll(filter, Page.builder().size(10).sortBy(Landing.Fields.VESSEL).build(), null);
            Assert.assertNotNull(vos);
            Assert.assertEquals(10, vos.size());
            logVesselRegistrationCode(vos);
        }
        // vessel sort reverse
        {
            List<LandingVO> vos = service.findAll(filter, Page.builder().size(10).sortBy(Landing.Fields.VESSEL).sortDirection(SortDirection.DESC).build(), null);
            Assert.assertNotNull(vos);
            Assert.assertEquals(10, vos.size());
            logVesselRegistrationCode(vos);
        }
    }

    protected void logVesselRegistrationCode(List<LandingVO> landings) {
        fillVesselSnapshot(landings);
        log.info(landings.stream().map(LandingVO::getVesselSnapshot).map(VesselSnapshotVO::getRegistrationCode).collect(Collectors.joining(",")));
    }

    protected <T extends IWithVesselSnapshotEntity<?, VesselSnapshotVO>> void fillVesselSnapshot(List<T> beans) {
        // Add vessel if need
        beans.forEach(bean -> {
            if (bean.getVesselSnapshot() == null && bean.getVesselId() != null && bean.getVesselSnapshot().getName() == null) {
                bean.setVesselSnapshot(vesselSnapshotService.getByIdAndDate(bean.getVesselId(), Dates.resetTime(bean.getVesselDateTime())));
            }
        });
    }

}

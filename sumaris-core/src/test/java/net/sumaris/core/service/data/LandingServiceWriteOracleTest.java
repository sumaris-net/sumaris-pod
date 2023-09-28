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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.DatabaseFixtures;
import net.sumaris.core.dao.DatabaseResource;
import net.sumaris.core.model.data.IWithVesselSnapshotEntity;
import net.sumaris.core.service.AbstractServiceTest;
import net.sumaris.core.service.data.vessel.VesselService;
import net.sumaris.core.service.data.vessel.VesselSnapshotService;
import net.sumaris.core.util.Dates;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.data.FishingAreaVO;
import net.sumaris.core.vo.data.LandingVO;
import net.sumaris.core.vo.data.TripVO;
import net.sumaris.core.vo.data.VesselSnapshotVO;
import net.sumaris.core.vo.referential.LocationVO;
import net.sumaris.core.vo.referential.MetierVO;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Ignore("Use only SIH Oracle database")
@ActiveProfiles("oracle")
@TestPropertySource(locations = "classpath:application-oracle.properties")
@Slf4j
public class LandingServiceWriteOracleTest extends AbstractServiceTest {

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.writeDb("oracle");

    @Autowired
    private LandingService service;

    @Autowired
    private VesselService vesselService;

    @Autowired
    private VesselSnapshotService vesselSnapshotService;

    @Autowired
    protected DatabaseFixtures fixtures;

    @Test
    public void saveWithATrip() {
        LandingVO landing = createLanding();

        // Add a trip
        TripVO trip = new TripVO();
        {
            trip.setDepartureLocation(landing.getLocation());
            trip.setReturnLocation(landing.getLocation());
            trip.setDepartureDateTime(Dates.addDays(landing.getDateTime(), -2));
            trip.setReturnDateTime(landing.getDateTime());
            landing.setTrip(trip);

            {
                FishingAreaVO fa = new FishingAreaVO();
                LocationVO zone = new LocationVO();
                zone.setId(fixtures.getLocationPortId(0));
                fa.setLocation(zone);
                trip.setFishingAreas(ImmutableList.of(fa));
            }

            {
                MetierVO metier = new MetierVO();
                metier.setId(fixtures.getMetierIdForOTB(0));
                trip.setMetiers(ImmutableList.of(metier));
            }
        }

        LandingVO savedLanding = service.save(landing);
        Assert.assertNotNull(savedLanding);
        Assert.assertNotNull(savedLanding.getId());
        Date firstUpdateDate = savedLanding.getUpdateDate();

        TripVO savedTrip = savedLanding.getTrip();
        Assert.assertNotNull(savedTrip);
        Assert.assertNotNull(savedTrip.getId());

        // Change the trip date
        savedTrip.setReturnDateTime(new Date());
        savedLanding.setTrip(savedTrip);

        savedLanding = service.save(savedLanding);
        Assert.assertNotNull(savedLanding);
        Assert.assertNotNull(savedLanding.getId());
        Date secondUpdateDate = savedLanding.getUpdateDate();

        Assert.assertTrue(secondUpdateDate.getTime() > firstUpdateDate.getTime());
    }

    protected void logVesselRegistrationCode(List<LandingVO> landings) {
        fillVesselSnapshot(landings);
        log.info(landings.stream().map(LandingVO::getVesselSnapshot).map(VesselSnapshotVO::getRegistrationCode).collect(Collectors.joining(",")));
    }

    protected <T extends IWithVesselSnapshotEntity<?, VesselSnapshotVO>> void fillVesselSnapshot(List<T> beans) {
        // Add vessel if need
        beans.forEach(bean -> {
            if (bean.getVesselSnapshot() != null && bean.getVesselId() != null && bean.getVesselSnapshot().getName() == null) {
                bean.setVesselSnapshot(vesselSnapshotService.getByIdAndDate(bean.getVesselId(), Dates.resetTime(bean.getVesselDateTime())));
            }
        });
    }

    protected LandingVO createLanding() {
        LandingVO vo = new LandingVO();
        vo.setProgram(fixtures.getDefaultProgram());
        vo.setDateTime(new Date());
        vo.setCreationDate(new Date());

        LocationVO location = new LocationVO();
        location.setId(fixtures.getLocationPortId(0));
        vo.setLocation(location);

        // Vessel
        VesselSnapshotVO vessel = new VesselSnapshotVO();
        vessel.setId(fixtures.getVesselId(0));
        vo.setVesselSnapshot(vessel);

        // Department
        DepartmentVO recorderDepartment = new DepartmentVO();
        recorderDepartment.setId(fixtures.getDepartmentId(0));
        vo.setRecorderDepartment(recorderDepartment);

        // Observers
        PersonVO observer1 = new PersonVO();
        observer1.setId(fixtures.getPersonId(0));
        PersonVO observer2 = new PersonVO();
        observer2.setId(fixtures.getPersonId(1));
        vo.setObservers(ImmutableSet.of(observer1, observer2));

        return vo;
    }
}

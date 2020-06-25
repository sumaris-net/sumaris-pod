package net.sumaris.core.service.data;

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

import net.sumaris.core.dao.DatabaseResource;
import net.sumaris.core.service.AbstractServiceTest;
import net.sumaris.core.vo.data.TripVO;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;

/**
 * @author peck7 on 06/12/2018.
 */
public class TripServiceQualityTest extends AbstractServiceTest {

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.writeDb();

    @Autowired
    private TripService service;

    @Test
    public void control() {

        TripVO trip = service.get(dbResource.getFixtures().getTripId(0));
        Assert.assertNotNull(trip);

        // Make sure control date is NOT set
        if (trip.getControlDate() != null) {
            trip.setControlDate(null);
            service.save(trip, false, false);
            trip = service.get(dbResource.getFixtures().getTripId(0));
            Assert.assertNotNull(trip);
        }

        Assert.assertNull(trip.getControlDate());

        trip = service.control(trip);

        Assert.assertNotNull(trip.getControlDate());
    }

    @Test
    public void validate() {

        TripVO trip = service.get(dbResource.getFixtures().getTripId(0));
        Assert.assertNotNull(trip);

        trip.setControlDate(new Date());
        Assert.assertNull(trip.getValidationDate());

        trip = service.validate(trip);

        Assert.assertNotNull(trip.getValidationDate());

    }

    @Test
    public void unvalidate() {

        TripVO trip = service.get(dbResource.getFixtures().getTripId(0));
        Assert.assertNotNull(trip);

        trip.setControlDate(new Date());
        trip.setValidationDate(new Date());

        trip = service.unvalidate(trip);

        Assert.assertNull(trip.getValidationDate());

    }


}

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
import net.sumaris.core.vo.data.ObservedLocationSaveOptions;
import net.sumaris.core.vo.data.ObservedLocationVO;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;

/**
 * @author peck7 on 06/12/2018.
 */
public class ObservedLocationServiceQualityTest extends AbstractServiceTest {

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.writeDb();

    @Autowired
    private ObservedLocationService service;

    @Test
    public void control() {

        ObservedLocationVO obs = service.get(fixtures.getObservedLocationId(0));
        Assert.assertNotNull(obs);
        if (obs.getControlDate() != null) {
            obs.setControlDate(null);
            obs.setValidationDate(null);
            obs = service.save(obs, ObservedLocationSaveOptions.builder().build());
        }
        Assert.assertNull(obs.getControlDate());

        obs = service.control(obs, null);

        Assert.assertNotNull(obs.getControlDate());
    }

    @Test
    public void validate() {

        ObservedLocationVO trip = service.get(fixtures.getObservedLocationId(0));
        Assert.assertNotNull(trip);

        trip.setControlDate(new Date());
        Assert.assertNull(trip.getValidationDate());

        trip = service.validate(trip, null);

        Assert.assertNotNull(trip.getValidationDate());
    }

    @Test
    public void unvalidate() {

        ObservedLocationVO trip = service.get(fixtures.getObservedLocationId(0));
        Assert.assertNotNull(trip);

        trip.setControlDate(new Date());
        trip.setValidationDate(new Date());

        trip = service.unvalidate(trip, null);

        Assert.assertNull(trip.getValidationDate());
    }

}

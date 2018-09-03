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

import net.sumaris.core.dao.DatabaseResource;
import net.sumaris.core.service.AbstractServiceTest;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.data.TripVO;
import net.sumaris.core.vo.data.VesselFeaturesVO;
import net.sumaris.core.vo.referential.LocationVO;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;

public class TripServiceWriteTest extends AbstractServiceTest{

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.writeDb();

    @Autowired
    private TripService service;

    @Test
    public void save() {
        TripVO vo = new TripVO();
        vo.setDepartureDateTime(new Date());
        vo.setReturnDateTime(new Date());

        VesselFeaturesVO vessel = new VesselFeaturesVO();
        vessel.setVesselId(dbResource.getFixtures().getVesselId(0));
        vo.setVesselFeatures(vessel);

        LocationVO departureLocation = new LocationVO();
        departureLocation.setId(dbResource.getFixtures().getLocationPortId(0));
        vo.setDepartureLocation(departureLocation);

        LocationVO returnLocation = new LocationVO();
        returnLocation.setId(dbResource.getFixtures().getLocationPortId(0));
        vo.setReturnLocation(returnLocation);

        vo.setCreationDate(new Date());

        DepartmentVO recorderDepartment = new DepartmentVO();
        recorderDepartment.setId(dbResource.getFixtures().getDepartmentId(0));
        vo.setRecorderDepartment(recorderDepartment);

        // Physical gear
        //PhysicalGear


        service.save(vo);
    }

    @Test
    public void delete() {
        service.delete(dbResource.getFixtures().getTripId(0));
    }
}

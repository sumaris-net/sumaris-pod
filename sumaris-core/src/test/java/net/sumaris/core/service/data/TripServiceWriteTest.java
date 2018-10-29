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

import com.google.common.collect.ImmutableMap;
import net.sumaris.core.dao.DatabaseResource;
import net.sumaris.core.model.administration.programStrategy.AcquisitionLevel;
import net.sumaris.core.model.administration.programStrategy.AcquisitionLevelEnum;
import net.sumaris.core.service.AbstractServiceTest;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.data.SampleVO;
import net.sumaris.core.vo.data.TripVO;
import net.sumaris.core.vo.data.VesselFeaturesVO;
import net.sumaris.core.vo.referential.LocationVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.junit.Assert;
import org.junit.Assume;
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
        TripVO vo = createTrip();
        TripVO savedVO = service.save(vo, false);

        Assert.assertNotNull(savedVO);
        Assert.assertNotNull(savedVO.getId());
    }

    @Test
    public void delete() {
        service.delete(dbResource.getFixtures().getTripId(0));
    }

    @Test
    public void deleteAfterCreate() {
        TripVO savedVO = null;
        try {
            savedVO = service.save(createTrip(), false);
            Assume.assumeNotNull(savedVO);
            Assume.assumeNotNull(savedVO.getId());
        }
        catch(Exception e) {
            Assume.assumeNoException(e);
        }

        service.delete(savedVO.getId());
    }

    /* -- Protected -- */

    protected TripVO createTrip() {
        TripVO vo = new TripVO();
        vo.setProgram(dbResource.getFixtures().getDefaultProgram());
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

        return vo;
    }
}

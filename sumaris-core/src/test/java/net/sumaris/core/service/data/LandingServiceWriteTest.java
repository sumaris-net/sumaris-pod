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

import com.google.common.collect.ImmutableSet;
import net.sumaris.core.dao.DatabaseResource;
import net.sumaris.core.service.AbstractServiceTest;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.data.LandingVO;
import net.sumaris.core.vo.data.VesselSnapshotVO;
import net.sumaris.core.vo.referential.location.LocationVO;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;

public class LandingServiceWriteTest extends AbstractServiceTest{

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.writeDb();

    @Autowired
    private LandingService service;

    @Test
    public void save() {
        LandingVO vo = createLanding();
        LandingVO savedVO = service.save(vo);

        Assert.assertNotNull(savedVO);
        Assert.assertNotNull(savedVO.getId());

        // Reload
        LandingVO reloadedVO = service.get(savedVO.getId());
        Assert.assertNotNull(reloadedVO);

        // Check observers
        Assert.assertNotNull(reloadedVO.getObservers());
        Assert.assertEquals(2, reloadedVO.getObservers().size());
    }

    @Test
    public void saveWithImages() {
        LandingVO vo = createLanding();

        // Add samples + images
        // TODO

        LandingVO savedVO = service.save(vo);

        Assert.assertNotNull(savedVO);
        Assert.assertNotNull(savedVO.getId());

        // Reload
        LandingVO reloadedVO = service.get(savedVO.getId());
        Assert.assertNotNull(reloadedVO);

        // Check images
        // TODO
        //Assert.assertNotNull(reloadedVO.getObservers());
        //Assert.assertEquals(2, reloadedVO.getObservers().size());
    }

    @Test
    public void delete() {
        service.delete(fixtures.getLandingId(0));
    }

    @Test
    public void deleteAfterCreate() {
        LandingVO savedVO = null;
        try {
            savedVO = service.save(createLanding());
            Assume.assumeNotNull(savedVO);
            Assume.assumeNotNull(savedVO.getId());
        }
        catch(Exception e) {
            Assume.assumeNoException(e);
        }

        service.delete(savedVO.getId());
    }

    /* -- Protected -- */

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
        vessel.setVesselId(fixtures.getVesselId(0));
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

        // Measurement
        // e.g. for SFA - Port State ?
        //Map<Integer, String> measurementValues = Maps.newHashMap();
        //measurementValues.put(PmfmEnum.CONTROL_TYPE.getId(), "220");
        //vo.setMeasurementValues(measurementValues);


        return vo;
    }
}

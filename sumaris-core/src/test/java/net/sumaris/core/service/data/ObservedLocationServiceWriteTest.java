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
import com.google.common.collect.Maps;
import net.sumaris.core.dao.DatabaseResource;
import net.sumaris.core.model.referential.pmfm.PmfmEnum;
import net.sumaris.core.service.AbstractServiceTest;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.data.ObservedLocationVO;
import net.sumaris.core.vo.data.VesselFeaturesVO;
import net.sumaris.core.vo.referential.LocationVO;
import org.apache.commons.collections4.MapUtils;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.Map;

public class ObservedLocationServiceWriteTest extends AbstractServiceTest{

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.writeDb();

    @Autowired
    private ObservedLocationService service;

    @Test
    public void save() {
        ObservedLocationVO vo = createObservedLocation();
        ObservedLocationVO savedVO = service.save(vo, false);

        Assert.assertNotNull(savedVO);
        Assert.assertNotNull(savedVO.getId());

        // Reload
        ObservedLocationVO reloadedVO = service.get(savedVO.getId());
        Assert.assertNotNull(reloadedVO);

        // Check observers
        Assert.assertNotNull(reloadedVO.getObservers());
        Assert.assertTrue(reloadedVO.getObservers().size() == 2);
    }

    @Test
    public void delete() {
        service.delete(dbResource.getFixtures().getObservedLocationId(0));
    }

    @Test
    public void deleteAfterCreate() {
        ObservedLocationVO savedVO = null;
        try {
            savedVO = service.save(createObservedLocation(), false);
            Assume.assumeNotNull(savedVO);
            Assume.assumeNotNull(savedVO.getId());
        }
        catch(Exception e) {
            Assume.assumeNoException(e);
        }

        service.delete(savedVO.getId());
    }

    /* -- Protected -- */

    protected ObservedLocationVO createObservedLocation() {
        ObservedLocationVO vo = new ObservedLocationVO();
        vo.setProgram(dbResource.getFixtures().getDefaultProgram());
        vo.setStartDateTime(new Date());

        LocationVO departureLocation = new LocationVO();
        departureLocation.setId(dbResource.getFixtures().getLocationPortId(0));
        vo.setLocation(departureLocation);

        vo.setCreationDate(new Date());

        DepartmentVO recorderDepartment = new DepartmentVO();
        recorderDepartment.setId(dbResource.getFixtures().getDepartmentId(0));
        vo.setRecorderDepartment(recorderDepartment);

        // Observers
        PersonVO observer1 = new PersonVO();
        observer1.setId(dbResource.getFixtures().getPersonId(0));
        PersonVO observer2 = new PersonVO();
        observer2.setId(dbResource.getFixtures().getPersonId(1));
        vo.setObservers(ImmutableSet.of(observer1, observer2));

        // Measurement
        Map<Integer, String> measurementValues = Maps.newHashMap();
        measurementValues.put(PmfmEnum.CONTROL_TYPE.getId(), "220"); // Type de vente = Avant-vente
        vo.setMeasurementValues(measurementValues);

        return vo;
    }
}

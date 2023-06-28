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
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.model.administration.programStrategy.ProgramPropertyEnum;
import net.sumaris.core.model.referential.pmfm.PmfmEnum;
import net.sumaris.core.service.AbstractServiceTest;
import net.sumaris.core.service.administration.programStrategy.ProgramService;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.data.*;
import net.sumaris.core.vo.filter.LandingFilterVO;
import net.sumaris.core.vo.filter.ObservedLocationFilterVO;
import net.sumaris.core.vo.referential.LocationVO;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class ObservedLocationServiceWriteTest extends AbstractServiceTest{

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.writeDb();

    @Autowired
    private ObservedLocationService service;

    @Autowired
    private ProgramService programService;

    @Autowired
    private LandingService landingService;

    @Test
    public void save() {
        ObservedLocationVO vo = createObservedLocation();
        ObservedLocationVO savedVO = service.save(vo, ObservedLocationSaveOptions.builder().build());

        Assert.assertNotNull(savedVO);
        Assert.assertNotNull(savedVO.getId());

        // Reload
        ObservedLocationVO reloadedVO = service.get(savedVO.getId());
        Assert.assertNotNull(reloadedVO);

        // Check observers
        Assert.assertNotNull(reloadedVO.getObservers());
        Assert.assertEquals(2, reloadedVO.getObservers().size());
    }

    @Test
    public void delete() {
        service.delete(fixtures.getObservedLocationId(0));
    }

    @Test
    public void deleteAfterCreate() {
        ObservedLocationVO savedVO = null;
        try {
            savedVO = service.save(createObservedLocation(), ObservedLocationSaveOptions.builder().build());
            Assume.assumeNotNull(savedVO);
            Assume.assumeNotNull(savedVO.getId());
        }
        catch(Exception e) {
            Assume.assumeNoException(e);
        }

        service.delete(savedVO.getId());
    }
    @Test
    public void validate() {
        ObservedLocationVO observedLocation = service.get(11);
        Assume.assumeNotNull(observedLocation);
        if (observedLocation.getControlDate() == null) {
            observedLocation = service.control(observedLocation);
            controlLandingsByObservedLocationId(observedLocation.getId());
        }

        Assume.assumeNotNull(observedLocation.getControlDate());
        Assume.assumeTrue(observedLocation.getValidationDate() == null);
        ObservedLocationVO result = service.validate(observedLocation);
        Assume.assumeNotNull(result.getValidationDate());

        // Sub landings must be controlled before validation
        {
            List<LandingVO> landings = landingService.findAll(LandingFilterVO.builder()
                            .observedLocationId(observedLocation.getId())
                            .build(),
                    Page.builder().offset(0).size(1000).build(),
                    LandingFetchOptions.MINIMAL);
            Assert.assertTrue(landings.size() > 0);
            landings.forEach(l -> {
                Assert.assertNotNull(l.getValidationDate());
            });
        }
    }

    @Test
    public void validateMeta() {
        ObservedLocationVO observedLocation = service.get(14/*SIH-OBSDEB-META*/);
        Assume.assumeNotNull(observedLocation);
        if (observedLocation.getControlDate() == null) {
            observedLocation = service.control(observedLocation);
        }

        Assume.assumeNotNull(observedLocation.getControlDate());
        Assume.assumeTrue(observedLocation.getValidationDate() == null);

        // Get the programLabel for children
        String subProgramLabel = programService.getPropertyValueByProgramLabel(observedLocation.getProgram().getLabel(),
                ProgramPropertyEnum.OBSERVED_LOCATION_AGGREGATED_LANDINGS_PROGRAM);
        Assume.assumeNotNull(subProgramLabel);

        ObservedLocationFilterVO childrenFilter = ObservedLocationFilterVO.builder()
                .programLabel(subProgramLabel)
                .startDate(observedLocation.getStartDateTime())
                .endDate(observedLocation.getEndDateTime())
                .build();

        // children observed location must be controlled before validation
        service.findAll(childrenFilter, Page.builder().build(), DataFetchOptions.MINIMAL)
                .forEach(ol -> {
                    if (ol.getControlDate() == null) service.control(ol);
                    controlLandingsByObservedLocationId(ol.getId());
                });

        // Validate the meta observed location
        ObservedLocationVO result = service.validate(observedLocation);
        Assert.assertNotNull(result.getValidationDate());

        // All subObservedLocation also must be validated
        service.findAll(childrenFilter, Page.builder().build(), DataFetchOptions.MINIMAL)
                .forEach(ol -> Assert.assertNotNull(ol.getValidationDate()));
    }


    /* -- Protected -- */

    protected ObservedLocationVO createObservedLocation() {
        ObservedLocationVO vo = new ObservedLocationVO();
        vo.setProgram(fixtures.getDefaultProgram());
        vo.setStartDateTime(new Date());

        LocationVO departureLocation = new LocationVO();
        departureLocation.setId(fixtures.getLocationPortId(0));
        vo.setLocation(departureLocation);

        vo.setCreationDate(new Date());

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
        Map<Integer, String> measurementValues = Maps.newHashMap();
        measurementValues.put(PmfmEnum.CONTROL_TYPE.getId(), "220"); // Type de vente = Avant-vente
        vo.setMeasurementValues(measurementValues);

        return vo;
    }

    protected void controlLandingsByObservedLocationId(int observedLocationId) {
        landingService.findAll(LandingFilterVO.builder()
                                .observedLocationId(observedLocationId)
                                .build(),
                        Page.builder().offset(0).size(1000).build(),
                        LandingFetchOptions.MINIMAL)
                .forEach(l -> {
                    if (l.getControlDate() == null) landingService.control(l);
                });
    }
}

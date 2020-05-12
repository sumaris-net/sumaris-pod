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

public class ObservedLocationServiceReadTest extends AbstractServiceTest{

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.readDb();

    @Autowired
    private ObservedLocationService service;

    @Test
    public void findByFilter() {
        ObservedLocationFilterVO filter = ObservedLocationFilterVO.builder()
                .programLabel("ADAP-CONTROLE")
                .build();
        List<ObservedLocationVO> vos = service.findByFilter(filter, 0, 100);
        Assert.assertNotNull(vos);
        Assert.assertTrue(vos.size() > 0);
    }

    @Test
    // FIXME: TODO implementer le filtrage par recorderDepartment
    public void findByFilterWithRecorderDepartment() {
        int recorderDepId = dbResource.getFixtures().getDepartmentId(0);
        ObservedLocationFilterVO filter = ObservedLocationFilterVO.builder()
                .recorderDepartmentId(recorderDepId)
                .build();
        List<ObservedLocationVO> vos = service.findByFilter(filter, 0, 100);
        Assert.assertNotNull(vos);
        Assert.assertTrue(vos.size() > 0);
        vos.stream()
                .map(ObservedLocationVO::getRecorderDepartment)
                .map(DepartmentVO::getId)
                .forEach(depId -> Assert.assertTrue(depId != null && depId.intValue() == recorderDepId));
    }

    @Test
    // FIXME: TODO implementer le filtrage par recorderPerson
    public void findByFilterWithRecorderPerson() {
        int recorderPersonId = -99;
        ObservedLocationFilterVO filter = ObservedLocationFilterVO.builder()
                .recorderPersonId(recorderPersonId)
                .build();
        List<ObservedLocationVO> vos = service.findByFilter(filter, 0, 100);
        Assert.assertNotNull(vos);
        vos.stream()
                .map(ObservedLocationVO::getRecorderPerson)
                .map(PersonVO::getId)
                .forEach(personId -> Assert.assertTrue(personId != null && personId.intValue() == recorderPersonId));
    }
}

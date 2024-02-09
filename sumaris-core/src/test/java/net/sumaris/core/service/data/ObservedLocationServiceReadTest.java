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

import lombok.NonNull;
import net.sumaris.core.dao.DatabaseResource;
import net.sumaris.core.model.data.DataQualityStatusEnum;
import net.sumaris.core.service.AbstractServiceTest;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.data.ObservedLocationVO;
import net.sumaris.core.vo.filter.ObservedLocationFilterVO;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

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
        List<ObservedLocationVO> vos = service.findAll(filter, 0, 100);
        Assert.assertNotNull(vos);
        Assert.assertEquals(1, vos.size());
    }

    @Test
    public void findByFilterWithRecorderDepartment() {
        int recorderDepId = fixtures.getDepartmentId(2);
        ObservedLocationFilterVO filter = ObservedLocationFilterVO.builder()
                .recorderDepartmentId(recorderDepId)
                .build();
        List<ObservedLocationVO> vos = service.findAll(filter, 0, 100);
        Assert.assertNotNull(vos);
        Assert.assertEquals(3, vos.size());
        vos.stream()
                .map(ObservedLocationVO::getRecorderDepartment)
                .map(DepartmentVO::getId)
                .forEach(depId -> Assert.assertTrue(depId != null && depId == recorderDepId));
    }

    @Test
    public void findByFilterWithRecorderPerson() {
        int recorderPersonId = fixtures.getPersonId(1);
        ObservedLocationFilterVO filter = ObservedLocationFilterVO.builder()
                .recorderPersonId(recorderPersonId)
                .build();
        List<ObservedLocationVO> vos = service.findAll(filter, 0, 100);
        Assert.assertNotNull(vos);
        Assert.assertEquals(5, vos.size());
        vos.stream()
                .map(ObservedLocationVO::getRecorderPerson)
                .map(PersonVO::getId)
                .forEach(personId -> Assert.assertTrue(personId != null && personId == recorderPersonId));
    }

    @Test
    public void findByFilterWithDataQuality() {

        assertFindAll(ObservedLocationFilterVO.builder()
            .dataQualityStatus(new DataQualityStatusEnum[]{DataQualityStatusEnum.MODIFIED})
            .build(),
            4);

        assertFindAll(ObservedLocationFilterVO.builder()
                .dataQualityStatus(new DataQualityStatusEnum[]{DataQualityStatusEnum.CONTROLLED})
                .build(),
            1);

        assertFindAll(ObservedLocationFilterVO.builder()
                .dataQualityStatus(new DataQualityStatusEnum[]{DataQualityStatusEnum.VALIDATED})
                .build(),
            0);

        assertFindAll(ObservedLocationFilterVO.builder()
                .dataQualityStatus(new DataQualityStatusEnum[]{DataQualityStatusEnum.QUALIFIED})
                .build(),
            0);
    }

    @Test
    public void findByFilterWithVesselIds() {

        assertFindAll(ObservedLocationFilterVO.builder()
                .vesselIds(new Integer[]{fixtures.getVesselId(0)})
                .build(),
            4);

        assertFindAll(ObservedLocationFilterVO.builder()
                .vesselIds(new Integer[]{fixtures.getVesselId(1)})
                .build(),
            1);
    }

    /* -- protected -- */

    protected List<ObservedLocationVO> assertFindAll(@NonNull ObservedLocationFilterVO filter, int expectedSize) {
        List<ObservedLocationVO> vos = service.findAll(filter, 0, 100);
        Assert.assertNotNull(vos);
        Assert.assertEquals(expectedSize, vos.size());
        return vos;
    }
}

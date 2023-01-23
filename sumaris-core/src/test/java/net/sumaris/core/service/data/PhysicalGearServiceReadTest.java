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
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.model.data.DataQualityStatusEnum;
import net.sumaris.core.service.AbstractServiceTest;
import net.sumaris.core.util.Dates;
import net.sumaris.core.vo.data.PhysicalGearVO;
import net.sumaris.core.vo.filter.PhysicalGearFilterVO;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;
import java.util.Date;
import java.util.List;

public class PhysicalGearServiceReadTest extends AbstractServiceTest{

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.readDb();

    @Autowired
    private PhysicalGearService service;

    @Test
    public void findPhysicalGears() throws ParseException {

        Date physicalGearDay = Dates.parseDate("2018-03-03", "yyyy-MM-dd");
        assertFindAll(PhysicalGearFilterVO.builder()
            .startDate(Dates.resetTime(physicalGearDay))
            .endDate(Dates.lastSecondOfTheDay(physicalGearDay))
            .excludeChildGear(true)
            .build(),
            2);

        assertFindAll(PhysicalGearFilterVO.builder()
            .startDate(Dates.parseDate("2018-01-01", "yyyy-MM-dd"))
            .endDate(Dates.parseDate("2018-03-30", "yyyy-MM-dd"))
            .excludeChildGear(true)
            .build(),
            2);

        assertFindAll(PhysicalGearFilterVO.builder()
            .startDate(Dates.parseDate("2018-01-01", "yyyy-MM-dd"))
            .endDate(Dates.parseDate("2018-05-30", "yyyy-MM-dd"))
            .excludeChildGear(true)
            .build(),
            3);

        assertFindAll(PhysicalGearFilterVO.builder()
            .startDate(Dates.parseDate("2018-02-28", "yyyy-MM-dd"))
            .endDate(Dates.parseDate("2018-04-18", "yyyy-MM-dd"))
            .excludeChildGear(true)
            .build(),
            3);

        assertFindAll(PhysicalGearFilterVO.builder()
            .startDate(Dates.parseDate("2018-02-28", "yyyy-MM-dd"))
            .excludeChildGear(true)
            .build(),
            4);

        assertFindAll(PhysicalGearFilterVO.builder()
            .startDate(Dates.parseDate("2018-03-03", "yyyy-MM-dd"))
            .excludeChildGear(true)
            .build(),
            3);

        assertFindAll(PhysicalGearFilterVO.builder()
            .startDate(Dates.parseDate("2018-03-04", "yyyy-MM-dd"))
            .excludeChildGear(true)
            .build(),
            3);

        assertFindAll(PhysicalGearFilterVO.builder()
            .endDate(Dates.parseDate("2018-03-04", "yyyy-MM-dd"))
            .excludeChildGear(true)
            .build(),
            2);

        assertFindAll(PhysicalGearFilterVO.builder()
            .endDate(Dates.parseDate("2018-04-20", "yyyy-MM-dd"))
            .excludeChildGear(true)
            .build(),
            3);

        assertFindAll(PhysicalGearFilterVO.builder()
            .recorderDepartmentId(1)
            .excludeChildGear(true)
            .build(),
            4);

        assertFindAll(PhysicalGearFilterVO.builder()
            .recorderPersonId(2)
            .excludeChildGear(true)
            .build(),
            0);

        // Find sub gears
        assertFindAll(PhysicalGearFilterVO.builder()
            .vesselId(fixtures.getVesselId(0))
            .programLabel(fixtures.getWithSubGearsProgram().getLabel())
            .excludeParentGear(true)
            .build(),
            3); // All sub gears in APASE data

        assertFindAll(PhysicalGearFilterVO.builder()
                //.vesselId(fixtures.getVesselId(0))
                //.programLabel(fixtures.getWithSubGearsProgram().getLabel())
                .parentGearId(70 /* =id of the root gear  */)
                .build(),
            3); // All sub gears in APASE data, the one trip
    }

    @Test
    public void findPhysicalGearsByQualityStatus() {

        assertFindAll(PhysicalGearFilterVO.builder()
                .dataQualityStatus(new DataQualityStatusEnum[]{DataQualityStatusEnum.MODIFIED})
                .build(),
            9);
    }

    private void assertFindAll(PhysicalGearFilterVO filter, int expectedSize) {
        List<PhysicalGearVO> physicalGears = service.findAll(filter, Page.builder().offset(0).size(100).build(), null);
        Assert.assertNotNull(physicalGears);
        Assert.assertEquals(expectedSize, physicalGears.size());
    }

}

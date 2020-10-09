package net.sumaris.core.service.administration;

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
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.model.administration.programStrategy.Program;
import net.sumaris.core.service.AbstractServiceTest;
import net.sumaris.core.service.administration.programStrategy.ProgramService;
import net.sumaris.core.vo.administration.programStrategy.ProgramVO;
import net.sumaris.core.vo.filter.ProgramFilterVO;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class ProgramServiceReadTest extends AbstractServiceTest{

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.readDb();

    @Autowired
    private ProgramService service;

    @Test
    public void getById() {
        ProgramVO program = service.get(1);
        Assert.assertNotNull(program);
        Assert.assertNotNull(program.getId());
        Assert.assertEquals(1, program.getId().intValue());

        Assert.assertNotNull(program.getProperties());
        Assert.assertEquals(6, program.getProperties().size());

        Assert.assertNotNull(program.getGearClassificationId());
        Assert.assertNotNull(program.getTaxonGroupTypeId());

        Assert.assertNull(program.getLocations());
        Assert.assertNull(program.getLocationIds());
        Assert.assertNull(program.getLocationClassifications());
        Assert.assertNull(program.getLocationClassificationIds());
    }

    @Test
    public void getByLabel() {
        ProgramVO program = service.getByLabel("ADAP-MER");
        Assert.assertNotNull(program);
        Assert.assertNotNull(program.getId());
        Assert.assertEquals(10, program.getId().intValue());

        Assert.assertNotNull(program.getProperties());
        Assert.assertEquals(10, program.getProperties().size());

        Assert.assertNotNull(program.getGearClassificationId());
        Assert.assertNotNull(program.getTaxonGroupTypeId());

        Assert.assertNull(program.getLocations());
        Assert.assertNull(program.getLocationIds());
        Assert.assertNull(program.getLocationClassifications());
        Assert.assertNull(program.getLocationClassificationIds());
    }

    @Test
    public void findByFilter() {

        List<ProgramVO> programs = service.findByFilter(ProgramFilterVO.builder().searchText("ADAP").build(), 0,10, Program.Fields.LABEL, SortDirection.ASC);
        Assert.assertNotNull(programs);
        Assert.assertEquals(2, programs.size());
        Assert.assertEquals("ADAP-CONTROLE", programs.get(0).getLabel());
        Assert.assertEquals("ADAP-MER", programs.get(1).getLabel());

        programs = service.findByFilter(ProgramFilterVO.builder().withProperty("sumaris.trip.operation.batch.autoFill").build(), 0,10, Program.Fields.LABEL, SortDirection.ASC);
        Assert.assertNotNull(programs);
        Assert.assertEquals(1, programs.size());
        Assert.assertEquals(10, programs.get(0).getId().intValue());

    }

    @Test
    public void getAll() {

        List<ProgramVO> programs = service.getAll();
        Assert.assertNotNull(programs);
        Assert.assertEquals(8, programs.size());

    }

}

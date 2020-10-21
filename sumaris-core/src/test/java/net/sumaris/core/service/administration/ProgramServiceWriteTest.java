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
import net.sumaris.core.model.referential.StatusEnum;
import net.sumaris.core.service.AbstractServiceTest;
import net.sumaris.core.service.administration.programStrategy.ProgramService;
import net.sumaris.core.vo.administration.programStrategy.ProgramVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.junit.*;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ProgramServiceWriteTest extends AbstractServiceTest{

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.writeDb();

    @Autowired
    private ProgramService service;


    @Test
    public void saveExisting() {
        ProgramVO program = service.getByLabel("ADAP-CONTROLE");
        Assert.assertNotNull(program);
        Assert.assertNotNull(program.getId());
        Assert.assertEquals(11, program.getId().intValue());
        Assert.assertNotNull(program.getProperties());
        Assert.assertEquals(3, program.getProperties().size());
        Assert.assertNull(program.getStrategies()); // no strategy

        // Modify name
        program.setName("Program Name changed");
        // Add a property
        program.getProperties().put("PROPERTY_TEST", "PROPERTY_VALUE");

        service.save(program, null);

        // reload by id
        program = service.get(11);
        Assert.assertNotNull(program);
        Assert.assertEquals("Program Name changed", program.getName());
        Assert.assertNotNull(program.getProperties());
        Assert.assertEquals(4, program.getProperties().size());
        Assert.assertEquals("PROPERTY_VALUE", program.getProperties().get("PROPERTY_TEST"));

        Assert.assertNull(program.getStrategies()); // no strategy
    }

    @Test
    @Ignore("TODO: save/update program with strategies and pmfm strategies")
    public void saveWithStrategies() {
        // TODO
    }

    @Test
    public void saveNew() {

        ProgramVO program = new ProgramVO();
        program.setLabel("PROG-TEST");
        program.setName("label test");
        program.setStatusId(StatusEnum.TEMPORARY.getId());
        ReferentialVO gearClassification = new ReferentialVO();
        gearClassification.setId(1);
        program.setGearClassification(gearClassification);
        ReferentialVO taxonGroupType = new ReferentialVO();
        taxonGroupType.setId(2);
        program.setTaxonGroupType(taxonGroupType);

        service.save(program, null);
    }

    @Test
    public void z_delete() {

        try {
            service.delete(11);
            Assert.fail("should throw exception");
        } catch (Exception e) {
            Assert.assertNotNull(e);
            // TODO this service delete should delete also children entities...
        }

    }

}

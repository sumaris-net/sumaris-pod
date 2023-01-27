package net.sumaris.core.service.referential;

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
import net.sumaris.core.model.referential.location.Location;
import net.sumaris.core.model.referential.location.LocationLevelEnum;
import net.sumaris.core.model.referential.taxon.TaxonGroup;
import net.sumaris.core.model.referential.taxon.TaxonName;
import net.sumaris.core.service.AbstractServiceTest;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ReferentialServiceWriteTest extends AbstractServiceTest{

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.writeDb();

    @Autowired
    private ReferentialService service;

    @Test
    public void saveExisting() {
        ReferentialVO ref = service.get(TaxonGroup.class, 1);
        Assert.assertNotNull(ref);
        Assert.assertEquals("ANA", ref.getLabel());
        Assert.assertEquals("Anadromous species (e.g. salmon, shads, sea trout...)", ref.getName());
        Assert.assertNull(ref.getComments());

        ref.setLabel("ANA-TEST");
        ref.setName("name test");
        ref.setComments("comments test");

        service.save(ref);

        // reload by label
        ref = service.findByUniqueLabel(TaxonGroup.class.getSimpleName(), "ANA-TEST");
        Assert.assertNotNull(ref);
        Assert.assertNotNull(ref.getId());
        Assert.assertEquals(1, ref.getId().intValue());
        Assert.assertEquals("ANA-TEST", ref.getLabel());
        Assert.assertEquals("name test", ref.getName());
        Assert.assertEquals("comments test", ref.getComments());
    }

    @Test
    public void saveNewAnDelete() {
        ReferentialVO ref = new ReferentialVO();
        ref.setEntityName(Location.class.getSimpleName());
        ref.setLabel("TEST-LABEL");
        ref.setName("name test");
        ref.setLevelId(LocationLevelEnum.HARBOUR.getId());
        ref.setStatusId(StatusEnum.ENABLE.getId());

        service.save(ref);

        Assert.assertNotNull(ref);
        Assert.assertNotNull(ref.getId());
        Assert.assertNotNull(ref.getUpdateDate());

        service.delete(Location.class.getSimpleName(), ref.getId());
    }

    @Test
    public void z_delete() {
        // this taxon name is not used, so it can be deleted
        service.delete(TaxonName.class.getSimpleName(), 1082); // PIL

        try {
            service.delete(TaxonGroup.class.getSimpleName(), 1003);
            Assert.fail("should throw exception, this taxon group is used");
        } catch (Exception e) {
            Assert.assertNotNull(e);
        }

    }

}

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
import net.sumaris.core.service.AbstractServiceTest;
import net.sumaris.core.service.referential.taxon.TaxonNameService;
import net.sumaris.core.vo.referential.TaxonNameFetchOptions;
import net.sumaris.core.vo.referential.TaxonNameVO;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TaxonNameServiceWriteTest extends AbstractServiceTest {

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.writeDb();

    @Autowired
    private TaxonNameService service;

    @Test
    public void saveExisting() {
        TaxonNameVO taxonName = service.getByLabel("SRX", TaxonNameFetchOptions.FULL);
        Assert.assertNotNull(taxonName);
        Assert.assertNotNull(taxonName.getId());
        Assert.assertEquals(1001, taxonName.getId().intValue());
        Assert.assertEquals(1001, taxonName.getReferenceTaxonId().intValue());
        Assert.assertNotNull(taxonName.getTaxonomicLevel());
        Assert.assertEquals(13, taxonName.getTaxonomicLevel().getId().intValue());

        // Modify name
        taxonName.setName("Taxon Name changed");
        service.save(taxonName);

        // reload by id
        taxonName = service.get(1001, TaxonNameFetchOptions.FULL);
        Assert.assertNotNull(taxonName);
        Assert.assertEquals("Taxon Name changed", taxonName.getName());

        Assert.assertNotNull(taxonName.getReferenceTaxonId());
        Assert.assertEquals(1001, taxonName.getReferenceTaxonId().intValue());
        Assert.assertNotNull(taxonName.getTaxonomicLevel());
        Assert.assertEquals(13, taxonName.getTaxonomicLevel().getId().intValue());

    }

    @Test
    public void saveNewWithReferenceTaxon() {
        TaxonNameVO taxonName = new TaxonNameVO();
        taxonName.setLabel("TEST");
        taxonName.setName("Test avec ref taxon existante");
        taxonName.setStatusId(StatusEnum.TEMPORARY.getId());
        taxonName.setReferenceTaxonId(1001);
        taxonName.setParentId(1042);
        taxonName.setTaxonomicLevelId(28);
        taxonName.setIsNaming(false);
        taxonName.setIsReferent(true);
        taxonName.setIsVirtual(false);
        taxonName.setStartDate(new Date());
        taxonName.setEntityName("TaxonName");

        service.save(taxonName);
    }

    @Test
    public void saveNewWithoutReferenceTaxon() {
        TaxonNameVO taxonName = new TaxonNameVO();
        taxonName.setLabel("TEST 2");
        taxonName.setName("Test sans ref taxon existant");
        taxonName.setStatusId(StatusEnum.TEMPORARY.getId());
        taxonName.setParentId(1042);
        taxonName.setTaxonomicLevelId(28);
        taxonName.setIsNaming(false);
        taxonName.setIsReferent(true);
        taxonName.setIsVirtual(false);
        taxonName.setStartDate(new Date());
        taxonName.setEntityName("TaxonName");

        service.save(taxonName);
    }
}

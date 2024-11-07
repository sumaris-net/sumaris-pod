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

import graphql.Assert;
import net.sumaris.core.dao.DatabaseFixtures;
import net.sumaris.core.dao.DatabaseResource;
import net.sumaris.core.service.AbstractServiceTest;
import net.sumaris.core.service.referential.taxon.TaxonNameService;
import net.sumaris.core.vo.referential.taxon.TaxonNameVO;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class TaxonNameServiceReadTest extends AbstractServiceTest{

    @Autowired
    private TaxonNameService service;

    @Autowired
    protected DatabaseFixtures fixtures;

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.readDb();

    @Test
    public void getAllByTaxonGroupId() {
        List<TaxonNameVO> result = service.findAllByTaxonGroupId(fixtures.getTaxonGroupIdWithManyTaxonName());
        Assert.assertNotNull(result);
        Assert.assertTrue(result.size() > 0);

        result.forEach(tn -> {
            Assert.assertNotNull(tn.getLabel());
            Assert.assertNotNull(tn.getName());

            // important (nNeed by the App)
            Assert.assertNotNull(tn.getReferenceTaxonId());
        });

    }
}

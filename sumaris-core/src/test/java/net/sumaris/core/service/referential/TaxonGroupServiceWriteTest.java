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
import net.sumaris.core.service.AbstractServiceTest;
import net.sumaris.core.service.referential.taxon.TaxonGroupService;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class TaxonGroupServiceWriteTest extends AbstractServiceTest{

    @Autowired
    private TaxonGroupService service;

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.writeDb();

    @Test
    public void updateTaxonGroupHierarchy() {
        service.updateTaxonGroupHierarchies();
    }
}

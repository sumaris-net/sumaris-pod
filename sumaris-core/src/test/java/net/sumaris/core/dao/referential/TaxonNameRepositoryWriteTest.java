package net.sumaris.core.dao.referential;

/*-
 * #%L
 * SUMARiS:: Core
 * %%
 * Copyright (C) 2018 - 2020 SUMARiS Consortium
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

import net.sumaris.core.dao.AbstractDaoTest;
import net.sumaris.core.dao.DatabaseResource;
import net.sumaris.core.dao.referential.taxon.TaxonNameRepository;
import net.sumaris.core.vo.referential.taxon.TaxonNameVO;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;

/**
 * @author peck7 on 19/08/2020.
 */
public class TaxonNameRepositoryWriteTest extends AbstractDaoTest {

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.writeDb();

    @Autowired
    private TaxonNameRepository taxonNameRepository;

    @Test
    public void saveTaxonName() {

        TaxonNameVO vo = new TaxonNameVO();
        vo.setLabel("TEST");
        vo.setName("Taxon name Test");
        vo.setStatusId(1);
        vo.setReferenceTaxonId(1001);
        vo.setParentId(1042);
        vo.setTaxonomicLevelId(28);
        vo.setIsNaming(false);
        vo.setIsReferent(true);
        vo.setIsVirtual(false);
        vo.setStartDate(new Date());
        vo.setEntityName("TaxonName");

        taxonNameRepository.save(vo);

    }


}

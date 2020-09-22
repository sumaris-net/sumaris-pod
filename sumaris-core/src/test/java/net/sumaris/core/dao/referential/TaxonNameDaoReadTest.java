package net.sumaris.core.dao.referential;

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

import net.sumaris.core.dao.AbstractDaoTest;
import net.sumaris.core.dao.DatabaseResource;
import net.sumaris.core.dao.referential.taxon.TaxonGroupRepository;
import net.sumaris.core.dao.referential.taxon.TaxonNameDao;
import net.sumaris.core.model.referential.taxon.TaxonGroup;
import net.sumaris.core.model.referential.taxon.TaxonGroupTypeEnum;
import net.sumaris.core.vo.referential.TaxonNameVO;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TaxonNameDaoReadTest extends AbstractDaoTest{

    /** Logger. */
    private static final Logger log =
            LoggerFactory.getLogger(TaxonNameDaoReadTest.class);

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.readDb();

    @Autowired
    protected TaxonGroupRepository taxonGroupRepository;

    @Autowired
    private TaxonNameDao dao;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        setCommitOnTearDown(false); // this is need because of delete test

        // Make sure to init taxon group hierarchies
        taxonGroupRepository.updateTaxonGroupHierarchies();
    }

    @Test
    public void getAllByTaxonGroupId() {
        // RAJ - Rajidae
        {
            TaxonGroup tg = taxonGroupRepository.getOneByLabelAndTaxonGroupTypeId("RAJ", TaxonGroupTypeEnum.FAO.getId());
            Assume.assumeNotNull(tg);
            List<TaxonNameVO> taxonNames = dao.getAllByTaxonGroupId(tg.getId());

            Assert.assertNotNull(taxonNames);
            Assert.assertTrue(taxonNames.size() > 0);
        }

        // SKA - Raja spp
        {
            TaxonGroup tg = taxonGroupRepository.getOneByLabelAndTaxonGroupTypeId("SKA", TaxonGroupTypeEnum.FAO.getId());
            Assume.assumeNotNull(tg);
            List<TaxonNameVO> taxonNames = dao.getAllByTaxonGroupId(tg.getId());

            Assert.assertNotNull(taxonNames);
            Assert.assertTrue(taxonNames.size() > 0);
        }

        // MNZ - Baudroie nca (=Lophius spp)
        {
            TaxonGroup tg = taxonGroupRepository.getOneByLabelAndTaxonGroupTypeId("MNZ", TaxonGroupTypeEnum.FAO.getId());
            Assume.assumeNotNull(tg);
            List<TaxonNameVO> taxonNames = dao.getAllByTaxonGroupId(tg.getId());

            Assert.assertNotNull(taxonNames);
            Assert.assertTrue(taxonNames.size() > 0);
        }

    }


}

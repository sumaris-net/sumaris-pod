/*
 * #%L
 * SUMARiS
 * %%
 * Copyright (C) 2019 SUMARiS Consortium
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

package net.sumaris.core.dao.referential;

import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.AbstractDaoTest;
import net.sumaris.core.dao.DatabaseResource;
import net.sumaris.core.dao.referential.taxon.TaxonGroupRepository;
import net.sumaris.core.model.referential.location.Location;
import net.sumaris.core.model.referential.taxon.TaxonGroup;
import net.sumaris.core.model.referential.taxon.TaxonGroupTypeEnum;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.List;

@Slf4j
public class TaxonGroupRepositoryWriteTest extends AbstractDaoTest {

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.writeDb();

    @Autowired
    protected TaxonGroupRepository taxonGroupRepository;

    @Autowired
    protected ReferentialDao referentialDao;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        setCommitOnTearDown(false); // this is need because of delete test

        // Make sure to init taxon group hierarchies
        taxonGroupRepository.updateTaxonGroupHierarchies();
    }

    @Test
    public void getAllDressingByTaxonGroupId() {

        TaxonGroup taxonGroup = taxonGroupRepository.getByLabelAndTaxonGroupTypeId("MNZ", TaxonGroupTypeEnum.FAO.getId());
        ReferentialVO countryLocation =  referentialDao.findByUniqueLabel(Location.class.getSimpleName(), "FRA").get();

        // MNZ - Baudroie
        {
            List<ReferentialVO> dressings = taxonGroupRepository.getAllDressingByTaxonGroupId(taxonGroup.getId(),
                new Date(), null,
                countryLocation.getId());

            Assert.assertNotNull(dressings);
            Assert.assertTrue(dressings.size() > 0);
        }

    }

    @Test
    public void getAllPreservingByTaxonGroupId() {

        TaxonGroup taxonGroup = taxonGroupRepository.getByLabelAndTaxonGroupTypeId("MNZ", TaxonGroupTypeEnum.FAO.getId());
        ReferentialVO countryLocation =  referentialDao.findByUniqueLabel(Location.class.getSimpleName(), "FRA").get();

        // MNZ - Baudroie
        {
            List<ReferentialVO> dressings = taxonGroupRepository.getAllPreservingByTaxonGroupId(taxonGroup.getId(),
                    new Date(), null,
                    countryLocation.getId());

            Assert.assertNotNull(dressings);
            Assert.assertTrue(dressings.size() > 0);
        }

    }


}

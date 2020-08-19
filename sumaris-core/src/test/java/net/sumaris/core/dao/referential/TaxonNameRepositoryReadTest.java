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

import com.google.common.collect.ImmutableList;
import net.sumaris.core.dao.AbstractDaoTest;
import net.sumaris.core.dao.DatabaseResource;
import net.sumaris.core.dao.referential.taxon.TaxonGroupRepository;
import net.sumaris.core.dao.referential.taxon.TaxonNameRepository;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.model.referential.taxon.TaxonGroup;
import net.sumaris.core.model.referential.taxon.TaxonGroupTypeId;
import net.sumaris.core.model.referential.taxon.TaxonName;
import net.sumaris.core.model.referential.taxon.TaxonomicLevelId;
import net.sumaris.core.vo.filter.TaxonNameFilterVO;
import net.sumaris.core.vo.referential.TaxonNameVO;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class TaxonNameRepositoryReadTest extends AbstractDaoTest {

    /**
     * Logger.
     */
    private static final Logger log =
        LoggerFactory.getLogger(TaxonNameRepositoryReadTest.class);

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.writeDb();

    @Autowired
    protected TaxonGroupRepository taxonGroupRepository;

    @Autowired
    private TaxonNameRepository taxonNameRepository;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        setCommitOnTearDown(false); // this is need because of delete test

        // Make sure to init taxon group hierarchies
        taxonGroupRepository.updateTaxonGroupHierarchies();
    }

    @Test
    public void getAllSpeciesAndSubSpecies() {
        List<TaxonNameVO> taxonNames = taxonNameRepository.getAll(false);
        Assert.assertNotNull(taxonNames);
        Assert.assertEquals(23, taxonNames.size());
        taxonNames = taxonNameRepository.getAll(false);
        Assert.assertNotNull(taxonNames);
        Assert.assertEquals(23, taxonNames.size());
    }

    @Test
    public void getTaxonNameReferent() {
        TaxonNameVO referent = taxonNameRepository.getTaxonNameReferent(1030);
        Assert.assertNotNull(referent);
        Assert.assertEquals(Integer.valueOf(1030), referent.getId());
        referent = taxonNameRepository.getTaxonNameReferent(1023);
        Assert.assertNotNull(referent);
        Assert.assertEquals(Integer.valueOf(1023), referent.getId());
        referent = taxonNameRepository.getTaxonNameReferent(9999);
        Assert.assertNull(referent);
    }

    @Test
    public void getAllTaxonNameByParentIds() {
        List<TaxonName> taxonNames = taxonNameRepository.getAllTaxonNameByParentTaxonNameIdInAndIsReferentTrue(ImmutableList.of(1004));
        Assert.assertNotNull(taxonNames);
        Assert.assertEquals(15, taxonNames.size());
        taxonNames = taxonNameRepository.getAllTaxonNameByParentTaxonNameIdInAndIsReferentTrue(ImmutableList.of(1030,1031,1032));
        Assert.assertNotNull(taxonNames);
        Assert.assertEquals(4, taxonNames.size());
        taxonNames = taxonNameRepository.getAllTaxonNameByParentTaxonNameIdInAndIsReferentTrue(ImmutableList.of(1014));
        Assert.assertNotNull(taxonNames);
        Assert.assertEquals(2, taxonNames.size());
    }

    @Test
    public void getAllByTaxonGroupId() {
        // RAJ - Rajidae
        assertAllByTaxonGroupLabel("RAJ", 17);

        // SKA - Raja spp
        assertAllByTaxonGroupLabel("SKA", 17);

        // MNZ - Baudroie nca (=Lophius spp)
        assertAllByTaxonGroupLabel("MNZ", 2);

    }

    private void assertAllByTaxonGroupLabel(String taxonGroupLabel, int expectedSize) {
        TaxonGroup tg = taxonGroupRepository.getOneByLabelAndTaxonGroupTypeId(taxonGroupLabel, TaxonGroupTypeId.FAO.getId());
        Assume.assumeNotNull(tg);
        List<TaxonNameVO> taxonNames = taxonNameRepository.getAllByTaxonGroupId(tg.getId());

        Assert.assertNotNull(taxonNames);
        Assert.assertEquals(expectedSize, taxonNames.size());
    }

    @Test
    public void findByFilter() {

        // no filter
        assertFilterResult(TaxonNameFilterVO.taxonNameBuilder().build(), 37);
        // with synonyms
        assertFilterResult(TaxonNameFilterVO.taxonNameBuilder().withSynonyms(true).build(), 38);
        // with status 0
        assertFilterResult(TaxonNameFilterVO.taxonNameBuilder().statusIds(new Integer[]{0}).build(), 12);
        // with parent taxon group 1014
        assertFilterResult(TaxonNameFilterVO.taxonNameBuilder().taxonGroupId(1014).build(), 3);
        // with parent taxon group 1014 with synonyms
        assertFilterResult(TaxonNameFilterVO.taxonNameBuilder().taxonGroupId(1014).withSynonyms(true).build(), 4);
        // with parent taxon group 1014 and status 1
        assertFilterResult(TaxonNameFilterVO.taxonNameBuilder().taxonGroupId(1014).statusIds(new Integer[]{1}).build(), 3);
        // with parent taxon group 1014 and status 1 with synonyms
        assertFilterResult(TaxonNameFilterVO.taxonNameBuilder().taxonGroupId(1014).statusIds(new Integer[]{1}).withSynonyms(true).build(), 3);
        // with parent taxon group 1160 1161
        assertFilterResult(TaxonNameFilterVO.taxonNameBuilder().taxonGroupIds(new Integer[]{1160, 1161}).build(), 2);
        // with taxonomic level (species and subspecies)
        assertFilterResult(TaxonNameFilterVO.taxonNameBuilder()
            .levelIds(new Integer[]{TaxonomicLevelId.SPECIES.getId(), TaxonomicLevelId.SUBSPECIES.getId()}).build(), 23);
        // with label search
        assertFilterResult(TaxonNameFilterVO.taxonNameBuilder().searchText("raja").build(), 13);
        // with exact label
        assertFilterResult(TaxonNameFilterVO.taxonNameBuilder().label("STT").build(), 1);

    }

    private void assertFilterResult(TaxonNameFilterVO filter, int expectedSize) {
        List<TaxonNameVO> tn = taxonNameRepository.findByFilter(filter, 0, 100, "id", SortDirection.ASC);
        Assert.assertNotNull(tn);
        Assert.assertEquals(expectedSize, tn.size());
    }
}

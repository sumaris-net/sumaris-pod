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
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.AbstractDaoTest;
import net.sumaris.core.dao.DatabaseResource;
import net.sumaris.core.dao.referential.taxon.TaxonGroupRepository;
import net.sumaris.core.dao.referential.taxon.TaxonNameRepository;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.model.referential.StatusEnum;
import net.sumaris.core.model.referential.taxon.TaxonGroup;
import net.sumaris.core.model.referential.taxon.TaxonGroupTypeEnum;
import net.sumaris.core.model.referential.taxon.TaxonName;
import net.sumaris.core.model.referential.taxon.TaxonomicLevelEnum;
import net.sumaris.core.vo.filter.TaxonNameFilterVO;
import net.sumaris.core.vo.referential.TaxonNameFetchOptions;
import net.sumaris.core.vo.referential.TaxonNameVO;
import org.junit.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Slf4j
public class TaxonNameRepositoryReadTest extends AbstractDaoTest {

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
    public void findAll() {
        // Test without synonym (default)
        List<TaxonNameVO> taxonNames = taxonNameRepository.findAll(TaxonNameFilterVO.builder().build(),
            Page.builder().size(100).build(), null);
        Assert.assertNotNull(taxonNames);
        Assert.assertEquals(43, taxonNames.size());

        // Test with synonym
        taxonNames = taxonNameRepository.findAll(TaxonNameFilterVO.builder()
                .withSynonyms(true)
            .build(), (Page)null, null);
        Assert.assertNotNull(taxonNames);
        Assert.assertEquals(44, taxonNames.size());
    }

    @Test
    public void findTaxonNameReferent() {
        TaxonNameVO referent = taxonNameRepository.findReferentByReferenceTaxonId(1030).orElse(null);
        Assert.assertNotNull(referent);
        Assert.assertEquals(Integer.valueOf(1030), referent.getId());
        referent = taxonNameRepository.findReferentByReferenceTaxonId(1023).orElse(null);
        Assert.assertNotNull(referent);
        Assert.assertEquals(Integer.valueOf(1023), referent.getId());
        referent = taxonNameRepository.findReferentByReferenceTaxonId(9999).orElse(null);
        Assert.assertNull(referent);
    }

    @Test
    public void getAllTaxonNameByParentIds() {
        List<TaxonName> taxonNames = taxonNameRepository.getAllTaxonNameByParentIdInAndIsReferentTrue(ImmutableList.of(1004));
        Assert.assertNotNull(taxonNames);
        Assert.assertEquals(15, taxonNames.size());
        taxonNames = taxonNameRepository.getAllTaxonNameByParentIdInAndIsReferentTrue(ImmutableList.of(1030, 1031, 1032));
        Assert.assertNotNull(taxonNames);
        Assert.assertEquals(4, taxonNames.size());
        taxonNames = taxonNameRepository.getAllTaxonNameByParentIdInAndIsReferentTrue(ImmutableList.of(1014));
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
        TaxonGroup tg = taxonGroupRepository.getByLabelAndTaxonGroupTypeId(taxonGroupLabel, TaxonGroupTypeEnum.FAO.getId());
        Assume.assumeNotNull(tg);
        List<TaxonNameVO> taxonNames = taxonNameRepository.getAllByTaxonGroupId(tg.getId());

        Assert.assertNotNull(taxonNames);
        Assert.assertEquals(expectedSize, taxonNames.size());
    }

    @Test
    public void findByFilter() {

        // no filter
        assertFilterResult(TaxonNameFilterVO.builder().build(), 43);
        // with synonyms
        assertFilterResult(TaxonNameFilterVO.builder().withSynonyms(true).build(), 44);
        // with status 0
        assertFilterResult(TaxonNameFilterVO.builder().statusIds(new Integer[]{StatusEnum.DISABLE.getId()}).build(), 17);
        // with parent taxon group 1014
        assertFilterResult(TaxonNameFilterVO.builder().taxonGroupId(1014).build(), 3);
        // with parent taxon group 1014 with synonyms
        assertFilterResult(TaxonNameFilterVO.builder().taxonGroupId(1014).withSynonyms(true).build(), 4);
        // with parent taxon group 1014 and status 1
        assertFilterResult(TaxonNameFilterVO.builder().taxonGroupId(1014).statusIds(new Integer[]{1}).build(), 3);
        // with parent taxon group 1014 and status 1 with synonyms
        assertFilterResult(TaxonNameFilterVO.builder().taxonGroupId(1014).statusIds(new Integer[]{1}).withSynonyms(true).build(), 3);
        // with parent taxon group 1160 1161
        assertFilterResult(TaxonNameFilterVO.builder().taxonGroupIds(new Integer[]{1160, 1161}).build(), 3);
        // with taxonomic level (species and subspecies)
        assertFilterResult(TaxonNameFilterVO.builder()
                .levelIds(new Integer[]{TaxonomicLevelEnum.SPECIES.getId(), TaxonomicLevelEnum.SUBSPECIES.getId()}).build(), 27);
        // with label search
        assertFilterResult(TaxonNameFilterVO.builder().searchText("raja").build(), 13);
        // with exact label
        assertFilterResult(TaxonNameFilterVO.builder().label("STT").build(), 1);

    }

    private void assertFilterResult(TaxonNameFilterVO filter, int expectedSize) {
        List<TaxonNameVO> tn = taxonNameRepository.findAll(filter, Page.builder()
                .size(100).sortBy("id").sortDirection(SortDirection.ASC)
            .build(), null);
        Assert.assertNotNull(tn);
        Assert.assertEquals(expectedSize, tn.size());
    }

    @Test
    public void getById() {
        TaxonNameVO tn = taxonNameRepository.get(1001, TaxonNameFetchOptions.DEFAULT);
        Assert.assertNotNull(tn);
        Assert.assertEquals(1001, tn.getId().intValue());
        Assert.assertEquals("SRX", tn.getLabel());
        Assert.assertEquals(1001, tn.getReferenceTaxonId().intValue());
        Assert.assertEquals(13, tn.getTaxonomicLevelId().intValue());
        Assert.assertNull(tn.getTaxonomicLevel()); // not fetch by default
        Assert.assertNull(tn.getParentTaxonName());

        tn = taxonNameRepository.get(1002, TaxonNameFetchOptions.FULL);
        Assert.assertNotNull(tn);
        Assert.assertNotNull(tn.getParentTaxonName());
        Assert.assertEquals(1001, tn.getParentTaxonName().getId().intValue());
        Assert.assertEquals(1001, tn.getParentId().intValue());
        Assert.assertNotNull(tn.getTaxonomicLevel()); // fetch in full fetch
        Assert.assertEquals(tn.getTaxonomicLevelId(), tn.getTaxonomicLevel().getId());
        Assert.assertNotNull(tn.getParentTaxonName()); // fetch in full fetch
    }

    @Test
    public void getByLabel() {
        TaxonNameVO tn = taxonNameRepository.getByLabel("SRX", TaxonNameFetchOptions.DEFAULT);
        Assert.assertNotNull(tn);
        Assert.assertEquals(1001, tn.getId().intValue());
        Assert.assertEquals("SRX", tn.getLabel());
        Assert.assertEquals(1001, tn.getReferenceTaxonId().intValue());
        Assert.assertEquals(13, tn.getTaxonomicLevelId().intValue());
        Assert.assertNull(tn.getTaxonomicLevel());
        Assert.assertNull(tn.getParentTaxonName());

        tn = taxonNameRepository.getByLabel("STT", TaxonNameFetchOptions.FULL);
        Assert.assertNotNull(tn);
        Assert.assertNotNull(tn.getParentTaxonName());
        Assert.assertEquals(1001, tn.getParentTaxonName().getId().intValue());
        Assert.assertEquals(1001, tn.getParentId().intValue());
        Assert.assertNotNull(tn.getTaxonomicLevel());
        Assert.assertNotNull(tn.getParentTaxonName());
    }
}

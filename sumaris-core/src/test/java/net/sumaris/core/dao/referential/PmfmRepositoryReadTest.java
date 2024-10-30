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
import net.sumaris.core.dao.referential.pmfm.PmfmRepository;
import net.sumaris.core.vo.referential.pmfm.PmfmFetchOptions;
import net.sumaris.core.vo.referential.pmfm.PmfmVO;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

/**
 * @author peck7 on 19/08/2020.
 */
public class PmfmRepositoryReadTest extends AbstractDaoTest {

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.readDb();

    @Autowired
    private PmfmRepository pmfmRepository;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        setCommitOnTearDown(false);
    }

    @Test
    public void findByLabel() {

        Optional<PmfmVO> pmfm = pmfmRepository.findByLabel("ZZZZZ");
        Assert.assertFalse(pmfm.isPresent());

        pmfm = pmfmRepository.findByLabel("GEAR_SPEED");
        Assert.assertTrue(pmfm.isPresent());
        Assert.assertEquals(9, pmfm.get().getId().intValue());

    }

    @Test
    public void getByLabel() {

        PmfmVO pmfm = pmfmRepository.getByLabel("MAIN_METIER");
        Assert.assertNotNull(pmfm);
        Assert.assertEquals(25, pmfm.getId().intValue());

        try {
            pmfmRepository.getByLabel("____");
            Assert.fail("should throw exception");
        } catch (Exception e) {
            Assert.assertNotNull(e);
        }
    }

    @Test
    public void get() {

        // Normal find with qualitative values
        PmfmVO pmfm = pmfmRepository.get(5);
        Assert.assertNotNull(pmfm);
        Assert.assertNotNull(pmfm.getQualitativeValues());
        Assert.assertEquals(6, pmfm.getQualitativeValues().size());

        // Without qualitative values
        pmfm = pmfmRepository.get(5, PmfmFetchOptions.builder().withInheritance(false).build());
        Assert.assertNotNull(pmfm);
        Assert.assertNull(pmfm.getQualitativeValues());

    }

    @Test
    public void checkPrefixSuffix() {

        Assert.assertTrue(pmfmRepository.hasLabelPrefix(50, "LANDING"));
        Assert.assertTrue(pmfmRepository.hasLabelPrefix(50, "OR", "LANDING"));
        Assert.assertFalse(pmfmRepository.hasLabelPrefix(50, "OR"));

        Assert.assertFalse(pmfmRepository.hasLabelSuffix(50, "LANDING", "XX"));
        Assert.assertTrue(pmfmRepository.hasLabelSuffix(50, "WEIGHT", "XX"));
        Assert.assertTrue(pmfmRepository.hasLabelSuffix(50, "WEIGHT"));

    }

}

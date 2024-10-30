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
import net.sumaris.core.vo.referential.pmfm.PmfmVO;
import net.sumaris.core.vo.referential.pmfm.PmfmValueType;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author peck7 on 19/08/2020.
 */
public class PmfmRepositoryWriteTest extends AbstractDaoTest {

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.writeDb();

    @Autowired
    private PmfmRepository pmfmRepository;

    @Test
    public void savePmfm() {

        PmfmVO pmfm = new PmfmVO();
        pmfm.setLabel("PMFM_TEST");
        pmfm.setParameterId(5); // is qualitative
        pmfm.setMatrixId(3);
        pmfm.setFractionId(1);
        pmfm.setMethodId(0);
        pmfm.setUnitId(0);
        pmfm.setStatusId(1);

        pmfm = pmfmRepository.save(pmfm);

        Assert.assertNotNull(pmfm);
        Assert.assertNotNull(pmfm.getId());
        Assert.assertNotNull(pmfm.getParameterId());
        Assert.assertEquals(5, pmfm.getParameterId().intValue());
        Assert.assertNotNull(pmfm.getLevelId());
        Assert.assertEquals(5, pmfm.getLevelId().intValue());
        Assert.assertNotNull(pmfm.getMatrixId());
        Assert.assertEquals(3, pmfm.getMatrixId().intValue());
        Assert.assertNotNull(pmfm.getFractionId());
        Assert.assertEquals(1, pmfm.getFractionId().intValue());
        Assert.assertNotNull(pmfm.getMethodId());
        Assert.assertEquals(0, pmfm.getMethodId().intValue());
        Assert.assertFalse(pmfm.getIsCalculated());
        Assert.assertFalse(pmfm.getIsEstimated());
        Assert.assertNotNull(pmfm.getUnitId());
        Assert.assertEquals(0, pmfm.getUnitId().intValue());
        Assert.assertNull(pmfm.getUnitLabel());
        Assert.assertNotNull(pmfm.getType());
        Assert.assertEquals(PmfmValueType.QUALITATIVE_VALUE.name(), pmfm.getType().toUpperCase());
        Assert.assertNull(pmfm.getQualitativeValues()); // qv list not loaded from parameter after a save

        // change parameter, method and unit
        pmfm.setParameterId(6); // is not qualitative
        pmfm.setMethodId(3); // estimated
        pmfm.setUnitId(3); // Kg

        pmfm = pmfmRepository.save(pmfm);

        Assert.assertNotNull(pmfm);
        Assert.assertNotNull(pmfm.getId());
        Assert.assertNotNull(pmfm.getParameterId());
        Assert.assertEquals(6, pmfm.getParameterId().intValue());
        Assert.assertNotNull(pmfm.getLevelId());
        Assert.assertEquals(6, pmfm.getLevelId().intValue());
        Assert.assertNotNull(pmfm.getMatrixId());
        Assert.assertEquals(3, pmfm.getMatrixId().intValue());
        Assert.assertNotNull(pmfm.getFractionId());
        Assert.assertEquals(1, pmfm.getFractionId().intValue());
        Assert.assertNotNull(pmfm.getMethodId());
        Assert.assertEquals(3, pmfm.getMethodId().intValue());
        Assert.assertFalse(pmfm.getIsCalculated());
        Assert.assertTrue(pmfm.getIsEstimated());
        Assert.assertNotNull(pmfm.getUnitId());
        Assert.assertEquals(3, pmfm.getUnitId().intValue());
        Assert.assertNotNull(pmfm.getUnitLabel());
        Assert.assertEquals("kg", pmfm.getUnitLabel());
        Assert.assertNotNull(pmfm.getType());
        Assert.assertEquals(PmfmValueType.DOUBLE.name(), pmfm.getType().toUpperCase());
        Assert.assertNull(pmfm.getQualitativeValues());

    }
}

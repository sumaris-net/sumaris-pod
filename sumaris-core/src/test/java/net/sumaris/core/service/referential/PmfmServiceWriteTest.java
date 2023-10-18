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
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.service.AbstractServiceTest;
import net.sumaris.core.service.referential.pmfm.PmfmService;
import net.sumaris.core.vo.referential.PmfmVO;
import net.sumaris.core.vo.referential.PmfmValueType;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.assertj.core.util.Lists;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class PmfmServiceWriteTest extends AbstractServiceTest{

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.writeDb();

    @Autowired
    private PmfmService service;


    @Test
    public void save() {

        PmfmVO pmfm = new PmfmVO();
        pmfm.setLabel("PMFM_TEST-" + System.currentTimeMillis());
        pmfm.setParameterId(5); // is qualitative
        pmfm.setMatrixId(3);
        pmfm.setFractionId(1);
        pmfm.setMethodId(0);
        pmfm.setUnitId(0);
        pmfm.setStatusId(1);

        pmfm = service.save(pmfm);
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

        // Add correct qv
        ReferentialVO qv1 = new ReferentialVO();
        qv1.setId(30);
        ReferentialVO qv2 = new ReferentialVO();
        qv2.setId(33);
        pmfm.setQualitativeValues(Lists.newArrayList(qv1, qv2));
        pmfm = service.save(pmfm);
        Assert.assertNotNull(pmfm.getQualitativeValues());
        Assert.assertEquals(2, pmfm.getQualitativeValues().size());

        // Remove a qv
        pmfm.getQualitativeValues().remove(qv2);
        pmfm = service.save(pmfm);
        Assert.assertNotNull(pmfm.getQualitativeValues());
        Assert.assertEquals(1, pmfm.getQualitativeValues().size());

        // Add incorrect qv
        ReferentialVO qv3 = new ReferentialVO();
        qv3.setId(45);
        pmfm.getQualitativeValues().add(qv3);
        try {
            service.save(pmfm);
            Assert.fail("should throw exception");
        } catch (Exception e) {
            Assert.assertTrue(e instanceof SumarisTechnicalException);
        }

        // change parameter, method and unit
        pmfm.setParameterId(6); // is not qualitative
        pmfm.setMethodId(3); // estimated
        pmfm.setUnitId(3); // Kg

        pmfm = service.save(pmfm);

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
        Assert.assertNull(pmfm.getQualitativeValues()); // removed by service
    }

}

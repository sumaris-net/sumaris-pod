package net.sumaris.core.service.data;

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
import net.sumaris.core.dao.technical.model.TreeNodeEntities;
import net.sumaris.core.service.AbstractServiceTest;
import net.sumaris.core.vo.data.BatchVO;
import net.sumaris.core.vo.data.DenormalizedBatchVO;
import org.apache.commons.lang3.mutable.MutableInt;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import sun.java2d.xr.MutableInteger;

import java.util.List;

public class DenormalizedBatchServiceReadTest extends AbstractServiceTest{

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.readDb();

    @Autowired
    private DenormalizedBatchService service;

    @Test
    public void denormalize() {

        BatchVO catchBatch = DataTestUtils.createAdapBatchTree(dbResource.getFixtures());
        Assume.assumeNotNull(catchBatch);
        List<BatchVO> batches = TreeNodeEntities.treeAsList(catchBatch);
        Assume.assumeNotNull(batches);

        List<DenormalizedBatchVO> result = service.denormalize(catchBatch);
        Assert.assertNotNull(result);
        Assert.assertEquals(batches.size(), result.size());

        MutableInt counter = new MutableInt(0);
        result.forEach(b -> {
            boolean isCatchBatch = counter.getValue() == 0;
            if (isCatchBatch) {
                Assert.assertNotNull(b.getChildren());
                Assert.assertTrue(b.getChildren().size() > 0);
            }
            else {
                Assert.assertNotNull(b.getParent());
            }
            counter.increment();
        });
        // Check species batches
        result.stream().filter(b -> b.getTaxonGroup() != null)
            .forEach(speciesBatch -> {
                Assert.assertNotNull("Species batch must have indirect weight", speciesBatch.getIndirectContextWeight());
                Assert.assertNotNull("Species batch must have indirect individual count", speciesBatch.getIndirectIndividualCount());
            });
    }
    /* -- -- */
}

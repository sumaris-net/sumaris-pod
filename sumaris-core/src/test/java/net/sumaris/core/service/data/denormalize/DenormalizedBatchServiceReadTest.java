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

package net.sumaris.core.service.data.denormalize;

import net.sumaris.core.dao.DatabaseResource;
import net.sumaris.core.model.TreeNodeEntities;
import net.sumaris.core.model.administration.programStrategy.AcquisitionLevelEnum;
import net.sumaris.core.service.AbstractServiceTest;
import net.sumaris.core.service.data.DataTestUtils;
import net.sumaris.core.vo.data.batch.BatchVO;
import net.sumaris.core.vo.data.batch.DenormalizedBatchOptions;
import net.sumaris.core.vo.data.batch.DenormalizedBatchVO;
import org.apache.commons.lang3.mutable.MutableInt;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class DenormalizedBatchServiceReadTest extends AbstractServiceTest{

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.readDb();

    @Autowired
    private DenormalizedBatchService service;

    @Test
    public void denormalize_ADAP() {

        BatchVO catchBatch = DataTestUtils.createAdapBatchTree(fixtures);
        Assume.assumeNotNull(catchBatch);
        List<BatchVO> batches = TreeNodeEntities.treeAsList(catchBatch);
        Assume.assumeNotNull(batches);

        List<DenormalizedBatchVO> result = service.denormalize(catchBatch, DenormalizedBatchOptions.DEFAULT);
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
        result.stream()
            .filter(b -> b.getTaxonGroup() != null)
            .forEach(speciesBatch -> {
                // Check has weight
                assertHasWeight(speciesBatch, "Species batch");
                // Check individual count
                assertHasIndividualCount(speciesBatch, "Species batch");
            });
    }

    /* -- protected functions -- */

    protected void assertHasWeight(DenormalizedBatchVO batch, String batchType) {
        Assert.assertNotNull(String.format("'%s' must have elevate weight (%s)", batch.getLabel(), batchType),batch.getElevateWeight());
        Assert.assertTrue(String.format("'%s' must have weight or indirect weight (%s)", batch.getLabel(), batchType),
            batch.getIndirectWeight() != null || batch.getWeight() != null);
    }

    protected void assertHasIndividualCount(DenormalizedBatchVO batch, String batchType) {
        // If has individual measure sub-batches, should have an indirect individual count
        boolean hasIndividualMeasure = TreeNodeEntities.treeAsList(batch).stream()
            .anyMatch(child -> child.getLabel().startsWith(AcquisitionLevelEnum.SORTING_BATCH_INDIVIDUAL.getLabel()));
        if (hasIndividualMeasure) {
            Assert.assertNotNull(String.format("'%s' must have indirect individual count (%s)", batch.getLabel(), batchType),
                batch.getIndirectIndividualCount());
        }
    }
}

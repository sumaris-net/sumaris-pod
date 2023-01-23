package net.sumaris.core.service.data.denormalize;

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

import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.DatabaseFixtures;
import net.sumaris.core.dao.DatabaseResource;
import net.sumaris.core.model.TreeNodeEntities;
import net.sumaris.core.service.AbstractServiceTest;
import net.sumaris.core.service.data.BatchService;
import net.sumaris.core.service.data.DenormalizedBatchService;
import net.sumaris.core.vo.data.batch.BatchVO;
import net.sumaris.core.vo.data.batch.DenormalizedBatchVO;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Slf4j
public class DenormalizeBatchServiceWriteTest extends AbstractServiceTest{

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.writeDb();

    @Autowired
    private BatchService batchService;

    @Autowired
    private DenormalizedBatchService service;

    @Autowired
    protected DatabaseFixtures fixtures;

    @Test
    public void denormalizeAndSaveByOperationId() {

        int operationId = fixtures.getOperationIdWithBatches();

        List<BatchVO> batches = batchService.getAllByOperationId(operationId);
        BatchVO catchBatch = TreeNodeEntities.listAsTree(batches, BatchVO::getParentId, false);
        Assume.assumeNotNull(catchBatch);

        List<DenormalizedBatchVO> result = service.denormalizeAndSaveByOperationId(operationId, null);

        Assert.assertNotNull(result);
        Assert.assertEquals(batches.size(), result.size());
    }

}

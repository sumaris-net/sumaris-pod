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

package net.sumaris.core.service.data.denormalize.oracle;

import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.DatabaseFixtures;
import net.sumaris.core.dao.DatabaseResource;
import net.sumaris.core.model.TreeNodeEntities;
import net.sumaris.core.model.administration.programStrategy.AcquisitionLevelEnum;
import net.sumaris.core.service.AbstractServiceTest;
import net.sumaris.core.service.data.BatchService;
import net.sumaris.core.service.data.denormalize.DenormalizedBatchService;
import net.sumaris.core.vo.data.batch.BatchVO;
import net.sumaris.core.vo.data.batch.DenormalizedBatchVO;
import net.sumaris.core.vo.data.batch.DenormalizedBatches;
import net.sumaris.core.vo.data.batch.TempDenormalizedBatchVO;
import org.junit.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Objects;

@Slf4j
@Ignore("Use only SFA Oracle database")
@ActiveProfiles("oracle")
@TestPropertySource(locations = "classpath:application-test-oracle-sfa.properties")
public class DenormalizeBatchWriteOracleSFATest extends AbstractServiceTest {
    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.writeDb("oracle-sfa");
    @Autowired
    private BatchService batchService;

    @Autowired
    private DenormalizedBatchService service;

    @Autowired
    protected DatabaseFixtures fixtures;

    @Test
    public void denormalizeOperation_issue521() {

        int operationId = 400668;

        List<BatchVO> batches = batchService.getAllByOperationId(operationId);
        BatchVO catchBatch = TreeNodeEntities.listAsTree(batches, BatchVO::getParentId, false);
        Assume.assumeNotNull(catchBatch);

        Integer expectedIndividualCountSum = batches.stream()
            .filter(b -> b.getLabel().startsWith(AcquisitionLevelEnum.SORTING_BATCH_INDIVIDUAL.getLabel()))
            .map(BatchVO::getIndividualCount)
            .filter(Objects::nonNull)
            .reduce(0, Integer::sum);
        Assume.assumeTrue(expectedIndividualCountSum == 15);


        List<DenormalizedBatchVO> result = service.denormalizeAndSaveByOperationId(operationId, null);

        Assert.assertNotNull(result);
        Assert.assertEquals(batches.size(), result.size());
        Integer actualIndividualCountSum = result.stream()
            .filter(b -> b.getLabel().startsWith(AcquisitionLevelEnum.SORTING_BATCH_INDIVIDUAL.getLabel()))
            .map(DenormalizedBatchVO::getIndividualCount)
            .filter(Objects::nonNull)
            .reduce(0, Integer::sum);
        Assert.assertEquals(expectedIndividualCountSum, actualIndividualCountSum);
    }
}

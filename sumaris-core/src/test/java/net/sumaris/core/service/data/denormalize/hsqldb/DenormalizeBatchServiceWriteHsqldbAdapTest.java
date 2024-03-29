package net.sumaris.core.service.data.denormalize.hsqldb;

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
import net.sumaris.core.model.administration.programStrategy.AcquisitionLevelEnum;
import net.sumaris.core.model.referential.pmfm.QualitativeValueEnum;
import net.sumaris.core.service.AbstractServiceTest;
import net.sumaris.core.service.data.BatchService;
import net.sumaris.core.service.data.denormalize.DenormalizedBatchService;
import net.sumaris.core.util.Dates;
import net.sumaris.core.vo.data.batch.BatchVO;
import net.sumaris.core.vo.data.batch.DenormalizedBatchOptions;
import net.sumaris.core.vo.data.batch.DenormalizedBatchVO;
import net.sumaris.core.vo.data.batch.DenormalizedBatches;
import org.junit.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Objects;

@Slf4j
@Ignore("Use only ADAP Hsqldb database")
@TestPropertySource(locations = "classpath:application-test-hsqldb-adap.properties")
public class DenormalizeBatchServiceWriteHsqldbAdapTest extends AbstractServiceTest {

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.writeDb();

    @Autowired
    private BatchService batchService;

    @Autowired
    private DenormalizedBatchService service;

    @Autowired
    protected DatabaseFixtures fixtures;

    /**
     * Test RTP weight computing during the denormalization process
     *
     * See issue sumaris-app#522
     */
    @Test
    public void denormalizeOperation_issue522() {

        int operationId = 209413;
        QualitativeValueEnum.SEX_UNSEXED.setId(9325);

        List<BatchVO> batches = batchService.getAllByOperationId(operationId);
        BatchVO catchBatch = TreeNodeEntities.listAsTree(batches, BatchVO::getParentId, false);
        Assume.assumeNotNull(catchBatch);

        List<BatchVO> lengthBatches = batches.stream()
            .filter(b -> b.getLabel().startsWith(AcquisitionLevelEnum.SORTING_BATCH_INDIVIDUAL.getLabel())
                && b.getTaxonName() != null && "COD".equals(b.getTaxonName().getLabel()))
            .toList();
        Integer referenceTaxonId = lengthBatches.get(0).getTaxonName().getReferenceTaxonId();
        Assume.assumeTrue(referenceTaxonId == 0); // = COD

        Integer expectedIndividualCountSum = lengthBatches.stream()
            .map(BatchVO::getIndividualCount)
            .filter(Objects::nonNull)
            .reduce(0, Integer::sum);
        Assume.assumeTrue(expectedIndividualCountSum == 3);

        DenormalizedBatchOptions denormalizedOptions = service.createOptionsByProgramLabel("ADAP-MER").clone();
        denormalizedOptions.setFishingAreaLocationIds(new Integer[]{ 2766 /* 28E3 */ });
        denormalizedOptions.setDateTime(Dates.safeParseDate("2023-06-18", "YYYY-MM-DD"));
        denormalizedOptions.setForce(true);

        List<DenormalizedBatchVO> denormalizedBatches = service.denormalizeAndSaveByOperationId(operationId, denormalizedOptions);
        Assert.assertNotNull(denormalizedBatches);
        Assert.assertEquals(batches.size(), denormalizedBatches.size());

        List<DenormalizedBatchVO> lengthDenormalizedBatches = denormalizedBatches.stream()
            .filter(b -> b.getLabel().startsWith(AcquisitionLevelEnum.SORTING_BATCH_INDIVIDUAL.getLabel())
                    && b.getTaxonName() != null && "COD".equals(b.getTaxonName().getLabel()))
            .toList();

        DenormalizedBatchVO lengthBatchCod43cm = lengthDenormalizedBatches.get(0);

        // Before the fix (issue sumaris-app#522), indirect RTP weight was = 0.904419, because half precision was NOT added to the length, before using the RTP conversion
        Assert.assertEquals(Double.valueOf(0.934884), lengthBatchCod43cm.getIndirectRtpWeight(), 0.000001);

    }


}

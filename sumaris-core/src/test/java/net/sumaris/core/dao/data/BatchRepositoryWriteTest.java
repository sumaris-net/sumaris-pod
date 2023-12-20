package net.sumaris.core.dao.data;

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
import net.sumaris.core.dao.data.batch.BatchRepository;
import net.sumaris.core.dao.data.operation.OperationRepository;
import net.sumaris.core.model.data.BatchQuantificationMeasurement;
import net.sumaris.core.model.data.BatchSortingMeasurement;
import net.sumaris.core.util.Beans;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.data.MeasurementVO;
import net.sumaris.core.vo.data.batch.BatchFetchOptions;
import net.sumaris.core.vo.data.batch.BatchFilterVO;
import net.sumaris.core.vo.data.batch.BatchVO;
import net.sumaris.core.vo.data.OperationVO;
import net.sumaris.core.vo.data.QuantificationMeasurementVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.apache.commons.collections4.MapUtils;
import org.assertj.core.util.Lists;
import org.junit.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Objects;

@Slf4j
public class BatchRepositoryWriteTest extends AbstractDaoTest {

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.writeDb();

    @Autowired
    private OperationRepository operationRepository;

    @Autowired
    private BatchRepository repository;

    private OperationVO parentOperation;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        setCommitOnTearDown(false); // this is need because of delete test

        parentOperation = operationRepository.get(2); // SUMARiS OPE -> catch haul
        Assume.assumeNotNull(parentOperation);
    }

    @Test
    public void findAllWithMeasurements() {
        List<BatchVO> batches = repository.findAll(BatchFilterVO.builder()
                .operationId(parentOperation.getId()).build(),
                BatchFetchOptions.builder()
                        .withChildrenEntities(false)
                        .withMeasurementValues(true)
                        .build());

        Assert.assertNotNull(batches);
        Assert.assertTrue(batches.size() > 1);

        batches.forEach(Assert::assertNotNull);

        long batchWithMeasurementValuesCount = batches.stream()
                .map(BatchVO::getMeasurementValues)
                .filter(Objects::nonNull)
                .filter(MapUtils::isNotEmpty)
                .count();
        Assert.assertTrue(batchWithMeasurementValuesCount > 0);
    }

    @Test
    public void save() {

        BatchVO batch = new BatchVO();
        batch.setOperationId(parentOperation.getId());
        batch.setComments("Catch batch #1 ope #" + parentOperation.getId());

        batch.setLabel("CATCH_BATCH");
        batch.setExhaustiveInventory(false);
        batch.setRankOrder(1);
        batch.setIndividualCount(1);

        // Taxon group
        ReferentialVO taxonGroup= new ReferentialVO();
        taxonGroup.setId(fixtures.getTaxonGroupFAOId(0));
        batch.setTaxonGroup(taxonGroup);

        // Recorder department
        DepartmentVO recorderDepartment = new DepartmentVO();
        recorderDepartment.setId(fixtures.getDepartmentId(0));
        batch.setRecorderDepartment(recorderDepartment);

        // Measurement: weight
        QuantificationMeasurementVO weightMeasurement = new QuantificationMeasurementVO();
        weightMeasurement.setPmfmId(fixtures.getPmfmBatchWeight()); // landing weight
        weightMeasurement.setEntityName(BatchQuantificationMeasurement.class.getSimpleName());
        weightMeasurement.setIsReferenceQuantification(true);

        batch.setQuantificationMeasurements(ImmutableList.of(weightMeasurement));

        BatchVO savedVO = repository.save(batch);
        Assert.assertNotNull(savedVO);
        Assert.assertNotNull(savedVO.getId());
    }

    @Test
    public void deleteById() {
        Integer id = fixtures.getBatchId(0);
        repository.deleteById(id);
    }

    @Test
    public void saveAllByOperationId() {

        List<BatchVO> batches = Lists.newArrayList();

        {
            BatchVO catchBatch = createCatchBatch();
            batches.add(catchBatch);

            // Child species batch 1
            {
                BatchVO child = createSpeciesBatch(0, 100, 1);

                // Link to parent
                child.setParent(catchBatch);

                // Add to full list
                batches.add(child);
            }
        }

        // Execute saveByOperationId()
        List<BatchVO> savedResult = repository.saveAllByOperationId(parentOperation.getId(), batches);
        Assert.assertNotNull(savedResult);
        Assert.assertEquals(2, savedResult.size());

        savedResult.forEach(batch -> {
            Assert.assertNotNull(batch);
            Assert.assertNotNull(batch.getId());
        });

        BatchVO savedChild = savedResult.get(1);
        Assert.assertNotNull(savedChild);
        Assert.assertNotNull(savedChild.getId());
        Assert.assertNull(savedChild.getParent()); // Parent should have been removed
        Assert.assertNotNull(savedChild.getParentId()); // But parent Id should have been saved



    }

    @Test
    public void saveAllByOperationId_issue498() {

        List<BatchVO> batches = Lists.newArrayList();
        String duplicatedLabel = "SORTING_BATCH_INDIVIDUAL#1";

        BatchVO catchBatch = createCatchBatch();
        batches.add(catchBatch);

        {

            // Child 1
            {
                BatchVO child = createSpeciesBatch(0, 100, 2);

                // Link to parent + Add to full list
                child.setParent(catchBatch);
                batches.add(child);

                // No individual yet (will be insert after the first save)
            }

            // Child 2
            {
                BatchVO child = createSpeciesBatch(1, 50, 1);

                // Link to parent + Add to full list
                child.setParent(catchBatch);
                batches.add(child);

                // Measure 1
                {
                    BatchVO individualBatch = createIndividualBatch(0, fixtures.getPmfmBatchTotalLengthCm(), 60, 1);
                    individualBatch.setLabel(duplicatedLabel); // /!\ Should have same label
                    individualBatch.setParent(child);
                    batches.add(individualBatch);
                }
            }
        }

        // First save, to get an ID on each BATCH
        List<BatchVO> savedResult = repository.saveAllByOperationId(parentOperation.getId(), batches);
        Assert.assertNotNull(savedResult);
        Assert.assertEquals(4, savedResult.size());

        // Now, we add 1 species + 1 measure with an already used label
        {
            // Use saved entities (with ID)
            batches = Lists.newArrayList(savedResult);
            BatchVO child = savedResult.stream()
                .filter(b -> "SORTING_BATCH#1".equals(b.getLabel()))
                .findFirst().orElseGet(() -> {
                    Assert.fail("Missing sorting batch #1");
                    return null;
                });

            // Measure 1 (with the existing label)
            {
                BatchVO individualBatch = createIndividualBatch(0, fixtures.getPmfmBatchTotalLengthCm(), 60, 1);
                individualBatch.setLabel(duplicatedLabel);
                individualBatch.setParent(child);

                // Insert after it parent (BEFORE the saved batch with the same label)
                batches.add(2 , individualBatch);
            }
        }

        // Save it for the second time. In issue 498, the second individual was getting the ID of the first individual,
        // because ID was fund by label
        savedResult = repository.saveAllByOperationId(parentOperation.getId(), batches);
        Assert.assertNotNull(savedResult);
        Assert.assertEquals(5, savedResult.size());

        List<BatchVO> individualBatches = savedResult.stream().filter(b -> duplicatedLabel.equals(b.getLabel())).toList();
        Assert.assertEquals(2, individualBatches.size());
        Assert.assertNotEquals("Batches with same label should not have same ID",
            individualBatches.get(0).getId(), individualBatches.get(1).getId());
    }


    /* -- -- */

    protected BatchVO createCatchBatch() {
        BatchVO catchBatch = new BatchVO();
        catchBatch.setOperationId(parentOperation.getId());
        catchBatch.setComments("Catch batch ope #" + parentOperation.getId());

        catchBatch.setLabel("CATCH_BATCH");
        catchBatch.setExhaustiveInventory(false);
        catchBatch.setRankOrder(1);

        // Recorder department
        DepartmentVO recorderDepartment = new DepartmentVO();
        recorderDepartment.setId(fixtures.getDepartmentId(0));
        catchBatch.setRecorderDepartment(recorderDepartment);

        return catchBatch;
    }

    protected BatchVO createSpeciesBatch(int taxonGroupIndex, double totalWeight, Integer individualCount) {
        int rankOrder = taxonGroupIndex+1;

        BatchVO child = new BatchVO();
        child.setOperationId(parentOperation.getId());
        child.setComments("Sorting batch #1 ope #" + parentOperation.getId());

        child.setLabel("SORTING_BATCH#" + rankOrder);
        child.setExhaustiveInventory(false);
        child.setRankOrder(rankOrder);
        child.setIndividualCount(individualCount);

        // Recorder department
        DepartmentVO recorderDepartment = new DepartmentVO();
        recorderDepartment.setId(fixtures.getDepartmentId(0));
        child.setRecorderDepartment(recorderDepartment);

        // Taxon group
        ReferentialVO taxonGroup = new ReferentialVO();
        taxonGroup.setId(fixtures.getTaxonGroupFAOId(taxonGroupIndex));
        child.setTaxonGroup(taxonGroup);

        // Measurement: weight
        QuantificationMeasurementVO weightMeasurement = new QuantificationMeasurementVO();
        weightMeasurement.setPmfmId(fixtures.getPmfmBatchWeight()); // landing weight
        weightMeasurement.setEntityName(BatchQuantificationMeasurement.class.getSimpleName());
        weightMeasurement.setIsReferenceQuantification(true);
        weightMeasurement.setNumericalValue(totalWeight);

        child.setQuantificationMeasurements(ImmutableList.of(weightMeasurement));

        return child;
    }

    protected BatchVO createIndividualBatch(int index, int pmfmId, double lengthValue, int individualCount) {
        int rankOrder = index+1;
        BatchVO child = new BatchVO();
        child.setOperationId(parentOperation.getId());
        child.setComments("Sorting batch individual #" + rankOrder);

        child.setLabel("SORTING_BATCH_INDIVIDUAL#" + rankOrder);
        child.setExhaustiveInventory(true);
        child.setRankOrder(rankOrder);
        child.setIndividualCount(individualCount);

        // Recorder department
        DepartmentVO recorderDepartment = new DepartmentVO();
        recorderDepartment.setId(fixtures.getDepartmentId(0));
        child.setRecorderDepartment(recorderDepartment);

        // Measurement: length
        MeasurementVO lengthMeasurement = new MeasurementVO();
        lengthMeasurement.setPmfmId(pmfmId); // landing weight
        lengthMeasurement.setEntityName(BatchSortingMeasurement.class.getSimpleName());
        lengthMeasurement.setNumericalValue(lengthValue);

        child.setSortingMeasurements(ImmutableList.of(lengthMeasurement));

        return child;
    }
}

package net.sumaris.core.dao.data;

import com.google.common.collect.ImmutableList;
import net.sumaris.core.dao.AbstractDaoTest;
import net.sumaris.core.dao.DatabaseResource;
import net.sumaris.core.dao.data.batch.BatchDao;
import net.sumaris.core.model.data.batch.BatchQuantificationMeasurement;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.data.BatchVO;
import net.sumaris.core.vo.data.MeasurementVO;
import net.sumaris.core.vo.data.OperationVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.assertj.core.util.Lists;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class BatchDaoWriteTest extends AbstractDaoTest {

    /** Logger. */
    private static final Logger log =
            LoggerFactory.getLogger(BatchDaoWriteTest.class);

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.writeDb();

    @Autowired
    private OperationDao operationDao;

    @Autowired
    private BatchDao dao;

    private OperationVO parentOperation;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        setCommitOnTearDown(false); // this is need because of delete test

        parentOperation = operationDao.get(1);
        Assume.assumeNotNull(parentOperation);
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
        taxonGroup.setId(dbResource.getFixtures().getTaxonGroupFAOId(0));
        batch.setTaxonGroup(taxonGroup);

        // Recorder department
        DepartmentVO recorderDepartment = new DepartmentVO();
        recorderDepartment.setId(dbResource.getFixtures().getDepartmentId(0));
        batch.setRecorderDepartment(recorderDepartment);

        // Measurement: weight
        MeasurementVO weightMeasurement = new MeasurementVO();
        weightMeasurement.setPmfmId(dbResource.getFixtures().getPmfmBatchWeight()); // landing weight
        weightMeasurement.setEntityName(BatchQuantificationMeasurement.class.getSimpleName());

        batch.setQuantificationMeasurements(ImmutableList.of(weightMeasurement));

        BatchVO savedVO = dao.save(batch);
        Assert.assertNotNull(savedVO);
        Assert.assertNotNull(savedVO.getId());
    }

    @Test
    public void delete() {
        Integer id = dbResource.getFixtures().getBatchId(0);
        dao.delete(id);
    }

    @Test
    public void saveByOperationId() {

        List<BatchVO> list = Lists.newArrayList();

        {
            BatchVO batch = new BatchVO();
            batch.setOperationId(parentOperation.getId());
            batch.setComments("Catch batch ope #" + parentOperation.getId());

            batch.setLabel("CATCH_BATCH");
            batch.setExhaustiveInventory(false);
            batch.setRankOrder(1);

            // Recorder department
            DepartmentVO recorderDepartment = new DepartmentVO();
            recorderDepartment.setId(dbResource.getFixtures().getDepartmentId(0));
            batch.setRecorderDepartment(recorderDepartment);

            list.add(batch);

            // Child 1
            {
                BatchVO child = new BatchVO();
                child.setOperationId(parentOperation.getId());
                child.setComments("Sorting batch #1 ope #" + parentOperation.getId());

                child.setLabel("SORTING_BATCH#1");
                child.setExhaustiveInventory(false);
                child.setRankOrder(1);
                child.setIndividualCount(1);

                // Recorder department
                child.setRecorderDepartment(recorderDepartment);

                // Taxon group
                ReferentialVO taxonGroup = new ReferentialVO();
                taxonGroup.setId(dbResource.getFixtures().getTaxonGroupFAOId(0));
                child.setTaxonGroup(taxonGroup);

                // Measurement: weight
                MeasurementVO weightMeasurement = new MeasurementVO();
                weightMeasurement.setPmfmId(dbResource.getFixtures().getPmfmBatchWeight()); // landing weight
                weightMeasurement.setEntityName(BatchQuantificationMeasurement.class.getSimpleName());

                child.setQuantificationMeasurements(ImmutableList.of(weightMeasurement));

                // Set the parent
                child.setParent(batch);

                // Add to full list
                list.add(child);
            }
        }

        // Execute saveByOperationId()
        List<BatchVO> savedResult = dao.saveByOperationId(parentOperation.getId(), list);
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


}

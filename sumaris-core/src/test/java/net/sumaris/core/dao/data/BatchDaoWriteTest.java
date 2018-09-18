package net.sumaris.core.dao.data;

import com.google.common.collect.ImmutableList;
import net.sumaris.core.dao.AbstractDaoTest;
import net.sumaris.core.dao.DatabaseResource;
import net.sumaris.core.dao.data.batch.BatchDao;
import net.sumaris.core.model.data.batch.BatchQuantitifcationMeasurement;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.data.BatchVO;
import net.sumaris.core.vo.data.MeasurementVO;
import net.sumaris.core.vo.data.OperationVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

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
        batch.setComments("Batch #1 ope #" + parentOperation.getId());

        batch.setLabel("B1");
        batch.setExhaustiveInventory(false);
        batch.setRankOrder(1);
        batch.setIndividualCount(1);

        // Taxon group
        ReferentialVO taxonGroup= new ReferentialVO();
        taxonGroup.setId(dbResource.getFixtures().getTaxonGroupFAO(0));
        batch.setTaxonGroup(taxonGroup);

        // Recorder department
        DepartmentVO recorderDepartment = new DepartmentVO();
        recorderDepartment.setId(dbResource.getFixtures().getDepartmentId(0));
        batch.setRecorderDepartment(recorderDepartment);

        // Measurement: weight
        MeasurementVO weightMeasurement = new MeasurementVO();
        weightMeasurement.setPmfmId(dbResource.getFixtures().getPmfmBatchWeight()); // landing weight
        weightMeasurement.setEntityName(BatchQuantitifcationMeasurement.class.getSimpleName());

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


}

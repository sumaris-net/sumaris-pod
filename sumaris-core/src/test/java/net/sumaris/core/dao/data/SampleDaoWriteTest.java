package net.sumaris.core.dao.data;

import net.sumaris.core.dao.AbstractDaoTest;
import net.sumaris.core.dao.DatabaseResource;
import net.sumaris.core.dao.data.sample.SampleDao;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.data.OperationVO;
import net.sumaris.core.vo.data.SampleVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;

public class SampleDaoWriteTest extends AbstractDaoTest {

    /** Logger. */
    private static final Logger log =
            LoggerFactory.getLogger(SampleDaoWriteTest.class);

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.writeDb();

    @Autowired
    private OperationDao operationDao;

    @Autowired
    private SampleDao dao;

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

        SampleVO sample = new SampleVO();
        sample.setOperationId(parentOperation.getId());
        sample.setComments("Sample #1 ope #" + parentOperation.getId());

        sample.setSampleDate(new Date());
        sample.setLabel("S1");
        sample.setIndividualCount(1);

        // Matrix
        ReferentialVO matrix = new ReferentialVO();
        matrix.setId(dbResource.getFixtures().getMatrixIdForIndividual());
        sample.setMatrix(matrix);

        // Taxon group
        ReferentialVO taxonGroup= new ReferentialVO();
        taxonGroup.setId(dbResource.getFixtures().getTaxonGroupFAO(0));
        sample.setTaxonGroup(taxonGroup);

        // Recorder department
        DepartmentVO recorderDepartment = new DepartmentVO();
        recorderDepartment.setId(dbResource.getFixtures().getDepartmentId(0));
        sample.setRecorderDepartment(recorderDepartment);

        SampleVO savedVO = dao.save(sample);
        Assert.assertNotNull(savedVO);
        Assert.assertNotNull(savedVO.getId());
    }

    @Test
    public void delete() {
        Integer id = dbResource.getFixtures().getSampleId(0);
        dao.delete(id);

    }


}

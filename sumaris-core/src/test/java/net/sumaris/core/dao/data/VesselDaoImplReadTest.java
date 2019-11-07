package net.sumaris.core.dao.data;

import net.sumaris.core.dao.AbstractDaoTest;
import net.sumaris.core.dao.DatabaseResource;
import net.sumaris.core.vo.data.VesselFeaturesVO;
import net.sumaris.core.vo.filter.VesselFilterVO;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * @author peck7 on 06/11/2019.
 */
public class VesselDaoImplReadTest extends AbstractDaoTest {

    /** Logger. */
    private static final Logger log =
        LoggerFactory.getLogger(VesselDaoImplReadTest.class);

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.readDb();

    @Autowired
    private VesselDao dao;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        setCommitOnTearDown(false); // this is need because of delete test
    }

    @Test
    public void findByFilter() {

        VesselFilterVO filter = new VesselFilterVO();

        List<VesselFeaturesVO> result = dao.findByFilter(filter, 0, 10, null, null);

        Assert.assertNotNull(result);
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(1, result.get(0).getVesselId().intValue());
        Assert.assertEquals("FRA000851751", result.get(0).getExteriorMarking());
        Assert.assertEquals("REG_FRA000851751", result.get(0).getRegistrationCode());
    }
}
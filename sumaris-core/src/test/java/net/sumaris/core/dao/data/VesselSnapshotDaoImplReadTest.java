package net.sumaris.core.dao.data;

import net.sumaris.core.dao.AbstractDaoTest;
import net.sumaris.core.dao.DatabaseResource;
import net.sumaris.core.vo.data.VesselSnapshotVO;
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
public class VesselSnapshotDaoImplReadTest extends AbstractDaoTest {

    /** Logger. */
    private static final Logger log =
        LoggerFactory.getLogger(VesselSnapshotDaoImplReadTest.class);

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.readDb();

    @Autowired
    private VesselSnapshotDao dao;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        setCommitOnTearDown(false); // this is need because of delete test
    }

    @Test
    public void findByFilter() {

        VesselFilterVO filter = new VesselFilterVO();

        List<VesselSnapshotVO> result = dao.findByFilter(filter, 0, 10, null, null);

        Assert.assertNotNull(result);
        Assert.assertEquals(2, result.size());
        VesselSnapshotVO vessel1 = result.get(0);
        Assert.assertEquals(1, vessel1.getId().intValue());
        Assert.assertEquals("CN851751", vessel1.getExteriorMarking());
        Assert.assertEquals("FRA000851751", vessel1.getRegistrationCode());
        Assert.assertNotNull(vessel1.getBasePortLocation());
        Assert.assertEquals(10, vessel1.getBasePortLocation().getId().intValue());
        Assert.assertNotNull(vessel1.getRegistrationLocation());
        Assert.assertEquals(1, vessel1.getRegistrationLocation().getId().intValue());
        VesselSnapshotVO vessel2 = result.get(1);
        Assert.assertEquals(2, vessel2.getId().intValue());
        Assert.assertEquals("CN851769", vessel2.getExteriorMarking());
        Assert.assertNull(vessel2.getRegistrationCode());
        Assert.assertNotNull(vessel2.getBasePortLocation());
        Assert.assertEquals(10, vessel2.getBasePortLocation().getId().intValue());
        Assert.assertNull(vessel2.getRegistrationLocation());
    }
}
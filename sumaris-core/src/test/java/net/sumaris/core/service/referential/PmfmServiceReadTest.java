package net.sumaris.core.service.referential;

import net.sumaris.core.dao.DatabaseResource;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.service.AbstractServiceTest;
import net.sumaris.core.service.referential.pmfm.PmfmService;
import net.sumaris.core.vo.filter.ReferentialFilterVO;
import net.sumaris.core.vo.referential.PmfmVO;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * @author peck7 on 20/08/2020.
 */
public class PmfmServiceReadTest extends AbstractServiceTest {

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.readDb();

    @Autowired
    private PmfmService service;

    @Test
    public void find() {

        // unique label
        assertFindResult(ReferentialFilterVO.builder().label("CONVEYOR_BELT").build(), 1);
        // search label
        assertFindResult(ReferentialFilterVO.builder().searchText("CONVEYOR_BELT").build(), 1);
        assertFindResult(ReferentialFilterVO.builder().searchText("NB").build(), 4);

    }

    private void assertFindResult(ReferentialFilterVO filter, int expectedSize) {
        List<PmfmVO> pmfms = service.findByFilter(filter, 0, 100, "id", SortDirection.ASC);
        Assert.assertNotNull(pmfms);
        Assert.assertEquals(expectedSize, pmfms.size());
    }
}

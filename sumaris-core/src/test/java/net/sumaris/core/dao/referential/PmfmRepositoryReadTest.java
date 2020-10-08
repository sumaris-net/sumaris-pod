package net.sumaris.core.dao.referential;

import net.sumaris.core.dao.AbstractDaoTest;
import net.sumaris.core.dao.DatabaseResource;
import net.sumaris.core.dao.referential.pmfm.PmfmRepository;
import net.sumaris.core.vo.referential.PmfmVO;
import net.sumaris.core.vo.referential.ReferentialFetchOptions;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

/**
 * @author peck7 on 19/08/2020.
 */
public class PmfmRepositoryReadTest extends AbstractDaoTest {

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.readDb();

    @Autowired
    private PmfmRepository pmfmRepository;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        setCommitOnTearDown(false);
    }

    @Test
    public void findByLabel() {

        Optional<PmfmVO> pmfm = pmfmRepository.findByLabel("ZZZZZ");
        Assert.assertFalse(pmfm.isPresent());

        pmfm = pmfmRepository.findByLabel("GEAR_SPEED");
        Assert.assertTrue(pmfm.isPresent());
        Assert.assertEquals(9, pmfm.get().getId().intValue());

    }

    @Test
    public void getByLabel() {

        PmfmVO pmfm = pmfmRepository.getByLabel("MAIN_METIER");
        Assert.assertNotNull(pmfm);
        Assert.assertEquals(25, pmfm.getId().intValue());

        try {
            pmfmRepository.getByLabel("____");
            Assert.fail("should throw exception");
        } catch (Exception e) {
            Assert.assertNotNull(e);
        }
    }

    @Test
    public void get() {

        // Normal find with qualitative values
        PmfmVO pmfm = pmfmRepository.get(5);
        Assert.assertNotNull(pmfm);
        Assert.assertNotNull(pmfm.getQualitativeValues());
        Assert.assertEquals(6, pmfm.getQualitativeValues().size());

        // Without qualitative values
        pmfm = pmfmRepository.get(5, ReferentialFetchOptions.builder().withInheritance(false).build());
        Assert.assertNotNull(pmfm);
        Assert.assertNull(pmfm.getQualitativeValues());

    }

    @Test
    public void checkPrefixSuffix() {

        Assert.assertTrue(pmfmRepository.hasLabelPrefix(50, "LANDING"));
        Assert.assertTrue(pmfmRepository.hasLabelPrefix(50, "OR", "LANDING"));
        Assert.assertFalse(pmfmRepository.hasLabelPrefix(50, "OR"));

        Assert.assertFalse(pmfmRepository.hasLabelSuffix(50, "LANDING", "XX"));
        Assert.assertTrue(pmfmRepository.hasLabelSuffix(50, "WEIGHT", "XX"));
        Assert.assertTrue(pmfmRepository.hasLabelSuffix(50, "WEIGHT"));

    }

}

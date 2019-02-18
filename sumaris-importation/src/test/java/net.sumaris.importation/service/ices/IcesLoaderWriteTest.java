package net.sumaris.importation.service.ices;

import net.sumaris.core.util.file.FileUtils;
import net.sumaris.importation.service.AbstractServiceTest;
import org.junit.Ignore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;

public class IcesLoaderWriteTest extends AbstractServiceTest {

    protected static final Logger log = LoggerFactory.getLogger(IcesLoaderWriteTest.class);

    @Autowired
    private IcesDataLoaderServiceImpl service = null;

    @Test
    @Ignore
    public void loadTestFiles() {
        String basePath = "src/test/data/import/";

        // Import FRA sample file
        File file = new File(basePath, "FRA-CL-test.csv");
        loadLanding(file, "FRA");

        // Import BEL sample file
        file = new File(basePath, "BEL-CL-test.csv");
        loadLanding(file, "BEL");

        // Import GBR sample file
        file = new File(basePath, "GBR-mix-test.csv");
        loadMixed(file, "GBR");
    }

    @Test
    @Ignore
    public void loadAll() {
        String basePath = System.getProperty("user.home") + "/Documents/sumaris/data/";

        // Import FRA file
        File file = new File(basePath + "/FRA", "CL_FRA_2000-2017.csv");
        loadLanding(file, "FRA");

        // Import BEL file
        file = new File(basePath+ "/BEL", "CL_BEL-2.csv");
        loadLanding(file, "BEL");

        // Import GBR file
        file = new File(basePath+ "/GBR", "Sumaris All.txt");
        loadMixed(file, "GBR");
    }

    /* -- protected method -- */

    private void loadLanding(File file, String country) {

        try {
            service.loadLanding(file, country, true, false);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            Assert.fail(e.getMessage());
        }
        finally {
            FileUtils.deleteTemporaryFiles(file);
        }

    }

    private void loadMixed(File file, String country) {
        try {
            service.loadMixed(file, country, true, false);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            Assert.fail(e.getMessage());
        }
        finally {
            FileUtils.deleteTemporaryFiles(file);
        }
    }

    private void detectFormatAndLoad(File file, String country) {
        try {
            service.detectFormatAndLoad(file, country, true, false);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            Assert.fail(e.getMessage());
        }
        finally {
            FileUtils.deleteTemporaryFiles(file);
        }
    }
}

package net.sumaris.importation.service.ices;

import net.sumaris.core.util.file.FileUtils;
import net.sumaris.importation.service.AbstractServiceTest;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;

public class IcesLoaderWriteTest extends AbstractServiceTest {

    protected static final Log log = LogFactory.getLog(IcesLoaderWriteTest.class);

    @Autowired
    private IcesDataLoaderServiceImpl service = null;

    @Test
    public void loadLanding() {
        String basePath = "src/test/data/import/";

        // Import FRA sample file
        File file = new File(basePath, "FRA-CL-test.csv");
        try {
            service.loadLanding(file, "FRA", true, false);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            Assert.fail(e.getMessage());
        }
        finally {
            FileUtils.deleteTemporaryFiles(file);
        }

        // Import BEL sample file
        file = new File(basePath, "BEL-CL-test.csv");
        try {
            service.loadLanding(file, "BEL", true, false);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            Assert.fail(e.getMessage());
        }
        finally {
            FileUtils.deleteTemporaryFiles(file);
        }

    }

    @Test
    public void loadMixed() {
        String basePath = "src/test/data/import/";

        // Import GBR sample file
        //File file = new File(basePath, "GBR-mix-test.csv");
        File file = new File(basePath, "GBR-all.csv");
        try {
            service.detectFormatAndLoad(file, "GBR", true, false);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            Assert.fail(e.getMessage());
        }
        finally {
            FileUtils.deleteTemporaryFiles(file);
        }
    }

}

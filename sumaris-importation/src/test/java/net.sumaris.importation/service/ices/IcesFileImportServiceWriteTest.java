package net.sumaris.importation.service.ices;

import net.sumaris.core.model.SumarisTable;
import net.sumaris.importation.exception.FileValidationException;
import net.sumaris.importation.service.AbstractServiceTest;
import net.sumaris.importation.service.FileImportService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.io.File;
import java.io.IOException;

public class IcesFileImportServiceWriteTest extends AbstractServiceTest {

    @Autowired
    private IcesFileImportServiceImpl fileImportService = null;

    @Test
    public void importFile() {
        String basePath = "src/test/data/import/";

        // Import a file
        importFile_FRA_CL(basePath);

    }

    protected void importFile_FRA_CL(String basePath) {
        // Import a valid file
        try {
            fileImportService.importFile(-1, new File(basePath, "FRA-CL-test.csv"), SumarisTable.FILE_ICES_V1_CL, "FRA", false, false);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }


}

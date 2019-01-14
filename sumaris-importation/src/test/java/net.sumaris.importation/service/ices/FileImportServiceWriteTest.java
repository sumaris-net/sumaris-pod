package net.sumaris.importation.service.rdb.v1;

import net.sumaris.core.model.SumarisTable;
import net.sumaris.core.service.ServiceLocator;
import net.sumaris.core.test.DatabaseResource;
import net.sumaris.importation.exception.FileValidationException;
import net.sumaris.importation.service.AbstractServiceTest;
import net.sumaris.importation.service.FileImportService;
import net.sumaris.importation.service.ImportationServiceLocator;
import org.junit.*;
import org.springframework.dao.DataIntegrityViolationException;

import java.io.File;
import java.io.IOException;

public class FileImportServiceWriteTest extends AbstractServiceTest {

    private FileImportService fileImportService = null;
    //private DataService dataService = null;

    @ClassRule
    public static DatabaseResource dbResource = DatabaseResource.(true);

    @Before
    public void setUp() throws Exception {
        fileImportService = ImportationServiceLocator.getFileImportService();
    }

    @After
    public void tearDown() throws Exception {
        fileImportService = null;
    }


    @Test
    public void importFile() {
        String basePath = "src/test/data/import/";


        // Import a file
        importFile_FRA_CL(basePath);

    }


    protected void importFile_FRA_CL(String basePath) {
        // Import a valid file
        try {
            fileImportService.importFile(-1, new File(basePath, "FRA_CL-test.csv"), SumarisTable.FILE_ICES_V1_CL, "FRA", false, false);
        } catch (IOException e) {
            Assert.fail();
        } catch (FileValidationException e) {
            Assert.fail(e.getMessage());
        } catch (DataIntegrityViolationException e) {
            Assert.fail(e.getMessage());
        }
    }


}

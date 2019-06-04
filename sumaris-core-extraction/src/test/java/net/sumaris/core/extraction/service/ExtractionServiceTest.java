package net.sumaris.core.extraction.service;

import net.sumaris.core.extraction.dao.DatabaseResource;

import java.io.File;
import java.io.IOException;

import net.sumaris.core.extraction.vo.ExtractionRawFormatEnum;
import net.sumaris.core.util.Files;
import net.sumaris.core.util.ZipUtils;
import org.apache.commons.io.FileUtils;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author peck7 on 17/12/2018.
 */
public class ExtractionServiceTest extends AbstractServiceTest {

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.writeDb();

    @Autowired
    private ExtractionService service;

    @Test
    public void extractLiveTripAsFile_ICES() {

        // Test the RDB format
        service.executeAndDumpTrips(ExtractionRawFormatEnum.RDB, null);
    }

    @Test
    public void extractLiveTripAsFile_SurvivalTest() {

        // Test Survival test format
        File outputFile = service.executeAndDumpTrips(ExtractionRawFormatEnum.SURVIVAL_TEST, null);

        File debugFile = new File("target/result.zip");
        File debugDirectory = new File("target/result");
        try {
            Files.deleteQuietly(debugFile);
            Files.copyFile(outputFile, debugFile);

            FileUtils.forceMkdir(debugDirectory);
            ZipUtils.uncompressFileToPath(debugFile, debugDirectory.getPath(), true);
        }
        catch(IOException e) {
            // Silent
        }
    }

}
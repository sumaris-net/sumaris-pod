package net.sumaris.core.extraction.service;

import net.sumaris.core.extraction.dao.DatabaseResource;

import java.awt.font.FontRenderContext;
import java.io.File;

import net.sumaris.core.extraction.vo.trip.ExtractionTripFormat;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
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

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void exportTripToFileIces() {

        // ICES export:
        service.exportTripsToFile(ExtractionTripFormat.ICES, null);

    }

    @Test
    public void exportTripToFileSurvivalTest() {

        // Survival test:
        service.exportTripsToFile(ExtractionTripFormat.SURVIVAL_TEST, null);
    }

}
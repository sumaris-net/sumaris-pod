package net.sumaris.core.extraction.service;

import net.sumaris.core.extraction.dao.DatabaseResource;
import java.io.File;
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
    public void executeToFile() {

        service.executeToFile(null, new File("target", String.format("EXT_%s.csv", System.currentTimeMillis())));

    }
}
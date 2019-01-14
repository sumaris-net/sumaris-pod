package net.sumaris.core.extraction.service;

import net.sumaris.core.config.SumarisConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author peck7 on 17/12/2018.
 *
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {ServiceTestConfiguration.class})
@TestPropertySource(locations="classpath:sumaris-core-extraction-test.properties")
public class AbstractServiceTest {

    /** Logger. */
    private static final Log log =
            LogFactory.getLog(AbstractServiceTest.class);

    @Autowired
    protected SumarisConfiguration config;

    /* -- Internal method -- */

    protected SumarisConfiguration getConfig() {
        return config;
    }

}

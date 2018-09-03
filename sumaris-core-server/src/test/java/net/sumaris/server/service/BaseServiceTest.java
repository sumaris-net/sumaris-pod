package net.sumaris.server.service;

import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.server.config.Application;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {Application.class})
@TestPropertySource(locations="classpath:sumaris-core-server-test.properties")
public abstract class BaseServiceTest {

    @Autowired
    protected SumarisConfiguration config;

    /* -- Internal method -- */

    protected SumarisConfiguration getConfig() {
        return config;
    }
}

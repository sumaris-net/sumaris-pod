package net.sumaris.core;

import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.dao.DatabaseFixtures;
import org.springframework.context.annotation.Bean;

/**
 * @author peck7 on 05/12/2018.
 */

@org.springframework.boot.test.context.TestConfiguration
public abstract class TestConfiguration extends net.sumaris.core.test.TestConfiguration {

    @Bean
    public DatabaseFixtures databaseFixtures() {
        return new DatabaseFixtures();
    }

    @Bean
    public static SumarisConfiguration sumarisConfiguration() {
        return initConfiguration("sumaris-core-test.properties");
    }

}

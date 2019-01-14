package net.sumaris.core.extraction.service;

import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.test.TestConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.freemarker.FreeMarkerAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author peck7 on 17/12/2018.
 *
 */
@SpringBootApplication(
        exclude = {
                LiquibaseAutoConfiguration.class,
                FreeMarkerAutoConfiguration.class
        },
        scanBasePackages = {
                "net.sumaris.core"
        }
)
@EntityScan("net.sumaris.core.model")
@EnableTransactionManagement
@EnableJpaRepositories("net.sumaris.core.dao")
public class ServiceTestConfiguration extends TestConfiguration {

        @Bean
        public static SumarisConfiguration sumarisConfiguration() {
                return initConfiguration("sumaris-core-extraction-test.properties");
        }
}

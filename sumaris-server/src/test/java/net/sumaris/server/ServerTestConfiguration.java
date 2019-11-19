package net.sumaris.server;

import net.sumaris.core.Application;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.server.config.SumarisServerConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.freemarker.FreeMarkerAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication(
        exclude = {
                LiquibaseAutoConfiguration.class,
                FreeMarkerAutoConfiguration.class
        },
        scanBasePackages = {
                "net.sumaris.core.dao",
                "net.sumaris.core.service",
                "net.sumaris.core.extraction",
                "net.sumaris.server"
        }
)
@EntityScan("net.sumaris.core.model")
@EnableTransactionManagement
@EnableJpaRepositories("net.sumaris.core.dao")
@org.springframework.boot.test.context.TestConfiguration
public class ServerTestConfiguration extends net.sumaris.core.test.TestConfiguration {

        @Bean
        public static SumarisServerConfiguration sumarisConfiguration() {
                return initConfiguration("sumaris-core-server-test.properties");
        }

        protected static SumarisServerConfiguration initConfiguration(String configFileName) {
                SumarisServerConfiguration config = SumarisServerConfiguration.getInstance();
                if (config == null) {
                        config = new SumarisServerConfiguration(configFileName);
                        SumarisConfiguration.setInstance(config);
                }
                return config;
        }
}
package net.sumaris.core.service;

import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.dao.cache.SumarisCacheAutoConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
            "net.sumaris.core"
    }
)
@EntityScan("net.sumaris.core.model")
@EnableTransactionManagement
@EnableJpaRepositories("net.sumaris.core.dao")
public class ServiceTestConfiguration {
    /** Logger. */
    private static final Logger log =
            LoggerFactory.getLogger(ServiceTestConfiguration.class);

    @Bean
    public static SumarisConfiguration sumarisConfiguration() {
        SumarisConfiguration config = SumarisConfiguration.getInstance();
        if (config == null) {
            log.warn("Sumaris configuration not exists: creating a new one");
            config = new SumarisConfiguration("sumaris-core-test.properties");
            SumarisConfiguration.setInstance(config);
        }
        return config;
    }

}

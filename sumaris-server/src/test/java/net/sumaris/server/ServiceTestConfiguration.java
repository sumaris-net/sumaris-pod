package net.sumaris.server;

/*-
 * #%L
 * SUMARiS:: Server
 * %%
 * Copyright (C) 2018 - 2019 SUMARiS Consortium
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import com.fasterxml.jackson.databind.ObjectMapper;
import com.graphql.spring.boot.test.GraphQLTestTemplate;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.server.config.SumarisServerConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.freemarker.FreeMarkerAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.ApplicationContext;
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
public class ServiceTestConfiguration extends net.sumaris.core.test.TestConfiguration {

    public static final String MODULE_NAME = "sumaris-server";
    public static final String DATASOURCE_PLATFORM = "hsqldb";
    public static final String CONFIG_FILE_PREFIX = MODULE_NAME + "-test";
    public static final String CONFIG_FILE_NAME = CONFIG_FILE_PREFIX + ".properties";
    public static final String I18N_BUNDLE_NAME = MODULE_NAME + "-i18n";

    @Bean
    public SumarisConfiguration configuration() {
        SumarisConfiguration config = super.configuration();

        // Encapsulate existing config into SumarisServerConfiguration class
        if (!(config instanceof SumarisServerConfiguration)) {
            config = new SumarisServerConfiguration(config.getApplicationConfig());
            SumarisConfiguration.setInstance(config);
        }

        return config;
    }

    @Override
    protected String getConfigFileName() {
        return CONFIG_FILE_NAME;
    }

    @Override
    protected String getI18nBundleName() {
        return I18N_BUNDLE_NAME;
    }

    @Bean
    public GraphQLTestTemplate graphQLTestTemplate() {
        return new GraphQLTestTemplate();
    }
}

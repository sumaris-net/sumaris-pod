/*
 * #%L
 * SUMARiS
 * %%
 * Copyright (C) 2019 SUMARiS Consortium
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

package net.sumaris.rdf;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.freemarker.FreeMarkerAutoConfiguration;
import org.springframework.boot.autoconfigure.jms.JmsAutoConfiguration;
import org.springframework.boot.autoconfigure.jms.JndiConnectionFactoryAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author peck7 on 05/12/2018.
 */

@SpringBootApplication(
        exclude = {
                LiquibaseAutoConfiguration.class,
                FreeMarkerAutoConfiguration.class,
                JndiConnectionFactoryAutoConfiguration.class,
                JmsAutoConfiguration.class,
        },
        scanBasePackages = {
                "net.sumaris.core",
                "net.sumaris.rdf"
        }
)
@EntityScan("net.sumaris.core.model")
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = {
        "net.sumaris.core.dao",
    "net.sumaris.rdf.core.dao"
}, considerNestedRepositories = true)
@org.springframework.boot.test.context.TestConfiguration
public abstract class TestConfiguration extends net.sumaris.core.test.TestConfiguration {

    public static final String MODULE_NAME = "sumaris-rdf";
    public static final String DATASOURCE_PLATFORM = "hsqldb";
    public static final String CONFIG_FILE_PREFIX = "application";
    public static final String CONFIG_FILE_NAME = CONFIG_FILE_PREFIX + ".properties";
    public static final String I18N_BUNDLE_NAME = MODULE_NAME + "-i18n";

    @Bean
    public DatabaseFixtures databaseFixtures() {
        return new DatabaseFixtures();
    }

    @Bean
    public static ObjectMapper jacksonObjectMapper() {
        return new Jackson2ObjectMapperBuilder()
                .indentOutput(true) // For test only
                .build();
    }

    @Override
    protected String getConfigFileName() {
        return CONFIG_FILE_NAME;
    }

    @Override
    protected String getI18nBundleName() {
        return I18N_BUNDLE_NAME;
    }
}

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

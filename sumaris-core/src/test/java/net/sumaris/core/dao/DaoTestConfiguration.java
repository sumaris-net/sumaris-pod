package net.sumaris.core.dao;

/*-
 * #%L
 * SUMARiS:: Core
 * %%
 * Copyright (C) 2018 SUMARiS Consortium
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

import com.google.common.base.Preconditions;
import net.sumaris.core.config.SumarisConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.freemarker.FreeMarkerAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.annotation.Resource;
import javax.sql.DataSource;

@SpringBootApplication(
        exclude = {
                LiquibaseAutoConfiguration.class,
                FreeMarkerAutoConfiguration.class
        },
        scanBasePackages = {
                "net.sumaris.core.dao",
                "net.sumaris.core.config",
                "net.sumaris.core.test"
        }
)
@EntityScan("net.sumaris.core.model")
@EnableTransactionManagement
@EnableJpaRepositories("net.sumaris.core.dao")
@org.springframework.boot.test.context.TestConfiguration
public class DaoTestConfiguration {

    /**
     * Logger.
     */
    protected static final Log log =
            LogFactory.getLog(DaoTestConfiguration.class);

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

    @Resource
    private SumarisConfiguration config;

    @Bean
    public DataSource dataSource() {

        // Driver datasource
        Preconditions.checkArgument(StringUtils.isNotBlank(config.getJdbcDriver()), "Missing jdbc driver in configuration");
        Preconditions.checkArgument(StringUtils.isNotBlank(config.getJdbcURL()), "Missing jdbc driver in configuration");
        Preconditions.checkArgument(StringUtils.isNotBlank(config.getJdbcUsername()), "Missing jdbc username in configuration");
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(config.getJdbcDriver());
        dataSource.setUrl(config.getJdbcURL());
        dataSource.setUsername(config.getJdbcUsername());
        dataSource.setPassword(config.getJdbcPassword());

        if (StringUtils.isNotBlank(config.getJdbcSchema())) {
            dataSource.setSchema(config.getJdbcSchema());
        }
        if (StringUtils.isNotBlank(config.getJdbcCatalog())) {
            dataSource.setCatalog(config.getJdbcCatalog());
        }
        return dataSource;
    }

    @Bean
    public DatabaseFixtures databaseFixtures() {
        return new DatabaseFixtures();
    }
}

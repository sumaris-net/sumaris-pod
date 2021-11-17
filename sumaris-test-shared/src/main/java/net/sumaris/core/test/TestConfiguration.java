package net.sumaris.core.test;

/*-
 * #%L
 * SUMARiS:: Test shared
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
import com.google.common.base.Preconditions;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.config.SumarisConfigurationOption;
import net.sumaris.core.util.I18nUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.io.File;

/**
 * @author peck7 on 17/12/2018.
 *
 */
@Slf4j
@org.springframework.boot.test.context.TestConfiguration()
public abstract class TestConfiguration {

    public static SumarisConfiguration createConfiguration(@NonNull String configFileName,
                                                           String... args) {
        log.info(String.format("Configuration file: %s", configFileName));
        SumarisConfiguration config = new SumarisConfiguration(configFileName, args);
        SumarisConfiguration.setInstance(config);
        return config;
    }

    @Bean
    public SumarisConfiguration configuration() {
        // If exists, use existing config (from DatabaseResource)
        SumarisConfiguration config = SumarisConfiguration.getInstance();
        if (config == null) {
            return createConfiguration(getConfigFileName(), getConfigArgs());
        }

        // Init i18n
        I18nUtil.init(config, getI18nBundleName());

        return config;
    }


    @Bean
    public DataSource dataSource(SumarisConfiguration testConfiguration) {

        Preconditions.checkArgument(StringUtils.isNotBlank(testConfiguration.getJdbcDriver()), "Missing jdbc driver in configuration");
        Preconditions.checkArgument(StringUtils.isNotBlank(testConfiguration.getJdbcURL()), "Missing jdbc driver in configuration");
        Preconditions.checkArgument(StringUtils.isNotBlank(testConfiguration.getJdbcUsername()), "Missing jdbc username in configuration");

        if (log.isDebugEnabled()) {
            log.debug(String.format("Database URL: %s", testConfiguration.getJdbcURL()));
            log.debug(String.format("Database username: %s", testConfiguration.getJdbcUsername()));
        }

        // Driver datasource
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(testConfiguration.getJdbcDriver());
        dataSource.setUrl(testConfiguration.getJdbcURL());
        dataSource.setUsername(testConfiguration.getJdbcUsername());
        dataSource.setPassword(testConfiguration.getJdbcPassword());

        if (StringUtils.isNotBlank(testConfiguration.getJdbcSchema())) {
            dataSource.setSchema(testConfiguration.getJdbcSchema());
        }
        if (StringUtils.isNotBlank(testConfiguration.getJdbcCatalog())) {
            dataSource.setCatalog(testConfiguration.getJdbcCatalog());
        }
        return dataSource;
    }

    protected abstract String getConfigFileName();
    protected abstract String getI18nBundleName();

    protected String[] getConfigArgs() {
        return null;
    }

}

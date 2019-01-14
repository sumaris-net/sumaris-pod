package net.sumaris.core.test;

import com.google.common.base.Preconditions;
import net.sumaris.core.config.SumarisConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.annotation.Resource;
import javax.sql.DataSource;

/**
 * @author peck7 on 17/12/2018.
 *
 */
@org.springframework.boot.test.context.TestConfiguration
public abstract class TestConfiguration {

    /** Logger. */
    private static final Logger log =
            LoggerFactory.getLogger(TestConfiguration.class);

    protected static SumarisConfiguration initConfiguration(String configFileName) {
        SumarisConfiguration config = SumarisConfiguration.getInstance();
        if (config == null) {
            log.info(String.format("Configuration file: %s", configFileName));
            config = new SumarisConfiguration(configFileName);
            SumarisConfiguration.setInstance(config);
        }
        return config;
    }

    @Bean
    public DataSource dataSource(SumarisConfiguration config) {

        Preconditions.checkArgument(StringUtils.isNotBlank(config.getJdbcDriver()), "Missing jdbc driver in configuration");
        Preconditions.checkArgument(StringUtils.isNotBlank(config.getJdbcURL()), "Missing jdbc driver in configuration");
        Preconditions.checkArgument(StringUtils.isNotBlank(config.getJdbcUsername()), "Missing jdbc username in configuration");

        log.info(String.format("Database URL: %s", config.getJdbcURL()));
        log.info(String.format("Database username: %s", config.getJdbcUsername()));

        // Driver datasource
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

}

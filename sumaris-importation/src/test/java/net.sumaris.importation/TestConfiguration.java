package net.sumaris.importation;

import com.google.common.base.Preconditions;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.importation.dao.DatabaseFixtures;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.annotation.Resource;
import javax.sql.DataSource;

/**
 * @author peck7 on 05/12/2018.
 */

@org.springframework.boot.test.context.TestConfiguration
public abstract class TestConfiguration {
    /** Logger. */
    private static final Logger log =
            LoggerFactory.getLogger(TestConfiguration.class);

    @Bean
    public static SumarisConfiguration sumarisConfiguration() {
        SumarisConfiguration config = SumarisConfiguration.getInstance();
        if (config == null) {
            log.warn("Sumaris configuration not exists: creating a new one");
            config = new SumarisConfiguration("sumaris-core-importation-test.properties");
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

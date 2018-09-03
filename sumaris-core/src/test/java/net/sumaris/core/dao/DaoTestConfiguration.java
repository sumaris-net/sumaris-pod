package net.sumaris.core.dao;

import com.google.common.base.Preconditions;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.dao.cache.SumarisCacheAutoConfiguration;
import net.sumaris.core.dao.technical.hibernate.HibernateImplicitNamingStrategy;
import net.sumaris.core.dao.technical.hibernate.HibernatePhysicalNamingStrategy;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.cfg.Environment;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.freemarker.FreeMarkerAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.util.Properties;

@SpringBootApplication(
        exclude = {
                LiquibaseAutoConfiguration.class,
                FreeMarkerAutoConfiguration.class,
                // Ignore cache
                SumarisCacheAutoConfiguration.class
        },
        scanBasePackages = {
                "net.sumaris.core.dao",
                "net.sumaris.core.config"
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
        return SumarisConfiguration.getInstance();
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

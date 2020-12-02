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

package net.sumaris.server;

import it.ozimov.springboot.mail.configuration.EnableEmailTools;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.service.ServiceLocator;
import net.sumaris.core.util.ApplicationUtils;
import net.sumaris.core.util.I18nUtil;
import net.sumaris.server.config.SumarisServerConfiguration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.nuiton.i18n.I18n;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.freemarker.FreeMarkerAutoConfiguration;
import org.springframework.boot.autoconfigure.jsonb.JsonbAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.HttpStatus;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.File;
import java.io.IOException;

@SpringBootApplication(
        scanBasePackages = {
                "net.sumaris.core",
                "net.sumaris.rdf",
                // TODO: rename c-ore-extraction into extraction
                //"net.sumaris.extraction",
                "net.sumaris.server"
        },
        exclude = {
                LiquibaseAutoConfiguration.class,
                FreeMarkerAutoConfiguration.class,
                JsonbAutoConfiguration.class
        }
)
@EntityScan(basePackages = {
        "net.sumaris.core.model",
        "net.sumaris.core.extraction.model",
})
@EnableJpaRepositories(basePackages = {
        "net.sumaris.core.dao",
        "net.sumaris.rdf.dao",
        "net.sumaris.core.extraction.dao"
})
@EnableEmailTools
@EnableTransactionManagement
@EnableCaching
@EnableJms
@EnableAsync
public class Application extends SpringBootServletInitializer {
    /**
     * Logger.
     */
    protected static final Logger log =
            LoggerFactory.getLogger(Application.class);
    public static final String CONFIG_FILE_NAME = "application.properties";
    private static final String CONFIG_FILE_ENV_PROPERTY = "spring.config.location";
    private static final String CONFIG_FILE_JNDI_NAME = "java:comp/env/" + CONFIG_FILE_NAME;

    public static void main(String[] args) {
        SumarisServerConfiguration.setArgs(ApplicationUtils.toApplicationConfigArgs(args));
        ConfigurableApplicationContext appContext = SpringApplication.run(Application.class, args);

        // Init service locator
        ServiceLocator.init(appContext);
    }

    @Bean
    public static SumarisServerConfiguration configuration() {
        SumarisServerConfiguration.initDefault(getConfigFile());
        SumarisServerConfiguration config = SumarisServerConfiguration.getInstance();

        // Init I18n
        I18nUtil.init(config, getI18nBundleName());

        // Init directories
        initDirectories(config);

        // Init active MQ
        initActiveMQ(config);

        return config;
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(Application.class);
    }

    @Bean
    public WebMvcConfigurer configureStaticPages() {
        return new WebMvcConfigurer() {
            @Override
            public void addViewControllers(ViewControllerRegistry registry) {

                // Error path
                registry.addViewController("/error")
                        .setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR)
                        .setViewName("forward:/core/error.html");
                registry.setOrder(Ordered.HIGHEST_PRECEDENCE);

                // API path
                {
                    final String API_PATH = "/api";
                    registry.addRedirectViewController("/", API_PATH);
                    registry.addRedirectViewController(API_PATH + "/", API_PATH);
                    registry.addViewController(API_PATH)
                            .setViewName("forward:/core/index.html");
                }

                // GraphiQL path
                {
                    final String GRAPHIQL_PATH = "/api/graphiql";
                    registry.addRedirectViewController(GRAPHIQL_PATH + "/", GRAPHIQL_PATH);
                    registry.addRedirectViewController("/graphiql", GRAPHIQL_PATH);
                    registry.addRedirectViewController("/graphiql/", GRAPHIQL_PATH);
                }

                // WebSocket test path
                {
                    final String WS_TEST_PATH = "/graphql/websocket/test";
                    registry.addRedirectViewController(WS_TEST_PATH + "/", WS_TEST_PATH);
                    registry.addRedirectViewController("/api/graphql/websocket/test", WS_TEST_PATH);
                    registry.addRedirectViewController("/api/graphql/websocket/test/", WS_TEST_PATH);
                    registry.addViewController(WS_TEST_PATH)
                            .setViewName("forward:/websocket/index.html");
                }
            }

            @Override
            public void addCorsMappings(CorsRegistry registry) {
                // Enable Global CORS support for the application
                //See https://stackoverflow.com/questions/35315090/spring-boot-enable-global-cors-support-issue-only-get-is-working-post-put-and
                registry.addMapping("/**")
                        .allowedOrigins("*")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "HEAD", "OPTIONS")
                        .allowedHeaders("accept", "access-control-allow-origin", "authorization", "content-type")
                        .allowCredentials(true);
            }

            @Override
            public void configurePathMatch(PathMatchConfigurer configurer) {
                configurer.setUseSuffixPatternMatch(false);
            }
        };
    }

    @Bean
    public TaskExecutor threadPoolTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("default_task_executor_thread");
        executor.initialize();
        return executor;
    }

    /* -- Internal method -- */

    /**
     * <p>getWebConfigFile.</p>
     *
     * @return a {@link String} object.
     */
    protected static String getConfigFile() {
        // Could override config file id (useful for dev)
        String configFile = CONFIG_FILE_NAME;
        if (System.getProperty(CONFIG_FILE_ENV_PROPERTY) != null) {
            configFile = System.getProperty(CONFIG_FILE_ENV_PROPERTY);
            configFile = configFile.replaceAll("\\\\", "/");
        }
        else {
            try {
                InitialContext ic = new InitialContext();
                String jndiPathToConfFile = (String) ic.lookup(CONFIG_FILE_JNDI_NAME);
                if (StringUtils.isNotBlank(jndiPathToConfFile)) {
                    configFile = jndiPathToConfFile;
                }
            } catch (NamingException e) {
                log.debug(String.format("Error while reading JNDI initial context. Skip configuration path override, from context [%s]", CONFIG_FILE_JNDI_NAME));
            }
        }

        return configFile;
    }


    /**
     * <p>initDirectories.</p>
     */
    protected static void initDirectories(SumarisServerConfiguration config) {

        try {

            // log the data directory used
            log.info(I18n.t("sumaris.server.init.data.directory", config.getDataDirectory()));

            // Data directory
            FileUtils.forceMkdir(config.getDataDirectory());

            // DB attachment directory
            FileUtils.forceMkdir(config.getDbAttachmentDirectory());

            // DB backup directory
            FileUtils.forceMkdir(config.getDbBackupDirectory());

            // Download directory
            FileUtils.forceMkdir(config.getDownloadDirectory());

            // Upload directory
            FileUtils.forceMkdir(config.getUploadDirectory());

            // Trash directory
            FileUtils.forceMkdir(config.getTrashDirectory());

            // temp directory
            File tempDirectory = config.getTempDirectory();
            if (tempDirectory.exists()) {
                // clean temp files
                FileUtils.cleanDirectory(tempDirectory);
            }
        } catch (IOException e) {
            throw new SumarisTechnicalException("Directories initialization failed", e);
        }

    }


    protected static void initActiveMQ(SumarisConfiguration config) {
        // Init active MQ data directory
        System.setProperty("org.apache.activemq.default.directory.prefix", config.getDataDirectory().getPath() + File.separator);
    }

    /**
     * <p>getI18nBundleName.</p>
     *
     * @return a {@link String} object.
     */
    protected static String getI18nBundleName() {
        return "sumaris-server-i18n";
    }
}

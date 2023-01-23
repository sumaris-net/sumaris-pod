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
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.config.SumarisConfigurationOption;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.util.ApplicationUtils;
import net.sumaris.core.util.I18nUtil;
import net.sumaris.core.util.StringUtils;
import net.sumaris.server.config.SumarisServerConfiguration;
import org.apache.commons.io.FileUtils;
import org.nuiton.i18n.I18n;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.freemarker.FreeMarkerAutoConfiguration;
import org.springframework.boot.autoconfigure.jsonb.JsonbAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.web.socket.config.annotation.EnableWebSocket;

import java.io.File;
import java.io.IOException;

@SpringBootApplication(
    scanBasePackages = {
        "net.sumaris.core",
        "net.sumaris.extraction",
        "net.sumaris.importation",
        "net.sumaris.rdf",
        "net.sumaris.server"
    },
    exclude = {
        LiquibaseAutoConfiguration.class,
        FreeMarkerAutoConfiguration.class,
        JsonbAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class,
    }
)
@EnableEmailTools
@EnableWebSocket
@Slf4j
@Profile("!test")
public class Application extends SpringBootServletInitializer {

    public static void main(String[] args) {
        // If not set yet, define custom config location
        if (StringUtils.isBlank(System.getProperty("spring.config.location"))) {
            System.getProperty("spring.config.location", "optional:file:./config/,classpath:/");
        }
        SumarisServerConfiguration.setArgs(ApplicationUtils.toApplicationConfigArgs(args));
        SpringApplication.run(Application.class, args);
    }

    @Bean
    @Primary
    @Profile("!test")
    public static SumarisServerConfiguration configuration(ConfigurableEnvironment env) {
        SumarisServerConfiguration.initDefault(env);
        SumarisServerConfiguration config = SumarisServerConfiguration.getInstance();

        // Init I18n
        I18nUtil.init(config, getI18nBundleName());

        // Init directories
        initDirectories(config);

        // Init active MQ
        initActiveMQ(config);

        // Init cache
        initCache(config);

        return config;
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(Application.class);
    }

    /* -- Internal method -- */

    /**
     * <p>initDirectories.</p>
     */
    protected static void initDirectories(SumarisServerConfiguration config) {

        try {

            // log the data directory used
            log.info(I18n.t("sumaris.server.init.data.directory", config.getDataDirectory()));

            // Data directory
            FileUtils.forceMkdir(config.getDataDirectory());

            // Meas files directory
            FileUtils.forceMkdir(config.getMeasFileDirectory());

            // Image attachment directory
            FileUtils.forceMkdir(config.getImageAttachmentDirectory());

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

    protected static void initCache(SumarisConfiguration config) {
        // Init EHCache directory (see 'ehcache.xml' file)
        System.setProperty(SumarisConfigurationOption.CACHE_DIRECTORY.getKey(), config.getCacheDirectory().getPath() + File.separator);
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

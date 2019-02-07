package net.sumaris.core;

/*-
 * #%L
 * Sumaris3 Batch :: Shape import/export
 * %%
 * Copyright (C) 2017 - 2018 Ifremer
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

import com.google.common.collect.ImmutableList;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.service.ServiceLocator;
import net.sumaris.core.util.ApplicationUtils;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.nuiton.i18n.I18n;
import org.nuiton.i18n.init.DefaultI18nInitializer;
import org.nuiton.i18n.init.UserI18nInitializer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.freemarker.FreeMarkerAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * <p>
 * Application class.
 * </p>
 * 
 */
@SpringBootApplication(
		exclude = {
				LiquibaseAutoConfiguration.class,
                FreeMarkerAutoConfiguration.class
		},
		scanBasePackages = {
				"net.sumaris.core"
		}
)
@EntityScan("net.sumaris.core.model")
@EnableTransactionManagement
@EnableJpaRepositories("net.sumaris.core.dao")
public class Application {

	/* Logger */
	private static final Logger log = LoggerFactory.getLogger(Application.class);

	private static String configFile;

	private static String[] args;

	/**
	 * <p>
	 * main.
	 * </p>
	 *
	 * @param args
	 *            an array of {@link String} objects.
	 */
	public static void main(String[] cmdArgs) {
		// By default, display help
		if (cmdArgs == null || cmdArgs.length == 0) {
			args = new String[] { "-h" };
		}
		else {
			args = cmdArgs;
		}

		// Could override config file id (useful for dev)
		configFile = "application.properties";
		if (System.getProperty(configFile) != null) {
			configFile = System.getProperty(configFile);
			configFile = configFile.replaceAll("\\\\", "/");
			// Override spring location file
			System.setProperty("spring.config.location", configFile);
		}

		SumarisConfiguration.setArgs(ApplicationUtils.adaptArgsForConfig(args));

		try {
            // Start Spring boot
            ConfigurableApplicationContext appContext = SpringApplication.run(Application.class, args);
            appContext.addApplicationListener(applicationEvent -> {
                if (applicationEvent != null && applicationEvent instanceof ContextClosedEvent) {
					log.info("Application closed");
                }
            });

            // Init service locator
            ServiceLocator.init(appContext);

            // Execute all action
			SumarisConfiguration.getInstance().getApplicationConfig().doAllAction();
        } catch (Exception e) {
            log.error("Error in action", e);
        }

    }

	@Bean
	public static SumarisConfiguration sumarisConfiguration() {

		SumarisConfiguration config = SumarisConfiguration.getInstance();
		if (config == null) {
			SumarisConfiguration.initDefault(configFile);
			config = SumarisConfiguration.getInstance();
		}

		// Init i18n
		try {
			initI18n();
		} catch (IOException e) {
			throw new SumarisTechnicalException("i18n initialization failed", e);
		}

		return config;
	}

	/**
	 * <p>
	 * initI18n.
	 * </p>
	 *
	 * @param config
	 *            a {@link SumarisConfiguration} object.
	 * @throws IOException
	 *             if any.
	 */
	protected static void initI18n() throws IOException {

		SumarisConfiguration config = SumarisConfiguration.getInstance();

		// --------------------------------------------------------------------//
		// init i18n
		// --------------------------------------------------------------------//
		File i18nDirectory = new File(config.getDataDirectory(), "i18n");
		if (i18nDirectory.exists()) {
			// clean i18n cache
			FileUtils.cleanDirectory(i18nDirectory);
		}

		FileUtils.forceMkdir(i18nDirectory);

		Locale i18nLocale = config.getI18nLocale();

		if (log.isInfoEnabled()) {
			log.info(String.format("Starts i18n with locale {%s} at {%s}",
					i18nLocale, i18nDirectory));
		}
		I18n.init(new UserI18nInitializer(
				i18nDirectory, new DefaultI18nInitializer(getI18nBundleName())),
				i18nLocale);
	}

	/**
	 * <p>
	 * getI18nBundleName.
	 * </p>
	 *
	 * @return a {@link String} object.
	 */
	protected static String getI18nBundleName() {
		return "sumaris-core-i18n";
	}



}

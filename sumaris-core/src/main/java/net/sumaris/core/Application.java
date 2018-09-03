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

import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.exception.SumarisTechnicalException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuiton.i18n.I18n;
import org.nuiton.i18n.init.DefaultI18nInitializer;
import org.nuiton.i18n.init.UserI18nInitializer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;

/**
 * <p>
 * Application class.
 * </p>
 * 
 */
@SpringBootApplication(
		exclude = {
				LiquibaseAutoConfiguration.class
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
	private static final Log log = LogFactory.getLog(Application.class);

	public static String MAIN_CONFIG_FILE;
    public static String[] MAIN_ARGS;

	/**
	 * <p>
	 * main.
	 * </p>
	 *
	 * @param args
	 *            an array of {@link String} objects.
	 */
	public static void main(String[] args) {
		if (log.isInfoEnabled()) {
			log.info("Starting SUMARIS :: Core with arguments: " + Arrays.toString(args));
		}

		// By default, display help
		if (args == null || args.length == 0) {
			args = new String[] { "-h" };
		}

		// Could override config file id (useful for dev)
		String configFile = "sumaris-core.config";
		if (System.getProperty(configFile) != null) {
			configFile = System.getProperty(configFile);
			configFile = configFile.replaceAll("\\\\", "/");
			// Override spring location file
			System.setProperty("spring.config.location", configFile);
		}

		// Create configuration
		SumarisConfiguration config = new SumarisConfiguration(configFile, args);
		SumarisConfiguration.setInstance(config);


		// Init i18n
		try {
			initI18n(config);
		} catch (IOException e) {
			throw new SumarisTechnicalException("i18n initialization failed", e);
		}

		SpringApplication.run(Application.class, args)
        .addApplicationListener(applicationEvent -> {
            if (applicationEvent != null) {
                log.warn(applicationEvent);
            }
        });


        try {
            SumarisConfiguration.getInstance().getApplicationConfig().doAllAction();
        } catch (Exception e) {
            log.error("Error in action", e);
        }
//        try {
//            SumarisConfiguration.getInstance().getApplicationConfig().doAllAction();
//        } catch (Exception e) {
//            log.error("Error in action", e);
//        }

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
	protected static void initI18n(SumarisConfiguration config) throws IOException {

		// --------------------------------------------------------------------//
		// init i18n
		// --------------------------------------------------------------------//
		File i18nDirectory = new File(config.getDataDirectory(), "i18n");
		if (i18nDirectory.exists()) {
			// clean i18n cache
			FileUtils.cleanDirectory(i18nDirectory);
		}

		FileUtils.forceMkdir(i18nDirectory);

		if (log.isDebugEnabled()) {
			log.debug("I18N directory: " + i18nDirectory);
		}

		Locale i18nLocale = config.getI18nLocale();

		if (log.isInfoEnabled()) {
			log.info(String.format("Starts i18n with locale [%s] at [%s]",
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

	@Bean
	public static SumarisConfiguration sumarisConfiguration() {
		return SumarisConfiguration.getInstance();
	}

}

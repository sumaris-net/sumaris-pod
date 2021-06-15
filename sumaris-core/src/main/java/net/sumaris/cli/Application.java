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

package net.sumaris.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.cli.action.ActionUtils;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.service.ServiceLocator;
import net.sumaris.core.service.technical.ConfigurationService;
import net.sumaris.core.util.ApplicationUtils;
import net.sumaris.core.util.I18nUtil;
import net.sumaris.core.util.StringUtils;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.freemarker.FreeMarkerAutoConfiguration;
import org.springframework.boot.autoconfigure.jms.JndiConnectionFactoryAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Component;


/**
 * <p>
 * Application class.
 * </p>
 *
 */
@SpringBootApplication(
	exclude = {
		LiquibaseAutoConfiguration.class,
		FreeMarkerAutoConfiguration.class,
		JndiConnectionFactoryAutoConfiguration.class
	},
	scanBasePackages = {
		"net.sumaris.core"
	}
)
@EnableAsync
@Component("core-application")
@Slf4j
@Profile("!test")
public class Application {

	private static String CONFIG_FILE;

	private static String[] ARGS;

	public static void run(String[] args, String configFile) {
		run(Application.class, args, configFile);
	}

	public static void run(Class<? extends Application> clazz, String[] args, String configFile) {
		// By default, display help
		if (args == null || args.length == 0) {
			ARGS = new String[] { "-h" };
		}
		else {
			ARGS = args;
		}

		// Could override config file id (useful for dev)
		configFile = StringUtils.isNotBlank(configFile) ? configFile : "application.properties";
		if (System.getProperty(configFile) != null) {
			configFile = System.getProperty(CONFIG_FILE);
			CONFIG_FILE = configFile.replaceAll("\\\\", "/");
			// Override spring location file
			System.setProperty("spring.config.location", CONFIG_FILE);
		}
		else {
			CONFIG_FILE = configFile;
		}

		SumarisConfiguration.setInstance(null); // Reset existing config
		SumarisConfiguration.setArgs(ApplicationUtils.toApplicationConfigArgs(ARGS));

		try {
			// Start Spring boot
			ConfigurableApplicationContext appContext = SpringApplication.run(clazz, ARGS);
			appContext.addApplicationListener(applicationEvent -> {
				// Log when application closed
				if (applicationEvent instanceof ContextClosedEvent) log.debug("Application closed");
			});

			// Init service locator
			ServiceLocator.init(appContext);

			// Log connection info
			ActionUtils.logConnectionProperties();

			// Execute all action
			doAllAction(appContext, true);
		} catch (Exception e) {
			log.error("Error while executing action", e);
		}
	}

	/**
	 * <p>
	 * main.
	 * </p>
	 *
	 * @param args
	 *            an array of {@link String} objects.
	 */
	public static void main(String[] args) {
		run(args, null);
	}

	@Bean
	public SumarisConfiguration configuration(ConfigurableEnvironment env) {

		SumarisConfiguration config = SumarisConfiguration.getInstance();
		if (config == null) {
			SumarisConfiguration.initDefault(env);
			config = SumarisConfiguration.getInstance();
		}

		// Init I18n
		I18nUtil.init(config, getI18nBundleName());

		return config;
	}

	/**
	 * <p>
	 * getI18nBundleName.
	 * </p>
	 *
	 * @return a {@link String} object.
	 */
	protected String getI18nBundleName() {
		return "sumaris-core-i18n";
	}


	protected static void doAllAction(ApplicationContext appContext, boolean runInThread) {
		if (runInThread) {
			try {
				// Execute in a thread
				TaskExecutor taskExecutor = appContext.getBean(TaskExecutor.class);
				taskExecutor.execute(() -> doAllAction(appContext, false));
				return;
			} catch (NoSuchBeanDefinitionException e) {
				// continue
			}
		}

		waitConfigurationReady(appContext);

		try {
			SumarisConfiguration.getInstance().getApplicationConfig().doAllAction();
			System.exit(0);
		} catch(Exception e) {
			log.error("Error while executing action", e);
			System.exit(1);
		}
	}

	protected static void waitConfigurationReady(ApplicationContext appContext) {
		try {
			// Get the configuration service bean
			ConfigurationService service = appContext.getBean(ConfigurationService.class);

			// Make sure configuration has been loaded
			while(!service.isReady()) {
				try {
					Thread.sleep(1000);
				}
				catch (InterruptedException e) {
					// End
				}
			}
		} catch (NoSuchBeanDefinitionException e) {
			// continue
		}
	}

	@Bean
	@Primary
	@ConditionalOnMissingBean
	ObjectMapper jacksonObjectMapper() {
		return new ObjectMapper();
	}
}

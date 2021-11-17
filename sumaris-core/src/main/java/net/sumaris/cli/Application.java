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
import org.springframework.boot.autoconfigure.condition.ConditionalOnNotWebApplication;
import org.springframework.boot.autoconfigure.freemarker.FreeMarkerAutoConfiguration;
import org.springframework.boot.autoconfigure.jms.JndiConnectionFactoryAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Scope;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
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
@ConditionalOnNotWebApplication()
public class Application {

	private static String[] ARGS;

	public static void main(String[] args) {
		run(args, null);
	}

	public static void run(String[] args, String configLocation) {
		run(Application.class, args, configLocation);
	}

	public static void run(Class<? extends Application> clazz, String[] args, String configLocation) {
		// By default, display help
		if (args == null || args.length == 0) {
			ARGS = new String[] { "-h" };
		}
		else {
			ARGS = args;
		}

		SumarisConfiguration.setInstance(null); // Reset existing config
		SumarisConfiguration.setArgs(ApplicationUtils.toApplicationConfigArgs(ARGS));

		// If not set yet, define custom config location
		if (StringUtils.isNotBlank(configLocation)) {
			System.setProperty("spring.config.location", configLocation);
		}
		else if (StringUtils.isBlank(System.getProperty("spring.config.location"))) {
			System.setProperty("spring.config.location", "optional:file:./config/,classpath:/");
		}

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


		try {
			waitConfigurationReady(appContext);

			SumarisConfiguration.getInstance().getApplicationConfig().doAllAction();
			System.exit(0);
		} catch(Exception e) {
			if (!(e instanceof InterruptedException)) {
				log.error("Error while executing action", e);
			}
			System.exit(1);
		}
	}

	protected static void waitConfigurationReady(ApplicationContext appContext) throws InterruptedException {
		try {
			// Get the configuration service bean
			ConfigurationService service = appContext.getBean(ConfigurationService.class);

			// Make sure configuration has been loaded
			while(!service.isReady()) {
				Thread.sleep(1000);
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

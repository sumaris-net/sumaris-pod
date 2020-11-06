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

import com.querydsl.jpa.impl.JPAQueryFactory;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.service.ServiceLocator;
import net.sumaris.core.util.ApplicationUtils;
import net.sumaris.core.util.I18nUtil;
import net.sumaris.core.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.freemarker.FreeMarkerAutoConfiguration;
import org.springframework.boot.autoconfigure.jms.JmsAutoConfiguration;
import org.springframework.boot.autoconfigure.jms.activemq.ActiveMQAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.persistence.EntityManager;

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
				JmsAutoConfiguration.class,
				ActiveMQAutoConfiguration.class
		},
		scanBasePackages = {
				"net.sumaris.core"
		}
)
@EntityScan("net.sumaris.core.model")
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = {
		"net.sumaris.core.dao"
})
@EnableAsync
@Component("core-application")
public class Application {

	/* Logger */
	private static final Logger log = LoggerFactory.getLogger(Application.class);

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
				if (applicationEvent instanceof ContextClosedEvent) log.info("Application closed");
			});

			// Init service locator
			ServiceLocator.init(appContext);

			// Execute all action
			doAllAction(appContext);
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
	public SumarisConfiguration configuration() {

		SumarisConfiguration config = SumarisConfiguration.getInstance();
		if (config == null) {
			SumarisConfiguration.initDefault(CONFIG_FILE);
			config = SumarisConfiguration.getInstance();
		}

		// Init I18n
		I18nUtil.init(config, getI18nBundleName());

		return config;
	}

	@Bean
	public JPAQueryFactory jpaQueryFactory(EntityManager em) {
		return new JPAQueryFactory(em);
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


	protected static void doAllAction(ApplicationContext appContext) {
		TaskExecutor taskExecutor = null;
		try {
			// Execute all action
			taskExecutor = appContext.getBean(TaskExecutor.class);
		} catch (NoSuchBeanDefinitionException e) {
			taskExecutor = null;
		}

		// Execute all action
		if (taskExecutor != null) {
			taskExecutor.execute(() -> {
				try {
					SumarisConfiguration.getInstance().getApplicationConfig().doAllAction();
				} catch(Exception e) {
					log.error("Error while executing action", e);
				}
			});
		}
		else {
			try {
				SumarisConfiguration.getInstance().getApplicationConfig().doAllAction();
			} catch (Exception e) {
				log.error("Error while executing action", e);
			}
		}
	}

}

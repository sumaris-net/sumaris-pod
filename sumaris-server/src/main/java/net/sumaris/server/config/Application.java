package net.sumaris.server.config;

/*-
 * #%L
 * SUMARiS:: Server
 * %%
 * Copyright (C) 2018 SUMARiS Consortium
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


import com.fasterxml.jackson.databind.ObjectMapper;
import it.ozimov.springboot.mail.configuration.EnableEmailTools;
import net.sumaris.core.util.ApplicationUtils;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.freemarker.FreeMarkerAutoConfiguration;
import org.springframework.boot.autoconfigure.jms.DefaultJmsListenerContainerFactoryConfigurer;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.HttpStatus;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerContainerFactory;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageType;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.util.ErrorHandler;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.jms.ConnectionFactory;

@EnableJms
@SpringBootApplication(
        scanBasePackages = {
                "net.sumaris.core",
                "net.sumaris.rdf",
                "net.sumaris.server"
        },
        exclude = {
                LiquibaseAutoConfiguration.class,
                FreeMarkerAutoConfiguration.class
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
public class Application extends SpringBootServletInitializer {
    /**
     * Logger.
     */
    protected static final Logger log =
            LoggerFactory.getLogger(Application.class);

    @Bean
    public static SumarisServerConfiguration sumarisConfiguration() {
        return SumarisServerConfiguration.getInstance();
    }

    public static void main(String[] args) {
        SumarisServerConfiguration.setArgs(ApplicationUtils.adaptArgsForConfig(args));
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public WebMvcConfigurer forwardToIndex() {
        return new WebMvcConfigurer() {
            @Override
            public void addViewControllers(ViewControllerRegistry registry) {

                // define path /
                registry.addRedirectViewController("/", "/api");
                registry.addRedirectViewController("/api/", "/api");
                registry.addViewController("/api")
                        .setViewName("forward:/core/index.html");

                // define path /graphiql
                registry.addRedirectViewController("/graphiql/", "/graphiql");
                registry.addRedirectViewController("/api/graphiql", "/graphiql");
                registry.addRedirectViewController("/api/graphiql/", "/graphiql");
                registry.addViewController("/graphiql").setViewName(
                        "forward:/graphiql/index.html");
                // define path /error
                registry.addViewController("/error")
                        .setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR)
                        .setViewName("forward:/core/error.html");
                registry.setOrder(Ordered.HIGHEST_PRECEDENCE);

                // define path to /graphql/websocket test page
                registry.addRedirectViewController("/graphql/websocket/test/", "/graphql/websocket/test");
                registry.addRedirectViewController("/api/graphql/websocket/test", "/graphql/websocket/test");
                registry.addRedirectViewController("/api/graphql/websocket/test/", "/graphql/websocket/test");
                registry.addViewController("/graphql/websocket/test")
                        .setViewName("forward:/websocket/index.html");

                // define path to /api/rdf test page
                registry.addRedirectViewController("/api/rdf/test/", "/api/rdf/test");
                registry.addViewController("/api/rdf/test")
                        .setViewName("forward:/rdf/index.html");
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
        executor.setThreadNamePrefix("default_task_executor_thread");
        executor.initialize();
        return executor;
    }

    @Bean
    public JmsListenerContainerFactory<?> jmsListenerContainerFactory(
            ConnectionFactory connectionFactory,
            DefaultJmsListenerContainerFactoryConfigurer configurer) {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setErrorHandler(t -> log.error("An error has occurred in the JMS transaction: " + t.getMessage(), t));
        configurer.configure(factory, connectionFactory);
        return factory;
    }

    @Bean
    public MessageConverter jacksonJmsMessageConverter(ObjectMapper objectMapper) {
        // Serialize message content to json using TextMessage
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setObjectMapper(objectMapper);
        converter.setTargetType(MessageType.TEXT);
        converter.setTypeIdPropertyName("_type");
        return converter;
    }

    @Bean
    @ConditionalOnClass(name = "org.apache.activemq.store.kahadb.KahaDBPersistenceAdapter")
    public ConnectionFactory connectionFactory() {
        // Enable persistence, with file storage
        return new ActiveMQConnectionFactory("vm://localhost?broker.persistent=true");
    }

}

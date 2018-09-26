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

import com.google.common.collect.ImmutableList;
import it.ozimov.springboot.mail.configuration.EnableEmailTools;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.freemarker.FreeMarkerAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@SpringBootApplication(
        scanBasePackages = {
                "net.sumaris.core",
                "net.sumaris.server"
        },
        exclude = {
                LiquibaseAutoConfiguration.class,
                FreeMarkerAutoConfiguration.class
        }
)
@EntityScan("net.sumaris.core.model")
@EnableJpaRepositories("net.sumaris.core.dao")
@EnableEmailTools
@EnableTransactionManagement
@EnableCaching
public class Application extends SpringBootServletInitializer {
    /**
     * Logger.
     */
    protected static final Log log =
            LogFactory.getLog(Application.class);

    @Bean
    public static SumarisServerConfiguration sumarisConfiguration() {
        return SumarisServerConfiguration.getInstance();
    }

    public static void main(String[] args) {
        SumarisServerConfiguration.setArgs(adaptArgsForConfig(args));
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public WebMvcConfigurerAdapter forwardToIndex() {
        return new WebMvcConfigurerAdapter() {
            @Override
            public void addViewControllers(ViewControllerRegistry registry) {

                // define path /
                registry.addViewController("/").setViewName(
                        "forward:/core/index.html");

                // define path /graphiql
                registry.addRedirectViewController("/graphiql/", "/graphiql");
                registry.addViewController("/graphiql").setViewName(
                        "forward:/graphiql/index.html");
                // define path /error
                registry.addViewController("/error")
                        .setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR)
                        .setViewName("forward:/core/error.html");
                registry.setOrder(Ordered.HIGHEST_PRECEDENCE);

                // define path to websocket test page
                registry.addRedirectViewController("/graphql/websocket/test/", "/graphql/websocket/test");
                registry.addViewController("/graphql/websocket/test")
                        .setViewName("forward:/websocket/index.html");
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
                super.configurePathMatch(configurer);
                configurer.setUseSuffixPatternMatch(false);
            }
        };
    }

    /* -- --*/
    protected static String[] adaptArgsForConfig(String... args) {

        final Pattern optionPattern = Pattern.compile("--([a-zA-Z0-9._]+)=([^ \t]+)");

        List<String> configArgs = ImmutableList.copyOf(args).stream()
                .map(optionPattern::matcher)
                .filter(matcher -> matcher.matches())
                .flatMap(matcher -> {
                    String name = matcher.group(1);
                    String value = matcher.group(1);
                    return ImmutableList.of("--option", name, value).stream();
            }).collect(Collectors.toList());

        return configArgs.toArray(new String[configArgs.size()]);
    }
}

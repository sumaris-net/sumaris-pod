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

package net.sumaris.server.config;

import net.sumaris.server.http.ExtractionRestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.servlet.Servlet;

@Configuration
@ConditionalOnClass({Servlet.class, DispatcherServlet.class})
@ConditionalOnBean({WebMvcConfigurer.class})
@ConditionalOnProperty(
        prefix = "sumaris.extraction",
        name = {"enabled"},
        matchIfMissing = true
)
public class ExtractionWebAutoConfiguration {
    /**
     * Logger.
     */
    protected static final Logger log =
            LoggerFactory.getLogger(ExtractionWebAutoConfiguration.class);

    @Bean
    @ConditionalOnProperty(
            prefix = "spring.main",
            name = {"web-application-type"},
            havingValue = "servlet",
            matchIfMissing = true
    )
    public WebMvcConfigurer configureExtractionStatics() {
        return new WebMvcConfigurer() {
            @Override
            public void addViewControllers(ViewControllerRegistry registry) {
                log.debug("Adding Extraction web redirects...");
                boolean debug = log.isDebugEnabled();

                // Extraction manual
                {
                    final String MANUAL_BASE_PATH = ExtractionRestController.MANUAL_BASE_PATH;
                    registry.addRedirectViewController(MANUAL_BASE_PATH + "/", MANUAL_BASE_PATH);
                    registry.addViewController(MANUAL_BASE_PATH).setViewName("forward:/manual/index.html");
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
}

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

package net.sumaris.extraction.server.config;

import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.model.technical.history.ProcessingFrequencyEnum;
import net.sumaris.extraction.core.config.ExtractionAutoConfiguration;
import net.sumaris.extraction.core.config.ExtractionConfiguration;
import net.sumaris.extraction.server.http.ExtractionRestController;
import net.sumaris.extraction.server.http.ExtractionRestPaths;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
@ConditionalOnBean({ExtractionAutoConfiguration.class})
@AutoConfigureAfter({ExtractionAutoConfiguration.class})
@ConditionalOnWebApplication
@ComponentScan(basePackages = "net.sumaris.extraction.server")
@EnableScheduling
@Slf4j
public class ExtractionWebAutoConfiguration {

    @Bean
    public WebMvcConfigurer configureExtractionWebMvc() {
        return new WebMvcConfigurer() {
            @Override
            public void addViewControllers(ViewControllerRegistry registry) {
                log.debug("Adding Extraction web redirects...");

                // Extraction manual
                {
                    final String DOC_BASE_PATH = ExtractionRestController.DOC_BASE_PATH;
                    registry.addRedirectViewController(DOC_BASE_PATH + "/", DOC_BASE_PATH);
                    registry.addViewController(DOC_BASE_PATH).setViewName("forward:/doc/index.html");
                }

            }

            @Override
            public void addCorsMappings(CorsRegistry registry) {
                // Enable Global CORS support for the application
                //See https://stackoverflow.com/questions/35315090/spring-boot-enable-global-cors-support-issue-only-get-is-working-post-put-and
                registry.addMapping(ExtractionRestPaths.BASE_PATH + "/**")
                        .allowedOriginPatterns("*")
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
    @ConditionalOnBean(name="extractionTaskExecutor")
    @ConditionalOnMissingBean({SchedulingConfigurer.class})
    public SchedulingConfigurer schedulingConfigurer(Executor extractionTaskExecutor) {
        return taskRegistrar -> taskRegistrar.setScheduler(extractionTaskExecutor);
    }

    @Bean
    @ConditionalOnProperty(
        name = "sumaris.extraction.scheduling.enabled",
        matchIfMissing = true
    )
    public Executor extractionTaskExecutor() {
        return Executors.newScheduledThreadPool((ProcessingFrequencyEnum.values().length - 1) * 2);
    }
}

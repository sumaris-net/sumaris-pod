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

import lombok.extern.slf4j.Slf4j;
import net.sumaris.server.http.filter.CORSFilters;
import net.sumaris.server.http.graphql.GraphQLPaths;
import net.sumaris.server.http.rest.RestPaths;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableAsync
@Slf4j
public class WebMvcConfiguration extends SpringBootServletInitializer {

    @Bean
    public WebMvcConfigurer configureStaticPages() {
        return new WebMvcConfigurer() {

            @Override
            public void addViewControllers(ViewControllerRegistry registry) {

                // 404 error
                registry.addViewController("/404")
                    .setStatusCode(HttpStatus.NOT_FOUND)
                    .setViewName("forward:/api/404.html");

                // Error path
                registry.addViewController("/error")
                    .setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR)
                    .setViewName("forward:/api/error.html");

                registry.setOrder(Ordered.HIGHEST_PRECEDENCE);

                // API path
                {
                    final String API_PATH = RestPaths.BASE_API_PATH;
                    registry.addRedirectViewController("/", API_PATH);
                    registry.addRedirectViewController(API_PATH + "/", API_PATH);
                    registry.addViewController(API_PATH)
                            .setViewName("forward:/api/index.html");
                }

                // GraphiQL path
                {
                    final String GRAPHIQL_PATH = "/api/graphiql";
                    registry.addRedirectViewController(GRAPHIQL_PATH + "/", GRAPHIQL_PATH);
                    registry.addRedirectViewController("/graphiql", GRAPHIQL_PATH);
                    registry.addRedirectViewController("/graphiql/", GRAPHIQL_PATH);
                }

                // GraphQL WebSocket test path
                {
                    final String WS_TEST_PATH = GraphQLPaths.SUBSCRIPTION_PATH + "/test";
                    registry.addRedirectViewController(WS_TEST_PATH + "/", WS_TEST_PATH);
                    registry.addRedirectViewController(RestPaths.BASE_API_PATH + WS_TEST_PATH, WS_TEST_PATH);
                    registry.addRedirectViewController(RestPaths.BASE_API_PATH + WS_TEST_PATH + "/", WS_TEST_PATH);
                    registry.addViewController(WS_TEST_PATH)
                        .setViewName("forward:/websocket/index.html");
                }
            }

            @Override
            public void configurePathMatch(PathMatchConfigurer configurer) {
                configurer.setUseSuffixPatternMatch(true);
            }
        };
    }


    @Bean
    public WebMvcConfigurer configureBaseCORS() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                // Enable CORS support for /api
                //See https://stackoverflow.com/questions/35315090/spring-boot-enable-global-cors-support-issue-only-get-is-working-post-put-and
                registry.addMapping(RestPaths.BASE_API_PATH + "/**")
                    .allowedOriginPatterns("*")
                    .allowedMethods(CORSFilters.ALLOWED_METHODS)
                    .allowedHeaders(CORSFilters.ALLOWED_HEADERS)
                    .allowCredentials(CORSFilters.ALLOWED_CREDENTIALS);

                // Enable CORS support for /graphql
                registry.addMapping(GraphQLPaths.BASE_PATH)
                    .allowedOriginPatterns("*")
                    .allowedMethods(CORSFilters.ALLOWED_METHODS)
                    .allowedHeaders(CORSFilters.ALLOWED_HEADERS)
                    .allowCredentials(CORSFilters.ALLOWED_CREDENTIALS);

                // Enable CORS support for /download
                registry.addMapping(RestPaths.DOWNLOAD_PATH + "/**")
                    .allowedOriginPatterns("*")
                    .allowedMethods(HttpMethod.GET.name(),
                        HttpMethod.HEAD.name(),
                        HttpMethod.OPTIONS.name())
                    .allowedHeaders(CORSFilters.ALLOWED_HEADERS)
                    .allowCredentials(CORSFilters.ALLOWED_CREDENTIALS);

                // Enable CORS support for /upload
                registry.addMapping(RestPaths.UPLOAD_PATH + "/**")
                    .allowedOriginPatterns("*")
                    .allowedMethods(HttpMethod.POST.name(),
                        HttpMethod.HEAD.name(),
                        HttpMethod.OPTIONS.name())
                    .allowedHeaders(CORSFilters.ALLOWED_HEADERS)
                    .allowCredentials(CORSFilters.ALLOWED_CREDENTIALS);
            }
        };
    }
}

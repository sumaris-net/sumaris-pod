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

package net.sumaris.rdf.server.config;

import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.model.ModelVocabularyEnum;
import net.sumaris.core.model.annotation.OntologyEntities;
import net.sumaris.rdf.core.config.RdfAutoConfiguration;
import net.sumaris.rdf.core.config.RdfConfiguration;
import net.sumaris.rdf.core.model.ModelURIs;
import net.sumaris.rdf.core.util.RdfFormat;
import net.sumaris.rdf.server.http.rest.RdfRestPaths;
import net.sumaris.rdf.server.http.rest.ontology.OntologyRdfRestController;
import org.apache.jena.ext.com.google.common.collect.Maps;
import org.nuiton.version.Version;
import org.nuiton.version.VersionBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

@Configuration
@ConditionalOnBean({RdfAutoConfiguration.class})
@ConditionalOnWebApplication
@Slf4j
public class RdfWebAutoConfiguration {

    @Bean
    @ConditionalOnProperty(
        prefix = "spring.main",
        name = {"web-application-type"},
        havingValue = "servlet",
        matchIfMissing = true
    )
    public WebMvcConfigurer configureRdfWebMvc(final RdfConfiguration config) {
        return new WebMvcConfigurer() {
            @Override
            public void addViewControllers(ViewControllerRegistry registry) {
                log.info("Adding RDF web redirects...");
                boolean debug = log.isDebugEnabled();

                // URI builder
                {
                    final String URI_BUILDER_PATH = RdfRestPaths.SCHEMA_BASE_PATH + "/uri";
                    registry.addRedirectViewController(RdfRestPaths.SCHEMA_BASE_PATH, URI_BUILDER_PATH);
                    registry.addRedirectViewController(RdfRestPaths.SCHEMA_BASE_PATH + "/", URI_BUILDER_PATH);
                    registry.addRedirectViewController("/uri", URI_BUILDER_PATH);
                    registry.addRedirectViewController("/uri/", URI_BUILDER_PATH);
                    registry.addStatusController("/schema/data", HttpStatus.BAD_REQUEST);
                    registry.addStatusController("/schema/data/", HttpStatus.BAD_REQUEST);
                    registry.addRedirectViewController("/ontology", URI_BUILDER_PATH);
                    registry.addRedirectViewController("/ontology/", URI_BUILDER_PATH);
                    registry.addRedirectViewController("/api/uri", URI_BUILDER_PATH);
                    registry.addRedirectViewController("/api/uri/", URI_BUILDER_PATH);
                    registry.addRedirectViewController("/api/schema", URI_BUILDER_PATH);
                    registry.addRedirectViewController("/api/schema/", URI_BUILDER_PATH);
                    registry.addRedirectViewController("/api/ontology", URI_BUILDER_PATH);
                    registry.addRedirectViewController("/api/ontology/", URI_BUILDER_PATH);
                    registry.addViewController(URI_BUILDER_PATH).setViewName("forward:/uri/index.html");

                    ModelURIs.RDF_URL_BY_PREFIX.keySet()
                        .forEach(ns -> Arrays.stream(RdfFormat.values())
                            .map(RdfFormat::toJenaLang)
                            .filter(Objects::nonNull)
                            .flatMap(lang -> lang.getFileExtensions().stream())
                            .forEach(fileExt -> {
                                final String fileUrlPath = String.format("%s/%s.%s", RdfRestPaths.SCHEMA_FILES_BASE_PATH, ns, fileExt);
                                log.debug("Adding virtual RDF file at: {}", fileUrlPath);
                                registry.addViewController(fileUrlPath)
                                    .setViewName(String.format("forward:%s?prefix=%s&format=%s", OntologyRdfRestController.SCHEMA_CONVERT_PATH, ns, fileExt));
                            })
                        );
                }

                // define path /webvowl
                {
                    final String WEBVOWL_PATH_SLASH = RdfRestPaths.WEBVOWL_BASE_PATH + "/";
                    registry.addRedirectViewController(RdfRestPaths.WEBVOWL_BASE_PATH, WEBVOWL_PATH_SLASH);
                    registry.addRedirectViewController("/api/webvowl", WEBVOWL_PATH_SLASH);
                    registry.addRedirectViewController("/api/webvowl/", WEBVOWL_PATH_SLASH);
                    registry.addRedirectViewController("/schema/webvowl", WEBVOWL_PATH_SLASH);
                    registry.addRedirectViewController("/schema/webvowl/", WEBVOWL_PATH_SLASH);
                    registry.addViewController(WEBVOWL_PATH_SLASH).setViewName("forward:/webvowl/index.html");

                    String DATA_FILE_PATH = WEBVOWL_PATH_SLASH + "data/%s.json";

                    // WebVOWL schema files: from internal vocabularies
                    Map<String, OntologyEntities.Definition> ontologyDefByVocabulary = Maps.newHashMap();
                    OntologyEntities.getOntologyEntityDefs(config.getDelegate(), ModelVocabularyEnum.DEFAULT.getLabel(), config.getModelVersion())
                        .stream()
                        .sorted((def1, def2) -> {
                            // Sort by name
                            int result = def1.getName().compareTo(def2.getName());
                            if (result != 0) return result;

                            // Sort by version (greatest first)
                            Version v1 = VersionBuilder.create(def1.getVersion()).build();
                            Version v2 = VersionBuilder.create(def1.getVersion()).build();
                            return v1.compareTo(v2) * -1;
                        })
                        .forEach(def -> {
                            // Insert first [vocabulary, version]
                            if (!ontologyDefByVocabulary.containsKey(def.getVocabulary())) {
                                ontologyDefByVocabulary.put(def.getVocabulary(), def);
                            }
                        });
                    ontologyDefByVocabulary.forEach((vocabulary, def) -> {
                            registry.addViewController(String.format(DATA_FILE_PATH, vocabulary))
                                .setViewName(String.format("forward:%s/%s/%s?format=vowl", RdfRestPaths.SCHEMA_BASE_PATH, vocabulary, def.getVersion()));
                    });

                    // WebVOWL schema files: from external URI
                    ModelURIs.RDF_URL_BY_PREFIX.keySet()
                        .forEach(ns -> registry.addViewController(String.format(DATA_FILE_PATH, ns))
                            .setViewName(String.format("forward:/webvowl/convert?prefix=%s&format=vowl", ns)));
                }

                // YasGUI
                {
                    final String YASGUI_PATH = "/sparql/ui";
                    registry.addRedirectViewController("/sparql/ui/", YASGUI_PATH);
                    registry.addRedirectViewController("/api/sparql/", YASGUI_PATH);
                    registry.addRedirectViewController("/api/sparql/ui/", YASGUI_PATH);
                    registry.addViewController(YASGUI_PATH).setViewName("forward:/yasgui/index.html");
                }
            }

            @Override
            public void addCorsMappings(CorsRegistry registry) {
                // Enable Global CORS support for the application
                //See https://stackoverflow.com/questions/35315090/spring-boot-enable-global-cors-support-issue-only-get-is-working-post-put-and
                registry.addMapping(RdfRestPaths.SPARQL_ENDPOINT + "/**")
                    .allowedOriginPatterns("*")
                    .allowedMethods("GET", "POST", "PUT", "DELETE", "HEAD", "OPTIONS")
                    .allowedHeaders("accept", "access-control-allow-origin", "authorization", "content-type")
                    .allowCredentials(true);

                registry.addMapping(RdfRestPaths.SCHEMA_BASE_PATH + "/**")
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
}

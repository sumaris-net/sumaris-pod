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

package net.sumaris.rdf.config;

import com.google.common.base.Preconditions;
import net.sumaris.rdf.model.ModelURIs;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.server.http.rest.RdfFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;
import java.io.File;
import java.util.Arrays;
import java.util.Objects;

@Configuration
@ConditionalOnProperty(
        prefix = "rdf",
        name = {"enabled"},
        matchIfMissing = true)
public class RdfConfiguration  {
    /**
     * Logger.
     */
    protected static final Logger log =
            LoggerFactory.getLogger(RdfConfiguration.class);

    @Resource(name = "sumarisConfiguration")
    private SumarisConfiguration config;

    private String modelBaseUri;

    @Bean
    public WebMvcConfigurer configureRdfStatics() {
        return new WebMvcConfigurer() {
            @Override
            public void addViewControllers(ViewControllerRegistry registry) {
                log.info("Adding RDF web redirects...");
                boolean debug = log.isDebugEnabled();

                // Ontology
                {
                    final String RDF_URI_TEST_PATH = "/ontology";
                    registry.addRedirectViewController(RDF_URI_TEST_PATH + "/", RDF_URI_TEST_PATH);
                    registry.addStatusController("/ontology/data", HttpStatus.BAD_REQUEST);
                    registry.addStatusController("/ontology/data/", HttpStatus.BAD_REQUEST);
                    registry.addRedirectViewController("/api/ontology", RDF_URI_TEST_PATH);
                    registry.addRedirectViewController("/api/ontology/", RDF_URI_TEST_PATH);
                    registry.addRedirectViewController("/api/ontology/", RDF_URI_TEST_PATH);
                    registry.addViewController(RDF_URI_TEST_PATH).setViewName("forward:/rdfuri/index.html");

                    registry.addStatusController("/ontology/data/TaxonName/1", HttpStatus.BAD_REQUEST);

                    ModelURIs.RDF_URL_BY_PREFIX.keySet()
                            .forEach(ns -> Arrays.stream(RdfFormat.values())
                                    .map(RdfFormat::toJenaLang)
                                    .filter(Objects::nonNull)
                                    .flatMap(lang -> lang.getFileExtensions().stream())
                                    .forEach(fileExt -> {
                                        if (debug) log.debug(String.format("Adding virtual rdf file: /ontology/files/%s.%s", ns, fileExt));
                                        registry
                                                .addViewController(String.format("/ontology/files/%s.%s", ns, fileExt))
                                                .setViewName(String.format("forward:/ontology/convert?prefix=%s&format=%s", ns, fileExt));
                                    })
                            );
                }

                // define path /webvowl
                {
                    final String WEBVOWL_PATH = "/webvowl/";
                    registry.addRedirectViewController("/webvowl", WEBVOWL_PATH);
                    registry.addRedirectViewController("/api/webvowl", WEBVOWL_PATH);
                    registry.addRedirectViewController("/api/webvowl/", WEBVOWL_PATH);
                    registry.addRedirectViewController("/ontology/webvowl", WEBVOWL_PATH);
                    registry.addRedirectViewController("/ontology/webvowl/", WEBVOWL_PATH);
                    registry.addViewController(WEBVOWL_PATH).setViewName("forward:/webvowl/index.html");

                    // WebVOWL data files
                    registry.addViewController("/webvowl/data/taxon.json").setViewName("forward:/ontology/schema/TaxonName/?format=vowl");
                    registry.addViewController("/webvowl/data/gear.json").setViewName("forward:/ontology/schema/Gear/?format=vowl");
                    ModelURIs.RDF_URL_BY_PREFIX.keySet()
                            .forEach(ns -> registry.addViewController(String.format("/webvowl/data/%s.json", ns))
                                    .setViewName(String.format("forward:/ontology/convert?prefix=%s&format=vowl", ns)));
                }

                // YasGUI
                {
                    registry.addRedirectViewController("/sparql/ui/", "/sparql/ui");
                    registry.addRedirectViewController("/api/sparql/", "/sparql/ui");
                    registry.addRedirectViewController("/api/sparql/ui/", "/sparql/ui");
                    registry.addViewController("/sparql/ui")
                            .setViewName("forward:/yasgui/index.html");
                }

                // Use case > Taxon search
                {
                    final String TAXON_PATH = "/api/search/taxon";
                    registry.addRedirectViewController(TAXON_PATH + "/", TAXON_PATH);
                    registry.addViewController(TAXON_PATH)
                            .setViewName("forward:/taxon/index.html");
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

    public boolean isRdfEnable() {
        return config.getApplicationConfig().getOptionAsBoolean(RdfConfigurationOption.RDF_ENABLED.getKey());
    }

    public File getRdfDirectory() {
        return config.getApplicationConfig().getOptionAsFile(RdfConfigurationOption.RDF_DIRECTORY.getKey());
    }

    public String getModelBaseUri() {
        if (this.modelBaseUri != null) return this.modelBaseUri;

        // Init property, if not init yet
        String modelPrefix = config.getApplicationConfig().getOption(RdfConfigurationOption.RDF_MODEL_BASE_URI.getKey());
        Preconditions.checkNotNull(modelPrefix, String.format("Missing configuration option {%s}", RdfConfigurationOption.RDF_MODEL_BASE_URI.getKey()));
        if (modelPrefix.lastIndexOf('/') != modelPrefix.length() - 1) {
            modelPrefix += "/";
        }

        this.modelBaseUri = modelPrefix;
        return modelPrefix;
    }

    public String getModelPrefix() {
        return config.getApplicationConfig().getOption(RdfConfigurationOption.RDF_MODEL_PREFIX.getKey()).toLowerCase();
    }

    public String getModelVersion() {
        return config.getApplicationConfig().getOption(RdfConfigurationOption.RDF_MODEL_VERSION.getKey());
    }

    public String getModelTitle() {
        return config.getAppName();
    }

    public String getModelDefaultLanguage() {
        return config.getApplicationConfig().getOption(RdfConfigurationOption.RDF_MODEL_LANGUAGE.getKey());
    }

    public String getModelLabel() {
        return config.getAppName();
    }


    public String getModelDescription() {
        return config.getApplicationConfig().getOption(RdfConfigurationOption.RDF_MODEL_DESCRIPTION.getKey());
    }

    public String getModelComment() {
        return config.getApplicationConfig().getOption(RdfConfigurationOption.RDF_MODEL_COMMENT.getKey());
    }

    public String getModelDate() {
        return config.getApplicationConfig().getOption(RdfConfigurationOption.RDF_MODEL_DATE.getKey());
    }

    public String getModelLicense() {
        return config.getApplicationConfig().getOption(RdfConfigurationOption.RDF_MODEL_LICENSE.getKey());
    }

    public String getModelAuthors() {
        return config.getApplicationConfig().getOption(RdfConfigurationOption.RDF_MODEL_AUTHORS.getKey());
    }

    public String getModelPublisher() {
        return config.getApplicationConfig().getOption(RdfConfigurationOption.RDF_MODEL_PUBLISHER.getKey());
    }

    public int getDefaultPageSize() {
        return config.getApplicationConfig().getOptionAsInt(RdfConfigurationOption.RDF_DEFAULT_PAGE_SIZE.getKey());
    }

    public int getMaxPageSize() {
        return config.getApplicationConfig().getOptionAsInt(RdfConfigurationOption.RDF_MAX_PAGE_SIZE.getKey());
    }


}

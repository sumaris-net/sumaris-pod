package net.sumaris.server.http.graphql;

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
import graphql.GraphQL;
import graphql.execution.SubscriptionExecutionStrategy;
import graphql.schema.GraphQLSchema;
import io.leangen.graphql.GraphQLSchemaGenerator;
import io.leangen.graphql.metadata.strategy.query.AnnotatedResolverBuilder;
import io.leangen.graphql.metadata.strategy.value.jackson.JacksonValueMapperFactory;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.technical.model.IEntity;
import net.sumaris.server.graphql.AggregationGraphQLService;
import net.sumaris.server.graphql.ExtractionGraphQLService;
import net.sumaris.server.http.graphql.administration.AccountGraphQLService;
import net.sumaris.server.http.graphql.administration.AdministrationGraphQLService;
import net.sumaris.server.http.graphql.administration.ProgramGraphQLService;
import net.sumaris.server.http.graphql.administration.StrategyPredocGraphQLService;
import net.sumaris.server.http.graphql.data.DataGraphQLService;
import net.sumaris.server.http.graphql.data.VesselGraphQLService;
import net.sumaris.server.http.graphql.referential.PmfmGraphQLService;
import net.sumaris.server.http.graphql.referential.ReferentialExternalGraphQLService;
import net.sumaris.server.http.graphql.referential.ReferentialGraphQLService;
import net.sumaris.server.http.graphql.referential.TaxonNameGraphQLService;
import net.sumaris.server.http.graphql.security.AuthGraphQLService;
import net.sumaris.server.http.graphql.social.SocialGraphQLService;
import net.sumaris.server.http.graphql.technical.*;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.handler.PerConnectionWebSocketHandler;

import java.io.Serializable;
import java.util.Optional;

@Configuration
@AutoConfigureOrder(3)
@EnableWebSocket
@Slf4j
public class GraphQLConfiguration implements WebSocketConfigurer {

    private final AccountGraphQLService accountGraphQLService;
    private final AdministrationGraphQLService administrationService;
    private final ProgramGraphQLService programService;
    private final StrategyPredocGraphQLService strategyPredocService;
    private final SoftwareGraphQLService softwareService;
    private final ConfigurationGraphQLService configurationService;
    private final SocialGraphQLService socialService;
    private final TrashGraphQLService trashService;
    private final DataGraphQLService dataService;
    private final VesselGraphQLService vesselService;
    private final ReferentialGraphQLService referentialService;
    private final PmfmGraphQLService pmfmService;
    private final ReferentialExternalGraphQLService referentialExternalService;
    private final TaxonNameGraphQLService taxonNameGraphQLService;
    private final AuthGraphQLService authGraphQLService;
    private final ObjectMapper objectMapper;
    private final Optional<ExtractionGraphQLService> extractionGraphQLService;
    private final Optional<AggregationGraphQLService> aggregationGraphQLService;
    private final Optional<CacheGraphQLService> cacheGraphQLService;

    public GraphQLConfiguration(AccountGraphQLService accountGraphQLService, AdministrationGraphQLService administrationService,
                                ProgramGraphQLService programService, StrategyPredocGraphQLService strategyPredocService,
                                SoftwareGraphQLService softwareService, ConfigurationGraphQLService configurationService,
                                SocialGraphQLService socialService, TrashGraphQLService trashService,
                                DataGraphQLService dataService, VesselGraphQLService vesselService,
                                ReferentialGraphQLService referentialService, PmfmGraphQLService pmfmService,
                                ReferentialExternalGraphQLService referentialExternalService, TaxonNameGraphQLService taxonNameGraphQLService,
                                AuthGraphQLService authGraphQLService, ObjectMapper objectMapper,
                                Optional<ExtractionGraphQLService> extractionGraphQLService,
                                Optional<AggregationGraphQLService> aggregationGraphQLService,
                                Optional<CacheGraphQLService> cacheGraphQLService) {
        this.accountGraphQLService = accountGraphQLService;
        this.administrationService = administrationService;
        this.programService = programService;
        this.strategyPredocService = strategyPredocService;
        this.softwareService = softwareService;
        this.configurationService = configurationService;
        this.socialService = socialService;
        this.trashService = trashService;
        this.dataService = dataService;
        this.vesselService = vesselService;
        this.referentialService = referentialService;
        this.pmfmService = pmfmService;
        this.referentialExternalService = referentialExternalService;
        this.taxonNameGraphQLService = taxonNameGraphQLService;
        this.authGraphQLService = authGraphQLService;
        this.objectMapper = objectMapper;
        this.extractionGraphQLService = extractionGraphQLService;
        this.aggregationGraphQLService = aggregationGraphQLService;
        this.cacheGraphQLService = cacheGraphQLService;
    }

    @Bean
    public WebMvcConfigurer configureGraphQL() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                // Enable Global CORS support for the application
                //See https://stackoverflow.com/questions/35315090/spring-boot-enable-global-cors-support-issue-only-get-is-working-post-put-and
                registry.addMapping(GraphQLPaths.BASE_PATH)
                    .allowedOriginPatterns("*")
                    .allowedMethods("GET", "POST", "PUT", "DELETE", "HEAD", "OPTIONS")
                    .allowedHeaders("accept", "access-control-allow-origin", "authorization", "content-type")
                    .allowCredentials(true);
            }
        };
    }


    @Bean
    public GraphQLSchema graphQLSchema() {

        log.info("Generating GraphQL schema (using SPQR)...");

        final GraphQLSchemaGenerator generator = new GraphQLSchemaGenerator()
            .withResolverBuilders(new AnnotatedResolverBuilder())

            // Auth and technical
            .withOperationsFromSingleton(authGraphQLService, AuthGraphQLService.class)
            .withOperationsFromSingleton(accountGraphQLService, AccountGraphQLService.class)
            .withOperationsFromSingleton(softwareService, SoftwareGraphQLService.class)
            .withOperationsFromSingleton(configurationService, ConfigurationGraphQLService.class)
            .withOperationsFromSingleton(trashService, TrashGraphQLService.class)

            // Administration & Referential
            .withOperationsFromSingleton(administrationService, AdministrationGraphQLService.class)
            .withOperationsFromSingleton(programService, ProgramGraphQLService.class)
            .withOperationsFromSingleton(strategyPredocService, StrategyPredocGraphQLService.class)
            .withOperationsFromSingleton(referentialService, ReferentialGraphQLService.class)
            .withOperationsFromSingleton(pmfmService, PmfmGraphQLService.class)
            .withOperationsFromSingleton(taxonNameGraphQLService, TaxonNameGraphQLService.class)
            .withOperationsFromSingleton(referentialExternalService, ReferentialExternalGraphQLService.class)

            // Data
            .withOperationsFromSingleton(dataService, DataGraphQLService.class)
            .withOperationsFromSingleton(vesselService, VesselGraphQLService.class)

            // Social
            .withOperationsFromSingleton(socialService, SocialGraphQLService.class)

            .withTypeTransformer(new DefaultTypeTransformer(false, true)
                // Replace unbounded IEntity<ID> with IEntity<Serializable>
                .addUnboundedReplacement(IEntity.class, Serializable.class))

            .withValueMapperFactory(new JacksonValueMapperFactory.Builder()
                .withPrototype(objectMapper)
                .build());

        // Add optional services
        cacheGraphQLService.ifPresent(service -> generator.withOperationsFromSingleton(service, CacheGraphQLService.class));
        extractionGraphQLService.ifPresent(service -> generator.withOperationsFromSingleton(service, ExtractionGraphQLService.class));
        aggregationGraphQLService.ifPresent(service -> generator.withOperationsFromSingleton(service, AggregationGraphQLService.class));

        return generator.generate();
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry webSocketHandlerRegistry) {

        log.info(String.format("Starting GraphQL websocket endpoint {%s}...", GraphQLPaths.SUBSCRIPTION_PATH));

        webSocketHandlerRegistry
            .addHandler(webSocketHandler(), GraphQLPaths.BASE_PATH)
            .setAllowedOriginPatterns("*")
            .withSockJS();
    }

    @Bean
    public WebSocketHandler webSocketHandler() {
        return new PerConnectionWebSocketHandler(SubscriptionWebSocketHandler.class);
    }

    @Bean
    public GraphQL webSocketGraphQL() {
        return GraphQL.newGraphQL(graphQLSchema())
            .subscriptionExecutionStrategy(new SubscriptionExecutionStrategy())
            .build();
    }
}


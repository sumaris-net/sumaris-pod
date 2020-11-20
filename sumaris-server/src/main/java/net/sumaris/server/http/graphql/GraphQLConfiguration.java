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
import graphql.schema.GraphQLSchema;
import io.leangen.graphql.GraphQLSchemaGenerator;
import io.leangen.graphql.metadata.strategy.query.AnnotatedResolverBuilder;
import io.leangen.graphql.metadata.strategy.value.jackson.JacksonValueMapperFactory;
import net.sumaris.core.dao.technical.model.IEntity;
import net.sumaris.server.http.graphql.administration.AdministrationGraphQLService;
import net.sumaris.server.http.graphql.administration.ProgramGraphQLService;
import net.sumaris.server.http.graphql.data.DataGraphQLService;
import net.sumaris.server.graphql.AggregationGraphQLService;
import net.sumaris.server.graphql.ExtractionGraphQLService;
import net.sumaris.server.http.graphql.referential.PmfmGraphQLService;
import net.sumaris.server.http.graphql.referential.ReferentialGraphQLService;
import net.sumaris.server.http.graphql.security.AuthGraphQLService;
import net.sumaris.server.http.graphql.technical.ConfigurationGraphQLService;
import net.sumaris.server.http.graphql.social.SocialGraphQLService;
import net.sumaris.server.http.graphql.technical.DefaultTypeTransformer;
import net.sumaris.server.http.graphql.technical.TrashGraphQLService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.handler.PerConnectionWebSocketHandler;

import java.io.Serializable;

@Configuration
@EnableWebSocket
public class GraphQLConfiguration implements WebSocketConfigurer {

    private static final Logger log = LoggerFactory.getLogger(GraphQLConfiguration.class);

    @Autowired
    private AdministrationGraphQLService administrationService;

    @Autowired
    private ProgramGraphQLService programService;

    @Autowired
    private ConfigurationGraphQLService configurationService;

    @Autowired
    private SocialGraphQLService socialService;

    @Autowired
    private TrashGraphQLService trashService;

    @Autowired
    private DataGraphQLService dataService;

    @Autowired
    private ReferentialGraphQLService referentialService;

    @Autowired
    private PmfmGraphQLService pmfmService;

    @Autowired(required = false)
    private ExtractionGraphQLService extractionGraphQLService;

    @Autowired(required = false)
    private AggregationGraphQLService aggregationGraphQLService;

    @Autowired
    private AuthGraphQLService authGraphQLService;

    @Autowired
    private ObjectMapper objectMapper;

    @Bean
    public GraphQLSchema graphQLSchema() {

        log.info("Generating GraphQL schema (using SPQR)...");

        GraphQLSchemaGenerator generator = new GraphQLSchemaGenerator()
                .withResolverBuilders(new AnnotatedResolverBuilder())

                // Auth and technical
                .withOperationsFromSingleton(authGraphQLService, AuthGraphQLService.class)
                .withOperationsFromSingleton(configurationService, ConfigurationGraphQLService.class)
                .withOperationsFromSingleton(trashService, TrashGraphQLService.class)

                // Administration & Referential
                .withOperationsFromSingleton(administrationService, AdministrationGraphQLService.class)
                .withOperationsFromSingleton(programService, ProgramGraphQLService.class)
                .withOperationsFromSingleton(referentialService, ReferentialGraphQLService.class)
                .withOperationsFromSingleton(pmfmService, PmfmGraphQLService.class)

                // Data
                .withOperationsFromSingleton(dataService, DataGraphQLService.class)


                // Social
                .withOperationsFromSingleton(socialService, SocialGraphQLService.class)

                .withTypeTransformer(new DefaultTypeTransformer(false, true)
                        // Replace unbounded IEntity<ID> with IEntity<Serializable>
                        .addUnboundedReplacement(IEntity.class, Serializable.class))

                .withValueMapperFactory(new JacksonValueMapperFactory.Builder()
                        .withPrototype(objectMapper)
                        .build());

        // Add optional services
        if (extractionGraphQLService != null) generator.withOperationsFromSingleton(extractionGraphQLService, ExtractionGraphQLService.class);
        if (aggregationGraphQLService != null) generator.withOperationsFromSingleton(aggregationGraphQLService, AggregationGraphQLService.class);

        return generator.generate();
    }

    @Bean
    public GraphQL graphQL() {
        return GraphQL.newGraphQL(graphQLSchema()).build();
    }


    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry webSocketHandlerRegistry) {

        log.info(String.format("Starting GraphQL websocket endpoint {%s}...", GraphQLPaths.SUBSCRIPTION_PATH));

        webSocketHandlerRegistry
                .addHandler(webSocketHandler(), GraphQLPaths.BASE_PATH)
                .setAllowedOrigins("*")
                .withSockJS();
    }

    @Bean
    public WebSocketHandler webSocketHandler() {
        return new PerConnectionWebSocketHandler(SubscriptionWebSocketHandler.class);
    }
}


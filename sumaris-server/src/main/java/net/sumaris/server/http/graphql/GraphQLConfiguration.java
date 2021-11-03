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

package net.sumaris.server.http.graphql;

import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.GraphQL;
import graphql.com.google.common.collect.Lists;
import graphql.execution.SubscriptionExecutionStrategy;
import graphql.schema.GraphQLSchema;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.GraphQLSchemaGenerator;
import io.leangen.graphql.metadata.strategy.query.AnnotatedResolverBuilder;
import io.leangen.graphql.metadata.strategy.value.jackson.JacksonValueMapperFactory;
import io.leangen.graphql.util.Utils;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.technical.model.IEntity;
import net.sumaris.server.http.graphql.technical.DefaultTypeTransformer;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ResolvableType;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.handler.PerConnectionWebSocketHandler;

import java.io.Serializable;
import java.lang.reflect.AnnotatedType;
import java.util.Collection;
import java.util.List;

@Configuration
@ConditionalOnClass(GraphQLSchemaGenerator.class)
@Slf4j
public class GraphQLConfiguration implements WebSocketConfigurer {

    private final ConfigurableApplicationContext context;
    private final ObjectMapper objectMapper;

    public GraphQLConfiguration(ConfigurableApplicationContext context,
                                ObjectMapper objectMapper
    ) {
        this.context = context;
        this.objectMapper = objectMapper;
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
            .withBasePackages("net.sumaris")
            .withResolverBuilders(new AnnotatedResolverBuilder())
            .withTypeTransformer(new DefaultTypeTransformer(false, true)
            // Replace unbounded IEntity<ID> with IEntity<Serializable>
            .addUnboundedReplacement(IEntity.class, Serializable.class))
            .withValueMapperFactory(new JacksonValueMapperFactory.Builder()
                .withPrototype(objectMapper)
                .build());

        findGraphQLConfigurers().forEach(configurer -> configurer.configureSchema(generator));

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

    /* -- private methods -- */

    private Collection<GraphQLConfigurer> findGraphQLConfigurers() {

        List<GraphQLConfigurer> result = Lists.newArrayList();

        // Add configurer beans
        result.addAll(context.getBeansOfType(GraphQLConfigurer.class)
            .values());

        final String[] apiBeanNames = context.getBeanNamesForAnnotation(GraphQLApi.class);
        final ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();

        for (String beanName : apiBeanNames) {
            BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);
            AnnotatedType beanType;
            BeanDefinition current = beanDefinition;
            BeanDefinition originatingBeanDefinition = current;
            while (current != null) {
                originatingBeanDefinition = current;
                current = current.getOriginatingBeanDefinition();
            }
            ResolvableType resolvableType = originatingBeanDefinition.getResolvableType();
            if (resolvableType != ResolvableType.NONE && Utils.isNotEmpty(originatingBeanDefinition.getBeanClassName())
                //Sanity check only -- should never happen
                && !originatingBeanDefinition.getBeanClassName().startsWith("org.springframework.")) {
                beanType = GenericTypeReflector.annotate(resolvableType.getType());
            } else {
                beanType = GenericTypeReflector.annotate(AopUtils.getTargetClass(context.getBean(beanName)));
            }

            result.add(schemaGenerator -> {
                Object bean = context.getBean(beanName, beanType);
                schemaGenerator.withOperationsFromSingleton(bean, beanType);
            });
        }
        return result;
    }
}


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

package net.sumaris.core.jms;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.util.StringUtils;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jms.DefaultJmsListenerContainerFactoryConfigurer;
import org.springframework.boot.autoconfigure.jms.JmsProperties;
import org.springframework.boot.autoconfigure.jms.activemq.ActiveMQConnectionFactoryCustomizer;
import org.springframework.boot.autoconfigure.jms.activemq.ActiveMQProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerContainerFactory;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;

import javax.jms.ConnectionFactory;
import java.util.List;
import java.util.Optional;

@Slf4j
@Configuration(proxyBeanMethods = false)
@EnableJms
@ConditionalOnProperty(name = "spring.jms.enabled", havingValue = "true")
@EnableConfigurationProperties(value = {ActiveMQProperties.class, JmsProperties.class})
public class JmsConfiguration {

    public static final String CONTAINER_FACTORY = "jmsListenerContainerFactory";

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnBean({ConnectionFactory.class})
    public class JmsContainerConfiguration {

        @Bean(CONTAINER_FACTORY)
        public JmsListenerContainerFactory<?> jmsListenerContainerFactory(
                Optional<DefaultJmsListenerContainerFactoryConfigurer> jmsListenerContainerFactoryConfigurer,
                ConnectionFactory jmsConnectionFactory,
                MessageConverter messageConverter
            ) {
            DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
            factory.setConnectionFactory(jmsConnectionFactory);
            factory.setMessageConverter(messageConverter);

            jmsListenerContainerFactoryConfigurer
                    .ifPresent(c -> c.configure(factory, jmsConnectionFactory));
            factory.setErrorHandler(t -> log.error("An error has occurred in the JMS transaction: " + t.getMessage(), t));
            return factory;
        }

        @Bean
        public MessageConverter messageConverter(ObjectMapper jacksonObjectMapper) {
            // Serialize message content to json using TextMessage
            MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
            converter.setObjectMapper(jacksonObjectMapper);
            converter.setTargetType(MessageType.TEXT);
            converter.setTypeIdPropertyName("_type");
            converter.setEncodingPropertyName("_encoding");
            return converter;
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnMissingBean({ConnectionFactory.class})
    public class ActiveMQConfiguration {

        private static ConnectionFactory createJmsConnectionFactory(ActiveMQProperties properties,
                                                                    List<ActiveMQConnectionFactoryCustomizer> customizers) {
            ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(properties.getBrokerUrl());
            connectionFactory.setUserName(properties.getUser());
            connectionFactory.setPassword(properties.getPassword());
            customizers.forEach(c -> c.customize(connectionFactory));
            return connectionFactory;
        }

        @Bean
        public ActiveMQConnectionFactoryCustomizer activeMQConnectionFactoryCustomizer() {
            return (factory) -> {
                // Configure
                factory.setTrustAllPackages(false);
                factory.setTrustedPackages(ImmutableList.of("net.sumaris"));

                String url = factory.getBrokerURL();
                String userName = factory.getUserName();
                if (StringUtils.isNotBlank(userName)) {
                    log.info("Connecting to ActiveMQ broker... {url: '{}', userName: '{}', password: '******'}", url, userName);
                } else {
                    log.info("Connecting to ActiveMQ broker... {url: '{}'}", url);
                }
            };
        }
        @Bean
        @ConditionalOnClass(CachingConnectionFactory.class)
        @ConditionalOnProperty(prefix = "spring.jms.cache", name = "enabled", havingValue = "true", matchIfMissing = true)
        public CachingConnectionFactory cachingConnectionFactory(SumarisConfiguration config,
                                                                 JmsProperties jmsProperties,
                                                                 ActiveMQProperties properties,
                                                                 List<ActiveMQConnectionFactoryCustomizer> customizers) {
            JmsProperties.Cache cacheProperties = jmsProperties.getCache();
            CachingConnectionFactory connectionFactory = new CachingConnectionFactory(
                    createJmsConnectionFactory(properties, customizers));
            connectionFactory.setCacheConsumers(cacheProperties.isConsumers());
            connectionFactory.setCacheProducers(cacheProperties.isProducers());
            connectionFactory.setSessionCacheSize(cacheProperties.getSessionCacheSize());
            return connectionFactory;
        }

        @Bean
        @ConditionalOnProperty(prefix = "spring.jms.cache", name = "enabled", havingValue = "false")
        public ConnectionFactory jmsConnectionFactory(ActiveMQProperties properties,
                                                      List<ActiveMQConnectionFactoryCustomizer> customizers) {
            return createJmsConnectionFactory(properties, customizers);
        }

        @Bean
        @ConditionalOnClass(CachingConnectionFactory.class)
        @ConditionalOnProperty(prefix = "spring.jms.cache", name = "enabled", havingValue = "true", matchIfMissing = true)
        CachingConnectionFactory jmsConnectionFactory(JmsProperties jmsProperties, ActiveMQProperties properties,
                                                          List<ActiveMQConnectionFactoryCustomizer> customizers) {
            JmsProperties.Cache cacheProperties = jmsProperties.getCache();
            CachingConnectionFactory connectionFactory = new CachingConnectionFactory(
                    createJmsConnectionFactory(properties, customizers));
            connectionFactory.setCacheConsumers(cacheProperties.isConsumers());
            connectionFactory.setCacheProducers(cacheProperties.isProducers());
            connectionFactory.setSessionCacheSize(cacheProperties.getSessionCacheSize());
            return connectionFactory;
        }
    }
}

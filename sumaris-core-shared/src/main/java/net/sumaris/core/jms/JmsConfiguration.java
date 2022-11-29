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
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.util.StringUtils;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.ActiveMQPrefetchPolicy;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerContainerFactory;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;

import javax.jms.ConnectionFactory;

@Configuration(proxyBeanMethods = false)
@Slf4j
@EnableJms
@ConditionalOnProperty(name = "spring.jms.enabled", havingValue = "true")
public class JmsConfiguration {

    public static final String CONTAINER_FACTORY = "jmsListenerContainerFactory";

    @Bean
    public JmsTemplate jmsTemplate(CachingConnectionFactory cachingConnectionFactory,
                                   MessageConverter messageConverter) {
        JmsTemplate jmsTemplate = new JmsTemplate(cachingConnectionFactory);
        jmsTemplate.setMessageConverter(messageConverter);
        return jmsTemplate;
    }

    @Bean
    public JmsListenerContainerFactory<?> jmsListenerContainerFactory(
        CachingConnectionFactory cachingConnectionFactory,
        MessageConverter messageConverter,
        TaskExecutor taskExecutor
        ) {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(cachingConnectionFactory);
        factory.setMessageConverter(messageConverter);
        factory.setTaskExecutor(taskExecutor);
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

    @Bean
    public CachingConnectionFactory cachingConnectionFactory(ConnectionFactory connectionFactory) {
        return new CachingConnectionFactory(connectionFactory);
    }

    @Bean
    public ConnectionFactory connectionFactory(SumarisConfiguration config) {
        String url = config.getActiveMQBrokerURL();
        int prefetchLimit = config.getActiveMQPrefetchLimit();

        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(url);

        // Configure prefetch policy
        if (prefetchLimit > 0) {
            ActiveMQPrefetchPolicy prefetchPolicy = new ActiveMQPrefetchPolicy();
            prefetchPolicy.setQueuePrefetch(prefetchLimit);
            prefetchPolicy.setMaximumPendingMessageLimit(prefetchLimit);
            connectionFactory.setPrefetchPolicy(prefetchPolicy);
        }

        // Configure username/password
        String userName = config.getActiveMQBrokerUserName();
        String password = config.getActiveMQBrokerPassword();
        if (StringUtils.isNotBlank(userName)) {
            log.info(String.format("Connecting to ActiveMQ broker... {url: '%s', userName: '%s', password: '******'}...", url, userName));
            connectionFactory.setUserName(userName);
            connectionFactory.setPassword(password);
        }
        else {
            log.info(String.format("Connecting to ActiveMQ broker... {url: '%s'}...", url));
        }

        connectionFactory.setTrustAllPackages(true);

        return connectionFactory;
    }

    /*@Bean
    public BrokerService brokerService(SumarisConfiguration config) throws Exception {
        String url = config.getActiveMQBrokerURL();
        log.info(String.format("Starting ActiveMQ broker... {url: '%s'}...", url));

        String brokerName = URI.create(url).getHost();
        BrokerService brokerService = new BrokerService();
        brokerService.addConnector(url);
        brokerService.setBrokerName(brokerName);
        brokerService.addConnector("tcp://localhost:61616");
        brokerService.setPersistent(true);
        brokerService.start();
        return brokerService;
    }*/
}

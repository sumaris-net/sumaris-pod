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

import com.fasterxml.jackson.databind.ObjectMapper;
import net.sumaris.core.util.StringUtils;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerContainerFactory;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;

import javax.jms.ConnectionFactory;

@Configuration
public class SumarisServerAutoConfiguration {
    /**
     * Logger.
     */
    protected static final Logger log =
            LoggerFactory.getLogger(SumarisServerAutoConfiguration.class);

    @Bean
    public JmsListenerContainerFactory<?> jmsListenerContainerFactory(
            ConnectionFactory connectionFactory,
            TaskExecutor taskExecutor) {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setTaskExecutor(taskExecutor);
        factory.setErrorHandler(t -> log.error("An error has occurred in the JMS transaction: " + t.getMessage(), t));
        factory.setConnectionFactory(connectionFactory);
        return factory;
    }

    @Bean
    public MessageConverter jacksonJmsMessageConverter(ObjectMapper objectMapper) {
        // Serialize message content to json using TextMessage
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setObjectMapper(objectMapper);
        converter.setTargetType(MessageType.TEXT);
        converter.setTypeIdPropertyName("_type");
        return converter;
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "spring.activemq",
            name = {"broker-url"}
    )
    @ConditionalOnClass(name = "org.apache.activemq.store.kahadb.KahaDBPersistenceAdapter")
    public ConnectionFactory connectionFactory(SumarisServerConfiguration config) {
        String url = config.getActiveMQBrokerURL();
        String userName = config.getActiveMQBrokerUserName();
        String password = config.getActiveMQBrokerPassword();

        // Use username/pwd constructor
        if (StringUtils.isNotBlank(userName)) {
            log.info(String.format("Starting JMS broker... {type: 'ActiveMQ', url: '%s', userName: '%s', password: '******'}...", url, userName));
            return new ActiveMQConnectionFactory(userName, password, url);
        }

        // Use URL only constructor
        log.info(String.format("Starting JMS broker... {type: 'ActiveMQ', url: '%s'}...", url));
        return new ActiveMQConnectionFactory(url);
    }
}

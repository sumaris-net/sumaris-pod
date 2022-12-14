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
import net.sumaris.core.util.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jms.DefaultJmsListenerContainerFactoryConfigurer;
import org.springframework.boot.autoconfigure.jms.activemq.ActiveMQConnectionFactoryCustomizer;
import org.springframework.boot.autoconfigure.jms.activemq.ActiveMQProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerContainerFactory;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;

import javax.jms.ConnectionFactory;

@Configuration
@Slf4j
@EnableJms
@ConditionalOnProperty(name = "spring.jms.enabled", havingValue = "true")
public class JmsConfiguration {

    public static final String CONTAINER_FACTORY = "jmsListenerContainerFactory";

    @Bean
    public ActiveMQConnectionFactoryCustomizer activeMQConnectionFactoryCustomizer() {
        return (factory) -> {
            String url = factory.getBrokerURL();
            String userName = factory.getUserName();
            if (StringUtils.isNotBlank(userName)) {
                log.info("Connecting to ActiveMQ broker... {url: '{}', userName: '{}', password: '******'}", url, userName);
            }
            else {
                log.info("Connecting to ActiveMQ broker... {url: '{}'}", url);
            }
        };
    }

    @Bean(CONTAINER_FACTORY)
    public DefaultJmsListenerContainerFactory jmsListenerContainerFactory(
            DefaultJmsListenerContainerFactoryConfigurer configurer,
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter
        ) {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
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

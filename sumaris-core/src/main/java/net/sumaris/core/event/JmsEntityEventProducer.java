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

package net.sumaris.core.event;

import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.event.entity.EntityDeleteEvent;
import net.sumaris.core.event.entity.EntityInsertEvent;
import net.sumaris.core.event.entity.EntityUpdateEvent;
import net.sumaris.core.event.entity.IEntityEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessagePostProcessor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import javax.annotation.PostConstruct;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.Message;
import java.util.Arrays;
import java.util.stream.Collectors;


@Component
@Slf4j
@ConditionalOnClass({JMSContext.class, JmsTemplate.class})
public class JmsEntityEventProducer {

    // WARN: @ConditionOnBean over this class is not working well, that why we use required=false
    @Autowired(required = false)
    private JmsTemplate jmsTemplate;

    @PostConstruct
    protected void init() {
        if (jmsTemplate != null) {
            log.info("Starting JMS entity events producer... {destinationPattern: '({})<EntityName>'}", Arrays.stream(IEntityEvent.EntityEventOperation.values())
                .map(Enum::name)
                .map(String::toLowerCase)
                .collect(Collectors.joining("|"))
            );
        }
        else {
            log.warn("Cannot start JMS entity events producer: missing a bean of class {}", JmsTemplate.class.getName());
        }
    }

    @Async
    @TransactionalEventListener(
            value = {EntityInsertEvent.class, EntityUpdateEvent.class, EntityDeleteEvent.class},
            phase = TransactionPhase.AFTER_COMMIT)
    public void onEntityEvent(IEntityEvent event) {
        if (jmsTemplate == null) return; // Skip

        Preconditions.checkNotNull(event);
        Preconditions.checkNotNull(event.getOperation());
        Preconditions.checkNotNull(event.getEntityName());
        Preconditions.checkNotNull(event.getId());

        // Compute a destination name
        String destinationName = event.getJmsDestinationName();

        log.debug("Sending JMS message... {destination: '{}', id: {}}", destinationName, event.getId());

        MessagePostProcessor postProcessor = message -> postProcessMessage(message, event);

        // Send data, or ID
        if (event.getData() != null) {
            jmsTemplate.convertAndSend(destinationName, event.getData(), postProcessor);
        }
        else {
            jmsTemplate.convertAndSend(destinationName, event.getId(), postProcessor);
        }
        jmsTemplate.convertAndSend(IEntityEvent.JMS_DESTINATION_NAME,
            event,
            postProcessor
        );
    }

    public Message postProcessMessage(final Message message, final IEntityEvent event) throws JMSException {
        message.setStringProperty("operation", event.getOperation().toString().toLowerCase());
        message.setStringProperty("entityName", event.getEntityName());
        message.setStringProperty("id", event.getId().toString());
        return message;
    }
}
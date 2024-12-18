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

import com.google.common.base.Preconditions;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.event.entity.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import javax.annotation.PostConstruct;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.Message;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;


@Component
@Slf4j
@ConditionalOnClass({JMSContext.class, JmsTemplate.class})
public class JmsEntityEventProducer {

    // WARN: @ConditionOnBean over this class is not working well, that why we use required=false
    private final JmsTemplate jmsTemplate;
    private final Optional<EntityEventService> entityEventService;

    @Value("${spring.jms.enabled:false}")
    private boolean jmsEnabled;

    public JmsEntityEventProducer(Optional<JmsTemplate> jmsTemplate,
                                  @Qualifier("entityEventService") Optional<EntityEventService> entityEventService) {
        this.jmsTemplate = jmsTemplate.orElse(null);
        this.entityEventService = entityEventService;
    }

    @PostConstruct
    protected void init() {
        if (jmsTemplate == null) {
            // Warn when enabled but template is missing.
            if (jmsEnabled) {
                log.warn("Cannot start JMS entity events producer: missing a bean of class {}", JmsTemplate.class.getName());
            }
            this.jmsEnabled = false;
            return;
        }

        // Start log
        if (jmsEnabled) {
            log.info("Starting entity events producer... {destination: '{}', operation: '({})'}",
                JmsEntityEvents.DESTINATION,
                Arrays.stream(IEntityEvent.EntityEventOperation.values())
                    .map(Enum::name)
                    .map(String::toLowerCase)
                    .collect(Collectors.joining("|"))
            );
        }
        else {
            log.info("Starting entity events producer... {fallback: true}");
        }
    }

    @Async
    @TransactionalEventListener(
            value = {EntityInsertEvent.class, EntityUpdateEvent.class, EntityDeleteEvent.class},
            phase = TransactionPhase.AFTER_COMMIT)
    public void onEntityEvent(IEntityEvent event) {
        Preconditions.checkNotNull(event);
        Preconditions.checkNotNull(event.getOperation());
        Preconditions.checkNotNull(event.getEntityName());
        Preconditions.checkNotNull(event.getId());

        log.trace("Receiving event {}", event.getClass().getSimpleName());

        if (!jmsEnabled) {
            // Redirect to consumer, as a fallback when JMS is not enabled
            entityEventService.ifPresent(consumer -> consumer.dispatchEvent(event));
            return; // Stop here
        }

        // Send data, or ID
        if (event.getData() != null) {
            jmsTemplate.convertAndSend(
                JmsEntityEvents.DESTINATION,
                event.getData(),
                message -> processMessage(message, event)
            );
        }
        else {
            jmsTemplate.convertAndSend(
                JmsEntityEvents.DESTINATION,
                event.getId(),
                message -> processMessage(message, event)
            );
        }
    }

    private Message processMessage(final Message message, @NonNull final IEntityEvent event) throws JMSException {
        String operation = event.getOperation().toString().toLowerCase();
        if (log.isDebugEnabled()) {
            log.debug("Sending JMS message... {destination: '{}', operation: '{}', entityName: '{}', id: {}}",
                JmsEntityEvents.DESTINATION,
                operation,
                event.getEntityName(),
                event.getId()
            );
        }

        // Add properties to be able to rebuild an event - @see EntityEvents
        message.setStringProperty(IEntityEvent.Fields.OPERATION, operation);
        message.setStringProperty(IEntityEvent.Fields.ENTITY_NAME, event.getEntityName());
        message.setStringProperty(IEntityEvent.Fields.ID, event.getId().toString());

        return message;
    }
}
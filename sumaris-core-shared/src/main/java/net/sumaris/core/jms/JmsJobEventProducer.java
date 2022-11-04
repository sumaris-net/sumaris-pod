package net.sumaris.core.jms;

/*-
 * #%L
 * Quadrige3 Core :: Server API
 * %%
 * Copyright (C) 2017 - 2022 Ifremer
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */


import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.event.job.IJobEvent;
import net.sumaris.core.event.job.JobEndEvent;
import net.sumaris.core.event.job.JobProgressionEvent;
import net.sumaris.core.event.job.JobStartEvent;
import net.sumaris.core.util.Assert;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.event.EventListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

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
public class JmsJobEventProducer {

    public static final String DESTINATION = "job-event";

    private final JmsTemplate jmsTemplate;

    public JmsJobEventProducer(Optional<JmsTemplate> jmsTemplate) {
        this.jmsTemplate = jmsTemplate.orElse(null);
    }

    @PostConstruct
    protected void init() {
        if (jmsTemplate == null) {
            log.warn("Cannot start JMS job events producer: missing a bean of class {}", JmsTemplate.class.getName());
            return;
        }

        log.info("Starting JMS job events producer... {destination: '{}', operation: '({})'}",
            DESTINATION,
            Arrays.stream(IJobEvent.JobEventOperation.values())
                .map(Enum::name)
                .map(String::toLowerCase)
                .collect(Collectors.joining("|"))
        );

    }

    @Async
    @EventListener(value = {JobStartEvent.class, JobEndEvent.class, JobProgressionEvent.class})
    public void onJobEvent(IJobEvent<?> event) {
        if (jmsTemplate == null) return; // Skip

        Assert.notNull(event);
        Assert.notNull(event.getOperation());

        // Send data or id
        if (event.getData() != null) {
            jmsTemplate.convertAndSend(
                DESTINATION,
                event.getData(),
                message -> processMessage(message, event)
            );
        } else {
            jmsTemplate.convertAndSend(
                DESTINATION,
                event.getId(),
                message -> processMessage(message, event)
            );
        }
    }

    private Message processMessage(final Message message, @NonNull final IJobEvent<?> event) throws JMSException {
        String operation = event.getOperation().toString().toLowerCase();
        if (log.isDebugEnabled()) {
            log.debug("Sending JMS message... {destination: '{}', operation: '{}', jobId: {}}",
                DESTINATION,
                operation,
                event.getId()
            );
        }

        // Add properties to be able to rebuild an event - @see JmsJobEvents
        message.setStringProperty(IJobEvent.Fields.OPERATION, operation);
        message.setIntProperty(IJobEvent.Fields.ID, event.getId());
        return message;
    }
}

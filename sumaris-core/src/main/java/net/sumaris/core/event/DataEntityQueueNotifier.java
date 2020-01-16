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

import net.sumaris.core.dao.schema.event.SchemaUpdatedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.event.EventListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class DataEntityQueueNotifier {

    private JmsTemplate jmsTemplate;

    @Autowired
    public DataEntityQueueNotifier(JmsTemplate jmsTemplate) {
        this.jmsTemplate = jmsTemplate;
    }

    @Async
    @EventListener(DataEntityCreatedEvent.class)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDataCreated(DataEntityCreatedEvent event) {
        String destination = DataEntityCreatedEvent.JMS_NAME_PREFIX + event.getEntityName();
        jmsTemplate.convertAndSend(destination, event.getData());
    }

    @Async
    @EventListener(DataEntityUpdatedEvent.class)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDataUpdated(DataEntityUpdatedEvent event) {
        String destination = DataEntityUpdatedEvent.JMS_NAME_PREFIX + event.getEntityName();
        jmsTemplate.convertAndSend(destination, event.getData());
    }

}
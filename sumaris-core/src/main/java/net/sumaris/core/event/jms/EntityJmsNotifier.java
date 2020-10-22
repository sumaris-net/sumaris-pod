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

package net.sumaris.core.event.jms;

import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.event.entity.EntityDeleteEvent;
import net.sumaris.core.event.entity.EntityInsertEvent;
import net.sumaris.core.event.entity.EntityUpdateEvent;
import net.sumaris.core.event.entity.IEntityEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

@Component
@ConditionalOnBean(JmsTemplate.class)
@Slf4j
public class EntityJmsNotifier {

    @Resource
    private JmsTemplate jmsTemplate;

    @PostConstruct
    protected void init() {
        log.info("Starting JMS notifier, for entity events...");
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

        // Compute a destination name
        String destinationName = event.getJmsDestinationName();

        if (log.isDebugEnabled()) log.debug(String.format("Sending JMS message... {destination: '%s', id: %s}",
                destinationName, event.getId()));

        // Send data, or ID
        if (event.getData() != null) {
            jmsTemplate.convertAndSend(destinationName, event.getData());
        }
        else {
            jmsTemplate.convertAndSend(destinationName, event.getId());
        }
    }

}
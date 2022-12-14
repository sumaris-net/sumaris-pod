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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.event.entity.EntityDeleteEvent;
import net.sumaris.core.event.entity.EntityUpdateEvent;
import net.sumaris.core.event.entity.IEntityEvent;
import net.sumaris.core.model.IEntity;
import net.sumaris.core.model.IUpdateDateEntity;
import net.sumaris.core.model.IValueObject;
import net.sumaris.core.event.entity.EntityEventService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.Message;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component("jmsEntityEventService")
@ConditionalOnProperty(value = "spring.jms.enabled", havingValue = "true")
@Slf4j
public class JmsEntityEventConsumer implements EntityEventService {

    private final Map<String, List<Listener>> listenersById = Maps.newConcurrentMap();

    @Override
    public Disposable registerListener(Listener listener, Class<? extends IEntity<?>>... entityClasses) {
        List<String> keys = Arrays.stream(entityClasses).map(Class::getSimpleName).toList();
        keys.forEach(classKey -> registerListener(classKey, listener));
        return () -> unregisterListener(listener, entityClasses);
    }

    @Override
    public Disposable registerListener(Listener listener, Class<? extends IEntity<?>> entityClass, Serializable id) {
        String listenerId = computeListenerId(entityClass, id);
        registerListener(listenerId, listener);
        return () -> unregisterListener(listener, listenerId);
    }

    @Override
    public void unregisterListener(Listener listener, Class<? extends IEntity<?>>... entityClasses) {
        List<String> keys = Arrays.stream(entityClasses).map(Class::getSimpleName).toList();
        keys.forEach(classKey -> unregisterListener(listener, classKey));
    }

    /* -- protected functions -- */

    @JmsListener(destination = JmsEntityEvents.DESTINATION, selector = "operation = 'update'")
    protected void onEntityUpdateEvent(IValueObject data, Message message) {
        EntityUpdateEvent event = JmsEntityEvents.parse(EntityUpdateEvent.class, message, data);
        // Get listener for this event
        List<Listener> listeners = getListenersByEvent(event);

        // Emit event
        if (CollectionUtils.isNotEmpty(listeners)) {
            log.debug("Receiving update on {}#{} (listener count: {}}", event.getEntityName(), event.getId(), listeners.size());
            listeners.forEach(c -> c.onUpdate(event));
        }
    }

    @JmsListener(destination = JmsEntityEvents.DESTINATION, selector = "operation = 'delete'")
    protected void onEntityDeleteEvent(IValueObject data, Message message) {
        EntityDeleteEvent event = JmsEntityEvents.parse(EntityDeleteEvent.class, message, data);
        // Get listener for this event
        List<Listener> listeners = getListenersByEvent(event);
        if (CollectionUtils.isNotEmpty(listeners)) {
            log.debug("Receiving delete {}#{} (listener count: {}}", event.getEntityName(), event.getId(), listeners.size());
            listeners.forEach(c -> c.onDelete(event));
        }
    }

    protected String computeListenerId(@NonNull Class<?> entityClass, @NonNull Serializable id) {
        return computeListenerId(entityClass.getSimpleName(), id);
    }

    protected String computeListenerId(@NonNull Class<?> entityClass) {
        return computeListenerId(entityClass.getSimpleName(), null);
    }

    protected String computeListenerId(@NonNull String entityName) {
        return computeListenerId(entityName, null);
    }

    protected String computeListenerId(@NonNull String entityName, Serializable id) {
        return id == null ? entityName : entityName + "#" + id;
    }

    protected <V extends IUpdateDateEntity<?, ?>> void registerListener(String key, Listener listener) {
        synchronized (listenersById) {
            List<Listener> listeners = listenersById.computeIfAbsent(key, k -> Lists.newCopyOnWriteArrayList());

            //log.debug("Listening updates on {} (listener count: {})", key, listeners.size() + 1);

            synchronized (listeners) {
                listeners.add(listener);
            }
        }
    }

    protected void unregisterListener(Listener listener, String key) {
        synchronized (this.listenersById) {
            List<Listener> listeners = this.listenersById.get(key);
            if (listeners == null) return;

            //log.debug("Stop listening updates on {} (listener count: {})", key, listeners.size() - 1);

            synchronized (listeners) {
                listeners.remove(listener);
            }
        }
    }

    protected List<Listener> getListenersByEvent(@NonNull IEntityEvent event) {

        // Get listeners on this entity
        String entityKey = computeListenerId(event.getEntityName(), event.getId());
        List<Listener> listeners = listenersById.get(entityKey);

        // Add listeners on class
        String classKey = computeListenerId(event.getEntityName());
        if (listenersById.containsKey(classKey)) {
            if (listeners == null) {
                listeners = listenersById.get(classKey);
            }
            else {
                listeners = ImmutableList.<Listener>builder()
                    .addAll(listeners)
                    .addAll(listenersById.get(classKey))
                    .build();
            }
        }

        return listeners;
    }

}

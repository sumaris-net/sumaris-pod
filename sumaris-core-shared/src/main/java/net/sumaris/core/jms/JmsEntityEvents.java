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
import net.sumaris.core.event.entity.EntityDeleteEvent;
import net.sumaris.core.event.entity.EntityInsertEvent;
import net.sumaris.core.event.entity.EntityUpdateEvent;
import net.sumaris.core.event.entity.IEntityEvent;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.util.Beans;

import javax.annotation.Nullable;
import javax.jms.JMSException;
import javax.jms.Message;
import java.io.Serializable;

@Slf4j
public abstract class JmsEntityEvents {

    public static final String DESTINATION = "entity-event";

    protected JmsEntityEvents() {
        // Helper class
    }

    public static <ID extends Serializable, V extends Serializable> IEntityEvent<ID, V> parse(final Message message) {
        return parse(message, null);
    }

    public static <ID extends Serializable, V extends Serializable> IEntityEvent<ID, V> parse(
        final Message message,
        V data) {

        try {
            String operation = message.getStringProperty(IEntityEvent.Fields.OPERATION);
            IEntityEvent<ID, V> event = createEvent(operation, null);
            return fillEvent(event, message, data);
        } catch (JMSException | IllegalArgumentException e) {
            throw new SumarisTechnicalException(e);
        }
    }

    public static <
        ID extends Serializable,
        V extends Serializable,
        E extends IEntityEvent<ID, V>>
    E parse(
        final Class<E> eventClass,
        final Message message,
        Object data) {

        try {
            String operation = message.getStringProperty(IEntityEvent.Fields.OPERATION);
            E event = createEvent(operation, eventClass);
            return fillEvent(event, message, data);
        }
        catch (JMSException e) {
            throw new SumarisTechnicalException(e);
        }
    }

    private static <
        ID extends Serializable,
        V extends Serializable,
        E extends IEntityEvent<ID, V>>
    E fillEvent(
        final E event,
        final Message message,
        Object data) {

        try {
            String entityName = message.getStringProperty(IEntityEvent.Fields.ENTITY_NAME);
            event.setEntityName(entityName);

            ID id = (ID) message.getObjectProperty(IEntityEvent.Fields.ID);
            event.setId(id);

            event.setData((V)data);

            return event;
        }
        catch (JMSException e) {
            throw new SumarisTechnicalException(e);
        }
    }

    private static <ID extends Serializable, V extends Serializable, E extends IEntityEvent<ID, V>> E createEvent(
        @NonNull String operation,
        @Nullable Class<E> eventClass
    ) {
        try {
            IEntityEvent.EntityEventOperation operationEnum = parseOperation(operation);

            // Use given class, if exists
            if (eventClass != null) {
                E event = Beans.newInstance(eventClass);
                Preconditions.checkArgument(event.getOperation() == operationEnum);
                return event;
            }

            switch (operationEnum) {
                case INSERT:
                    return (E) new EntityInsertEvent();
                case UPDATE:
                    return (E) new EntityUpdateEvent();
                case DELETE:
                    return (E) new EntityDeleteEvent();
                default:
                    throw new IllegalArgumentException("Invalid message - unknown operation: " + operation);
            }
        } catch (Exception e) {
            throw new SumarisTechnicalException(e);
        }
    }

    public static IEntityEvent.EntityEventOperation parseOperation(@NonNull String operation) {
        return IEntityEvent.EntityEventOperation.valueOf(operation.toUpperCase());
    }
}

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

package net.sumaris.core.event.entity;

import net.sumaris.core.model.IEntity;
import org.springframework.lang.NonNull;

import java.io.Serializable;

public interface EntityEventService {

    interface Listener {
        default void onUpdate(EntityUpdateEvent event) {};
        default void onInsert(EntityInsertEvent event) {}
        default void onDelete(EntityDeleteEvent event) {}
    }

    @FunctionalInterface
    interface Disposable {
        void dispose();
    }

    Disposable registerListener(Listener listener, Class<? extends IEntity<?>>... entityClasses);

    Disposable registerListener(Listener listener, Class<? extends IEntity<?>> entityClass, Serializable id);

    void unregisterListener(Listener listener, Class<? extends IEntity<?>>... entityClasses);

    void dispatchEvent(@NonNull IEntityEvent event);
}

package net.sumaris.server.service.technical;

/*-
 * #%L
 * SUMARiS:: Server
 * %%
 * Copyright (C) 2018 SUMARiS Consortium
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

import io.reactivex.rxjava3.core.Observable;
import net.sumaris.core.model.IEntity;
import net.sumaris.core.model.IUpdateDateEntity;
import net.sumaris.core.event.entity.IEntityEvent;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Function;


public interface EntityEventService {

    <ID extends Serializable, D extends Date, T extends IUpdateDateEntity<ID, D>, V extends IUpdateDateEntity<ID, D>> Observable<V>
    watchEntity(Class<T> entityClass,
                Class<V> targetClass,
                ID id,
                @Nullable Integer intervalInSeconds,
                boolean startWithActualValue);

    <ID extends Serializable, D extends Date, V extends IUpdateDateEntity<ID, D>> Observable<V>
    watchEntity(Function<D, Optional<V>> getter,
                int intervalInSeconds,
                boolean startWithActualValue);

    <ID extends Serializable, D extends Date,
        T extends IUpdateDateEntity<ID, D>,
        V extends IUpdateDateEntity<ID, D>,
        L extends Collection<V>> Observable<L>
    watchEntities(Class<T> entityClass,
                  Callable<Optional<L>> loader,
                  @Nullable Integer intervalInSeconds,
                  boolean startWithActualValue);

    <ID extends Serializable, D extends Date, V extends IUpdateDateEntity<ID, D>, L extends Collection<V>> Observable<L>
    watchEntities(Function<D, Optional<L>> loader,
                  int intervalInSeconds,
                  boolean startWithActualValue);

    <L extends Collection<?>> Observable<L>
    watchCollection(Callable<Optional<L>> loader,
                    int intervalInSeconds,
                    boolean startWithActualValue);

    <ID extends Serializable, D extends Date,
        T extends IUpdateDateEntity<ID, D>,
        V extends IUpdateDateEntity<ID, D>,
        L extends Collection<V>> Observable<Long>
    watchEntitiesCount(Class<T> entityClass,
                       Callable<Optional<L>> loader,
                       @Nullable Integer intervalInSeconds,
                       boolean startWithActualValue);

    <ID extends Serializable, T extends IEntity<ID>>
    Observable<IEntityEvent> watchEntityEvents(Class<T> entityClass);

    <O> Observable<O>  watchByLoader(Callable<Optional<O>> loader,
                                     int intervalInSeconds,
                                     boolean startWithActualValue);
}

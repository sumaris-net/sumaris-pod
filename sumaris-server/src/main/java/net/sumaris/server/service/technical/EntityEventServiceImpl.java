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

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.config.JmsConfiguration;
import net.sumaris.core.dao.technical.cache.CacheManager;
import net.sumaris.core.dao.technical.model.Entities;
import net.sumaris.core.dao.technical.model.IEntity;
import net.sumaris.core.dao.technical.model.IUpdateDateEntityBean;
import net.sumaris.core.event.entity.EntityDeleteEvent;
import net.sumaris.core.event.entity.EntityInsertEvent;
import net.sumaris.core.event.entity.EntityUpdateEvent;
import net.sumaris.core.event.entity.IEntityEvent;
import net.sumaris.core.exception.DataNotFoundException;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.util.reactive.Observables;
import net.sumaris.server.dao.technical.EntityDao;
import org.apache.commons.collections4.CollectionUtils;
import org.nuiton.i18n.I18n;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

@Service("entityEventService")
@Slf4j
public class EntityEventServiceImpl implements EntityEventService {

    interface Listener {
        void onUpdate(EntityUpdateEvent event);
        default void onInsert(EntityInsertEvent event) {}
        default void onDelete(EntityDeleteEvent event) {}
    }

    @Value("${sumaris.entity.watch.minIntervalInSeconds:10}")
    private int minIntervalInSeconds;

    @Autowired(required = false)
    protected TaskExecutor taskExecutor;

    @Autowired
    private EntityDao dataChangeDao;

    @Autowired
    private ConversionService conversionService;

    @Autowired
    private CacheManager cacheManager;

    private final AtomicLong timerObserverCount = new AtomicLong(0);
    private final Map<String, List<Listener>> listenersById = Maps.newConcurrentMap();

    @Override
    public <K extends Serializable, D extends Date, T extends IUpdateDateEntityBean<K, D>, V extends IUpdateDateEntityBean<K, D>> Observable<V>
    watchEntity(@NonNull final Class<T> entityClass,
                @NonNull final Class<V> targetClass,
                @NonNull final K id,
                Integer intervalInSeconds,
                boolean startWithActualValue) {

        final AtomicReference<D> lastUpdateDate = new AtomicReference<>();

        // Watch entity, using update events
        Observable<V> result = watchEntityByUpdateEvent(entityClass, targetClass, id);

        // Add watch at interval
        if (intervalInSeconds != null && intervalInSeconds > 0) {
            Observable<V> timer = watchEntityAtInterval(
                entityClass, targetClass, id, lastUpdateDate,
                intervalInSeconds);
            result = Observable.merge(result, timer);
        }

        // Keep only more recent
        result = Observables.latest(result, lastUpdateDate);

        // Starting with the actual value
        if (startWithActualValue) {
            V initialVO = findAndConvert(entityClass, targetClass, id);
            lastUpdateDate.set(initialVO.getUpdateDate());
            result = result.startWith(initialVO);
        }

        String listenerId = computeListenerId(entityClass, id);

        return result
            .doOnLifecycle(
                (subscription) -> log.info("Watching updates {} every {}s (observer count: {})", listenerId, intervalInSeconds, timerObserverCount.get() + 1),
                () -> log.info("Stop watching updates {}. (observer count: {})", listenerId, timerObserverCount.get())
            );

    }

    @Override
    public <K extends Serializable, D extends Date, V extends IUpdateDateEntityBean<K, D>> Observable<V>
    watchEntity(final Function<D, Optional<V>> getter,
                int intervalInSeconds,
                boolean startWithActualValue) {

        Preconditions.checkArgument(intervalInSeconds >= 10, "Minimum interval is 10 seconds");

        AtomicReference<D> lastUpdateDate = new AtomicReference<>();

        Observable<V> timer = watchAtInterval(
            () -> getter.apply(lastUpdateDate.get()),
            intervalInSeconds);

        Observable<V> result = Observables.latest(timer, lastUpdateDate);

        // Starting with the actual value
        if (startWithActualValue) {
            V initialVO = getter.apply(null).orElseThrow(() -> new DataNotFoundException("Unable to get actual value: data not found"));
            lastUpdateDate.set(initialVO.getUpdateDate());
            result = result.startWith(initialVO);
        }

        return result.doOnLifecycle(
            (subscription) -> timerObserverCount.incrementAndGet(),
            timerObserverCount::decrementAndGet
        );
    }

    @Override
    public <ID extends Serializable,
        D extends Date,
        T extends IUpdateDateEntityBean<ID, D>,
        V extends IUpdateDateEntityBean<ID, D>,
        L extends Collection<V>>
    Observable<L> watchEntities(Class<T> entityClass,
                                Callable<Optional<L>> loader,
                                Integer intervalInSeconds,
                                boolean startWithActualValue) {
        AtomicReference<Integer> hashCode = new AtomicReference<>();

        // Watch entity events
        Observable<L> result = watchEntityEvents(entityClass)
            .map(event -> loader.call())
            .filter(Optional::isPresent)
            .map(Optional::get);

        // Add timer
        if (intervalInSeconds != null && intervalInSeconds > 0) {
            result = Observable.merge(result,
                watchCollection(loader, intervalInSeconds, false));
        }

        // Distinguish changed (by hash code)
        result = Observables.distinctUntilChanged(result, hashCode);

        if (startWithActualValue) {
            try {
                L initialVOs = loader.call().orElse(null);
                if (initialVOs != null) {
                    hashCode.set(initialVOs.hashCode());
                    result = result.startWith(initialVOs);
                }
            } catch (Exception e) {
                throw new SumarisTechnicalException(e);
            }
        }

        return result;
    }

    public <K extends Serializable, D extends Date, V extends IUpdateDateEntityBean<K, D>, L extends Collection<V>> Observable<L>
    watchEntities(final Function<D, Optional<L>> loader,
                  int intervalInSeconds,
                  boolean startWithActualValue) {

        Preconditions.checkArgument(intervalInSeconds >= minIntervalInSeconds, "Minimum interval is 10 seconds");

        final AtomicReference<D> lastUpdateDate = new AtomicReference<>();
        final AtomicReference<Integer> lastHashCode = new AtomicReference<>();

        // Try to find a newer entities
        Observable<L> result = watchAtInterval(
            () -> loader.apply(lastUpdateDate.get())
                .flatMap(list -> {
                    D newMaxUpdateDate = Entities.maxUpdateDate(list);
                    // Empty list, but optional is present: return the original optional
                    if (newMaxUpdateDate == null) {
                        lastUpdateDate.set(null);
                        lastHashCode.set(null);
                        return Optional.of(list); // OK
                    }

                    // max(updateDate) changed
                    if (lastUpdateDate.get() == null
                        || lastUpdateDate.get().before(newMaxUpdateDate)) {
                        lastUpdateDate.set(newMaxUpdateDate);
                        return Optional.of(list); // OK
                    }

                    // Hash code changed (e.g. when item removed, updateDate not always changed)
                    int newHash = list.hashCode();
                    if (lastHashCode.get() == null || lastHashCode.get() != newHash) {
                        lastHashCode.set(newHash);
                        return Optional.of(list); // OK
                    }

                    return Optional.empty(); // Skip
                }), intervalInSeconds);

        // Sending the initial value when starting
        if (startWithActualValue) {
            // Convert the entity into VO
            Optional<L> entities = loader.apply(null);
            if (entities.isPresent()) {
                D newUpdateDate = Entities.maxUpdateDate(entities.get());
                lastUpdateDate.set(newUpdateDate);
                result = result.startWith(entities.get());
            }
        }

        return result
            .doOnLifecycle(
                (subscription) -> timerObserverCount.incrementAndGet(),
                timerObserverCount::decrementAndGet
            );
    }


    public <L extends Collection<?>> Observable<L> watchCollection(final Callable<Optional<L>> loader,
                                                                   int intervalInSeconds,
                                                                   boolean startWithActualValue) {

        Preconditions.checkArgument(intervalInSeconds >= minIntervalInSeconds, "Minimum interval is 10 seconds");

        final AtomicReference<Integer> lastHashCode = new AtomicReference();

        Observable<L> result = Observables.distinctUntilChanged(
            watchAtInterval(loader, intervalInSeconds),
            lastHashCode
        );

        // Sending the initial values when starting
        if (startWithActualValue) {
            try {
                L initialValue = loader.call().orElseThrow(() -> new DataNotFoundException("Unable to get actual values: data not found"));
                int newHashCode = initialValue.hashCode();
                lastHashCode.set(newHashCode);
                result = result.startWith(initialValue);
            }
            catch(Exception e) {
                throw new SumarisTechnicalException(e);
            }
        }

        return result.doOnLifecycle(
            (subscription) -> timerObserverCount.incrementAndGet(),
            timerObserverCount::decrementAndGet
        );
    }

    public <ID extends Serializable, T extends IEntity<ID>> Observable<IEntityEvent> watchEntityEvents(
        @NonNull Class<T> entityClass
    ) {
        final String listenerId = computeListenerId(entityClass);

        return Observable.create(emitter -> {
            Listener listener = new Listener() {
                @Override
                public void onUpdate(EntityUpdateEvent event) {
                    emitter.onNext(event);
                }

                @Override
                public void onInsert(EntityInsertEvent event) {
                    emitter.onNext(event);
                }

                @Override
                public void onDelete(EntityDeleteEvent event) {
                    emitter.onNext(event);
                }
            };
            registerListener(listenerId, listener);
            emitter.setCancellable(() -> unregisterListener(listenerId, listener));
        });
    }

    /* -- Listeners management -- */

    @JmsListener(destination = IEntityEvent.JMS_DESTINATION_NAME,
        selector = "operation = 'update'",
        containerFactory = JmsConfiguration.CONTAINER_FACTORY_NAME)
    protected void onEntityUpdateEvent(EntityUpdateEvent event) {
        // Get listener for this event
        List<Listener> listeners = getListenersByEvent(event);

        // Emit event
        if (CollectionUtils.isNotEmpty(listeners)) {
            log.debug("Receiving update on {}#{} (listener count: {}}", event.getEntityName(), event.getId(), listeners.size());
            listeners.forEach(c -> c.onUpdate(event));
        }
    }

    @JmsListener(destination = IEntityEvent.JMS_DESTINATION_NAME,
        selector = "operation = 'delete'",
        containerFactory = JmsConfiguration.CONTAINER_FACTORY_NAME)
    protected void onEntityDeleteEvent(EntityDeleteEvent event) {
        // Get listener for this event
        List<Listener> listeners = getListenersByEvent(event);
        if (CollectionUtils.isNotEmpty(listeners)) {
            log.debug("Receiving delete {}#{} (listener count: {}}", event.getEntityName(), event.getId(), listeners.size());
            listeners.forEach(c -> c.onDelete(event));
        }
    }

    /* -- protected functions -- */

    protected <ID extends Serializable,
        D extends Date,
        T extends IUpdateDateEntityBean<ID, D>,
        V extends IUpdateDateEntityBean<ID, D>>
    Observable<V> watchEntityByUpdateEvent(Class<T> entityClass, Class<V> targetClass, ID id) {

        final String listenerId = computeListenerId(entityClass, id);

        return Observable.create(emitter -> {
            Listener listener = (source) -> new Listener() {
                @Override
                public void onUpdate(EntityUpdateEvent event) {
                    // Already converted into expected type: use source as target
                    if (source != null && source.getClass() == targetClass) {
                        emitter.onNext((V)source);
                    }
                    // Fetch entity, and transform to target class
                    else {
                        V target = findAndConvert(entityClass, targetClass, id);
                        emitter.onNext((V)target);
                    }
                }

                @Override
                public void onDelete(EntityDeleteEvent event) {
                    // Close the observable, as entity has been removed
                    log.debug("Closing observable on {}#{}", event.getEntityName(), event.getId());
                    emitter.onComplete();
                }
            };
            registerListener(listenerId, listener);
            emitter.setCancellable(() -> unregisterListener(listenerId, listener));
        });
    }

    protected <K extends Serializable,
        D extends Date,
        T extends IUpdateDateEntityBean<K, D>,
        V extends IUpdateDateEntityBean<K, D>>
    Observable<V> watchEntityAtInterval(final Class<T> entityClass,
                                        final Class<V> targetClass,
                                        final K id,
                                        final AtomicReference<D> lastUpdateDate,
                                        int intervalInSecond) {
        String key = computeCacheKey(entityClass, targetClass, id);
        int cacheDuration = Math.max(minIntervalInSeconds, Math.round(intervalInSecond / 2));
        return watchAtInterval(
            cacheManager.cacheable(
                () -> findNewerById(entityClass, targetClass, id, lastUpdateDate.get()),
                key, cacheDuration
            ),
            intervalInSecond);
    }

    protected <V> Observable<V> watchAtInterval(@NonNull final Callable<Optional<V>> getter, int intervalInSecond) {

        Preconditions.checkArgument(intervalInSecond >= minIntervalInSeconds, "Invalid interval: " + intervalInSecond);

        return Observable
            .interval(intervalInSecond, TimeUnit.SECONDS)
            .observeOn(taskExecutor == null ? Schedulers.io() : Schedulers.from(taskExecutor))
            .map(n -> getter.call())
            .filter(Optional::isPresent)
            .map(Optional::get);
    }

    protected String computeCacheKey(@NonNull Class<?> entityClass,
                                     @NonNull Class<?> targetClass,
                                     @NonNull Serializable id) {
        return Joiner.on('|').join(
            computeListenerId(entityClass, id),
            targetClass.getSimpleName()
        );
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

    protected <V extends IUpdateDateEntityBean<?, ?>> void registerListener(String key, Listener listener) {
        synchronized (listenersById) {
            List<Listener> listeners = listenersById.computeIfAbsent(key, k -> Lists.newCopyOnWriteArrayList());

            log.debug("Listening updates {} (listener count: {})", key, listeners.size() + 1);

            synchronized (listeners) {
                listeners.add(listener);
            }
        }
    }

    protected void unregisterListener(String key, Listener listener) {
        synchronized (this.listenersById) {
            List<Listener> listeners = this.listenersById.get(key);
            if (listeners == null) return;

            log.debug("Stop listening updates {} (listener count: {})", key, listeners.size() - 1);

            synchronized (listeners) {
                listeners.remove(listener);
            }
        }
    }

    protected List<Listener> getListenersByEvent(@NonNull IEntityEvent event) {
        // Get listeners on this entity
        String entityListenerId = computeListenerId(event.getEntityName(), event.getId());
        List<Listener> listeners = listenersById.get(entityListenerId);

        // Add listeners on all entities
        String entitiesListenerId = computeListenerId(event.getEntityName());
        if (listenersById.containsKey(entitiesListenerId)) {
            if (listeners == null) {
                listeners = listenersById.get(entitiesListenerId);
            }
            else {
                listeners.addAll(listenersById.get(entitiesListenerId));
            }
        }
        return listeners;
    }

    @Transactional(readOnly = true)
    protected <K extends Serializable,
        D extends Date,
        T extends IUpdateDateEntityBean<K, D>,
        V extends IUpdateDateEntityBean<K, D>>
    Optional<V> findNewerById(Class<T> entityClass,
                              Class<V> targetClass,
                              K id,
                              Date lastUpdateDate) {

        log.debug("Checking update on {}#{}...", entityClass.getSimpleName(), id);
        T entity = dataChangeDao.find(entityClass, id);
        // Entity has been deleted
        if (entity == null) {
            return Optional.empty();
        }

        if (lastUpdateDate == null
            // Entity is newer than last update date
            || entity.getUpdateDate() != null && entity.getUpdateDate().after(lastUpdateDate)) {

            // Check can convert
            if (!conversionService.canConvert(entityClass, targetClass)) {
                throw new SumarisTechnicalException(I18n.t("sumaris.error.missingConverter", entityClass.getSimpleName()));
            }

            // Apply conversion
            V target = conversionService.convert(entity, targetClass);
            return Optional.of(target);
        }
        return Optional.empty();
    }

    @Transactional(readOnly = true)
    protected <K extends Serializable, D extends Date,
        T extends IUpdateDateEntityBean<K, D>,
        V extends IUpdateDateEntityBean<K, D>
        > V findAndConvert(
        Class<T> entityClass,
        Class<V> targetClass,
        K id) {

        T entity = dataChangeDao.find(entityClass, id);

        // Entity has been deleted
        if (entity == null) {
            throw new DataNotFoundException(I18n.t("sumaris.error.notFound", entityClass.getSimpleName(), id));
        }

        if (entityClass == targetClass) {
            return (V)entity;
        }

        if (!conversionService.canConvert(entityClass, targetClass)) {
            throw new SumarisTechnicalException(I18n.t("sumaris.error.missingConverter", entityClass.getSimpleName()));
        }
        return conversionService.convert(entity, targetClass);
    }

    @Transactional(readOnly = true)
    protected <K extends Serializable, D extends Date, T extends IUpdateDateEntityBean<K, D>> T find(Class<T> entityClass, K id) {
        return dataChangeDao.find(entityClass, id);
    }

}

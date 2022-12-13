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
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.jms.JmsConfiguration;
import net.sumaris.core.dao.technical.cache.CacheManager;
import net.sumaris.core.model.Entities;
import net.sumaris.core.model.IEntity;
import net.sumaris.core.model.IUpdateDateEntity;
import net.sumaris.core.model.IValueObject;
import net.sumaris.core.jms.JmsEntityEvents;
import net.sumaris.core.event.entity.EntityDeleteEvent;
import net.sumaris.core.event.entity.EntityInsertEvent;
import net.sumaris.core.event.entity.EntityUpdateEvent;
import net.sumaris.core.event.entity.IEntityEvent;
import net.sumaris.core.exception.DataNotFoundException;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.util.reactive.Observables;
import net.sumaris.server.dao.technical.EntityDao;
import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.Nullable;
import org.nuiton.i18n.I18n;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.jms.Message;
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


    protected final Optional<TaskExecutor> taskExecutor;

    private final EntityDao entityDao;

    private final ConversionService conversionService;

    private final CacheManager cacheManager;

    private final AtomicLong timerObserverCount = new AtomicLong(0);
    private final Map<String, List<Listener>> listenersById = Maps.newConcurrentMap();


    public EntityEventServiceImpl(Optional<TaskExecutor> taskExecutor, EntityDao entityDao, ConversionService conversionService, CacheManager cacheManager) {
        this.taskExecutor = taskExecutor;
        this.entityDao = entityDao;
        this.conversionService = conversionService;
        this.cacheManager = cacheManager;
    }

    @Override
    public <K extends Serializable, D extends Date, T extends IUpdateDateEntity<K, D>, V extends IUpdateDateEntity<K, D>> Observable<V>
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
            V initialVO = findAndConvert(entityClass, targetClass, id)
                .orElseThrow(() -> new DataNotFoundException("Unable to get actual value: data not found"));
            lastUpdateDate.set(initialVO.getUpdateDate());
            result = result.startWithItem(initialVO);
        }

        String listenerId = computeListenerId(entityClass, id);

        return result
            .doOnLifecycle(
                (subscription) -> log.info("Watching {} every {}s ({} observers)", listenerId, intervalInSeconds, timerObserverCount.get() + 1),
                () -> log.info("Stop watching {} ({} observers)", listenerId, timerObserverCount.get())
            );

    }

    @Override
    public <K extends Serializable, D extends Date, V extends IUpdateDateEntity<K, D>> Observable<V>
    watchEntity(final Function<D, Optional<V>> getter,
                int intervalInSeconds,
                boolean startWithActualValue) {

        checkInterval(intervalInSeconds);

        AtomicReference<D> lastUpdateDate = new AtomicReference<>();

        Observable<V> timer = watchAtInterval(
            () -> getter.apply(lastUpdateDate.get()),
            intervalInSeconds);

        Observable<V> result = Observables.latest(timer, lastUpdateDate);

        // Starting with the actual value
        if (startWithActualValue) {
            V initialVO = getter.apply(null).orElseThrow(() -> new DataNotFoundException("Unable to get actual value: data not found"));
            lastUpdateDate.set(initialVO.getUpdateDate());
            result = result.startWithItem(initialVO);
        }

        return result.doOnLifecycle(
            (subscription) -> timerObserverCount.incrementAndGet(),
            timerObserverCount::decrementAndGet
        );
    }

    @Override
    public <ID extends Serializable,
        D extends Date,
        T extends IUpdateDateEntity<ID, D>,
        V extends IUpdateDateEntity<ID, D>,
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
                watchAtInterval(loader, intervalInSeconds));
        }

        // Distinguish changed (by hash code)
        result = Observables.distinctUntilChanged(result, hashCode);

        if (startWithActualValue) {
            try {
                L initialVOs = loader.call().orElse(null);
                if (initialVOs != null) {
                    hashCode.set(initialVOs.hashCode());
                    result = result.startWithItem(initialVOs);
                }
            } catch (Exception e) {
                throw new SumarisTechnicalException(e);
            }
        }

        return result;
    }

    @Override
    public <O> Observable<O>  watchByLoader(Callable<Optional<O>> loader,
                                            int intervalInSeconds,
                                            boolean startWithActualValue) {
        AtomicReference<Integer> hashCode = new AtomicReference<>();

        // Distinguish changed (by hash code)
        Observable<O> result = Observables.distinctUntilChanged(
            watchAtInterval(loader, intervalInSeconds),
            hashCode);

        if (startWithActualValue) {
            try {
                O initial = loader.call().orElse(null);
                if (initial != null) {
                    hashCode.set(initial.hashCode());
                    result = result.startWithItem(initial);
                }
            } catch (Exception e) {
                throw new SumarisTechnicalException(e);
            }
        }

        return result;
    }

    public <K extends Serializable, D extends Date, V extends IUpdateDateEntity<K, D>, L extends Collection<V>> Observable<L>
    watchEntities(final Function<D, Optional<L>> loader,
                  int intervalInSeconds,
                  boolean startWithActualValue) {

        checkInterval(intervalInSeconds);

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

                    // first time we compute the max(updateDate)
                    if (lastUpdateDate.get() == null
                        || lastUpdateDate.get().before(newMaxUpdateDate)) {
                        lastUpdateDate.set(newMaxUpdateDate);
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
                result = result.startWithItem(entities.get());
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
        checkInterval(intervalInSeconds);

        final AtomicReference<Integer> lastHashCode = new AtomicReference();

        Observable<L> result = Observables.distinctUntilChanged(
            watchAtInterval(loader, intervalInSeconds),
            lastHashCode
        );

        // Sending the initial values when starting
        if (startWithActualValue) {
            try {
                L initialValue = loader.call().orElseThrow(() -> new DataNotFoundException("Unable to get actual values: data not found"));
                lastHashCode.set(initialValue.hashCode());
                result = result.startWithItem(initialValue);
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

    @Override
    public <ID extends Serializable, D extends Date, T extends IUpdateDateEntity<ID, D>, V extends IUpdateDateEntity<ID, D>, L extends Collection<V>> Observable<Long> watchEntitiesCount(Class<T> entityClass, Callable<Optional<L>> loader, @Nullable Integer intervalInSeconds, boolean startWithActualValue) {
        AtomicReference<Integer> hashCode = new AtomicReference<>();

        // Watch entity events
        Observable<Long> result = watchEntityEvents(entityClass)
            .map(event -> loader.call())
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(vs -> ((long) vs.size()));

        // Add timer
        if (intervalInSeconds != null && intervalInSeconds > 0) {
            result = Observable.merge(result,
                watchCollectionSize(entityClass, loader, intervalInSeconds, false));
        }

        // Distinguish changed (by hash code)
        result = Observables.distinctUntilChanged(result, hashCode);

        if (startWithActualValue) {
            try {
                L initialVOs = loader.call().orElse(null);
                if (initialVOs != null) {
                    hashCode.set(initialVOs.hashCode());
                    result = result.startWithItem((long) initialVOs.size());
                }
            } catch (Exception e) {
                throw new SumarisTechnicalException(e);
            }
        }

        String listenerId = computeListenerId(entityClass);

        return result.doOnLifecycle(
            (subscription) -> log.debug("Watching count on {} every {}s ({} observers)", listenerId, intervalInSeconds, timerObserverCount.get() + 1),
            () -> log.debug("Stop watching count on {} ({} observers)", listenerId, timerObserverCount.get())
        );
    }

    /* -- Listeners management -- */

    @JmsListener(destination = JmsEntityEvents.DESTINATION,
        selector = "operation = 'update'",
        containerFactory = JmsConfiguration.CONTAINER_FACTORY)
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

    @JmsListener(destination = JmsEntityEvents.DESTINATION,
        selector = "operation = 'delete'",
        containerFactory = JmsConfiguration.CONTAINER_FACTORY)
    protected void onEntityDeleteEvent(IValueObject data, Message message) {
        EntityDeleteEvent event = JmsEntityEvents.parse(EntityDeleteEvent.class, message, data);
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
        T extends IUpdateDateEntity<ID, D>,
        V extends IUpdateDateEntity<ID, D>>
    Observable<V> watchEntityByUpdateEvent(Class<T> entityClass, Class<V> targetClass, ID id) {
        String cacheKey = computeCacheKey(entityClass, targetClass, id);
        int cacheDuration = minIntervalInSeconds / 2;
        return watchEntityByUpdateEvent(entityClass, targetClass, id,
            // We use cache to avoid to many fetch of the same entity, from many user sessions
            cacheManager.cacheable(null,
                    cacheKey,
                    () -> findAndConvert(entityClass, targetClass, id),
                    cacheDuration, TimeUnit.SECONDS)
        );
    }


    protected <ID extends Serializable,
        D extends Date,
        T extends IUpdateDateEntity<ID, D>,
        V extends IUpdateDateEntity<ID, D>>
    Observable<V> watchEntityByUpdateEvent(Class<T> entityClass,
                                           Class<V> targetClass,
                                           ID id,
                                           Callable<Optional<V>> loader) {

        final String listenerId = computeListenerId(entityClass, id);

        return Observable.create(emitter -> {
            Listener listener = new Listener() {
                @Override
                public void onUpdate(EntityUpdateEvent event) {
                    Object data = event.getData();
                    // Already converted into expected type: use source as target
                    if (data != null && targetClass.isAssignableFrom(data.getClass())) {
                        emitter.onNext((V)data);
                    }
                    // Fetch entity, and transform to target class
                    else {
                        try {
                            loader.call()
                                .ifPresent(emitter::onNext);
                        }
                        catch (Exception e) {
                            throw new SumarisTechnicalException(e);
                        }
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
        T extends IUpdateDateEntity<K, D>,
        V extends IUpdateDateEntity<K, D>>
    Observable<V> watchEntityAtInterval(final Class<T> entityClass,
                                        final Class<V> targetClass,
                                        final K id,
                                        final AtomicReference<D> lastUpdateDate,
                                        int intervalInSecond) {
        String cacheKey = computeCacheKey(entityClass, targetClass, id);
        int cacheDuration = Math.round((float) Math.max(minIntervalInSeconds, intervalInSecond) / 2);
        return watchAtInterval(
            // We use cache to avoid to many fetch of the same entity, from many user sessions
            cacheManager.cacheable(null,
                    cacheKey,
                    () -> findNewerById(entityClass, targetClass, id, lastUpdateDate.get()),
                    cacheDuration, TimeUnit.SECONDS),
            intervalInSecond);
    }

    protected <V> Observable<V> watchAtInterval(@NonNull final Callable<Optional<V>> getter, int intervalInSecond) {

        Preconditions.checkArgument(intervalInSecond >= minIntervalInSeconds, "Invalid interval: " + intervalInSecond);

        return Observable
            .interval(intervalInSecond, TimeUnit.SECONDS)
            .observeOn(taskExecutor.map(Schedulers::from).orElseGet(Schedulers::io))
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

    protected <V extends IUpdateDateEntity<?, ?>> void registerListener(String key, Listener listener) {
        synchronized (listenersById) {
            List<Listener> listeners = listenersById.computeIfAbsent(key, k -> Lists.newCopyOnWriteArrayList());

            //log.debug("Listening updates on {} (listener count: {})", key, listeners.size() + 1);

            synchronized (listeners) {
                listeners.add(listener);
            }
        }
    }

    protected void unregisterListener(String key, Listener listener) {
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
        T extends IUpdateDateEntity<K, D>,
        V extends IUpdateDateEntity<K, D>>
    Optional<V> findNewerById(Class<T> entityClass,
                              Class<V> targetClass,
                              K id,
                              Date lastUpdateDate) {

        log.debug("Checking update on {}#{}...", entityClass.getSimpleName(), id);
        T entity = entityDao.find(entityClass, id);
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
        T extends IUpdateDateEntity<K, D>,
        V extends IUpdateDateEntity<K, D>
        > Optional<V> findAndConvert(
        Class<T> entityClass,
        Class<V> targetClass,
        K id) {

        T entity = entityDao.find(entityClass, id);

        // Entity has been deleted
        if (entity == null) {
            return Optional.empty();
        }

        if (entityClass == targetClass) {
            return Optional.of((V)entity);
        }

        if (!conversionService.canConvert(entityClass, targetClass)) {
            throw new SumarisTechnicalException(I18n.t("sumaris.error.missingConverter", entityClass.getSimpleName()));
        }
        // Apply conversion
        V target = conversionService.convert(entity, targetClass);
        return Optional.of(target);
    }

    @Transactional(readOnly = true)
    protected <K extends Serializable, D extends Date, T extends IUpdateDateEntity<K, D>> T find(Class<T> entityClass, K id) {
        return entityDao.find(entityClass, id);
    }

    protected void checkInterval(int intervalInSeconds) {
        Preconditions.checkArgument(intervalInSeconds >= minIntervalInSeconds,
            String.format("interval must be zero (no timer) or greater than %ss (actual : %ss)", minIntervalInSeconds, intervalInSeconds));
    }

    protected  <ID extends Serializable,
        D extends Date,
        V extends IUpdateDateEntity<ID, D>,
        L extends Collection<?>> Observable<Long> watchCollectionSize(final Class<V> entityClass,
                                                                      final Callable<Optional<L>> loader,
                                                                      int intervalInSeconds,
                                                                      boolean startWithActualValue) {
        checkInterval(intervalInSeconds);

        final AtomicReference<Integer> lastHashCode = new AtomicReference<>();

        Observable<Long> result = Observables.distinctUntilChanged(
            watchAtInterval(loader, intervalInSeconds).map(objects -> (long) objects.size()),
            lastHashCode
        );

        // Add debug log, when subscribe/unsubscribe
//        if (log.isDebugEnabled()) {
//            result = result.doOnLifecycle(
//                disposable -> log.debug("watchAtInterval:onSubscribe {}", entityClass.getSimpleName()),
//                () -> log.debug("watchAtInterval:onDispose {}", entityClass.getSimpleName())
//            );
//        }

        // Sending the initial values when starting
        if (startWithActualValue) {
            try {
                L initialValue = loader.call().orElseThrow(() -> new DataNotFoundException("Unable to get actual values: data not found"));
                lastHashCode.set(initialValue.hashCode());
                result = result.startWithItem((long) initialValue.size());
            } catch (Exception e) {
                throw new SumarisTechnicalException(e);
            }
        }

        return result.doOnLifecycle(
            (subscription) -> timerObserverCount.incrementAndGet(),
            timerObserverCount::decrementAndGet
        );
    }

}

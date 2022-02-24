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
import com.google.common.cache.Cache;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.config.JmsConfiguration;
import net.sumaris.core.dao.technical.model.IUpdateDateEntityBean;
import net.sumaris.core.event.entity.IEntityEvent;
import net.sumaris.core.exception.DataNotFoundException;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.server.dao.technical.EntityDao;
import org.apache.commons.collections4.CollectionUtils;
import org.nuiton.i18n.I18n;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

@Service("changesPublisherService")
@Slf4j
public class ChangesPublisherServiceImpl implements ChangesPublisherService {

    @FunctionalInterface
    interface Listener {
        void onNext(Object value);
    }

    @Autowired(required = false)
    protected TaskExecutor taskExecutor;

    @Autowired
    private EntityDao dataChangeDao;

    @Autowired
    private ConversionService conversionService;

    private final AtomicLong observerCount = new AtomicLong(0);
    private final Map<String, List<Listener>> listenersById = Maps.newConcurrentMap();
    private final Map<Integer, Cache<String, Object>> callableCachesByDuration = Maps.newConcurrentMap();

    @Override
    public <K extends Serializable, D extends Date, T extends IUpdateDateEntityBean<K, D>, V extends IUpdateDateEntityBean<K, D>> Observable<V>
    watchEntity(@NonNull final Class<T> entityClass,
                @NonNull final Class<V> targetClass,
                @NonNull final K id,
                Integer intervalInSeconds,
                boolean startWithActualValue) {

        final AtomicReference<D> lastUpdateDate = new AtomicReference<>();

        // Watch entity, using JMS message
        Observable<V> result = watchEntity(entityClass, targetClass, id);

        // Add watch at interval
        if (intervalInSeconds != null && intervalInSeconds > 0) {
            Observable<V> timer = watchEntityAtInterval(
                entityClass, targetClass, id, lastUpdateDate,
                intervalInSeconds);
            result = Observable.merge(result, timer);
        }

        // Keep only more recent
        result = latest(result, lastUpdateDate);

        // Starting with the actual value
        if (startWithActualValue) {
            V initialVO = findAndConvert(entityClass, targetClass, id);
            lastUpdateDate.set(initialVO.getUpdateDate());
            result = result.startWith(initialVO);
        }

        String listenerId = computeListenerId(entityClass, id);

        return result
            .doOnLifecycle(
                (subscription) -> log.info("Watching updates {} every {}s (observer count: {})", listenerId, intervalInSeconds, observerCount.get() + 1),
                () -> log.info("Stop watching updates {}. (observer count: {})", listenerId, observerCount.get())
            );

    }

    public <K extends Serializable, D extends Date, V extends IUpdateDateEntityBean<K, D>, L extends Collection<V>> Observable<L>
    watchCollection(final Function<D, L> getter,
                    int intervalInSeconds,
                    boolean startWithActualValue) {

        Preconditions.checkArgument(intervalInSeconds >= MIN_INTERVAL_IN_SECONDS, "Minimum interval is 10 seconds");

        final AtomicReference<D> lastUpdateDate = new AtomicReference<>();

        Observable<L> result = Observable
            .interval(intervalInSeconds, TimeUnit.SECONDS)
            .observeOn(taskExecutor == null ? Schedulers.io() : Schedulers.from(taskExecutor))
            .flatMap(n -> {
                // Try to find a newer bean
                L entities = getter.apply(lastUpdateDate.get());

                // Update the date used for comparison
                if (CollectionUtils.isNotEmpty(entities)) {
                    D newUpdateDate = entities.stream()
                            .map(IUpdateDateEntityBean::getUpdateDate)
                            .filter(Objects::nonNull)
                            .max(Comparator.comparingLong(Date::getTime))
                            .orElse(null);
                    if (newUpdateDate != null && lastUpdateDate.get().before(newUpdateDate)) {
                        lastUpdateDate.set(newUpdateDate);
                        return Observable.<L>just(entities);
                    }
                }
                return Observable.<L>empty();
            });

        // Sending the initial value when starting
        if (startWithActualValue) {
            // Convert the entity into VO
            L entities = getter.apply(null);
            if (CollectionUtils.isNotEmpty(entities)) {
                D newUpdateDate = entities.stream()
                        .map(IUpdateDateEntityBean::getUpdateDate)
                        .filter(Objects::nonNull)
                        .max(Comparator.comparingLong(Date::getTime))
                        .orElse(null);
                lastUpdateDate.set(newUpdateDate);
                result = result.startWith(entities);
            }
        }

        return result
            .doOnLifecycle(
                (subscription) -> observerCount.incrementAndGet(),
                () -> observerCount.decrementAndGet()
            );
    }

    @Override
    public <K extends Serializable, D extends Date, V extends IUpdateDateEntityBean<K, D>> Observable<V>
    watch(final Function<D, Optional<V>> getter,
          int intervalInSeconds,
          boolean startWithActualValue) {

        Preconditions.checkArgument(intervalInSeconds >= 10, "Minimum interval is 10 seconds");

        AtomicReference<D> lastUpdateDate = new AtomicReference<>();

        // Create stop event, after a too long delay (to be sure old publisher are closed)
        Observable<?> stop = Observable.just(Boolean.TRUE).delay(1, TimeUnit.HOURS);
        Disposable stopSubscription = stop.subscribe(o -> log.debug("Stop watching updates, after a too long delay (1h) (observer count: {})", observerCount.get() - 1));

        Observable<V> timer = watchAtInterval(
            () -> getter.apply(lastUpdateDate.get()),
            intervalInSeconds)
            .takeUntil(stop);

        Observable<V> result = latest(timer, lastUpdateDate);

        // Starting with the actual value
        if (startWithActualValue) {
            V initialVO = getter.apply(null).orElseThrow(() -> new DataNotFoundException("Unable to get actual value: data not found"));
            lastUpdateDate.set(initialVO.getUpdateDate());
            result = result.startWith(initialVO);
        }

        return result.doOnLifecycle(
                (subscription) -> observerCount.incrementAndGet(),
                () -> {
                    observerCount.decrementAndGet();
                    stopSubscription.dispose();
                }
            );
    }


    /* -- protected functions -- */

    @JmsListener(destination = IEntityEvent.JMS_DESTINATION_NAME,
        selector = "operation = 'update'",
        containerFactory = JmsConfiguration.CONTAINER_FACTORY_NAME)
    protected void onEntityEvent(IEntityEvent event) {
        String listenerId = computeListenerId(event);
        List<Listener> listeners = listenersById.get(listenerId);
        if (CollectionUtils.isNotEmpty(listeners)) {
            log.debug("Receiving update {} (listener count: {}}", listenerId, listeners.size());
            listeners.forEach(c -> c.onNext(event.getData()));
        }
    }

    protected <K extends Serializable, D extends Date, T extends IUpdateDateEntityBean<K, D>, V extends IUpdateDateEntityBean<K, D>>
    Observable<V> watchEntity(Class<T> entityClass, Class<V> targetClass, K id) {

        final String listenerId = computeListenerId(entityClass, id);

        return Observable.create(emitter -> {
            Listener listener = (source) -> {
                // Already converted into expected type: use source as target
                if (source != null && source.getClass() == targetClass) {
                    emitter.onNext((V)source);
                }
                // Fetch entity, and transform to target class
                else {
                    V target = findAndConvert(entityClass, targetClass, id);
                    emitter.onNext((V)target);
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
        String cacheKey = computeCacheKey(entityClass, targetClass, id);
        int cacheDuration = Math.max(MIN_INTERVAL_IN_SECONDS, Math.round(intervalInSecond / 2));
        return watchAtInterval(
            cacheable(
                () -> findNewerById(entityClass, targetClass, id, lastUpdateDate.get()),
                cacheKey, cacheDuration
            ),
            intervalInSecond);
    }

    protected <K extends Serializable,
        D extends Date,
        V extends IUpdateDateEntityBean<K, D>>
    Observable<V> watchAtInterval(@NonNull final Callable<Optional<V>> getter, int intervalInSecond) {

        Preconditions.checkArgument(intervalInSecond >= MIN_INTERVAL_IN_SECONDS, "Invalid interval: " + intervalInSecond);

        return Observable
            .interval(intervalInSecond, TimeUnit.SECONDS)
            .observeOn(taskExecutor == null ? Schedulers.io() : Schedulers.from(taskExecutor))
            .map(n -> getter.call())
            .filter(Optional::isPresent)
            .map(Optional::get);
    }

    protected <K extends Serializable,
        D extends Date,
        V extends IUpdateDateEntityBean<K, D>>
    Observable<V> latest(@NonNull Observable<V> observable,
                         @NonNull final AtomicReference<D> lastUpdateDate) {
        return observable.filter(entity -> {
            if (lastUpdateDate.get() != null && lastUpdateDate.get().before(entity.getUpdateDate())) {
                lastUpdateDate.set(entity.getUpdateDate());
                return true;
            }
            return false;
        });
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

    protected String computeListenerId(@NonNull IEntityEvent entityEvent) {
        return computeListenerId(entityEvent.getEntityName(), entityEvent.getId());
    }

    protected String computeListenerId(@NonNull String entityName, @NonNull Serializable id) {
        return entityName + "#" + id;
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


    /**
     * Decorare a callable, using a cache with the given duration (in seconds)
     * @param loader
     * @param cacheKey
     * @param cacheDurationInSeconds
     * @param <R>
     * @return
     * @throws ExecutionException
     */

    protected <R extends Object> Callable<R> cacheable(Callable<R> loader, String cacheKey, Integer cacheDurationInSeconds) {
        // Get the cache for the expected duration
        Cache<String, Object> cache = callableCachesByDuration.computeIfAbsent(cacheDurationInSeconds,
            (d) -> com.google.common.cache.CacheBuilder.newBuilder()
                .expireAfterWrite(d, TimeUnit.SECONDS)
                .maximumSize(500)
                .build());

        // Create a new callable
        return () -> {
            try {
                return (R) cache.get(cacheKey, loader);
            }
            catch (ExecutionException e) {
                throw new SumarisTechnicalException(e.getMessage(), e);
            }
        };
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

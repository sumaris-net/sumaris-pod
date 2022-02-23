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

import com.google.common.base.Preconditions;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.technical.model.IUpdateDateEntityBean;
import net.sumaris.core.exception.DataNotFoundException;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.server.dao.technical.EntityDao;
import org.apache.commons.collections4.CollectionUtils;
import org.nuiton.i18n.I18n;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

@Service("changesPublisherService")
@Slf4j
public class ChangesPublisherServiceImpl implements ChangesPublisherService {

    private final AtomicLong publisherCount = new AtomicLong(0);

    @Autowired(required = false)
    protected TaskExecutor taskExecutor;

    @Autowired
    private EntityDao dataChangeDao;

    @Autowired
    private ConversionService conversionService;

    public <K extends Serializable, D extends Date, V extends IUpdateDateEntityBean<K, D>, L extends Collection<V>> Observable<L>
    watchCollection(final Function<Date, L> getter,
                    Integer minIntervalInSecond,
                    boolean startWithActualValue) {

        Preconditions.checkArgument(minIntervalInSecond == null || minIntervalInSecond.intValue() >= 10, "minimum interval value should be >= 10 seconds");
        if (minIntervalInSecond == null) minIntervalInSecond = 30;

        final Calendar lastUpdateDate = Calendar.getInstance();

        // Create stop event, after a too long delay (to be sure old publisher are closed)
        Observable stop = Observable.just(Boolean.TRUE).delay(1, TimeUnit.HOURS);
        stop.subscribe(o -> log.debug("Closing publisher after a too long delay (1h) (publisher count: {})", publisherCount.get() - 1));

        Observable<L> observable = Observable
                .interval(minIntervalInSecond, TimeUnit.SECONDS)
                .takeUntil(stop)
                .observeOn(taskExecutor == null ? Schedulers.io() : Schedulers.from(taskExecutor))
                .flatMap(n -> {
                    // Try to find a newer bean
                    L entities = getter.apply(lastUpdateDate.getTime());

                    // Update the date used for comparison
                    if (CollectionUtils.isNotEmpty(entities)) {
                        D newUpdateDate = entities.stream()
                                .map(IUpdateDateEntityBean::getUpdateDate)
                                .filter(Objects::nonNull)
                                .max(Comparator.comparingLong(Date::getTime))
                                .orElse(null);
                        if (newUpdateDate != null && lastUpdateDate.getTime().before(newUpdateDate)) {
                            lastUpdateDate.setTime(newUpdateDate);
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
                lastUpdateDate.setTime(newUpdateDate);
                observable = observable.startWith(entities);
            }
        }

        return observable.doOnLifecycle(
            (subscription) -> publisherCount.incrementAndGet(),
            () -> publisherCount.decrementAndGet()
        );
    }

    @Override
    public <K extends Serializable, D extends Date, V extends IUpdateDateEntityBean<K, D>> Observable<V>
    watch(final Function<Date, V> getter,
          Integer intervalInSec,
          boolean startWithActualValue) {

        Preconditions.checkArgument(intervalInSec == null || intervalInSec.intValue() >= 10, "minimum interval value should be >= 10 seconds");
        if (intervalInSec == null) intervalInSec = 30;

        final Calendar lastUpdateDate = Calendar.getInstance();

        // Create stop event, after a too long delay (to be sure old publisher are closed)
        Observable stop = Observable.just(Boolean.TRUE).delay(1, TimeUnit.HOURS);
        stop.subscribe(o -> log.debug("Closing publisher after a too long delay (1h) (publisher count: {})", publisherCount.get() - 1));

        Observable<V> observable = Observable
                .interval(intervalInSec, TimeUnit.SECONDS)
                .takeUntil(stop)
                .observeOn(taskExecutor == null ? Schedulers.io() : Schedulers.from(taskExecutor))
                .flatMap(n -> {
                    // Try to find a newer bean
                    V entity = getter.apply(lastUpdateDate.getTime());

                    // Update the date used for comparision
                    if (entity != null && lastUpdateDate.getTime().before(entity.getUpdateDate())) {
                        lastUpdateDate.setTime(entity.getUpdateDate());
                        return Observable.<V>just(entity);
                    }
                    return Observable.<V>empty();
                });


        // Sending the initial value when starting
        if (startWithActualValue) {
            V initialVO = getter.apply(null);
            lastUpdateDate.setTime(initialVO.getUpdateDate());
            observable = observable.startWith(initialVO);
        }

        return observable.doOnLifecycle(
                (subscription) -> publisherCount.incrementAndGet(),
                () -> publisherCount.decrementAndGet()
            );
    }

    @Override
    public <K extends Serializable, D extends Date, T extends IUpdateDateEntityBean<K, D>, V extends IUpdateDateEntityBean<K, D>> Observable<V>
    watch(final Class<T> entityClass,
                 final Class<V> targetClass,
                 final K id,
                 Integer intervalInSecond,
                 boolean startWithActualValue) {

        return watch((lastUpdateDate) -> findNewerById(entityClass, targetClass, id, lastUpdateDate),
                intervalInSecond,
                startWithActualValue)
            .doOnLifecycle(
                (subscription) -> log.info("Listening changes {}#{} every {}s (publisher count: {})", entityClass.getSimpleName(), id, intervalInSecond, publisherCount.get()),
                () -> log.info("Stop listening changes {}#{}. (publisher count: {})", entityClass.getSimpleName(), id, publisherCount.get() - 1)
            );
    }

    /* -- protected functions -- */

    @Transactional(readOnly = true)
    protected <K extends Serializable, D extends Date, T extends IUpdateDateEntityBean<K, D>, V extends IUpdateDateEntityBean<K, D>> V
    findNewerById(Class<T> entityClass,
                  Class<V> targetClass,
                  K id,
                  Date lastUpdateDate) {

        log.info("Checking changes {}#{}...", entityClass.getSimpleName(), id);
        T entity = dataChangeDao.find(entityClass, id);
        // Entity has been deleted
        if (entity == null) {
            throw new DataNotFoundException(I18n.t("sumaris.error.notFound", entityClass.getSimpleName(), id));
        }

        if (lastUpdateDate == null
            // Entity is newer than last update date
            || entity.getUpdateDate() != null && entity.getUpdateDate().after(lastUpdateDate)) {
            if (!conversionService.canConvert(entityClass, targetClass)) {
                throw new SumarisTechnicalException(I18n.t("sumaris.error.missingConverter", entityClass.getSimpleName()));
            }
            return conversionService.convert(entity, targetClass);
        }
        return null;
    }

    @Transactional(readOnly = true)
    protected <K extends Serializable, D extends Date, T extends IUpdateDateEntityBean<K, D>> T find(Class<T> entityClass, K id) {
        return dataChangeDao.find(entityClass, id);
    }

}

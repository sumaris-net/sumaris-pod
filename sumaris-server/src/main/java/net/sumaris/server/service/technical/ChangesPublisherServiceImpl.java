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
import com.google.common.collect.Comparators;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.technical.model.IUpdateDateEntityBean;
import net.sumaris.core.exception.DataNotFoundException;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.vo.administration.programStrategy.StrategyVO;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.server.dao.technical.EntityDao;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.nuiton.i18n.I18n;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.ConversionService;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.function.Supplier;

@Service("changesPublisherService")
@Slf4j
public class ChangesPublisherServiceImpl implements ChangesPublisherService {

    private final AtomicLong publisherCount = new AtomicLong(0);

    @Autowired
    private EntityDao dataChangeDao;

    @Autowired
    private ConversionService conversionService;

    @Autowired
    private ChangesPublisherService self; // Loop back to be able to force transaction handling

    public <K extends Serializable, D extends Date, V extends IUpdateDateEntityBean<K, D>, L extends List<V>> Publisher<L>
    getListPublisher(final Function<Date, L> getter,
                       Integer minIntervalInSecond,
                       boolean startWithActualValue) {

        Preconditions.checkArgument(minIntervalInSecond == null || minIntervalInSecond.intValue() >= 10, "minimum interval value should be >= 10 seconds");
        if (minIntervalInSecond == null) minIntervalInSecond = 30;

        log.info(String.format("Checking changes (using getter function), every %s sec. (total publishers: %s)", minIntervalInSecond, publisherCount.incrementAndGet()));

        final Calendar lastUpdateDate = Calendar.getInstance();

        // Create stop event, after a too long delay (to be sure old publisher are closed)
        Observable stop = Observable.just(Boolean.TRUE).delay(1, TimeUnit.HOURS);
        stop.subscribe(o -> log.debug(String.format("Closing publisher after a too long delay (1h) (total publishers: %s)", publisherCount.get() - 1)));

        Observable<L> observable = Observable
                .interval(minIntervalInSecond, TimeUnit.SECONDS)
                .takeUntil(stop)
                .observeOn(Schedulers.io())
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

        return observable
                .doAfterTerminate(publisherCount::decrementAndGet) // Decrement counter
                .toFlowable(BackpressureStrategy.LATEST);
    }

    public <K extends Serializable, D extends Date, V extends IUpdateDateEntityBean<K, D>> Publisher<V>
    getPublisher(final Function<Date, V> getter,
                       Integer minIntervalInSecond,
                       boolean startWithActualValue) {

        Preconditions.checkArgument(minIntervalInSecond == null || minIntervalInSecond.intValue() >= 10, "minimum interval value should be >= 10 seconds");
        if (minIntervalInSecond == null) minIntervalInSecond = 30;

        log.info(String.format("Checking changes (using getter function), every %s sec. (total publishers: %s)", minIntervalInSecond, publisherCount.incrementAndGet()));


        final Calendar lastUpdateDate = Calendar.getInstance();

        // Create stop event, after a too long delay (to be sure old publisher are closed)
        Observable stop = Observable.just(Boolean.TRUE).delay(1, TimeUnit.HOURS);
        stop.subscribe(o -> log.debug(String.format("Closing publisher after a too long delay (1h) (total publishers: %s)", publisherCount.get() - 1)));

        Observable<V> observable = Observable
                .interval(minIntervalInSecond, TimeUnit.SECONDS)
                .takeUntil(stop)
                .observeOn(Schedulers.io())
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
            // Convert the entity into VO
            V entity = getter.apply(null);
            if (entity == null) {
                throw new DataNotFoundException(I18n.t("sumaris.error.notFound"));
            }
            lastUpdateDate.setTime(entity.getUpdateDate());
            observable = observable.startWith(entity);
        }

        return observable
                .doAfterTerminate(publisherCount::decrementAndGet) // Decrement counter
                .toFlowable(BackpressureStrategy.LATEST);
    }

    @Override
    public <K extends Serializable, D extends Date, T extends IUpdateDateEntityBean<K, D>, V extends IUpdateDateEntityBean<K, D>> Publisher<V>
    getPublisher(final Class<T> entityClass,
                 final Class<V> targetClass,
                 final K id,
                 Integer intervalInSecond,
                 boolean startWithActualValue) {

        Preconditions.checkArgument(intervalInSecond == null || intervalInSecond >= 10, "interval should be >= 10 seconds");
        if (intervalInSecond == null) intervalInSecond = 30; // Default interval: 30s

        // Check conversion is possible
        if (!conversionService.canConvert(entityClass, targetClass)) {
            throw new SumarisTechnicalException(I18n.t("sumaris.error.missingConverter", entityClass.getSimpleName()));
        }

        // Make sure the entity exists
        T initialEntity = dataChangeDao.find(entityClass, id);
        if (initialEntity == null) {
            throw new DataNotFoundException(I18n.t("sumaris.error.notFound", entityClass.getSimpleName(), id));
        }

        log.info(String.format("Checking changes on %s #%s every %s sec. (total publishers: %s)", entityClass.getSimpleName(), id, intervalInSecond, publisherCount.incrementAndGet()));

        final Calendar lastUpdateDate = Calendar.getInstance();
        if (initialEntity.getUpdateDate() != null) {
            lastUpdateDate.setTime(initialEntity.getUpdateDate());
        } else {
            lastUpdateDate.setTime(new Date());
        }

        // Create stop event, after a too long delay (to be sure old publisher are closed)
        Observable stop = Observable.just(Boolean.TRUE).delay(1, TimeUnit.HOURS);
        stop.subscribe(o -> log.debug("Closing publisher on {} #{}: max time reached. (total publishers: {})", entityClass.getSimpleName(), id, publisherCount.get() - 1));

        Observable<V> observable = Observable
                .interval(intervalInSecond, TimeUnit.SECONDS)
                .takeUntil(stop)
                .observeOn(Schedulers.io())
                .flatMap(n -> {
                    log.debug("Refreshing {} #{}", entityClass.getSimpleName(), id);

                    // Try to find a newer bean
                    V newerVOOrNull = self.getIfNewer(entityClass, targetClass, id, lastUpdateDate.getTime());

                    // Update the date used for comparison
                    if (newerVOOrNull != null) {
                        lastUpdateDate.setTime(newerVOOrNull.getUpdateDate());
                        return Observable.<V>just(newerVOOrNull);
                    }
                    return Observable.<V>empty();
                });

        // Sending the initial value when starting
        if (startWithActualValue) {
            // Convert the entity into VO
            V initialVO = conversionService.convert(initialEntity, targetClass);
            if (initialVO == null) {
                throw new DataNotFoundException(I18n.t("sumaris.error.notFound", entityClass.getSimpleName(), id));
            }
            observable = observable.startWith(initialVO);
        }

        return observable
                .doAfterTerminate(publisherCount::decrementAndGet) // Decrement counter
                .toFlowable(BackpressureStrategy.LATEST);
    }

    @Override
    public <K extends Serializable, D extends Date, T extends IUpdateDateEntityBean<K, D>, V extends IUpdateDateEntityBean<K, D>> V
    getIfNewer(Class<T> entityClass,
               Class<V> targetClass,
               K id,
               Date lastUpdateDate) {

        T entity = dataChangeDao.find(entityClass, id);
        // Entity has been deleted
        if (entity == null) {
            throw new DataNotFoundException(I18n.t("sumaris.error.notFound", entityClass.getSimpleName(), id));
        }

        // Entity is newer than last update date
        if (entity.getUpdateDate() != null && entity.getUpdateDate().after(lastUpdateDate)) {
            if (!conversionService.canConvert(entityClass, targetClass)) {
                throw new SumarisTechnicalException(I18n.t("sumaris.error.missingConverter", entityClass.getSimpleName()));
            }
            return conversionService.convert(entity, targetClass);
        }
        return null;
    }

    /* -- -- */
}

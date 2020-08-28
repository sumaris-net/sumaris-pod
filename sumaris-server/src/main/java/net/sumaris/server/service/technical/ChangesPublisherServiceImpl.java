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
import io.reactivex.BackpressureStrategy;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import net.sumaris.core.dao.technical.model.IUpdateDateEntityBean;
import net.sumaris.core.exception.DataNotFoundException;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.server.dao.technical.EntityDao;
import org.nuiton.i18n.I18n;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Service("changesPublisherService")
public class ChangesPublisherServiceImpl implements ChangesPublisherService {

    /**
     * Logger.
     */
    private static final Logger log =
            LoggerFactory.getLogger(ChangesPublisherServiceImpl.class);

    private final AtomicLong publicherCount = new AtomicLong(0);

    @Autowired
    private EntityDao dataChangeDao;

    @Autowired
    private ConversionService conversionService;

    @Autowired
    private ChangesPublisherService self; // Loop back to be able to force transaction handling

    @Override
    public <K extends Serializable, D extends Date, T extends IUpdateDateEntityBean<K, D>, V extends IUpdateDateEntityBean<K, D>> Publisher<V>
    getPublisher(final Class<T> entityClass,
                 final Class<V> targetClass,
                 final K id,
                 Integer minIntervalInSecond,
                 boolean startWithActualValue) {

        Preconditions.checkArgument(minIntervalInSecond == null || minIntervalInSecond.intValue() >= 10, "minimum interval value should be >= 10 seconds");
        if (minIntervalInSecond == null) minIntervalInSecond = 30;

        // Check conversion is possible
        if (!conversionService.canConvert(entityClass, targetClass)) {
            throw new SumarisTechnicalException(I18n.t("sumaris.error.missingConverter", entityClass.getSimpleName()));
        }

        // Make sure the entity exists
        T initialEntity = dataChangeDao.find(entityClass, id);
        if (initialEntity == null) {
            throw new DataNotFoundException(I18n.t("sumaris.error.notFound", entityClass.getSimpleName(), id));
        }

        log.info(String.format("Checking changes on %s #%s every %s sec. (total publishers: %s)", entityClass.getSimpleName(), id, minIntervalInSecond, publicherCount.incrementAndGet()));


        final Calendar lastUpdateDate = Calendar.getInstance();
        if (initialEntity.getUpdateDate() != null) {
            lastUpdateDate.setTime(initialEntity.getUpdateDate());
        } else {
            lastUpdateDate.setTime(new Date());
        }

        // Create stop event, after a too long delay (to be sure old publisher are closed)
        Observable stop = Observable.just(Boolean.TRUE).delay(1, TimeUnit.HOURS);
        stop.subscribe(o -> log.debug(String.format("Closing publisher on %s #%s: max time reached. (total publishers: %s)", entityClass.getSimpleName(), id, publicherCount.decrementAndGet())));

        Observable<V> observable = Observable
                .interval(minIntervalInSecond, TimeUnit.SECONDS)
                .takeUntil(stop)
                .observeOn(Schedulers.io())
                .flatMap(n -> {
                    // Try to find a newer bean
                    V newerVOOrNull = self.getIfNewer(entityClass, targetClass, id, lastUpdateDate.getTime());

                    // Update the date used for comparision
                    if (newerVOOrNull != null) {
                        lastUpdateDate.setTime(newerVOOrNull.getUpdateDate());
                        return Observable.just(newerVOOrNull);
                    }
                    return Observable.empty();
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


        return observable.toFlowable(BackpressureStrategy.LATEST);
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

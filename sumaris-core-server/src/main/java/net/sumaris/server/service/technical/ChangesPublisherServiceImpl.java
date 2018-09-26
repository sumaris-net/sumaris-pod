package net.sumaris.server.service.technical;

import com.google.common.base.Preconditions;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Observable;
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
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Service("changesPublisherService")
public class ChangesPublisherServiceImpl implements ChangesPublisherService {

    /** Logger. */
    private static final Logger log =
            LoggerFactory.getLogger(ChangesPublisherServiceImpl.class);

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
            throw new SumarisTechnicalException(I18n.t("sumaris.error.missingConverter",  entityClass.getSimpleName()));
        }

        // Make sure the entity exists
        T initialEntity = dataChangeDao.get(entityClass, id);
        if (initialEntity == null) {
            throw new DataNotFoundException(I18n.t("sumaris.error.notFound",  entityClass.getSimpleName(), id));
        }

        log.debug(String.format("Register subscription on %s {%s} every %s sec", entityClass.getSimpleName(), id, minIntervalInSecond));

        final Calendar lastUpdateDate = Calendar.getInstance();
        if (initialEntity.getUpdateDate() != null) {
            lastUpdateDate.setTime(initialEntity.getUpdateDate());
        }
        else {
            lastUpdateDate.setTime(new Date());
        }

        Observable<V> observable = Observable
            .interval(minIntervalInSecond, TimeUnit.SECONDS)
            .flatMap(n -> {
                // Try to get a newer bean
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
                throw new DataNotFoundException(I18n.t("sumaris.error.notFound",  entityClass.getSimpleName(), id));
            }
            observable = observable.startWith(initialVO);
        }

        return observable.toFlowable(BackpressureStrategy.BUFFER);
    }

    @Override
    public
        <K extends Serializable, D extends Date, T extends IUpdateDateEntityBean<K, D>, V extends IUpdateDateEntityBean<K, D>> V
        getIfNewer(Class<T> entityClass,
                   Class<V> targetClass,
                   K id,
                   Date lastUpdateDate) {

        T entity = dataChangeDao.get(entityClass, id);
        // Entity has been deleted
        if (entity == null) {
            throw new DataNotFoundException(I18n.t("sumaris.error.notFound", entityClass.getSimpleName(), id));
        }

        // Entity is newer than last update date
        if (entity.getUpdateDate() != null && entity.getUpdateDate().after(lastUpdateDate)) {
            if (!conversionService.canConvert(entityClass, targetClass)) {
                throw new SumarisTechnicalException(I18n.t("sumaris.error.missingConverter",  entityClass.getSimpleName()));
            }
            return conversionService.convert(entity, targetClass);
        }
        return null;
    }

    /* -- -- */
}

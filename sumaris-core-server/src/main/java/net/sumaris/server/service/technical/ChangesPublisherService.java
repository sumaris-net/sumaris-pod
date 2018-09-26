package net.sumaris.server.service.technical;

import net.sumaris.core.dao.technical.model.IUpdateDateEntityBean;
import org.reactivestreams.Publisher;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.Date;

@Transactional
public interface ChangesPublisherService {

    @Transactional(readOnly = true)
    <K extends Serializable, D extends Date, T extends IUpdateDateEntityBean<K, D>, V extends IUpdateDateEntityBean<K, D>> Publisher<V>
    getPublisher(Class<T> entityClass,
                 Class<V> targetClass,
                 K id,
                 Integer minIntervalInSecond,
                 final boolean startWithActualValue);

    <K extends Serializable, D extends Date, T extends IUpdateDateEntityBean<K, D>, V extends IUpdateDateEntityBean<K, D>> V
    getIfNewer(Class<T> entityClass,
               Class<V> targetClass,
               K id,
               Date lastUpdateDate);
}

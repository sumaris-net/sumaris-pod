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

import io.reactivex.Observable;
import net.sumaris.core.dao.technical.model.IUpdateDateEntityBean;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.Optional;
import java.util.function.Function;


public interface ChangesPublisherService {

    int MIN_INTERVAL_IN_SECONDS = 10;

    <K extends Serializable, D extends Date, T extends IUpdateDateEntityBean<K, D>, V extends IUpdateDateEntityBean<K, D>> Observable<V>
    watchEntity(Class<T> entityClass,
                Class<V> targetClass,
                K id,
                @Nullable Integer intervalInSeconds,
                boolean startWithActualValue);

    <K extends Serializable, D extends Date, V extends IUpdateDateEntityBean<K, D>, L extends Collection<V>> Observable<L>
    watchCollection(final Function<D, L> supplier,
                    int intervalInSeconds,
                    boolean startWithActualValue);

    <K extends Serializable, D extends Date, V extends IUpdateDateEntityBean<K, D>> Observable<V>
    watch(final Function<D, Optional<V>> supplier,
          int intervalInSeconds,
          boolean startWithActualValue);

}

package net.sumaris.core.dao.technical.cache;

/*-
 * #%L
 * SUMARiS:: Core shared
 * %%
 * Copyright (C) 2018 - 2022 SUMARiS Consortium
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

import lombok.NonNull;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public interface ICacheManager {

    Map<String, Map<String, Long>> getCacheStats();

    boolean clearAllCaches();

    boolean clearCache(String name);

    /**
     * Allow to create many caches (one by duration). The final cache Name will be '<cacheGroupName>#<ttl>'
     * @param cacheGroupName optional
     * @param key the value key
     * @param callable the loader function, to load the value, if not in the cache
     * @param timeToLive Time to live, for the value inside the cache
     * @param timeUnit Unit of the previous parameter
     * @return
     * @param <R>
     */
    <R> Callable<R> cacheable(@Nullable String cacheGroupName,
                              @NonNull String key,
                              @NonNull Callable<R> callable,
                              long timeToLive,
                              @NonNull TimeUnit timeUnit);

    <R> Callable<R> cacheable(@NonNull String key,
                              @NonNull Callable<R> callable,
                              long cacheDurationInSeconds);
}

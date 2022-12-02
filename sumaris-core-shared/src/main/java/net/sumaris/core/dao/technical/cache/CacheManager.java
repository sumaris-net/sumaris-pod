/*
 * #%L
 * SUMARiS
 * %%
 * Copyright (C) 2019 SUMARiS Consortium
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

package net.sumaris.core.dao.technical.cache;

import com.google.common.collect.Maps;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.exception.SumarisTechnicalException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import javax.cache.Cache;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component(value = "applicationCacheManager")
@RequiredArgsConstructor
public class CacheManager implements ICacheManager {

    private final Optional<javax.cache.CacheManager> cacheManager;

    private final Map<String, com.google.common.cache.Cache<String, Object>> internalCaches = Maps.newConcurrentMap();

    public Map<String, Map<String, Long>> getCacheStats() {
       return cacheManager.map(Caches::getStatistics).orElseGet(Maps::newHashMap);
    }

    public boolean clearAllCaches() {
        log.info("Clearing caches...");

        // Clear using the delegate cache manager (JCache)
        if (!cacheManager.isEmpty()) {
            try {
                Caches.clearAll(cacheManager.get());
            } catch (RuntimeException e) {
                log.error("Error while clearing JCache caches", e);
                return false;
            }
        }

        // Clear custom callable caches
        if (!internalCaches.isEmpty()) {
            try {
                internalCaches.values()
                        .forEach(com.google.common.cache.Cache::cleanUp);
            } catch (RuntimeException e) {
                log.error("Error while clearing custom caches", e);
                return false;
            }
        }

        log.info("Caches cleared");
        return true;
    }

    public boolean clearCache(@NonNull String name) {
        if (cacheManager.isEmpty()) return false;

        try {
            log.info("Clearing cache ({})...", name);
            Cache cache = cacheManager.get().getCache(name);
            if (cache != null) cache.removeAll();
            log.info("Cache cleared ({})", name);
            return true;
        } catch (RuntimeException e) {
            log.error("Error while clearing cache ({})...", name, e);
            return false;
        }
    }


    @Override
    public <R> Callable<R> cacheable(@NonNull String key, @NonNull Callable<R> callable, long cacheDurationInSeconds) {
        return cacheable(null, key, callable, cacheDurationInSeconds, TimeUnit.SECONDS);
    }

    /**
     * Decorate a callable, using a cache with the given duration (in seconds)
     * @param callable
     * @param key
     * @param <R>
     * @return
     * @throws ExecutionException
     */
    public <R> Callable<R> cacheable(@Nullable String cacheGroupName,
                                     @NonNull String key,
                                     @NonNull Callable<R> callable,
                                     long timeToLive, TimeUnit timeUnit) {
        // Get the cache, with the expected duration
        String cacheName = String.valueOf(timeUnit.toNanos(timeToLive));
        com.google.common.cache.Cache<String, Object> cache = getCache(
                cacheGroupName,
                cacheName,
                timeToLive, timeUnit);

        // Create a new callable
        return () -> {
            try {
                return (R) cache.get(key, callable);
            }
            catch (ExecutionException e) {
                throw new SumarisTechnicalException(e);
            }
        };
    }

    /**
     * Decorare a callable, using a cache with the given duration (in seconds)
     * @param ttl
     * @throws ExecutionException
     */
    public com.google.common.cache.Cache<String, Object> getCache(
            @Nullable final String cacheNamePrefix,
            @NonNull final String cacheName,
            final long ttl,
            @NonNull final TimeUnit timeUnit) {
        final String fullCacheName = StringUtils.isNotBlank(cacheNamePrefix)
                ? cacheNamePrefix + "#" + cacheName
                : cacheName;

        // Get the cache for the expected duration
        return internalCaches.computeIfAbsent(fullCacheName,
                (n) -> com.google.common.cache.CacheBuilder.newBuilder()
                        .expireAfterWrite(ttl, timeUnit)
                        .concurrencyLevel(3)
                        .maximumSize(500)
                        .build());
    }

}

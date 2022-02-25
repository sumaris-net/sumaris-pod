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
import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLQuery;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.exception.SumarisTechnicalException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.cache.Cache;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component(value = "applicationCacheManager")
@ConditionalOnProperty(
    prefix = "spring",
    name = {"cache.enabled"},
    havingValue = "true",
    matchIfMissing = true
)
public class CacheManager {

    @Autowired(required = false)
    private javax.cache.CacheManager cacheManager;

    private final Map<Integer, com.google.common.cache.Cache<String, Object>> callableCachesByDuration = Maps.newConcurrentMap();

    public Map<String, Map<String, Long>> getCacheStats() {
        if (cacheManager == null) return Maps.newHashMap();
        return Caches.getStatistics(cacheManager);
    }

    public boolean clearCache(@NonNull String name) {
        if (cacheManager == null) return false;

        try {
            if (StringUtils.isBlank(name)) {
                log.info("Clearing caches...");
                Caches.clearAll(cacheManager);

            } else {
                log.info(String.format("Clearing cache (%s)...", name));
                Cache cache = cacheManager.getCache(name);
                if (cache != null) cache.removeAll();
            }
        } catch (RuntimeException e) {
            log.error("Error while clearing caches", e);
            return false;
        }
        log.info("Caches cleared.");
        return true;
    }

    /**
     * Decorare a callable, using a cache with the given duration (in seconds)
     * @param loader
     * @param key
     * @param cacheDurationInSeconds
     * @param <R>
     * @return
     * @throws ExecutionException
     */
    public <R extends Object> Callable<R> cacheable(Callable<R> loader, String key, Integer cacheDurationInSeconds) {
        // Get the cache for the expected duration
        com.google.common.cache.Cache<String, Object> cache = callableCachesByDuration.computeIfAbsent(cacheDurationInSeconds,
            (d) -> com.google.common.cache.CacheBuilder.newBuilder()
                .expireAfterWrite(d, TimeUnit.SECONDS)
                .maximumSize(500)
                .build());

        // Create a new callable
        return () -> {
            try {
                return (R) cache.get(key, loader);
            }
            catch (ExecutionException e) {
                throw new SumarisTechnicalException(e);
            }
        };
    }
}

package net.sumaris.core.dao.technical.cache;

/*-
 * #%L
 * SUMARiS:: Core shared
 * %%
 * Copyright (C) 2018 - 2021 SUMARiS Consortium
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

import com.google.common.collect.Maps;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.util.Beans;
import org.ehcache.config.CacheConfiguration;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.ExpiryPolicyBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.MemoryUnit;
import org.ehcache.core.spi.service.StatisticsService;
import org.ehcache.core.statistics.CacheStatistics;
import org.ehcache.core.statistics.DefaultStatisticsService;
import org.ehcache.core.statistics.TierStatistics;
import org.ehcache.jsr107.Eh107Configuration;
import org.springframework.cache.interceptor.SimpleKey;
import org.springframework.cache.jcache.JCacheCacheManager;
import org.springframework.data.util.CastUtils;

import javax.cache.Cache;
import javax.cache.CacheManager;
import java.io.Serializable;
import java.time.Duration;
import java.util.Collection;
import java.util.Map;

/**

 Helper class
 */
@Slf4j
public class Caches {

    protected Caches() {
        // Helper class
    }


    public static void clearAll(@NonNull CacheManager cacheManager) {
        cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).removeAll());
    }

    public static <V> Cache<SimpleKey, V> createHeapCache(
        CacheManager cacheManager,
        String cacheName,
        Class<V> valueClass,
        int timeToLive,
        int entries) {
        return createHeapCache(cacheManager, cacheName, SimpleKey.class, valueClass, timeToLive, entries);
    }

    public static <K, V> Cache<K, V> createHeapCache(
        CacheManager cacheManager,
        String cacheName,
        Class<K> keyType,
        Class<V> valueType,
        int timeToLive,
        int entries) {

        CacheConfiguration<K, V> cacheConfiguration = CacheConfigurationBuilder.newCacheConfigurationBuilder(
            keyType,
            valueType,
            ResourcePoolsBuilder.heap(entries)
        )
            .withDefaultEventListenersThreadPool()
            .withExpiry(
                ExpiryPolicyBuilder.timeToLiveExpiration(Duration.ofSeconds(timeToLive))
            )
            .build();

        log.debug(" Add cache " + cacheName);
        return cacheManager.createCache(cacheName, Eh107Configuration.fromEhcacheCacheConfiguration(cacheConfiguration));

    }

    public static <C extends Collection<V>, V> Cache<SimpleKey, C> createCollectionHeapCache(
        CacheManager cacheManager,
        String cacheName,
        Class<V> valueType,
        int timeToLive,
        int entries) {

        return createCollectionHeapCache(cacheManager, cacheName, SimpleKey.class, valueType, timeToLive, entries);
    }

    public static <K, C extends Collection<V>, V> Cache<K, C> createCollectionHeapCache(
        CacheManager cacheManager,
        String cacheName,
        Class<K> keyType,
        Class<V> valueType,
        int timeToLive,
        int entries) {

        Class<C> collectionClass = CastUtils.cast(Collection.class);
        CacheConfiguration<K, C> cacheConfiguration = CacheConfigurationBuilder.newCacheConfigurationBuilder(
            keyType,
            collectionClass,
            ResourcePoolsBuilder.heap(entries)
        )
            .withExpiry(
                ExpiryPolicyBuilder.timeToLiveExpiration(Duration.ofSeconds(timeToLive))
            )
            .build();

        log.debug(" Add cache " + cacheName);
        return cacheManager.createCache(cacheName, Eh107Configuration.fromEhcacheCacheConfiguration(cacheConfiguration));
    }

    public static <V> Cache<SimpleKey, V> createEternalHeapCache(
        CacheManager cacheManager,
        String cacheName,
        Class<V> valueClass,
        int entries) {

        return createHeapCache(cacheManager, cacheName, valueClass, CacheDurations.ETERNAL, entries);
    }

    public static <K, V> Cache<K, V> createEternalHeapCache(
        CacheManager cacheManager,
        String cacheName,
        Class<K> keyType,
        Class<V> valueClass,
        int entries) {

        return createHeapCache(cacheManager, cacheName, keyType, valueClass, CacheDurations.ETERNAL, entries);
    }

    public static <C extends Collection<V>, V> Cache<SimpleKey, C> createEternalCollectionHeapCache(
        CacheManager cacheManager,
        String cacheName,
        Class<V> valueClass,
        int entries) {

        return createCollectionHeapCache(cacheManager, cacheName, valueClass, CacheDurations.ETERNAL, entries);
    }

    public static <K, C extends Collection<V>, V> Cache<K, C> createEternalCollectionHeapCache(
        CacheManager cacheManager,
        String cacheName,
        Class<K> keyType,
        Class<V> valueClass,
        int entries) {

        return createCollectionHeapCache(cacheManager, cacheName, keyType, valueClass, CacheDurations.ETERNAL, entries);
    }

    /*
    Do not use it for the moment, the cacheManager must have a persistence
    see https://www.ehcache.org/documentation/3.3/tiering.html
     */
    // FIXME
    public static <K extends Serializable, V> Cache<K, V> createEternalDiskCache(
        CacheManager cacheManager,
        String cacheName,
        Class<K> keyType,
        Class<V> valueType,
        int timeToLive,
        int entries,
        int diskSizeMB) {

        CacheConfiguration<K, V> cacheConfiguration = CacheConfigurationBuilder.newCacheConfigurationBuilder(
            keyType,
            valueType,
            ResourcePoolsBuilder
                .heap(entries)
                .disk(diskSizeMB, MemoryUnit.MB)
        )
            .withExpiry(
                ExpiryPolicyBuilder.timeToLiveExpiration(Duration.ofSeconds(timeToLive))
            )
            .build();

        log.debug(" Add cache " + cacheName);
        return cacheManager.createCache(cacheName, Eh107Configuration.fromEhcacheCacheConfiguration(cacheConfiguration));

    }

    public static Map<String, Map<String, Long>> getStatistics(CacheManager cacheManager) {

        Map<String, Map<String, Long>> result = Maps.newTreeMap();

        DefaultStatisticsService statisticsService = Beans.getPrivateProperty(cacheManager, "statisticsService");
        if (statisticsService != null && statisticsService.isStarted())  {
            cacheManager.getCacheNames().forEach(cacheName -> {
                result.put(cacheName, getEhCacheStatistics(statisticsService, cacheName));
            });
        }

        return result;
    }

    private static Map<String, Long> getEhCacheStatistics(StatisticsService statisticsService, String cacheName) {
        Map<String, Long> result = Maps.newLinkedHashMap();
        CacheStatistics cacheStatistics = statisticsService.getCacheStatistics(cacheName);
        Map<String, TierStatistics> tierStatistics = cacheStatistics.getTierStatistics();
        TierStatistics onHeapStatistics = tierStatistics.get("OnHeap");
        if (onHeapStatistics != null) {
            result.put("size", onHeapStatistics.getHits());
            result.put("heapSize", onHeapStatistics.getAllocatedByteSize());
        }
        TierStatistics offHeapStatistics = tierStatistics.get("OffHeap");
        if (offHeapStatistics != null) {
            result.put("offHeapSize", offHeapStatistics.getOccupiedByteSize());
        }
        TierStatistics diskStatistics = tierStatistics.get("Disk");
        if (diskStatistics != null) {
            result.put("diskSize", diskStatistics.getOccupiedByteSize());
        }
        return result;
    }
}

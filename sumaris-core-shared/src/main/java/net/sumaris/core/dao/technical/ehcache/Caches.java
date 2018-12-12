package net.sumaris.core.dao.technical.ehcache;

/*-
 * #%L
 * SUMARiS:: Core shared
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

import net.sf.ehcache.CacheManager;
import org.springframework.cache.ehcache.EhCacheFactoryBean;

/**
 * Helper class
 */
public class Caches {

    protected static final int ETERNAL_TIME_TO_LIVE = 24 * 60* 60; // 1 days
    protected static final int ETERNAL_TIME_TO_IDLE = 24 * 60* 60; // 1 days


    private Caches() {
        // Helper class
    }

    public static EhCacheFactoryBean createHeapCache(CacheManager cacheManager, String cacheName, int timeToLive, int timeToIdle, int entriesLocalHeap) {
        EhCacheFactoryBean factory = new EhCacheFactoryBean();
        // Default properties
        factory.setCacheName(cacheName);
        factory.setCacheManager(cacheManager);

        factory.setMaxEntriesLocalHeap(entriesLocalHeap);

        factory.setEternal(false);
        factory.setOverflowToOffHeap(false);
        factory.setDiskSpoolBufferSize(0);
        factory.setDiskExpiryThreadIntervalSeconds(0);
        factory.setTimeToLive(timeToLive);
        factory.setTimeToIdle(timeToIdle);
        return factory;
    }

    public static EhCacheFactoryBean createEternalHeapCache(CacheManager cacheManager, String cacheName, int entriesLocalHeap) {
       return createHeapCache(cacheManager, cacheName, ETERNAL_TIME_TO_LIVE, ETERNAL_TIME_TO_IDLE, entriesLocalHeap);
    }

    public static EhCacheFactoryBean createEternalDiskCache(CacheManager cacheManager, String cacheName, int timeToLive, int timeToIdle, int entriesLocalHeap, int diskSpoolBufferSize) {
        EhCacheFactoryBean factory = new EhCacheFactoryBean();
        // Default properties
        factory.setCacheName(cacheName);
        factory.setCacheManager(cacheManager);
        factory.setMaxEntriesLocalHeap(entriesLocalHeap);
        factory.setEternal(true);
        factory.setOverflowToOffHeap(true);
        factory.setDiskSpoolBufferSize(diskSpoolBufferSize);
        factory.setDiskExpiryThreadIntervalSeconds(timeToLive);
        factory.setTimeToLive(timeToLive);
        factory.setTimeToIdle(timeToIdle);
        return factory;
    }
}

package net.sumaris.core.service.technical;

/*-
 * #%L
 * SUMARiS:: Core
 * %%
 * Copyright (C) 2018 - 2020 SUMARiS Consortium
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


import net.sf.ehcache.Ehcache;
import net.sf.ehcache.statistics.StatisticsGateway;
import net.sumaris.core.util.Beans;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.ehcache.EhCacheCacheManager;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author peck7 on 03/03/2020.
 */
@Service
public class CacheStatistics {

    private CacheManager cacheManager;

    @Inject
    public void setCacheManager(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    public Map<String, Map<String, Long>> getCacheDetails() {

        Map<String, Map<String, Long>> result = new TreeMap<>();

        if (cacheManager instanceof EhCacheCacheManager) {
            // ehcache manager
            net.sf.ehcache.CacheManager ehCacheManager = ((EhCacheCacheManager) cacheManager).getCacheManager();
            if (ehCacheManager != null) {
                Beans.getStream(ehCacheManager.getCacheNames()).forEach(cacheName ->
                    result.put(cacheName, getEhCacheStatistics(ehCacheManager.getCache(cacheName))));
            }
        }

        // default cache manager
        Beans.getStream(cacheManager.getCacheNames()).forEach(cacheName -> {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null && cache.getNativeCache() instanceof Ehcache) {
                result.putIfAbsent(cacheName, getEhCacheStatistics((Ehcache) cache.getNativeCache()));
            }
        });

        return result;
    }

    private Map<String, Long> getEhCacheStatistics(Ehcache ehCache) {
        Map<String, Long> result = new LinkedHashMap<>();
        StatisticsGateway statistics = ehCache.getStatistics();
        result.put("size", statistics.getSize());
        result.put("heapSize", statistics.getLocalHeapSizeInBytes());
        result.put("offHeapSize", statistics.getLocalOffHeapSizeInBytes());
        result.put("diskSize", statistics.getLocalDiskSizeInBytes());
        return result;
    }
}

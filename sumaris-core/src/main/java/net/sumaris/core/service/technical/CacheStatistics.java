package net.sumaris.core.service.technical;

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

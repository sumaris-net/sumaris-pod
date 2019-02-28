package net.sumaris.core.service.technical;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

@Service
public class EhCacheStatistics {


    private CacheManager cacheManager;

    @Inject
    public void setCacheManager(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    public long getTotalEhCacheSize() {
        long totalSize = 0l;
        for (String cacheName : cacheManager.getCacheNames()) {
            Cache cache = cacheManager.getCache(cacheName);
            Object nativeCache = cache.getNativeCache();
            if (nativeCache instanceof net.sf.ehcache.Ehcache) {
                net.sf.ehcache.Ehcache ehCache = (net.sf.ehcache.Ehcache) nativeCache;
                totalSize += ehCache.getStatistics().getSize();
            }
        }
        return totalSize;
    }

    public Map<String, Long> getCacheDetails() {

        Map<String, Long> res = new HashMap<>();
        long totalSize = 0l;
        for (String cacheName : cacheManager.getCacheNames()) {
            Cache cache = cacheManager.getCache(cacheName);
            Object nativeCache = cache.getNativeCache();
            if (nativeCache instanceof net.sf.ehcache.Ehcache) {
                net.sf.ehcache.Ehcache ehCache = (net.sf.ehcache.Ehcache) nativeCache;
                totalSize += ehCache.getStatistics().getSize();

                res.put(cacheName,totalSize );
            }
        }
        return res;
    }

}
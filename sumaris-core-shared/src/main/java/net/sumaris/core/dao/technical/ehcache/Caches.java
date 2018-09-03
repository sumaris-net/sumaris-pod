package net.sumaris.core.dao.technical.ehcache;

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

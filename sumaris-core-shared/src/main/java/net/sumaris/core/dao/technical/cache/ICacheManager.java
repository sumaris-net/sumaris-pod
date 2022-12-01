package net.sumaris.core.dao.technical.cache;

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

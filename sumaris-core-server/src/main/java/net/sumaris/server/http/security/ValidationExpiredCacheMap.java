package net.sumaris.server.http.security;

/*
 * #%L
 * duniter4j :: UI Wicket
 * %%
 * Copyright (C) 2014 - 2016 EIS
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

import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.apache.commons.lang3.StringUtils;

import java.util.concurrent.TimeUnit;

/**
 * Expiring cache map
 *
 * Created by lpecquot on 03/12/18.
 */
public class ValidationExpiredCacheMap<T> {

    private final Cache<String, T> cache;

    public ValidationExpiredCacheMap(final int lifeTimeInSeconds) {
        this.cache = CacheBuilder.newBuilder()
                .expireAfterWrite(Math.max(lifeTimeInSeconds, 60 /* min value */), TimeUnit.SECONDS)
                .build();
    }

    public boolean contains(String key) {
        return StringUtils.isNotBlank(key) && get(key) != null;
    }

    public T get(String key) {
        Preconditions.checkArgument(StringUtils.isNotBlank(key));
        return cache.getIfPresent(key);
    }

    public void remove(String key) {
        cache.invalidate(key);
    }

    public void add(String key, T data) {
        cache.put(key, data);
    }

    public void clean() {
        this.cache.invalidateAll();
    }
}

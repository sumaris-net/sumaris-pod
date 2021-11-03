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

package net.sumaris.server.http.security;

import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Created by blavenie on 06/01/16.
 */
public class ValidationExpiredCache {

    private final Cache<String, String> cache;

    public ValidationExpiredCache(final int lifeTimeInSeconds) {
        this.cache = CacheBuilder.newBuilder()
                .expireAfterWrite(Math.max(lifeTimeInSeconds, 60 /* min value */), TimeUnit.SECONDS)
                .build();
    }

    public boolean contains(String data) {
        Preconditions.checkArgument(StringUtils.isNotBlank(data));
        String storedData = cache.getIfPresent(data);
        return Objects.equals(storedData, data);
    }

    public void remove(String data) {
        cache.invalidate(data);
    }

    public void add(String data) {
        cache.put(data, data);
    }

    public void clean() {
        this.cache.invalidateAll();
    }
}

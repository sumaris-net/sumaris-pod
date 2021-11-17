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

package net.sumaris.server.http.graphql.technical;

import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLQuery;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.technical.cache.Caches;
import net.sumaris.server.http.graphql.GraphQLApi;
import net.sumaris.server.http.security.IsAdmin;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.ext.com.google.common.collect.Maps;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.cache.Cache;
import javax.cache.CacheManager;
import java.util.Map;

@Slf4j
@Component
@GraphQLApi
@ConditionalOnProperty(
    prefix = "spring",
    name = {"cache.enabled"},
    havingValue = "true",
    matchIfMissing = true
)
public class CacheGraphQLService {

    @Autowired(required = false)
    private CacheManager cacheManager;

    @GraphQLQuery(name = "cacheStatistics", description = "Get cache statistics")
    @IsAdmin
    public Map<String, Map<String, Long>> getCacheStats() {
        if (cacheManager == null) return Maps.newHashMap();
        return Caches.getStatistics(cacheManager);
    }

    @GraphQLQuery(name = "clearCache", description = "Clear a single cache or all caches")
    @IsAdmin
    public boolean clearCache(
        @GraphQLArgument(name = "name") String name
    ) {
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

}

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
import net.sumaris.core.dao.technical.cache.CacheManager;
import net.sumaris.core.util.StringUtils;
import net.sumaris.server.http.graphql.GraphQLApi;
import net.sumaris.server.http.security.IsAdmin;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Map;

@Slf4j
@Component
@GraphQLApi
@ConditionalOnBean({CacheManager.class})
public class CacheGraphQLService {

    @Resource(name = "applicationCacheManager")
    private CacheManager cacheManager;

    @GraphQLQuery(name = "cacheStatistics", description = "Get cache statistics")
    @IsAdmin
    public Map<String, Map<String, Long>> getCacheStats() {
        return cacheManager.getCacheStats();
    }

    @GraphQLQuery(name = "clearCache", description = "Clear a single cache or all caches")
    @IsAdmin
    public boolean clearCache(
        @GraphQLArgument(name = "name", defaultValue = "") String name
    ) {
        if (StringUtils.isBlank(name)) {
            return cacheManager.clearAllCaches();
        }
        return cacheManager.clearCache(name);
    }

}

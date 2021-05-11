package net.sumaris.server.http.graphql.technical;

/*-
 * #%L
 * SUMARiS:: Server
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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLEnvironment;
import io.leangen.graphql.annotations.GraphQLMutation;
import io.leangen.graphql.annotations.GraphQLQuery;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.technical.cache.Caches;
import net.sumaris.core.service.administration.DepartmentService;
import net.sumaris.core.service.technical.ConfigurationService;
import net.sumaris.core.service.technical.SoftwareService;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.technical.ConfigurationVO;
import net.sumaris.core.vo.technical.SoftwareVO;
import net.sumaris.server.config.SumarisServerConfigurationOption;
import net.sumaris.server.http.graphql.administration.AdministrationGraphQLService;
import net.sumaris.server.http.security.IsAdmin;
import net.sumaris.server.service.administration.ImageService;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.cache.Cache;
import javax.cache.CacheManager;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Transactional
@Slf4j
@ConditionalOnBean({CacheManager.class})
public class CacheGraphQLService {

    @Autowired
    private CacheManager cacheManager;

    @GraphQLQuery(name = "cacheStatistics", description = "Get cache statistics")
    @IsAdmin
    public Map<String, Map<String, Long>> getCacheStats() {

        return Caches.getStatistics(cacheManager);
    }

    @GraphQLQuery(name = "clearCache", description = "Clear a single cache or all caches")
    @IsAdmin
    public boolean clearCache(
        @GraphQLArgument(name = "name") String name
    ) {
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

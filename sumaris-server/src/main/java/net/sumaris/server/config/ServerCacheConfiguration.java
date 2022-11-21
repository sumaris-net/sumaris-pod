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

package net.sumaris.server.config;

import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.config.CacheConfiguration;
import net.sumaris.core.dao.technical.cache.Caches;
import org.springframework.boot.autoconfigure.cache.JCacheManagerCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Optional;

@Configuration
@ConditionalOnBean({CacheConfiguration.class})
@Slf4j
public class ServerCacheConfiguration {

    public interface Names {
        String FAVICON = "net.sumaris.server.http.rest.imageRestController.favicon";

        String AVATAR_BY_PUBKEY = "net.sumaris.server.http.rest.imageRestController.avatarByPubkey";
        String IMAGE_BY_ID = "net.sumaris.server.http.rest.imageRestController.imageById";
        String CHANGES_PUBLISHER_FIND_IF_NEWER = "net.sumaris.server.service.technical.changePublisherServer.findIfNewer";
    }

    @Bean
    public JCacheManagerCustomizer serverCacheManagerCustomizer() {

        return cacheManager -> {
            log.info("Adding {Server} caches...");

            // Image cache
            Caches.createEternalHeapCache(cacheManager, Names.FAVICON, Object.class, 1);
            Caches.createHeapCache(cacheManager, Names.IMAGE_BY_ID, Object.class, Duration.ofSeconds(600) /*10 min*/, 50);

            // Change listener
            Caches.createHeapCache(cacheManager, Names.CHANGES_PUBLISHER_FIND_IF_NEWER, Optional.class, Duration.ofSeconds(5), 600);
        };
    }


}

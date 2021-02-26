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
package net.sumaris.rdf.cache;

import lombok.extern.slf4j.Slf4j;
import net.sf.ehcache.CacheManager;
import net.sumaris.core.dao.cache.CacheConfiguration;
import net.sumaris.core.dao.technical.ehcache.Caches;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.cache.ehcache.EhCacheFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnBean({CacheConfiguration.class})
@Slf4j
public class RdfCacheConfiguration {

    protected CacheManager cacheManager;

    @Autowired
    protected RdfCacheConfiguration(CacheConfiguration cacheConfiguration) {
        this.cacheManager = cacheConfiguration.getCacheManager();
        log.info("Adding {RDF} caches...");
    }

    @Bean
    public EhCacheFactoryBean ontologyByNameCache() {
        return Caches.createHeapCache(cacheManager, RdfCacheNames.ONTOLOGY_BY_NAME, 1500, 1500, 600);
    }
}
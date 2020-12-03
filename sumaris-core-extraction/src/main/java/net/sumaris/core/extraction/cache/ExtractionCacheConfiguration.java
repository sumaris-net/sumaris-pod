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
package net.sumaris.core.extraction.cache;

import net.sf.ehcache.CacheManager;
import net.sumaris.core.dao.cache.CacheConfiguration;
import net.sumaris.core.dao.technical.ehcache.Caches;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.cache.ehcache.EhCacheFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnBean({CacheConfiguration.class})
public class ExtractionCacheConfiguration {
    /**
     * Logger.
     */
    protected static final Logger log =
            LoggerFactory.getLogger(ExtractionCacheConfiguration.class);

    protected CacheManager cacheManager;

    @Autowired
    protected ExtractionCacheConfiguration(CacheConfiguration cacheConfiguration) {
        this.cacheManager = cacheConfiguration.getCacheManager();
        log.info("Adding {Extraction} caches...");
    }

    @Bean
    public EhCacheFactoryBean aggregationTypeById() {
        return Caches.createHeapCache(cacheManager, ExtractionCacheNames.AGGREGATION_TYPE_BY_ID, 1500, 1500, 100);
    }

    @Bean
    public EhCacheFactoryBean aggregationTypeByFormat() {
        return Caches.createHeapCache(cacheManager, ExtractionCacheNames.AGGREGATION_TYPE_BY_FORMAT, 60*10000, 60*1000, 100);
    }
}
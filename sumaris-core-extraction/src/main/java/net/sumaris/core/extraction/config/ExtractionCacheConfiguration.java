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
package net.sumaris.core.extraction.config;

import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.config.CacheConfiguration;
import net.sumaris.core.dao.technical.cache.CacheDurations;
import net.sumaris.core.dao.technical.cache.Caches;
import net.sumaris.core.extraction.vo.AggregationTypeVO;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.cache.JCacheManagerCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@AutoConfigureAfter({CacheConfiguration.class})
@ConditionalOnBean({ExtractionAutoConfiguration.class})
@Slf4j
public class ExtractionCacheConfiguration {

    public interface Names {
        String AGGREGATION_TYPE_BY_ID_AND_OPTIONS = "net.sumaris.core.extraction.service.aggregationTypeById";
        String AGGREGATION_TYPE_BY_FORMAT = "net.sumaris.core.extraction.service.aggregationTypeByFormat";
    }

    @Bean
    public JCacheManagerCustomizer extractionCacheCustomizer() {
        return cacheManager -> {
            log.info("Adding {Extraction} caches...");
            Caches.createHeapCache(cacheManager, Names.AGGREGATION_TYPE_BY_ID_AND_OPTIONS, AggregationTypeVO.class, CacheDurations.DEFAULT, 100);
            Caches.createHeapCache(cacheManager, Names.AGGREGATION_TYPE_BY_FORMAT, String.class, AggregationTypeVO.class, CacheDurations.DEFAULT, 100);
        };
    }
}
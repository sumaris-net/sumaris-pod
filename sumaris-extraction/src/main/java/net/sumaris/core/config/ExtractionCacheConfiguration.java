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
package net.sumaris.core.config;

import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.technical.cache.CacheTTL;
import net.sumaris.core.dao.technical.cache.Caches;
import net.sumaris.core.model.technical.extraction.IExtractionType;
import net.sumaris.core.vo.technical.extraction.ExtractionProductVO;
import net.sumaris.extraction.core.vo.ExtractionResultVO;
import net.sumaris.extraction.core.vo.ExtractionTypeVO;
import org.springframework.boot.autoconfigure.cache.JCacheManagerCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

@Configuration
@ConditionalOnBean({CacheConfiguration.class})
@ConditionalOnProperty(
    name = "sumaris.extraction.enabled",
    matchIfMissing = true
)
@Slf4j
public class ExtractionCacheConfiguration {

    public interface Names {

        String EXTRACTION_ROWS_PREFIX = "net.sumaris.core.dao.technical.extraction.extractionRows.";

        String EXTRACTION_TYPES = "net.sumaris.extraction.core.service.extractionTypes";
        String EXTRACTION_TYPE_BY_EXAMPLE = "net.sumaris.extraction.core.service.productByExample";

        String PRODUCT_BY_ID = "net.sumaris.extraction.core.service.productById";
    }

    @Bean
    public JCacheManagerCustomizer extractionCacheCustomizer() {
        return cacheManager -> {
            log.info("Adding {Extraction} caches...");
            Caches.createCollectionHeapCache(cacheManager, Names.EXTRACTION_TYPES, ExtractionTypeVO.class, CacheTTL.MEDIUM.asDuration(), 100);
            Caches.createHeapCache(cacheManager, Names.EXTRACTION_TYPE_BY_EXAMPLE, String.class, IExtractionType.class, CacheTTL.DEFAULT.asDuration(), 500);
            Caches.createHeapCache(cacheManager, Names.PRODUCT_BY_ID, ExtractionProductVO.class, CacheTTL.DEFAULT.asDuration(), 100);

            Arrays.stream(CacheTTL.values())
                .filter(ttl -> ttl.asDuration().getSeconds() > 0) // Skip NONE
                .forEach(ttl -> Caches.createHeapCache(cacheManager, Names.EXTRACTION_ROWS_PREFIX + ttl.name(),
                    Integer.class, ExtractionResultVO.class, ttl.asDuration(), 50));
        };
    }
}
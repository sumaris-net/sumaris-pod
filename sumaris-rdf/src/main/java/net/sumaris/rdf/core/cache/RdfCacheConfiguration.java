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

package net.sumaris.rdf.core.cache;

import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.config.CacheConfiguration;
import net.sumaris.core.dao.technical.cache.CacheTTL;
import net.sumaris.core.dao.technical.cache.Caches;
import org.apache.jena.rdf.model.Model;
import org.springframework.boot.autoconfigure.cache.JCacheManagerCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnBean({CacheConfiguration.class})
@ConditionalOnProperty(name = "rdf.enabled")
@Slf4j
public class RdfCacheConfiguration {

    public interface Names {
        String ONTOLOGY = "net.sumaris.rdf.service.schema.ontology";
        String TYPES_FOR_INDIVIDUALS = "net.sumaris.rdf.service.schema.individualTypes";
        String CLASS_FOR_INDIVIDUALS = "net.sumaris.rdf.service.schema.individualClassNames";
    }

    @Bean
    public JCacheManagerCustomizer rdfCacheCustomizer() {
        return cacheManager -> {
            log.info("Adding {RDF} caches...");
            Caches.createHeapCache(cacheManager, Names.ONTOLOGY, Integer.class, Model.class, CacheTTL.DEFAULT.asDuration(), 50);
            Caches.createCollectionHeapCache(cacheManager, Names.TYPES_FOR_INDIVIDUALS, Integer.class, Class.class, CacheTTL.LONG.asDuration(), 600);
            Caches.createCollectionHeapCache(cacheManager, Names.CLASS_FOR_INDIVIDUALS, Integer.class, String.class, CacheTTL.LONG.asDuration(), 50);
        };
    }

}
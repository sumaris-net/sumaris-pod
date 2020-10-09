package net.sumaris.core.dao.cache;

/*-
 * #%L
 * SUMARiS:: Core
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

import net.sf.ehcache.CacheManager;
import net.sumaris.core.dao.technical.ehcache.Caches;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.ehcache.EhCacheCacheManager;
import org.springframework.cache.ehcache.EhCacheFactoryBean;
import org.springframework.cache.ehcache.EhCacheManagerFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import javax.annotation.PostConstruct;
@Configuration
@ConditionalOnClass({org.springframework.cache.CacheManager.class, net.sf.ehcache.Cache.class})
public class CacheConfiguration {
    /**
     * Logger.
     */
    protected static final Logger log =
            LoggerFactory.getLogger(CacheConfiguration.class);

    @Autowired(required = false)
    protected CacheManager cacheManager;

    @PostConstruct
    protected void init() {
        if (this.cacheManager == null) {
            log.info("Starting cache manager...");
            this.cacheManager = ehcache();
        }
    }

    @Bean(name="ehcacheFactory")
    @ConditionalOnMissingBean({CacheManager.class, EhCacheManagerFactoryBean.class, EhCacheCacheManager.class})
    public EhCacheManagerFactoryBean ehcacheFactory(){
        EhCacheManagerFactoryBean factoryBean = new EhCacheManagerFactoryBean();
        ClassPathResource configFile = new ClassPathResource("ehcache.xml");
        if (!configFile.isReadable()) {
            configFile = new ClassPathResource("ehcache-failsafe.xml");
        }
        factoryBean.setConfigLocation(configFile);
        factoryBean.setShared(true);
        factoryBean.setAcceptExisting(true);
        return factoryBean;
    }

    @Bean
    @ConditionalOnMissingBean({EhCacheCacheManager.class})
    public EhCacheCacheManager cacheManager() {
        EhCacheCacheManager cacheManager = new EhCacheCacheManager();
        cacheManager.setCacheManager(ehcache());
        return cacheManager;
    }

    @Bean
    public EhCacheFactoryBean maxUpdateDateByTypeCache() {
        return Caches.createHeapCache(ehcache(), CacheNames.REFERENTIAL_MAX_UPDATE_DATE_BY_TYPE, CacheDurations.DEFAULT, CacheDurations.DEFAULT, 200);
    }

    @Bean
    public EhCacheFactoryBean departmentByIdCache() {
        return Caches.createHeapCache(ehcache(), CacheNames.DEPARTMENT_BY_ID, CacheDurations.DEFAULT, CacheDurations.DEFAULT, 600);
    }


    @Bean
    public EhCacheFactoryBean departmentByLabelCache() {
        return Caches.createHeapCache(ehcache(), CacheNames.DEPARTMENT_BY_LABEL, CacheDurations.DEFAULT, CacheDurations.DEFAULT, 600);
    }

    @Bean
    public EhCacheFactoryBean personByIdCache() {
        return Caches.createHeapCache(ehcache(), CacheNames.PERSON_BY_ID, CacheDurations.DEFAULT, CacheDurations.DEFAULT, 600);
    }

    @Bean
    public EhCacheFactoryBean personByPubkeyCache() {
        return Caches.createHeapCache(ehcache(), CacheNames.PERSON_BY_PUBKEY, CacheDurations.DEFAULT, CacheDurations.DEFAULT, 600);
    }

    @Bean
    public EhCacheFactoryBean strategiesByProgramId() {
        return Caches.createHeapCache(ehcache(), CacheNames.STRATEGIES_BY_PROGRAM_ID, CacheDurations.DEFAULT, CacheDurations.DEFAULT, 100);
    }

    @Bean
    public EhCacheFactoryBean pmfmByStrategyIdCache() {
        return Caches.createHeapCache(ehcache(), CacheNames.PMFM_BY_STRATEGY_ID, CacheDurations.DEFAULT, CacheDurations.DEFAULT, 100);
    }

    @Bean
    public EhCacheFactoryBean programByIdCache() {
        return Caches.createHeapCache(ehcache(), CacheNames.PROGRAM_BY_ID, 100, CacheDurations.DEFAULT, 100);
    }

    @Bean
    public EhCacheFactoryBean programByLabelCache() {
        return Caches.createEternalHeapCache(ehcache(), CacheNames.PROGRAM_BY_LABEL, 100);
    }

    @Bean
    public EhCacheFactoryBean pmfmByIdCache() {
        return Caches.createEternalHeapCache(ehcache(), CacheNames.PMFM_BY_ID, 600);
    }

    @Bean
    public EhCacheFactoryBean pmfmHasPrefix() {
        return Caches.createEternalHeapCache(ehcache(), CacheNames.PMFM_HAS_PREFIX, 600);
    }

    @Bean
    public EhCacheFactoryBean pmfmHasSuffix() {
        return Caches.createEternalHeapCache(ehcache(), CacheNames.PMFM_HAS_SUFFIX, 600);
    }

    @Bean
    public EhCacheFactoryBean pmfmHasMatrix() {
        return Caches.createEternalHeapCache(ehcache(), CacheNames.PMFM_HAS_MATRIX, 600);
    }

    @Bean
    public EhCacheFactoryBean taxonNameByTaxonReferenceId() {
        return Caches.createEternalHeapCache(ehcache(), CacheNames.TAXON_NAME_BY_TAXON_REFERENCE_ID, 600);
    }

    @Bean
    public EhCacheFactoryBean taxonNamesByTaxonGroupId() {
        return Caches.createEternalHeapCache(ehcache(), CacheNames.TAXON_NAMES_BY_TAXON_GROUP_ID, 600);
    }

    @Bean
    public EhCacheFactoryBean referenceTaxonIdByTaxonNameId() {
        return Caches.createEternalHeapCache(ehcache(), CacheNames.REFERENCE_TAXON_ID_BY_TAXON_NAME_ID, 600);
    }

    @Bean
    public EhCacheFactoryBean referentialTypesCache() {
        return Caches.createEternalHeapCache(ehcache(), CacheNames.REFERENTIAL_TYPES, 600);
    }

    @Bean
    public EhCacheFactoryBean locationLevelByLabel() {
        return Caches.createEternalHeapCache(ehcache(), CacheNames.LOCATION_LEVEL_BY_LABEL, 600);
    }

    @Bean
    public EhCacheFactoryBean productByLabelCache() {
        return Caches.createEternalHeapCache(ehcache(), CacheNames.PRODUCT_BY_LABEL, 100);
    }

    @Bean
    public EhCacheFactoryBean productsCache() {
        return Caches.createEternalHeapCache(ehcache(), CacheNames.PRODUCTS, 100);
    }

    @Bean
    public EhCacheFactoryBean productsByFilterCache() {
        return Caches.createEternalHeapCache(ehcache(), CacheNames.PRODUCTS_BY_FILTER, 100);
    }

    @Bean
    public EhCacheFactoryBean tableMetaByNameCache() {
        return Caches.createHeapCache(ehcache(), CacheNames.TABLE_META_BY_NAME, CacheDurations.DEFAULT, CacheDurations.DEFAULT, 500);
    }


    /* protected */
    protected CacheManager ehcache() {
        return cacheManager != null ? cacheManager : ehcacheFactory().getObject();
    }

}

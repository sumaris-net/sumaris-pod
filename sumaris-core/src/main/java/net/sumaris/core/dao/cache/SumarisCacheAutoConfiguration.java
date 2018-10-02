package net.sumaris.core.dao.cache;

import net.sf.ehcache.CacheManager;
import net.sumaris.core.dao.technical.ehcache.Caches;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
public class SumarisCacheAutoConfiguration {
    /**
     * Logger.
     */
    protected static final Log log =
            LogFactory.getLog(SumarisCacheAutoConfiguration.class);

    @Autowired(required = false)
    protected CacheManager cacheManager;

    @PostConstruct
    public void afterContruct() {
        if (this.cacheManager == null)
            this.cacheManager = ehcache();
    }

    @Bean
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
    @ConditionalOnMissingBean({org.springframework.cache.ehcache.EhCacheCacheManager.class})
    public org.springframework.cache.ehcache.EhCacheCacheManager cacheManager() {
        org.springframework.cache.ehcache.EhCacheCacheManager cacheManager = new EhCacheCacheManager();
        cacheManager.setCacheManager(ehcache());
        return cacheManager;
    }

    @Bean
    public EhCacheFactoryBean departmentByIdCache() {
        return Caches.createHeapCache(ehcache(), CacheNames.DEPARTMENT_BY_ID, 1500, 1500, 600);
    }


    @Bean
    public EhCacheFactoryBean departmentByLabelCache() {
        return Caches.createHeapCache(ehcache(), CacheNames.DEPARTMENT_BY_LABEL, 1500, 1500, 600);
    }

    @Bean
    public EhCacheFactoryBean personByIdCache() {
        return Caches.createHeapCache(ehcache(), CacheNames.PERSON_BY_ID, 1500, 1500, 600);
    }

    @Bean
    public EhCacheFactoryBean personByPubkeyCache() {
        return Caches.createHeapCache(ehcache(), CacheNames.PERSON_BY_PUBKEY, 1500, 1500, 600);
    }

    @Bean
    public EhCacheFactoryBean pmfmByProgramIdCache() {
        return Caches.createHeapCache(ehcache(), CacheNames.PMFM_BY_PROGRAM_ID, 1500, 1500, 100);
    }

    @Bean
    public EhCacheFactoryBean programByLabelCache() {
        return Caches.createHeapCache(ehcache(), CacheNames.PROGRAM_BY_LABEL, 1500, 1500, 100);
    }

    @Bean
    public EhCacheFactoryBean entityNamesCache() {
        return Caches.createEternalHeapCache(ehcache(), CacheNames.REFERENTIAL_TYPES, 600);
    }

    @Bean
    public EhCacheFactoryBean pmfmByIdCache() {
        return Caches.createEternalHeapCache(ehcache(), CacheNames.PMFM_BY_ID, 600);
    }

    /* protected */
    protected net.sf.ehcache.CacheManager ehcache() {
        return cacheManager != null ? cacheManager : ehcacheFactory().getObject();
    }

}

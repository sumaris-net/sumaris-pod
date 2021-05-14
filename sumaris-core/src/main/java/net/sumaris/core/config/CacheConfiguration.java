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
import net.sumaris.core.dao.technical.cache.CacheDurations;
import net.sumaris.core.dao.technical.cache.Caches;
import net.sumaris.core.dao.technical.schema.SumarisTableMetadata;
import net.sumaris.core.vo.administration.programStrategy.DenormalizedPmfmStrategyVO;
import net.sumaris.core.vo.administration.programStrategy.PmfmStrategyVO;
import net.sumaris.core.vo.administration.programStrategy.ProgramVO;
import net.sumaris.core.vo.administration.programStrategy.StrategyVO;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.data.VesselSnapshotVO;
import net.sumaris.core.vo.referential.PmfmVO;
import net.sumaris.core.vo.referential.ReferentialTypeVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import net.sumaris.core.vo.referential.TaxonNameVO;
import net.sumaris.core.vo.technical.extraction.ExtractionProductVO;
import org.hibernate.cache.jcache.ConfigSettings;
import org.springframework.boot.autoconfigure.cache.JCacheManagerCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Date;

@Configuration
@ConditionalOnClass({javax.cache.Cache.class, org.ehcache.Cache.class})
@ConditionalOnProperty(
    name = "spring.cache.enabled",
    havingValue = "true",
    matchIfMissing = true
)
@EnableCaching
@Slf4j
public class CacheConfiguration extends CachingConfigurerSupport {

    public interface Names {
        String DEPARTMENT_BY_ID = "net.sumaris.core.dao.administration.user.departmentById";
        String DEPARTMENT_BY_LABEL = "net.sumaris.core.dao.administration.user.departmentByLabel";
        String PERSON_BY_ID = "net.sumaris.core.dao.administration.user.personById";
        String PERSON_BY_PUBKEY = "net.sumaris.core.dao.administration.user.personByPubkey";
        String REFERENTIAL_MAX_UPDATE_DATE_BY_TYPE = "net.sumaris.core.dao.referential.maxUpdateDateByType";
        String REFERENTIAL_TYPES = "net.sumaris.core.dao.referential.allTypes";
        String LOCATION_LEVEL_BY_LABEL = "net.sumaris.core.dao.referential.findByUniqueLabel";

        String VESSEL_SNAPSHOT_BY_ID_AND_DATE = "net.sumaris.core.service.data.vesselSnapshotById";

        String PROGRAM_BY_ID = "net.sumaris.core.dao.administration.programStrategy.programById";
        String PROGRAM_BY_LABEL = "net.sumaris.core.dao.administration.programStrategy.programByLabel";

        String STRATEGY_BY_ID = "net.sumaris.core.dao.administration.programStrategy.strategyById";
        String STRATEGY_BY_LABEL = "net.sumaris.core.dao.administration.programStrategy.strategyByLabel";
        String STRATEGIES_BY_FILTER = "net.sumaris.core.dao.administration.programStrategy.strategiesByFilter";

        String PMFM_BY_ID = "net.sumaris.core.dao.referential.pmfmById";
        String PMFM_COMPLETE_NAME_BY_ID = "net.sumaris.core.dao.referential.pmfmCompleteNameById";
        String PMFM_HAS_PREFIX = "net.sumaris.core.dao.referential.pmfmHasPrefix";
        String PMFM_HAS_SUFFIX = "net.sumaris.core.dao.referential.pmfmHasSuffix";
        String PMFM_HAS_MATRIX = "net.sumaris.core.dao.referential.pmfmHasMatrix";
        String PMFM_STRATEGIES_BY_FILTER = "net.sumaris.core.dao.administration.programStrategy.pmfmStrategiesByFilter";
        String DENORMALIZED_PMFM_BY_FILTER = "net.sumaris.core.dao.administration.programStrategy.denormalizedPmfmByFilter";

        String TAXON_NAME_BY_TAXON_REFERENCE_ID = "net.sumaris.core.dao.referential.taxonNameByReferenceId";
        String TAXON_NAMES_BY_TAXON_GROUP_ID = "net.sumaris.core.dao.referential.taxonNamesByTaxonGroupId";
        String REFERENCE_TAXON_ID_BY_TAXON_NAME_ID = "net.sumaris.core.dao.referential.referenceTaxonIdByTaxonNameId";

        String ANALYTIC_REFERENCES_BY_FILTER = "net.sumaris.core.dao.referential.analyticReferenceByFilter";

        String PRODUCT_BY_LABEL_AND_OPTIONS = "net.sumaris.core.dao.technical.product.productByLabel";
        String PRODUCTS_BY_FILTER = "net.sumaris.core.dao.technical.product.productByFilter";

        // Technical caches
        String TABLE_META_BY_NAME = "net.sumaris.core.dao.technical.schema.tableMetaByName";
    }

    @Bean
    public HibernatePropertiesCustomizer hibernatePropertiesCustomizer(javax.cache.CacheManager cacheManager) {
        return hibernateProperties -> hibernateProperties.put(ConfigSettings.CACHE_MANAGER, cacheManager);
    }

    @Bean
    public JCacheManagerCustomizer cacheManagerCustomizer(SumarisConfiguration config) {

        return cacheManager -> {
            log.info("Starting cache manager {{}} on {{}}",
                cacheManager.getClass().getSimpleName(),
                config.getCacheDirectory());

            log.info("Adding {Core} caches...");
            Caches.createHeapCache(cacheManager, Names.DEPARTMENT_BY_ID, Integer.class, DepartmentVO.class, CacheDurations.DEFAULT, 600);
            Caches.createHeapCache(cacheManager, Names.DEPARTMENT_BY_LABEL, String.class, DepartmentVO.class, CacheDurations.DEFAULT, 600);
            Caches.createHeapCache(cacheManager, Names.PERSON_BY_ID, Integer.class, PersonVO.class, CacheDurations.DEFAULT, 600);
            Caches.createHeapCache(cacheManager, Names.PERSON_BY_PUBKEY, String.class, PersonVO.class, CacheDurations.DEFAULT, 600);
            Caches.createHeapCache(cacheManager, Names.REFERENTIAL_MAX_UPDATE_DATE_BY_TYPE, String.class, Date.class, CacheDurations.DEFAULT, 600);
            Caches.createHeapCache(cacheManager, Names.VESSEL_SNAPSHOT_BY_ID_AND_DATE, VesselSnapshotVO.class, CacheDurations.DEFAULT, 600);
            Caches.createCollectionHeapCache(cacheManager, Names.STRATEGIES_BY_FILTER, StrategyVO.class, CacheDurations.DEFAULT, 100);
            Caches.createHeapCache(cacheManager, Names.STRATEGY_BY_ID, Integer.class, StrategyVO.class, CacheDurations.LONG, 500);
            Caches.createHeapCache(cacheManager, Names.STRATEGY_BY_LABEL, String.class, StrategyVO.class, CacheDurations.LONG, 500);
            Caches.createCollectionHeapCache(cacheManager, Names.PMFM_STRATEGIES_BY_FILTER, PmfmStrategyVO.class, CacheDurations.DEFAULT, 500);
            Caches.createCollectionHeapCache(cacheManager, Names.DENORMALIZED_PMFM_BY_FILTER, DenormalizedPmfmStrategyVO.class, CacheDurations.DEFAULT, 500);
            Caches.createHeapCache(cacheManager, Names.PROGRAM_BY_ID, Integer.class, ProgramVO.class, CacheDurations.DEFAULT, 100);
            Caches.createEternalHeapCache(cacheManager, Names.PROGRAM_BY_LABEL, String.class, ProgramVO.class, 100);
            Caches.createEternalHeapCache(cacheManager, Names.PMFM_BY_ID, Integer.class, PmfmVO.class, 600);
            Caches.createEternalHeapCache(cacheManager, Names.PMFM_COMPLETE_NAME_BY_ID, Integer.class, String.class, 600);
            Caches.createEternalHeapCache(cacheManager, Names.PMFM_HAS_PREFIX, Boolean.class, 600);
            Caches.createEternalHeapCache(cacheManager, Names.PMFM_HAS_SUFFIX, Boolean.class, 600);
            Caches.createEternalHeapCache(cacheManager, Names.PMFM_HAS_MATRIX, Boolean.class, 600);
            Caches.createEternalHeapCache(cacheManager, Names.TAXON_NAME_BY_TAXON_REFERENCE_ID, Integer.class, TaxonNameVO.class, 600);
            Caches.createEternalCollectionHeapCache(cacheManager, Names.TAXON_NAMES_BY_TAXON_GROUP_ID, Integer.class, TaxonNameVO.class, 600);
            Caches.createEternalHeapCache(cacheManager, Names.REFERENCE_TAXON_ID_BY_TAXON_NAME_ID, Integer.class, Integer.class, 600);
            Caches.createEternalCollectionHeapCache(cacheManager, Names.REFERENTIAL_TYPES, ReferentialTypeVO.class, 600);
            Caches.createEternalHeapCache(cacheManager, Names.LOCATION_LEVEL_BY_LABEL, String.class, ReferentialVO.class, 600);
            Caches.createEternalHeapCache(cacheManager, Names.ANALYTIC_REFERENCES_BY_FILTER, ReferentialVO.class, 100);
            Caches.createEternalHeapCache(cacheManager, Names.PRODUCT_BY_LABEL_AND_OPTIONS, ExtractionProductVO.class, 100);
            Caches.createEternalCollectionHeapCache(cacheManager, Names.PRODUCTS_BY_FILTER, ExtractionProductVO.class, 100);
            Caches.createHeapCache(cacheManager, Names.TABLE_META_BY_NAME, String.class, SumarisTableMetadata.class, CacheDurations.DEFAULT, 500);
        };
    }


}

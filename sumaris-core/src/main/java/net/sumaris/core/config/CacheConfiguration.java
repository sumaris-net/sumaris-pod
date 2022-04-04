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
import net.sumaris.core.dao.technical.schema.SumarisTableMetadata;
import net.sumaris.core.vo.administration.programStrategy.DenormalizedPmfmStrategyVO;
import net.sumaris.core.vo.administration.programStrategy.PmfmStrategyVO;
import net.sumaris.core.vo.administration.programStrategy.ProgramVO;
import net.sumaris.core.vo.administration.programStrategy.StrategyVO;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.data.VesselSnapshotVO;
import net.sumaris.core.vo.referential.*;
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
    prefix = "spring",
    name = {"cache.enabled"},
    havingValue = "true",
    matchIfMissing = true
)
@EnableCaching
@Slf4j
public class CacheConfiguration extends CachingConfigurerSupport {

    public interface Names {
        String REFERENTIAL_MAX_UPDATE_DATE_BY_TYPE = "net.sumaris.core.dao.referential.maxUpdateDateByType";
        String REFERENTIAL_TYPES = "net.sumaris.core.dao.referential.allTypes";

        // Department
        String DEPARTMENT_BY_ID = "net.sumaris.core.dao.administration.user.departmentById";
        String DEPARTMENT_BY_LABEL = "net.sumaris.core.dao.administration.user.departmentByLabel";

        // Person
        String PERSON_BY_ID = "net.sumaris.core.dao.administration.user.personById";
        String PERSON_BY_PUBKEY = "net.sumaris.core.dao.administration.user.personByPubkey";

        // Location
        String LOCATION_LEVEL_BY_LABEL = "net.sumaris.core.dao.referential.location.locationLevelByLabel";
        String LOCATION_BY_ID = "net.sumaris.core.dao.referential.location.locationById";

        // Program
        String PROGRAM_BY_ID = "net.sumaris.core.dao.administration.programStrategy.programById";
        String PROGRAM_BY_LABEL = "net.sumaris.core.dao.administration.programStrategy.programByLabel";
        String PROGRAM_IDS_BY_USER_ID = "net.sumaris.core.dao.administration.programStrategy.programIdsByUserId";

        // Program privilege
        String PROGRAM_PRIVILEGE_BY_ID = "net.sumaris.core.dao.administration.programStrategy.programPrivilegeById";

        // Strategy
        String STRATEGY_BY_ID = "net.sumaris.core.dao.administration.programStrategy.strategyById";
        String STRATEGY_BY_LABEL = "net.sumaris.core.dao.administration.programStrategy.strategyByLabel";
        String STRATEGIES_BY_FILTER = "net.sumaris.core.dao.administration.programStrategy.strategiesByFilter";

        // Pmfm
        String PMFM_BY_ID = "net.sumaris.core.dao.referential.pmfmById";
        String PMFM_COMPLETE_NAME_BY_ID = "net.sumaris.core.dao.referential.pmfmCompleteNameById";
        String PMFM_HAS_PREFIX = "net.sumaris.core.dao.referential.pmfmHasPrefix";
        String PMFM_HAS_SUFFIX = "net.sumaris.core.dao.referential.pmfmHasSuffix";
        String PMFM_HAS_MATRIX = "net.sumaris.core.dao.referential.pmfmHasMatrix";
        String PMFM_HAS_PARAMETER_GROUP = "net.sumaris.core.dao.referential.pmfmHasParameterGroup";

        // Pmfm strategies
        String PMFM_STRATEGIES_BY_FILTER = "net.sumaris.core.dao.administration.programStrategy.pmfmStrategiesByFilter";
        String DENORMALIZED_PMFM_BY_FILTER = "net.sumaris.core.dao.administration.programStrategy.denormalizedPmfmByFilter";

        // Taxon
        String TAXON_NAME_BY_ID = "net.sumaris.core.dao.referential.taxon.taxonNameById";
        String TAXON_NAME_BY_FILTER = "net.sumaris.core.dao.referential.taxon.taxonNameByFilter";
        String TAXON_NAME_BY_TAXON_REFERENCE_ID = "net.sumaris.core.dao.referential.taxon.taxonNameByReferenceId";
        String TAXON_NAMES_BY_TAXON_GROUP_ID = "net.sumaris.core.dao.referential.taxon.taxonNamesByTaxonGroupId";
        String TAXONONOMIC_LEVEL_BY_ID = "net.sumaris.core.dao.referential.taxon.taxonomicLevelById";
        String REFERENCE_TAXON_ID_BY_TAXON_NAME_ID = "net.sumaris.core.dao.referential.taxon.referenceTaxonIdByTaxonNameId";

        // Vessel
        String VESSEL_SNAPSHOT_BY_ID_AND_DATE = "net.sumaris.core.service.data.vessel.vesselSnapshotByIdAndDate";
        String VESSEL_SNAPSHOTS_BY_FILTER = "net.sumaris.core.service.data.vessel.vesselSnapshotByFilter";
        String VESSEL_SNAPSHOTS_COUNT_BY_FILTER = "net.sumaris.core.service.data.vessel.vesselSnapshotCountByFilter";

        // Extraction
        String PRODUCT_BY_LABEL_AND_OPTIONS = "net.sumaris.core.dao.technical.extraction.productByLabel";
        String PRODUCTS_BY_FILTER = "net.sumaris.core.dao.technical.extraction.productByFilter";
        String TABLE_META_BY_NAME = "net.sumaris.core.dao.technical.schema.tableMetaByName";

        // Other
        String GEAR_BY_ID = "net.sumaris.core.dao.referential.gear.gearById";
        String ANALYTIC_REFERENCES_BY_FILTER = "net.sumaris.core.dao.referential.analyticReferenceByFilter";
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
            // Referential
            Caches.createEternalCollectionHeapCache(cacheManager, Names.REFERENTIAL_TYPES, ReferentialTypeVO.class, 600);
            Caches.createHeapCache(cacheManager, Names.REFERENTIAL_MAX_UPDATE_DATE_BY_TYPE, String.class, Date.class, CacheTTL.DEFAULT.asDuration(), 600);

            // Department
            Caches.createHeapCache(cacheManager, Names.DEPARTMENT_BY_ID, Integer.class, DepartmentVO.class, CacheTTL.DEFAULT.asDuration(), 600);
            Caches.createHeapCache(cacheManager, Names.DEPARTMENT_BY_LABEL, String.class, DepartmentVO.class, CacheTTL.DEFAULT.asDuration(), 600);

            // Person
            Caches.createHeapCache(cacheManager, Names.PERSON_BY_ID, Integer.class, PersonVO.class, CacheTTL.DEFAULT.asDuration(), 600);
            Caches.createHeapCache(cacheManager, Names.PERSON_BY_PUBKEY, String.class, PersonVO.class, CacheTTL.DEFAULT.asDuration(), 600);

            // Location
            Caches.createEternalHeapCache(cacheManager, Names.LOCATION_LEVEL_BY_LABEL, String.class, ReferentialVO.class, 600);
            Caches.createEternalHeapCache(cacheManager, Names.LOCATION_BY_ID, Integer.class, LocationVO.class, 2000);

            // Gear
            Caches.createHeapCache(cacheManager, Names.GEAR_BY_ID, Integer.class, ReferentialVO.class, CacheTTL.DEFAULT.asDuration(), 300);

            // Program
            Caches.createHeapCache(cacheManager, Names.PROGRAM_BY_ID, Integer.class, ProgramVO.class, CacheTTL.DEFAULT.asDuration(), 100);
            Caches.createEternalHeapCache(cacheManager, Names.PROGRAM_BY_LABEL, String.class, ProgramVO.class, 100);
            Caches.createEternalHeapCache(cacheManager, Names.PROGRAM_PRIVILEGE_BY_ID, Integer.class, ReferentialVO.class, 10);
            Caches.createCollectionHeapCache(cacheManager, Names.PROGRAM_IDS_BY_USER_ID, Integer.class, Integer.class, CacheTTL.MEDIUM.asDuration(), 500);

            // Strategy
            Caches.createCollectionHeapCache(cacheManager, Names.STRATEGIES_BY_FILTER, StrategyVO.class, CacheTTL.DEFAULT.asDuration(), 100);
            Caches.createHeapCache(cacheManager, Names.STRATEGY_BY_ID, Integer.class, StrategyVO.class, CacheTTL.LONG.asDuration(), 500);
            Caches.createHeapCache(cacheManager, Names.STRATEGY_BY_LABEL, String.class, StrategyVO.class, CacheTTL.LONG.asDuration(), 500);
            Caches.createCollectionHeapCache(cacheManager, Names.PMFM_STRATEGIES_BY_FILTER, PmfmStrategyVO.class, CacheTTL.DEFAULT.asDuration(), 500);
            Caches.createCollectionHeapCache(cacheManager, Names.DENORMALIZED_PMFM_BY_FILTER, DenormalizedPmfmStrategyVO.class, CacheTTL.DEFAULT.asDuration(), 500);

            // Pmfm
            Caches.createEternalHeapCache(cacheManager, Names.PMFM_BY_ID, Integer.class, PmfmVO.class, 600);
            Caches.createEternalHeapCache(cacheManager, Names.PMFM_COMPLETE_NAME_BY_ID, Integer.class, String.class, 600);
            Caches.createEternalHeapCache(cacheManager, Names.PMFM_HAS_PREFIX, Boolean.class, 600);
            Caches.createEternalHeapCache(cacheManager, Names.PMFM_HAS_SUFFIX, Boolean.class, 600);
            Caches.createEternalHeapCache(cacheManager, Names.PMFM_HAS_MATRIX, Boolean.class, 600);
            Caches.createEternalHeapCache(cacheManager, Names.PMFM_HAS_PARAMETER_GROUP, Boolean.class, 600);

            // Taxon name
            Caches.createEternalHeapCache(cacheManager, Names.TAXON_NAME_BY_TAXON_REFERENCE_ID, Integer.class, TaxonNameVO.class, 600);
            Caches.createEternalHeapCache(cacheManager, Names.TAXON_NAME_BY_ID, Integer.class, TaxonNameVO.class, 600);
            Caches.createEternalHeapCache(cacheManager, Names.TAXON_NAME_BY_FILTER, Integer.class, TaxonNameVO.class, 600);
            Caches.createEternalCollectionHeapCache(cacheManager, Names.TAXON_NAMES_BY_TAXON_GROUP_ID, Integer.class, TaxonNameVO.class, 600);
            Caches.createEternalHeapCache(cacheManager, Names.REFERENCE_TAXON_ID_BY_TAXON_NAME_ID, Integer.class, Integer.class, 600);
            Caches.createEternalHeapCache(cacheManager, Names.TAXONONOMIC_LEVEL_BY_ID, Integer.class, ReferentialVO.class, 50);

            // Vessel
            Caches.createHeapCache(cacheManager, Names.VESSEL_SNAPSHOT_BY_ID_AND_DATE, VesselSnapshotVO.class, CacheTTL.DEFAULT.asDuration(), 600);
            Caches.createCollectionHeapCache(cacheManager, Names.VESSEL_SNAPSHOTS_BY_FILTER, VesselSnapshotVO.class, CacheTTL.DEFAULT.asDuration(), 50);
            Caches.createHeapCache(cacheManager, Names.VESSEL_SNAPSHOTS_COUNT_BY_FILTER, Integer.class, Long.class, CacheTTL.DEFAULT.asDuration(), 50);

            // Extraction
            Caches.createEternalHeapCache(cacheManager, Names.PRODUCT_BY_LABEL_AND_OPTIONS, ExtractionProductVO.class, 100);
            Caches.createEternalCollectionHeapCache(cacheManager, Names.PRODUCTS_BY_FILTER, ExtractionProductVO.class, 100);
            Caches.createHeapCache(cacheManager, Names.TABLE_META_BY_NAME, String.class, SumarisTableMetadata.class, CacheTTL.DEFAULT.asDuration(), 500);


            // Other entities
            Caches.createEternalCollectionHeapCache(cacheManager, Names.ANALYTIC_REFERENCES_BY_FILTER, ReferentialVO.class, 100);
        };
    }


}

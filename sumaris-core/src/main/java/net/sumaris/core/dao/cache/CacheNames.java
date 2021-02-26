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

public interface CacheNames {

    String DEPARTMENT_BY_ID = "net.sumaris.core.dao.administration.user.departmentByIdCache";
    String DEPARTMENT_BY_LABEL = "net.sumaris.core.dao.administration.user.departmentByLabelCache";
    String PERSON_BY_ID = "net.sumaris.core.dao.administration.user.personByIdCache";
    String PERSON_BY_PUBKEY = "net.sumaris.core.dao.administration.user.personByPubkeyCache";
    String REFERENTIAL_MAX_UPDATE_DATE_BY_TYPE = "net.sumaris.core.dao.referential.maxUpdateDateByType";
    String REFERENTIAL_TYPES = "net.sumaris.core.dao.referential.allTypesCache";
    String LOCATION_LEVEL_BY_LABEL = "net.sumaris.core.dao.referential.findByUniqueLabelCache";

    String PROGRAM_BY_ID = "net.sumaris.core.dao.administration.programStrategy.programByIdCache";
    String PROGRAM_BY_LABEL = "net.sumaris.core.dao.administration.programStrategy.programByLabelCache";

    String STRATEGY_BY_ID = "net.sumaris.core.dao.administration.programStrategy.strategyByIdCache";
    String STRATEGY_BY_LABEL = "net.sumaris.core.dao.administration.programStrategy.strategyByLabelCache";
    String STRATEGIES_BY_ID = "net.sumaris.core.dao.administration.programStrategy.strategiesByIdCache";
    String STRATEGIES_BY_LABEL = "net.sumaris.core.dao.administration.programStrategy.strategiesByLabelCache";
    String STRATEGIES_BY_PROGRAM_ID = "net.sumaris.core.dao.administration.programStrategy.strategiesByProgramId";

    String PMFM_BY_ID = "net.sumaris.core.dao.referential.pmfmByIdCache";
    String PMFM_COMPLETE_NAME_BY_ID = "net.sumaris.core.dao.referential.pmfmCompleteNameByIdCache";
    String PMFM_HAS_PREFIX = "net.sumaris.core.dao.referential.pmfmHasPrefix";
    String PMFM_HAS_SUFFIX = "net.sumaris.core.dao.referential.pmfmHasSuffix";
    String PMFM_HAS_MATRIX = "net.sumaris.core.dao.referential.pmfmHasMatrix";
    String PMFM_BY_STRATEGY_ID = "net.sumaris.core.dao.administration.programStrategy.pmfmByStrategyIdCache";
    String DENORMALIZED_PMFM_BY_STRATEGY_ID = "net.sumaris.core.dao.administration.programStrategy.denormalized.pmfmByStrategyIdCache";

    String TAXON_NAME_BY_TAXON_REFERENCE_ID = "net.sumaris.core.dao.referential.taxonNameByReferenceId";
    String TAXON_NAMES_BY_TAXON_GROUP_ID = "net.sumaris.core.dao.referential.taxonNamesByTaxonGroupId";
    String REFERENCE_TAXON_ID_BY_TAXON_NAME_ID = "net.sumaris.core.dao.referential.referenceTaxonIdByTaxonNameId";

    String ANALYTIC_REFERENCES_BY_FILTER = "net.sumaris.core.dao.referential.analyticReferenceByFilterCache";

    String PRODUCT_BY_LABEL = "net.sumaris.core.dao.technical.product.productByLabelCache";
    String PRODUCTS = "net.sumaris.core.dao.technical.product.productsCache";
    String PRODUCTS_BY_FILTER = "net.sumaris.core.dao.technical.product.productByFilterCache";

    // Technical caches
    String TABLE_META_BY_NAME = "net.sumaris.core.dao.technical.schema.tableMetaByName";

    // Hibernate caches
    String QUERY_CACHE_NAME = "org.hibernate.cache.spi.QueryResultsRegion";
    String TIMESTAMPS_REGION_CACHE_NAME = "org.hibernate.cache.spi.TimestampsRegion";
}

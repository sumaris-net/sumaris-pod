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

package net.sumaris.core.dao.technical.elasticsearch;

import com.google.common.collect.Lists;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.dao.data.vessel.VesselSnapshotRepository;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.event.config.ConfigurationEvent;
import net.sumaris.core.event.config.ConfigurationReadyEvent;
import net.sumaris.core.event.config.ConfigurationUpdatedEvent;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.model.data.VesselFeatures;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.administration.programStrategy.ProgramVO;
import net.sumaris.core.vo.data.VesselSnapshotVO;
import net.sumaris.core.vo.data.vessel.VesselFetchOptions;
import net.sumaris.core.vo.filter.VesselFilterVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.*;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilter;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.Query;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Stream;

@Slf4j
public class VesselSnapshotElasticsearchRepositoryImpl
    implements VesselSnapshotElasticsearchSpecifications {

    protected final SumarisConfiguration configuration;
    protected final ElasticsearchRestTemplate elasticsearchRestTemplate;

    protected final IndexOperations indexOperations;

    private boolean enableRegistrationCodeSearchAsPrefix = true;

    private boolean enable = false;

    public VesselSnapshotElasticsearchRepositoryImpl(SumarisConfiguration configuration,
                                                     ElasticsearchRestTemplate elasticsearchRestTemplate,
                                                     ElasticsearchOperations operations) {
        this.configuration = configuration;
        this.elasticsearchRestTemplate = elasticsearchRestTemplate;
        this.indexOperations = operations.indexOps(VesselSnapshotVO.class);
    }

    @PostConstruct
    @EventListener({ConfigurationReadyEvent.class, ConfigurationUpdatedEvent.class})
    public void onConfigurationReady() {
        enableRegistrationCodeSearchAsPrefix = configuration.enableVesselRegistrationCodeSearchAsPrefix();

        boolean enable = configuration.enableElasticsearch();
        if (this.enable != enable) {
            this.enable = enable && initIndex();
        }
    }

    @Override
    public long count() {
        if (!this.enable || !indexOperations.exists()) return 0L;

        return count(VesselFilterVO.builder().build());
    }

    @Override
    public void recreate() {
        checkEnable();

        if (indexOperations.exists()) {
            log.debug("Elasticsearch index {{}}: recreating mapping", indexOperations.getIndexCoordinates().getIndexName());
            indexOperations.delete();
        }
        indexOperations.createWithMapping();
    }

    protected boolean initIndex() {
        try {
            // Create index with mapping
            if (!indexOperations.exists()) {
                log.debug("Elasticsearch index {{}}: creating mapping", indexOperations.getIndexCoordinates().getIndexName());
                indexOperations.createWithMapping();
            } else {
                log.debug("Elasticsearch index {{}}c: refreshing mapping", indexOperations.getIndexCoordinates().getIndexName());
                indexOperations.refresh();
            }
            return true;
        } catch (Exception e) {
            log.error("Failed to init Elasticsearch index {{}}: {}", indexOperations.getIndexCoordinates().getIndexName(), e.getMessage(), e);
            return false;
        }
    }

    public Optional<Date> findMaxUpdateDate() {
        checkEnable();

        NativeSearchQuery searchQuery = new NativeSearchQueryBuilder()
            .withQuery(QueryBuilders.matchAllQuery())
            .withSorts(SortBuilders.fieldSort(VesselSnapshotVO.Fields.UPDATE_DATE).order(SortOrder.DESC))
            .withPageable(PageRequest.of(0, 1))
            .withSourceFilter(new FetchSourceFilter(new String[]{VesselSnapshotVO.Fields.UPDATE_DATE}, null))
            .build();

        SearchHits<VesselSnapshotVO> searchHits = elasticsearchRestTemplate.search(searchQuery, VesselSnapshotVO.class);
        if (!searchHits.hasSearchHits()) return Optional.empty(); // No items
        return Optional.ofNullable(searchHits.getSearchHit(0).getContent().getUpdateDate());
    }

    @Override
    public List<Integer> findAllIds() {
        checkEnable();

        Query searchQuery = new NativeSearchQueryBuilder()
           .withFields(VesselSnapshotVO.Fields.ID)
           .withPageable(PageRequest.of(0, 1000)) // Adjust the page size as needed
           .build();

        List<Integer> ids = Lists.newArrayList();
        try (SearchHitsIterator<VesselSnapshotVO> stream = elasticsearchRestTemplate.searchForStream(searchQuery, VesselSnapshotVO.class)) {
            stream.forEachRemaining(hit -> {
                if (hit.getId() != null) ids.add(Integer.parseInt(hit.getId()));
            });
        }
        return ids;
    }

    @Override
    public List<VesselSnapshotVO> findAll(@NonNull VesselFilterVO filter,
                                          @Nullable Page page, VesselFetchOptions fetchOptions) {
        checkEnable();

        QueryBuilder queryBuilder = toSpecification(filter);

        NativeSearchQueryBuilder searchQuery= new NativeSearchQueryBuilder()
            .withQuery(queryBuilder);

        if (page != null) {
            searchQuery.withPageable(page.asPageable());
        }

        try (SearchHitsIterator<VesselSnapshotVO> streamIte = elasticsearchRestTemplate.searchForStream(searchQuery.build(), VesselSnapshotVO.class, IndexCoordinates.of(VesselSnapshotVO.INDEX));
             Stream<SearchHit<VesselSnapshotVO>> stream = streamIte.stream()) {
            return stream.map(SearchHit::getContent).toList();
        }
    }

    @Override
    public long count(@NonNull VesselFilterVO filter) {
        checkEnable();

        QueryBuilder queryBuilder = toSpecification(filter);

        NativeSearchQueryBuilder searchQuery= new NativeSearchQueryBuilder()
            .withQuery(queryBuilder);

        return elasticsearchRestTemplate.count(searchQuery.build(), VesselSnapshotVO.class, IndexCoordinates.of(VesselSnapshotVO.INDEX));
    }

    @Override
    public boolean enableRegistrationCodeSearchAsPrefix() {
        return enableRegistrationCodeSearchAsPrefix;
    }

    protected void checkEnable() {
        if (!this.enable) throw new SumarisTechnicalException("Elasticsearch client has been disabled");
    }

    protected QueryBuilder toSpecification(@NonNull VesselFilterVO filter) {
        BoolQueryBuilder query = QueryBuilders.boolQuery();

        // Filter on vessel id
        if (filter.getVesselId() != null) {
            query.filter(QueryBuilders.termQuery(VesselSnapshotVO.Fields.VESSEL_ID, filter.getVesselId()));
        }

        // Filter on included ids
        if (ArrayUtils.isNotEmpty(filter.getIncludedIds())) {
            query.filter(QueryBuilders.termsQuery(VesselSnapshotVO.Fields.VESSEL_ID, (Object[])filter.getIncludedIds()));
        }

        // Filter on excluded ids
        if (ArrayUtils.isNotEmpty(filter.getExcludedIds())) {
            query.mustNot(QueryBuilders.termsQuery(VesselSnapshotVO.Fields.VESSEL_ID, (Object[])filter.getExcludedIds()));
        }

        // Filter on VesselSnapshotVO.program.label
        if (StringUtils.isNotBlank(filter.getProgramLabel())) {
            query.filter(QueryBuilders.nestedQuery(
                VesselSnapshotVO.Fields.PROGRAM,
                QueryBuilders.termQuery(StringUtils.doting(VesselSnapshotVO.Fields.PROGRAM, ProgramVO.Fields.LABEL), filter.getProgramLabel().toLowerCase()),
                ScoreMode.None));
        }

        // Filter on VesselSnapshotVO.program.id
        if (ArrayUtils.isNotEmpty(filter.getProgramIds())) {
            query.filter(QueryBuilders.nestedQuery(
                VesselSnapshotVO.Fields.PROGRAM,
                QueryBuilders.termsQuery(StringUtils.doting(VesselSnapshotVO.Fields.PROGRAM, ProgramVO.Fields.ID), (Object[])filter.getProgramIds()),
                ScoreMode.None));
        }

        // Filter on vessel type id
        if (filter.getVesselTypeId() != null) {
            query.filter(QueryBuilders.nestedQuery(
                VesselSnapshotVO.Fields.VESSEL_TYPE,
                QueryBuilders.termQuery(StringUtils.doting(VesselSnapshotVO.Fields.VESSEL_TYPE, ReferentialVO.Fields.ID), filter.getVesselTypeId()),
                ScoreMode.None));
        }
        else if (ArrayUtils.isNotEmpty(filter.getVesselTypeIds())) {
            query.filter(QueryBuilders.nestedQuery(
                VesselSnapshotVO.Fields.VESSEL_TYPE,
                QueryBuilders.termsQuery(StringUtils.doting(VesselSnapshotVO.Fields.VESSEL_TYPE, ReferentialVO.Fields.ID), (Object[])filter.getVesselTypeIds()),
                ScoreMode.None));
        }

        // Filter on dates
        if (filter.getStartDate() != null && filter.getEndDate() != null) {
            query.mustNot(
                QueryBuilders.boolQuery()
                    .should(QueryBuilders.rangeQuery("endDate").lt(filter.getStartDate().getTime()))
                    .should(QueryBuilders.rangeQuery("startDate").gt(filter.getEndDate().getTime()))
            );
        }
        // Start date only
        else if (filter.getStartDate() != null) {
            query.filter(QueryBuilders.rangeQuery("endDate").gte(filter.getStartDate().getTime()));
        }
        // End date only
        else if (filter.getEndDate() != null) {
            query.filter(QueryBuilders.rangeQuery("startDate").lte(filter.getEndDate().getTime()));
        }

        // Search searchText on each searchAttributes
        if (StringUtils.isNotBlank(filter.getSearchText())) {
            String escapedSearchText = ElasticsearchUtils.getEscapedSearchText(filter.getSearchText(), true);
            String[] attributes = ArrayUtils.isNotEmpty(filter.getSearchAttributes()) ? filter.getSearchAttributes() : VesselSnapshotRepository.DEFAULT_SEARCH_ATTRIBUTES;
            boolean enableRegistrationCodeSearchAsPrefix = enableRegistrationCodeSearchAsPrefix();

            BoolQueryBuilder searchQuery = QueryBuilders.boolQuery().minimumShouldMatch(1);
            for (String attr : attributes) {
                boolean usePrefixMatch = enableRegistrationCodeSearchAsPrefix && !attr.endsWith(VesselFeatures.Fields.NAME);
                if (usePrefixMatch) {
                    searchQuery.should(QueryBuilders.prefixQuery(attr, escapedSearchText));
                }
                else {
                    searchQuery.should(QueryBuilders.wildcardQuery(attr, "*" + escapedSearchText + "*"));
                }
            }
            query.filter(searchQuery);
        }

        // Filter on the property statusId
        if (CollectionUtils.isNotEmpty(filter.getStatusIds())) {
            query.must(QueryBuilders.termsQuery(VesselSnapshotVO.Fields.VESSEL_STATUS_ID, filter.getStatusIds()));
        }

        return query;
    }
}

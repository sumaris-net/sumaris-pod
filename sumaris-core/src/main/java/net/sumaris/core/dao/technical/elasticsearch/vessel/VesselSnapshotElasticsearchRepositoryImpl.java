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

package net.sumaris.core.dao.technical.elasticsearch.vessel;

import com.google.common.collect.Lists;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.dao.technical.elasticsearch.ElasticsearchSpecification;
import net.sumaris.core.event.config.ConfigurationReadyEvent;
import net.sumaris.core.event.config.ConfigurationUpdatedEvent;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.vo.data.VesselSnapshotVO;
import net.sumaris.core.vo.data.vessel.VesselFetchOptions;
import net.sumaris.core.vo.filter.VesselFilterVO;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
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
import java.util.Date;
import java.util.List;
import java.util.Optional;
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
        if (!this.enable || !indexOperations.exists()) return -1L;
        NativeSearchQueryBuilder query= new NativeSearchQueryBuilder()
            .withQuery(QueryBuilders.matchAllQuery());
        return elasticsearchRestTemplate.count(query.build(), VesselSnapshotVO.class, IndexCoordinates.of(VesselSnapshotVO.INDEX));
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
                log.debug("Elasticsearch index {{}}: refreshing mapping", indexOperations.getIndexCoordinates().getIndexName());
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

        QueryBuilder query = createQuery(toSpecification(filter));
        NativeSearchQueryBuilder searchQuery = new NativeSearchQueryBuilder()
            .withQuery(query);

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

        QueryBuilder query = createQuery(ElasticsearchSpecification.constantScore(toSpecification(filter)));
        NativeSearchQueryBuilder searchQuery = new NativeSearchQueryBuilder()
            .withQuery(query);

        return elasticsearchRestTemplate.count(searchQuery.build(), VesselSnapshotVO.class, IndexCoordinates.of(VesselSnapshotVO.INDEX));
    }

    @Override
    public boolean enableRegistrationCodeSearchAsPrefix() {
        return enableRegistrationCodeSearchAsPrefix;
    }

    protected void checkEnable() {
        if (!this.enable) throw new SumarisTechnicalException("Elasticsearch client has been disabled");
    }

    protected QueryBuilder createQuery(@org.springframework.lang.Nullable ElasticsearchSpecification<QueryBuilder> specification) {
        QueryBuilder query = specification != null ? specification.toPredicate() : null;
        return query != null ? query : QueryBuilders.matchAllQuery();
    }

    protected ElasticsearchSpecification<QueryBuilder> toSpecification(@NonNull VesselFilterVO filter) {

        return ElasticsearchSpecification.bool()
            // IDs
            .filter(id(filter.getVesselFeaturesId()))
            .filter(vesselId(filter.getVesselId()))
            .filter(includedVesselIds(filter.getIncludedIds()))
            .filter(excludedVesselIds(filter.getExcludedIds()))
            // Type
            .filter(vesselTypeId(filter.getVesselTypeId()))
            .filter(vesselTypeIds(filter.getVesselTypeIds()))
            // by locations
            .filter(registrationLocation(filter.getRegistrationLocationId()))
            .filter(basePortLocation(filter.getBasePortLocationId()))
            // by Status
            .filter(hasStatusIds(filter.getStatusIds()))
            // by program
            .filter(programLabel(filter.getProgramLabel()))
            .filter(programIds(filter.getProgramIds()))
            // Dates
            .filter(betweenDates(filter.getStartDate(), filter.getEndDate()))
            .filter(newerThan(filter.getMinUpdateDate()))
            // Text
            .must(searchText(filter.getSearchAttributes(), filter.getSearchText()))
            ;
    }
}

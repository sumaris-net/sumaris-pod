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

package net.sumaris.core.dao.technical.optimization.vessel;

import co.elastic.clients.elasticsearch.snapshot.Repository;
import lombok.RequiredArgsConstructor;
import net.sumaris.core.vo.data.VesselSnapshotVO;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilter;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;

import java.util.Date;
import java.util.Optional;

public class VesselSnapshotElasticsearchRepositoryImpl
    implements VesselSnapshotElasticsearchSpecifications {

    protected final ElasticsearchRestTemplate elasticsearchRestTemplate;

    @Autowired
    public VesselSnapshotElasticsearchRepositoryImpl(ElasticsearchRestTemplate elasticsearchRestTemplate) {
        this.elasticsearchRestTemplate = elasticsearchRestTemplate;
    }

    public Optional<Date> findMaxUpdateDate() {
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
}

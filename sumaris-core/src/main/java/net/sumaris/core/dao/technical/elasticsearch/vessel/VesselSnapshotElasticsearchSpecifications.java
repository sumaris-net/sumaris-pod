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

import lombok.NonNull;
import net.sumaris.core.dao.data.vessel.IVesselSnapshotSpecifications;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.dao.technical.elasticsearch.ElasticsearchSpecification;
import net.sumaris.core.dao.technical.elasticsearch.ElasticsearchUtils;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.administration.programStrategy.ProgramVO;
import net.sumaris.core.vo.data.VesselFeaturesVO;
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

import javax.annotation.Nullable;
import java.util.Date;
import java.util.List;
import java.util.Optional;


public interface VesselSnapshotElasticsearchSpecifications extends IVesselSnapshotSpecifications {


    String[] DEFAULT_SEARCH_ATTRIBUTES = new String[] {
        VesselSnapshotVO.Fields.NAME,
        VesselSnapshotVO.Fields.REGISTRATION_CODE,
        VesselSnapshotVO.Fields.EXTERIOR_MARKING
    };

    default ElasticsearchSpecification<QueryBuilder> vesselFeaturesId(Integer vesselFeaturesId) {
        if (vesselFeaturesId == null) return null;
        return () -> QueryBuilders.termQuery(VesselSnapshotVO.Fields.VESSEL_FEATURES_ID, vesselFeaturesId);
    }

    default ElasticsearchSpecification<QueryBuilder> vesselId(Integer vesselId) {
        if (vesselId == null) return null;
        return () -> QueryBuilders.termQuery(VesselSnapshotVO.Fields.VESSEL_ID, vesselId);
    }

    default ElasticsearchSpecification<QueryBuilder> includedVesselIds(Integer[] vesselIds) {
        if (ArrayUtils.isEmpty(vesselIds)) return null;
        return () -> QueryBuilders.termsQuery(VesselSnapshotVO.Fields.VESSEL_ID, (Object[])vesselIds);
    }

    default ElasticsearchSpecification<QueryBuilder> excludedVesselIds(Integer[] vesselIds) {
        if (ArrayUtils.isEmpty(vesselIds)) return null;
        return () -> QueryBuilders.termsQuery(VesselSnapshotVO.Fields.VESSEL_ID, (Object[])vesselIds);
    }

    default ElasticsearchSpecification<QueryBuilder> programLabel(String programLabel) {
        if (StringUtils.isBlank(programLabel)) return null;
        return () -> QueryBuilders.nestedQuery(
            VesselSnapshotVO.Fields.PROGRAM,
            QueryBuilders.termQuery(StringUtils.doting(VesselSnapshotVO.Fields.PROGRAM, ProgramVO.Fields.LABEL), programLabel.toLowerCase()),
            ScoreMode.None);
    }


    default ElasticsearchSpecification<QueryBuilder> programIds(Integer[] programIds) {
        if (ArrayUtils.isEmpty(programIds)) return null;
        return () -> QueryBuilders.nestedQuery(
                VesselSnapshotVO.Fields.PROGRAM,
                QueryBuilders.termsQuery(StringUtils.doting(VesselSnapshotVO.Fields.PROGRAM, ProgramVO.Fields.ID), (Object[])programIds),
                ScoreMode.None);
    }

    default ElasticsearchSpecification<QueryBuilder> vesselTypeId(Integer vesselTypeId) {
        if (vesselTypeId == null) return null;
        return () -> QueryBuilders.nestedQuery(
            VesselSnapshotVO.Fields.VESSEL_TYPE,
            QueryBuilders.termQuery(StringUtils.doting(VesselSnapshotVO.Fields.VESSEL_TYPE, ReferentialVO.Fields.ID), vesselTypeId),
            ScoreMode.None);
    }

    default ElasticsearchSpecification<QueryBuilder> vesselTypeIds(Integer... vesselTypeIds) {
        if (ArrayUtils.isEmpty(vesselTypeIds)) return null;
        return () -> QueryBuilders.nestedQuery(
                VesselSnapshotVO.Fields.VESSEL_TYPE,
                QueryBuilders.termsQuery(StringUtils.doting(VesselSnapshotVO.Fields.VESSEL_TYPE, ReferentialVO.Fields.ID), (Object[])vesselTypeIds),
                ScoreMode.None);
    }

    default ElasticsearchSpecification<QueryBuilder> registrationLocation(Integer registrationLocationId) {
        if (registrationLocationId == null) return null;
        return () -> QueryBuilders.nestedQuery(
            VesselSnapshotVO.Fields.REGISTRATION_LOCATION,
            QueryBuilders.termQuery(StringUtils.doting(VesselSnapshotVO.Fields.REGISTRATION_LOCATION, ReferentialVO.Fields.ID), registrationLocationId),
            ScoreMode.None);
    }

    default ElasticsearchSpecification<QueryBuilder> countryRegistrationLocation(Integer countryRegistrationLocationId) {
        if (countryRegistrationLocationId == null) return null;
        return () -> QueryBuilders.nestedQuery(
            VesselSnapshotVO.Fields.COUNTRY_REGISTRATION_LOCATION,
            QueryBuilders.termQuery(StringUtils.doting(VesselSnapshotVO.Fields.COUNTRY_REGISTRATION_LOCATION, ReferentialVO.Fields.ID), countryRegistrationLocationId),
            ScoreMode.None);
    }

    default ElasticsearchSpecification<QueryBuilder> basePortLocation(Integer basePortLocationId) {
        if (basePortLocationId == null) return null;
        return () -> QueryBuilders.nestedQuery(
            VesselSnapshotVO.Fields.BASE_PORT_LOCATION,
            QueryBuilders.termQuery(StringUtils.doting(VesselSnapshotVO.Fields.BASE_PORT_LOCATION, ReferentialVO.Fields.ID), basePortLocationId),
            ScoreMode.None);
    }

    default ElasticsearchSpecification<QueryBuilder> hasStatusIds(List<Integer> statusIds) {
        if (CollectionUtils.isEmpty(statusIds)) return null;
        return () -> QueryBuilders.termsQuery(VesselSnapshotVO.Fields.VESSEL_STATUS_ID, statusIds);
    }

    default ElasticsearchSpecification<QueryBuilder> betweenDates(Date startDate, Date endDate) {
        if (startDate == null && endDate == null) return null;
        return () -> {
            if (startDate != null && endDate != null) {
                return QueryBuilders.constantScoreQuery(
                    QueryBuilders.boolQuery()
                        .filter(QueryBuilders.rangeQuery(VesselSnapshotVO.Fields.START_DATE).lte(endDate.getTime()))
                        .filter(QueryBuilders.rangeQuery(VesselSnapshotVO.Fields.END_DATE).gte(startDate.getTime()))
                );
            }
            // Start date only
            else if (startDate != null) {
                return QueryBuilders.rangeQuery(VesselSnapshotVO.Fields.END_DATE).gte(startDate.getTime());
            }
            // End date only
            else {
                return QueryBuilders.rangeQuery(VesselSnapshotVO.Fields.START_DATE).lte(endDate.getTime());
            }
        };
    }

    default ElasticsearchSpecification<QueryBuilder> newerThan(Date minUpdateDate) {
        if (minUpdateDate == null) return null;
        return () -> QueryBuilders.rangeQuery(VesselSnapshotVO.Fields.UPDATE_DATE).gt(minUpdateDate.getTime());
    }

    default ElasticsearchSpecification<QueryBuilder> searchText(String[] searchAttributes, String searchText) {
        if (StringUtils.isBlank(searchText)) return null;
        String escapedSearchText = ElasticsearchUtils.getEscapedSearchText(searchText, false);
        return () -> {
            boolean enableRegistrationCodeSearchAsPrefix = enableRegistrationCodeSearchAsPrefix();

            BoolQueryBuilder searchTextQuery = QueryBuilders.boolQuery().minimumShouldMatch(1);
            String[] searchWords = escapedSearchText.split("\\s+");

            String[] attributes = ArrayUtils.isNotEmpty(searchAttributes) ? searchAttributes : DEFAULT_SEARCH_ATTRIBUTES;
            for (String attr : attributes) {
                BoolQueryBuilder attrQuery = QueryBuilders.boolQuery().minimumShouldMatch(searchWords.length);
                for (int i = 0; i < searchWords.length; i++) {
                    String word = searchWords[i];
                    boolean isPrefixMatch = enableRegistrationCodeSearchAsPrefix && i == 0 && !attr.endsWith(VesselFeaturesVO.Fields.NAME);
                    boolean noWildcard = word.indexOf('*') == -1 && word.indexOf('?') == -1;

                    if (isPrefixMatch && noWildcard) {
                        attrQuery.should(QueryBuilders.prefixQuery(attr, ElasticsearchUtils.trimWildcard(word)));
                    } else {
                        String pattern = (isPrefixMatch || word.startsWith("*") ? "" : "*") + word + "*";
                        attrQuery.should(QueryBuilders.wildcardQuery(attr, pattern));
                    }
                }
                searchTextQuery.should(attrQuery);
            }

            return searchTextQuery;
        };
    }

    default List<VesselSnapshotVO> findAll(@NonNull VesselFilterVO filter, int offset, int size, String sortAttribute, SortDirection sortDirection, VesselFetchOptions fetchOptions) {
        return findAll(filter, Page.create(offset, size, sortAttribute, sortDirection), fetchOptions);
    }

    List<VesselSnapshotVO> findAll(@NonNull VesselFilterVO filter,
                                   @Nullable Page page,
                                   @Nullable VesselFetchOptions fetchOptions);

    org.springframework.data.domain.Page<VesselSnapshotVO> findAllAsPage(@NonNull VesselFilterVO filter,
                                                                         @Nullable Page page,
                                                                         @Nullable VesselFetchOptions fetchOptions);

    long count();

    long count(@NonNull VesselFilterVO filter);

    void recreate();

    Optional<Date> findMaxUpdateDate();

    List<Integer> findAllVesselFeaturesIds();

    List<Integer> findAllVesselFeaturesIdsByFilter(@Nullable VesselFilterVO filter);

    boolean enableRegistrationCodeSearchAsPrefix();

}

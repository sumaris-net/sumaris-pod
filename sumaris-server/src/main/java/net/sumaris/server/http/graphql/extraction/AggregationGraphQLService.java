package net.sumaris.server.http.graphql.extraction;

/*-
 * #%L
 * SUMARiS:: Server
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

import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLEnvironment;
import io.leangen.graphql.annotations.GraphQLMutation;
import io.leangen.graphql.annotations.GraphQLQuery;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.dao.technical.model.IEntity;
import net.sumaris.core.extraction.service.AggregationService;
import net.sumaris.core.extraction.specification.AggRdbSpecification;
import net.sumaris.core.extraction.vo.AggregationResultVO;
import net.sumaris.core.extraction.vo.AggregationStrataVO;
import net.sumaris.core.extraction.vo.AggregationTypeVO;
import net.sumaris.core.extraction.vo.ExtractionFilterVO;
import net.sumaris.core.extraction.vo.filter.AggregationTypeFilterVO;
import net.sumaris.core.model.data.IWithRecorderDepartmentEntity;
import net.sumaris.core.model.data.IWithRecorderPersonEntity;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.technical.extraction.ExtractionTableColumnVO;
import net.sumaris.core.vo.technical.extraction.ExtractionProductFetchOptions;
import net.sumaris.server.http.geojson.extraction.GeoJsonExtractions;
import net.sumaris.server.http.security.IsSupervisor;
import net.sumaris.server.http.security.IsUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Component
public class AggregationGraphQLService {

    @Autowired
    private AggregationService aggregationService;


    /* -- aggregation service -- */

    @GraphQLQuery(name = "aggregationType", description = "Get one aggregation type")
    public AggregationTypeVO getAllAggregationTypes(@GraphQLArgument(name = "id") int id,
                                                    @GraphQLEnvironment() Set<String> fields) {
        return aggregationService.get(id, getFetchOptions(fields));
    }

    @GraphQLQuery(name = "aggregationTypes", description = "Get all available aggregation types")
    public List<AggregationTypeVO> getAllAggregationTypes(@GraphQLArgument(name = "filter") AggregationTypeFilterVO filter,
                                                          @GraphQLEnvironment() Set<String> fields) {
        return aggregationService.findByFilter(filter, getFetchOptions(fields));
    }

    @GraphQLQuery(name = "aggregationRows", description = "Read an aggregation")
    @IsUser
    public AggregationResultVO getAggregationRows(@GraphQLArgument(name = "type") AggregationTypeVO type,
                                                  @GraphQLArgument(name = "filter") ExtractionFilterVO filter,
                                                  @GraphQLArgument(name = "strata") AggregationStrataVO strata,
                                                  @GraphQLArgument(name = "offset", defaultValue = "0") Integer offset,
                                                  @GraphQLArgument(name = "size", defaultValue = "1000") Integer size,
                                                  @GraphQLArgument(name = "sortBy") String sort,
                                                  @GraphQLArgument(name = "sortDirection", defaultValue = "asc") String direction) {

        return aggregationService.read(type, filter, strata, offset, size, sort, SortDirection.fromString(direction));
    }

    @GraphQLQuery(name = "aggregationColumns", description = "Read columns from aggregation")
    //@IsUser
    public List<ExtractionTableColumnVO> getAggregationColumns(@GraphQLArgument(name = "type") AggregationTypeVO type,
                                                               @GraphQLArgument(name = "sheet") String sheet) {

        return aggregationService.getColumnsBySheetName(type, sheet);
    }


    @GraphQLQuery(name = "aggregationGeoJson", description = "Execute an aggregation and return as GeoJson")
    // TODO: enable auth ?
    //@IsUser
    public Object getGeoJsonAggregation(@GraphQLArgument(name = "type") AggregationTypeVO type,
                                        @GraphQLArgument(name = "filter") ExtractionFilterVO filter,
                                        @GraphQLArgument(name = "strata") AggregationStrataVO strata,
                                        @GraphQLArgument(name = "offset", defaultValue = "0") Integer offset,
                                        @GraphQLArgument(name = "size", defaultValue = "1000") Integer size,
                                        @GraphQLArgument(name = "sortBy") String sort,
                                        @GraphQLArgument(name = "sortDirection", defaultValue = "asc") String direction) {
        strata = (strata == null) ? new AggregationStrataVO() : strata;
        filter = filter == null ? new ExtractionFilterVO() : filter;

        // Fill default values for strata
        if (strata.getSpaceColumnName() == null) {
            strata.setSpaceColumnName(AggRdbSpecification.COLUMN_SQUARE);
        }
        if (strata.getTimeColumnName() == null){
            strata.setTimeColumnName(AggRdbSpecification.COLUMN_YEAR);
        }

        return GeoJsonExtractions.toFeatureCollection(
                getAggregationRows(type, filter, strata, offset, size, sort, direction),
                strata.getSpaceColumnName()
        );
    }

    @GraphQLMutation(name = "saveAggregation", description = "Create or update a data aggregation")
    @IsSupervisor
    public AggregationTypeVO saveAggregation(@GraphQLArgument(name = "type") AggregationTypeVO type,
                                             @GraphQLArgument(name = "filter") ExtractionFilterVO filter
    ) {
        return aggregationService.save(type, filter);
    }

    @GraphQLMutation(name = "deleteAggregations", description = "Delete some aggregations")
    @IsSupervisor
    public void deleteAggregations(@GraphQLArgument(name = "ids") int[] id) {
        Arrays.stream(id).forEach(aggregationService::delete);
    }

//    @GraphQLQuery(name = "aggregation", description = "Execute and read an aggregation")
//    @IsSupervisor
//    public AggregationResultVO getAggregation(@GraphQLArgument(name = "type") AggregationTypeVO type,
//                                              @GraphQLArgument(name = "filter") ExtractionFilterVO filter,
//                                              @GraphQLArgument(name = "strata") AggregationStrataVO strata,
//                                              @GraphQLArgument(name = "offset", defaultValue = "0") Integer offset,
//                                              @GraphQLArgument(name = "size", defaultValue = "1000") Integer size,
//                                              @GraphQLArgument(name = "sortBy") String sort,
//                                              @GraphQLArgument(name = "sortDirection", defaultValue = "asc") String direction) {
//
//        return aggregationService.executeAndRead(type, filter, strata, offset, size, sort, SortDirection.fromString(direction));
//    }

    protected ExtractionProductFetchOptions getFetchOptions(Set<String> fields) {
        return ExtractionProductFetchOptions.builder()
                .withRecorderDepartment(fields.contains(StringUtils.slashing(IWithRecorderDepartmentEntity.Fields.RECORDER_DEPARTMENT, IEntity.Fields.ID)))
                .withRecorderPerson(fields.contains(StringUtils.slashing(IWithRecorderPersonEntity.Fields.RECORDER_PERSON, IEntity.Fields.ID)))
                // Tables (=sheets)
                .withTables(fields.contains(AggregationTypeVO.PROPERTY_SHEET_NAMES))
                // Columns not need
                .withColumns(false)
                // Stratum
                .withStratum(fields.contains(AggregationTypeVO.PROPERTY_STRATUM))

                .build();
    }

}

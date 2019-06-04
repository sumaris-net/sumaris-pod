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
import io.leangen.graphql.annotations.GraphQLQuery;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.extraction.service.AggregationService;
import net.sumaris.core.extraction.vo.*;
import net.sumaris.server.http.geojson.extraction.GeoJsonExtractions;
import net.sumaris.server.http.security.IsUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AggregationGraphQLService {

    @Autowired
    private AggregationService aggregationService;

    /* -- aggregation service -- */

    @GraphQLQuery(name = "aggregationTypes", description = "Get all available aggregation types")
    public List<AggregationTypeVO> getAllAggregationTypes() {
        return aggregationService.getAllAggregationTypes();
    }

    @GraphQLQuery(name = "aggregation", description = "Execute a aggregation")
    @IsUser
    public AggregationResultVO aggregate(@GraphQLArgument(name = "type") AggregationTypeVO type,
                                                            @GraphQLArgument(name = "filter") ExtractionFilterVO filter,
                                                            @GraphQLArgument(name = "strata") AggregationStrataVO strata,
                                                            @GraphQLArgument(name = "offset", defaultValue = "0") Integer offset,
                                                            @GraphQLArgument(name = "size", defaultValue = "1000") Integer size,
                                                            @GraphQLArgument(name = "sortBy") String sort,
                                                            @GraphQLArgument(name = "sortDirection", defaultValue = "asc") String direction) {

        return aggregationService.executeAndRead(type, filter, strata, offset, size, sort, SortDirection.fromString(direction));
    }

    @GraphQLQuery(name = "geoJsonAggregation", description = "Execute an aggregation and return as GeoJson")
    @IsUser
    public Object getGeoJsonAggregation(@GraphQLArgument(name = "type") AggregationTypeVO type,
                                    @GraphQLArgument(name = "filter") ExtractionFilterVO filter,
                                    @GraphQLArgument(name = "strata") AggregationStrataVO strata,
                                    @GraphQLArgument(name = "offset", defaultValue = "0") Integer offset,
                                    @GraphQLArgument(name = "size", defaultValue = "1000") Integer size,
                                    @GraphQLArgument(name = "sortBy") String sort,
                                    @GraphQLArgument(name = "sortDirection", defaultValue = "asc") String direction) {
        if (strata == null) {
            strata = new AggregationStrataVO();
            strata.setSpace("square");
            strata.setTime("year");
        }
        if (filter == null) {
            filter = new ExtractionFilterVO();
        }
        return GeoJsonExtractions.toFeatureCollection(
                aggregate(type, filter, strata, offset, size, sort, direction)
        );
    }

}

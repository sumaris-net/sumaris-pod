package net.sumaris.server.http.rest;

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

import com.google.common.collect.ImmutableList;
import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLContext;
import io.leangen.graphql.annotations.GraphQLQuery;
import net.sumaris.core.dao.referential.location.Locations;
import net.sumaris.core.extraction.service.AggregationService;
import net.sumaris.core.extraction.service.ExtractionService;
import net.sumaris.core.extraction.vo.*;
import net.sumaris.core.util.StringUtils;
import net.sumaris.server.http.geojson.GeoJsonGeometries;
import net.sumaris.server.http.geojson.extraction.GeoJsonExtractions;
import org.geojson.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.websocket.server.PathParam;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class AggregationRestService {

    @Autowired
    private AggregationService aggregationService;

    @ResponseBody
    @RequestMapping(value = "aggregation/{type}/geojson", method = RequestMethod.GET,
            produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public FeatureCollection getGeoAggregation(@PathVariable(name = "type") String typeLabel,
                                               @PathParam(value = "offset") Integer offsetParam,
                                               @PathParam(value = "size") Integer sizeParam) {

        AggregationTypeVO type = new AggregationTypeVO();
        type.setLabel(typeLabel);

        ExtractionFilterVO filter = null;
        AggregationStrataVO strata = null;

        int offset = offsetParam != null ? offsetParam.intValue() : 0;
        int size = sizeParam != null ? sizeParam.intValue() : 100;

        // Limit to 1000 rows
        if (size > 1000) size = 1000;

        return GeoJsonExtractions.toFeatureCollection(aggregationService.executeAndRead(
                type,
                filter,
                strata,
                offset, size,
                null, null));
    }


}

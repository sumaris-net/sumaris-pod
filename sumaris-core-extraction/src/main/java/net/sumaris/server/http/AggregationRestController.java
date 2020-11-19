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

package net.sumaris.server.http;

import net.sumaris.core.exception.ErrorCodes;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.extraction.format.specification.AggRdbSpecification;
import net.sumaris.core.extraction.service.AggregationService;
import net.sumaris.core.extraction.vo.AggregationStrataVO;
import net.sumaris.core.extraction.vo.AggregationTypeVO;
import net.sumaris.core.model.technical.extraction.ExtractionCategoryEnum;
import net.sumaris.core.extraction.vo.ExtractionFilterVO;
import net.sumaris.server.config.ExtractionWebAutoConfiguration;
import net.sumaris.server.security.ExtractionSecurityService;
import net.sumaris.server.geojson.ExtractionGeoJsonConverter;
import net.sumaris.server.util.QueryParamUtils;
import org.apache.commons.lang3.StringUtils;
import org.geojson.FeatureCollection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;


@RestController
@ConditionalOnBean({ExtractionWebAutoConfiguration.class})
public class AggregationRestController {

    protected static final String BASE_PATH = "/api/extraction/product";
    protected static final String GEOJSON_LABEL_PATH = BASE_PATH + "/{label:[a-zA-Z0-9-_]+}";
    protected static final String GEOJSON_LABEL_WITH_SPACE_PATH = GEOJSON_LABEL_PATH + "/{space}";

    protected static final String GEOJSON_EXTENSION = ".geojson";

    @Autowired
    private AggregationService aggregationService;

    @Autowired
    private ExtractionGeoJsonConverter geoJsonConverter;

    @Autowired
    private ExtractionSecurityService securityService;

    @ResponseBody
    @RequestMapping(value = {
                        GEOJSON_LABEL_PATH,
                        GEOJSON_LABEL_PATH + GEOJSON_EXTENSION,
                        GEOJSON_LABEL_WITH_SPACE_PATH,
                        GEOJSON_LABEL_WITH_SPACE_PATH + GEOJSON_EXTENSION,
                    },
                    method = RequestMethod.GET,
                    produces = {
                        MediaType.APPLICATION_JSON_UTF8_VALUE,
                        MediaType.APPLICATION_JSON_VALUE
                })
    public FeatureCollection getGeoAggregation(@PathVariable(name = "label") String label,
                                               @PathVariable(name = "space", required = false) String spaceStrata,
                                               @RequestParam(value = "offset", required = false) Integer offsetParam,
                                               @RequestParam(value = "size", required = false) Integer sizeParam,
                                               @RequestParam(value = "time", required = false) String timeStrata,
                                               @RequestParam(value = "agg", required = false) String aggStrata,
                                               @RequestParam(value = "q", required = false) String queryString) {

        AggregationTypeVO type = new AggregationTypeVO();
        type.setLabel(label);
        type.setCategory(ExtractionCategoryEnum.PRODUCT);

        // Check access right
        securityService.checkReadAccess(type);

        ExtractionFilterVO filter;
        try {
            filter = QueryParamUtils.parseFilterQueryString(queryString);
        } catch (ParseException e) {
            throw new SumarisTechnicalException(ErrorCodes.BAD_REQUEST, "Invalid query: " + queryString);
        }

        int offset = offsetParam != null ? offsetParam.intValue() : 0;
        int size = sizeParam != null ? sizeParam.intValue() : 100;
        // Limit to 1000 rows
        if (size > 1000) size = 1000;

        AggregationStrataVO strata = new AggregationStrataVO();
        strata.setTimeColumnName(StringUtils.isNotBlank(timeStrata) ? timeStrata : AggRdbSpecification.COLUMN_YEAR);
        strata.setSpatialColumnName(StringUtils.isNotBlank(spaceStrata) ? spaceStrata : AggRdbSpecification.COLUMN_SQUARE);
        strata.setAggColumnName(StringUtils.isNotBlank(aggStrata) ? aggStrata : AggRdbSpecification.COLUMN_FISHING_TIME);
        strata.setTechColumnName(null);

        return geoJsonConverter.toFeatureCollection(aggregationService.read(
                type,
                filter,
                strata,
                offset, size,
                null, null),
                strata.getSpatialColumnName());
    }


}

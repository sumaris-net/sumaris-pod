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

package net.sumaris.extraction.server.http;

import net.sumaris.core.config.ExtractionAutoConfiguration;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.exception.ErrorCodes;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.model.technical.extraction.IExtractionType;
import net.sumaris.core.vo.technical.extraction.AggregationStrataVO;
import net.sumaris.core.vo.technical.extraction.ExtractionProductFetchOptions;
import net.sumaris.core.vo.technical.extraction.ExtractionProductVO;
import net.sumaris.extraction.core.service.ExtractionManager;
import net.sumaris.extraction.core.specification.data.trip.AggRdbSpecification;
import net.sumaris.extraction.core.vo.ExtractionFilterVO;
import net.sumaris.extraction.core.vo.ExtractionResultVO;
import net.sumaris.extraction.core.vo.ExtractionTypeVO;
import net.sumaris.extraction.server.geojson.ExtractionGeoJsonConverter;
import net.sumaris.extraction.server.security.ExtractionSecurityService;
import net.sumaris.extraction.server.util.QueryParamUtils;
import org.apache.commons.lang3.StringUtils;
import org.geojson.FeatureCollection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;


@RestController
@ConditionalOnBean({ExtractionAutoConfiguration.class})
@ConditionalOnWebApplication
public class AggregationRestController implements ExtractionRestPaths {

    @Autowired
    private ExtractionManager extractionManager;

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

        // Check type
        ExtractionProductVO product = getProductByExample(ExtractionTypeVO.builder()
            .label(label)
            .build(),
            ExtractionProductFetchOptions.builder()
            .withStratum(true)
            .build());

        checkIsSpatial(product);

        // Check access right
        securityService.checkReadAccess(product);

        ExtractionFilterVO filter;
        try {
            filter = QueryParamUtils.parseFilterQueryString(queryString);
        } catch (ParseException e) {
            throw new SumarisTechnicalException(ErrorCodes.BAD_REQUEST, "Invalid query: " + queryString);
        }

        int offset = offsetParam != null ? offsetParam : 0;
        int size = sizeParam != null ? sizeParam : 100;
        // Limit to 1000 rows
        if (size > 1000) size = 1000;

        AggregationStrataVO strata = new AggregationStrataVO();
        strata.setTimeColumnName(StringUtils.isNotBlank(timeStrata) ? timeStrata : AggRdbSpecification.COLUMN_YEAR);
        strata.setSpatialColumnName(StringUtils.isNotBlank(spaceStrata) ? spaceStrata : AggRdbSpecification.COLUMN_SQUARE);
        strata.setAggColumnName(StringUtils.isNotBlank(aggStrata) ? aggStrata : AggRdbSpecification.COLUMN_FISHING_TIME);
        strata.setTechColumnName(null);

        ExtractionResultVO result = extractionManager.executeAndRead(product, filter, strata,
            Page.builder()
                .offset(offset)
                .size(size)
                .build());

        return geoJsonConverter.toFeatureCollection(result, strata.getSpatialColumnName());
    }

    protected ExtractionProductVO getProductByExample(IExtractionType source, ExtractionProductFetchOptions fetchOptions) {
        IExtractionType checkedType = extractionManager.getByExample(source, fetchOptions);

        if (!(checkedType instanceof ExtractionProductVO)) throw new SumarisTechnicalException("Not a product extraction");

        return (ExtractionProductVO)checkedType;
    }

    protected void checkIsSpatial(ExtractionProductVO target) {
        if (!target.getIsSpatial()) throw new SumarisTechnicalException("Not a spatial product");
    }
}

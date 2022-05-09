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

package net.sumaris.extraction.server.graphql;

import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLQuery;
import net.sumaris.core.config.ExtractionAutoConfiguration;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.model.technical.extraction.IExtractionType;
import net.sumaris.core.util.Beans;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.technical.extraction.*;
import net.sumaris.extraction.core.service.AggregationService;
import net.sumaris.extraction.core.service.ExtractionManager;
import net.sumaris.extraction.core.service.ExtractionProductService;
import net.sumaris.extraction.core.vo.*;
import net.sumaris.extraction.server.geojson.ExtractionGeoJsonConverter;
import net.sumaris.extraction.server.security.ExtractionSecurityService;
import net.sumaris.server.http.graphql.GraphQLApi;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@GraphQLApi
@Service
@Transactional
@ConditionalOnBean({ExtractionAutoConfiguration.class})
@ConditionalOnWebApplication
public class AggregationGraphQLService {

    private final ExtractionManager extractionManager;
    private final AggregationService aggregationService;
    private final ExtractionProductService productService;
    private final ExtractionSecurityService securityService;
    private final ExtractionGeoJsonConverter geoJsonConverter;

    public AggregationGraphQLService(ExtractionManager extractionManager,
                                     AggregationService aggregationService,
                                     ExtractionProductService productService,
                                     ExtractionSecurityService securityService,
                                     ExtractionGeoJsonConverter geoJsonConverter) {
        this.extractionManager = extractionManager;
        this.aggregationService = aggregationService;
        this.productService = productService;
        this.securityService = securityService;
        this.geoJsonConverter = geoJsonConverter;
    }

    /* -- aggregation service -- */

    @GraphQLQuery(name = "aggregationRows", description = "Read an aggregation")
    @Transactional(readOnly = true)
    public AggregationResultVO getAggregationRows(@GraphQLArgument(name = "type") ExtractionTypeVO type,
                                                  @GraphQLArgument(name = "filter") ExtractionFilterVO filter,
                                                  @GraphQLArgument(name = "strata") AggregationStrataVO strata,
                                                  @GraphQLArgument(name = "offset", defaultValue = "0") Integer offset,
                                                  @GraphQLArgument(name = "size", defaultValue = "1000") Integer size,
                                                  @GraphQLArgument(name = "sortBy") String sort,
                                                  @GraphQLArgument(name = "sortDirection", defaultValue = "asc") String direction) {

        // Check type
        ExtractionProductVO product = getByExample(type);

        checkIsSpatial(product);

        // Check access right
        securityService.checkReadAccess(product);

        return aggregationService.readBySpace(product, filter, strata, Page.builder()
            .offset(offset)
            .size(size)
            .sortBy(sort)
            .sortDirection(SortDirection.fromString(direction))
            .build());
    }



    @GraphQLQuery(name = "aggregationGeoJson", description = "Execute an aggregation and return as GeoJson")
    @Transactional(readOnly = true)
    public Object getGeoJsonAggregation(@GraphQLArgument(name = "type") ExtractionTypeVO type,
                                        @GraphQLArgument(name = "filter") ExtractionFilterVO filter,
                                        @GraphQLArgument(name = "strata") AggregationStrataVO strata,
                                        @GraphQLArgument(name = "offset", defaultValue = "0") Integer offset,
                                        @GraphQLArgument(name = "size", defaultValue = "1000") Integer size,
                                        @GraphQLArgument(name = "sortBy") String sort,
                                        @GraphQLArgument(name = "sortDirection", defaultValue = "asc") String direction) {

        // Check type
        ExtractionProductVO product = getByExample(type, ExtractionProductFetchOptions.builder()
            .withStratum(true)
            .build());

        checkIsSpatial(product);

        // Check access right
        securityService.checkReadAccess(product);

        // Use the first product's strata, as default
        if ((strata == null || strata.getSpatialColumnName() == null || strata.getTimeColumnName() == null)
            && CollectionUtils.isNotEmpty(product.getStratum())) {

            // Get a strata, to use by default
            final String sheetName = strata != null ? strata.getSheetName() : (filter != null ? filter.getSheetName() : null);
            AggregationStrataVO defaultStrata = product.getStratum().stream()
                .filter(s -> sheetName == null || sheetName.equalsIgnoreCase(s.getSheetName()))
                .findFirst()
                .orElseThrow(() -> new SumarisTechnicalException(String.format("Unknown sheetName '%s' in type '%s'", sheetName, type.getLabel())));

            // Apply default strata, if need
            if (strata == null) {
                strata = new AggregationStrataVO();
                Beans.copyProperties(defaultStrata, strata);
            }

            else {
                if (strata.getSpatialColumnName() == null) {
                    strata.setSpatialColumnName(defaultStrata.getSpatialColumnName());
                }
                if (strata.getTimeColumnName() == null){
                    strata.setTimeColumnName(defaultStrata.getTimeColumnName());
                }
            }
        }

        // Make sure strata has been filled
        if (strata == null || strata.getSpatialColumnName() == null) throw new SumarisTechnicalException(String.format("No strata or spatial column found, in type '%s'", type.getLabel()));

        // Sort by spatial column, by default
        // This is import, to get all pages
        if (StringUtils.isBlank(sort)) {
            sort = strata.getSpatialColumnName();
        }

        // Get data
        AggregationResultVO data = aggregationService.readBySpace(product, filter, strata, Page.builder()
            .offset(offset)
            .size(size)
            .sortBy(sort)
            .sortDirection(SortDirection.fromString(direction, SortDirection.ASC))
            .build());


        // Convert to GeoJSON
        return geoJsonConverter.toFeatureCollection(data, strata.getSpatialColumnName());
    }

    @GraphQLQuery(name = "aggregationTech", description = "Execute an aggregation and return as GeoJson")
    @Transactional(readOnly = true)
    public AggregationTechResultVO getAggregationByTech(@GraphQLArgument(name = "type") ExtractionTypeVO type,
                                                        @GraphQLArgument(name = "filter") ExtractionFilterVO filter,
                                                        @GraphQLArgument(name = "strata") AggregationStrataVO strata,
                                                        @GraphQLArgument(name = "sortBy") String sort,
                                                        @GraphQLArgument(name = "sortDirection", defaultValue = "asc") String direction) {

        // Check type
        ExtractionProductVO product = getByExample(type);

        checkIsSpatial(product);

        // Check access right
        securityService.checkReadAccess(product);

        return aggregationService.readByTech(product, filter, strata, sort, SortDirection.fromString(direction));
    }

    @GraphQLQuery(name = "aggregationTechMinMax", description = "Execute an aggregation and return as GeoJson")
    @Transactional(readOnly = true)
    public MinMaxVO getAggregationByTech(@GraphQLArgument(name = "type") ExtractionTypeVO type,
                                         @GraphQLArgument(name = "filter") ExtractionFilterVO filter,
                                         @GraphQLArgument(name = "strata") AggregationStrataVO strata) {

        // Check type
        ExtractionProductVO product = getByExample(type);

        checkIsSpatial(product);

        // Check access right
        securityService.checkReadAccess(product);

        return aggregationService.getTechMinMax(product, filter, strata);
    }


    protected void checkIsSpatial(ExtractionProductVO target) {

        if (!target.getIsSpatial()) throw new SumarisTechnicalException("Not a spatial product");
    }

    protected ExtractionProductVO getByExample(IExtractionType source) {
        return getByExample(source, ExtractionProductFetchOptions.TABLES_AND_RECORDER);
    }

    protected ExtractionProductVO getByExample(IExtractionType source, ExtractionProductFetchOptions fetchOptions) {
        IExtractionType checkedType = extractionManager.getByExample(source, fetchOptions);

        if (!(checkedType instanceof ExtractionProductVO)) throw new SumarisTechnicalException("Not a product extraction");

        return (ExtractionProductVO)checkedType;
    }
}

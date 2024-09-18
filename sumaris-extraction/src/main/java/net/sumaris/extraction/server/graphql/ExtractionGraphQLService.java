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

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;
import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLNonNull;
import io.leangen.graphql.annotations.GraphQLQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.config.ExtractionAutoConfiguration;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.dao.technical.cache.CacheTTL;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.model.technical.extraction.IExtractionType;
import net.sumaris.core.util.Beans;
import net.sumaris.core.util.MapUtils;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.technical.extraction.AggregationStrataVO;
import net.sumaris.core.vo.technical.extraction.ExtractionProductFetchOptions;
import net.sumaris.core.vo.technical.extraction.ExtractionProductVO;
import net.sumaris.extraction.core.service.ExtractionService;
import net.sumaris.extraction.core.util.ExtractionTypes;
import net.sumaris.extraction.core.vo.*;
import net.sumaris.extraction.server.geojson.ExtractionGeoJsonConverter;
import net.sumaris.extraction.server.security.ExtractionSecurityService;
import net.sumaris.server.http.graphql.GraphQLApi;
import net.sumaris.server.security.IFileController;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
@GraphQLApi
@ConditionalOnBean({ExtractionAutoConfiguration.class})
@ConditionalOnWebApplication
@Slf4j
public class ExtractionGraphQLService {

    private final ExtractionSecurityService extractionSecurityService;
    private final ExtractionService extractionService;
    private final IFileController fileController;
    private final ExtractionGeoJsonConverter geoJsonConverter;

    @GraphQLQuery(name = "extractionRows", description = "Preview some extraction rows")
    public ExtractionResultVO read(@GraphQLNonNull @GraphQLArgument(name = "type") ExtractionTypeVO type,
                                   @GraphQLArgument(name = "filter") ExtractionFilterVO filter,
                                   @GraphQLArgument(name = "strata") AggregationStrataVO strata,
                                   @GraphQLArgument(name = "offset", defaultValue = "0") Integer offset,
                                   @GraphQLArgument(name = "size", defaultValue = "100") Integer size,
                                   @GraphQLArgument(name = "sortBy") String sort,
                                   @GraphQLArgument(name = "sortDirection", defaultValue = "asc") String direction,
                                   @GraphQLArgument(name = "cacheDuration") String cacheDuration
    ) {
        Preconditions.checkNotNull(type, "Argument 'type' must not be null.");
        Preconditions.checkNotNull(offset, "Argument 'offset' must not be null.");
        Preconditions.checkNotNull(size, "Argument 'size' must not be null.");

        IExtractionType checkedType = extractionService.getByExample(type);

        extractionSecurityService.checkReadAccess(checkedType);

        Page page = Page.builder()
            .offset(offset)
            .size(size)
            .sortBy(sort)
            .sortDirection(SortDirection.fromString(direction, SortDirection.ASC))
            .build();

        // Read product
        if (ExtractionTypes.isProduct(type)) {
            ExtractionResultVO result = extractionService.read(checkedType, filter, strata, page,
                CacheTTL.fromString(cacheDuration));
            result.setType(new ExtractionTypeVO(checkedType));
            return result;
        }
        // Live extraction
        else {
            try {
                ExtractionResultVO result = extractionService.executeAndRead(checkedType, filter, strata, page,
                    CacheTTL.fromString(cacheDuration));
                result.setType(new ExtractionTypeVO(checkedType));
                return result;
            } catch (Throwable t) {
                log.error(t.getMessage(), t);
                throw t;
            }
        }
    }

    @GraphQLQuery(name = "extraction", description = "Read extraction data")
    public JsonNode readAsJson(@GraphQLNonNull @GraphQLArgument(name = "type") ExtractionTypeVO type,
                               @GraphQLArgument(name = "filter") ExtractionFilterVO filter,
                               @GraphQLArgument(name = "strata") AggregationStrataVO strata,
                               @GraphQLArgument(name = "offset", defaultValue = "0") Integer offset,
                               @GraphQLArgument(name = "size", defaultValue = "1000") Integer size,
                               @GraphQLArgument(name = "sortBy") String sort,
                               @GraphQLArgument(name = "sortDirection", defaultValue = "asc") String direction,
                               @GraphQLArgument(name = "cacheDuration") String cacheDuration
    ) {
        Preconditions.checkNotNull(type, "Argument 'type' must not be null.");
        Preconditions.checkNotNull(offset, "Argument 'offset' must not be null.");
        Preconditions.checkNotNull(size, "Argument 'size' must not be null.");

        IExtractionType checkedType = extractionService.getByExample(type);

        // Check access rights
        //extractionSecurityService.checkReadAccess(checkedType);

        Page page = Page.builder()
            .offset(offset)
            .size(size)
            .sortBy(sort)
            .sortDirection(SortDirection.fromString(direction))
            .build();

        boolean useMapResult = CollectionUtils.isNotEmpty(filter.getSheetNames());

        // If one one sheetname, force to use 'sheetName' instead of 'sheetNames'
        if (CollectionUtils.size(filter.getSheetNames()) == 1 && filter.getSheetName() == null){
            filter.setSheetName(filter.getSheetNames().iterator().next());
            filter.setSheetNames(null);
        }

        boolean hasManySheetNames = CollectionUtils.size(filter.getSheetNames()) > 1;

        CacheTTL ttl = CacheTTL.fromString(cacheDuration);

        // Read product
        if (ExtractionTypes.isProduct(type)) {
            // Many sheetNames
            if (hasManySheetNames) {
                Map<String, ExtractionResultVO> data = extractionService.readMany(checkedType, filter, strata, page, ttl);
                return extractionService.toJsonMap(data);
            }
            // Single sheet name (=preview mode)
            else {
                ExtractionResultVO data = extractionService.read(checkedType, filter, strata, page, ttl);
                if (useMapResult) {
                    return extractionService.toJsonMap(MapUtils.of(filter.getSheetName(), data));
                } else {
                    return extractionService.toJsonArray(data);
                }
            }
        }
        // Live extraction
        else {
            // Many sheetNames
            if (hasManySheetNames) {
                Map<String, ExtractionResultVO> data = extractionService.executeAndReadMany(checkedType, filter, strata, page, ttl);
                return extractionService.toJsonMap(data);
            }
            // Single sheet name (=preview mode)
            else {
                ExtractionResultVO data = extractionService.executeAndRead(checkedType, filter, strata, page, ttl);
                if (useMapResult) {
                    return extractionService.toJsonMap(MapUtils.of(filter.getSheetName(), data));
                } else {
                    return extractionService.toJsonArray(data);
                }
            }
        }
    }

    @GraphQLQuery(name = "extractionFile", description = "Extract data into a file")
    public String getExtractionFile(@GraphQLNonNull @GraphQLArgument(name = "type") ExtractionTypeVO type,
                                    @GraphQLArgument(name = "filter") ExtractionFilterVO filter,
                                    @GraphQLArgument(name = "strata") AggregationStrataVO strata
    ) throws IOException {
        Preconditions.checkNotNull(type, "Argument 'type' must not be null.");

        extractionSecurityService.checkReadAccess(type);

        File tempFile = extractionService.executeAndDump(type, filter, strata);

        // Add to download controller
        String filePath = fileController.registerFile(tempFile, true);

       return filePath;
    }

    @GraphQLQuery(name = "aggregationGeoJson", description = "Execute an aggregation and return as GeoJson")
    public Object getGeoJsonAggregation(@GraphQLNonNull @GraphQLArgument(name = "type") ExtractionTypeVO type,
                                        @GraphQLArgument(name = "filter") ExtractionFilterVO filter,
                                        @GraphQLArgument(name = "strata") AggregationStrataVO strata,
                                        @GraphQLArgument(name = "offset", defaultValue = "0") Integer offset,
                                        @GraphQLArgument(name = "size", defaultValue = "1000") Integer size,
                                        @GraphQLArgument(name = "sortBy") String sort,
                                        @GraphQLArgument(name = "sortDirection", defaultValue = "asc") String direction,
                                        @GraphQLArgument(name = "cacheDuration") String cacheDuration) {

        // Check type
        ExtractionProductVO checkedType = getProductByExample(type, ExtractionProductFetchOptions.builder()
            .withStratum(true)
            .build());

        checkIsSpatial(checkedType);

        // Check access right
        extractionSecurityService.checkReadAccess(checkedType);

        // Use the first product's strata, as default
        if ((strata == null || strata.getSpatialColumnName() == null || strata.getTimeColumnName() == null)
            && CollectionUtils.isNotEmpty(checkedType.getStratum())) {

            // Get a strata, to use by default
            final String sheetName = strata != null ? strata.getSheetName() : (filter != null ? filter.getSheetName() : null);
            AggregationStrataVO defaultStrata = checkedType.getStratum().stream()
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
        if (strata == null || strata.getSpatialColumnName() == null) {
            throw new SumarisTechnicalException(String.format("No strata or spatial column found, in type '%s'", type.getLabel()));
        }

        // Sort by spatial column, by default
        // This is import, to get all pages
        if (StringUtils.isBlank(sort)) {
            sort = strata.getSpatialColumnName();
        }

        Page page = Page.builder()
            .offset(offset)
            .size(size)
            .sortBy(sort)
            .sortDirection(SortDirection.fromString(direction, SortDirection.ASC))
            .build();

        // Read data
        ExtractionResultVO data = extractionService.read(checkedType, filter, strata, page,
            CacheTTL.fromString(cacheDuration));

        // Convert to GeoJSON
        return geoJsonConverter.toFeatureCollection(data, strata.getSpatialColumnName());
    }

    @GraphQLQuery(name = "aggregationTech", description = "Execute an aggregation and return as GeoJson")
    public AggregationTechResultVO readByTech(@GraphQLNonNull @GraphQLArgument(name = "type") ExtractionTypeVO type,
                                              @GraphQLArgument(name = "filter") ExtractionFilterVO filter,
                                              @GraphQLArgument(name = "strata") AggregationStrataVO strata,
                                              @GraphQLArgument(name = "sortBy") String sort,
                                              @GraphQLArgument(name = "sortDirection", defaultValue = "asc") String direction) {

        // Check type
        ExtractionProductVO checkedType = getProductByExample(type);

        checkIsSpatial(checkedType);

        // Check access right
        extractionSecurityService.checkReadAccess(checkedType);

        return extractionService.readByTech(checkedType, filter, strata, sort, SortDirection.fromString(direction));
    }

    @GraphQLQuery(name = "aggregationTechMinMax", description = "Execute an aggregation and return as GeoJson")
    public MinMaxVO getTechMinMax(@GraphQLNonNull @GraphQLArgument(name = "type") ExtractionTypeVO type,
                                  @GraphQLArgument(name = "filter") ExtractionFilterVO filter,
                                  @GraphQLArgument(name = "strata") AggregationStrataVO strata) {

        // Check type
        ExtractionProductVO checkedType = getProductByExample(type);

        checkIsSpatial(checkedType);

        // Check access right
        extractionSecurityService.checkReadAccess(checkedType);

        return extractionService.getTechMinMax(checkedType, filter, strata);
    }


    /* -- protected methods -- */

    protected ExtractionProductVO getProductByExample(IExtractionType source) {
        return getProductByExample(source, ExtractionProductFetchOptions.TABLES_AND_RECORDER);
    }

    protected void checkIsSpatial(ExtractionProductVO target) {
        if (!target.getIsSpatial()) {
            throw new SumarisTechnicalException("Not a spatial product");
        }
    }

    protected ExtractionProductVO getProductByExample(IExtractionType source, ExtractionProductFetchOptions fetchOptions) {
        IExtractionType checkedType = extractionService.getByExample(source, fetchOptions);

        if (!(checkedType instanceof ExtractionProductVO)) {
            throw new SumarisTechnicalException("Not a product extraction");
        }

        return (ExtractionProductVO)checkedType;
    }
}

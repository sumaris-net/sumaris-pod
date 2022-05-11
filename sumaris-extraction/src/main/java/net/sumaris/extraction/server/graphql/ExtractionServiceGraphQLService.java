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

import com.google.common.base.Preconditions;
import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLQuery;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.config.ExtractionAutoConfiguration;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.dao.technical.cache.CacheTTL;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.model.technical.extraction.IExtractionType;
import net.sumaris.core.util.Beans;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.technical.extraction.AggregationStrataVO;
import net.sumaris.core.vo.technical.extraction.ExtractionProductFetchOptions;
import net.sumaris.core.vo.technical.extraction.ExtractionProductVO;
import net.sumaris.core.vo.technical.extraction.ExtractionTableColumnVO;
import net.sumaris.extraction.core.service.ExtractionService;
import net.sumaris.extraction.core.vo.*;
import net.sumaris.extraction.server.geojson.ExtractionGeoJsonConverter;
import net.sumaris.extraction.server.security.ExtractionSecurityService;
import net.sumaris.server.http.graphql.GraphQLApi;
import net.sumaris.server.security.IDownloadController;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
@GraphQLApi
@ConditionalOnBean({ExtractionAutoConfiguration.class})
@ConditionalOnWebApplication
@Slf4j
public class ExtractionServiceGraphQLService {

    private final ExtractionSecurityService extractionSecurityService;
    private final ExtractionService extractionService;
    private final IDownloadController downloadController;
    private final ExtractionGeoJsonConverter geoJsonConverter;

    public ExtractionServiceGraphQLService(
        IDownloadController downloadController,
        ExtractionSecurityService extractionSecurityService,
        ExtractionService extractionService,
        ExtractionGeoJsonConverter geoJsonConverter) {

        this.downloadController = downloadController;
        this.extractionSecurityService = extractionSecurityService;
        this.extractionService = extractionService;
        this.geoJsonConverter = geoJsonConverter;
    }

    @GraphQLQuery(name = "extractionRows", description = "Preview some extraction rows")
    @Transactional
    public ExtractionResultVO readExtractionRows(@GraphQLArgument(name = "type") ExtractionTypeVO type,
                                                 @GraphQLArgument(name = "filter") ExtractionFilterVO filter,
                                                 @GraphQLArgument(name = "strata") AggregationStrataVO strata,
                                                 @GraphQLArgument(name = "offset", defaultValue = "0") Integer offset,
                                                 @GraphQLArgument(name = "size", defaultValue = "100") Integer size,
                                                 @GraphQLArgument(name = "sortBy") String sort,
                                                 @GraphQLArgument(name = "sortDirection", defaultValue = "asc") String direction,
                                                 @GraphQLArgument(name = "cacheDuration") String cacheDuration
    ) {
        Preconditions.checkNotNull(type, "Argument 'type' must not be null.");
        Preconditions.checkNotNull(type.getLabel(), "Argument 'type.label' must not be null.");
        Preconditions.checkNotNull(offset, "Argument 'offset' must not be null.");
        Preconditions.checkNotNull(size, "Argument 'size' must not be null.");

        extractionSecurityService.checkReadAccess(type);

        return extractionService.executeAndRead(type, filter, strata, Page.builder()
            .offset(offset)
            .size(size)
            .sortBy(sort)
            .sortDirection(SortDirection.fromString(direction, SortDirection.ASC))
            .build(),
            cacheDuration != null ? CacheTTL.fromString(cacheDuration) : null);
    }

    @GraphQLQuery(name = "extraction", description = "Read extraction data")
    @Transactional
    public List<Map<String, String>> readExtractionMap(@GraphQLArgument(name = "type") ExtractionTypeVO type,
                                                       @GraphQLArgument(name = "filter") ExtractionFilterVO filter,
                                                       @GraphQLArgument(name = "strata") AggregationStrataVO strata,
                                                       @GraphQLArgument(name = "offset", defaultValue = "0") Integer offset,
                                                       @GraphQLArgument(name = "size", defaultValue = "1000") Integer size,
                                                       @GraphQLArgument(name = "sortBy") String sort,
                                                       @GraphQLArgument(name = "sortDirection", defaultValue = "asc") String direction,
                                                       @GraphQLArgument(name = "cacheDuration") String cacheDuration
    ) {
        Preconditions.checkNotNull(type, "Argument 'type' must not be null.");
        Preconditions.checkNotNull(type.getLabel(), "Argument 'type.label' must not be null.");
        Preconditions.checkNotNull(offset, "Argument 'offset' must not be null.");
        Preconditions.checkNotNull(size, "Argument 'size' must not be null.");

        extractionSecurityService.checkReadAccess(type);

        Page page = Page.builder()
            .offset(offset)
            .size(size)
            .sortBy(sort)
            .sortDirection(SortDirection.fromString(direction))
            .build();

        ExtractionResultVO resultVO = extractionService.executeAndRead(type, filter, strata, page,
            cacheDuration != null ? CacheTTL.fromString(cacheDuration) : null);

        return toJsonArray(resultVO);
    }

    @GraphQLQuery(name = "extractionFile", description = "Extract data into a file")
    @Transactional(timeout = 10000000)
    public String getExtractionFile(@GraphQLArgument(name = "type") ExtractionTypeVO type,
                                    @GraphQLArgument(name = "filter") ExtractionFilterVO filter,
                                    @GraphQLArgument(name = "strata") AggregationStrataVO strata
    ) throws IOException {
        Preconditions.checkNotNull(type, "Argument 'type' must not be null.");
        Preconditions.checkNotNull(type.getLabel(), "Argument 'type.label' must not be null.");

        extractionSecurityService.checkReadAccess(type);

        File tempFile = extractionService.executeAndDump(type, filter, strata);

        // Add to download controller
        String filePath = downloadController.registerFile(tempFile, true);

       return filePath;
    }


    @GraphQLQuery(name = "aggregationRows", description = "Read an aggregation")
    @Transactional(readOnly = true)
    public ExtractionResultVO readAggregation(@GraphQLArgument(name = "type") ExtractionTypeVO type,
                                              @GraphQLArgument(name = "filter") ExtractionFilterVO filter,
                                              @GraphQLArgument(name = "strata") AggregationStrataVO strata,
                                              @GraphQLArgument(name = "offset", defaultValue = "0") Integer offset,
                                              @GraphQLArgument(name = "size", defaultValue = "1000") Integer size,
                                              @GraphQLArgument(name = "sortBy") String sort,
                                              @GraphQLArgument(name = "sortDirection", defaultValue = "asc") String direction,
                                              @GraphQLArgument(name = "cacheDuration") String cacheDuration) {

        // Check type
        ExtractionProductVO product = getProductByExample(type);

        checkIsSpatial(product);

        // Check access right
        extractionSecurityService.checkReadAccess(product);

        // Get data
        return extractionService.executeAndRead(product, filter, strata, Page.builder()
                .offset(offset)
                .size(size)
                .sortBy(sort)
                .sortDirection(SortDirection.fromString(direction, SortDirection.ASC))
                .build(),
            cacheDuration != null ? CacheTTL.fromString(cacheDuration) : null);
    }


    @GraphQLQuery(name = "aggregationGeoJson", description = "Execute an aggregation and return as GeoJson")
    @Transactional(readOnly = true)
    public Object getGeoJsonAggregation(@GraphQLArgument(name = "type") ExtractionTypeVO type,
                                        @GraphQLArgument(name = "filter") ExtractionFilterVO filter,
                                        @GraphQLArgument(name = "strata") AggregationStrataVO strata,
                                        @GraphQLArgument(name = "offset", defaultValue = "0") Integer offset,
                                        @GraphQLArgument(name = "size", defaultValue = "1000") Integer size,
                                        @GraphQLArgument(name = "sortBy") String sort,
                                        @GraphQLArgument(name = "sortDirection", defaultValue = "asc") String direction,
                                        @GraphQLArgument(name = "cacheDuration") String cacheDuration) {

        // Check type
        ExtractionProductVO product = getProductByExample(type, ExtractionProductFetchOptions.builder()
            .withStratum(true)
            .build());

        checkIsSpatial(product);

        // Check access right
        extractionSecurityService.checkReadAccess(product);

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
        ExtractionResultVO data = extractionService.executeAndRead(product, filter, strata, Page.builder()
                .offset(offset)
                .size(size)
                .sortBy(sort)
                .sortDirection(SortDirection.fromString(direction, SortDirection.ASC))
                .build(),
            cacheDuration != null ? CacheTTL.fromString(cacheDuration) : null);

        // Convert to GeoJSON
        return geoJsonConverter.toFeatureCollection(data, strata.getSpatialColumnName());
    }

    @GraphQLQuery(name = "aggregationTech", description = "Execute an aggregation and return as GeoJson")
    @Transactional(readOnly = true)
    public AggregationTechResultVO readByTech(@GraphQLArgument(name = "type") ExtractionTypeVO type,
                                              @GraphQLArgument(name = "filter") ExtractionFilterVO filter,
                                              @GraphQLArgument(name = "strata") AggregationStrataVO strata,
                                              @GraphQLArgument(name = "sortBy") String sort,
                                              @GraphQLArgument(name = "sortDirection", defaultValue = "asc") String direction) {

        // Check type
        ExtractionProductVO product = getProductByExample(type);

        checkIsSpatial(product);

        // Check access right
        extractionSecurityService.checkReadAccess(product);

        return extractionService.readByTech(product, filter, strata, sort, SortDirection.fromString(direction));
    }

    @GraphQLQuery(name = "aggregationTechMinMax", description = "Execute an aggregation and return as GeoJson")
    @Transactional(readOnly = true)
    public MinMaxVO getTechMinMax(@GraphQLArgument(name = "type") ExtractionTypeVO type,
                                  @GraphQLArgument(name = "filter") ExtractionFilterVO filter,
                                  @GraphQLArgument(name = "strata") AggregationStrataVO strata) {

        // Check type
        ExtractionProductVO product = getProductByExample(type);

        checkIsSpatial(product);

        // Check access right
        extractionSecurityService.checkReadAccess(product);

        return extractionService.getTechMinMax(product, filter, strata);
    }


    /* -- protected methods -- */

    protected List<Map<String, String>> toJsonArray(ExtractionResultVO source) {
        if (CollectionUtils.isNotEmpty(source.getColumns())) return null;

        String[] columnNames = source.getColumns().stream()
            .map(ExtractionTableColumnVO::getLabel)
            .toArray(String[]::new);

        return source.getRows()
            .stream().map(row -> {
                Map<String, String> rowMap = new LinkedHashMap<>();
                for (int i = 0; i < row.length; i++) {
                    rowMap.put(columnNames[i], row[i]);
                }
                return rowMap;
            }).collect(Collectors.toList());
    }

    protected ExtractionProductVO getProductByExample(IExtractionType source) {
        return getProductByExample(source, ExtractionProductFetchOptions.TABLES_AND_RECORDER);
    }

    protected void checkIsSpatial(ExtractionProductVO target) {
        if (!target.getIsSpatial()) throw new SumarisTechnicalException("Not a spatial product");
    }

    protected ExtractionProductVO getProductByExample(IExtractionType source, ExtractionProductFetchOptions fetchOptions) {
        IExtractionType checkedType = extractionService.getByExample(source, fetchOptions);

        if (!(checkedType instanceof ExtractionProductVO)) throw new SumarisTechnicalException("Not a product extraction");

        return (ExtractionProductVO)checkedType;
    }
}

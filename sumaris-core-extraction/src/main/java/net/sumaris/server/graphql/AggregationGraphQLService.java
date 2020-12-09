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

package net.sumaris.server.graphql;

import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLEnvironment;
import io.leangen.graphql.annotations.GraphQLMutation;
import io.leangen.graphql.annotations.GraphQLQuery;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.dao.technical.model.IEntity;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.extraction.service.AggregationService;
import net.sumaris.core.extraction.service.ExtractionProductService;
import net.sumaris.core.extraction.vo.*;
import net.sumaris.core.extraction.vo.filter.AggregationTypeFilterVO;
import net.sumaris.core.model.data.IWithRecorderDepartmentEntity;
import net.sumaris.core.model.data.IWithRecorderPersonEntity;
import net.sumaris.core.model.referential.StatusEnum;
import net.sumaris.core.util.Beans;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.technical.extraction.ExtractionProductFetchOptions;
import net.sumaris.core.vo.technical.extraction.ExtractionProductStrataVO;
import net.sumaris.core.vo.technical.extraction.ExtractionTableColumnFetchOptions;
import net.sumaris.core.vo.technical.extraction.ExtractionTableColumnVO;
import net.sumaris.server.config.ExtractionWebAutoConfiguration;
import net.sumaris.server.security.ExtractionSecurityService;
import net.sumaris.server.geojson.ExtractionGeoJsonConverter;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

@Service
@Transactional
@ConditionalOnBean({ExtractionWebAutoConfiguration.class})
public class AggregationGraphQLService {

    @Autowired
    private AggregationService aggregationService;

    @Autowired
    private ExtractionProductService productService;

    @Autowired
    private ExtractionSecurityService securityService;

    @Autowired
    private ExtractionGeoJsonConverter geoJsonConverter;

    /* -- aggregation service -- */

    @GraphQLQuery(name = "aggregationType", description = "Get one aggregation type")
    @Transactional(readOnly = true)
    public AggregationTypeVO getAggregationType(@GraphQLArgument(name = "id") int id,
                                                @GraphQLEnvironment() Set<String> fields) {
        securityService.checkReadAccess(id);
        return aggregationService.getTypeById(id, getFetchOptions(fields));
    }

    @GraphQLQuery(name = "aggregationTypes", description = "Get all available aggregation types")
    @Transactional(readOnly = true)
    public List<AggregationTypeVO> getAllAggregationTypes(@GraphQLArgument(name = "filter") AggregationTypeFilterVO filter,
                                                          @GraphQLEnvironment() Set<String> fields) {
        filter = fillFilterDefaults(filter);
        return aggregationService.findTypesByFilter(filter, getFetchOptions(fields));
    }

    @GraphQLQuery(name = "aggregationRows", description = "Read an aggregation")
    @Transactional(readOnly = true)
    public AggregationResultVO getAggregationRows(@GraphQLArgument(name = "type") AggregationTypeVO type,
                                                  @GraphQLArgument(name = "filter") ExtractionFilterVO filter,
                                                  @GraphQLArgument(name = "strata") AggregationStrataVO strata,
                                                  @GraphQLArgument(name = "offset", defaultValue = "0") Integer offset,
                                                  @GraphQLArgument(name = "size", defaultValue = "1000") Integer size,
                                                  @GraphQLArgument(name = "sortBy") String sort,
                                                  @GraphQLArgument(name = "sortDirection", defaultValue = "asc") String direction) {

        // Check access right
        securityService.checkReadAccess(type);

        return aggregationService.getAggBySpace(type, filter, strata, offset, size, sort, SortDirection.fromString(direction));
    }

    @GraphQLQuery(name = "aggregationColumns", description = "Read columns from aggregation")
    @Transactional(readOnly = true)
    public List<ExtractionTableColumnVO> getAggregationColumns(@GraphQLArgument(name = "type") AggregationTypeVO type,
                                                               @GraphQLArgument(name = "sheet") String sheetName,
                                                               @GraphQLEnvironment() Set<String> fields) {

        // Check type
        type = aggregationService.getTypeByFormat(type);

        // Check access right
        securityService.checkReadAccess(type);

        ExtractionTableColumnFetchOptions fetchOptions = ExtractionTableColumnFetchOptions.builder()
                .withRankOrder(fields.contains(ExtractionTableColumnVO.Fields.RANK_ORDER))
                .build();

        return productService.getColumnsBySheetName(type.getId(), sheetName, fetchOptions);
    }


    @GraphQLQuery(name = "aggregationGeoJson", description = "Execute an aggregation and return as GeoJson")
    @Transactional(readOnly = true)
    public Object getGeoJsonAggregation(@GraphQLArgument(name = "type") AggregationTypeVO format,
                                        @GraphQLArgument(name = "filter") ExtractionFilterVO filter,
                                        @GraphQLArgument(name = "strata") AggregationStrataVO strata,
                                        @GraphQLArgument(name = "offset", defaultValue = "0") Integer offset,
                                        @GraphQLArgument(name = "size", defaultValue = "1000") Integer size,
                                        @GraphQLArgument(name = "sortBy") String sort,
                                        @GraphQLArgument(name = "sortDirection", defaultValue = "asc") String direction) {

        // Check type
        final AggregationTypeVO type = aggregationService.getTypeByFormat(format);

        // Check access right
        securityService.checkReadAccess(type);

        // Use the first product's strata, as default
        if ((strata == null || strata.getSpatialColumnName() == null || strata.getTimeColumnName() == null)
                && CollectionUtils.isNotEmpty(type.getStratum())) {

            // Get a strata, to use by default
            final String sheetName = strata != null ? strata.getSheetName() : (filter != null ? filter.getSheetName() : null);
            ExtractionProductStrataVO defaultStrata = type.getStratum().stream()
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

        // Get data
        AggregationResultVO data = aggregationService.getAggBySpace(type, filter, strata, offset, size, sort, SortDirection.fromString(direction));

        // Convert to GeoJSON
        return geoJsonConverter.toFeatureCollection(data, strata.getSpatialColumnName());
    }

    @GraphQLQuery(name = "aggregationTech", description = "Execute an aggregation and return as GeoJson")
    @Transactional(readOnly = true)
    public AggregationTechResultVO getAggregationByTech(@GraphQLArgument(name = "type") AggregationTypeVO format,
                                                        @GraphQLArgument(name = "filter") ExtractionFilterVO filter,
                                                        @GraphQLArgument(name = "strata") AggregationStrataVO strata,
                                                        @GraphQLArgument(name = "sortBy") String sort,
                                                        @GraphQLArgument(name = "sortDirection", defaultValue = "asc") String direction) {

        // Check type
        final AggregationTypeVO type = aggregationService.getTypeByFormat(format);

        // Check access right
        securityService.checkReadAccess(type);

        return aggregationService.getAggByTech(type, filter, strata, sort, SortDirection.fromString(direction));
    }


    @GraphQLQuery(name = "aggregationTechMinMax", description = "Execute an aggregation and return as GeoJson")
    @Transactional(readOnly = true)
    public MinMaxVO getAggregationByTech(@GraphQLArgument(name = "type") AggregationTypeVO format,
                                         @GraphQLArgument(name = "filter") ExtractionFilterVO filter,
                                         @GraphQLArgument(name = "strata") AggregationStrataVO strata) {

        // Check type
        final AggregationTypeVO type = aggregationService.getTypeByFormat(format);

        // Check access right
        securityService.checkReadAccess(type);

        return aggregationService.getAggMinMaxByTech(type, filter, strata);
    }

    @GraphQLMutation(name = "saveAggregation", description = "Create or update a data aggregation")
    @Async
    public AggregationTypeVO saveAggregation(@GraphQLArgument(name = "type") AggregationTypeVO type,
                                             @GraphQLArgument(name = "filter") ExtractionFilterVO filter
    ) throws ExecutionException, InterruptedException {
        boolean isNew = type.getId() == null;
        if (isNew) {
            securityService.checkWriteAccess();
        }
        else {
            securityService.checkWriteAccess(type.getId());
        }

        return aggregationService.asyncSave(type, filter).get();
    }

    @GraphQLMutation(name = "deleteAggregations", description = "Delete some aggregations")
    public void deleteAggregations(@GraphQLArgument(name = "ids") int[] ids) {

        // Make sure can be deleted
        Arrays.stream(ids).forEach(securityService::checkWriteAccess);

        // Do deletion
        Arrays.stream(ids).forEach(productService::delete);
    }

    /* -- protected methods --*/

    protected ExtractionProductFetchOptions getFetchOptions(Set<String> fields) {
        return ExtractionProductFetchOptions.builder()
                .withRecorderDepartment(fields.contains(StringUtils.slashing(IWithRecorderDepartmentEntity.Fields.RECORDER_DEPARTMENT, IEntity.Fields.ID)))
                .withRecorderPerson(fields.contains(StringUtils.slashing(IWithRecorderPersonEntity.Fields.RECORDER_PERSON, IEntity.Fields.ID)))
                // Tables (=sheets)
                .withTables(fields.contains(ExtractionTypeVO.Fields.SHEET_NAMES))
                // Columns not need
                .withColumns(false)
                // Stratum
                .withStratum(fields.contains(AggregationTypeVO.Fields.STRATUM))

                .build();
    }


    protected AggregationTypeFilterVO fillFilterDefaults(AggregationTypeFilterVO filter) {
        AggregationTypeFilterVO result = filter != null ? filter : new AggregationTypeFilterVO();

        // Restrict to self data - issue #199
        if (!securityService.canReadAll()) {
            PersonVO user = securityService.getAuthenticatedUser().orElse(null);
            if (user != null) {
                result.setRecorderPersonId(user.getId());
                result.setStatusIds(new Integer[]{StatusEnum.ENABLE.getId(), StatusEnum.TEMPORARY.getId()});
            }
            else {
                result.setStatusIds(new Integer[]{StatusEnum.ENABLE.getId()});
            }
        }

        return result;
    }

}

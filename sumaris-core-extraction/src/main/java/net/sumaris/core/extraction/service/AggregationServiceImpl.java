package net.sumaris.core.extraction.service;

/*-
 * #%L
 * SUMARiS:: Core Extraction
 * %%
 * Copyright (C) 2018 - 2019 SUMARiS Consortium
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import lombok.NonNull;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.extraction.cache.ExtractionCacheNames;
import net.sumaris.core.extraction.dao.technical.table.ExtractionTableColumnOrder;
import net.sumaris.core.extraction.dao.technical.table.ExtractionTableDao;
import net.sumaris.core.extraction.dao.trip.cost.AggregationCostDao;
import net.sumaris.core.extraction.dao.trip.rdb.AggregationRdbTripDao;
import net.sumaris.core.extraction.dao.trip.survivalTest.AggregationSurvivalTestDao;
import net.sumaris.core.extraction.format.LiveFormatEnum;
import net.sumaris.core.extraction.format.ProductFormatEnum;
import net.sumaris.core.extraction.util.ExtractionFormats;
import net.sumaris.core.extraction.util.ExtractionProducts;
import net.sumaris.core.extraction.vo.*;
import net.sumaris.core.extraction.vo.filter.AggregationTypeFilterVO;
import net.sumaris.core.extraction.vo.trip.rdb.AggregationRdbTripContextVO;
import net.sumaris.core.model.referential.StatusEnum;
import net.sumaris.core.model.technical.extraction.ExtractionCategoryEnum;
import net.sumaris.core.model.technical.extraction.IExtractionFormat;
import net.sumaris.core.util.Beans;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.technical.extraction.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.nuiton.i18n.I18n;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.ApplicationContext;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import javax.annotation.Resource;
import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * @author peck7 on 17/12/2018.
 */
@Service("aggregationService")
public class AggregationServiceImpl implements AggregationService {

    private static final Logger log = LoggerFactory.getLogger(AggregationServiceImpl.class);

    @Autowired
    private ExtractionService extractionService;

    @Autowired
    private ExtractionProductService productService;

    @Resource(name = "aggregationRdbTripDao")
    private AggregationRdbTripDao aggregationRdbTripDao;

    @Autowired
    private AggregationSurvivalTestDao aggregationSurvivalTestDao;

    @Autowired
    private AggregationCostDao aggregationCostDao;

    @Autowired
    private ExtractionTableDao extractionTableDao;

    @Autowired(required = false)
    protected TaskExecutor taskExecutor = null;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ApplicationContext applicationContext;

    @Override
    public List<AggregationTypeVO> findTypesByFilter(AggregationTypeFilterVO filter, ExtractionProductFetchOptions fetchOptions) {

        final AggregationTypeFilterVO notNullFilter = filter != null ? filter : new AggregationTypeFilterVO();

        return ListUtils.emptyIfNull(getProductAggregationTypes(notNullFilter, fetchOptions))
            .stream()
            .filter(t -> notNullFilter.getIsSpatial() == null || Objects.equals(notNullFilter.getIsSpatial(), t.getIsSpatial()))
            .collect(Collectors.toList());
    }

    @Override
    @Cacheable(cacheNames = ExtractionCacheNames.AGGREGATION_TYPE_BY_ID)
    public AggregationTypeVO getTypeById(int id, ExtractionProductFetchOptions fetchOptions) {
        ExtractionProductVO source = productService.get(id, fetchOptions);
        return toAggregationType(source);
    }

    @Override
    @Cacheable(cacheNames = ExtractionCacheNames.AGGREGATION_TYPE_BY_FORMAT,
            key = "#format.category + #format.label + #format.version",
            condition = " #format != null", unless = "#result == null")
    public AggregationTypeVO getTypeByFormat(IExtractionFormat format) {
        return ExtractionFormats.findOneMatch(getAllAggregationTypes(null), format);
    }

    @Override
    public AggregationContextVO execute(AggregationTypeVO type, ExtractionFilterVO filter, AggregationStrataVO strata) {
        type = getTypeByFormat(type);
        ExtractionProductVO source;

        switch (type.getCategory()) {
            case PRODUCT:
                // Get the product VO
                source = productService.getByLabel(type.getLabel(), ExtractionProductFetchOptions.TABLES_AND_STRATUM);
                // Execute, from product
                return aggregate(source, filter, strata);

            case LIVE:
                // First execute the raw extraction
                ExtractionContextVO rawExtractionContext = extractionService.execute(type, filter);

                try {
                    source = extractionService.toProductVO(rawExtractionContext);
                    ExtractionFilterVO aggregationFilter = null;
                    if (filter != null) {
                        aggregationFilter = new ExtractionFilterVO();
                        aggregationFilter.setSheetName(filter.getSheetName());
                    }

                    // Execute, from product
                    return aggregate(source, aggregationFilter, strata);
                } finally {
                    // Clean intermediate tables
                    extractionService.asyncClean(rawExtractionContext);
                }
            default:
                throw new SumarisTechnicalException(String.format("Aggregation on category %s not implemented yet !", type.getCategory()));
        }
    }

    @Override
    public AggregationResultVO getAggBySpace(AggregationTypeVO type, @Nullable ExtractionFilterVO filter, @Nullable AggregationStrataVO strata, int offset, int size, String sort, SortDirection direction) {
        Preconditions.checkNotNull(type);

        ExtractionProductVO product = productService.getByLabel(type.getLabel(),
                ExtractionProductFetchOptions.TABLES);

        // Convert to context VO (need the next read() function)
        String sheetName = strata.getSheetName() != null ? strata.getSheetName() : filter.getSheetName();
        AggregationContextVO context = toContextVO(product, sheetName);

        return getAggBySpace(context, filter, strata, offset, size, sort, direction);
    }

    @Override
    public AggregationResultVO getAggBySpace(@NonNull AggregationContextVO context,
                                             @Nullable ExtractionFilterVO filter,
                                             @Nullable AggregationStrataVO strata,
                                             int offset, int size, String sort, SortDirection direction) {
        filter = filter != null ? filter : new ExtractionFilterVO();
        strata = strata != null ? strata : (context.getStrata() != null ? context.getStrata() : new AggregationStrataVO());
        String sheetName = strata.getSheetName() != null ? strata.getSheetName() : filter.getSheetName();

        String tableName = StringUtils.isNotBlank(sheetName) ? context.getTableNameBySheetName(sheetName) : null;

        // Missing the expected sheet = return no data
        if (tableName == null) return createEmptyResult();

        // Force strata and filter to have the same sheet
        strata.setSheetName(sheetName);
        filter.setSheetName(sheetName);

        // Read the data
        ProductFormatEnum format = ExtractionFormats.getProductFormat(context);
        switch (format) {
            case AGG_RDB:
            case AGG_COST:
            case AGG_SURVIVAL_TEST:
                return aggregationRdbTripDao.getAggBySpace(tableName, filter, strata, offset, size, sort, direction);
            default:
                throw new SumarisTechnicalException(String.format("Unable to read data on type '%s': not implemented", context.getLabel()));
        }

    }

    @Override
    public AggregationTechResultVO getAggByTech(AggregationTypeVO type,
                                                ExtractionFilterVO filter,
                                                AggregationStrataVO strata,
                                                String sort,
                                                SortDirection direction) {
        Preconditions.checkNotNull(type);
        filter = filter != null ? filter : new ExtractionFilterVO();

        ExtractionProductVO product = productService.getByLabel(type.getLabel(),
                ExtractionProductFetchOptions.TABLES);

        // Convert to context VO (need the next read() function)
        String sheetName = strata != null && strata.getSheetName() != null ? strata.getSheetName() : filter.getSheetName();
        Preconditions.checkNotNull(sheetName, String.format("Missing 'filter.%s' or 'strata.%s",
                ExtractionFilterVO.Fields.SHEET_NAME,
                AggregationStrataVO.Fields.LABEL));

        AggregationContextVO context = toContextVO(product, sheetName);

        strata = strata != null ? strata : (context.getStrata() != null ? context.getStrata() : new AggregationStrataVO());

        String tableName = StringUtils.isNotBlank(sheetName) ? context.getTableNameBySheetName(sheetName) : null;

        return aggregationRdbTripDao.getAggByTech(tableName, filter, strata, sort, direction);
    }

    @Override
    public MinMaxVO getAggMinMaxByTech(AggregationTypeVO type, ExtractionFilterVO filter, AggregationStrataVO strata) {
        Preconditions.checkNotNull(type);
        filter = filter != null ? filter : new ExtractionFilterVO();

        ExtractionProductVO product = productService.getByLabel(type.getLabel(),
                ExtractionProductFetchOptions.TABLES);

        // Convert to context VO (need the next read() function)
        String sheetName = strata != null && strata.getSheetName() != null ? strata.getSheetName() : filter.getSheetName();
        Preconditions.checkNotNull(sheetName, String.format("Missing 'filter.%s' or 'strata.%s",
                ExtractionFilterVO.Fields.SHEET_NAME,
                AggregationStrataVO.Fields.LABEL));

        AggregationContextVO context = toContextVO(product, sheetName);

        strata = strata != null ? strata : (context.getStrata() != null ? context.getStrata() : new AggregationStrataVO());

        String tableName = StringUtils.isNotBlank(sheetName) ? context.getTableNameBySheetName(sheetName) : null;

        return aggregationRdbTripDao.getAggMinMaxByTech(tableName, filter, strata);
    }

    @Override
    public AggregationResultVO executeAndRead(AggregationTypeVO type, ExtractionFilterVO filter, AggregationStrataVO strata,
                                              int offset, int size, String sort, SortDirection direction) {
        // Execute the aggregation
        AggregationContextVO context = execute(type, filter, strata);

        // Prepare the read filter
        ExtractionFilterVO readFilter = null;
        if (filter != null) {
            readFilter = new ExtractionFilterVO();
            readFilter.setSheetName(filter.getSheetName());
        }

        try {
            // Read data
            return getAggBySpace(context, readFilter, strata, offset, size, sort, direction);
        } finally {
            // Clean created tables
            clean(context, true);
        }
    }

    @Override
    public File executeAndDump(AggregationTypeVO type, @Nullable ExtractionFilterVO filter, @Nullable AggregationStrataVO strata) {
        // Execute the aggregation
        AggregationContextVO context = execute(type, filter, strata);
        try {
            return extractionService.dumpTablesToFile(context, null /*already apply*/);
        }
        finally {
            // Delete aggregation tables, after dump
            clean(context, true);
        }
    }

    @Override
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = ExtractionCacheNames.AGGREGATION_TYPE_BY_ID, allEntries = true),
                    @CacheEvict(cacheNames = ExtractionCacheNames.AGGREGATION_TYPE_BY_FORMAT, allEntries = true)
            }
    )
    public CompletableFuture<AggregationTypeVO> asyncSave(AggregationTypeVO type, @Nullable ExtractionFilterVO filter) {
        return CompletableFuture.completedFuture(save(type, filter));
    }

    @Override
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = ExtractionCacheNames.AGGREGATION_TYPE_BY_ID, allEntries = true),
                    @CacheEvict(cacheNames = ExtractionCacheNames.AGGREGATION_TYPE_BY_FORMAT, allEntries = true)
            }
    )
    public AggregationTypeVO save(AggregationTypeVO type, @Nullable ExtractionFilterVO filter) {
        Preconditions.checkNotNull(type);
        Preconditions.checkNotNull(type.getLabel());
        Preconditions.checkNotNull(type.getName());
        Collection<String> existingTablesToDrop = Lists.newArrayList();

        // Load the product
        ExtractionProductVO target = null;
        if (type.getId() != null) {
            target = productService.findById(type.getId(), ExtractionProductFetchOptions.FOR_UPDATE).orElse(null);
        }
        boolean isNew = target == null;
        if (isNew) {
            target = new ExtractionProductVO();
            target.setLabel(type.getLabel().toUpperCase());

            // Check label != format
            Preconditions.checkArgument(!Objects.equals(type.getLabel(), type.getRawFormatLabel()), "Invalid label. Expected pattern: <type_name>-NNN");
        }
        else {
            // Check label was not changed
            String previousLabel = target.getLabel();
            Preconditions.checkArgument(previousLabel.equalsIgnoreCase(type.getLabel()), "Cannot change a product label");

            filter = filter != null ? filter : readFilterString(target.getFilter());

        }

        // Check if need aggregate (ig new or if filter changed)
        String filterAsString = writeFilterAsString(filter);
        boolean aggregate = isNew || !Objects.equals(target.getFilter(), filterAsString);

        // Run the aggregation (if need) before saving
        if (aggregate) {

            // Should clean existing table
            if (!isNew) {
                existingTablesToDrop.addAll(target.getTableNames());
            }

            // Prepare a executable type (with label=format)
            AggregationTypeVO executableType = new AggregationTypeVO();
            executableType.setLabel(type.getRawFormatLabel());
            executableType.setCategory(type.getCategory());

            // Execute the aggregation
            AggregationContextVO context = execute(executableType, filter, null);

            // Update product tables, using the aggregation result
            toProductVO(context, target);

            // Copy some properties from the given type
            target.setName(type.getName());
            target.setUpdateDate(type.getUpdateDate());
            target.setDescription(type.getDescription());
            target.setDocumentation(type.getDocumentation());
            target.setStatusId(type.getStatusId());
            target.setRecorderDepartment(type.getRecorderDepartment());
            target.setRecorderPerson(type.getRecorderPerson());
        }

        // Not need new aggregation: update entity before saving
        else {
            Preconditions.checkArgument(StringUtils.equalsIgnoreCase(target.getLabel(), type.getLabel()), "Cannot update the label of an existing product");
            target.setName(type.getName());
            target.setUpdateDate(type.getUpdateDate());
            target.setDescription(type.getDescription());
            target.setDocumentation(type.getDocumentation());
            target.setComments(type.getComments());
            target.setStatusId(type.getStatusId());
            target.setUpdateDate(type.getUpdateDate());
            target.setIsSpatial(type.getIsSpatial());
        }
        target.setStratum(type.getStratum());
        target.setFilter(filterAsString);

        // Save the product
        target = productService.save(target);

        // Drop old tables
        dropTables(existingTablesToDrop);

        // Transform back to type
        return toAggregationType(target);
    }

    /* -- protected -- */

    protected List<AggregationTypeVO> getAllAggregationTypes(ExtractionProductFetchOptions fetchOptions) {
        return ImmutableList.<AggregationTypeVO>builder()
            .addAll(getProductAggregationTypes(fetchOptions))
            .addAll(getLiveAggregationTypes())
            .build();
    }

    protected List<AggregationTypeVO> getProductAggregationTypes(ExtractionProductFetchOptions fetchOptions) {
        return getProductAggregationTypes(null, fetchOptions);
    }

    protected List<AggregationTypeVO> getProductAggregationTypes(@Nullable AggregationTypeFilterVO filter, ExtractionProductFetchOptions fetchOptions) {
        filter = filter != null ? filter : new AggregationTypeFilterVO();

        // Exclude types with a DISABLE status, by default
        if (ArrayUtils.isEmpty(filter.getStatusIds())) {
            filter.setStatusIds(new Integer[]{StatusEnum.ENABLE.getId(), StatusEnum.TEMPORARY.getId()});
        }

        final Boolean filterIsSpatial = filter.getIsSpatial();

        return ListUtils.emptyIfNull(productService.findByFilter(filter, fetchOptions))
            .stream()
            .filter(p -> filterIsSpatial == null || filterIsSpatial.equals(p.getIsSpatial()))
            .map(this::toAggregationType)
            .collect(Collectors.toList());
    }

    protected List<AggregationTypeVO> getLiveAggregationTypes() {
        return Arrays.stream(LiveFormatEnum.values())
            .map(format -> {
                AggregationTypeVO type = new AggregationTypeVO();
                type.setLabel(format.getLabel().toLowerCase());
                type.setCategory(ExtractionCategoryEnum.LIVE);
                type.setSheetNames(format.getSheetNames());
                return type;
            })
            .collect(Collectors.toList());
    }

    protected AggregationResultVO createEmptyResult() {
        AggregationResultVO result = new AggregationResultVO();
        result.setColumns(ImmutableList.of());
        result.setTotal(0);
        result.setRows(ImmutableList.of());
        return result;
    }

    public AggregationContextVO aggregate(ExtractionProductVO source,
                                          ExtractionFilterVO filter,
                                          AggregationStrataVO strata) {
        Preconditions.checkNotNull(source);
        Preconditions.checkNotNull(source.getLabel());

        LiveFormatEnum format = ExtractionFormats.getLiveFormat(source);

        switch (format) {
            case RDB:
                return aggregationRdbTripDao.aggregate(source, filter, strata);

            case COST:
                return aggregationCostDao.aggregate(source, filter, strata);

            case SURVIVAL_TEST:
                return aggregationSurvivalTestDao.aggregate(source, filter, strata);

            case FREE1: // TODO
            case FREE2: // TODO
            default:
                throw new SumarisTechnicalException(String.format("Data aggregation on type '%s' is not implemented!", format.name()));
        }
    }

    @Override
    public CompletableFuture<Boolean> asyncClean(AggregationContextVO context) {
        try {
            clean(context);
            return CompletableFuture.completedFuture(Boolean.TRUE);
        } catch (Exception e) {
            log.warn(String.format("Error while cleaning aggregation #%s: %s", context.getId(), e.getMessage()), e);
            return CompletableFuture.completedFuture(Boolean.FALSE);
        }
    }

    @Override
    public void clean(AggregationContextVO context) {
        clean(context, false);
    }


    /* -- protected methods -- */


    protected AggregationTypeVO toAggregationType(ExtractionProductVO source) {
        AggregationTypeVO target = new AggregationTypeVO();

        Beans.copyProperties(source, target);

        // Change label to lowercase (better for UI client)
        target.setLabel(source.getLabel().toLowerCase());

        // Stratum
        if (CollectionUtils.isNotEmpty(source.getStratum())) {
            target.setStratum(source.getStratum());
        }

        return target;
    }

    protected void toProductVO(AggregationContextVO source, ExtractionProductVO target) {

        target.setLabel(ExtractionProducts.getProductLabel(source, source.getId()));
        target.setName(ExtractionProducts.getProductDisplayName(source, source.getId()));
        target.setFormat(source.getRawFormatLabel());
        target.setVersion(source.getVersion());
        target.setIsSpatial(source.isSpatial());

        target.setTables(toProductTableVO(source));
    }

    protected List<ExtractionTableVO> toProductTableVO(AggregationContextVO source) {

        final List<String> tableNames = ImmutableList.copyOf(source.getTableNames());
        return tableNames.stream()
            .map(tableName -> {
                ExtractionTableVO table = new ExtractionTableVO();
                table.setTableName(tableName);

                // Keep rankOrder from original linked has map
                table.setRankOrder(tableNames.indexOf(tableName) + 1);

                // Label (=the sheet name)
                String sheetName = source.getSheetName(tableName);
                table.setLabel(sheetName);

                table.setName(ExtractionProducts.getSheetDisplayName(source, sheetName));
                table.setIsSpatial(source.hasSpatialColumn(tableName));

                // Columns
                List<ExtractionTableColumnVO> columns = toProductColumnVOs(source, tableName,
                        ExtractionTableColumnFetchOptions.builder()
                                .withRankOrder(false) // skip rankOrder, because fill later, by format and sheetName (more accuracy)
                                .build());

                // Fill rank order
                ExtractionTableColumnOrder.fillRankOrderByFormatAndSheet(source, sheetName, columns);

                table.setColumns(columns);

                return table;
            })
            .collect(Collectors.toList());
    }

    protected List<ExtractionTableColumnVO> toProductColumnVOs(AggregationContextVO context, String tableName,
                                                               ExtractionTableColumnFetchOptions fetchOptions) {


        // Get columns (from table metadata), but exclude hidden columns
        List<ExtractionTableColumnVO> columns = extractionTableDao.getColumns(tableName, fetchOptions);

        Set<String> hiddenColumns = Beans.getSet(context.getHiddenColumns(tableName));
        Map<String, List<String>> columnValues = Beans.getMap(context.getColumnValues(tableName));

        // For each column
        Beans.getStream(columns).forEach(column -> {
            String columnName = column.getColumnName();
            // If hidden, replace the type with 'hidden'
            if (hiddenColumns.contains(columnName)) {
                column.setType("hidden");
            }

            // Set values
            List<String> values = columnValues.get(columnName);
            if (CollectionUtils.isNotEmpty(values)) {
                column.setValues(values);
            }
        });

        return columns;
    }

    protected AggregationContextVO toContextVO(ExtractionProductVO source, String sheetName) {

        AggregationContextVO target = new AggregationContextVO();

        target.setId(source.getId());
        target.setLabel(source.getLabel());
        target.setCategory(source.getCategory());
        target.setVersion(source.getVersion());

        ListUtils.emptyIfNull(source.getTables())
            .forEach(t -> target.addTableName(t.getTableName(), t.getLabel()));

        // Find the strata to apply, by sheetName
        if (sheetName != null && source.getStratum() != null) {
            AggregationStrataVO productStrata = source.getStratum().stream()
                    .filter(s -> sheetName.equals(s.getSheetName()))
                    .findFirst().orElse(null);
            if (productStrata != null) {
                AggregationStrataVO strata = new AggregationStrataVO();
                Beans.copyProperties(productStrata, strata);
                target.setStrata(strata);
            }
        }

        return target;
    }

    protected String getI18nSheetName(String format, String sheetName) {
        return I18n.t(String.format("sumaris.extraction.%s.%s", format.toUpperCase(), sheetName.toUpperCase()));
    }

    protected void dropTables(Collection<String> tableNames) {
        Beans.getStream(tableNames).forEach(extractionTableDao::dropTable);
    }

    protected String writeFilterAsString(ExtractionFilterVO filter) {
        if (filter == null) return null;
        try {
            return objectMapper.writeValueAsString(filter);
        }
        catch(JsonProcessingException e) {
            throw new SumarisTechnicalException(e);
        }
    }

    protected ExtractionFilterVO readFilterString(String json) {
        if (StringUtils.isBlank(json)) return null;
        try {
            return objectMapper.readValue(json, ExtractionFilterVO.class);
        }
        catch(JsonProcessingException e) {
            throw new SumarisTechnicalException(e);
        }
    }


    protected void clean(AggregationContextVO context, boolean async) {
        if (context == null) return;
        if (async) {
            getSelfBean().asyncClean(context);
        }
        else if (context instanceof AggregationRdbTripContextVO) {
            log.info("Cleaning aggregation #{}-{}", context.getLabel(), context.getId());
            aggregationRdbTripDao.clean((AggregationRdbTripContextVO) context);
        }
    }

    /**
     * Get self bean, to be able to use new transation
     * @return
     */
    protected AggregationService getSelfBean() {
        return applicationContext.getBean("aggregationService", AggregationService.class);
    }

}

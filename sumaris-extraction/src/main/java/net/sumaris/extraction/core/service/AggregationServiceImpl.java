package net.sumaris.extraction.core.service;

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
import com.google.common.collect.Maps;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.exception.DataNotFoundException;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.util.TimeUtils;
import net.sumaris.extraction.core.config.ExtractionAutoConfiguration;
import net.sumaris.extraction.core.config.ExtractionCacheConfiguration;
import net.sumaris.extraction.core.dao.AggregationDao;
import net.sumaris.extraction.core.dao.technical.Daos;
import net.sumaris.extraction.core.dao.technical.table.ExtractionTableColumnOrder;
import net.sumaris.extraction.core.dao.technical.table.ExtractionTableDao;
import net.sumaris.extraction.core.format.LiveFormatEnum;
import net.sumaris.extraction.core.format.ProductFormatEnum;
import net.sumaris.extraction.core.specification.data.trip.AggSpecification;
import net.sumaris.extraction.core.util.ExtractionFormats;
import net.sumaris.extraction.core.util.ExtractionProducts;
import net.sumaris.extraction.core.vo.*;
import net.sumaris.extraction.core.vo.filter.AggregationTypeFilterVO;
import net.sumaris.core.model.referential.StatusEnum;
import net.sumaris.core.model.technical.extraction.ExtractionCategoryEnum;
import net.sumaris.core.model.technical.extraction.IExtractionFormat;
import net.sumaris.core.model.technical.history.ProcessingFrequencyEnum;
import net.sumaris.core.util.Beans;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.technical.extraction.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.nuiton.i18n.I18n;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * @author peck7 on 17/12/2018.
 */
@Slf4j
@Service("aggregationService")
@ConditionalOnBean({ExtractionAutoConfiguration.class})
public class AggregationServiceImpl implements AggregationService {

    private final ConfigurableApplicationContext context;
    private final DataSource dataSource;
    private final ExtractionService extractionService;
    private final ExtractionProductService extractionProductService;
    private final ExtractionTableDao extractionTableDao;
    private final ObjectMapper objectMapper;

    private Optional<TaskExecutor> taskExecutor;
    private Map<IExtractionFormat, AggregationDao<?,?,?>> daosByFormat = Maps.newHashMap();

    public AggregationServiceImpl(ConfigurableApplicationContext context,
                                  ObjectMapper objectMapper,
                                  DataSource dataSource,
                                  ExtractionTableDao extractionTableDao,
                                  ExtractionService extractionService,
                                  ExtractionProductService extractionProductService) {
        this.context = context;
        this.objectMapper = objectMapper;
        this.dataSource = dataSource;

        this.extractionTableDao = extractionTableDao;

        this.extractionService = extractionService;
        this.extractionProductService = extractionProductService;
    }

    @PostConstruct
    protected void init() {
        this.taskExecutor = Optional.ofNullable(context.getBean(TaskExecutor.class));

            // Register all extraction daos
        context.getBeansOfType(AggregationDao.class).values()
            .forEach(dao -> {
                IExtractionFormat format = dao.getFormat();
                // Check if unique, by format
                if (daosByFormat.containsKey(format)) {
                    throw new BeanInitializationException(
                        String.format("Too many ExtractionDao for the same format %s: [%s, %s]",
                            daosByFormat.get(dao.getFormat()).getClass().getSimpleName(),
                            dao.getClass().getSimpleName()));
                }
                // Register the dao
                daosByFormat.put(format, dao);
            });
    }

    @Override
    public List<AggregationTypeVO> findTypesByFilter(AggregationTypeFilterVO filter, ExtractionProductFetchOptions fetchOptions) {

        final AggregationTypeFilterVO notNullFilter = filter != null ? filter : new AggregationTypeFilterVO();

        return ListUtils.emptyIfNull(getProductAggregationTypes(notNullFilter, fetchOptions))
            .stream()
            .filter(t -> notNullFilter.getIsSpatial() == null || Objects.equals(notNullFilter.getIsSpatial(), t.getIsSpatial()))
            .collect(Collectors.toList());
    }

    @Override
    @Cacheable(cacheNames = ExtractionCacheConfiguration.Names.AGGREGATION_TYPE_BY_ID_AND_OPTIONS)
    public AggregationTypeVO getTypeById(int id, ExtractionProductFetchOptions fetchOptions) {
        ExtractionProductVO source = extractionProductService.get(id, fetchOptions);
        return toAggregationType(source);
    }

    @Override
    @Cacheable(cacheNames = ExtractionCacheConfiguration.Names.AGGREGATION_TYPE_BY_FORMAT,
            key = "#format.category + #format.label + #format.version",
            condition = " #format != null", unless = "#result == null")
    public AggregationTypeVO getTypeByFormat(IExtractionFormat format) {
        return ExtractionFormats.findOneMatch(getAllAggregationTypes(null), format);
    }

    @Override
    public AggregationContextVO aggregate(AggregationTypeVO type,
                                          @Nullable ExtractionFilterVO filter,
                                          AggregationStrataVO strata) {
        type = getTypeByFormat(type);
        ExtractionProductVO source;

        switch (type.getCategory()) {
            case PRODUCT:
                // Get the product VO
                source = extractionProductService.getByLabel(type.getLabel(), ExtractionProductFetchOptions.TABLES_AND_STRATUM);
                // Execute, from product
                return aggregateDao(source, filter, strata);

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
                    return aggregateDao(source, aggregationFilter, strata);
                } finally {
                    // Clean intermediate tables
                    extractionService.asyncClean(rawExtractionContext);
                }
            default:
                throw new SumarisTechnicalException(String.format("Aggregation on category %s not implemented yet !", type.getCategory()));
        }
    }

    @Override
    public AggregationResultVO getAggBySpace(AggregationTypeVO type,
                                             @Nullable ExtractionFilterVO filter,
                                             @Nullable AggregationStrataVO strata,
                                             Page page) {
        Preconditions.checkNotNull(type);

        filter = ExtractionFilterVO.nullToEmpty(filter);
        ExtractionProductVO product = extractionProductService.getByLabel(type.getLabel(),
                ExtractionProductFetchOptions.TABLES);

        // Convert to context VO (need the next read() function)
        String sheetName = strata != null && strata.getSheetName() != null ? strata.getSheetName() : filter.getSheetName();
        AggregationContextVO context = toContextVO(product, sheetName);

        return getAggBySpace(context, filter, strata, page);
    }

    @Override
    public AggregationResultVO getAggBySpace(@NonNull AggregationContextVO context,
                                             @Nullable ExtractionFilterVO filter,
                                             @Nullable AggregationStrataVO strata,
                                             Page page) {
        filter = ExtractionFilterVO.nullToEmpty(filter);
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
        return getDao(format).getAggBySpace(tableName, filter, strata, page);
    }

    @Override
    public AggregationTechResultVO getAggByTech(AggregationTypeVO type,
                                                @Nullable ExtractionFilterVO filter,
                                                @Nullable AggregationStrataVO strata,
                                                String sort,
                                                SortDirection direction) {
        Preconditions.checkNotNull(type);

        filter = ExtractionFilterVO.nullToEmpty(filter);
        ExtractionProductVO product = extractionProductService.getByLabel(type.getLabel(),
                ExtractionProductFetchOptions.TABLES);

        // Convert to context VO (need the next read() function)
        String sheetName = strata != null && strata.getSheetName() != null ? strata.getSheetName() : filter.getSheetName();
        Preconditions.checkNotNull(sheetName, String.format("Missing 'filter.%s' or 'strata.%s",
                ExtractionFilterVO.Fields.SHEET_NAME,
                AggregationStrataVO.Fields.LABEL));

        AggregationContextVO context = toContextVO(product, sheetName);

        strata = strata != null ? strata : (context.getStrata() != null ? context.getStrata() : new AggregationStrataVO());

        String tableName = StringUtils.isNotBlank(sheetName) ? context.getTableNameBySheetName(sheetName) : null;

        // Missing the expected sheet = return no data
        if (tableName == null) return createEmptyTechResult();

        ProductFormatEnum format = ExtractionFormats.getProductFormat(context);
        return getDao(format).getAggByTech(tableName, filter, strata, sort, direction);
    }

    @Override
    public MinMaxVO getAggMinMaxByTech(@NonNull AggregationTypeVO type, @Nullable ExtractionFilterVO filter, @Nullable AggregationStrataVO strata) {

        filter = ExtractionFilterVO.nullToEmpty(filter);
        ExtractionProductVO product = extractionProductService.getByLabel(type.getLabel(),
                ExtractionProductFetchOptions.TABLES);

        // Convert to context VO (need the next read() function)
        String sheetName = strata != null && strata.getSheetName() != null ? strata.getSheetName() : filter.getSheetName();
        Preconditions.checkNotNull(sheetName, String.format("Missing 'filter.%s' or 'strata.%s",
                ExtractionFilterVO.Fields.SHEET_NAME,
                AggregationStrataVO.Fields.LABEL));

        AggregationContextVO context = toContextVO(product, sheetName);

        strata = strata != null ? strata : (context.getStrata() != null ? context.getStrata() : new AggregationStrataVO());

        String tableName = StringUtils.isNotBlank(sheetName) ? context.getTableNameBySheetName(sheetName) : null;

        ProductFormatEnum format = ExtractionFormats.getProductFormat(context);
        return getDao(format).getAggMinMaxByTech(tableName, filter, strata);
    }

    @Override
    public AggregationResultVO executeAndRead(AggregationTypeVO type,
                                              @Nullable ExtractionFilterVO filter,
                                              @Nullable AggregationStrataVO strata,
                                              Page page) {
        // Execute the aggregation
        AggregationContextVO context = aggregate(type, filter, strata);
        Daos.commitIfHsqldbOrPgsql(dataSource);

        // Prepare the read filter
        ExtractionFilterVO readFilter = null;
        if (filter != null) {
            readFilter = new ExtractionFilterVO();
            readFilter.setSheetName(filter.getSheetName());
        }

        try {
            // Read data
            return getAggBySpace(context, readFilter, strata, page);
        } finally {
            // Clean created tables
            clean(context, true);
        }
    }

    @Override
    public File executeAndDump(AggregationTypeVO type, @Nullable ExtractionFilterVO filter, @Nullable AggregationStrataVO strata) {
        // Execute the aggregation
        AggregationContextVO context = aggregate(type, filter, strata);
        Daos.commitIfHsqldbOrPgsql(dataSource);

        try {
            // Dump to files
            return extractionService.dumpTablesToFile(context, null /*already apply*/);
        }
        finally {
            // Delete aggregation tables, after dump
            clean(context);
        }
    }

    @Override
    @Caching(
            evict = {
                @CacheEvict(cacheNames = ExtractionCacheConfiguration.Names.AGGREGATION_TYPE_BY_ID_AND_OPTIONS, allEntries = true),
                @CacheEvict(cacheNames = ExtractionCacheConfiguration.Names.AGGREGATION_TYPE_BY_FORMAT, allEntries = true),
                @CacheEvict(cacheNames = ExtractionCacheConfiguration.Names.EXTRACTION_TYPES, allEntries = true)
            }
    )
    public CompletableFuture<AggregationTypeVO> asyncSave(AggregationTypeVO type, @Nullable ExtractionFilterVO filter) {
        return CompletableFuture.completedFuture(save(type, filter));
    }

    @Override
    @Caching(
        evict = {
            @CacheEvict(cacheNames = ExtractionCacheConfiguration.Names.AGGREGATION_TYPE_BY_ID_AND_OPTIONS, allEntries = true),
            @CacheEvict(cacheNames = ExtractionCacheConfiguration.Names.AGGREGATION_TYPE_BY_FORMAT, allEntries = true),
            @CacheEvict(cacheNames = ExtractionCacheConfiguration.Names.EXTRACTION_TYPES, allEntries = true)
        }
    )
    public AggregationTypeVO updateProduct(int productId) {

        ExtractionProductVO target = extractionProductService.findById(productId, ExtractionProductFetchOptions.FOR_UPDATE)
            .orElseThrow(() -> new DataNotFoundException(String.format("Unknown product {id: %s}", productId)));
        Collection<String> previousTableNames = Lists.newArrayList(target.getTableNames());

        // Read filter
        ExtractionFilterVO filter = readFilter(target.getFilter());

        long startTime = System.currentTimeMillis();
        log.debug("Updating extraction {id: {}, label: '{}'}...", productId, target.getLabel());

        boolean needAggregation = false;
        String rawFormat = target.getRawFormatLabel();
        if (rawFormat.startsWith(AggSpecification.FORMAT_PREFIX)) {
            rawFormat = rawFormat.substring(AggSpecification.FORMAT_PREFIX.length());
            needAggregation = true;
        }


        // Execute the aggregation
        if (!needAggregation) {
            // Prepare an executable type (with label=format)
            ExtractionTypeVO executableType = new ExtractionTypeVO();
            executableType.setLabel(rawFormat);
            executableType.setCategory(ExtractionCategoryEnum.LIVE);
            ExtractionContextVO extractionContextVO = extractionService.execute(executableType, filter);

            ExtractionProductVO source = target;
            target = extractionService.toProductVO(extractionContextVO);

            copyIdAndUpdateDate(source, target);
        }
        else {
            AggregationTypeVO executableType = new AggregationTypeVO();
            executableType.setLabel(rawFormat);
            executableType.setCategory(ExtractionCategoryEnum.LIVE);
            AggregationContextVO context = aggregate(executableType, filter, null);
            // Update product tables, using the aggregation result
            toProductVO(context, target);
        }


        // Save the product
        extractionProductService.save(target);

        // Drop each orphan tables
        List<String> newTableNames = Beans.getList(target.getTableNames());
        previousTableNames.stream()
            .filter(tableName -> !newTableNames.contains(tableName))
            .forEach(extractionTableDao::dropTable);

        log.debug("Updating extraction {id: {}, label: '{}'} [OK] in {}", productId,
            target.getLabel(),
            TimeUtils.printDurationFrom(startTime));

        // Transform to type
        return toAggregationType(target);
    }

    @Override
    @Caching(
        evict = {
            @CacheEvict(cacheNames = ExtractionCacheConfiguration.Names.AGGREGATION_TYPE_BY_ID_AND_OPTIONS, allEntries = true),
            @CacheEvict(cacheNames = ExtractionCacheConfiguration.Names.AGGREGATION_TYPE_BY_FORMAT, allEntries = true),
            @CacheEvict(cacheNames = ExtractionCacheConfiguration.Names.EXTRACTION_TYPES, allEntries = true)
        }
    )
    public AggregationTypeVO save(AggregationTypeVO source, @Nullable ExtractionFilterVO filter) {
        Preconditions.checkNotNull(source);
        Preconditions.checkNotNull(source.getLabel());
        Preconditions.checkNotNull(source.getName());
        Collection<String> tablesToDrop = Lists.newArrayList();

        // Load the product
        ExtractionProductVO target = null;
        if (source.getId() != null) {
            target = extractionProductService.findById(source.getId(), ExtractionProductFetchOptions.FOR_UPDATE).orElse(null);
        }
        boolean isNew = target == null;
        if (isNew) {
            target = new ExtractionProductVO();
            target.setLabel(source.getLabel().toUpperCase());

            // Check label != format
            Preconditions.checkArgument(!Objects.equals(source.getLabel(), source.getRawFormatLabel()), "Invalid label. Expected pattern: <type_name>-NNN");
        }
        else {
            // Check label was not changed
            String previousLabel = target.getLabel();
            Preconditions.checkArgument(previousLabel.equalsIgnoreCase(source.getLabel()), "Cannot change a product label");

            // If not given in arguments, parse the product's filter string
            filter = filter != null ? filter : readFilter(target.getFilter());

        }

        // Check if need aggregate (ig new or if filter changed)
        ProcessingFrequencyEnum frequency = source.getProcessingFrequencyId() != null
            ? ProcessingFrequencyEnum.valueOf(source.getProcessingFrequencyId())
            : ProcessingFrequencyEnum.MANUALLY;

        String filterAsString = writeFilterAsString(filter);

        boolean needExecution = (isNew || !Objects.equals(target.getFilter(), filterAsString))
            && (frequency == ProcessingFrequencyEnum.MANUALLY);

        // Execute the aggregation (if need) before saving
        if (needExecution) {

            // Should clean existing table
            if (!isNew) {
                tablesToDrop.addAll(Beans.getList(target.getTableNames()));
            }

            // Execute the aggregation
            {
                // Prepare a executable type (with label=format)
                AggregationTypeVO executableType = new AggregationTypeVO();
                executableType.setLabel(source.getRawFormatLabel());
                executableType.setCategory(source.getCategory());

                // Execute the aggregation
                AggregationContextVO context = aggregate(executableType, filter, null);

                // Update product, using the aggregation result
                toProductVO(context, target);
            }

            // Copy some properties from the given type
            target.setName(source.getName());
            target.setUpdateDate(source.getUpdateDate());
            target.setDescription(source.getDescription());
            target.setDocumentation(source.getDocumentation());
            target.setStatusId(source.getStatusId());
            target.setRecorderDepartment(source.getRecorderDepartment());
            target.setRecorderPerson(source.getRecorderPerson());
        }

        // Not need new aggregation: update entity before saving
        else {
            Preconditions.checkArgument(StringUtils.equalsIgnoreCase(target.getLabel(), source.getLabel()), "Cannot update the label of an existing product");
            target.setName(source.getName());
            target.setUpdateDate(source.getUpdateDate());
            target.setDescription(source.getDescription());
            target.setDocumentation(source.getDocumentation());
            target.setComments(source.getComments());
            target.setStatusId(source.getStatusId());
            target.setIsSpatial(source.getIsSpatial());

            if (target.getRecorderDepartment() == null) {
                target.setRecorderDepartment(source.getRecorderDepartment());
            }
            if (target.getRecorderPerson() == null) {
                target.setRecorderPerson(source.getRecorderPerson());
            }
        }
        target.setStratum(source.getStratum());
        target.setFilter(filterAsString);
        target.setProcessingFrequencyId(frequency.getId());

        // Save the product
        target = extractionProductService.save(target);

        // Drop old tables
        dropTables(tablesToDrop);

        // Transform back to type
        return toAggregationType(target);
    }

    public AggregationContextVO aggregateDao(@NonNull ExtractionProductVO source,
                                             @Nullable ExtractionFilterVO filter,
                                             AggregationStrataVO strata) {
        Preconditions.checkNotNull(source.getLabel());

        ProductFormatEnum format = ProductFormatEnum.valueOf(AggSpecification.FORMAT_PREFIX + source.getLabel(), null);

        // Aggregate (create agg tables)
        return getDao(format).aggregate(source, filter, strata);
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

        return ListUtils.emptyIfNull(extractionProductService.findByFilter(filter, fetchOptions))
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

    protected AggregationTechResultVO createEmptyTechResult() {
        AggregationTechResultVO result = new AggregationTechResultVO();
        result.setData(Maps.newHashMap());;
        return result;
    }

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

    protected AggregationContextVO toContextVO(@NonNull ExtractionProductVO source,
                                               @NonNull String sheetName) {

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

    protected ExtractionFilterVO readFilter(String json) {
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
        if (async && taskExecutor.isPresent()) {
            taskExecutor.get().execute(() -> self().clean(context));
        }
        else {
            log.info("Cleaning aggregation #{}-{}", context.getLabel(), context.getId());
            getDao(context.getFormat()).clean(context);
        }
    }

    /**
     * Get self bean, to be able to use new transation
     * @return
     */
    protected AggregationService self() {
        return context.getBean("aggregationService", AggregationService.class);
    }

    protected <C extends AggregationContextVO, F extends ExtractionFilterVO, S extends AggregationStrataVO>
        AggregationDao<C, F, S> getDao(IExtractionFormat format) {

        AggregationDao dao = daosByFormat.get(format);
        if (dao == null) throw new SumarisTechnicalException("Unknown aggregation format (no targeted dao): " + format);
        return dao;
    }

    protected void copyIdAndUpdateDate(ExtractionProductVO source, ExtractionProductVO target) {
        target.setName(source.getName());
        target.setUpdateDate(source.getUpdateDate());
        target.setDescription(source.getDescription());
        target.setDocumentation(source.getDocumentation());
        target.setComments(source.getComments());
        target.setStatusId(source.getStatusId());
        target.setIsSpatial(source.getIsSpatial());

        if (target.getRecorderDepartment() == null) {
            target.setRecorderDepartment(source.getRecorderDepartment());
        }
        if (target.getRecorderPerson() == null) {
            target.setRecorderPerson(source.getRecorderPerson());
        }

        target.setStratum(source.getStratum());
        target.setProcessingFrequencyId(source.getProcessingFrequencyId());
    }

}

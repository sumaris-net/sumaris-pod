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
import com.google.common.collect.Sets;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.config.ExtractionAutoConfiguration;
import net.sumaris.core.config.ExtractionCacheConfiguration;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.dao.technical.cache.CacheTTL;
import net.sumaris.core.dao.technical.model.IEntity;
import net.sumaris.core.dao.technical.schema.SumarisDatabaseMetadata;
import net.sumaris.core.dao.technical.schema.SumarisTableMetadata;
import net.sumaris.core.event.config.ConfigurationEvent;
import net.sumaris.core.event.config.ConfigurationReadyEvent;
import net.sumaris.core.event.config.ConfigurationUpdatedEvent;
import net.sumaris.core.exception.DataNotFoundException;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.model.referential.StatusEnum;
import net.sumaris.core.model.referential.location.Location;
import net.sumaris.core.model.referential.location.LocationLevelEnum;
import net.sumaris.core.model.technical.extraction.ExtractionCategoryEnum;
import net.sumaris.core.model.technical.extraction.ExtractionProduct;
import net.sumaris.core.model.technical.extraction.IExtractionType;
import net.sumaris.core.model.technical.history.ProcessingFrequency;
import net.sumaris.core.model.technical.history.ProcessingFrequencyEnum;
import net.sumaris.core.service.referential.LocationService;
import net.sumaris.core.service.referential.ReferentialService;
import net.sumaris.core.util.*;
import net.sumaris.core.vo.technical.extraction.*;
import net.sumaris.extraction.core.config.ExtractionConfiguration;
import net.sumaris.extraction.core.dao.administration.ExtractionStrategyDao;
import net.sumaris.extraction.core.dao.technical.Daos;
import net.sumaris.extraction.core.dao.technical.csv.ExtractionCsvDao;
import net.sumaris.extraction.core.dao.technical.schema.SumarisTableMetadatas;
import net.sumaris.extraction.core.dao.technical.table.ExtractionTableColumnOrder;
import net.sumaris.extraction.core.dao.technical.table.ExtractionTableDao;
import net.sumaris.extraction.core.dao.trip.ExtractionTripDao;
import net.sumaris.extraction.core.specification.administration.StratSpecification;
import net.sumaris.extraction.core.specification.data.trip.RdbSpecification;
import net.sumaris.extraction.core.type.LiveExtractionTypeEnum;
import net.sumaris.extraction.core.util.ExtractionTypes;
import net.sumaris.extraction.core.vo.*;
import net.sumaris.extraction.core.vo.administration.ExtractionStrategyFilterVO;
import net.sumaris.core.vo.technical.extraction.ExtractionTypeFilterVO;
import net.sumaris.extraction.core.vo.trip.ExtractionTripFilterVO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * @author blavenie
 */
@Slf4j
@Service("extractionManager")
@ConditionalOnBean({ExtractionAutoConfiguration.class})
public class ExtractionManagerImpl implements ExtractionManager {

    private final ExtractionConfiguration configuration;
    private final DataSource dataSource;

    private final ExtractionTripDao extractionRdbTripDao;
    private final ExtractionStrategyDao extractionStrategyDao;
    private final ExtractionCsvDao extractionCsvDao;

    private final ExtractionProductService productService;
    private final LocationService locationService;
    private final ReferentialService referentialService;
    private final SumarisDatabaseMetadata databaseMetadata;

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired(required = false)
    private CacheManager cacheManager;

    @Autowired(required = false)
    private Executor extractionExecutor;

    @Autowired
    private ExtractionService extractionService;

    @Autowired
    private AggregationService aggregationService;

    @Autowired
    private ExtractionTypeService extractionTypeService;

    private boolean enableProduct = false;
    private boolean enableTechnicalTablesUpdate = false;
    private CacheTTL cacheDefaultTtl;

    public ExtractionManagerImpl(ExtractionConfiguration configuration,
                                 DataSource dataSource,
                                 SumarisDatabaseMetadata databaseMetadata,
                                 ExtractionTripDao extractionRdbTripDao,
                                 ExtractionStrategyDao extractionStrategyDao,
                                 ExtractionCsvDao extractionCsvDao,
                                 ExtractionProductService productService,
                                 LocationService locationService,
                                 ReferentialService referentialService
    ) {
        this.configuration = configuration;
        this.dataSource = dataSource;
        this.databaseMetadata = databaseMetadata;

        this.extractionRdbTripDao = extractionRdbTripDao;
        this.extractionStrategyDao = extractionStrategyDao;
        this.extractionCsvDao = extractionCsvDao;

        this.productService = productService;
        this.locationService = locationService;
        this.referentialService = referentialService;
    }

    @PostConstruct
    protected void init() {
        enableProduct = configuration.enableExtractionProduct();

        // Register enum extraction types
        this.extractionTypeService.registerLiveTypes(extractionService.getTypes());
        this.extractionTypeService.registerLiveTypes(aggregationService.getTypes());
    }

    @EventListener({ConfigurationReadyEvent.class, ConfigurationUpdatedEvent.class})
    protected void onConfigurationReady(ConfigurationEvent event) {

        // Update technical tables (if option changed)
        if (this.enableTechnicalTablesUpdate != configuration.enableTechnicalTablesUpdate()) {
            this.enableTechnicalTablesUpdate = configuration.enableTechnicalTablesUpdate();

            // Init rectangles
            if (this.enableTechnicalTablesUpdate) initRectangleLocations();
        }

        CacheTTL cacheDefaultTtl = configuration.getExtractionCacheDefaultTtl();
        if (cacheDefaultTtl == null) {
            cacheDefaultTtl = CacheTTL.DEFAULT;
        }
        boolean enableProduct = configuration.enableExtractionProduct();

        // Update if need
        if (this.enableProduct != enableProduct || this.cacheDefaultTtl != cacheDefaultTtl) {
            this.enableProduct = enableProduct;
            this.cacheDefaultTtl = cacheDefaultTtl;

            log.info("Extraction manager started with {cacheDefaultTtl: '{}' ({}), enableProduct: {}}",
                this.cacheDefaultTtl.name(),
                DurationFormatUtils.formatDuration(this.cacheDefaultTtl.asDuration().toMillis(), "H:mm:ss", true),
                enableProduct);

            this.clearCache();
        }
    }

    @Override
    public IExtractionType getById(int id) {
        Preconditions.checkArgument(id >= 0);
        return getByExample(ExtractionTypeVO.builder().id(id).build());
    }

    @Override
    public IExtractionType getByExample(@NonNull IExtractionType source) {
        return getByExample(source, ExtractionProductFetchOptions.TABLES_AND_RECORDER);
    }

    @Override
    @Cacheable(cacheNames = ExtractionCacheConfiguration.Names.PRODUCT_BY_EXAMPLE,
        key = "#source.id + #source.label + #source.format + #source.version + #fetchOptions.hashCode()",
        condition = " #source != null", unless = "#result == null")
    public IExtractionType getByExample(@NonNull IExtractionType source, @NonNull ExtractionProductFetchOptions fetchOptions) {
        return extractionTypeService.getByExample(source, fetchOptions);
    }

    @Caching(evict = {
        @CacheEvict(cacheNames = ExtractionCacheConfiguration.Names.EXTRACTION_TYPES, allEntries = true),
        @CacheEvict(cacheNames = ExtractionCacheConfiguration.Names.PRODUCT_BY_EXAMPLE, allEntries = true),
        @CacheEvict(cacheNames = ExtractionCacheConfiguration.Names.PRODUCT_BY_ID_AND_OPTIONS, allEntries = true)
    })
    protected void clearCache() {
        if (this.cacheManager == null) return; // Skip

        log.debug("Cleaning {Extraction} caches...");

        // Clear all rows cache (by TTL)
        Arrays.stream(CacheTTL.values())
            .map(ttl -> {
                try {
                    return cacheManager.getCache(ExtractionCacheConfiguration.Names.EXTRACTION_ROWS_PREFIX + ttl.name());
                }
                catch (Exception e) {
                    // Cache not exists: skip
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .forEach(Cache::clear);
    }
    @Override
    public <R extends ExtractionResultVO> R executeAndRead(@NonNull IExtractionType type,
                                                           @Nullable ExtractionFilterVO filter,
                                                           @Nullable AggregationStrataVO strata,
                                                           Page page) {
        // Make sure type has category AND label filled
        type = getByExample(type, ExtractionProductFetchOptions.TABLES_AND_COLUMNS);


        ExtractionContextVO context;
        try {
            // Mark as preview
            filter = ExtractionFilterVO.nullToEmpty(filter);
            filter.setPreview(true);

            // Execute extraction into temp tables
            context = execute(type, filter, strata);
        } catch (DataNotFoundException e) {
            return (R)createEmptyResult();
        }

        // Force commit
        Daos.commitIfHsqldbOrPgsql(dataSource);

        try {
            // Create a read filter, with sheetname only, because already applied by execute()
            ExtractionFilterVO readFilter = ExtractionFilterVO.builder()
                .sheetName(filter.getSheetName())
                .build();

            // Read
            return (R)read(context, readFilter, strata, page);
        } finally {
            // Clean created tables
            asyncClean(context);
        }

    }

    @Override
    public ExtractionResultVO executeAndRead(IExtractionType type, @Nullable ExtractionFilterVO filter, Page page) {
        return executeAndRead(type, filter, null, page);
    }

    @Override
    public ExtractionResultVO executeAndReadWithCache(@NonNull IExtractionType type,
                                                      @Nullable ExtractionFilterVO filter,
                                                      @Nullable AggregationStrataVO strata,
                                                      @NonNull Page page,
                                                      @Nullable CacheTTL ttl) {
        Preconditions.checkNotNull(this.cacheManager, "Cache has been disabled by configuration. Please enable cache before retry");

        filter = ExtractionFilterVO.nullToEmpty(filter);
        ttl = CacheTTL.nullToDefault(CacheTTL.nullToDefault(ttl, this.cacheDefaultTtl), CacheTTL.DEFAULT);

        Integer cacheKey = new HashCodeBuilder(17, 37)
            .append(type)
            .append(filter)
            .append(page)
            .append(strata)
            // Not need to use TTL in cache key, because using a cache by TTL
            //.append(ttl)
            .build();

        // Get cache (if exists)
        Cache<Integer, ExtractionResultVO> cache = cacheManager.getCache(ExtractionCacheConfiguration.Names.EXTRACTION_ROWS_PREFIX + ttl.name());

        ExtractionResultVO result = cache.get(cacheKey);
        if (result != null) return result;

        // Get extraction rows
        result = executeAndRead(type, filter, page);

        // Add result to cache
        cache.put(cacheKey, result);

        return result;
    }

    @Override
    public ExtractionResultVO executeAndReadWithCache(@NonNull IExtractionType type,
                                                      @Nullable ExtractionFilterVO filter,
                                                      @NonNull Page page,
                                                      @Nullable CacheTTL ttl) {
        return executeAndReadWithCache(type, filter, null, page, ttl);
    }

    protected ExtractionResultVO read(@NonNull ExtractionContextVO context,
                                      ExtractionFilterVO filter,
                                      AggregationStrataVO strata,
                                      Page page) {
        filter = ExtractionFilterVO.nullToEmpty(filter);

        // Read aggregation
        if (ExtractionTypes.isAggregation(context)) {
            return aggregationService.readBySpace(context, filter, strata, page);
        }

        // Get simple extraction
        else {
            return extractionService.read(context, filter, page);
        }
    }

    @Override
    public ExtractionContextVO execute(@NonNull IExtractionType type, ExtractionFilterVO filter) {
        return execute(type, filter, null);
    }

    @Override
    public ExtractionProductVO executeAndSave(IExtractionType source, ExtractionFilterVO filter, AggregationStrataVO strata) {

        ExtractionProductVO target = new ExtractionProductVO(source);

        // Execute extraction
        try {
            ExtractionContextVO context = execute(source, filter, strata);
            // Fill tables from context
            toProductVO(context, target);
        }
        catch (DataNotFoundException e) {
            // no data: clear tables
            target.setTables(null);
        }

        target.setFilterContent(writeFilterAsString(filter));

        return productService.save(target);
    }

    @Override
    public ExtractionContextVO execute(IExtractionType source, ExtractionFilterVO filter, AggregationStrataVO strata) {
        IExtractionType parent = getParent(source);

        filter = ExtractionFilterVO.nullToEmpty(filter);

        // If has parent
        if (parent != null) {
            // Refresh parent if need (if not an existing product)
            if (!ExtractionTypes.isProduct(parent)) {
                // Execute the parent
                ExtractionContextVO parentResult = execute(parent, filter, strata);
                parent = toProductVO(parentResult);
            }

            // Set as source's parent
            source = new ExtractionProductVO(source);
            ((ExtractionProductVO) source).setParent(parent);
        }

        // Aggregation
        if (ExtractionTypes.isAggregation(source)) {
            return aggregationService.aggregate(source, filter, strata);
        }

        // Product extraction: nothing to do
        if (ExtractionTypes.isProduct(source)) {
            IExtractionType type = getByExample(source, ExtractionProductFetchOptions.TABLES_AND_RECORDER);
            Preconditions.checkArgument(type instanceof ExtractionProductVO);
            return new ExtractionProductContextVO((ExtractionProductVO)source);
        }

        // Live extraction
        else {
            IExtractionType type = getByExample(source);
            return extractionService.execute(type, filter);
        }
    }

    @Override
    public File executeAndDump(@NonNull IExtractionType type, ExtractionFilterVO filter, AggregationStrataVO strata) {

        // Execute extraction
        ExtractionContextVO context = execute(type, filter, strata);
        Daos.commitIfHsqldbOrPgsql(dataSource);

        // Dump to file
        try {
            return dumpTablesToFile(context, filter);
        }
        finally {
            clean(context);
        }
    }

    @Override
    public File executeAndDump(@NonNull IExtractionType type, ExtractionFilterVO filter) {
        return executeAndDump(type, filter, null);
    }

    @Override
    public File executeAndDumpTrips(LiveExtractionTypeEnum format, ExtractionTripFilterVO tripFilter) {
        String tripSheetName = ArrayUtils.isNotEmpty(format.getSheetNames()) ? format.getSheetNames()[0] : RdbSpecification.TR_SHEET_NAME;
        ExtractionFilterVO filter = extractionRdbTripDao.toExtractionFilterVO(tripFilter, tripSheetName);
        return executeAndDump(format, filter);
    }

    @Override
    public File executeAndDumpStrategies(LiveExtractionTypeEnum format, ExtractionStrategyFilterVO strategyFilter) {
        String strategySheetName = ArrayUtils.isNotEmpty(format.getSheetNames()) ? format.getSheetNames()[0] : StratSpecification.ST_SHEET_NAME;
        ExtractionFilterVO filter = extractionStrategyDao.toExtractionFilterVO(strategyFilter, strategySheetName);
        return executeAndDump(format, filter);
    }

    @Override
    public File dumpTablesToFile(ExtractionContextVO context,
                                 @Nullable ExtractionFilterVO filter) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(context.getFormat());

        if (CollectionUtils.isEmpty(context.getTableNames())) return null;

        // Dump table to CSV files
        log.debug(String.format("Extraction #%s > Creating CSV files...", context.getId()));

        String dateStr = Dates.formatDate(new Date(context.getId()), "yyyy-MM-dd-HHmm");
        String basename = context.getFormat() + "-" + dateStr;

        final ExtractionFilterVO tableFilter = ExtractionFilterVO.nullToEmpty(filter);
        final Set<String> defaultExcludeColumns = SetUtils.emptyIfNull(tableFilter.getExcludeColumnNames());
        final boolean defaultEnableDistinct = filter != null && filter.isDistinct();

        File outputDirectory = createTempDirectory(basename);
        List<File> outputFiles = context.getTableNames().stream()
                .map(tableName -> {
                    try {
                        // Add table's hidden columns has excluded columns
                        Set<String> hiddenColumns = context.getHiddenColumns(tableName);

                        boolean enableDistinct = defaultEnableDistinct ||
                                // Force distinct, when excluded columns AND distinct option on the XML query
                                (CollectionUtils.isNotEmpty(hiddenColumns) && context.isDistinctEnable(tableName));

                        tableFilter.setExcludeColumnNames(SetUtils.union(defaultExcludeColumns,
                                SetUtils.emptyIfNull(hiddenColumns)));
                        tableFilter.setDistinct(enableDistinct);

                        // Compute the table output file
                        File tempCsvFile = new File(outputDirectory, context.getSheetName(tableName) + ".csv");
                        dumpTableToFile(tableName, tableFilter, tempCsvFile);
                        return tempCsvFile;
                    } catch (IOException e) {
                        log.error(String.format("Could not generate CSV file for table {%s}", tableName), e);
                        throw new SumarisTechnicalException(e);
                    }
                })
                .collect(Collectors.toList());

        File outputFile;

        // One file: copy to result file
        if (outputFiles.size() == 1) {
            File uniqueFile = outputFiles.get(0);
            basename = String.format("%s-%s-%s.%s",
                    context.getFormat(),
                    Files.getNameWithoutExtension(uniqueFile),
                    dateStr,
                    Files.getExtension(uniqueFile).orElse("csv"));
            outputFile = new File(outputDirectory.getParent(), basename);
            try {
                FileUtils.moveFile(uniqueFile, outputFile);
            } catch (IOException e) {
                throw new SumarisTechnicalException(e);
            }
            log.debug(String.format("Extraction file created at {%s}", outputFile.getPath()));
        }

        // Many files: create a zip archive
        else {
            outputFile = new File(outputDirectory.getParent(), basename + ".zip");
            log.debug(String.format("Creating extraction file {%s}...", outputFile.getPath()));
            try {
                ZipUtils.compressFilesInPath(outputDirectory, outputFile, false);
            } catch (IOException e) {
                throw new SumarisTechnicalException(e);
            }
            log.debug(String.format("Extraction file created at {%s}", outputFile.getPath()));
        }

        return outputFile;
    }

    @Override
    public void executeAll(@NonNull ProcessingFrequencyEnum frequency) {
        if (!enableProduct) {
            throw new IllegalStateException("Cannot update extraction products. Service is not ready, or not enabled");
        }

        if (frequency == ProcessingFrequencyEnum.NEVER) {
            throw new IllegalArgumentException(String.format("Cannot update extraction products with frequency '%s'", frequency));
        }

        long now = System.currentTimeMillis();
        log.info("Updating {} extraction products...", frequency.name().toLowerCase());

        // Get products to refresh
        List<ExtractionProductVO> products = productService.findByFilter(ExtractionTypeFilterVO.builder()
                // Filter on public or private products
                .statusIds(new Integer[]{StatusEnum.ENABLE.getId(), StatusEnum.TEMPORARY.getId()})
                // With the expected frequency
                .searchJoin(ExtractionProduct.Fields.PROCESSING_FREQUENCY)
                .searchAttribute(ProcessingFrequency.Fields.LABEL)
                .searchText(frequency.getLabel())
                .build(),
            ExtractionProductFetchOptions.MINIMAL);

        if (CollectionUtils.isEmpty(products)) {
            log.info("Updating {} extractions [OK] - No extraction found.", frequency.name().toLowerCase());
            return;
        }

        int successCount = 0;
        int errorCount = 0;

        for (ExtractionProductVO product: products) {
            try {
                executeAndSave(product.getId());
                successCount++;
            }
            catch(Throwable e) {
                log.error("Error while updating extraction product #{} {label: '{}'}: {}", product.getId(), product.getLabel(), e.getMessage(), e);
                errorCount++;

                // TODO: add to processing history
            }
        }

        log.info("Updating {} extraction products [OK] in {}ms (success: {}, errors: {})",
            frequency.name().toLowerCase(),
            System.currentTimeMillis() - now,
            successCount, errorCount);
    }
    
    /*@Caching(
        evict = {
            @CacheEvict(cacheNames = ExtractionCacheConfiguration.Names.PRODUCT_BY_ID_AND_OPTIONS, allEntries = true),
            @CacheEvict(cacheNames = ExtractionCacheConfiguration.Names.PRODUCT_BY_EXAMPLE, allEntries = true),
            @CacheEvict(cacheNames = ExtractionCacheConfiguration.Names.EXTRACTION_TYPES, allEntries = true)
        }
    )*/
    public ExtractionProductVO executeAndSave(int id) {

        long startTime = System.currentTimeMillis();
        log.info("Updating product #{}...", id);

        ExtractionProductVO source = productService.findById(id, ExtractionProductFetchOptions.FOR_UPDATE)
            .orElseThrow(() -> new DataNotFoundException(String.format("Unknown product {id: %s}", id)));

        // Read filter
        ExtractionFilterVO filter = parseFilter(source.getFilterContent());

        ExtractionProductVO target = executeAndSave(source, filter, null);

        log.info("Updating product #{} [OK] in {}", id, TimeUtils.printDurationFrom(startTime));

        return target;
    }


    /* -- protected -- */

    protected List<ExtractionTypeVO> getProductExtractionTypes(ExtractionTypeFilterVO filter) {
        Preconditions.checkNotNull(filter);

        return ListUtils.emptyIfNull(
            productService.findByFilter(filter, ExtractionProductFetchOptions.builder()
                .withRecorderDepartment(true)
                .withTables(true)
                .build()))
            .stream()
            .map(this::toExtractionTypeVO)
            .collect(Collectors.toList());
    }

    protected void dumpTableToFile(String tableName, ExtractionFilterVO filter, File outputFile) throws IOException {
        SumarisTableMetadata table;
        try {
            table = databaseMetadata.getTable(tableName);
        } catch (Exception e) {
            log.debug(String.format("Table %s not found. Skipping", tableName));
            return;
        }

        boolean enableDistinct = filter != null && filter.isDistinct();

        Set<String> columnNames = table.getColumnNames();
        String[] orderedColumnNames = ExtractionTableColumnOrder.COLUMNS_BY_TABLE.get(tableName);
        if (orderedColumnNames != null) {
            final Set<String> newOrderedColumns = Sets.newLinkedHashSet();
            Arrays.stream(orderedColumnNames)
                .filter(table::hasColumn)
                .forEach(newOrderedColumns::add);
            columnNames = newOrderedColumns;
        }
        // Excludes some columns
        if (filter != null && CollectionUtils.isNotEmpty(filter.getExcludeColumnNames())) {
            columnNames = columnNames.stream()
                .filter(column -> !filter.getExcludeColumnNames().contains(column))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        }

        String whereClause = SumarisTableMetadatas.getSqlWhereClause(table, filter);
        String query = table.getSelectQuery(enableDistinct, columnNames, whereClause, null, null);

        extractionCsvDao.dumpQueryToCSV(outputFile, query,
            getAliasByColumnMap(columnNames),
            null,
            null,
            null);

    }

    protected boolean initRectangleLocations() {
        boolean updateAreas = false;
        try {
            // Insert missing rectangles
            long statisticalRectanglesCount = referentialService.countByLevelId(Location.class.getSimpleName(), LocationLevelEnum.RECTANGLE_ICES.getId())
                + referentialService.countByLevelId(Location.class.getSimpleName(), LocationLevelEnum.RECTANGLE_GFCM.getId());
            if (statisticalRectanglesCount == 0) {
                locationService.insertOrUpdateRectangleLocations();
                updateAreas = true;
            }

            // FIXME - We don't really need to store square 10x10, because extractions (and map) can compute it dynamically, using lat/long
            // Insert missing squares
            /*long square10minCount = referentialService.countByLevelId(Location.class.getSimpleName(), LocationLevelEnum.SQUARE_10.getId());
            if (square10minCount == 0) {
                locationService.insertOrUpdateSquares10();
                updateAreas = true;
            }*/

            if (updateAreas) {
                // Update area
                locationService.insertOrUpdateRectangleAndSquareAreas();

                // Update location hierarchy
                locationService.updateLocationHierarchy();
            }
            return true;

        } catch (Throwable t) {
            log.error("Error while initializing rectangle locations: " + t.getMessage(), t);
            return false;
        }

    }

    protected File createTempDirectory(String dirName) {
        try {
            File outputDirectory = new File(configuration.getTempDirectory(), dirName);
            int counter = 1;
            while (outputDirectory.exists()) {
                outputDirectory = new File(configuration.getTempDirectory(), dirName + "_" + counter++);
            }
            FileUtils.forceMkdir(outputDirectory);
            return outputDirectory;
        } catch (IOException e) {
            throw new SumarisTechnicalException("Could not create temporary directory for extraction", e);
        }
    }

    protected Map<String, String> getAliasByColumnMap(Set<String> tableNames) {
        return tableNames.stream()
            .collect(Collectors.toMap(
                String::toUpperCase,
                StringUtils::underscoreToChangeCase));
    }

    protected ExtractionResultVO createEmptyResult() {
        ExtractionResultVO result = new ExtractionResultVO();
        result.setColumns(ImmutableList.of());
        result.setTotal(0);
        result.setRows(ImmutableList.of());
        return result;
    }

    protected ExtractionTypeVO toExtractionTypeVO(ExtractionProductVO product) {
        ExtractionTypeVO type = new ExtractionTypeVO();
        toExtractionTypeVO(product, type);
        return type;
    }

    protected void toExtractionTypeVO(ExtractionProductVO source, ExtractionTypeVO target) {

        Beans.copyProperties(source, target);
        target.setLabel(source.getLabel());

        // Recorder department
        target.setRecorderDepartment(source.getRecorderDepartment());
    }

    protected void clean(ExtractionContextVO context) {
        try {
            asyncClean(context).get();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    protected CompletableFuture<Void> asyncClean(ExtractionContextVO context) {
        if (context != null) {
            if (ExtractionTypes.isAggregation(context)) {
                aggregationService.clean((AggregationContextVO) context);
            } else {
                extractionService.clean(context);
            }
        }
        return CompletableFuture.completedFuture(null);
    }

    protected ExtractionProductVO toProductVO(ExtractionContextVO source) {
        if (source == null) return null;
        ExtractionProductVO target = new ExtractionProductVO();
        toProductVO(source, target);

        return target;
    }

    protected void toProductVO(ExtractionContextVO source, ExtractionProductVO target) {

        target.setFormat(source.getFormat());
        target.setVersion(source.getVersion());

        if (source instanceof AggregationContextVO) {
            List<ExtractionTableVO> tables = aggregationService.toProductTableVO((AggregationContextVO)source);
            target.setTables(tables);
        }
        else {
            List<ExtractionTableVO> tables = extractionService.toProductTableVO(source);
            target.setTables(tables);
        }
    }

    protected IExtractionType getParent(IExtractionType source) {
        if (source.getParent() != null) {
            return getByExample(source.getParent());
        }
        Integer parentId = source.getParentId();
        if (parentId != null) {
            if (parentId < 0) {
                log.warn("TODO: Trying to get parent from negative id -> avoid this case");
            }
            else {
                return getById(parentId);
            }
        }
        // Aggregation always has a parent : a LIVE extraction by default
        if (ExtractionTypes.isAggregation(source)) {
            IExtractionType aggLiveType = getByExample(ExtractionTypeVO.builder()
                .category(ExtractionCategoryEnum.LIVE)
                .format(source.getFormat())
                .version(source.getVersion())
                .build());
            return aggLiveType.getParent();
        }
        return null;
    }


    protected ExtractionFilterVO parseFilter(String json) {
        if (StringUtils.isBlank(json)) return null;

        try {
            return objectMapper.readValue(json, ExtractionFilterVO.class);
        }
        catch(JsonProcessingException e) {
            throw new SumarisTechnicalException(e);
        }
    }

    /*@Override
    @Caching(evict = {
        @CacheEvict(cacheNames = ExtractionCacheConfiguration.Names.PRODUCT_BY_ID_AND_OPTIONS, allEntries = true),
        @CacheEvict(cacheNames = ExtractionCacheConfiguration.Names.PRODUCT_BY_EXAMPLE, allEntries = true),
        @CacheEvict(cacheNames = ExtractionCacheConfiguration.Names.EXTRACTION_TYPES, allEntries = true)
    })
    public void save(ExtractionProductVO source, ExtractionFilterVO filter) {

        // If not given in arguments, parse the product's filter string
        filter = filter != null ? filter : parseFilter(source.getFilter());

        // Check if need aggregate (ig new or if filter changed)
        ProcessingFrequencyEnum frequency = source.getProcessingFrequencyId() != null
            ? ProcessingFrequencyEnum.valueOf(source.getProcessingFrequencyId())
            : ProcessingFrequencyEnum.MANUALLY;

        String filterAsString = writeFilterAsString(filter);

        boolean needUpdateData = (isNew || !Objects.equals(target.getFilter(), filterAsString))
            && (frequency == ProcessingFrequencyEnum.MANUALLY);

    }*/

    protected String writeFilterAsString(ExtractionFilterVO filter) {
        if (filter == null) return null;
        try {
            return objectMapper.writeValueAsString(filter);
        }
        catch(JsonProcessingException e) {
            throw new SumarisTechnicalException(e);
        }
    }
}

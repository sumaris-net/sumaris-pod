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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.config.ExtractionAutoConfiguration;
import net.sumaris.core.config.ExtractionCacheConfiguration;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.dao.technical.cache.CacheTTL;
import net.sumaris.core.dao.technical.extraction.ExtractionTableRepository;
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
import net.sumaris.core.model.technical.extraction.ExtractionProduct;
import net.sumaris.core.model.technical.extraction.IExtractionType;
import net.sumaris.core.model.technical.history.ProcessingFrequency;
import net.sumaris.core.model.technical.history.ProcessingFrequencyEnum;
import net.sumaris.core.service.referential.LocationService;
import net.sumaris.core.service.referential.ReferentialService;
import net.sumaris.core.util.*;
import net.sumaris.core.vo.technical.extraction.*;
import net.sumaris.extraction.core.config.ExtractionConfiguration;
import net.sumaris.extraction.core.dao.AggregationDao;
import net.sumaris.extraction.core.dao.AggregationDaoDispatcher;
import net.sumaris.extraction.core.dao.ExtractionDao;
import net.sumaris.extraction.core.dao.ExtractionDaoDispatcher;
import net.sumaris.extraction.core.dao.administration.ExtractionStrategyDao;
import net.sumaris.extraction.core.dao.technical.Daos;
import net.sumaris.extraction.core.dao.technical.csv.ExtractionCsvDao;
import net.sumaris.extraction.core.dao.technical.schema.SumarisTableUtils;
import net.sumaris.extraction.core.dao.technical.table.ExtractionTableColumnOrder;
import net.sumaris.extraction.core.dao.trip.ExtractionTripDao;
import net.sumaris.extraction.core.specification.administration.StratSpecification;
import net.sumaris.extraction.core.specification.data.trip.RdbSpecification;
import net.sumaris.extraction.core.type.LiveExtractionTypeEnum;
import net.sumaris.extraction.core.util.ExtractionProducts;
import net.sumaris.extraction.core.util.ExtractionTypes;
import net.sumaris.extraction.core.vo.*;
import net.sumaris.extraction.core.vo.administration.ExtractionStrategyFilterVO;
import net.sumaris.extraction.core.vo.trip.ExtractionTripFilterVO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.nuiton.i18n.I18n;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author blavenie
 */
@Slf4j
@Service("extractionService")
@RequiredArgsConstructor
@ConditionalOnBean({ExtractionAutoConfiguration.class})
public class ExtractionServiceImpl implements ExtractionService {

    private final ExtractionConfiguration configuration;
    private final DataSource dataSource;

    private final ExtractionTripDao extractionRdbTripDao;
    private final ExtractionStrategyDao extractionStrategyDao;
    private final ExtractionCsvDao extractionCsvDao;

    private final ExtractionProductService productService;
    private final SumarisDatabaseMetadata databaseMetadata;
    private final LocationService locationService;
    private final ReferentialService referentialService;

    private final ExtractionTableRepository extractionTableRepository;

    private final ObjectMapper objectMapper;
    private final Optional<CacheManager> cacheManager;

    private final ExtractionDaoDispatcher extractionDaoDispatcher;

    private final AggregationDaoDispatcher aggregationDaoDispatcher;

    private final ExtractionTypeService extractionTypeService;

    private boolean enableCache = false;
    private boolean enableProduct = false;
    private boolean enableTechnicalTablesUpdate = false;
    private CacheTTL cacheDefaultTtl;


    @PostConstruct
    protected void init() {
        enableProduct = configuration.enableExtractionProduct();
        enableCache = configuration.enableCache() && this.cacheManager.isPresent();

        // Register enum extraction types
        this.extractionTypeService.registerLiveTypes(extractionDaoDispatcher.getManagedTypes());
        this.extractionTypeService.registerLiveTypes(aggregationDaoDispatcher.getManagedTypes());

        // Update technical tables (if option changed)
        if (this.enableTechnicalTablesUpdate != configuration.enableTechnicalTablesUpdate()) {
            this.enableTechnicalTablesUpdate = configuration.enableTechnicalTablesUpdate();

            // Init rectangles
            if (this.enableTechnicalTablesUpdate) initRectangleLocations();
        }
    }

    @EventListener({ConfigurationReadyEvent.class, ConfigurationUpdatedEvent.class})
    public void onConfigurationReady(ConfigurationEvent event) {

        boolean enableCache = configuration.enableCache() && this.cacheManager != null;
        CacheTTL cacheDefaultTtl = configuration.getExtractionCacheDefaultTtl();
        if (cacheDefaultTtl == null) {
            cacheDefaultTtl = CacheTTL.DEFAULT;
        }
        boolean enableProduct = configuration.enableExtractionProduct();

        // Update if need
        if (this.enableCache != enableCache
            || this.cacheDefaultTtl != cacheDefaultTtl
            || this.enableProduct != enableProduct) {
            this.enableCache = enableCache;
            this.enableProduct = enableProduct;
            this.cacheDefaultTtl = cacheDefaultTtl;

            log.info("Started Extraction manager {enableCache: {}, cacheDefaultTtl: '{}' ({}), enableProduct: {}}",
                enableCache,
                this.cacheDefaultTtl.name(),
                DurationFormatUtils.formatDuration(this.cacheDefaultTtl.asDuration().toMillis(), "H:mm:ss", true),
                enableProduct);

            this.clearCache();
        }
    }

    @Override
    public IExtractionType getByExample(@NonNull IExtractionType source) {
        return getByExample(source, ExtractionProductFetchOptions.TABLES_AND_RECORDER);
    }

    @Override
    public IExtractionType getByExample(@NonNull IExtractionType source, @NonNull ExtractionProductFetchOptions fetchOptions) {
        return extractionTypeService.getByExample(source, fetchOptions);
    }

    @Override
    public ExtractionResultVO executeAndRead(@NonNull IExtractionType type,
                                             @Nullable ExtractionFilterVO filter,
                                             @Nullable AggregationStrataVO strata,
                                             @NonNull Page page,
                                             @Nullable CacheTTL ttl) {

        // Fetch type
        IExtractionType finalType = getByExample(type);
        ExtractionFilterVO finalFilter = ExtractionFilterVO.nullToEmpty(filter);

        // Get cached result
        return getCachedResultOrPut(finalType, finalFilter, strata, page,
            ExtractionCacheConfiguration.Names.EXTRACTION_ROWS_PREFIX, ttl,
            () -> executeAndRead(finalType, finalFilter, strata, page, true /*already fetched*/));
    }

    @Override
    public Map<String, ExtractionResultVO> executeAndReadMany(IExtractionType type,
                                                              @NonNull ExtractionFilterVO filter,
                                                              @Nullable AggregationStrataVO strata,
                                                              Page page,
                                                              @Nullable CacheTTL ttl) {

        // Fetch type
        IExtractionType finalType = getByExample(type);

        // Get cached result
        return getCachedResultOrPut(finalType, filter, strata, page,
            ExtractionCacheConfiguration.Names.EXTRACTION_ROWS_MANY_PREFIX, ttl,
            () -> executeAndReadMany(finalType, filter, strata, page, true /*already fetched*/));
    }

    @Override
    public ExtractionResultVO read(@NonNull IExtractionType type,
                                   ExtractionFilterVO filter,
                                   AggregationStrataVO strata,
                                   Page page,
                                   CacheTTL ttl) {
        return read(type, filter, strata, page, ttl, false);
    }

    @Override
    public Map<String, ExtractionResultVO> readMany(IExtractionType type, @NonNull ExtractionFilterVO filter, @Nullable AggregationStrataVO strata, Page page, @Nullable CacheTTL ttl) {
        Preconditions.checkArgument(CollectionUtils.isNotEmpty(filter.getSheetNames()));

        // Fetch type
        IExtractionType finalType = getByExample(type);

        return getCachedResultOrPut(type, filter, strata, page,
            ExtractionCacheConfiguration.Names.EXTRACTION_ROWS_MANY_PREFIX, ttl,
            () -> readMany(finalType, filter, strata, page, true /*already fetched*/));
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

        // Set product's filter
        target.setFilterContent(writeFilterAsString(filter));

        // Set product's strata, if not set yet
        if (strata != null && CollectionUtils.isEmpty(target.getStratum())) {
            target.setStratum(Lists.newArrayList(strata));
        }

        return productService.save(target, ExtractionProductSaveOptions.WITH_TABLES_AND_STRATUM);
    }

    @Override
    public ExtractionContextVO execute(IExtractionType source, ExtractionFilterVO filter, AggregationStrataVO strata) {
        IExtractionType parent = getParent(source);

        filter = ExtractionFilterVO.nullToEmpty(filter);

        // If has parent
        if (parent != null) {
            // Refresh parent (if need)
            if (ExtractionTypes.isLive(parent)) {
                ExtractionContextVO parentResult = execute(parent, filter, strata);
                parent = toProductVO(parentResult);
            }

            // Aggregation
            if (ExtractionTypes.isAggregation(source)) {
                return aggregationDaoDispatcher.execute(source, parent, filter, strata);
            }
            else if (ExtractionTypes.isProduct(parent)) {
                // TODO: merge filter, to add it to existing ?
                ExtractionProductVO parentProduct = (ExtractionProductVO)parent;
                log.info(parentProduct.getFilterContent());
            }
        }

        // Live extraction
        IExtractionType type = getByExample(ExtractionTypeVO.builder()
            .format(source.getFormat())
            .version(source.getVersion())
            .build());
        return extractionDaoDispatcher.execute(type, filter);
    }

    @Override
    public File executeAndDump(@NonNull IExtractionType type, ExtractionFilterVO filter, AggregationStrataVO strata) {

        // Execute extraction
        ExtractionContextVO context = execute(type, filter, strata);
        Daos.commitIfHsqldbOrPgsql(dataSource);

        // Make sure context has the valid type (need for file name)
        context.setType(type);

        // Dump to file
        try {
            return dumpTablesToFile(context, filter);
        }
        finally {
            clean(context);
        }
    }

    @Override
    public File executeAndDumpTrips(LiveExtractionTypeEnum format, ExtractionTripFilterVO tripFilter) {
        String tripSheetName = ArrayUtils.isNotEmpty(format.getSheetNames()) ? format.getSheetNames()[0] : RdbSpecification.TR_SHEET_NAME;
        ExtractionFilterVO filter = extractionRdbTripDao.toExtractionFilterVO(tripFilter, tripSheetName);
        return executeAndDump(format, filter, null);
    }

    @Override
    public File executeAndDumpStrategies(LiveExtractionTypeEnum format, ExtractionStrategyFilterVO strategyFilter) {
        String strategySheetName = ArrayUtils.isNotEmpty(format.getSheetNames()) ? format.getSheetNames()[0] : StratSpecification.ST_SHEET_NAME;
        ExtractionFilterVO filter = extractionStrategyDao.toExtractionFilterVO(strategyFilter, strategySheetName);
        return executeAndDump(format, filter, null);
    }

    @Override
    public ExtractionResultVO executeAndReadStrategies(LiveExtractionTypeEnum format, ExtractionStrategyFilterVO strategyFilter, Page page) {
        String strategySheetName = ArrayUtils.isNotEmpty(format.getSheetNames()) ? format.getSheetNames()[0] : StratSpecification.ST_SHEET_NAME;
        ExtractionFilterVO filter = extractionStrategyDao.toExtractionFilterVO(strategyFilter, strategySheetName);
        return executeAndRead(format, filter, null, page, true);
    }

    @Override
    public File dumpTablesToFile(ExtractionContextVO context,
                                 @Nullable ExtractionFilterVO filter) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(context.getFormat());

        if (CollectionUtils.isEmpty(context.getTableNames())) return null;

        // Dump table to CSV files
        log.debug(String.format("Extraction #%s > Creating CSV files...", context.getId()));

        Date date = context.getUpdateDate() != null ? context.getUpdateDate() : new Date();
        String dateStr = Dates.formatDate(date, "yyyy-MM-dd-HHmm");
        String basename = context.getFormat() + "-" + dateStr;

        final ExtractionFilterVO tableFilter = ExtractionFilterVO.nullToEmpty(filter);
        final Set<String> defaultExcludeColumns = SetUtils.emptyIfNull(tableFilter.getExcludeColumnNames());
        final boolean defaultEnableDistinct = filter != null && filter.isDistinct();

        File outputDirectory = createTempDirectory(basename);
        List<File> outputFiles = Beans.getStream(context.getTableNames())
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
                        File tempCsvFile = new File(outputDirectory, context.findSheetNameByTableName(tableName).orElse(tableName)
                            + ".csv");
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


    @Caching(evict = {
        @CacheEvict(cacheNames = ExtractionCacheConfiguration.Names.EXTRACTION_TYPES, allEntries = true),
        @CacheEvict(cacheNames = ExtractionCacheConfiguration.Names.EXTRACTION_TYPE_BY_EXAMPLE, allEntries = true),
        @CacheEvict(cacheNames = ExtractionCacheConfiguration.Names.PRODUCT_BY_ID, allEntries = true)
    })
    protected void clearCache() {
        if (this.cacheManager.isEmpty()) return; // Skip

        log.debug("Cleaning {Extraction} caches...");

        // Clear all rows cache (by TTL)
        Arrays.stream(CacheTTL.values())
            .map(ttl -> {
                try {
                    return cacheManager.get().getCache(ExtractionCacheConfiguration.Names.EXTRACTION_ROWS_PREFIX + ttl.name());
                }
                catch (Exception e) {
                    // Cache not exists: skip
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .forEach(Cache::clear);

        // Clear all rows (many sheetNames) cache (by TTL)
        Arrays.stream(CacheTTL.values())
            .map(ttl -> {
                try {
                    return cacheManager.get().getCache(ExtractionCacheConfiguration.Names.EXTRACTION_ROWS_MANY_PREFIX + ttl.name());
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
    public ExtractionProductVO executeAndSave(int id) {

        long startTime = System.currentTimeMillis();
        log.info("Updating product #{}...", id);

        ExtractionProductVO source = productService.findById(id, ExtractionProductFetchOptions.FOR_UPDATE)
            .orElseThrow(() -> new DataNotFoundException(I18n.t("sumaris.extraction.product.notFoundById", id)));

        // Read filter
        ExtractionFilterVO filter = parseFilter(source.getFilterContent());

        ExtractionProductVO target = executeAndSave(source, filter, null);

        log.info("Updating product #{} [OK] in {}", id, TimeUtils.printDurationFrom(startTime));

        return target;
    }

    @Override
    public AggregationTechResultVO readByTech(IExtractionType type, @Nullable ExtractionFilterVO filter, @Nullable AggregationStrataVO strata, String sort, SortDirection direction) {
        return aggregationDaoDispatcher.readByTech(type, filter, strata, sort, direction);
    }

    @Override
    public MinMaxVO getTechMinMax(IExtractionType type, @Nullable ExtractionFilterVO filter, @Nullable AggregationStrataVO strata) {
        return aggregationDaoDispatcher.getTechMinMax(type, filter, strata);
    }

    @Override
    public List<Map<String, String>> toListOfMap(ExtractionResultVO source) {
        if (source == null || CollectionUtils.isNotEmpty(source.getColumns())) return null;

        String[] columnNames = source.getColumns().stream()
            .map(ExtractionTableColumnVO::getLabel)
            .toArray(String[]::new);

        return Beans.getStream(source.getRows())
            .map(row -> {
                Map<String, String> rowMap = new LinkedHashMap<>();
                for (int i = 0; i < row.length; i++) {
                    rowMap.put(columnNames[i], row[i]);
                }
                return rowMap;
            }).collect(Collectors.toList());
    }

    @Override
    public ObjectNode toJsonMap(Map<String, ExtractionResultVO> source) {
        if (source == null) return null;
        final ObjectNode node = objectMapper.createObjectNode();
        source.entrySet().forEach(e -> {
            ArrayNode array = node.putArray(e.getKey());
            toJsonStream(e.getValue()).forEach(array::add);
        });
        return node;
    }

    @Override
    public ArrayNode toJsonArray(ExtractionResultVO source) {
        ArrayNode array = objectMapper.createArrayNode();
        toJsonStream(source).forEach(array::add);
        return array;
    }


    /**
     * Clean all temporary table (EXT_xxx and AGG_xxx) not used by any product
     */
    @Override
    public int dropTemporaryTables() {
        int count = 0 ;
        log.info("Cleaning temporary extraction tables...");

        // Get all products tables
        Set<String> productTableNames = Beans.getStream(extractionTableRepository.findAllDistinctTableName())
            .map(String::toUpperCase)
            .collect(Collectors.toSet());

        // Read all tables (EXT_*)
        {
            Set<String> extTableNames = databaseMetadata.findTableNamesByPrefix(ExtractionDao.TABLE_NAME_PREFIX);
            ExtractionContextVO context = new ExtractionContextVO();
            context.setTableNamePrefix(ExtractionDao.TABLE_NAME_PREFIX);
            extTableNames.stream()
                .filter(tableName -> !productTableNames.contains(tableName.toUpperCase()))
                .forEach(context::addRawTableName);
            count += CollectionUtils.size(context.getRawTableNames());
            // Apply drop
            extractionRdbTripDao.clean(context);
        }

        // Read all tables (AGG_*)
        {
            Set<String> aggTableNames = databaseMetadata.findTableNamesByPrefix(AggregationDao.TABLE_NAME_PREFIX);
            ExtractionContextVO context = new ExtractionContextVO();
            context.setTableNamePrefix(ExtractionDao.TABLE_NAME_PREFIX);
            aggTableNames.stream()
                .filter(tableName -> !productTableNames.contains(tableName.toUpperCase()))
                .forEach(context::addRawTableName);
            count += CollectionUtils.size(context.getRawTableNames());
            // Apply drop
            extractionRdbTripDao.clean(context);
        }

        if (count == 0) {
            log.info("No temporary extraction tables found");
        }
        else {
            log.info("{} temporary extraction tables dropped", count);
        }

        return count;
    }

    /* -- protected -- */




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

    protected Map<String, ExtractionResultVO> executeAndReadMany(@NonNull IExtractionType type,
                                                                               @NonNull ExtractionFilterVO filter,
                                                                               @Nullable AggregationStrataVO strata,
                                                                               @NonNull Page page,
                                                                               boolean skipFetchType) {
        Preconditions.checkArgument(CollectionUtils.isNotEmpty(filter.getSheetNames()));

        // Fetch type (if need)
        type = skipFetchType ? type : getByExample(type);

        ExtractionContextVO context = null;
        try {
            // Mark as NOT a preview (to force all sheet to be rendered)
            filter.setPreview(false);

            // Execute extraction into temp tables
            context = execute(type, filter, strata);

            // Force commit
            Daos.commitIfHsqldbOrPgsql(dataSource);
            Map<String, ExtractionResultVO> result = Maps.newHashMap();

            for (String sheetName: filter.getSheetNames()) {

                // Result has not the expected sheetname: skip
                if (!context.hasSheet(sheetName)) {
                    log.debug("No data found for sheetName {}, in extraction #{}. Skipping", sheetName, context.getId());
                    result.put(sheetName, createEmptyResult());
                }
                else {
                    try {
                        // Create a read filter, with sheetname only, because filter already applied by execute()
                        ExtractionFilterVO readFilter = ExtractionFilterVO.builder()
                            .sheetName(sheetName)
                            .build();

                        ExtractionResultVO sheetData = read(context, readFilter, strata, page, true /*already checked*/);
                        result.put(sheetName, sheetData);
                    } catch (DataNotFoundException e) {
                        result.put(sheetName, createEmptyResult());
                    }
                }
            }

            return result;
        }
        finally {
            // Clean created tables
            if (context != null) {
                asyncClean(context);
            }
        }
    }
    protected <R extends ExtractionResultVO> R executeAndRead(@NonNull IExtractionType type,
                                                              @NonNull ExtractionFilterVO filter,
                                                              @Nullable AggregationStrataVO strata,
                                                              @NonNull Page page,
                                                              boolean skipFetchType) {
        // Fetch type (if need)
        type = skipFetchType ? type : getByExample(type);

        ExtractionContextVO context = null;
        try {
            // Mark as preview
            filter.setPreview(true);

            // Execute extraction into temp tables
            context = execute(type, filter, strata);

            // Result has not the expected sheetname: skip
            if (filter.getSheetName() != null && !context.hasSheet(filter.getSheetName())) {
                log.debug("No sheetName to read, in extraction #{}. Skipping", context.getId());
                return (R)createEmptyResult();
            }

            // Force commit
            Daos.commitIfHsqldbOrPgsql(dataSource);

            // Create a read filter, with sheetname only, because filter already applied by execute()
            ExtractionFilterVO readFilter = ExtractionFilterVO.builder()
                .sheetName(filter.getSheetName())
                .build();

            // Read
            return (R)read(context, readFilter, strata, page, CacheTTL.NONE, true /*already checked*/);
        }
        catch (DataNotFoundException e) {
            return (R)createEmptyResult();
        }
        finally {
            // Clean created tables
            if (context != null) {
                asyncClean(context);
            }
        }
    }

    protected Map<String, ExtractionResultVO> readMany(@NonNull IExtractionType type,
                                                       @NonNull ExtractionFilterVO filter,
                                                       AggregationStrataVO strata,
                                                       Page page,
                                                       boolean skipFetchType) {
        Preconditions.checkArgument(CollectionUtils.isNotEmpty(filter.getSheetNames()));

        // Fetch type (if need)
        type = skipFetchType ? type : getByExample(type);
        Map<String, ExtractionResultVO> result = Maps.newHashMap();

        for (String sheetName: filter.getSheetNames()) {
            try {
                ExtractionResultVO sheetResult = read(type, filter, strata, page, true /*already checked*/);
                result.put(sheetName, sheetResult);
            } catch (DataNotFoundException e) {
                result.put(sheetName, createEmptyResult());
            }
        }

        return result;
    }

    protected ExtractionResultVO read(@NonNull IExtractionType type,
                                      ExtractionFilterVO filter,
                                      @Nullable AggregationStrataVO strata,
                                      @Nullable Page page,
                                      @Nullable CacheTTL ttl,
                                      boolean skipFetchType) {

        // Fetch type (if need)
        IExtractionType finalType = skipFetchType ? type : getByExample(type);
        ExtractionFilterVO finalFilter = ExtractionFilterVO.nullToEmpty(filter);

        return getCachedResultOrPut(finalType, finalFilter, strata, page,
            ExtractionCacheConfiguration.Names.EXTRACTION_ROWS_PREFIX, ttl,
            () -> read(finalType, finalFilter, strata, page, true /*already fetched*/));
    }

    protected ExtractionResultVO read(@NonNull IExtractionType type,
                                      ExtractionFilterVO filter,
                                      AggregationStrataVO strata,
                                      Page page,
                                      boolean skipFetchType) {

        // Fetch type (if need)
        type = skipFetchType ? type : getByExample(type);
        filter = ExtractionFilterVO.nullToEmpty(filter);

        // Use aggregation dao
        if (ExtractionTypes.isAggregation(type)) {
            return aggregationDaoDispatcher.read(type, filter, strata, page);
        }
        // Or simple extraction dao
        else {
            return extractionDaoDispatcher.read(type, filter, page);
        }
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

        Map<String, String> dateFormats = Maps.newHashMap();
        columnNames.stream().map(table::getColumnMetadata)
            .filter(SumarisTableUtils::isDateColumn)
            .forEach(column -> dateFormats.put(column.getName(), Dates.CSV_DATE_TIME));

        String whereClause = SumarisTableUtils.getSqlWhereClause(table, filter);
        String query = table.getSelectQuery(enableDistinct, columnNames, whereClause, null, null);

        Map<String, String> aliases = getAliasByColumnMap(columnNames);

        extractionCsvDao.dumpQueryToCSV(outputFile, query,
            aliases,
            dateFormats,
            null,
            null);

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

    protected Map<String, String> getAliasByColumnMap(Set<String> columnNames) {
        return columnNames.stream()
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
                aggregationDaoDispatcher.clean((AggregationContextVO) context);
            } else {
                extractionDaoDispatcher.clean(context);
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
        List<ExtractionTableVO> tables = toProductTableVO(source);
        target.setTables(tables);
    }


    protected List<ExtractionTableVO> toProductTableVO(ExtractionContextVO source) {
        final List<String> tableNames = ImmutableList.copyOf(source.getTableNames());
        return Beans.getStream(tableNames)
            .map(tableName -> {
                ExtractionTableVO table = new ExtractionTableVO();
                table.setTableName(tableName);

                // Keep rankOrder from original linked has map
                table.setRankOrder(tableNames.indexOf(tableName) + 1);

                // Label (=the sheet name)
                String sheetName = source.findSheetNameByTableName(tableName).orElse(tableName);
                table.setLabel(sheetName);
                table.setName(ExtractionProducts.getSheetDisplayName(source.getFormat(), sheetName));

                if (source instanceof AggregationContextVO) {
                    AggregationContextVO aggContext = (AggregationContextVO) source;
                    table.setIsSpatial(aggContext.hasSpatialColumn(tableName));

                    // Columns
                    List<ExtractionTableColumnVO> columns = toProductColumnVOs(aggContext, tableName,
                        ExtractionTableColumnFetchOptions.builder()
                            .withRankOrder(false) // skip rankOrder, because fill later, by format and sheetName (more accuracy)
                            .build());

                    // Fill rank order
                    ExtractionTableColumnOrder.fillRankOrderByTypeAndSheet(source, sheetName, columns);

                    table.setColumns(columns);
                }
                else {
                    table.setIsSpatial(false);
                }

                return table;
            })
            .collect(Collectors.toList());
    }

    protected List<ExtractionTableColumnVO> toProductColumnVOs(AggregationContextVO context, String tableName,
                                                               ExtractionTableColumnFetchOptions fetchOptions) {


        // Get columns (from table metadata), but exclude hidden columns
        List<ExtractionTableColumnVO> columns = productService.getColumns(tableName, fetchOptions);

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

    protected IExtractionType getParent(IExtractionType source) {
        return getParent(source, ExtractionProductFetchOptions.TABLES_AND_RECORDER);
    }

    protected IExtractionType getParent(IExtractionType source, ExtractionProductFetchOptions fetchOptions) {
        // Use the existing parent
        if (source.getParent() != null) {
            return getByExample(source.getParent(), fetchOptions);
        }

        // Get parent by id (if product)
        Integer parentId = source.getParentId();
        if (parentId != null && parentId >= 0) {
            return productService.get(parentId, fetchOptions);
        }

        // Aggregation always has a parent : a LIVE extraction by default
        if (ExtractionTypes.isAggregation(source)) {
            IExtractionType aggLiveType = getByExample(ExtractionTypeVO.builder()
                .format(source.getFormat())
                .version(source.getVersion())
                .build());
            return aggLiveType.getParent();
        }

        return null;
    }


    @Override
    public ExtractionFilterVO parseFilter(String jsonFilter) {
        if (StringUtils.isBlank(jsonFilter)) return null;

        try {
            return objectMapper.readValue(jsonFilter, ExtractionFilterVO.class);
        }
        catch(JsonProcessingException e) {
            throw new SumarisTechnicalException(e);
        }
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

    protected Stream<ObjectNode> toJsonStream(ExtractionResultVO source) {
        if (source == null) return Stream.empty();
        if (CollectionUtils.isEmpty(source.getColumns())) {
            log.warn("Cannot convert extraction result: missing columns. Is the result empty ?");
            return Stream.empty();
        }

        String[] columnNames = source.getColumns().stream()
            .map(ExtractionTableColumnVO::getLabel)
            .toArray(String[]::new);

        return Beans.getStream(source.getRows())
            .map(row -> {
                ObjectNode node = objectMapper.createObjectNode();
                for (int i = 0; i < row.length; i++) {
                    node.put(columnNames[i], row[i]);
                }
                return node;
            });
    }

    protected Integer computeCacheKey(IExtractionType type, ExtractionFilterVO filter, AggregationStrataVO strata, Page page) {
        // Compute a cache key
        // note: Not need to use TTL in cache key, because using a cache by TTL
        return new HashCodeBuilder(17, 37)
            .append(type)
            .append(filter)
            .append(strata)
            .append(page)
            .build();
    }

    protected <R> R getCachedResultOrPut(@NonNull IExtractionType type,
                                                                    @NonNull ExtractionFilterVO filter,
                                                                    @Nullable AggregationStrataVO strata, Page page,
                                                                    String cacheNamePrefix,
                                                                    @Nullable CacheTTL ttl,
                                                                    Supplier<R> supplier) {
        ttl = CacheTTL.nullToDefault(CacheTTL.nullToDefault(ttl, this.cacheDefaultTtl), CacheTTL.DEFAULT);

        // No cache: compute value
        if (!enableCache || ttl == CacheTTL.NONE) return supplier.get();

        // Compute a cache key
        Integer cacheKey = computeCacheKey(type, filter, strata, page);

        // Get the cache
        Cache<Integer, R> cache = cacheManager.get()
            .getCache(cacheNamePrefix + ttl.name());

        // Reuse cached value if exists
        R result = cache.get(cacheKey);
        if (result != null) return result;

        // Not exists in cache: compute it
        result = supplier.get();
        if (result == null) return null;

        // Add to cache
        cache.put(cacheKey, result);
        return result;
    }
}

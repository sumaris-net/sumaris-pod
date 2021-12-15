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
import com.google.common.collect.Sets;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.config.SumarisConfigurationOption;
import net.sumaris.core.dao.schema.DatabaseSchemaDao;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.dao.technical.cache.CacheTTL;
import net.sumaris.core.dao.technical.model.IEntity;
import net.sumaris.core.dao.technical.schema.SumarisDatabaseMetadata;
import net.sumaris.core.dao.technical.schema.SumarisTableMetadata;
import net.sumaris.core.event.config.ConfigurationEvent;
import net.sumaris.core.event.config.ConfigurationReadyEvent;
import net.sumaris.core.event.config.ConfigurationUpdatedEvent;
import net.sumaris.core.exception.DataNotFoundException;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.extraction.core.config.ExtractionAutoConfiguration;
import net.sumaris.extraction.core.config.ExtractionCacheConfiguration;
import net.sumaris.extraction.core.config.ExtractionConfiguration;
import net.sumaris.extraction.core.dao.ExtractionDao;
import net.sumaris.extraction.core.dao.administration.ExtractionStrategyDao;
import net.sumaris.extraction.core.dao.technical.Daos;
import net.sumaris.extraction.core.dao.technical.csv.ExtractionCsvDao;
import net.sumaris.extraction.core.dao.technical.schema.SumarisTableMetadatas;
import net.sumaris.extraction.core.dao.technical.table.ExtractionTableColumnOrder;
import net.sumaris.extraction.core.dao.technical.table.ExtractionTableDao;
import net.sumaris.extraction.core.dao.trip.ExtractionTripDao;
import net.sumaris.extraction.core.format.LiveFormatEnum;
import net.sumaris.extraction.core.specification.administration.StratSpecification;
import net.sumaris.extraction.core.specification.data.trip.RdbSpecification;
import net.sumaris.extraction.core.util.ExtractionFormats;
import net.sumaris.extraction.core.util.ExtractionProducts;
import net.sumaris.extraction.core.vo.*;
import net.sumaris.extraction.core.vo.administration.ExtractionStrategyFilterVO;
import net.sumaris.extraction.core.vo.filter.ExtractionTypeFilterVO;
import net.sumaris.extraction.core.vo.trip.ExtractionTripFilterVO;
import net.sumaris.core.model.referential.StatusEnum;
import net.sumaris.core.model.referential.location.Location;
import net.sumaris.core.model.referential.location.LocationLevelEnum;
import net.sumaris.core.model.technical.extraction.ExtractionCategoryEnum;
import net.sumaris.core.model.technical.extraction.IExtractionFormat;
import net.sumaris.core.model.technical.history.ProcessingFrequencyEnum;
import net.sumaris.core.service.referential.LocationService;
import net.sumaris.core.service.referential.ReferentialService;
import net.sumaris.core.util.*;
import net.sumaris.core.vo.technical.extraction.ExtractionProductFetchOptions;
import net.sumaris.core.vo.technical.extraction.ExtractionProductVO;
import net.sumaris.core.vo.technical.extraction.ExtractionTableColumnVO;
import net.sumaris.core.vo.technical.extraction.ExtractionTableVO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.core.task.TaskExecutor;
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
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author blavenie
 */
@Slf4j
@Service("extractionService")
@ConditionalOnBean({ExtractionAutoConfiguration.class})
public class ExtractionServiceImpl implements ExtractionService {

    private final ExtractionConfiguration configuration;
    private final ObjectMapper objectMapper;
    private final DataSource dataSource;
    private final ApplicationContext applicationContext;
    private final DatabaseSchemaDao databaseSchemaDao;

    private final ExtractionTripDao extractionRdbTripDao;
    private final ExtractionStrategyDao extractionStrategyDao;
    private final ExtractionTableDao extractionTableDao;
    private final ExtractionCsvDao extractionCsvDao;

    private final ExtractionProductService extractionProductService;
    private final LocationService locationService;
    private final ReferentialService referentialService;
    private final SumarisDatabaseMetadata databaseMetadata;

    @Autowired(required = false)
    private CacheManager cacheManager;

    @Autowired(required = false)
    private TaskExecutor taskExecutor;

    private boolean enableProduct = false;
    private boolean enableTechnicalTablesUpdate = false;
    private CacheTTL cacheDefaultTtl;

    private Map<IExtractionFormat, ExtractionDao<? extends ExtractionContextVO, ? extends ExtractionFilterVO>>
        daosByFormat = Maps.newHashMap();

    public ExtractionServiceImpl(ExtractionConfiguration configuration,
                                 ObjectMapper objectMapper,
                                 DataSource dataSource,
                                 ApplicationContext applicationContext, DatabaseSchemaDao databaseSchemaDao,
                                 SumarisDatabaseMetadata databaseMetadata,
                                 ExtractionTripDao extractionRdbTripDao,
                                 ExtractionStrategyDao extractionStrategyDao,
                                 ExtractionTableDao extractionTableDao,
                                 ExtractionCsvDao extractionCsvDao,
                                 ExtractionProductService extractionProductService,
                                 LocationService locationService,
                                 ReferentialService referentialService
    ) {
        this.configuration = configuration;
        this.objectMapper = objectMapper;
        this.dataSource = dataSource;
        this.applicationContext = applicationContext;
        this.databaseSchemaDao = databaseSchemaDao;
        this.databaseMetadata = databaseMetadata;

        this.extractionRdbTripDao = extractionRdbTripDao;
        this.extractionStrategyDao = extractionStrategyDao;
        this.extractionTableDao = extractionTableDao;
        this.extractionCsvDao = extractionCsvDao;

        this.extractionProductService = extractionProductService;
        this.locationService = locationService;
        this.referentialService = referentialService;
    }

    @PostConstruct
    protected void init() {
        enableProduct = configuration.enableExtractionProduct();

        // Register all extraction daos
        applicationContext.getBeansOfType(ExtractionDao.class).values()
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

    @EventListener({ConfigurationReadyEvent.class, ConfigurationUpdatedEvent.class})
    protected void onConfigurationReady(ConfigurationEvent event) {
        this.enableProduct = configuration.enableExtractionProduct();
        this.cacheDefaultTtl = configuration.getExtractionCacheDefaultTtl();
        if (this.cacheDefaultTtl == null) {
            this.cacheDefaultTtl = CacheTTL.DEFAULT;
        }

        log.info("Extraction configured with {cacheDefaultTtl: '{}' ({}), enableProduct: {}}",
            this.cacheDefaultTtl.name(),
            DurationFormatUtils.formatDuration(this.cacheDefaultTtl.asDuration().toMillis(), "H:mm:ss", true),
            this.enableProduct);

        // Update technical tables (if option changed)
        if (this.enableTechnicalTablesUpdate != configuration.enableTechnicalTablesUpdate()) {
            this.enableTechnicalTablesUpdate = configuration.enableTechnicalTablesUpdate();

            // Init rectangles
            if (this.enableTechnicalTablesUpdate) initRectangleLocations();
        }

    }

    @Override
    public ExtractionTypeVO getByFormat(@NonNull IExtractionFormat format) {
        return ExtractionFormats.findOneMatch(this.findAll(), format);
    }

    @Override
    public List<ExtractionTypeVO> getLiveExtractionTypes() {

        MutableInt id = new MutableInt(-1);
        return Arrays.stream(LiveFormatEnum.values())
            // Sort by label
            .sorted(Comparator.comparing(LiveFormatEnum::getLabel))
            .map(format -> {
                ExtractionTypeVO type = new ExtractionTypeVO();

                // Generate a negative an unique id
                type.setId(-1 * format.hashCode());

                type.setLabel(format.getLabel().toLowerCase());
                type.setCategory(ExtractionCategoryEnum.LIVE);
                type.setSheetNames(format.getSheetNames());
                type.setStatusId(StatusEnum.TEMPORARY.getId()); // = not public by default
                type.setVersion(format.getVersion());
                type.setLiveFormat(format);
                return type;
            })
            .collect(Collectors.toList());
    }

    public List<ExtractionTypeVO> findAll() {
        return findAll(null, null);
    }

    @Override
    @Cacheable(cacheNames = ExtractionCacheConfiguration.Names.EXTRACTION_TYPES)
    public List<ExtractionTypeVO> findAll(@Nullable ExtractionTypeFilterVO filter,
                                          @Nullable Page page) {
        if (page == null) {
            return this.findAll(filter, 0, 1000, null, null);
        }
        return this.findAll(filter, (int)page.getOffset(), page.getSize(), page.getSortBy(), page.getSortDirection());
    }

    @Override
    public ExtractionResultVO executeAndRead(ExtractionTypeVO type, ExtractionFilterVO filter,
                                             @NonNull Page page) {
        // Make sure type has category AND label filled
        type = getByFormat(type);

        filter = ExtractionFilterVO.nullToEmpty(filter);

        // Force preview
        filter.setPreview(true);

        switch (type.getCategory()) {
            case PRODUCT:
                ExtractionProductVO product = extractionProductService.getByLabel(type.getLabel(),
                        ExtractionProductFetchOptions.TABLES_AND_COLUMNS);
                Set<String> hiddenColumns = Beans.getStream(product.getTables())
                        .map(ExtractionTableVO::getColumns)
                        .filter(Objects::nonNull)
                        .flatMap(List::stream)
                        .filter(c -> "hidden".equalsIgnoreCase(c.getType()))
                        .map(ExtractionTableColumnVO::getColumnName)
                        .collect(Collectors.toSet());
                filter.setExcludeColumnNames(hiddenColumns);
                return readProductRows(product, filter, page);
            case LIVE:
                return extractLiveAndRead(type, filter, page);
            default:
                throw new SumarisTechnicalException(String.format("Extraction of category %s not implemented yet !", type.getCategory()));
        }
    }

    @Override
    public ExtractionResultVO executeAndReadWithCache(@NonNull ExtractionTypeVO type,
                                                      @Nullable ExtractionFilterVO filter,
                                                      @NonNull Page page,
                                                      @Nullable CacheTTL ttl) {
        Preconditions.checkNotNull(this.cacheManager, "Cache has been disabled by configuration. Please enable cache before retry");

        filter = ExtractionFilterVO.nullToEmpty(filter);
        ttl = CacheTTL.nullToDefault(ttl, this.cacheDefaultTtl);
        if (ttl == null) throw new IllegalArgumentException("Missing required 'ttl' argument");

        Integer cacheKey = new HashCodeBuilder(17, 37)
            .append(type)
            .append(filter)
            .append(page)
            .append(ttl)
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
    public ExtractionResultVO read(@NonNull ExtractionContextVO context,
                                   ExtractionFilterVO filter,
                                   Page page) {

        filter = filter != null ? filter : new ExtractionFilterVO();

        String tableName;
        if (StringUtils.isNotBlank(filter.getSheetName())) {
            tableName = context.getTableNameBySheetName(filter.getSheetName());
        } else {
            tableName = context.getTableNames().iterator().next();
        }

        // Missing the expected sheet = no data
        if (tableName == null) return createEmptyResult();

        // Create a filter for rows previous, with only includes/exclude columns,
        // because criterion are not need (already applied when writing temp tables)
        ExtractionFilterVO rowsFilter = new ExtractionFilterVO();
        rowsFilter.setIncludeColumnNames(filter.getIncludeColumnNames()); // Copy given include columns
        rowsFilter.setExcludeColumnNames(SetUtils.union(
            SetUtils.emptyIfNull(filter.getIncludeColumnNames()),
            SetUtils.emptyIfNull(context.getHiddenColumns(tableName))
        ));

        // Force distinct if there is excluded columns AND distinct is enable on the XML query
        boolean enableDistinct = filter.isDistinct() || CollectionUtils.isNotEmpty(rowsFilter.getExcludeColumnNames())
            && context.isDistinctEnable(tableName);
        rowsFilter.setDistinct(enableDistinct);

        // Get rows from exported tables
        return extractionTableDao.getRows(tableName, rowsFilter, page);

    }

    @Override
    public File executeAndDump(ExtractionTypeVO type, ExtractionFilterVO filter) {
        // Make sure type has category AND label filled
        type = getByFormat(type);

        filter = filter != null ? filter : new ExtractionFilterVO();

        // Force full extraction (not a preview)
        filter.setPreview(false);

        switch (type.getCategory()) {
            case PRODUCT:
                ExtractionProductVO product = extractionProductService.getByLabel(type.getLabel(),
                        ExtractionProductFetchOptions.builder()
                                .withRecorderDepartment(false)
                                .withRecorderPerson(false)
                                .withColumns(false)
                                .build());
                return dumpProductToFile(product, filter);
            case LIVE:
                LiveFormatEnum format = LiveFormatEnum.valueOf(type.getLabel().toUpperCase());
                return extractLiveAndDump(format, filter);
            default:
                throw new SumarisTechnicalException(String.format("Extraction of category %s not implemented yet !", type.getCategory()));
        }

    }

    @Override
    public ExtractionContextVO execute(@NonNull IExtractionFormat format, ExtractionFilterVO filter) {
        // Make sure type has category AND label filled
        ExtractionTypeVO type = getByFormat(format);

        filter = filter != null ? filter : new ExtractionFilterVO();

        // Force full extraction (not a preview)
        filter.setPreview(false);

        switch (type.getCategory()) {
            case PRODUCT:
                throw new IllegalArgumentException("execute not implemented yet for product");
                //    ExtractionProduct product = ExtractionProduct.valueOf(checkedType.getLabel().toUpperCase());
                //    return extractProductToTables(product, filter);
            case LIVE:
                return executeLiveDao(type.getLiveFormat(), filter);
            default:
                throw new SumarisTechnicalException(String.format("Extraction of category %s not implemented yet !", type.getCategory()));
        }
    }

    @Override
    public File executeAndDumpTrips(LiveFormatEnum format, ExtractionTripFilterVO tripFilter) {
        String tripSheetName = ArrayUtils.isNotEmpty(format.getSheetNames()) ? format.getSheetNames()[0] : RdbSpecification.TR_SHEET_NAME;
        ExtractionFilterVO filter = extractionRdbTripDao.toExtractionFilterVO(tripFilter, tripSheetName);
        return extractLiveAndDump(format, filter);
    }

    @Override
    public File executeAndDumpStrategies(LiveFormatEnum format, ExtractionStrategyFilterVO strategyFilter) {
        String strategySheetName = ArrayUtils.isNotEmpty(format.getSheetNames()) ? format.getSheetNames()[0] : StratSpecification.ST_SHEET_NAME;
        ExtractionFilterVO filter = extractionStrategyDao.toExtractionFilterVO(strategyFilter, strategySheetName);
        return extractLiveAndDump(format, filter);
    }

    @Override
    public void clean(ExtractionContextVO context) {
        clean(context, false);
    }

    @Override
    public ExtractionProductVO toProductVO(ExtractionContextVO source) {
        if (source == null) return null;
        ExtractionProductVO target = new ExtractionProductVO();
        toProductVO(source, target);

        return target;
    }

    @Override
    public ExtractionTypeVO save(ExtractionTypeVO source, ExtractionFilterVO filter) {
        Preconditions.checkNotNull(source);
        Preconditions.checkNotNull(source.getLabel(), "Missing 'type.label'");
        Preconditions.checkNotNull(source.getName(), "Missing 'type.name'");
        Preconditions.checkArgument(source.getId() == null || source.getId() >= 0); // Negative ID not allowed

        Collection<String> tablesToDrop = Lists.newArrayList();

        // Load the product
        ExtractionProductVO target = null;
        if (source.getId() != null) {
            target = extractionProductService.findById(source.getId(), ExtractionProductFetchOptions.FOR_UPDATE).orElse(null);
        }

        boolean isNew = target == null;
        if (isNew) {
            target = new ExtractionProductVO();
            target.setLabel(source.getLabel());
            target.setRecorderDepartment(source.getRecorderDepartment());
            target.setRecorderPerson(source.getRecorderPerson());
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

        // Execute the extraction
        if (needExecution) {
            // Should clean existing table
            if (!isNew) {
                tablesToDrop.addAll(Beans.getList(target.getTableNames()));
            }

            // Prepare a executable type (with label=format)
            ExtractionTypeVO executableType = new ExtractionTypeVO();
            executableType.setLabel(source.getRawFormatLabel());
            executableType.setCategory(source.getCategory());

            // Run the execution
            ExtractionContextVO context = execute(executableType, filter);

            // Update product, using the extraction result
            toProductVO(context, target);
        }

        // Copy some properties from the given type
        {
            target.setName(source.getName());
            target.setDescription(source.getDescription());
            target.setIsSpatial(false);

            // Default status
            Integer statusId = source.getStatusId() != null ? source.getStatusId() : StatusEnum.TEMPORARY.getId();
            target.setStatusId(statusId);

            // Frequency
            Integer frequencyId = source.getProcessingFrequencyId() != null ? source.getProcessingFrequencyId()
                : ProcessingFrequencyEnum.MANUALLY.getId();
            target.setProcessingFrequencyId(frequencyId);
        }

        // Save the product
        target = extractionProductService.save(target);

        // Drop old tables
        dropTables(tablesToDrop);

        // Transform back to type
        return toExtractionTypeVO(target);
    }

    @Override
    public File dumpTablesToFile(ExtractionContextVO context,
                                 @Nullable ExtractionFilterVO filter) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(context.getLabel());

        if (CollectionUtils.isEmpty(context.getTableNames())) return null;

        // Dump table to CSV files
        log.debug(String.format("Extraction #%s > Creating CSV files...", context.getId()));

        String dateStr = Dates.formatDate(new Date(context.getId()), "yyyy-MM-dd-HHmm");
        String basename = context.getLabel() + "-" + dateStr;

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
                    context.getLabel(),
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
    public CompletableFuture<Boolean> asyncClean(ExtractionContextVO context) {
        try {
            clean(context, true);
            return CompletableFuture.completedFuture(Boolean.TRUE);
        } catch (Exception e) {
            log.warn(String.format("Error while cleaning extraction #%s: %s", context.getId(), e.getMessage()), e);
            return CompletableFuture.completedFuture(Boolean.FALSE);
        }
    }

    /* -- protected -- */

    /**
     * @deprecated
     * @return
     */
    @Deprecated
    protected List<ExtractionTypeVO> getProductExtractionTypes(ExtractionTypeFilterVO filter) {
        Preconditions.checkNotNull(filter);

        return ListUtils.emptyIfNull(
            extractionProductService.findByFilter(filter, ExtractionProductFetchOptions.builder()
                .withRecorderDepartment(true)
                .withTables(true)
                .build()))
            .stream()
            .map(this::toExtractionTypeVO)
            .collect(Collectors.toList());
    }

    protected ExtractionResultVO extractLiveAndRead(ExtractionTypeVO type,
                                                    ExtractionFilterVO filter,
                                                    Page page) {
        Preconditions.checkNotNull(type);
        Preconditions.checkNotNull(type.getLiveFormat());

        filter.setPreview(true);

        // Replace default sort attribute
        if (IEntity.Fields.ID.equalsIgnoreCase(page.getSortBy())) {
            page.setSortBy(null);
        }

        // Execute extraction into temp tables
        ExtractionContextVO context;
        try {
            context = executeLiveDao(type.getLiveFormat(), filter);
        } catch (DataNotFoundException e) {
            return createEmptyResult();
        }

        try {
            // Read
            return read(context, filter, page);
        } finally {
            // Clean created tables
            clean(context, true);
        }
    }

    protected File extractLiveAndDump(LiveFormatEnum format,
                                      ExtractionFilterVO filter) {
        Preconditions.checkNotNull(format);

        // Execute live extraction to temp tables
        ExtractionContextVO context = executeLiveDao(format, filter);
        Daos.commitIfHsqldbOrPgsql(dataSource);

        try {
            log.info(String.format("Dumping tables of extraction #%s to files...", context.getId()));

            // Dump tables
            return dumpTablesToFile(context, null /*no filter, because already applied*/);
        }
        finally {
            clean(context);
        }
    }

    protected ExtractionResultVO readProductRows(@NonNull ExtractionProductVO product,
                                                 @NonNull ExtractionFilterVO filter,
                                                 @NonNull Page page) {
        Preconditions.checkArgument(page.getOffset() >= 0);
        Preconditions.checkArgument(page.getSize() >= 0, "'size' must be greater or equals to 0");
        Preconditions.checkArgument(page.getSize() <= 1000, "maximum value for 'size' is: 1000");

        // Get table name
        String tableName = ExtractionFormats.getTableName(product, filter.getSheetName());

        // Get table rows
        return extractionTableDao.getRows(tableName, filter, page);
    }

    protected File dumpProductToFile(ExtractionProductVO product, ExtractionFilterVO filter) {
        Preconditions.checkNotNull(product);
        Preconditions.checkNotNull(filter);

        // Create a new context
        ExtractionProductContextVO context = new ExtractionProductContextVO(product);
        context.setId(System.currentTimeMillis());

        // Dump to file
        return dumpTablesToFile(context, filter);
    }

    protected ExtractionContextVO executeLiveDao(LiveFormatEnum format, ExtractionFilterVO filter) {
        return getDao(format).execute(filter);
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
            columnNames = Sets.newLinkedHashSet(Arrays.asList(orderedColumnNames));
        }
        // Excludes some columns
        if (CollectionUtils.isNotEmpty(filter.getExcludeColumnNames())) {
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
        try {
            // Insert missing rectangles
            long statisticalRectanglesCount = referentialService.countByLevelId(Location.class.getSimpleName(), LocationLevelEnum.RECTANGLE_ICES.getId())
                + referentialService.countByLevelId(Location.class.getSimpleName(), LocationLevelEnum.RECTANGLE_GFCM.getId());
            if (statisticalRectanglesCount == 0) {
                locationService.insertOrUpdateRectangleLocations();
            }

            // Insert missing squares
            long square10minCount = referentialService.countByLevelId(Location.class.getSimpleName(), LocationLevelEnum.SQUARE_10.getId());
            if (square10minCount == 0) {
                // We don't really need to store square 10x10, because extractions and map can compute it dynamically
                // locationService.insertOrUpdateSquares10();
            }

            if (statisticalRectanglesCount == 0 || square10minCount == 0) {
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

        // Force lower case label (better in UI)
        target.setLabel(source.getLabel().toLowerCase());

        // Recorder department
        target.setRecorderDepartment(source.getRecorderDepartment());
    }

    protected void toProductVO(ExtractionContextVO source, ExtractionProductVO target) {

        target.setLabel(ExtractionProducts.getProductLabel(source, source.getId()));
        target.setName(ExtractionProducts.getProductDisplayName(source, source.getId()));
        target.setFormat(source.getRawFormatLabel());
        target.setVersion(source.getVersion());

        target.setTables(SetUtils.emptyIfNull(source.getTableNames())
            .stream()
            .map(t -> {
                String sheetName = source.getSheetName(t);
                ExtractionTableVO table = new ExtractionTableVO();
                table.setLabel(sheetName);
                table.setName(ExtractionProducts.getSheetDisplayName(source, sheetName));
                table.setTableName(t);
                return table;
            })
            .collect(Collectors.toList()));
    }

    protected void clean(ExtractionContextVO context, boolean async) {
        if (context == null) return;
        if (async && taskExecutor != null) {
            taskExecutor.execute(() -> self().clean(context));
        }
        else  {
            log.info("Cleaning extraction #{}-{}", context.getRawFormatLabel(), context.getId());
            getDao(context.getFormat()).clean(context);
        }
    }

    /**
     * Get self bean, to be able to use new transaction
     * @return
     */
    protected ExtractionService self() {
        return applicationContext.getBean("extractionService", ExtractionService.class);
    }

    protected <C extends ExtractionContextVO, F extends ExtractionFilterVO> ExtractionDao<C, F> getDao(IExtractionFormat format) {
        ExtractionDao<?, ?> dao = daosByFormat.get(format);
        if (dao == null) throw new SumarisTechnicalException("Unknown extraction format (no targeted dao): " + format);
        return (ExtractionDao<C, F>)dao;
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

    protected String writeFilterAsString(ExtractionFilterVO filter) {
        if (filter == null) return null;
        try {
            return objectMapper.writeValueAsString(filter);
        }
        catch(JsonProcessingException e) {
            throw new SumarisTechnicalException(e);
        }
    }

    protected void dropTables(Collection<String> tableNames) {
        Beans.getStream(tableNames).forEach(extractionTableDao::dropTable);
    }


    private List<ExtractionTypeVO> findAll(@Nullable ExtractionTypeFilterVO filter,
                                             int offset,
                                             int size,
                                             String sortAttribute,
                                             SortDirection sortDirection) {
        List<ExtractionTypeVO> types = Lists.newArrayList();
        filter = ExtractionTypeFilterVO.nullToEmpty(filter);

        // Exclude types with a DISABLE status, by default
        if (ArrayUtils.isEmpty(filter.getStatusIds())) {
            filter.setStatusIds(new Integer[]{StatusEnum.ENABLE.getId(), StatusEnum.TEMPORARY.getId()});
        }

        boolean includeLiveTypes = ArrayUtils.contains(filter.getStatusIds(), StatusEnum.TEMPORARY.getId()) &&
            filter.getRecorderPersonId() == null;
        ExtractionCategoryEnum filterCategory = ExtractionCategoryEnum.fromString(filter.getCategory()).orElse(null);

        // Add live extraction types (= private by default)
        if (includeLiveTypes && (filterCategory == null || filterCategory == ExtractionCategoryEnum.LIVE)) {
            types.addAll(getLiveExtractionTypes());
        }

        // Add product types
        if (enableProduct && (filterCategory == null || filterCategory == ExtractionCategoryEnum.PRODUCT)) {
            types.addAll(getProductExtractionTypes(filter));
        }

        return types.stream()
            .filter(getTypePredicate(filter))
            .sorted(Beans.naturalComparator(sortAttribute, sortDirection))
            .skip(offset)
            .limit((size < 0) ? types.size() : size)
            .collect(Collectors.toList()
            );
    }

    private Predicate<ExtractionTypeVO> getTypePredicate(@NonNull ExtractionTypeFilterVO filter) {

        Pattern searchPattern = net.sumaris.core.dao.technical.Daos.searchTextIgnoreCasePattern(filter.getSearchText(), false);
        Pattern searchAnyPattern = net.sumaris.core.dao.technical.Daos.searchTextIgnoreCasePattern(filter.getSearchText(), true);

        return s -> (filter.getId() == null || filter.getId().equals(s.getId()))
            && (filter.getLabel() == null || filter.getLabel().equalsIgnoreCase(s.getLabel()))
            && (filter.getName() == null || filter.getName().equalsIgnoreCase(s.getName()))
            && (filter.getCategory() == null || filter.getCategory().equalsIgnoreCase(s.getCategory().name()))
            && (filter.getStatusIds() == null || Arrays.asList(filter.getStatusIds()).contains(s.getStatusId()))
            && (searchPattern == null || searchPattern.matcher(s.getLabel()).matches() || searchAnyPattern.matcher(s.getName()).matches());
    }
}

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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import lombok.NonNull;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.dao.schema.DatabaseSchemaDao;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.dao.technical.extraction.ExtractionProductRepository;
import net.sumaris.core.dao.technical.model.IEntity;
import net.sumaris.core.dao.technical.schema.SumarisDatabaseMetadata;
import net.sumaris.core.dao.technical.schema.SumarisTableMetadata;
import net.sumaris.core.event.config.ConfigurationEvent;
import net.sumaris.core.event.config.ConfigurationReadyEvent;
import net.sumaris.core.event.config.ConfigurationUpdatedEvent;
import net.sumaris.core.exception.DataNotFoundException;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.extraction.dao.administration.ExtractionStrategyDao;
import net.sumaris.core.extraction.dao.technical.Daos;
import net.sumaris.core.extraction.dao.technical.csv.ExtractionCsvDao;
import net.sumaris.core.extraction.dao.technical.schema.SumarisTableMetadatas;
import net.sumaris.core.extraction.dao.technical.table.ExtractionTableColumnOrder;
import net.sumaris.core.extraction.dao.technical.table.ExtractionTableDao;
import net.sumaris.core.extraction.dao.trip.cost.ExtractionCostTripDao;
import net.sumaris.core.extraction.dao.trip.free.ExtractionFree1TripDao;
import net.sumaris.core.extraction.dao.trip.free2.ExtractionFree2TripDao;
import net.sumaris.core.extraction.dao.trip.rdb.ExtractionRdbTripDao;
import net.sumaris.core.extraction.dao.trip.survivalTest.ExtractionSurvivalTestDao;
import net.sumaris.core.extraction.format.LiveFormatEnum;
import net.sumaris.core.extraction.specification.data.trip.RdbSpecification;
import net.sumaris.core.extraction.specification.administration.StratSpecification;
import net.sumaris.core.extraction.util.ExtractionFormats;
import net.sumaris.core.extraction.util.ExtractionProducts;
import net.sumaris.core.extraction.vo.*;
import net.sumaris.core.extraction.vo.administration.ExtractionStrategyContextVO;
import net.sumaris.core.extraction.vo.administration.ExtractionStrategyFilterVO;
import net.sumaris.core.extraction.vo.filter.ExtractionTypeFilterVO;
import net.sumaris.core.extraction.vo.trip.ExtractionTripFilterVO;
import net.sumaris.core.extraction.vo.trip.rdb.ExtractionRdbTripContextVO;
import net.sumaris.core.model.referential.StatusEnum;
import net.sumaris.core.model.referential.location.Location;
import net.sumaris.core.model.referential.location.LocationLevelEnum;
import net.sumaris.core.model.technical.extraction.ExtractionCategoryEnum;
import net.sumaris.core.model.technical.extraction.IExtractionFormat;
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
import org.apache.commons.lang3.mutable.MutableInt;
import org.nuiton.i18n.I18n;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.Resource;
import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * @author peck7 on 17/12/2018.
 */
@Service("extractionService")
@Slf4j
public class ExtractionServiceImpl implements ExtractionService {

    @Autowired
    protected SumarisConfiguration configuration;

    @Autowired
    protected DataSource dataSource;

    @Resource(name = "extractionRdbTripDao")
    protected ExtractionRdbTripDao extractionRdbTripDao;

    @Autowired
    protected ExtractionCostTripDao extractionCostTripDao;

    @Autowired
    protected ExtractionFree1TripDao extractionFreeV1TripDao;

    @Autowired
    protected ExtractionFree2TripDao extractionFree2TripDao;

    @Autowired
    protected ExtractionSurvivalTestDao extractionSurvivalTestDao;

    @Autowired
    protected ExtractionStrategyDao extractionStrategyDao;

    @Autowired
    protected ExtractionProductRepository extractionProductRepository;

    @Autowired
    protected ExtractionTableDao extractionTableDao;

    @Autowired
    protected ExtractionCsvDao extractionCsvDao;

    @Autowired
    protected LocationService locationService;

    @Autowired
    protected ReferentialService referentialService;

    @Autowired
    protected SumarisDatabaseMetadata databaseMetadata;

    @Autowired(required = false)
    protected TaskExecutor taskExecutor = null;

    @Autowired
    protected ApplicationContext applicationContext;

    @Autowired
    protected DatabaseSchemaDao databaseSchemaDao;

    private boolean includeProductTypes;

    @EventListener({ConfigurationReadyEvent.class, ConfigurationUpdatedEvent.class})
    protected void onConfigurationReady(ConfigurationEvent event) {
        includeProductTypes = configuration.enableExtractionProduct();
        if (configuration.enableTechnicalTablesUpdate()) {
            initRectangleLocations();
        }
    }

    @Override
    public ExtractionTypeVO getByFormat(@Nonnull IExtractionFormat format) {
        return ExtractionFormats.findOneMatch(this.getAllTypes(), format);
    }

    @Override
    public List<ExtractionTypeVO> getLiveExtractionTypes() {
        MutableInt id = new MutableInt(-1);
        return Arrays.stream(LiveFormatEnum.values())
                .map(format -> {
                    ExtractionTypeVO type = new ExtractionTypeVO();
                    type.setId(id.getValue());
                    type.setLabel(format.getLabel().toLowerCase());
                    type.setCategory(ExtractionCategoryEnum.LIVE);
                    type.setSheetNames(format.getSheetNames());
                    type.setStatusId(StatusEnum.TEMPORARY.getId()); // = not public by default
                    type.setVersion(format.getVersion());
                    type.setLiveFormat(format);
                    id.decrement();
                    return type;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<ExtractionTypeVO> findByFilter(ExtractionTypeFilterVO filter) {
        ImmutableList.Builder<ExtractionTypeVO> builder = ImmutableList.builder();
        filter = filter != null ? filter : new ExtractionTypeFilterVO();

        // Exclude types with a DISABLE status, by default
        if (ArrayUtils.isEmpty(filter.getStatusIds())) {
            filter.setStatusIds(new Integer[]{StatusEnum.ENABLE.getId(), StatusEnum.TEMPORARY.getId()});
        }

        boolean includeLiveTypes = ArrayUtils.contains(filter.getStatusIds(), StatusEnum.TEMPORARY.getId()) &&
                filter.getRecorderPersonId() == null;
        ExtractionCategoryEnum filterCategory = ExtractionCategoryEnum.fromString(filter.getCategory()).orElse(null);

        // Add live extraction types (= private by default)
        if (includeLiveTypes && (filterCategory == null || filterCategory == ExtractionCategoryEnum.LIVE)) {
            builder.addAll(getLiveExtractionTypes());
        }

        // Add product types
        if (includeProductTypes && (filterCategory == null || filterCategory == ExtractionCategoryEnum.PRODUCT)) {
            builder.addAll(getProductExtractionTypes(filter));
        }

        return builder.build();
    }

    @Override
    public ExtractionResultVO executeAndRead(ExtractionTypeVO type, ExtractionFilterVO filter, int offset, int size, String sort, SortDirection direction) {
        // Make sure type has category AND label filled
        type = getByFormat(type);

        filter = filter != null ? filter : new ExtractionFilterVO();

        // Force preview
        filter.setPreview(true);

        switch (type.getCategory()) {
            case PRODUCT:
                ExtractionProductVO product = extractionProductRepository.getByLabel(type.getLabel(),
                        ExtractionProductFetchOptions.TABLES_AND_COLUMNS);
                Set<String> hiddenColumns = Beans.getStream(product.getTables())
                        .map(ExtractionTableVO::getColumns)
                        .filter(Objects::nonNull)
                        .flatMap(List::stream)
                        .filter(c -> "hidden".equalsIgnoreCase(c.getType()))
                        .map(ExtractionTableColumnVO::getColumnName)
                        .collect(Collectors.toSet());
                filter.setExcludeColumnNames(hiddenColumns);
                return readProductRows(product, filter, offset, size, sort, direction);
            case LIVE:
                return extractLiveAndRead(type, filter, offset, size, sort, direction);
            default:
                throw new SumarisTechnicalException(String.format("Extraction of category %s not implemented yet !", type.getCategory()));
        }
    }

    @Override
    public ExtractionResultVO read(@NonNull ExtractionContextVO context,
                                   ExtractionFilterVO filter,
                                   int offset, int size, String sort, SortDirection direction) {

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
        return extractionTableDao.getRows(tableName, rowsFilter, offset, size, sort, direction);

    }

    @Override
    public File executeAndDump(ExtractionTypeVO type, ExtractionFilterVO filter) throws IOException {
        // Make sure type has category AND label filled
        type = getByFormat(type);

        filter = filter != null ? filter : new ExtractionFilterVO();

        // Force full extraction (not a preview)
        filter.setPreview(false);

        switch (type.getCategory()) {
            case PRODUCT:
                ExtractionProductVO product = extractionProductRepository.getByLabel(type.getLabel(),
                        ExtractionProductFetchOptions.builder()
                                .withRecorderDepartment(false)
                                .withRecorderPerson(false)
                                .withColumns(false)
                                .build());
                return dumpProductToFile(product, filter);
            case LIVE:
                LiveFormatEnum format = LiveFormatEnum.valueOf(type.getLabel().toUpperCase());
                return extractRawDataAndDumpToFile(format, filter);
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
                return extractRawData(type.getLiveFormat(), filter);
            default:
                throw new SumarisTechnicalException(String.format("Extraction of category %s not implemented yet !", type.getCategory()));
        }
    }

    @Override
    public File executeAndDumpTrips(LiveFormatEnum format, ExtractionTripFilterVO tripFilter) {
        String tripSheetName = ArrayUtils.isNotEmpty(format.getSheetNames()) ? format.getSheetNames()[0] : RdbSpecification.TR_SHEET_NAME;
        ExtractionFilterVO filter = extractionRdbTripDao.toExtractionFilterVO(tripFilter, tripSheetName);
        return extractRawDataAndDumpToFile(format, filter);
    }

    @Override
    public File executeAndDumpStrategies(LiveFormatEnum format, ExtractionStrategyFilterVO strategyFilter) {
        String strategySheetName = ArrayUtils.isNotEmpty(format.getSheetNames()) ? format.getSheetNames()[0] : StratSpecification.ST_SHEET_NAME;
        ExtractionFilterVO filter = extractionStrategyDao.toExtractionFilterVO(strategyFilter, strategySheetName);
        return extractRawDataAndDumpToFile(format, filter);
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
    public ExtractionTypeVO save(ExtractionTypeVO type, ExtractionFilterVO filter) {
        Preconditions.checkNotNull(type);

        // Load the product
        ExtractionProductVO target = extractionProductRepository.findByLabel(
            type.getLabel(), ExtractionProductFetchOptions.builder().withTables(false).build()
        ).orElse(null);

        if (target == null) {
            target = new ExtractionProductVO();
            target.setLabel(type.getLabel());
            target.setRecorderDepartment(type.getRecorderDepartment());
        }

        // Execute the aggregation
        ExtractionContextVO context;
        {
            ExtractionTypeVO cleanType = new ExtractionTypeVO();
            cleanType.setLabel(type.getRawFormatLabel());
            cleanType.setCategory(type.getCategory());
            context = execute(cleanType, filter);
        }
        toProductVO(context, target);

        // Set the status
        target.setStatusId(type.getStatusId());

        // Save the product
        target = extractionProductRepository.save(target);

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

        final ExtractionFilterVO tableFilter = filter != null ? filter : new ExtractionFilterVO();
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

        // Remove created tables
        clean(context, true);

        return outputFile;
    }

    @Override
    public CompletableFuture<Boolean> asyncClean(ExtractionContextVO context) {
        try {
            clean(context);
            return CompletableFuture.completedFuture(Boolean.TRUE);
        } catch (Exception e) {
            log.warn(String.format("Error while cleaning extraction #%s: %s", context.getId(), e.getMessage()), e);
            return CompletableFuture.completedFuture(Boolean.FALSE);
        }
    }

    /* -- protected -- */

    protected List<ExtractionTypeVO> getAllTypes() {
        return findByFilter(null);
    }

    /**
     * @deprecated
     * @param filter
     * @return
     */
    @Deprecated
    protected List<ExtractionTypeVO> getProductExtractionTypes(ExtractionTypeFilterVO filter) {
        Preconditions.checkNotNull(filter);

        return ListUtils.emptyIfNull(
            extractionProductRepository.findAll(filter, ExtractionProductFetchOptions.builder()
                .withRecorderDepartment(true)
                .withTables(true)
                .build()))
            .stream()
            .map(this::toExtractionTypeVO)
            .collect(Collectors.toList());
    }



    protected ExtractionResultVO extractLiveAndRead(ExtractionTypeVO type,
                                                    ExtractionFilterVO filter,
                                                    int offset, int size, String sort, SortDirection direction) {
        Preconditions.checkNotNull(type);
        Preconditions.checkNotNull(type.getLiveFormat());

        filter.setPreview(true);

        // Replace default sort attribute
        if (IEntity.Fields.ID.equalsIgnoreCase(sort)) {
            sort = null;
        }

        // Execute extraction into temp tables
        ExtractionContextVO context;
        try {
            context = extractRawData(type.getLiveFormat(), filter);
        } catch (DataNotFoundException e) {
            return createEmptyResult();
        }

        try {
            // Read
            return read(context, filter, offset, size, sort, direction);
        } finally {
            // Clean created tables
            clean(context, true);
        }
    }

    protected File extractRawDataAndDumpToFile(LiveFormatEnum format,
                                               ExtractionFilterVO filter) {
        Preconditions.checkNotNull(format);

        // Execute live extraction to temp tables
        ExtractionContextVO context = extractRawData(format, filter);

        commitIfHsqldb();
        log.info(String.format("Dumping tables of extraction #%s to files...", context.getId()));

        // Dump tables
        return dumpTablesToFile(context, null /*no filter, because already applied*/);
    }

    protected ExtractionResultVO readProductRows(ExtractionProductVO product, ExtractionFilterVO filter, int offset, int size, String sort, SortDirection direction) {
        Preconditions.checkNotNull(product);
        Preconditions.checkNotNull(filter);
        Preconditions.checkArgument(offset >= 0);
        Preconditions.checkArgument(size <= 1000, "maximum value for 'size' is: 1000");
        Preconditions.checkArgument(size >= 0, "'size' must be greater or equals to 0");

        // Get table name
        String tableName = ExtractionFormats.getTableName(product, filter.getSheetName());

        // Get table rows
        return extractionTableDao.getRows(tableName, filter, offset, size, sort, direction);
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

    protected ExtractionContextVO extractRawData(LiveFormatEnum format,
                                                 ExtractionFilterVO filter) {

        ExtractionContextVO context;

        switch (format) {
            case RDB:
                context = extractionRdbTripDao.execute(filter);
                break;
            case COST:
                context = extractionCostTripDao.execute(filter);
                break;
            case FREE1:
                context = extractionFreeV1TripDao.execute(filter);
                break;
            case FREE2:
                context = extractionFree2TripDao.execute(filter);
                break;
            case SURVIVAL_TEST:
                context = extractionSurvivalTestDao.execute(filter);
                break;
            case STRAT:
                context = extractionStrategyDao.execute(filter);
                break;
            default:
                throw new SumarisTechnicalException("Unknown extraction type: " + format);
        }

        return context;
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
                //locationService.insertOrUpdateSquares10();
            }

            if (statisticalRectanglesCount == 0 || square10minCount == 0) {
                // Update area
                // FIXME: no stored procedure fillLocationHierarchy on HSQLDB
                //locationService.insertOrUpdateRectangleAndSquareAreas();

                // Update location hierarchy
                //locationService.updateLocationHierarchy();
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
                columnName -> columnName.toUpperCase(),
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

    protected void commitIfHsqldb() {
        Connection conn = DataSourceUtils.getConnection(dataSource);
        try {
            if (Daos.isHsqlDatabase(conn) && DataSourceUtils.isConnectionTransactional(conn, dataSource)) {
                try {
                    conn.commit();
                } catch (SQLException e) {
                    log.warn("Cannot execute intermediate commit: " + e.getMessage(), e);
                }
            }
        } finally {
            DataSourceUtils.releaseConnection(conn, dataSource);
        }
    }

    protected void clean(ExtractionContextVO context, boolean async) {
        if (context == null) return;
        if (async && taskExecutor != null) {
            taskExecutor.execute(() -> getSelfBean().clean(context));
        }
        else if (context instanceof ExtractionRdbTripContextVO) {
            log.info("Cleaning extraction #{}-{}", context.getLabel(), context.getId());
            extractionRdbTripDao.clean((ExtractionRdbTripContextVO) context);
        }
        else if (context instanceof ExtractionStrategyContextVO) {
            log.info("Cleaning extraction #{}-{}", context.getLabel(), context.getId());
            extractionStrategyDao.clean((ExtractionStrategyContextVO) context);
        }
    }

    /**
     * Get self bean, to be able to use new transaction
     * @return
     */
    protected ExtractionService getSelfBean() {
        return applicationContext.getBean("extractionService", ExtractionService.class);
    }
}
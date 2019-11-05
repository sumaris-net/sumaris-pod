package net.sumaris.core.extraction.service;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.dao.technical.extraction.ExtractionProductDao;
import net.sumaris.core.dao.technical.model.IEntity;
import net.sumaris.core.dao.technical.schema.SumarisDatabaseMetadata;
import net.sumaris.core.dao.technical.schema.SumarisTableMetadata;
import net.sumaris.core.exception.DataNotFoundException;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.extraction.dao.technical.csv.ExtractionCsvDao;
import net.sumaris.core.extraction.dao.technical.schema.SumarisTableMetadatas;
import net.sumaris.core.extraction.dao.technical.table.ExtractionTableColumnOrder;
import net.sumaris.core.extraction.dao.technical.table.ExtractionTableDao;
import net.sumaris.core.extraction.dao.trip.cost.ExtractionCostTripDao;
import net.sumaris.core.extraction.dao.trip.free.ExtractionFreeTripDao;
import net.sumaris.core.extraction.dao.trip.rdb.ExtractionRdbTripDao;
import net.sumaris.core.extraction.dao.trip.survivalTest.ExtractionSurvivalTestDao;
import net.sumaris.core.extraction.utils.ExtractionBeans;
import net.sumaris.core.extraction.vo.*;
import net.sumaris.core.extraction.vo.filter.ExtractionTypeFilterVO;
import net.sumaris.core.extraction.vo.trip.ExtractionTripFilterVO;
import net.sumaris.core.model.referential.StatusEnum;
import net.sumaris.core.model.referential.location.Location;
import net.sumaris.core.model.referential.location.LocationLevelEnum;
import net.sumaris.core.service.referential.LocationService;
import net.sumaris.core.service.referential.ReferentialService;
import net.sumaris.core.util.*;
import net.sumaris.core.vo.technical.extraction.ExtractionProductTableVO;
import net.sumaris.core.vo.technical.extraction.ExtractionProductVO;
import net.sumaris.core.vo.technical.extraction.ProductFetchOptions;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.mutable.MutableInt;
import org.nuiton.i18n.I18n;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.task.TaskExecutor;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author peck7 on 17/12/2018.
 */
@Service("extractionService")
@Lazy
public class ExtractionServiceImpl implements ExtractionService {

    private static final Logger log = LoggerFactory.getLogger(ExtractionServiceImpl.class);

    @Autowired
    SumarisConfiguration configuration;

    @Resource(name = "extractionRdbTripDao")
    ExtractionRdbTripDao extractionRdbTripDao;

    @Resource(name = "extractionCostTripDao")
    ExtractionCostTripDao extractionCostTripDao;

    @Resource(name = "extractionFreeTripDao")
    ExtractionFreeTripDao extractionFreeTripDao;

    @Resource(name = "extractionSurvivalTestDao")
    ExtractionSurvivalTestDao extractionSurvivalTestDao;

    @Autowired
    ExtractionProductDao extractionProductDao;

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
    private ExtractionService self;

    @PostConstruct
    protected void afterPropertiesSet() {


        // Make sure statistical rectangle exists (need by trip extraction)
        initRectangleLocations();
    }

    @Override
    public List<ExtractionTypeVO> findByFilter(ExtractionTypeFilterVO filter) {
        ImmutableList.Builder<ExtractionTypeVO> builder = ImmutableList.builder();
        filter = filter != null ? filter : new ExtractionTypeFilterVO();

        // Add live extraction types
        if (filter.getCategory() == null || filter.getCategory().equalsIgnoreCase(ExtractionCategoryEnum.LIVE.name())) {
            builder.addAll(getLiveExtractionTypes());
        }

        // Add products
        if (filter.getCategory() == null || filter.getCategory().equalsIgnoreCase(ExtractionCategoryEnum.PRODUCT.name())) {
            builder.addAll(getProductExtractionTypes(filter));
        }

        return builder.build();
    }

    @Override
    public ExtractionResultVO executeAndRead(ExtractionTypeVO type, ExtractionFilterVO filter, int offset, int size, String sort, SortDirection direction) {
        // Make sure type has category AND label filled
        ExtractionTypeVO checkedType = ExtractionBeans.checkAndFindType(this.getAllExtractionTypes(), type);
        ExtractionCategoryEnum category = ExtractionCategoryEnum.valueOf(checkedType.getCategory().toUpperCase());

        // Force preview
        filter.setPreview(true);

        filter = filter != null ? filter : new ExtractionFilterVO();

        switch (category) {
            case PRODUCT:
                ExtractionProductVO product = extractionProductDao.getByLabel(checkedType.getLabel(),
                        ProductFetchOptions.MINIMAL_WITH_TABLES);
                return readProductRows(product, filter, offset, size, sort, direction);
            case LIVE:
                String formatName = checkedType.getLabel();
                return extractRawDataAndRead(formatName, filter, offset, size, sort, direction);
            default:
                throw new SumarisTechnicalException(String.format("Extraction of category %s not implemented yet !", type.getCategory()));
        }
    }

    @Override
    public ExtractionResultVO read(ExtractionContextVO context, ExtractionFilterVO filter,
                                   int offset, int size, String sort, SortDirection direction) {
        Preconditions.checkNotNull(context);

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
        return extractionTableDao.getTableRows(tableName, rowsFilter, offset, size, sort, direction);

    }

    @Override
    public File executeAndDump(ExtractionTypeVO type, ExtractionFilterVO filter) throws IOException {
        // Make sure type has category AND label filled
        ExtractionTypeVO checkedType = ExtractionBeans.checkAndFindType(getAllExtractionTypes(), type);
        ExtractionCategoryEnum category = ExtractionCategoryEnum.valueOf(checkedType.getCategory().toUpperCase());

        filter = filter != null ? filter : new ExtractionFilterVO();

        // Force full extraction (not a preview)
        filter.setPreview(false);

        switch (category) {
            case PRODUCT:
                ExtractionProductVO product = extractionProductDao.getByLabel(checkedType.getLabel(),
                        ProductFetchOptions.builder()
                                .withRecorderDepartment(false)
                                .withRecorderPerson(false)
                                .withColumns(false)
                                .build());
                return dumpProductToFile(product, filter);
            case LIVE:
                ExtractionRawFormatEnum format = ExtractionRawFormatEnum.valueOf(checkedType.getLabel().toUpperCase());
                return extractRawDataAndDumpToFile(format, filter);
            default:
                throw new SumarisTechnicalException(String.format("Extraction of category %s not implemented yet !", type.getCategory()));
        }

    }

    @Override
    public ExtractionContextVO execute(ExtractionTypeVO type, ExtractionFilterVO filter) {
        // Make sure type has category AND label filled
        ExtractionTypeVO checkedType = ExtractionBeans.checkAndFindType(getAllExtractionTypes(), type);
        ExtractionCategoryEnum category = ExtractionCategoryEnum.valueOf(checkedType.getCategory().toUpperCase());

        filter = filter != null ? filter : new ExtractionFilterVO();

        // Force full extraction (not a preview)
        filter.setPreview(false);

        switch (category) {
            case PRODUCT:
                throw new IllegalArgumentException("execute not implemented yet for product");
                //    ExtractionProduct product = ExtractionProduct.valueOf(checkedType.getLabel().toUpperCase());
                //    return extractProductToTables(product, filter);
            case LIVE:
                ExtractionRawFormatEnum format = ExtractionRawFormatEnum.valueOf(checkedType.getLabel().toUpperCase());
                return extractRawData(format, filter);
            default:
                throw new SumarisTechnicalException(String.format("Extraction of category %s not implemented yet !", type.getCategory()));
        }

    }

    @Override
    public File executeAndDumpTrips(ExtractionRawFormatEnum format, ExtractionTripFilterVO tripFilter) {

        ExtractionFilterVO filter = extractionRdbTripDao.toExtractionFilterVO(tripFilter);
        return extractRawDataAndDumpToFile(format, filter);
    }

    @Override
    public void clean(ExtractionContextVO context) {
        Preconditions.checkNotNull(context);

        if (CollectionUtils.isEmpty(context.getTableNames())) return;

        context.getTableNames().stream()
                // Keep only tables with EXT_ prefix
                .filter(tableName -> tableName != null && tableName.startsWith("EXT_"))
                .forEach(extractionTableDao::dropTable);
    }

    @Override
    public ExtractionProductVO toProductVO(ExtractionContextVO source) {
        if (source == null) return null;
        Preconditions.checkNotNull(source.getLabel());

        ExtractionProductVO target = new ExtractionProductVO();

        String format = source.getFormatName();
        if (StringUtils.isNotBlank(format)) {
            target.setLabel(StringUtils.changeCaseToUnderscore(format).toUpperCase());
        } else {
            target.setLabel(source.getLabel());
        }
        target.setName(String.format("Extraction #%s", source.getId()));

        target.setTables(SetUtils.emptyIfNull(source.getTableNames())
                .stream()
                .map(t -> {
                    ExtractionProductTableVO table = new ExtractionProductTableVO();
                    table.setLabel(source.getSheetName(t));
                    table.setName(t);
                    table.setTableName(t);
                    return table;
                })
                .collect(Collectors.toList()));

        return target;
    }

    @Override
    public ExtractionTypeVO save(ExtractionTypeVO type, ExtractionFilterVO filter) {
        Preconditions.checkNotNull(type);

        // Load the product
        ExtractionProductVO target = null;
        try {
            target = extractionProductDao.getByLabel(type.getLabel(), ProductFetchOptions.builder()
                    .withTables(false)
                    .build());
        } catch (Throwable t) {
            // Not found
        }

        if (target == null) {
            target = new ExtractionProductVO();
            target.setLabel(type.getLabel());
        }

        // Execute the aggregation
        ExtractionContextVO context;
        {
            ExtractionTypeVO cleanType = new ExtractionTypeVO();
            cleanType.setLabel(type.getFormat());
            cleanType.setCategory(type.getCategory());
            context = execute(cleanType, filter);
        }
        toProductVO(context, target);

        // Set the status
        target.setStatusId(type.getStatusId());

        // Save the product
        target = extractionProductDao.save(target);

        // Transform back to type
        return toExtractionTypeVO(target);
    }

    /* -- protected -- */

    protected List<ExtractionTypeVO> getAllExtractionTypes() {
        return findByFilter(new ExtractionTypeFilterVO());
    }

    protected List<ExtractionTypeVO> getProductExtractionTypes(ExtractionTypeFilterVO filter) {
        Preconditions.checkNotNull(filter);

        // Exclude types with a DISABLE status, by default
        if (ArrayUtils.isEmpty(filter.getStatusIds())) {
            filter.setStatusIds(new Integer[]{StatusEnum.ENABLE.getId(), StatusEnum.TEMPORARY.getId()});
        }

        return ListUtils.emptyIfNull(
                extractionProductDao.findByFilter(filter, ProductFetchOptions.builder()
                        .withRecorderDepartment(true)
                        .withTables(true)
                        .build()))
                .stream()
                .map(this::toExtractionTypeVO)
                .collect(Collectors.toList());
    }

    protected List<ExtractionTypeVO> getLiveExtractionTypes() {
        MutableInt id = new MutableInt(-1);
        return Arrays.stream(ExtractionRawFormatEnum.values())
                .map(format -> {
                    ExtractionTypeVO type = new ExtractionTypeVO();
                    type.setId(id.getValue());
                    type.setLabel(format.name().toLowerCase());
                    type.setCategory(ExtractionCategoryEnum.LIVE.name().toLowerCase());
                    type.setSheetNames(format.getSheetNames());
                    type.setStatusId(StatusEnum.TEMPORARY.getId()); // = not public
                    id.decrement();
                    return type;
                })
                .collect(Collectors.toList());
    }


    protected ExtractionResultVO extractRawDataAndRead(String formatStr, ExtractionFilterVO filter,
                                                       int offset, int size, String sort, SortDirection direction) {
        Preconditions.checkNotNull(formatStr);

        ExtractionRawFormatEnum format = ExtractionRawFormatEnum.valueOf(formatStr.toUpperCase());

        filter.setPreview(true);

        // Replace default sort attribute
        if (IEntity.PROPERTY_ID.equalsIgnoreCase(sort)) {
            sort = null;
        }

        // Execute extraction into temp tables
        ExtractionContextVO context;
        try {
            context = extractRawData(format, filter);
        } catch (DataNotFoundException e) {
            return createEmptyResult();
        }

        try {
            // Read
            return read(context, filter, offset, size, sort, direction);
        } finally {
            // Clean created tables
            asyncClean(context);
        }
    }

    protected File extractRawDataAndDumpToFile(ExtractionRawFormatEnum format, ExtractionFilterVO filter) {
        Preconditions.checkNotNull(format);

        // Execute live extraction to temp tables
        ExtractionContextVO context = extractRawData(format, filter);

        log.info(String.format("Dumping tables of extraction #%s to files...", context.getId()));

        int retryCounter = 0;
        while (true) {
            try {
                // Dump tables
                return dumpTablesToFile(context, null /*no filter, because already applied*/);
            }

            // Workaround for issue #142: sleep 1s to wait end of table creation
            catch (DataAccessResourceFailureException e) {
                retryCounter++;
                if (retryCounter >= 3) throw e;
                log.warn(String.format("Error while dumping tables of extraction #%s to files (issue #142) - Retrying in 1s (%s/3)... \n\tError:%s",
                        context.getId(), retryCounter, e.getMessage()));
            }

            // Sleeping 1s before retrying
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ie) {
                throw new SumarisTechnicalException(ie);
            }
        }
    }

    protected ExtractionResultVO readProductRows(ExtractionProductVO product, ExtractionFilterVO filter, int offset, int size, String sort, SortDirection direction) {
        Preconditions.checkNotNull(product);
        Preconditions.checkNotNull(filter);
        Preconditions.checkArgument(offset >= 0);
        Preconditions.checkArgument(size <= 1000, "maximum value for 'size' is: 1000");
        Preconditions.checkArgument(size >= 0, "'size' must be greater or equals to 0");

        // Get table name
        String tableName = ExtractionBeans.getTableName(product, filter.getSheetName());

        // Get table rows
        return extractionTableDao.getTableRows(tableName, filter, offset, size, sort, direction);
    }

    protected File dumpProductToFile(ExtractionProductVO product, ExtractionFilterVO filter) throws IOException {
        Preconditions.checkNotNull(product);
        Preconditions.checkNotNull(filter);

        // Create a new context
        ExtractionProductContextVO context = new ExtractionProductContextVO(product);
        context.setId(System.currentTimeMillis());

        // Dump to file
        return dumpTablesToFile(context, filter);
    }

    protected ExtractionContextVO extractRawData(ExtractionRawFormatEnum format,
                                                 ExtractionFilterVO filter) {

        ExtractionContextVO context;

        switch (format) {
            case RDB:
                context = extractionRdbTripDao.execute(filter);
                break;
            case COST:
                context = extractionCostTripDao.execute(filter);
                break;
            case FREE:
                context = extractionFreeTripDao.execute(filter);
                break;
            case SURVIVAL_TEST:
                context = extractionSurvivalTestDao.execute(filter);
                break;
            default:
                throw new SumarisTechnicalException("Unknown extraction type: " + format);
        }

        return context;
    }

    protected File dumpTablesToFile(ExtractionContextVO context,
                                    @Nullable ExtractionFilterVO filter) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(context.getLabel());

        if (CollectionUtils.isEmpty(context.getTableNames())) return null;

        // Dump table to CSV files
        log.debug("Creating extraction CSV files...");

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
                    Files.getExtension(uniqueFile));
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
        asyncClean(context);

        return outputFile;
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

    public void asyncClean(ExtractionContextVO context) {
        if (taskExecutor == null) {
            clean(context);
        } else {
            taskExecutor.execute(() -> {
                try {
                    // Call elf, to
                    self.clean(context);
                } catch (Exception e) {
                    log.warn("Error while cleaning extraction tables", e);
                }
            });
        }
    }

    protected void initRectangleLocations() {
        // Insert missing rectangles
        long statisticalRectanglesCount = referentialService.countByLevelId(Location.class.getSimpleName(), LocationLevelEnum.RECTANGLE_ICES.getId())
                + referentialService.countByLevelId(Location.class.getSimpleName(), LocationLevelEnum.RECTANGLE_CGPM_GFCM.getId());
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

        // Force category to product
        target.setCategory(ExtractionCategoryEnum.PRODUCT.name().toLowerCase());

        // Sheetnames, from product tables
        Collection<String> sheetNames = source.getSheetNames();
        if (CollectionUtils.isNotEmpty(sheetNames)) {
            target.setSheetNames(sheetNames.toArray(new String[sheetNames.size()]));
        }

        // Recorder department
        target.setRecorderDepartment(source.getRecorderDepartment());
    }

    protected void toProductVO(ExtractionContextVO source, ExtractionProductVO target) {

        target.setLabel(source.getLabel().toUpperCase() + "-" + source.getId());
        target.setName(String.format("Extraction #%s", source.getId()));

        target.setTables(SetUtils.emptyIfNull(source.getTableNames())
                .stream()
                .map(t -> {
                    String sheetName = source.getSheetName(t);
                    ExtractionProductTableVO table = new ExtractionProductTableVO();
                    table.setLabel(sheetName);
                    table.setName(getNameBySheet(source.getFormatName(), sheetName));
                    table.setTableName(t);
                    return table;
                })
                .collect(Collectors.toList()));
    }

    protected String getNameBySheet(String format, String sheetname) {
        return I18n.t(String.format("sumaris.extraction.%s.%s", format.toUpperCase(), sheetname.toUpperCase()));
    }
}

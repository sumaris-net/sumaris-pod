package net.sumaris.core.extraction.service;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.dao.technical.model.IDataEntity;
import net.sumaris.core.dao.technical.schema.DatabaseTableEnum;
import net.sumaris.core.dao.technical.schema.SumarisDatabaseMetadata;
import net.sumaris.core.dao.technical.schema.SumarisTableMetadata;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.extraction.dao.technical.csv.ExtractionCsvDao;
import net.sumaris.core.extraction.dao.technical.schema.SumarisTableMetadatas;
import net.sumaris.core.extraction.dao.technical.table.ExtractionTableColumnOrder;
import net.sumaris.core.extraction.dao.technical.table.ExtractionTableDao;
import net.sumaris.core.extraction.dao.trip.cost.ExtractionCostTripDao;
import net.sumaris.core.extraction.dao.trip.rdb.ExtractionRdbTripDao;
import net.sumaris.core.extraction.dao.trip.survivalTest.ExtractionSurvivalTestDao;
import net.sumaris.core.extraction.vo.*;
import net.sumaris.core.extraction.vo.live.ExtractionLiveContextVO;
import net.sumaris.core.extraction.vo.live.trip.ExtractionTripFilterVO;
import net.sumaris.core.extraction.vo.live.ExtractionLiveFormat;
import net.sumaris.core.extraction.vo.product.ExtractionProduct;
import net.sumaris.core.extraction.vo.product.ExtractionProductContextVO;
import net.sumaris.core.model.referential.location.Location;
import net.sumaris.core.model.referential.location.LocationLevelEnum;
import net.sumaris.core.service.referential.LocationService;
import net.sumaris.core.service.referential.ReferentialService;
import net.sumaris.core.util.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.task.TaskExecutor;
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

    private static Map<String, String> rdbProductTableBySheet = ImmutableMap.<String, String>builder()
            .put("TR", DatabaseTableEnum.P01_ICES_TRIP.name())
            .put("HH", DatabaseTableEnum.P01_ICES_STATION.name())
            .put("SL", DatabaseTableEnum.P01_ICES_SPECIES_LIST.name())
            .put("HL", DatabaseTableEnum.P01_ICES_SPECIES_LENGTH.name())
            .put("CL", DatabaseTableEnum.P01_ICES_LANDING.name())
            .build();

    @Autowired
    SumarisConfiguration configuration;

    @Resource(name = "extractionRdbTripDao")
    ExtractionRdbTripDao rdbExtractionTripDao;

    @Resource(name = "extractionCostTripDao")
    ExtractionCostTripDao extractionCostTripDao;

    @Resource(name = "extractionSurvivalTestDao")
    ExtractionSurvivalTestDao extractionSurvivalTestDao;

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

    private List<ExtractionTypeVO> extractionTypes;

    @PostConstruct
    protected void afterPropertiesSet() {
        // Init extractions types
        extractionTypes = ImmutableList.<ExtractionTypeVO>builder()
                .addAll(getProductExtractionTypes())
                .addAll(getLiveExtractionTypes())
                .build();

        // Make sure statistical rectangle exists (need by trip extraction)
        initRectangleLocations();
    }

    @Override
    public List<ExtractionTypeVO> getAllTypes() {
        return extractionTypes;
    }

    @Override
    public ExtractionResultVO getRows(ExtractionTypeVO type, ExtractionFilterVO filter, int offset, int size, String sort, SortDirection direction) {
        // Make sure type has category AND label filled
        ExtractionTypeVO checkedType = checkAndGetFullType(type);
        ExtractionCategoryEnum category = ExtractionCategoryEnum.valueOf(checkedType.getCategory().toUpperCase());

        // Force preview
        filter.setPreview(true);

        filter = filter != null ? filter : new ExtractionFilterVO();

        switch (category) {
            case PRODUCT:
                ExtractionProduct product = ExtractionProduct.valueOf(checkedType.getLabel().toUpperCase());
                return extractProductAsRows(product, filter, offset, size, sort, direction);
            case LIVE:
                String formatName = checkedType.getLabel();
                return extractLiveDataAsRows(formatName, filter, offset, size, sort, direction);
            default:
                throw new SumarisTechnicalException(String.format("Extraction of category %s not implemented yet !", type.getCategory()));
        }
    }


    @Override
    public File extractAsFile(ExtractionTypeVO type, ExtractionFilterVO filter) throws IOException {
        // Make sure type has category AND label filled
        ExtractionTypeVO checkedType = checkAndGetFullType(type);
        ExtractionCategoryEnum category = ExtractionCategoryEnum.valueOf(checkedType.getCategory().toUpperCase());

        filter = filter != null ? filter : new ExtractionFilterVO();

        // Force full extraction (not a preview)
        filter.setPreview(false);

        switch (category) {
            case PRODUCT:
                ExtractionProduct product = ExtractionProduct.valueOf(checkedType.getLabel().toUpperCase());
                return extractProductAsFile(product, filter);
            case LIVE:
                ExtractionLiveFormat format = ExtractionLiveFormat.valueOf(checkedType.getLabel().toUpperCase());
                return extractLiveDataAsFile(format, filter);
            default:
                throw new SumarisTechnicalException(String.format("Extraction of category %s not implemented yet !", type.getCategory()));
        }

    }

    @Override
    public ExtractionContextVO extract(ExtractionTypeVO type, ExtractionFilterVO filter) {
        // Make sure type has category AND label filled
        ExtractionTypeVO checkedType = checkAndGetFullType(type);
        ExtractionCategoryEnum category = ExtractionCategoryEnum.valueOf(checkedType.getCategory().toUpperCase());

        filter = filter != null ? filter : new ExtractionFilterVO();

        // Force full extraction (not a preview)
        filter.setPreview(false);

        switch (category) {
            case PRODUCT:
                throw new IllegalArgumentException("extract not implemented yet for product");
                //    ExtractionProduct product = ExtractionProduct.valueOf(checkedType.getLabel().toUpperCase());
                //    return extractProductToTables(product, filter);
            case LIVE:
                ExtractionLiveFormat format = ExtractionLiveFormat.valueOf(checkedType.getLabel().toUpperCase());
                return extractLiveDataAsContext(format, filter);
            default:
                throw new SumarisTechnicalException(String.format("Extraction of category %s not implemented yet !", type.getCategory()));
        }

    }

    @Override
    public File extractTripAsFile(ExtractionLiveFormat format, ExtractionTripFilterVO tripFilter) {

        ExtractionFilterVO filter = rdbExtractionTripDao.toExtractionFilterVO(tripFilter);
        return extractLiveDataAsFile(format, filter);
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

    /* -- protected -- */

    protected List<ExtractionTypeVO> getProductExtractionTypes() {
        return Arrays.stream(ExtractionProduct.values())
                .map(product -> {
                    ExtractionTypeVO type = new ExtractionTypeVO();
                    type.setLabel(product.name().toLowerCase());
                    type.setCategory(ExtractionCategoryEnum.PRODUCT.name().toLowerCase());
                    type.setSheetNames(product.getSheetNames());
                    return type;
                }).collect(Collectors.toList());
    }

    protected List<ExtractionTypeVO> getLiveExtractionTypes() {
        return Arrays.stream(ExtractionLiveFormat.values())
                .map(format -> {
                    ExtractionTypeVO type = new ExtractionTypeVO();
                    type.setLabel(format.name().toLowerCase());
                    type.setCategory(ExtractionCategoryEnum.LIVE.name().toLowerCase());
                    type.setSheetNames(format.getSheetNames());
                    return type;
                })
                .collect(Collectors.toList());
    }


    protected ExtractionResultVO extractLiveDataAsRows(String formatStr, ExtractionFilterVO filter, int offset, int size, String sort, SortDirection direction) {
        Preconditions.checkNotNull(formatStr);

        ExtractionLiveFormat format = ExtractionLiveFormat.valueOf(formatStr.toUpperCase());

        filter.setPreview(true);

        // Replace default sort attribute
        if (IDataEntity.PROPERTY_ID.equalsIgnoreCase(sort)) {
            sort = null;
        }

        // Execute the live export (to temp tables)
        ExtractionLiveContextVO context = extractLiveDataAsContext(format, filter);

        String tableName = context.getTableNames().iterator().next();
        if (StringUtils.isNotBlank(filter.getSheetName())) {
            tableName = context.getTableNameBySheetName(filter.getSheetName());
        }

        // Missing the expected sheet = no data
        if (tableName == null) return createEmptyResult();

        // Create a filter for rows previous, with only includes/exclude columns,
        // because criterion are not need (already applied when writing temp tables)
        ExtractionFilterVO previousFilter = new ExtractionFilterVO();
        previousFilter.setIncludeColumnNames(filter.getIncludeColumnNames()); // Copy given include columns
        previousFilter.setExcludeColumnNames(SetUtils.union(
                SetUtils.emptyIfNull(filter.getIncludeColumnNames()),
                SetUtils.emptyIfNull(context.getHiddenColumns(tableName))
        ));

        // Force distinct if there is excluded columns AND distinct is enable on the XML query
        boolean enableDistinct = filter.isDistinct() || CollectionUtils.isNotEmpty(previousFilter.getExcludeColumnNames())
                && context.isDistinctEnable(tableName);
        previousFilter.setDistinct(enableDistinct);

        // Get rows from exported tables
        ExtractionResultVO result = extractionTableDao.getTableRows(tableName, previousFilter, offset, size, sort, direction);

        // Clean created tables
        asyncClean(context);

        return result;
    }

    protected File extractLiveDataAsFile(ExtractionLiveFormat format, ExtractionFilterVO filter) {
        Preconditions.checkNotNull(format);

        // Execute live extraction to temp tables
        ExtractionLiveContextVO context = extractLiveDataAsContext(format, filter);

        // Dump tables
        return writeTablesToFile(context, null /*no filter, because already applied*/);
    }

    protected ExtractionResultVO extractProductAsRows(ExtractionProduct product, ExtractionFilterVO filter, int offset, int size, String sort, SortDirection direction) {
        Preconditions.checkNotNull(product);
        Preconditions.checkNotNull(filter);
        Preconditions.checkArgument(offset >= 0);
        Preconditions.checkArgument(size <= 1000, "maximum value for 'size' is: 1000");
        Preconditions.checkArgument(size >= 0, "'size' must be greater or equals to 0");

        // Get table name
        String tableName = getProductTableName(product, filter.getSheetName());

        // Get table rows
        return extractionTableDao.getTableRows(tableName, filter, offset, size, sort, direction);
    }

    protected File extractProductAsFile(ExtractionProduct product, ExtractionFilterVO filter) throws IOException {
        Preconditions.checkNotNull(product);
        Preconditions.checkNotNull(filter);

        ExtractionProductContextVO context = new ExtractionProductContextVO();
        context.setId(System.currentTimeMillis());
        context.setProduct(product);

        // No sheet (product with only one table)
        if (ArrayUtils.isEmpty(product.getSheetNames())) {
            String tableName = getProductTableName(product, null);
            context.addTableName(tableName, context.getLabel(), filter.getExcludeColumnNames(), filter.isDistinct());
        }

        // One or more sheet
        else {
            Arrays.stream(product.getSheetNames())
                    .forEach(sheetName -> {
                        String tableName = getProductTableName(product, sheetName);
                        context.addTableName(tableName, sheetName, null, filter.isDistinct());
                    });
        }

        // Dump to file
        return writeTablesToFile(context, filter);
    }

    protected ExtractionLiveContextVO extractLiveDataAsContext(ExtractionLiveFormat format,
                                                               ExtractionFilterVO filter) {

        ExtractionLiveContextVO context;

        switch (format) {
            case RDB:
                context = rdbExtractionTripDao.execute(rdbExtractionTripDao.toTripFilterVO(filter), filter);
                break;
            case COST:
                context = extractionCostTripDao.execute(rdbExtractionTripDao.toTripFilterVO(filter), filter);
                break;
            case SURVIVAL_TEST:
                context = extractionSurvivalTestDao.execute(extractionSurvivalTestDao.toTripFilterVO(filter), filter);
                break;
            default:
                throw new SumarisTechnicalException("Unknown extraction type: " + format);
        }

        return context;
    }

    protected File writeTablesToFile(ExtractionContextVO context,
                                     @Nullable ExtractionFilterVO filter) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(context.getLabel());

        if (CollectionUtils.isEmpty(context.getTableNames())) return null;

        // Dump table to CSV files
        log.debug("Creating extraction CSV files...");

        String dateStr = Dates.formatDate(new Date(context.getId()), "yyyy-MM-dd-HHMM");
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
                        writeTableToFile(tableName, tableFilter, tempCsvFile);
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

    protected void writeTableToFile(String tableName, ExtractionFilterVO filter, File outputFile) throws IOException {
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

    protected void asyncClean(ExtractionContextVO context) {
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

    protected Map<String, String> getAliasByColumnMap(SumarisTableMetadata table) {
        return getAliasByColumnMap(table.getColumnNames());
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

    protected ExtractionTypeVO checkAndGetFullType(ExtractionTypeVO type) throws IllegalArgumentException {
        Preconditions.checkNotNull(type, "Missing argument 'type' ");
        Preconditions.checkNotNull(type.getLabel(), "Missing argument 'type.label'");

        // Retrieve the extraction type, from list
        final String extractionLabel = type.getLabel();
        if (type.getCategory() == null) {
            type = this.extractionTypes.stream()
                    .filter(aType -> aType.getLabel().equalsIgnoreCase(extractionLabel))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(String.format("Unknown extraction type label {%s}", extractionLabel)));
        } else {
            final String extractionCategory = type.getCategory();
            type = this.extractionTypes.stream()
                    .filter(aType -> aType.getLabel().equalsIgnoreCase(extractionLabel) && aType.getCategory().equalsIgnoreCase(extractionCategory))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(String.format("Unknown extraction type category/label {%s/%s}", extractionCategory, extractionLabel)));
        }
        return type;
    }

    protected String getProductTableName(ExtractionProduct product, String sheetName) {
        if (StringUtils.isNotBlank(sheetName)) {
            Preconditions.checkArgument(
                    product.getSheetNames() != null && Arrays.asList(product.getSheetNames()).contains(sheetName.toUpperCase()),
                    String.format("Invalid sheet {%s}: not exists on product {%s}", sheetName, product.name()));
        }

        // Or use default sheetname
        else if (ArrayUtils.isNotEmpty(product.getSheetNames())) {
            sheetName = product.getSheetNames()[0];
        }

        // Get table name
        String tableName = null;
        if (StringUtils.isNotBlank(sheetName)) {
            if (product == ExtractionProduct.P01_RDB) {
                tableName = rdbProductTableBySheet.get(sheetName);
            }
            if (StringUtils.isBlank(tableName)) {
                tableName = "P_" + product.name() + "_" + sheetName.toUpperCase();
            }
        } else {
            tableName = "P_" + product.name();
        }

        return tableName;
    }
}

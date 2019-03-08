package net.sumaris.core.extraction.service;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.dao.technical.model.IEntityBean;
import net.sumaris.core.dao.technical.schema.DatabaseTableEnum;
import net.sumaris.core.dao.technical.schema.SumarisDatabaseMetadata;
import net.sumaris.core.dao.technical.schema.SumarisTableMetadata;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.extraction.dao.technical.csv.ExtractionCsvDao;
import net.sumaris.core.extraction.dao.technical.schema.SumarisTableMetadatas;
import net.sumaris.core.extraction.dao.technical.table.ExtractionTableDao;
import net.sumaris.core.extraction.dao.trip.ices.ExtractionIcesDao;
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
import net.sumaris.core.util.Dates;
import net.sumaris.core.util.Files;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.util.ZipUtils;
import org.apache.commons.collections4.CollectionUtils;
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

    private static Map<String, String> icesProductTableBySheet = ImmutableMap.<String, String>builder()
            .put("TR", DatabaseTableEnum.P01_ICES_TRIP.name())
            .put("HH", DatabaseTableEnum.P01_ICES_STATION.name())
            .put("SL", DatabaseTableEnum.P01_ICES_SPECIES_LIST.name())
            .put("HL", DatabaseTableEnum.P01_ICES_SPECIES_LENGTH.name())
            .put("CL", DatabaseTableEnum.P01_ICES_LANDING.name())
            .build();

    @Autowired
    SumarisConfiguration configuration;

    @Autowired
    ExtractionIcesDao extractionIcesDao;

    @Autowired
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
                return getProductRows(product, filter, offset, size, sort, direction);
            case LIVE:
                String formatName = checkedType.getLabel();
                return getLiveRows(formatName, filter, offset, size, sort, direction)    ;
            default:
                throw new SumarisTechnicalException(String.format("Extraction of category %s not implemented yet !", type.getCategory()));
        }
    }


    @Override
    public File getFile(ExtractionTypeVO type, ExtractionFilterVO filter) throws IOException {
        // Make sure type has category AND label filled
        ExtractionTypeVO checkedType = checkAndGetFullType(type);
        ExtractionCategoryEnum category = ExtractionCategoryEnum.valueOf(checkedType.getCategory().toUpperCase());

        // Force full extraction (not a preview)
        filter.setPreview(false);

        filter = filter != null ? filter : new ExtractionFilterVO();

        switch (category) {
            case PRODUCT:
                ExtractionProduct product = ExtractionProduct.valueOf(checkedType.getLabel().toUpperCase());
                return getProductFile(product, filter);
            case LIVE:
                ExtractionLiveFormat format = ExtractionLiveFormat.valueOf(checkedType.getLabel().toUpperCase());
                return getLiveFile(format, filter);
            default:
                throw new SumarisTechnicalException(String.format("Extraction of category %s not implemented yet !", type.getCategory()));
        }

    }

    @Override
    public File getFile(ExtractionLiveFormat format, ExtractionTripFilterVO tripFilter) {

        ExtractionFilterVO filter = extractionIcesDao.toExtractionFilterVO(tripFilter);
        return getLiveFile(format, filter);
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
        return  Arrays.stream(ExtractionProduct.values())
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


    protected ExtractionResultVO getLiveRows(String formatStr, ExtractionFilterVO filter, int offset, int size, String sort, SortDirection direction) {
        Preconditions.checkNotNull(formatStr);

        ExtractionLiveFormat format = ExtractionLiveFormat.valueOf(formatStr.toUpperCase());

        filter.setPreview(true);

        // Replace default sort attribute
        if (IEntityBean.PROPERTY_ID.equalsIgnoreCase(sort)) {
            sort = null;
        }

        // Execute the live export (to temp tables)
        ExtractionLiveContextVO context = executeLiveExport(format, filter);

        String selectedTableName = context.getTableNames().iterator().next();
        if (StringUtils.isNotBlank(filter.getSheetName())) {
            selectedTableName = context.getTableNameBySheetName(filter.getSheetName());
        }

        // Missing the expected sheet = no data
        if (selectedTableName == null) return createEmptyResult();

        // Get rows from exported tables
        ExtractionResultVO result = extractionTableDao.getTableRows(selectedTableName, filter, offset, size, sort, direction);

        // Clean created tables
        asyncClean(context);

        return result;
    }

    protected File getLiveFile(ExtractionLiveFormat format, ExtractionFilterVO filter) {
        Preconditions.checkNotNull(format);

        // Execute live extraction to temp tables
        ExtractionLiveContextVO context = executeLiveExport(format, filter);

        // Dump tables
        return writeTablesToFile(context);
    }

    protected ExtractionResultVO getProductRows(ExtractionProduct product, ExtractionFilterVO filter, int offset, int size, String sort, SortDirection direction) {
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


    protected File getProductFile(ExtractionProduct product, ExtractionFilterVO filter) throws IOException {
        Preconditions.checkNotNull(product);
        Preconditions.checkNotNull(filter);

        ExtractionProductContextVO context = new ExtractionProductContextVO();
        context.setId(System.currentTimeMillis());
        context.setProduct(product);

        // No sheet (product with only one table)
        if (ArrayUtils.isEmpty(product.getSheetNames())) {
            String tableName = getProductTableName(product, null);
            context.addTableName(tableName, context.getLabel());
        }

        // One or more sheet
        else {
            Arrays.stream(product.getSheetNames())
                    .forEach(sheetName -> {
                        String tableName = getProductTableName(product, sheetName);
                        context.addTableName(tableName, sheetName);
            });
        }

        // Dump to file
        return writeTablesToFile(context, filter);
    }

    protected ExtractionLiveContextVO executeLiveExport(ExtractionLiveFormat format,
                                                        ExtractionFilterVO filter) {

        ExtractionLiveContextVO context;

        switch (format) {
            case ICES:
                context = extractionIcesDao.execute(extractionIcesDao.toTripFilterVO(filter), filter);
                break;
            case SURVIVAL_TEST:
                context = extractionSurvivalTestDao.execute(extractionSurvivalTestDao.toTripFilterVO(filter), filter);
                break;
            default:
                throw new SumarisTechnicalException("Unknown extraction type: " + format);
        }

        return context;
    }

    protected File writeTablesToFile(ExtractionContextVO context) {
        return writeTablesToFile(context, null);
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

        File outputDirectory = createTempDirectory(basename);
        List<File> outputFiles = context.getTableNames().stream()
                .map(tableName -> {
                    try {
                        File tempCsvFile = new File(outputDirectory, context.getSheetName(tableName) + ".csv");
                        writeTableToFile(tableName, filter, tempCsvFile);
                        return tempCsvFile;
                    } catch (IOException e) {
                        log.error(String.format("Could not generate CSV file for table {%s}", tableName), e);
                        throw new SumarisTechnicalException(e);
                    }
                })
                .collect(Collectors.toList());


        // One file: copy to result file
        File outputFile;
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
            }
            catch(IOException e) {
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
        }
        catch(Exception e) {
            log.debug(String.format("Table %s not found. Skipping", tableName));
            return;
        }

        String whereClause = SumarisTableMetadatas.getSqlWhereClause(table, filter);
        String query = table.getSelectAllQuery() + whereClause;

        Map<String, String> fieldNamesByAlias = getFieldNameAlias(table);
        Map<String, String> decimalFormats = Maps.newHashMap();
        Map<String, String> dateFormats = Maps.newHashMap();
        List<String> ignoredField = getIgnoredFields(table);

        extractionCsvDao.dumpQueryToCSV(outputFile, query, fieldNamesByAlias, decimalFormats, dateFormats, ignoredField);

    }

    protected void asyncClean(ExtractionContextVO context) {
        if (taskExecutor == null) {
            clean(context);
        }
        else {
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
        long rectCount = referentialService.countByLevelId(Location.class.getSimpleName(), LocationLevelEnum.RECTANGLE_ICES.getId());
        if (rectCount == 0) {
            // Insert missing rectangle
            locationService.insertOrUpdateRectangleLocations();

            // Update location hierarchy
            locationService.updateLocationHierarchy();
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
        }catch(IOException e) {
            throw new SumarisTechnicalException("Could not create temporary directory for extraction", e);
        }
    }

    protected Map<String, String> getFieldNameAlias(SumarisTableMetadata table) {
        return table.getColumnNames().stream()
                .collect(Collectors.toMap(
                        columnName -> columnName.toUpperCase(),
                        StringUtils::underscoreToChangeCase));
    }

    protected List<String> getIgnoredFields(SumarisTableMetadata table) {
        return table.getColumnNames().stream()
                // Exclude internal FK columns
                .filter(columnName -> columnName.toLowerCase().endsWith("_fk"))
                .collect(Collectors.toList());
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
        }
        else {
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
            if (product == ExtractionProduct.ICES) {
                tableName = icesProductTableBySheet.get(sheetName);
            }
            if (StringUtils.isBlank(tableName)) {
                tableName = "P_" + product.name() + "_" + sheetName.toUpperCase();
            }
        }
        else {
            tableName = "P_" + product.name();
        }

        return tableName;
    }
}

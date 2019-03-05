package net.sumaris.core.extraction.service;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.dao.technical.model.IEntityBean;
import net.sumaris.core.dao.technical.schema.DatabaseTableEnum;
import net.sumaris.core.dao.technical.schema.SumarisDatabaseMetadata;
import net.sumaris.core.dao.technical.schema.SumarisTableMetadata;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.extraction.dao.technical.csv.ExtractionCsvDao;
import net.sumaris.core.extraction.dao.technical.table.ExtractionTableDao;
import net.sumaris.core.extraction.dao.trip.ExtractionTripDao;
import net.sumaris.core.extraction.dao.trip.ices.ExtractionIcesDao;
import net.sumaris.core.extraction.dao.trip.survivalTest.ExtractionSurvivalTestDao;
import net.sumaris.core.extraction.vo.ExtractionFilterVO;
import net.sumaris.core.extraction.vo.ExtractionResultVO;
import net.sumaris.core.extraction.vo.ExtractionTypeVO;
import net.sumaris.core.extraction.vo.trip.ExtractionTripContextVO;
import net.sumaris.core.extraction.vo.trip.ExtractionTripFilterVO;
import net.sumaris.core.extraction.vo.trip.ExtractionTripFormat;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
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

    private List<ExtractionTypeVO> extractionTypes;

    @PostConstruct
    protected void afterPropertiesSet() {
        // Init extractions types
        extractionTypes = ImmutableList.<ExtractionTypeVO>builder()
                .addAll(getTableExtractionTypes())
                .addAll(getTripExtractionTypes())
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

        switch (type.getCategory()) {
            case ExtractionTableDao.CATEGORY:
                return getTableRows(type.getLabel(), filter, offset, size, sort, direction);
            case ExtractionTripDao.CATEGORY:
                return getTripRows(type.getLabel(), filter, offset, size, sort, direction)    ;
            default:
                throw new SumarisTechnicalException(String.format("Extraction of category %s not implemented yet !", type.getCategory()));
        }
    }


    @Override
    public File exportTripsToFile(ExtractionTripFormat format, ExtractionTripFilterVO filter) {

        ExtractionTripContextVO context = exportTripsToTables(format, filter);

        // Write the table
        return writeTablesToFile(context);
    }

    /* -- protected -- */

    protected List<ExtractionTypeVO> getTableExtractionTypes() {
        return extractionTableDao.getAllTableNames().stream()
                .map(tableName -> {
                    ExtractionTypeVO type = new ExtractionTypeVO();
                    type.setLabel(tableName);
                    type.setCategory(ExtractionTableDao.CATEGORY);
                    return type;
                }).collect(Collectors.toList());
    }

    protected List<ExtractionTypeVO> getTripExtractionTypes() {
        return Arrays.stream(ExtractionTripFormat.values())
                .map(format -> {
                    ExtractionTypeVO type = new ExtractionTypeVO();
                    type.setLabel(format.name().toLowerCase());
                    type.setCategory(ExtractionTripDao.CATEGORY);
                    type.setSheetNames(format.getSheetNames());
                    return type;
                })
                .collect(Collectors.toList());
    }


    protected ExtractionResultVO getTripRows(String formatStr, ExtractionFilterVO filter, int offset, int size, String sort, SortDirection direction) {
        Preconditions.checkNotNull(formatStr);

        ExtractionTripFilterVO tripFilter = toTripFilterVO(filter);
        ExtractionTripFormat format = ExtractionTripFormat.valueOf(formatStr.toUpperCase());

        // Get only the first table, when possible
        if (StringUtils.isBlank(filter.getSheetName())) {
            tripFilter.setFirstTableOnly(true);
        }

        // Replace default sort attribute
        if (IEntityBean.PROPERTY_ID.equalsIgnoreCase(sort)) {
            sort = null;
        }

        // Export data to temp tables
        ExtractionTripContextVO context = exportTripsToTables(format, tripFilter);

        String selectedTableName = context.getTableNames().iterator().next();
        if (StringUtils.isNotBlank(filter.getSheetName())) {
            selectedTableName = context.getTableNameBySheetName(filter.getSheetName());
        }

        // Missing the expected sheet = no data
        if (selectedTableName == null) return createEmptyResult();

        // Return the selected table rows
        return extractionTableDao.getTableRows(selectedTableName, filter, offset, size, sort, direction);
    }

    protected ExtractionResultVO getTableRows(String tableName, ExtractionFilterVO filter, int offset, int size, String sort, SortDirection direction) {
        Preconditions.checkNotNull(tableName);
        Preconditions.checkArgument(offset >= 0);
        Preconditions.checkArgument(size <= 1000, "maximum value for 'size' is: 1000");
        Preconditions.checkArgument(size >= 0, "'size' must be greater or equals to 0");

        // Make sure the table is allow to be exported
        DatabaseTableEnum.valueOf(tableName.toUpperCase());

        return extractionTableDao.getTableRows(tableName, filter != null ? filter : new ExtractionFilterVO(), offset, size, sort, direction);
    }

    protected ExtractionTripContextVO exportTripsToTables(ExtractionTripFormat format, ExtractionTripFilterVO tripFilter) {

        // TODO: limit to the first table, and execute as a view, without table ?
        log.warn("TODO: Limit trip extraction rows view to first table, and limit row");

//        Pageable pageable = Pageable.builder()
//                .setOffset(offset)
//                .setSize(size)
//                .setSortAttribute(sort)
//                .setSortDirection(direction)
//                .build();


        ExtractionTripContextVO context;

        switch (format) {
            case ICES:
                context = extractionIcesDao.execute(tripFilter);
                break;
            case SURVIVAL_TEST:
                context = extractionSurvivalTestDao.execute(tripFilter);
                break;
            default:
                throw new SumarisTechnicalException("Unknown extraction type: " + format);
        }

        return context;
    }



    protected File writeTablesToFile(ExtractionTripContextVO context) {
        Preconditions.checkNotNull(context);

        // Dump table to CSV files
        if (CollectionUtils.isNotEmpty(context.getTableNames())) {
            log.debug("Creating CSV files...");

            File outputDirectory = createTempDirectory(context);
            List<File> outputFiles = context.getTableNames().stream()
                    .map(tableName -> {
                        try {

                            File tempCsvFile = new File(outputDirectory, context.getSheetName(tableName) + ".csv");
                            writeTableToFile(tableName, tempCsvFile);
                            return tempCsvFile;
                        } catch (IOException e) {
                            log.error(String.format("Could not generate CSV file for table {%s}", tableName), e);
                            throw new SumarisTechnicalException(e);
                        }
                    })
                    .collect(Collectors.toList());

            // One file: copy to result file
            if (outputFiles.size() == 1) {
                File outputFile = outputFiles.get(0);
                log.debug(String.format("Extraction file generated at {%s}", outputFile.getPath()));
                return outputFile;
            }

            // Many files: create a zip archive
            else if (outputFiles.size() > 1) {
                log.debug("Creating a ZIP archive...");

                File zipOutputFile = new File(outputDirectory.getParent(), context.getId() + ".zip");
                try {
                    ZipUtils.compressFilesInPath(outputDirectory, zipOutputFile, false);
                }
                catch(IOException e) {
                    throw new SumarisTechnicalException(e);
                }
                log.debug(String.format("Extraction file generated at {%s}", zipOutputFile.getPath()));
                return zipOutputFile;
            }

        }
        return null;
    }

    protected void writeTableToFile(String tableName, File outputFile) throws IOException {
        SumarisTableMetadata table;
        try {
            table = databaseMetadata.getTable(tableName);
        }
        catch(Exception e) {
            log.debug(String.format("Table %s not found. Skipping", tableName));
            return;
        }

        String query = table.getSelectAllQuery();

        Map<String, String> fieldNamesByAlias = getFieldNameAlias(table);
        Map<String, String> decimalFormats = Maps.newHashMap();
        Map<String, String> dateFormats = Maps.newHashMap();
        List<String> ignoredField = getIgnoredFields(table);

        extractionCsvDao.dumpQueryToCSV(outputFile, query, fieldNamesByAlias, decimalFormats, dateFormats, ignoredField);

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

    protected File createTempDirectory(ExtractionTripContextVO context) {
        try {
            File outputDirectory = new File(configuration.getTempDirectory(), String.valueOf(context.getId()));
            if (outputDirectory.exists()) {
                Files.cleanDirectory(outputDirectory, "Could not create temporary directory");
            } else {
                FileUtils.forceMkdir(outputDirectory);
            }
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

    protected ExtractionTripFilterVO toTripFilterVO(ExtractionFilterVO source){
        ExtractionTripFilterVO target = new ExtractionTripFilterVO();
        if (source == null) return target;

        target.setSheetName(source.getSheetName());

        if (CollectionUtils.isNotEmpty(source.getCriteria())) {

            source.getCriteria().stream()
                    .filter(criterion ->
                            org.apache.commons.lang3.StringUtils.isNotBlank(criterion.getValue())
                            && "=".equals(criterion.getOperator()))
                    .forEach(criterion -> {
                        switch (criterion.getName().toLowerCase()) {
                            case "project":
                                target.setProgramLabel(criterion.getValue());
                                break;
                            case "year":
                                int year = Integer.parseInt(criterion.getValue());
                                target.setStartDate(Dates.getFirstDayOfYear(year));
                                target.setEndDate(Dates.getLastSecondOfYear(year));
                                break;
                        }
                    });
        }
        return target;
    }

    protected ExtractionResultVO createEmptyResult() {
        ExtractionResultVO result = new ExtractionResultVO();
        result.setColumns(ImmutableList.of());
        result.setTotal(0);
        result.setRows(ImmutableList.of());
        return result;
    }
}

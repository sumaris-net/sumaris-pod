package net.sumaris.core.extraction.service;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.dao.technical.schema.DatabaseTableEnum;
import net.sumaris.core.dao.technical.schema.SumarisDatabaseMetadata;
import net.sumaris.core.dao.technical.schema.SumarisTableMetadata;
import net.sumaris.core.exception.DataNotFoundException;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.extraction.dao.csv.ExtractionCsvDao;
import net.sumaris.core.extraction.dao.table.ExtractionTableDao;
import net.sumaris.core.extraction.dao.trip.ExtractionTripDao;
import net.sumaris.core.extraction.dao.trip.ices.ExtractionIcesDao;
import net.sumaris.core.extraction.dao.trip.survivalTest.ExtractionSurvivalTestDao;
import net.sumaris.core.extraction.vo.ExtractionFilterVO;
import net.sumaris.core.extraction.vo.ExtractionResultVO;
import net.sumaris.core.extraction.vo.ExtractionTypeVO;
import net.sumaris.core.extraction.vo.trip.ExtractionTripContextVO;
import net.sumaris.core.extraction.vo.trip.ExtractionTripFilterVO;
import net.sumaris.core.extraction.vo.trip.ExtractionTripFormat;
import net.sumaris.core.util.Files;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.util.ZipUtils;
import net.sumaris.core.vo.filter.TripFilterVO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author peck7 on 17/12/2018.
 */
@Service("extractionService")
@Lazy
public class ExtractionServiceImpl implements ExtractionService {

    private static final Logger log = LoggerFactory.getLogger(ExtractionServiceImpl.class);


    private interface TripExtractionDao {
        ExtractionTripContextVO execute(TripFilterVO filter);
    }

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
    protected SumarisDatabaseMetadata databaseMetadata;

    private Map<ExtractionTripFormat, Function<ExtractionTripFilterVO, ExtractionTripContextVO>> tripExtractionsByFormat =
            ImmutableMap.of(
            ExtractionTripFormat.ICES, tripFilterVO -> extractionIcesDao.execute(tripFilterVO),
            ExtractionTripFormat.SURVIVAL_TEST, tripFilterVO -> extractionSurvivalTestDao.execute(tripFilterVO));


    @Override
    public List<ExtractionTypeVO> getAllTypes() {
        return ImmutableList.<ExtractionTypeVO>builder()
                .addAll(getTableExtractionTypes())
                .addAll(getTripExtractionTypes())
                .build();
    }

    @Override
    public ExtractionResultVO getRows(ExtractionTypeVO type, ExtractionFilterVO filter, int offset, int size, String sort, SortDirection direction) {
        Preconditions.checkNotNull(type);
        Preconditions.checkNotNull(type.getLabel());
        Preconditions.checkNotNull(type.getCategory());

        switch (type.getCategory()) {
            case ExtractionTableDao.CATEGORY:
                return getTableRows(type.getLabel(), filter, offset, size, sort, direction);
            case ExtractionTripDao.CATEGORY:
                return getTripRows(type.getLabel(), filter, offset, size, sort, direction)    ;
            default:
                throw new IllegalArgumentException("Unknown extraction category: " + type.getCategory());
        }
    }


    @Override
    public File exportTripsToFile(ExtractionTripFormat format, ExtractionTripFilterVO filter) {

        Function<ExtractionTripFilterVO, ExtractionTripContextVO> function = tripExtractionsByFormat.get(format);
        ExtractionTripContextVO context;
        try {
            context = function.apply(filter);
        } catch (DataNotFoundException e) {
            log.info("No data exported");
            // No data: skip
            return null;
        }

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
        return tripExtractionsByFormat.keySet().stream()
                .map(format -> {
                    ExtractionTypeVO type = new ExtractionTypeVO();
                    type.setLabel(format.name().toLowerCase());
                    type.setCategory(ExtractionTripDao.CATEGORY);
                    return type;
                })
                .collect(Collectors.toList());
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

   
    protected ExtractionResultVO getTripRows(String format, ExtractionFilterVO filter, int offset, int size, String sort, SortDirection direction) {
        Preconditions.checkNotNull(format);
        
        ExtractionTripFormat formatEnum = ExtractionTripFormat.valueOf(format.toUpperCase());
        Function<ExtractionTripFilterVO, ExtractionTripContextVO> function = tripExtractionsByFormat.get(formatEnum);

        ExtractionTripFilterVO tripFilter = toTripFilterVO(filter);

        log.warn("TODO: Limit trip extraction rows view to first table, and limit row");
        // TODO: limit to the first table, and execute as a view, without table ?
        ExtractionTripContextVO context =  function.apply(tripFilter);

//        Pageable pageable = Pageable.builder()
//                .setOffset(offset)
//                .setSize(size)
//                .setSortAttribute(sort)
//                .setSortDirection(direction)
//                .build();

        String firstTable = context.getTableNames().iterator().next();
        return extractionTableDao.getTableRows(firstTable, null, offset, size, sort, direction);
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

                            File tempCsvFile = new File(outputDirectory, context.getUserFriendlyName(tableName) + ".csv");
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
                        columnName -> columnName,
                        StringUtils::underscoreToChangeCase));
    }

    protected List<String> getIgnoredFields(SumarisTableMetadata table) {
        return table.getColumnNames().stream()
                // Exclude internal FK columns
                .filter(columnName -> columnName.endsWith("_FK"))
                .collect(Collectors.toList());
    }

    protected ExtractionTripFilterVO toTripFilterVO(ExtractionFilterVO source){
        log.warn("TODO: implement conversion of generic filter to TripFilterVO");
        return null;
    }
}

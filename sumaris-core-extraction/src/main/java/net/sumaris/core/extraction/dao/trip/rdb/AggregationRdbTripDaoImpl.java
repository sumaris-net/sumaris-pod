package net.sumaris.core.extraction.dao.trip.rdb;

import com.google.common.base.Preconditions;
import com.google.common.collect.*;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.dao.technical.schema.SumarisColumnMetadata;
import net.sumaris.core.dao.technical.schema.SumarisDatabaseMetadata;
import net.sumaris.core.dao.technical.schema.SumarisTableMetadata;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.extraction.dao.technical.ExtractionBaseDaoImpl;
import net.sumaris.core.extraction.dao.technical.XMLQuery;
import net.sumaris.core.extraction.dao.technical.schema.SumarisTableMetadatas;
import net.sumaris.core.extraction.dao.technical.table.ExtractionTableDao;
import net.sumaris.core.extraction.dao.trip.AggregationTripDao;
import net.sumaris.core.extraction.vo.*;
import net.sumaris.core.extraction.vo.trip.rdb.AggregationRdbTripContextVO;
import net.sumaris.core.extraction.vo.trip.rdb.ExtractionRdbTripVersion;
import net.sumaris.core.model.referential.pmfm.PmfmEnum;
import net.sumaris.core.service.administration.programStrategy.ProgramService;
import net.sumaris.core.service.administration.programStrategy.StrategyService;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.technical.extraction.ExtractionProductVO;
import org.apache.commons.collections4.SetUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.nuiton.i18n.I18n.t;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 */
@Repository("aggregationRdbDao")
@Lazy
public class AggregationRdbTripDaoImpl<
        C extends AggregationRdbTripContextVO,
        F extends ExtractionFilterVO,
        S extends AggregationStrataVO>
        extends ExtractionBaseDaoImpl
        implements AggregationRdbTripDao<C, F, S>, AggregationTripDao {

    private static final Logger log = LoggerFactory.getLogger(AggregationRdbTripDaoImpl.class);

    private static final String TR_TABLE_NAME_PATTERN = TABLE_NAME_PREFIX + TR_SHEET_NAME + "_%s";
    private static final String HH_TABLE_NAME_PATTERN = TABLE_NAME_PREFIX + HH_SHEET_NAME + "_%s";
    private static final String SL_TABLE_NAME_PATTERN = TABLE_NAME_PREFIX + SL_SHEET_NAME + "_%s";
    private static final String HL_TABLE_NAME_PATTERN = TABLE_NAME_PREFIX + HL_SHEET_NAME + "_%s";
    private static final String CA_TABLE_NAME_PATTERN = TABLE_NAME_PREFIX + CA_SHEET_NAME + "_%s";

    private static List<String> SPACE_STRATA = ImmutableList.of("area", "rect", "square");
    private static List<String> TIME_STRATA = ImmutableList.of("year", "quarter", "month");
    private static Map<String, List<String>> TECH_STRATA_BY_SHEETNAME = ImmutableMap.<String, List<String>>builder()
            .put(HH_SHEET_NAME, ImmutableList.of("trip_count"))
            .build();

    @Autowired
    protected StrategyService strategyService;

    @Autowired
    protected ProgramService programService;

    @Autowired
    protected ResourceLoader resourceLoader;

    @Autowired
    protected SumarisDatabaseMetadata databaseMetadata;

    @javax.annotation.Resource(name = "extractionRdbTripDao")
    protected ExtractionRdbTripDao extractionRdbTripDao;

    @Autowired
    protected ExtractionTableDao extractionTableDao;

    @Override
    public <R extends C> R aggregate(ExtractionProductVO source, F filter) {

        // Init context
        R context = createNewContext();
        context.setTripFilter(extractionRdbTripDao.toTripFilterVO(filter));
        context.setFilter(filter);
        context.setFormatName(RDB_FORMAT);
        context.setFormatVersion(ExtractionRdbTripVersion.VERSION_1_3.getLabel());
        context.setId(System.currentTimeMillis());

        // Compute table names
        context.setTripTableName(String.format(TR_TABLE_NAME_PATTERN, context.getId()));
        context.setStationTableName(String.format(HH_TABLE_NAME_PATTERN, context.getId()));
        context.setSpeciesListTableName(String.format(SL_TABLE_NAME_PATTERN, context.getId()));
        context.setSpeciesLengthTableName(String.format(HL_TABLE_NAME_PATTERN, context.getId()));
        context.setSampleTableName(String.format(CA_TABLE_NAME_PATTERN, context.getId()));

        // Expected sheet name
        String sheetName = filter != null && filter.isPreview() ? filter.getSheetName() : null;

        // Trip
        long rowCount;
        //rowCount = createTripTable(context);
        //if (rowCount == 0) throw new DataNotFoundException(t("sumaris.aggregation.noData"));
        //if (sheetName != null && context.hasSheet(sheetName)) return context;

        // Station
        rowCount = createStationTable(source, context);
        if (rowCount == 0) return context;
        if (sheetName != null && context.hasSheet(sheetName)) return context;

        // Species List
        //rowCount = createSpeciesListTable(context);
        //if (rowCount == 0) return context;
        //if (sheetName != null && context.hasSheet(sheetName)) return context;

        // Species Length
        //createSpeciesLengthTable(context);

        return context;

    }

    public AggregationResultVO read(String tableName, F filter, S strata, int offset, int size, String sortAttribute, SortDirection direction) {
        Preconditions.checkNotNull(tableName);
        Preconditions.checkNotNull(strata);

        Set<String> groupByColumnNames = Sets.newLinkedHashSet();
        Map<String, ExtractionTableDao.SQLAggregatedFunction> techColumns = Maps.newHashMap();

        // Process space strata
        {
            String spaceStrata = strata.getSpace() != null ? strata.getSpace().toLowerCase() : COLUMN_AREA;

            // Replace alias
            if (COLUMN_ALIAS.containsKey(spaceStrata)) spaceStrata = COLUMN_ALIAS.get(spaceStrata);

            switch (spaceStrata) {
                case COLUMN_SQUARE:
                    groupByColumnNames.add(COLUMN_SQUARE);
                case COLUMN_STATISTICAL_RECTANGLE:
                    groupByColumnNames.add(COLUMN_STATISTICAL_RECTANGLE);
                case COLUMN_AREA:
                default:
                    groupByColumnNames.add(COLUMN_AREA);
            }
        }

        // Time strata
        {
            String timeStrata = strata.getTime() != null ? strata.getTime().toLowerCase() : COLUMN_YEAR;
            switch (timeStrata) {
                case COLUMN_MONTH:
                    groupByColumnNames.add(COLUMN_MONTH);
                case COLUMN_QUARTER:
                    groupByColumnNames.add(COLUMN_QUARTER);
                case COLUMN_YEAR:
                default:
                    groupByColumnNames.add(COLUMN_YEAR);
            }
        }

        // Tech strata
        {
            String techStrata = strata.getTech() != null ? strata.getTech().toLowerCase() : "trip_count";
            ExtractionTableDao.SQLAggregatedFunction function = strata.getTechFunction() != null ?
                    ExtractionTableDao.SQLAggregatedFunction.valueOf(strata.getTechFunction().toUpperCase()) :
                    ExtractionTableDao.SQLAggregatedFunction.SUM;
            techColumns.put(techStrata, function);
        }

        ExtractionResultVO rows = extractionTableDao.getTableGroupByRows(tableName, filter, groupByColumnNames, techColumns,
                offset, size, sortAttribute, direction);

        AggregationResultVO result = new AggregationResultVO(rows);

        // TODO: filter is NULL values ?
        result.setSpaceStrata(SPACE_STRATA);
        result.setTimeStrata(TIME_STRATA);
        if (filter.getSheetName() != null) {
            result.setTechStrata(TECH_STRATA_BY_SHEETNAME.get(filter.getSheetName()));
        }

        return result;
    }


    /* -- protected methods -- */

    protected <C extends AggregationRdbTripContextVO> C createNewContext() {
        Class<? extends AggregationRdbTripContextVO> contextClass = getContextClass();
        Preconditions.checkNotNull(contextClass);

        try {
            return (C) contextClass.newInstance();
        } catch (Exception e) {
            throw new SumarisTechnicalException("Could not create an instance of context class " + contextClass.getName());
        }
    }

    protected Class<? extends AggregationRdbTripContextVO> getContextClass() {
        return AggregationRdbTripContextVO.class;
    }

    protected long createStationTable(ExtractionProductVO source, AggregationRdbTripContextVO context) {

        String tableName = context.getStationTableName();
        XMLQuery xmlQuery = createStationQuery(source, context);

        // aggregate insertion
        execute(xmlQuery);
        long count = countFrom(tableName);

        // Clean row using generic tripFilter
        if (count == 0) return 0;

        count -= cleanRow(tableName, context.getFilter(), HH_SHEET_NAME);

        // Analyze row
        Map<String, List<Object>> columnValues = null;
        if (context.isEnableAnalyze()) {
            columnValues = analyzeRow(tableName, xmlQuery, COLUMN_YEAR);
        }

        // Add result table to context
        context.addTableName(tableName, HH_SHEET_NAME, columnValues);
        log.debug(String.format("Station table: %s rows inserted", count));

        return count;
    }

    protected XMLQuery createStationQuery(ExtractionProductVO source, AggregationRdbTripContextVO context) {

        XMLQuery xmlQuery = createXMLQuery(context, "createStationTable");
        xmlQuery.bind("rawTripTableName", source.getTableNameBySheetName(ExtractionRdbTripDao.TR_SHEET_NAME)
                .orElseThrow(() -> new SumarisTechnicalException("Missing TR table")));
        xmlQuery.bind("rawStationTableName", source.getTableNameBySheetName(ExtractionRdbTripDao.HH_SHEET_NAME)
                .orElseThrow(() -> new SumarisTechnicalException("Missing HH table")));
        xmlQuery.bind("stationTableName", context.getStationTableName());

        return xmlQuery;
    }

    protected long createSpeciesListTable(AggregationRdbTripContextVO context) {

        XMLQuery xmlQuery = createSpeciesListQuery(context, true/*exclude invalid station*/);

        // aggregate insertion
        execute(xmlQuery);
        long count = countFrom(context.getSpeciesListTableName());

        // Clean row using generic tripFilter
        if (count > 0) {
            count -= cleanRow(context.getSpeciesListTableName(), context.getFilter(), SL_SHEET_NAME);
        }

        // Add result table to context
        if (count > 0) {
            context.addTableName(context.getSpeciesListTableName(), SL_SHEET_NAME);
            log.debug(String.format("Species list table: %s rows inserted", count));
        }
        return count;
    }

    protected XMLQuery createSpeciesListQuery(AggregationRdbTripContextVO context, boolean excludeInvalidStation) {
        XMLQuery xmlQuery = createXMLQuery(context, "createSpeciesListTable");
        xmlQuery.bind("stationTableName", context.getStationTableName());
        xmlQuery.bind("speciesListTableName", context.getSpeciesListTableName());

        // Bind some ids
        xmlQuery.bind("catchCategoryPmfmId", String.valueOf(PmfmEnum.DISCARD_OR_LANDING.getId()));

        // Exclude not valid station
        xmlQuery.setGroup("excludeInvalidStation", excludeInvalidStation);

        return xmlQuery;
    }


    protected long createSpeciesLengthTable(AggregationRdbTripContextVO context) {

        XMLQuery xmlQuery = createSpeciesLengthQuery(context);

        // aggregate insertion
        execute(xmlQuery);
        long count = countFrom(context.getSpeciesLengthTableName());

        // Clean row using generic tripFilter
        if (count > 0) {
            count -= cleanRow(context.getSpeciesLengthTableName(), context.getFilter(), HL_SHEET_NAME);
        }

        // Add result table to context
        if (count > 0) {
            context.addTableName(context.getSpeciesLengthTableName(), HL_SHEET_NAME);
            log.debug(String.format("Species length table: %s rows inserted", count));
        }
        return count;
    }

    protected XMLQuery createSpeciesLengthQuery(AggregationRdbTripContextVO context) {
        XMLQuery xmlQuery = createXMLQuery(context, "createSpeciesLengthTable");
        xmlQuery.bind("stationTableName", context.getStationTableName());
        xmlQuery.bind("speciesListTableName", context.getSpeciesListTableName());
        xmlQuery.bind("speciesLengthTableName", context.getSpeciesLengthTableName());

        // Bind some ids
        xmlQuery.bind("sexPmfmId", String.valueOf(PmfmEnum.SEX.getId()));
        xmlQuery.bind("lengthTotalPmfmId", String.valueOf(PmfmEnum.LENGTH_TOTAL_CM.getId()));

        return xmlQuery;
    }

    protected int execute(XMLQuery xmlQuery) {
        return queryUpdate(xmlQuery.getSQLQueryAsString());
    }

    protected long countFrom(String tableName) {
        XMLQuery xmlQuery = createXMLQuery("countFrom");
        xmlQuery.bind("tableName", tableName);
        return queryCount(xmlQuery.getSQLQueryAsString());
    }

    protected String getQueryFullName(ExtractionContextVO context, String queryName) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(context.getFormatName());
        Preconditions.checkNotNull(context.getFormatVersion());

        return String.format("%s/v%s/aggregation/%s",
                context.getFormatName(),
                context.getFormatVersion().replaceAll("[.]", "_"),
                queryName);
    }

    protected XMLQuery createXMLQuery(ExtractionContextVO context, String queryName) {
        return createXMLQuery(getQueryFullName(context, queryName));
    }

    protected XMLQuery createXMLQuery(String queryName) {
        XMLQuery query = createXMLQuery();
        query.setQuery(getXMLQueryClasspathURL(queryName));
        return query;
    }

    protected URL getXMLQueryURL(ExtractionContextVO context, String queryName) {
        return getXMLQueryClasspathURL(getQueryFullName(context, queryName));
    }

    protected URL getXMLQueryClasspathURL(String queryName) {
        Resource resource = resourceLoader.getResource(ResourceLoader.CLASSPATH_URL_PREFIX + XML_QUERY_PATH + "/" + queryName + ".xml");
        if (!resource.exists())
            throw new SumarisTechnicalException(t("sumaris.extraction.xmlQuery.notFound", queryName));
        try {
            return resource.getURL();
        } catch (IOException e) {
            throw new SumarisTechnicalException(e);
        }
    }

    protected int cleanRow(String tableName, ExtractionFilterVO filter, String sheetName) {
        Preconditions.checkNotNull(tableName);
        if (filter == null) return 0;

        // TODO add cache
        SumarisTableMetadata table = databaseMetadata.getTable(tableName.toLowerCase());
        Preconditions.checkNotNull(table);

        String whereClauseContent = SumarisTableMetadatas.getSqlWhereClauseContent(table, filter, sheetName, table.getAlias());
        if (StringUtils.isBlank(whereClauseContent)) return 0;

        String deleteQuery = table.getDeleteQuery(String.format("NOT(%s)", whereClauseContent));
        return queryUpdate(deleteQuery);
    }


    protected Map<String, List<Object>> analyzeRow(final String tableName, XMLQuery xmlQuery, String... numericColumnToAnalyse) {
        Preconditions.checkNotNull(tableName);
        Preconditions.checkNotNull(xmlQuery);

        return Stream.concat(xmlQuery.getNotNumericColumnNames().stream(), Stream.of(numericColumnToAnalyse))
                .collect(Collectors.toMap(
                        c -> c,
                        c -> query(String.format("SELECT DISTINCT %s FROM %s", c, tableName), Object.class))
                );
    }

}

package net.sumaris.core.extraction.dao.trip.rdb;

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

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import net.sumaris.core.dao.technical.DatabaseType;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.dao.technical.schema.SumarisDatabaseMetadata;
import net.sumaris.core.dao.technical.schema.SumarisTableMetadata;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.extraction.dao.technical.Daos;
import net.sumaris.core.extraction.dao.technical.ExtractionBaseDaoImpl;
import net.sumaris.core.extraction.dao.technical.XMLQuery;
import net.sumaris.core.extraction.dao.technical.schema.SumarisTableMetadatas;
import net.sumaris.core.extraction.dao.technical.table.ExtractionTableDao;
import net.sumaris.core.extraction.dao.trip.AggregationTripDao;
import net.sumaris.core.extraction.specification.AggRdbSpecification;
import net.sumaris.core.extraction.specification.RdbSpecification;
import net.sumaris.core.extraction.vo.*;
import net.sumaris.core.extraction.vo.trip.rdb.AggregationRdbTripContextVO;
import net.sumaris.core.model.referential.pmfm.PmfmEnum;
import net.sumaris.core.model.technical.extraction.rdb.ProductRdbSpeciesList;
import net.sumaris.core.model.technical.extraction.rdb.ProductRdbStation;
import net.sumaris.core.service.administration.programStrategy.ProgramService;
import net.sumaris.core.service.administration.programStrategy.StrategyService;
import net.sumaris.core.util.Beans;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.technical.extraction.ExtractionProductVO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;
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
        implements AggregationRdbTripDao<C, F, S>,
        AggregationTripDao, AggRdbSpecification {

    private static final Logger log = LoggerFactory.getLogger(AggregationRdbTripDaoImpl.class);

    private static final String TR_TABLE_NAME_PATTERN = TABLE_NAME_PREFIX + TR_SHEET_NAME + "_%s"; // TODO to remove
    private static final String HH_TABLE_NAME_PATTERN = TABLE_NAME_PREFIX + HH_SHEET_NAME + "_%s";
    private static final String SL_TABLE_NAME_PATTERN = TABLE_NAME_PREFIX + SL_SHEET_NAME + "_%s";
    private static final String HL_TABLE_NAME_PATTERN = TABLE_NAME_PREFIX + HL_SHEET_NAME + "_%s";

    private static final String HL_MAP_TABLE_NAME_PATTERN = TABLE_NAME_PREFIX + HL_SHEET_NAME + "_MAP_%s";


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
        long rowCount;

        // Init context
        R context = createNewContext();
        context.setTripFilter(extractionRdbTripDao.toTripFilterVO(filter));
        context.setFilter(filter);
        context.setFormatName(AggRdbSpecification.FORMAT);
        context.setFormatVersion(AggRdbSpecification.VERSION_1_3);
        context.setId(System.currentTimeMillis());

        // Compute table names
        context.setTripTableName(String.format(TR_TABLE_NAME_PATTERN, context.getId()));
        context.setStationTableName(String.format(HH_TABLE_NAME_PATTERN, context.getId()));
        context.setSpeciesListTableName(String.format(SL_TABLE_NAME_PATTERN, context.getId()));
        context.setSpeciesLengthTableName(String.format(HL_TABLE_NAME_PATTERN, context.getId()));
        context.setSpeciesLengthMapTableName(String.format(HL_MAP_TABLE_NAME_PATTERN, context.getId()));

        // Expected sheet name
        String sheetName = filter != null && filter.isPreview() ? filter.getSheetName() : null;

        try {
            // Station
            rowCount = createStationTable(source, context);
            if (rowCount == 0) return context;
            if (sheetName != null && context.hasSheet(sheetName)) return context;

            // Species List
            rowCount = createSpeciesListTable(source, context);
            if (rowCount == 0) return context;
            if (sheetName != null && context.hasSheet(sheetName)) return context;

            // Species Raw table
            rowCount = createSpeciesLengthMapTable(source, context);
            if (rowCount == 0) return context;

            // Species Length
            createSpeciesLengthTable(source, context);

            return context;
        }
        finally {
            //dropHiddenColumns(context);
        }

    }

    public AggregationResultVO read(String tableName, F filter, S strata, int offset, int size, String sortAttribute, SortDirection direction) {
        Preconditions.checkNotNull(tableName);
        Preconditions.checkNotNull(strata);

        Set<String> groupByColumnNames = Sets.newLinkedHashSet();
        Map<String, ExtractionTableDao.SQLAggregatedFunction> aggColumns = Maps.newHashMap();

        // Process space strata
        {
            String spaceStrata = strata.getSpaceColumnName() != null ? strata.getSpaceColumnName().toLowerCase() : COLUMN_AREA;

            // Replace alias
            spaceStrata = COLUMN_ALIAS.containsKey(spaceStrata) ? COLUMN_ALIAS.get(spaceStrata) : spaceStrata;

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
            String timeStrata = strata.getTimeColumnName() != null ? strata.getTimeColumnName().toLowerCase() : COLUMN_YEAR;
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

        // Agg strata
        {
            String aggStrata = strata.getAggColumnName() != null ? strata.getAggColumnName().toLowerCase() : COLUMN_STATION_COUNT;
            ExtractionTableDao.SQLAggregatedFunction function = strata.getAggFunction() != null ?
                    ExtractionTableDao.SQLAggregatedFunction.valueOf(strata.getAggFunction().toUpperCase()) :
                    ExtractionTableDao.SQLAggregatedFunction.SUM;
            aggColumns.put(aggStrata, function);
        }

        ExtractionResultVO rows = extractionTableDao.getTableGroupByRows(tableName, filter, groupByColumnNames, aggColumns,
                offset, size, sortAttribute, direction);

        AggregationResultVO result = new AggregationResultVO(rows);

        result.setSpaceStrata(SPACE_STRATA);
        result.setTimeStrata(TIME_STRATA);
        if (filter.getSheetName() != null) {
            result.setAggStrata(AGG_STRATA_BY_SHEETNAME.get(filter.getSheetName()));
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

        if (count == 0) return 0;

        // Clean row using filter
        count -= cleanRow(tableName, context.getFilter(), HH_SHEET_NAME);

        // Analyze row
        Map<String, List<String>> columnValues = null;
        if (context.isEnableAnalyze()) {
            columnValues = analyzeRow(tableName, xmlQuery, COLUMN_YEAR);
        }

        // Add result table to context
        context.addTableName(tableName, HH_SHEET_NAME,
                xmlQuery.getHiddenColumnNames(),
                getSpatialColumnNames(xmlQuery),
                columnValues);
        log.debug(String.format("Station table: %s rows inserted", count));

        return count;
    }

    protected XMLQuery createStationQuery(ExtractionProductVO source, AggregationRdbTripContextVO context) {

        String rawTripTableName = source.getTableNameBySheetName(RdbSpecification.TR_SHEET_NAME)
                .orElseThrow(() -> new SumarisTechnicalException(String.format("Missing %s table", RdbSpecification.TR_SHEET_NAME)));
        String rawStationTableName = source.getTableNameBySheetName(RdbSpecification.HH_SHEET_NAME)
                .orElseThrow(() -> new SumarisTechnicalException(String.format("Missing %s table", RdbSpecification.HH_SHEET_NAME)));

        SumarisTableMetadata rawStationTable = databaseMetadata.getTable(rawStationTableName);

        XMLQuery xmlQuery = createXMLQuery(context, "createStationTable");

        xmlQuery.bind("rawTripTableName", rawTripTableName);
        xmlQuery.bind("rawStationTableName", rawStationTableName);
        xmlQuery.bind("stationTableName", context.getStationTableName());

        // Date
        xmlQuery.setGroup("startDateFilter", context.getStartDate() != null);
        xmlQuery.bind("startDate", net.sumaris.core.extraction.dao.technical.Daos.getSqlToDate(context.getStartDate()));
        xmlQuery.setGroup("endDateFilter", context.getEndDate() != null);
        xmlQuery.bind("endDate", Daos.getSqlToDate(context.getEndDate()));

        // Program
        xmlQuery.setGroup("programFilter", CollectionUtils.isNotEmpty(context.getProgramLabels()));
        xmlQuery.bind("progLabels", Daos.getSqlInValueFromStringCollection(context.getProgramLabels()));

        // Vessel
        boolean hasVesselFilter = CollectionUtils.isNotEmpty(context.getVesselIds());
        xmlQuery.setGroup("vesselFilter", hasVesselFilter);
        xmlQuery.bind("vesselIds", Daos.getSqlInValueFromIntegerCollection(context.getVesselIds()));

        // Trip
        boolean hasTripFilter = CollectionUtils.isNotEmpty(context.getTripCodes());
        xmlQuery.setGroup("tripFilter", hasTripFilter);
        xmlQuery.bind("tripCodes", Daos.getSqlInValueFromStringCollection(context.getTripCodes()));

        xmlQuery.setGroup("excludeInvalidStation", true);
        xmlQuery.setGroup("gearType", rawStationTable.getColumnMetadata(ProductRdbStation.COLUMN_GEAR_TYPE) != null);

        xmlQuery.setGroup("hsqldb", this.databaseType == DatabaseType.hsqldb);
        xmlQuery.setGroup("oracle", this.databaseType == DatabaseType.oracle);

        return xmlQuery;
    }

    protected long createSpeciesListTable(ExtractionProductVO source, AggregationRdbTripContextVO context) {
        log.warn("createSpeciesListTable...");
        XMLQuery xmlQuery = createSpeciesListQuery(source, context);

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
        else {
            context.addRawTableName(context.getSpeciesListTableName());
        }
        return count;
    }

    protected XMLQuery createSpeciesListQuery(ExtractionProductVO source, AggregationRdbTripContextVO context) {
        String rawStationTableName = source.getTableNameBySheetName(RdbSpecification.HH_SHEET_NAME)
                .orElseThrow(() -> new SumarisTechnicalException(String.format("Missing %s table", RdbSpecification.HH_SHEET_NAME)));
        String rawSpeciesListTableName = source.getTableNameBySheetName(RdbSpecification.SL_SHEET_NAME)
                .orElseThrow(() -> new SumarisTechnicalException(String.format("Missing %s table", RdbSpecification.SL_SHEET_NAME)));

        XMLQuery xmlQuery = createXMLQuery(context, "createSpeciesListTable");

        xmlQuery.bind("rawSpeciesListTableName", rawSpeciesListTableName);
        xmlQuery.bind("stationTableName", context.getStationTableName());
        xmlQuery.bind("speciesListTableName", context.getSpeciesListTableName());

        // Enable/Disable group, on DBMS
        xmlQuery.setGroup("hsqldb", this.databaseType == DatabaseType.hsqldb);
        xmlQuery.setGroup("oracle", this.databaseType == DatabaseType.oracle);

        // Enable/Disable group, on optional columns
        SumarisTableMetadata rawStationTable = databaseMetadata.getTable(rawStationTableName);
        xmlQuery.setGroup("gearType", rawStationTable.getColumnMetadata(ProductRdbStation.COLUMN_GEAR_TYPE) != null);

        SumarisTableMetadata rawSpeciesListTable = databaseMetadata.getTable(rawSpeciesListTableName);
        xmlQuery.setGroup("hasSampleIds", rawSpeciesListTable.hasColumn(COLUMN_SAMPLE_IDS));
        xmlQuery.setGroup("hasId", rawSpeciesListTable.hasColumn(COLUMN_ID));

        return xmlQuery;
    }

    /**
     * Create a map, used for P01_RDB product aggregation
     * @param source
     * @param context
     * @return
     */
    protected long createSpeciesLengthMapTable(ExtractionProductVO source, AggregationRdbTripContextVO context) {

        String tableName = context.getSpeciesLengthMapTableName();
        log.warn("createSpeciesLengthMapTable... " + tableName);

        XMLQuery xmlQuery = createSpeciesLengthMapQuery(source, context);
        if (xmlQuery == null) return -1; // Skip

        // Create the table
        execute(xmlQuery);

        // Add index
        queryUpdate(String.format("CREATE INDEX %s_IDX on %s (%s)", tableName, tableName, "SL_ID"));

        long count = countFrom(tableName);

        if (count > 0) {
            log.debug(String.format("Species length map table: %s rows inserted", count));
        }

        // Add result table to context
        context.addRawTableName(tableName);

        return count;
    }

    protected XMLQuery createSpeciesLengthMapQuery(ExtractionProductVO source, AggregationRdbTripContextVO context) {
        String rawSpeciesListTableName = source.getTableNameBySheetName(RdbSpecification.SL_SHEET_NAME)
                .orElse(null);
        String rawSpeciesLengthTableName = source.getTableNameBySheetName(RdbSpecification.HL_SHEET_NAME)
                .orElse(null);
        if (rawSpeciesListTableName == null || rawSpeciesLengthTableName == null) return null; // Skip

        // Skip if SL.SAMPLE_IDS exists
        SumarisTableMetadata rawSpeciesListTable = databaseMetadata.getTable(rawSpeciesListTableName);
        if (rawSpeciesListTable.hasColumn(COLUMN_SAMPLE_IDS)) return null;

        // Check column SL.ID exists (e.g when rax tables comes from the 'P01_RDB' product)
        if (!rawSpeciesListTable.hasColumn(COLUMN_ID)) {
            throw new SumarisTechnicalException(String.format("Cannot aggregate. Missing columns '%s' or '%s' in table '%s'",
                    COLUMN_SAMPLE_IDS, COLUMN_ID, rawSpeciesListTableName));
        }

        // Check column HL.ID exists
        SumarisTableMetadata rawSpeciesLengthTable = databaseMetadata.getTable(rawSpeciesLengthTableName);
        if (!rawSpeciesLengthTable.hasColumn(COLUMN_ID)) {
            throw new SumarisTechnicalException(String.format("Cannot aggregate. Missing column '%s' in table '%s'",
                    COLUMN_ID, rawSpeciesLengthTableName));
        }

        XMLQuery xmlQuery = createXMLQuery(context, "createSpeciesLengthMapTable");
        xmlQuery.bind("rawSpeciesListTableName", rawSpeciesListTableName);
        xmlQuery.bind("rawSpeciesLengthTableName", rawSpeciesLengthTableName);
        xmlQuery.bind("speciesLengthMapTableName", context.getSpeciesLengthMapTableName());

        // Program
        xmlQuery.setGroup("programFilter", CollectionUtils.isNotEmpty(context.getProgramLabels()));
        xmlQuery.bind("progLabels", Daos.getSqlInValueFromStringCollection(context.getProgramLabels()));

        // Vessel
        xmlQuery.setGroup("vesselFilter", CollectionUtils.isNotEmpty(context.getVesselIds()));
        xmlQuery.bind("vesselIds", Daos.getSqlInValueFromIntegerCollection(context.getVesselIds()));

        // Trip
        xmlQuery.setGroup("tripFilter", CollectionUtils.isNotEmpty(context.getTripCodes()));
        xmlQuery.bind("tripCodes", Daos.getSqlInValueFromStringCollection(context.getTripCodes()));

        return xmlQuery;
    }

    protected long createSpeciesLengthTable(ExtractionProductVO source, AggregationRdbTripContextVO context) {

        String tableName = context.getSpeciesLengthTableName();
        log.warn("createSpeciesLengthTable... " + tableName);

        XMLQuery xmlQuery = createSpeciesLengthQuery(source, context);

        // aggregate insertion
        execute(xmlQuery);
        long count = countFrom(tableName);

        // Clean row using generic tripFilter
        if (count > 0) {
            count -= cleanRow(tableName, context.getFilter(), RdbSpecification.HL_SHEET_NAME);
        }

        // Add result table to context
        if (count > 0) {
            context.addTableName(tableName, RdbSpecification.HL_SHEET_NAME);
            log.debug(String.format("Species length table: %s rows inserted", count));
        }
        return count;
    }

    protected XMLQuery createSpeciesLengthQuery(ExtractionProductVO source, AggregationRdbTripContextVO context) {
        String rawSpeciesListTableName = source.getTableNameBySheetName(RdbSpecification.SL_SHEET_NAME)
                .orElseThrow(() -> new SumarisTechnicalException(String.format("Missing %s table", RdbSpecification.SL_SHEET_NAME)));
        String rawSpeciesLengthTableName = source.getTableNameBySheetName(RdbSpecification.HL_SHEET_NAME)
                .orElseThrow(() -> new SumarisTechnicalException(String.format("Missing %s table", RdbSpecification.HL_SHEET_NAME)));

        XMLQuery xmlQuery = createXMLQuery(context, "createSpeciesLengthTable");
        xmlQuery.bind("rawSpeciesLengthTableName", rawSpeciesLengthTableName);
        xmlQuery.bind("speciesListTableName", context.getSpeciesListTableName());
        xmlQuery.bind("speciesLengthTableName", context.getSpeciesLengthTableName());
        xmlQuery.bind("speciesLengthMapTableName", context.getSpeciesLengthMapTableName());

        SumarisTableMetadata rawSpeciesListTable = databaseMetadata.getTable(rawSpeciesListTableName);

        boolean hasSampleIds = rawSpeciesListTable.hasColumn(COLUMN_SAMPLE_IDS);
        boolean hasId = !hasSampleIds && rawSpeciesListTable.hasColumn(COLUMN_ID);

        // If missing SAMPLE_IDS, must have an ID column
        if (!hasSampleIds && !hasId) {
          throw new SumarisTechnicalException(String.format("Missing column '%s' or '%s' on table '%s'",
                  COLUMN_SAMPLE_IDS, COLUMN_ID, rawSpeciesListTableName));
        }

        // Enable/Disable group, on DBMS
        xmlQuery.setGroup("hasId", hasId);
        xmlQuery.setGroup("hasSampleIds", hasSampleIds);
        xmlQuery.setGroup("hsqldb-hasSampleIds", hasSampleIds && this.databaseType == DatabaseType.hsqldb);
        xmlQuery.setGroup("oracle-hasSampleIds", hasSampleIds && this.databaseType == DatabaseType.oracle);

        return xmlQuery;
    }

    protected int execute(XMLQuery xmlQuery) {
        return queryUpdate(xmlQuery.getSQLQueryAsString());
    }

    protected long countFrom(String tableName) {
        return extractionTableDao.getRowCount(tableName);
    }

    protected String getQueryFullName(ExtractionContextVO context, String queryName) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(context.getFormatName());
        Preconditions.checkNotNull(context.getFormatVersion());

        return String.format("%s/v%s/aggregation/%s",
                context.getFormatName().toLowerCase(),
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

        SumarisTableMetadata table = databaseMetadata.getTable(tableName);
        Preconditions.checkNotNull(table);

        String whereClauseContent = SumarisTableMetadatas.getSqlWhereClauseContent(table, filter, sheetName, table.getAlias(), true);
        if (StringUtils.isBlank(whereClauseContent)) return 0;

        String deleteQuery = table.getDeleteQuery(String.format("NOT(%s)", whereClauseContent));
        return queryUpdate(deleteQuery);
    }

    protected Map<String, List<String>> analyzeRow(final String tableName, XMLQuery xmlQuery,
                                                   String... includedNumericColumnNames) {
        return analyzeRow(tableName, xmlQuery, includedNumericColumnNames, true);
    }

    protected Map<String, List<String>> analyzeRow(final String tableName, XMLQuery xmlQuery,
                                                   String[] includedNumericColumnNames,
                                                   boolean excludeHiddenColumns) {
        Preconditions.checkNotNull(tableName);
        Preconditions.checkNotNull(xmlQuery);

        Set<String> hiddenColumns = Beans.getSet(xmlQuery.getHiddenColumnNames());

        return Stream.concat(xmlQuery.getNotNumericColumnNames().stream(), Stream.of(includedNumericColumnNames))
                .filter(columnName -> !excludeHiddenColumns || !hiddenColumns.contains(columnName))
                .collect(Collectors.toMap(
                        c -> c,
                        c -> query(String.format("SELECT DISTINCT %s FROM %s where %s IS NOT NULL", c, tableName, c), Object.class)
                                .stream().map(String::valueOf).collect(Collectors.toList())
                        )
                );
    }

    protected Set<String> getSpatialColumnNames(final XMLQuery xmlQuery) {
        return xmlQuery.getVisibleColumnNames()
                .stream()
                .map(c -> c.toLowerCase())
                .filter(SPACE_STRATA::contains)
                .collect(Collectors.toSet());
    }

    protected <R extends C> void dropHiddenColumns(R context) {
        Map<String, Set<String>> hiddenColumns = context.getHiddenColumnNames();
        context.getTableNames().forEach(tableName -> {
            dropHiddenColumns(tableName, hiddenColumns.get(tableName));
            databaseMetadata.clearCache(tableName);
        });
    }

    protected void dropHiddenColumns(final String tableName, Set<String> hiddenColumnNames) {
        Preconditions.checkNotNull(tableName);
        if (CollectionUtils.isEmpty(hiddenColumnNames)) return; // Skip

        hiddenColumnNames.forEach(columnName -> {
            String sql = String.format("ALTER TABLE %s DROP column %s", tableName, columnName);
            queryUpdate(sql);
        });

    }
}

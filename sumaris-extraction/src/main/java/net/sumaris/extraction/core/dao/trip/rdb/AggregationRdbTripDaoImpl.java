package net.sumaris.extraction.core.dao.trip.rdb;

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
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.dao.technical.schema.SumarisTableMetadata;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.model.referential.taxon.TaxonGroupTypeEnum;
import net.sumaris.core.model.technical.extraction.IExtractionType;
import net.sumaris.core.service.administration.programStrategy.ProgramService;
import net.sumaris.core.service.administration.programStrategy.StrategyService;
import net.sumaris.core.util.Beans;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.util.TimeUtils;
import net.sumaris.core.vo.technical.extraction.AggregationStrataVO;
import net.sumaris.core.vo.technical.extraction.IExtractionTypeWithTablesVO;
import net.sumaris.extraction.core.dao.AggregationBaseDaoImpl;
import net.sumaris.extraction.core.dao.technical.Daos;
import net.sumaris.extraction.core.dao.technical.SQLAggregatedFunction;
import net.sumaris.extraction.core.dao.technical.xml.XMLQuery;
import net.sumaris.extraction.core.dao.trip.ExtractionTripDao;
import net.sumaris.extraction.core.specification.data.trip.AggRdbSpecification;
import net.sumaris.extraction.core.specification.data.trip.RdbSpecification;
import net.sumaris.extraction.core.type.AggExtractionTypeEnum;
import net.sumaris.extraction.core.vo.*;
import net.sumaris.extraction.core.vo.trip.rdb.AggregationRdbTripContextVO;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;

import javax.annotation.Nullable;
import javax.persistence.PersistenceException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 */
@Repository("aggregationRdbTripDao")
@Lazy
@Slf4j
public class AggregationRdbTripDaoImpl<
        C extends AggregationRdbTripContextVO,
        F extends ExtractionFilterVO,
        S extends AggregationStrataVO>
        extends AggregationBaseDaoImpl<C, F, S>
        implements
        AggregationRdbTripDao<C, F, S>,
        AggRdbSpecification {

    private static final String HH_TABLE_NAME_PATTERN = TABLE_NAME_PREFIX + HH_SHEET_NAME + "_%s";
    private static final String SL_TABLE_NAME_PATTERN = TABLE_NAME_PREFIX + SL_SHEET_NAME + "_%s";
    private static final String HL_TABLE_NAME_PATTERN = TABLE_NAME_PREFIX + HL_SHEET_NAME + "_%s";
    private static final String CL_TABLE_NAME_PATTERN = TABLE_NAME_PREFIX + CL_SHEET_NAME + "_%s";

    private static final String HL_MAP_TABLE_NAME_PATTERN = TABLE_NAME_PREFIX + HL_SHEET_NAME + "_MAP_%s";

    @Autowired
    protected StrategyService strategyService;

    @Autowired
    protected ProgramService programService;

    @javax.annotation.Resource(name = "extractionRdbTripDao")
    protected ExtractionTripDao<?, ?> extractionRdbTripDao;

    @Override
    public Set<IExtractionType> getManagedTypes() {
        return ImmutableSet.of(AggExtractionTypeEnum.AGG_RDB);
    }

    @Override
    public <R extends C> R aggregate(IExtractionTypeWithTablesVO source, @Nullable F filter, S strata) {
        long rowCount;

        // Init context
        R context = createNewContext();
        context.setTripFilter(extractionRdbTripDao.toTripFilterVO(filter));
        context.setFilter(filter);
        context.setStrata(strata);
        context.setUpdateDate(new Date());
        context.setType(AggExtractionTypeEnum.AGG_RDB);
        context.setTableNamePrefix(TABLE_NAME_PREFIX);

        Long startTime = null;
        if (log.isInfoEnabled()) {
            startTime = System.currentTimeMillis();
            StringBuilder filterInfo = new StringBuilder();
            String filterStr = (filter != null) ? Beans.getStream(filter.getCriteria())
                        .map(ExtractionFilterCriterionVO::toString)
                        .collect(Collectors.joining("\n - ")) : null;
            if (StringUtils.isNotBlank(filterStr)) {
                filterInfo.append("with filter:\n - ").append(filterStr);
            } else {
                filterInfo.append("(without filter)");
            }
            log.info("Starting aggregation #{} (trips)... {}", context.getId(), filterInfo);
        }

        // Fill context table names
        fillContextTableNames(context);

        // Expected sheet name
        String sheetName = filter != null && filter.isPreview() ? filter.getSheetName() : null;

        // -- Execute the aggregation --

        try {
            // If only CL expected: skip station/species aggregation
            if (!CL_SHEET_NAME.equals(sheetName)) {
                // Station
                rowCount = createStationTable(source, context);
                if (rowCount == 0) return context;
                if (sheetName != null && context.hasSheet(sheetName)) return context;

                // Species List
                rowCount = createSpeciesListTable(source, context);
                if (sheetName != null && context.hasSheet(sheetName)) return context;

                // Species length map table
                if (rowCount != 0) {
                    long mapRowCount = createSpeciesLengthMapTable(source, context);
                    // Optional: if -1 then ignore result (= map table not need)
                    if (mapRowCount != -1) rowCount = mapRowCount;
                }

                // Species Length
                if (rowCount != 0) {
                    createSpeciesLengthTable(source, context);
                    if (sheetName != null && context.hasSheet(sheetName)) return context;
                }
            }

            // Landing
            createLandingTable(source, context);
        }
        catch (PersistenceException e) {
            // If error,clean created tables first, then rethrow the exception
            clean(context);

            startTime = null; // Avoid log

            throw e;
        }
        finally {
            if (startTime != null) {
                log.info("Aggregation #{} finished in {}", context.getId(), TimeUtils.printDurationFrom(startTime));
            }

            // Force to set the real subclasses type
            context.setType(getManagedTypes().iterator().next());
        }
        return context;
    }

    @Override
    public AggregationResultVO read(@NonNull String tableName, F filter, @NonNull S strata, Page page) {

        SumarisTableMetadata table = databaseMetadata.getTable(tableName);
        Set<String> groupByColumnNames = getExistingGroupByColumnNames(strata, table);
        Map<String, SQLAggregatedFunction> aggColumns = getAggColumnNames(table, strata);

        ExtractionResultVO rows = readWithAggColumns(tableName, filter,
                groupByColumnNames, aggColumns, page);

        AggregationResultVO result = new AggregationResultVO(rows);

        result.setSpaceStrata(SPATIAL_COLUMNS.stream()
                .filter(table::hasColumn)
                .collect(Collectors.toSet()));
        result.setTimeStrata(TIME_COLUMNS.stream()
                .filter(table::hasColumn)
                .collect(Collectors.toSet()));
        String sheetName = strata.getSheetName() != null ? strata.getSheetName() : filter.getSheetName();
        if (sheetName != null) {
            Set<String> aggColumnNames = getAggColumnNamesBySheetName(sheetName);
            result.setAggStrata(aggColumnNames);
        }

        return result;
    }

    @Override
    public AggregationTechResultVO readByTech(@NonNull String tableName,
                                              @Nullable F filter,
                                              @NonNull S strata,
                                              String sortAttribute, SortDirection direction) {

        Preconditions.checkNotNull(strata.getTechColumnName(), String.format("Missing 'strata.%s'", AggregationStrataVO.Fields.TECH_COLUMN_NAME));
        Preconditions.checkNotNull(strata.getAggColumnName(), String.format("Missing 'strata.%s'", AggregationStrataVO.Fields.AGG_COLUMN_NAME));
        AggregationTechResultVO result = new AggregationTechResultVO();

        SumarisTableMetadata table = databaseMetadata.getTable(tableName);

        Map<String, SQLAggregatedFunction> aggColumns = getAggColumnNames(table, strata);
        Map.Entry<String, SQLAggregatedFunction> aggColumn = aggColumns.entrySet().stream().findFirst()
                .orElseThrow(() -> new IllegalArgumentException(String.format("Missing 'strata.%s'", AggregationStrataVO.Fields.AGG_COLUMN_NAME)));

        result.setData(readAggColumnByTech(table, filter,
                aggColumn.getKey(),
                aggColumn.getValue(),
                strata.getTechColumnName(),
                sortAttribute, direction));

        return result;
    }

    @Override
    public MinMaxVO getTechMinMax(String tableName, F filter, S strata) {
        Preconditions.checkNotNull(strata.getTechColumnName(), String.format("Missing 'strata.%s'", AggregationStrataVO.Fields.TECH_COLUMN_NAME));
        Preconditions.checkNotNull(strata.getAggColumnName(), String.format("Missing 'strata.%s'", AggregationStrataVO.Fields.AGG_COLUMN_NAME));

        SumarisTableMetadata table = databaseMetadata.getTable(tableName);

        Map<String, SQLAggregatedFunction> aggColumns = getAggColumnNames(table, strata);
        Map.Entry<String, SQLAggregatedFunction> aggColumn = aggColumns.entrySet().stream().findFirst()
                .orElseThrow(() -> new IllegalArgumentException(String.format("Missing 'strata.%s'", AggregationStrataVO.Fields.AGG_COLUMN_NAME)));

        Set<String> timeColumnNames = getGroupByTimesColumnNames(strata.getTimeColumnName());

        return getTechMinMax(tableName, filter,
                timeColumnNames,
                aggColumn.getKey(),
                aggColumn.getValue(),
                strata.getTechColumnName());
    }

    /* -- protected methods -- */

    protected <R extends AggregationRdbTripContextVO> R createNewContext() {
        Class<? extends AggregationRdbTripContextVO> contextClass = getContextClass();
        Preconditions.checkNotNull(contextClass);

        try {
            return (R) contextClass.getConstructor().newInstance();
        } catch (Exception e) {
            throw new SumarisTechnicalException("Could not create an instance of context class " + contextClass.getName());
        }
    }

    protected Class<? extends AggregationRdbTripContextVO> getContextClass() {
        return AggregationRdbTripContextVO.class;
    }

    protected void fillContextTableNames(C context) {
        // Set unique table names
        context.setStationTableName(formatTableName(HH_TABLE_NAME_PATTERN, context.getId()));
        context.setSpeciesListTableName(formatTableName(SL_TABLE_NAME_PATTERN, context.getId()));
        context.setSpeciesLengthTableName(formatTableName(HL_TABLE_NAME_PATTERN, context.getId()));
        context.setSpeciesLengthMapTableName(formatTableName(HL_MAP_TABLE_NAME_PATTERN, context.getId()));
        context.setLandingTableName(formatTableName(CL_TABLE_NAME_PATTERN, context.getId()));
    }

    protected long createStationTable(IExtractionTypeWithTablesVO source, C context) {

        String tableName = context.getStationTableName();
        XMLQuery xmlQuery = createStationQuery(source, context);

        // aggregate insertion
        execute(context, xmlQuery);
        long count = countFrom(tableName);

        if (count == 0) {
            context.addRawTableName(tableName);
            return 0;
        }

        // Clean row using filter
        count -= cleanRow(tableName, context.getFilter(), HH_SHEET_NAME);

        // Create an index
        createDefaultIndex(tableName);

        // Analyze row
        Map<String, List<String>> columnValues = null;
        if (context.isEnableAnalyze()) {
            columnValues = analyzeRow(context, tableName, xmlQuery, COLUMN_YEAR);
        }

        // Add result table to context
        context.addTableName(tableName, HH_SHEET_NAME,
                xmlQuery.getHiddenColumnNames(),
                getSpatialColumnNames(xmlQuery),
                columnValues);
        log.debug(String.format("Station table: %s rows inserted", count));

        return count;
    }

    protected XMLQuery createStationQuery(IExtractionTypeWithTablesVO source, C context) {

        String stationTableName = context.getStationTableName();
        String rawTripTableName = source.findTableNameBySheetName(RdbSpecification.TR_SHEET_NAME)
                .orElseThrow(() -> new SumarisTechnicalException(String.format("Missing %s table", RdbSpecification.TR_SHEET_NAME)));
        String rawStationTableName = source.findTableNameBySheetName(RdbSpecification.HH_SHEET_NAME)
                .orElseThrow(() -> new SumarisTechnicalException(String.format("Missing %s table", RdbSpecification.HH_SHEET_NAME)));

        XMLQuery xmlQuery = createXMLQuery(context, "createStationTable");

        xmlQuery.bind("rawTripTableName", rawTripTableName);
        xmlQuery.bind("rawStationTableName", rawStationTableName);
        xmlQuery.bind("stationTableName", stationTableName);

        // Bind column names, because som raw tables can have change this (e.g. Free1 format use 'FISHING_DURATION' instead of 'FISHING_TIME')
        // TODO: find a way to use a replacement map, with regexp, to replace all columns
        // E.g. a map 'columnNameMapping'
        //xmlQuery.bind(AggRdbSpecification.COLUMN_FISHING_TIME.toUpperCase(), context.getFishingTimeColumnName().toUpperCase());

        // Date
        xmlQuery.setGroup("startDateFilter", context.getStartDate() != null);
        xmlQuery.bind("startDate", Daos.getSqlToDate(context.getStartDate()));
        xmlQuery.setGroup("endDateFilter", context.getEndDate() != null);
        xmlQuery.bind("endDate", Daos.getSqlToDate(context.getEndDate()));

        // Program
        xmlQuery.setGroup("programFilter", CollectionUtils.isNotEmpty(context.getProgramLabels()));
        xmlQuery.bind("progLabels", Daos.getSqlInEscapedStrings(context.getProgramLabels()));

        // Vessel
        boolean hasVesselFilter = CollectionUtils.isNotEmpty(context.getVesselIds());
        xmlQuery.setGroup("vesselFilter", hasVesselFilter);
        xmlQuery.bind("vesselIds", Daos.getSqlInNumbers(context.getVesselIds()));

        // Trip
        boolean hasTripFilter = CollectionUtils.isNotEmpty(context.getTripCodes());
        xmlQuery.setGroup("tripFilter", hasTripFilter);
        xmlQuery.bind("tripCodes", Daos.getSqlInEscapedStrings(context.getTripCodes()));

        xmlQuery.setGroup("excludeInvalidStation", true);

        Set<String> groupByColumnNames = getGroupByColumnNames(context.getStrata());
        xmlQuery.setGroup("quarter", groupByColumnNames.contains(COLUMN_QUARTER));
        xmlQuery.setGroup("month", groupByColumnNames.contains(COLUMN_MONTH));
        xmlQuery.setGroup("area", groupByColumnNames.contains(COLUMN_AREA));
        xmlQuery.setGroup("rect", groupByColumnNames.contains(COLUMN_STATISTICAL_RECTANGLE));
        xmlQuery.setGroup("square", groupByColumnNames.contains(COLUMN_SQUARE));

        SumarisTableMetadata rawStationTable = databaseMetadata.getTable(rawStationTableName);
        xmlQuery.setGroup("nationalMetier", rawStationTable.hasColumn(AggRdbSpecification.COLUMN_NATIONAL_METIER));
        xmlQuery.setGroup("euMetierLevel5", rawStationTable.hasColumn(AggRdbSpecification.COLUMN_EU_METIER_LEVEL5));
        xmlQuery.setGroup("euMetierLevel6", rawStationTable.hasColumn(AggRdbSpecification.COLUMN_EU_METIER_LEVEL6));
        xmlQuery.setGroup("gearType", rawStationTable.hasColumn(AggRdbSpecification.COLUMN_GEAR_TYPE));

        return xmlQuery;
    }

    protected Set<String> getGroupByColumnNames(AggregationStrataVO strata) {

        Set<String> result = Sets.newLinkedHashSet();

        if (strata == null) {
            result.addAll(SPATIAL_COLUMNS);
            result.addAll(TIME_COLUMNS);
        }

        else {
            // Process space strata
            String spaceStrata = strata.getSpatialColumnName() != null ? strata.getSpatialColumnName().toLowerCase() : COLUMN_AREA;
            spaceStrata = COLUMN_ALIAS.getOrDefault(spaceStrata, spaceStrata); // Replace alias

            switch (spaceStrata) {
                case COLUMN_SQUARE:
                    result.add(COLUMN_SQUARE);
                case COLUMN_SUB_POLYGON:
                    result.add(COLUMN_SUB_POLYGON);
                case COLUMN_STATISTICAL_RECTANGLE:
                    result.add(COLUMN_STATISTICAL_RECTANGLE);
                case COLUMN_AREA:
                default:
                    result.add(COLUMN_AREA);
            }

            // Time strata
            String timeColumnName = strata.getTimeColumnName() != null ? strata.getTimeColumnName().toLowerCase() : COLUMN_YEAR;
            result.addAll(getGroupByTimesColumnNames(timeColumnName));
        }

        return result;
    }

    protected Set<String> getGroupByTimesColumnNames(String timeColumnName) {
        Set<String> result = Sets.newLinkedHashSet();

        if (timeColumnName == null) {
            result.addAll(TIME_COLUMNS);
        }

        else {
            timeColumnName = COLUMN_ALIAS.getOrDefault(timeColumnName, timeColumnName); // Replace alias

            switch (timeColumnName) {
                case COLUMN_MONTH:
                    result.add(COLUMN_MONTH);
                case COLUMN_QUARTER:
                    result.add(COLUMN_QUARTER);
                case COLUMN_YEAR:
                default:
                    result.add(COLUMN_YEAR);
            }
        }

        return result;
    }

    protected Set<String> getExistingGroupByColumnNames(final AggregationStrataVO strata,
                                                        final SumarisTableMetadata table) {
        return getGroupByColumnNames(strata)
                .stream()
                .filter(table::hasColumn)
                .collect(Collectors.toSet());
    }

    protected Map<String, SQLAggregatedFunction> getAggColumnNames(SumarisTableMetadata table, AggregationStrataVO strata) {
        Map<String, SQLAggregatedFunction> aggColumns = Maps.newHashMap();

        // Read strata agg column
        String aggColumnName = strata.getAggColumnName();
        if (aggColumnName == null) {
            Set<String> aggColumnNames = getAggColumnNamesBySheetName(strata.getSheetName());
            if (CollectionUtils.isNotEmpty(aggColumnNames)) {
                aggColumnName = aggColumnNames.iterator().next();
            }
        }

        // Replace alias
        aggColumnName = COLUMN_ALIAS.getOrDefault(aggColumnName, aggColumnName);

        // Make sure column exists, in table
        aggColumnName = aggColumnName != null && !table.hasColumn(aggColumnName) ? null : aggColumnName;

        if (aggColumnName != null) {
            SQLAggregatedFunction function = strata.getAggFunction() != null ?
                    SQLAggregatedFunction.valueOf(strata.getAggFunction().toUpperCase()) :
                    SQLAggregatedFunction.SUM;
            aggColumns.put(aggColumnName, function);
        }

        return aggColumns;
    }

    protected Set<String> getAggColumnNamesBySheetName(String sheetName) {
        sheetName = sheetName != null ? sheetName : AggRdbSpecification.HH_SHEET_NAME;
        return AGG_COLUMNS_BY_SHEETNAME.get(sheetName);
    }


    protected long createSpeciesListTable(IExtractionTypeWithTablesVO source, C context) {
        String tableName = context.getSpeciesListTableName();
        log.debug(String.format("Aggregation #%s > Creating Species List table...", context.getId()));

        XMLQuery xmlQuery = createSpeciesListQuery(source, context);
        if (xmlQuery == null) return 0;

        // aggregate insertion
        execute(context, xmlQuery);
        long count = countFrom(tableName);

        if (count == 0) {
            context.addRawTableName(tableName);
            return 0;
        }

        // Clean row using generic tripFilter
        count -= cleanRow(tableName, context.getFilter(), SL_SHEET_NAME);

        // Create index
        createDefaultIndex(tableName);

        // Analyze row
        Map<String, List<String>> columnValues = null;
        if (context.isEnableAnalyze()) {
            columnValues = analyzeRow(context, tableName, xmlQuery, COLUMN_YEAR);
        }

        // Add result table to context
        context.addTableName(tableName, SL_SHEET_NAME,
                xmlQuery.getHiddenColumnNames(),
                getSpatialColumnNames(xmlQuery),
                columnValues);
        log.debug(String.format("Species list table: %s rows inserted", count));

        return count;
    }

    protected XMLQuery createSpeciesListQuery(IExtractionTypeWithTablesVO source, C context) {
        String rawSpeciesListTableName = source.findTableNameBySheetName(RdbSpecification.SL_SHEET_NAME)
                .orElse(null);
        String stationTableName = context.getStationTableName();

        if (rawSpeciesListTableName == null) return null;

        XMLQuery xmlQuery = createXMLQuery(context, "createSpeciesListTable");

        xmlQuery.bind("rawSpeciesListTableName", rawSpeciesListTableName);
        xmlQuery.bind("stationTableName", stationTableName);
        xmlQuery.bind("speciesListTableName", context.getSpeciesListTableName());

        xmlQuery.bind("speciesTaxonGroupTypeId", String.valueOf(TaxonGroupTypeEnum.FAO.getId()));

        // Enable/Disable group, on optional columns
        SumarisTableMetadata stationTable = databaseMetadata.getTable(stationTableName);
        xmlQuery.setGroup("month", stationTable.hasColumn(AggRdbSpecification.COLUMN_MONTH));
        xmlQuery.setGroup("quarter", stationTable.hasColumn(AggRdbSpecification.COLUMN_QUARTER));
        xmlQuery.setGroup("area", stationTable.hasColumn(AggRdbSpecification.COLUMN_AREA));
        xmlQuery.setGroup("rect", stationTable.hasColumn(AggRdbSpecification.COLUMN_STATISTICAL_RECTANGLE));
        xmlQuery.setGroup("square", stationTable.hasColumn(AggRdbSpecification.COLUMN_SQUARE));
        xmlQuery.setGroup("nationalMetier", stationTable.hasColumn(AggRdbSpecification.COLUMN_NATIONAL_METIER));
        xmlQuery.setGroup("euMetierLevel5", stationTable.hasColumn(AggRdbSpecification.COLUMN_EU_METIER_LEVEL5));
        xmlQuery.setGroup("euMetierLevel6", stationTable.hasColumn(AggRdbSpecification.COLUMN_EU_METIER_LEVEL6));
        xmlQuery.setGroup("gearType", stationTable.hasColumn(AggRdbSpecification.COLUMN_GEAR_TYPE));

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
    protected long createSpeciesLengthMapTable(IExtractionTypeWithTablesVO source, C context) {

        String tableName = context.getSpeciesLengthMapTableName();
        log.debug(String.format("Aggregation #%s > Creating Species Length Map table...", context.getId()));

        XMLQuery xmlQuery = createSpeciesLengthMapQuery(source, context);
        if (xmlQuery == null) return -1; // Skip

        // Create the table
        execute(context, xmlQuery);

        // Create index on SL_ID
        createIndex(tableName, tableName + "_IDX", ImmutableList.of("SL_ID"), false);

        long count = countFrom(tableName);

        if (count > 0) log.debug(String.format("Species length map table: %s rows inserted", count));

        // Add result table to context
        context.addRawTableName(tableName);

        return count;
    }

    protected XMLQuery createSpeciesLengthMapQuery(IExtractionTypeWithTablesVO source, C context) {
        String rawSpeciesListTableName = source.findTableNameBySheetName(RdbSpecification.SL_SHEET_NAME)
                .orElse(null);
        String rawSpeciesLengthTableName = source.findTableNameBySheetName(RdbSpecification.HL_SHEET_NAME)
                .orElse(null);
        if (rawSpeciesListTableName == null || rawSpeciesLengthTableName == null) return null; // Skip

        // Skip if SL.SAMPLE_IDS exists
        SumarisTableMetadata rawSpeciesListTable = databaseMetadata.getTable(rawSpeciesListTableName);
        if (rawSpeciesListTable.hasColumn(COLUMN_SAMPLE_IDS)) return null; // Skip (map table not need)

        // Check column SL.ID exists (e.g when raw tables comes from the 'P01_RDB' product)
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
        xmlQuery.bind("progLabels", Daos.getSqlInEscapedStrings(context.getProgramLabels()));

        // Vessel
        xmlQuery.setGroup("vesselFilter", CollectionUtils.isNotEmpty(context.getVesselIds()));
        xmlQuery.bind("vesselIds", Daos.getSqlInNumbers(context.getVesselIds()));

        // Trip
        xmlQuery.setGroup("tripFilter", CollectionUtils.isNotEmpty(context.getTripCodes()));
        xmlQuery.bind("tripCodes", Daos.getSqlInEscapedStrings(context.getTripCodes()));

        return xmlQuery;
    }

    protected long createSpeciesLengthTable(IExtractionTypeWithTablesVO source, C context) {

        String tableName = context.getSpeciesLengthTableName();
        log.debug(String.format("Aggregation #%s > Creating Species Length table...", context.getId()));

        XMLQuery xmlQuery = createSpeciesLengthQuery(source, context);
        if (xmlQuery == null) return 0; // Skip

        // aggregate insertion
        execute(context, xmlQuery);
        long count = countFrom(tableName);

        if (count == 0) {
            context.addRawTableName(tableName);
            return 0;
        }

        // Clean row using generic tripFilter
        count -= cleanRow(tableName, context.getFilter(), RdbSpecification.HL_SHEET_NAME);

        // Create index
        createDefaultIndex(tableName);

        // Analyze row
        Map<String, List<String>> columnValues = null;
        if (context.isEnableAnalyze() && count > 0) {
            columnValues = analyzeRow(context, tableName, xmlQuery, COLUMN_YEAR);
        }

        // Add result table to context
        context.addTableName(tableName, HL_SHEET_NAME,
                xmlQuery.getHiddenColumnNames(),
                getSpatialColumnNames(xmlQuery),
                columnValues);
        log.debug(String.format("Species length table: %s rows inserted", count));

        return count;
    }

    protected XMLQuery createSpeciesLengthQuery(IExtractionTypeWithTablesVO source, C context) {
        String rawSpeciesListTableName = source.findTableNameBySheetName(RdbSpecification.SL_SHEET_NAME)
                .orElse(null);
        String rawSpeciesLengthTableName = source.findTableNameBySheetName(RdbSpecification.HL_SHEET_NAME)
            .orElse(null);

        // No species length raw data: skip
        if (rawSpeciesLengthTableName == null || rawSpeciesLengthTableName == null) return null;

        String stationTableName = context.getStationTableName();

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
        xmlQuery.setGroup("hasId", hasId);
        xmlQuery.setGroup("hasSampleIds", hasSampleIds);

        // Enable/disable columns depending on station existing columns
        SumarisTableMetadata stationTable = databaseMetadata.getTable(stationTableName);
        xmlQuery.setGroup("month", stationTable.hasColumn(AggRdbSpecification.COLUMN_MONTH));
        xmlQuery.setGroup("quarter", stationTable.hasColumn(AggRdbSpecification.COLUMN_QUARTER));
        xmlQuery.setGroup("area", stationTable.hasColumn(AggRdbSpecification.COLUMN_AREA));
        xmlQuery.setGroup("rect", stationTable.hasColumn(AggRdbSpecification.COLUMN_STATISTICAL_RECTANGLE));
        xmlQuery.setGroup("square", stationTable.hasColumn(AggRdbSpecification.COLUMN_SQUARE));
        xmlQuery.setGroup("nationalMetier", stationTable.hasColumn(AggRdbSpecification.COLUMN_NATIONAL_METIER));
        xmlQuery.setGroup("euMetierLevel5", stationTable.hasColumn(AggRdbSpecification.COLUMN_EU_METIER_LEVEL5));
        xmlQuery.setGroup("euMetierLevel6", stationTable.hasColumn(AggRdbSpecification.COLUMN_EU_METIER_LEVEL6));
        xmlQuery.setGroup("gearType", stationTable.hasColumn(AggRdbSpecification.COLUMN_GEAR_TYPE));


        return xmlQuery;
    }


    protected long createLandingTable(IExtractionTypeWithTablesVO source, C context) {
        String tableName = context.getLandingTableName();
        log.debug(String.format("Aggregation #%s > Creating Landing table...", context.getId()));

        XMLQuery xmlQuery = createLandingQuery(source, context);
        if (xmlQuery == null) return 0; // Skip

        // aggregate insertion
        execute(context, xmlQuery);
        long count = countFrom(tableName);

        if (count == 0) {
            context.addRawTableName(tableName);
            return 0;
        }

        // Clean row using generic tripFilter
        count -= cleanRow(tableName, context.getFilter(), CL_SHEET_NAME);

        // Create index
        createDefaultIndex(tableName);

        // Analyze row
        Map<String, List<String>> columnValues = null;
        if (context.isEnableAnalyze()) {
            columnValues = analyzeRow(context, tableName, xmlQuery, COLUMN_YEAR);
        }

        // Add result table to context
        context.addTableName(tableName, CL_SHEET_NAME,
                xmlQuery.getHiddenColumnNames(),
                getSpatialColumnNames(xmlQuery),
                columnValues);
        log.debug(String.format("Landing table: %s rows inserted", count));

        return count;
    }

    protected XMLQuery createLandingQuery(IExtractionTypeWithTablesVO source, C context) {
        String rawLandingTableName = source.findTableNameBySheetName(RdbSpecification.CL_SHEET_NAME)
                .orElse(null);
        if (rawLandingTableName == null) return null; // Skip
        String landingTableName = context.getLandingTableName();

        XMLQuery xmlQuery = createXMLQuery(context, "createLandingTable");

        xmlQuery.bind("rawLandingTableName", rawLandingTableName);
        xmlQuery.bind("landingTableName", landingTableName);

        xmlQuery.bind("speciesTaxonGroupTypeId", String.valueOf(TaxonGroupTypeEnum.FAO.getId()));

        // Enable/Disable group, on optional columns
        SumarisTableMetadata rawLandingTable = databaseMetadata.getTable(rawLandingTableName);
        Set<String> groupByColumnNames = getExistingGroupByColumnNames(context.getStrata(), rawLandingTable);
        xmlQuery.setGroup("year", groupByColumnNames.contains(AggRdbSpecification.COLUMN_YEAR));
        xmlQuery.setGroup("month", groupByColumnNames.contains(AggRdbSpecification.COLUMN_MONTH));
        xmlQuery.setGroup("quarter", groupByColumnNames.contains(AggRdbSpecification.COLUMN_QUARTER));
        xmlQuery.setGroup("area", groupByColumnNames.contains(AggRdbSpecification.COLUMN_AREA));
        xmlQuery.setGroup("rect", groupByColumnNames.contains(AggRdbSpecification.COLUMN_STATISTICAL_RECTANGLE));
        xmlQuery.setGroup("subPolygon", groupByColumnNames.contains(AggRdbSpecification.COLUMN_SUB_POLYGON));

        xmlQuery.setGroup("nationalMetier", rawLandingTable.hasColumn(AggRdbSpecification.COLUMN_NATIONAL_METIER));
        xmlQuery.setGroup("euMetierLevel5", rawLandingTable.hasColumn(AggRdbSpecification.COLUMN_EU_METIER_LEVEL5));
        xmlQuery.setGroup("euMetierLevel6", rawLandingTable.hasColumn(AggRdbSpecification.COLUMN_EU_METIER_LEVEL6));

        return xmlQuery;
    }

    protected int execute(C context, XMLQuery xmlQuery) {
        String sqlQuery = xmlQuery.getSQLQueryAsString();

        // Do column names replacement (e.g. see FREE extraction)
        sqlQuery = Daos.sqlReplaceColumnNames(sqlQuery, context.getColumnNamesMapping(), false);

        return queryUpdate(sqlQuery);
    }


    protected Map<String, List<String>> analyzeRow(
        final C context,
        final String tableName,
        XMLQuery xmlQuery,
        String... includedNumericColumnNames) {
        return analyzeRow(context, tableName, xmlQuery, includedNumericColumnNames, true);
    }

    protected Map<String, List<String>> analyzeRow(
        final C context,
        final String tableName, XMLQuery xmlQuery,
        String[] includedNumericColumnNames,
        boolean excludeHiddenColumns) {

        Preconditions.checkNotNull(tableName);
        Preconditions.checkNotNull(xmlQuery);

        Set<String> hiddenColumns = Beans.getSet(xmlQuery.getHiddenColumnNames());

        return Stream.concat(xmlQuery.getNotNumericColumnNames().stream(), Stream.of(includedNumericColumnNames))
                .filter(columnName -> !excludeHiddenColumns || !hiddenColumns.contains(columnName))
                .filter(columnName -> !columnName.contains("&")) // Skip injected pmfm columns
                .collect(Collectors.toMap(
                        c -> c,
                        c -> query(
                            Daos.sqlReplaceColumnNames(
                                // WArn: make
                                String.format("SELECT DISTINCT T.%s FROM %s T where T.%s IS NOT NULL", c, tableName, c),
                                context.getColumnNamesMapping(), false),
                            Object.class)
                                .stream()
                                .map(String::valueOf)
                                .collect(Collectors.toList())
                    )
                );
    }

    protected Set<String> getSpatialColumnNames(final XMLQuery xmlQuery) {
        return xmlQuery.getVisibleColumnNames()
                .stream()
                .map(String::toLowerCase)
                .filter(SPATIAL_COLUMNS::contains)
                .collect(Collectors.toSet());
    }

    /**
     * Create common index on a AGG_ table
     */
    protected void createDefaultIndex(String tableName) {
        createIndex(tableName,
                tableName + "_IDX",
                new ImmutableList.Builder<String>()
                        .addAll(TIME_COLUMNS)
                        .addAll(SPATIAL_COLUMNS)
                        .build(),
                false);
    }

    protected void createIndex(String tableName,
                               String indexName,
                               Collection<String> columnNames,
                               boolean isUnique) {

        SumarisTableMetadata table = databaseMetadata.getTable(tableName);

        // Filter on existing columns
        columnNames = columnNames.stream()
                .filter(table::hasColumn)
                .collect(Collectors.toList());

        // If has columns to index: create the index
        if (CollectionUtils.isNotEmpty(columnNames)) {
            super.createIndex(tableName,
                    indexName,
                    columnNames,
                    isUnique
            );
        }
        else {
            log.debug("Skipping index {} on table {}: no columns to index", indexName, tableName);
        }

    }
}

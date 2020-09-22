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

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.dao.technical.schema.SumarisDatabaseMetadata;
import net.sumaris.core.dao.technical.schema.SumarisTableMetadata;
import net.sumaris.core.exception.SumarisTechnicalException;
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
import net.sumaris.core.model.technical.extraction.rdb.ProductRdbStation;
import net.sumaris.core.service.administration.programStrategy.ProgramService;
import net.sumaris.core.service.administration.programStrategy.StrategyService;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.technical.extraction.ExtractionProductVO;
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

    private static final String TR_TABLE_NAME_PATTERN = TABLE_NAME_PREFIX + TR_SHEET_NAME + "_%s";
    private static final String HH_TABLE_NAME_PATTERN = TABLE_NAME_PREFIX + HH_SHEET_NAME + "_%s";
    private static final String SL_TABLE_NAME_PATTERN = TABLE_NAME_PREFIX + SL_SHEET_NAME + "_%s";



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
        context.setFormatVersion(AggRdbSpecification.VERSION_1_4);
        context.setId(System.currentTimeMillis());

        // Compute table names
        context.setTripTableName(String.format(TR_TABLE_NAME_PATTERN, context.getId()));
        context.setStationTableName(String.format(HH_TABLE_NAME_PATTERN, context.getId()));
        context.setSpeciesListTableName(String.format(SL_TABLE_NAME_PATTERN, context.getId()));

        // Expected sheet name
        String sheetName = filter != null && filter.isPreview() ? filter.getSheetName() : null;

        // Trip
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

        // Clean row using generic tripFilter
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

        String rawStationTableName = source.getTableNameBySheetName(RdbSpecification.HH_SHEET_NAME)
                .orElseThrow(() -> new SumarisTechnicalException(String.format("Missing %s table", RdbSpecification.HH_SHEET_NAME)));
        String rawTripTableName = source.getTableNameBySheetName(RdbSpecification.TR_SHEET_NAME)
                .orElseThrow(() -> new SumarisTechnicalException(String.format("Missing %s table", RdbSpecification.TR_SHEET_NAME)));

        SumarisTableMetadata rawStationTable = databaseMetadata.getTable(rawStationTableName);

        XMLQuery xmlQuery = createXMLQuery(context, "createStationTable");

        xmlQuery.bind("rawTripTableName", rawTripTableName);
        xmlQuery.bind("rawStationTableName", rawStationTableName);
        xmlQuery.bind("stationTableName", context.getStationTableName());

        xmlQuery.setGroup("gearType", rawStationTable.getColumnMetadata(ProductRdbStation.COLUMN_GEAR_TYPE) != null);

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
        else {
            context.addRawTableName(context.getSpeciesListTableName());
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
            count -= cleanRow(context.getSpeciesLengthTableName(), context.getFilter(), RdbSpecification.HL_SHEET_NAME);
        }

        // Add result table to context
        if (count > 0) {
            context.addTableName(context.getSpeciesLengthTableName(), RdbSpecification.HL_SHEET_NAME);
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
        xmlQuery.bind("lengthTotalCmPmfmId", String.valueOf(PmfmEnum.LENGTH_TOTAL_CM.getId()));

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


    protected Map<String, List<String>> analyzeRow(final String tableName, XMLQuery xmlQuery, String... includedNumericColumnNames) {
        Preconditions.checkNotNull(tableName);
        Preconditions.checkNotNull(xmlQuery);

        return Stream.concat(xmlQuery.getNotNumericColumnNames().stream(), Stream.of(includedNumericColumnNames))
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
}

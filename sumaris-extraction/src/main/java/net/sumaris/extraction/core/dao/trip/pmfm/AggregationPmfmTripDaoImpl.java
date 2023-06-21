package net.sumaris.extraction.core.dao.trip.pmfm;

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
import com.google.common.collect.ImmutableSet;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.technical.schema.SumarisColumnMetadata;
import net.sumaris.core.dao.technical.schema.SumarisTableMetadata;
import net.sumaris.core.model.technical.extraction.IExtractionType;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.technical.extraction.IExtractionTypeWithTablesVO;
import net.sumaris.extraction.core.dao.technical.schema.SumarisTableUtils;
import net.sumaris.xml.query.XMLQuery;
import net.sumaris.extraction.core.dao.trip.rdb.AggregationRdbTripDaoImpl;
import net.sumaris.extraction.core.specification.data.trip.AggRdbSpecification;
import net.sumaris.extraction.core.specification.data.trip.PmfmTripSpecification;
import net.sumaris.extraction.core.type.AggExtractionTypeEnum;
import net.sumaris.extraction.core.specification.data.trip.AggPmfmTripSpecification;
import net.sumaris.extraction.core.specification.data.trip.AggSurvivalTestSpecification;
import net.sumaris.extraction.core.vo.ExtractionFilterVO;
import net.sumaris.extraction.core.vo.trip.pmfm.AggregationPmfmTripContextVO;
import net.sumaris.extraction.core.vo.trip.rdb.AggregationRdbTripContextVO;
import net.sumaris.core.vo.technical.extraction.AggregationStrataVO;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;

import javax.annotation.Nullable;
import javax.persistence.PersistenceException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 */
@Repository("aggregationPmfmTripDao")
@Lazy
@Slf4j
public class AggregationPmfmTripDaoImpl<
    C extends AggregationPmfmTripContextVO,
    F extends ExtractionFilterVO,
    S extends AggregationStrataVO>
    extends AggregationRdbTripDaoImpl<C, F, S>
    implements AggPmfmTripSpecification {


    private static final String ST_TABLE_NAME_PATTERN = TABLE_NAME_PREFIX + ST_SHEET_NAME + "_%s";
    private static final String RL_TABLE_NAME_PATTERN = TABLE_NAME_PREFIX + RL_SHEET_NAME + "_%s";
    @Override
    public Set<IExtractionType> getManagedTypes() {
        return ImmutableSet.of(AggExtractionTypeEnum.AGG_PMFM_TRIP);
    }

    @Override
    public <R extends C> R aggregate(IExtractionTypeWithTablesVO source, @Nullable F filter, S strata) {
        R context = super.aggregate(source, filter, strata);

        context.setType(AggExtractionTypeEnum.AGG_PMFM_TRIP);
        context.setSampleTableName(formatTableName(ST_TABLE_NAME_PATTERN, context.getId()));
        context.setReleaseTableName(formatTableName(RL_TABLE_NAME_PATTERN, context.getId()));

        // Stop here, if sheet already filled
        String sheetName = filter != null && filter.isPreview() ? filter.getSheetName() : null;
        if (sheetName != null && context.hasSheet(sheetName)) return context;

        try {
            // If only CL expected: skip station/species aggregation
            if (!CL_SHEET_NAME.equals(sheetName)) {

                // Restore the previous (HH) row count
                long rowCount = countFrom(context.getStationTableName());

                // Sample table
                if (rowCount != 0) {
                    rowCount = createSampleTable(source, context);
                    if (sheetName != null && context.hasSheet(sheetName)) return context;
                }

                // Release table (=sub sample)
                if (rowCount != 0) {
                    rowCount = createReleaseTable(source, context);
                    if (sheetName != null && context.hasSheet(sheetName)) return context;
                }

                return context;
            }
        }
         catch (PersistenceException e) {
            // If error,clean created tables first, then rethrow the exception
            clean(context);
            throw e;
        }

        return context;
    }

    /* -- protected methods -- */

    @Override
    protected Class<? extends AggregationRdbTripContextVO> getContextClass() {
        return AggregationPmfmTripContextVO.class;
    }

    @Override
    protected XMLQuery createSpeciesLengthQuery(IExtractionTypeWithTablesVO source, C context) {
        XMLQuery xmlQuery = super.createSpeciesLengthQuery(source, context);

        // Special case for COST format:

        // - Hide sex columns, then replace by a new column
        xmlQuery.setGroup("sex", false);

        xmlQuery.injectQuery(getXMLQueryURL(context, "injectionSpeciesLengthTable"));

        return xmlQuery;

    }

    protected String getQueryFullName(C context, String queryName) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(context.getVersion());

        switch (queryName) {
            case "injectionSpeciesLengthTable":
            case "injectionPmfm":
                return getQueryFullName(AggPmfmTripSpecification.FORMAT, AggPmfmTripSpecification.VERSION_1_0, queryName);
            default:
                return super.getQueryFullName(context, queryName);
        }
    }

    protected long createSampleTable(IExtractionTypeWithTablesVO source, C context) {

        String tableName = context.getSampleTableName();
        log.debug(String.format("Aggregation #%s > Creating samples table...", context.getId()));

        XMLQuery xmlQuery = createSampleQuery(source, context);
        if (xmlQuery == null) return -1;

        // aggregate insertion
        execute(context, xmlQuery);
        long count = countFrom(tableName);

        // Clean row using generic tripFilter
        if (count == 0) {
            context.addRawTableName(tableName);
            return 0;
        }

        count -= cleanRow(tableName, context.getFilter(), ST_SHEET_NAME);

        // Analyze row
        Map<String, List<String>> columnValues = null;
        if (context.isEnableAnalyze()) {
            columnValues = analyzeRow(context, tableName, xmlQuery, COLUMN_YEAR);
        }

        // Add result table to context
        context.addTableName(tableName, ST_SHEET_NAME,
                xmlQuery.getHiddenColumnNames(),
                getSpatialColumnNames(xmlQuery),
                columnValues);
        log.debug(String.format("Aggregation #%s > Survival test table: %s rows inserted", context.getId(), count));

        return count;
    }

    protected XMLQuery createSampleQuery(IExtractionTypeWithTablesVO source, C context) {
        String rawSampleTableName = source.findTableNameBySheetName(PmfmTripSpecification.ST_SHEET_NAME)
                .orElse(null);
        if (rawSampleTableName == null) return null; // Skip

        String stationTableName = context.getStationTableName();
        String tableName = context.getSampleTableName();

        XMLQuery xmlQuery = createXMLQuery(context, "createSampleTable");
        xmlQuery.bind("rawSampleTableName", rawSampleTableName);
        xmlQuery.bind("stationTableName", stationTableName);
        xmlQuery.bind("sampleTableName", tableName);

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

        // Add PMFM columns (should be AFTER column "individual_count")
        SumarisTableMetadata rawSampleTable = databaseMetadata.getTable(rawSampleTableName);
        List<String> columnNames = rawSampleTable.getColumnNames().stream()
            .map(String::toLowerCase)
            .collect(Collectors.toList());
        int lastStaticColumnIndex = columnNames.indexOf(AggPmfmTripSpecification.COLUMN_INDIVIDUAL_COUNT);

        // If Pmfm columns exists: inject all
        if (lastStaticColumnIndex != -1 && lastStaticColumnIndex < columnNames.size() - 1) {
            injectPmfmColumns(context, xmlQuery,
                "ST",
                rawSampleTable,
                columnNames.subList(
                    lastStaticColumnIndex + 1,
                    columnNames.size() - 1)
            );
        }

        return xmlQuery;
    }

    protected long createReleaseTable(IExtractionTypeWithTablesVO source, C context) {

        String tableName = context.getReleaseTableName();
        log.debug(String.format("Aggregation #%s > Creating releases table...", context.getId()));

        XMLQuery xmlQuery = createReleaseQuery(source, context);
        if (xmlQuery == null) return -1; // Skip

        // aggregate insertion
        execute(context, xmlQuery);
        long count = countFrom(tableName);

        // Clean row using generic tripFilter
        if (count == 0) {
            context.addRawTableName(tableName);
            return 0;
        }

        count -= cleanRow(tableName, context.getFilter(), RL_SHEET_NAME);

        // Analyze row
        Map<String, List<String>> columnValues = null;
        if (context.isEnableAnalyze()) {
            columnValues = analyzeRow(context, tableName, xmlQuery, COLUMN_YEAR);
        }

        // Add result table to context
        context.addTableName(tableName, RL_SHEET_NAME,
                xmlQuery.getHiddenColumnNames(),
                getSpatialColumnNames(xmlQuery),
                columnValues);
        log.debug(String.format("Aggregation #%s > Release table: %s rows inserted", context.getId(), count));

        return count;
    }

    protected XMLQuery createReleaseQuery(IExtractionTypeWithTablesVO source, C context) {
        String rawReleaseTableName = source.findTableNameBySheetName(AggSurvivalTestSpecification.RL_SHEET_NAME)
                .orElse(null);
        if (rawReleaseTableName == null) return null; // Skip

        String stationTableName = context.getStationTableName();
        String tableName = context.getReleaseTableName();

        XMLQuery xmlQuery = createXMLQuery(context, "createReleaseTable");
        xmlQuery.bind("rawReleaseTableName", rawReleaseTableName);
        xmlQuery.bind("stationTableName", stationTableName);
        xmlQuery.bind("releaseTableName", tableName);

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

    protected void injectPmfmColumns(C context,
                                     XMLQuery xmlQuery,
                                     String tableAlias,
                                     SumarisTableMetadata rawTable,
                                     List<String> columnNames){
        Preconditions.checkArgument(CollectionUtils.isNotEmpty(columnNames));
        injectPmfmColumns(context,
            xmlQuery,
            tableAlias,
            columnNames.stream().map(rawTable::getColumnMetadata)
        );
    }
    protected void injectPmfmColumns(C context,
                                     @NonNull final XMLQuery xmlQuery,
                                     @NonNull final String tableAlias,
                                     @NonNull Stream<SumarisColumnMetadata> columns){
        final URL injectionQuery = getXMLQueryURL(context, "injectionPmfm");
        columns
            .forEach(column -> {
                String columnName = column.getName();
                String aliasedColumnName = SumarisTableUtils.getAliasedColumnName(tableAlias, column.getEscapedName());
                boolean isNumericColumn = SumarisTableUtils.isNumericColumn(column);
                String suffix = StringUtils.capitalize(StringUtils.underscoreToChangeCase(columnName));
                xmlQuery.injectQuery(injectionQuery, "%suffix%", suffix);
                xmlQuery.bind("columnAlias" + suffix, columnName);
                xmlQuery.bind("columnName" + suffix, aliasedColumnName);
                xmlQuery.setGroup("number" + suffix, isNumericColumn);
                xmlQuery.setGroup("text" + suffix, !isNumericColumn);
            });
    }
}

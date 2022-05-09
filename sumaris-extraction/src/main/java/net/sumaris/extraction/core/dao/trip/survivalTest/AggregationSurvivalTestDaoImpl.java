package net.sumaris.extraction.core.dao.trip.survivalTest;

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
import net.sumaris.core.dao.technical.schema.SumarisTableMetadata;
import net.sumaris.extraction.core.dao.technical.xml.XMLQuery;
import net.sumaris.extraction.core.dao.trip.rdb.AggregationRdbTripDaoImpl;
import net.sumaris.extraction.core.type.AggExtractionTypeEnum;
import net.sumaris.extraction.core.specification.data.trip.AggRdbSpecification;
import net.sumaris.extraction.core.specification.data.trip.AggSurvivalTestSpecification;
import net.sumaris.extraction.core.specification.data.trip.SurvivalTestSpecification;
import net.sumaris.extraction.core.vo.ExtractionFilterVO;
import net.sumaris.extraction.core.vo.trip.rdb.AggregationRdbTripContextVO;
import net.sumaris.extraction.core.vo.trip.survivalTest.AggregationSurvivalTestContextVO;
import net.sumaris.core.vo.technical.extraction.AggregationStrataVO;
import net.sumaris.core.vo.technical.extraction.ExtractionProductVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;

import javax.annotation.Nullable;
import javax.persistence.PersistenceException;
import java.util.List;
import java.util.Map;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 */
@Repository("aggregationSurvivalTestDao")
@Lazy
public class AggregationSurvivalTestDaoImpl<C extends AggregationSurvivalTestContextVO, F extends ExtractionFilterVO,
        S extends AggregationStrataVO>
        extends AggregationRdbTripDaoImpl<C, F, S>
        implements AggSurvivalTestSpecification {

    private static final Logger log = LoggerFactory.getLogger(AggregationSurvivalTestDaoImpl.class);

    private static final String ST_TABLE_NAME_PATTERN = TABLE_NAME_PREFIX + ST_SHEET_NAME + "_%s";
    private static final String RL_TABLE_NAME_PATTERN = TABLE_NAME_PREFIX + RL_SHEET_NAME + "_%s";

    @Override
    public AggExtractionTypeEnum getFormat() {
        return AggExtractionTypeEnum.AGG_SURVIVAL_TEST;
    }

    @Override
    public <R extends C> R aggregate(ExtractionProductVO source, @Nullable F filter, S strata) {
        // Execute inherited aggregation
        R context = super.aggregate(source, filter, strata);

        // Override some context properties
        context.setType(AggExtractionTypeEnum.AGG_SURVIVAL_TEST);
        context.setSurvivalTestTableName(formatTableName(ST_TABLE_NAME_PATTERN, context.getId()));
        context.setReleaseTableName(formatTableName(RL_TABLE_NAME_PATTERN, context.getId()));

        // Stop here, if sheet already filled
        String sheetName = filter != null && filter.isPreview() ? filter.getSheetName() : null;
        if (sheetName != null && context.hasSheet(sheetName)) return context;

        try {
            // Survival test table
            long rowCount = createSurvivalTestTable(source, context);
            if (rowCount == 0) return context;
            if (sheetName != null && context.hasSheet(sheetName)) return context;

            // Release table
            createReleaseTable(source, context);
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
        return AggregationSurvivalTestContextVO.class;
    }

    protected String getQueryFullName(C context, String queryName) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(context.getVersion());

        switch (queryName) {
            case "injectionStationTable":
            case "injectionSpeciesLengthTable":
                return getQueryFullName(AggSurvivalTestSpecification.FORMAT, AggSurvivalTestSpecification.VERSION_1_0, queryName);
            default:
                return super.getQueryFullName(context, queryName);
        }
    }

    @Override
    protected XMLQuery createStationQuery(ExtractionProductVO source, C context) {
        XMLQuery xmlQuery = super.createStationQuery(source, context);

        // Inject specific select clause
        xmlQuery.injectQuery(getXMLQueryURL(context, "injectionStationTable"));

        return xmlQuery;
    }

    @Override
    protected XMLQuery createSpeciesLengthQuery(ExtractionProductVO source, C context) {
        XMLQuery xmlQuery = super.createSpeciesLengthQuery(source, context);

        // Inject specific select clause
        xmlQuery.injectQuery(getXMLQueryURL(context, "injectionSpeciesLengthTable"));

        return xmlQuery;
    }

    protected long createSurvivalTestTable(ExtractionProductVO source, C context) {

        String tableName = context.getSurvivalTestTableName();
        log.debug(String.format("Aggregation #%s > Creating survival tests table...", context.getId()));

        XMLQuery xmlQuery = createSurvivalTestQuery(source, context);
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

    protected XMLQuery createSurvivalTestQuery(ExtractionProductVO source, C context) {
        String rawSurvivalTestTableName = source.findTableNameBySheetName(SurvivalTestSpecification.ST_SHEET_NAME)
                .orElse(null);
        if (rawSurvivalTestTableName == null) return null; // Skip

        String stationTableName = context.getStationTableName();
        String tableName = context.getSurvivalTestTableName();

        XMLQuery xmlQuery = createXMLQuery(context, "createSurvivalTestTable");
        xmlQuery.bind("rawSurvivalTestTableName", rawSurvivalTestTableName);
        xmlQuery.bind("stationTableName", stationTableName);
        xmlQuery.bind("survivalTestTableName", tableName);

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

    protected long createReleaseTable(ExtractionProductVO source, C context) {

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

    protected XMLQuery createReleaseQuery(ExtractionProductVO source, C context) {
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
}

/*
 * #%L
 * SUMARiS
 * %%
 * Copyright (C) 2019 SUMARiS Consortium
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

package net.sumaris.core.extraction.dao.trip.free2;

import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.technical.schema.SumarisDatabaseMetadata;
import net.sumaris.core.extraction.dao.technical.Daos;
import net.sumaris.core.extraction.dao.technical.XMLQuery;
import net.sumaris.core.extraction.dao.technical.table.ExtractionTableDao;
import net.sumaris.core.extraction.dao.trip.rdb.ExtractionRdbTripDaoImpl;
import net.sumaris.core.extraction.format.LiveFormatEnum;
import net.sumaris.core.extraction.specification.data.trip.Free2Specification;
import net.sumaris.core.extraction.vo.ExtractionFilterVO;
import net.sumaris.core.extraction.vo.trip.free2.ExtractionFree2ContextVO;
import net.sumaris.core.model.referential.pmfm.PmfmEnum;
import net.sumaris.core.model.referential.pmfm.QualitativeValueEnum;
import net.sumaris.core.model.referential.pmfm.UnitEnum;
import net.sumaris.core.service.administration.programStrategy.ProgramService;
import net.sumaris.core.service.administration.programStrategy.StrategyService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Repository;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 */
@Repository("extractionFree2TripDao")
@Lazy
@Slf4j
public class ExtractionFree2TripDaoImpl<C extends ExtractionFree2ContextVO, F extends ExtractionFilterVO>
        extends ExtractionRdbTripDaoImpl<C, F>
        implements ExtractionFree2TripDao<C, F>, Free2Specification {

    private static final String XML_QUERY_FREE_PATH = "free2/v%s/%s";

    @Autowired
    protected StrategyService strategyService;

    @Autowired
    protected ProgramService programService;

    @Autowired
    protected ResourceLoader resourceLoader;

    @Autowired
    protected SumarisDatabaseMetadata databaseMetadata;

    @Autowired
    protected ExtractionTableDao extractionTableDao;

    @Override
    public <R extends C> R execute(F filter) {
        String sheetName = filter != null ? filter.getSheetName() : null;

        R context = super.execute(filter);

        // Override some context properties
        context.setFormat(LiveFormatEnum.FREE2);

        // Stop here, if sheet already filled
        if (sheetName != null && context.hasSheet(sheetName)) return context;

        // Gear table
        long rowCount = createGearTable(context);
        if (rowCount == 0) return context;
        if (sheetName != null && context.hasSheet(sheetName)) return context;

        // Strategy table
        rowCount = createStrategyTable(context);
        if (rowCount == 0) return context;
        if (sheetName != null && context.hasSheet(sheetName)) return context;

        // Strategy table
        rowCount = createDetailTable(context);
        if (rowCount == 0) return context;
        if (sheetName != null && context.hasSheet(sheetName)) return context;


        return context;
    }


    /* -- protected methods -- */

    @Override
    protected Class<? extends ExtractionFree2ContextVO> getContextClass() {
        return ExtractionFree2ContextVO.class;
    }

    @Override
    protected void fillContextTableNames(C context) {
        super.fillContextTableNames(context);

        // Set table names
        context.setTripTableName(TABLE_NAME_PREFIX + TRIP_SHEET_NAME + "_" + context.getId());
        context.setStationTableName(TABLE_NAME_PREFIX + STATION_SHEET_NAME + "_" + context.getId());
        context.setGearTableName(TABLE_NAME_PREFIX + GEAR_SHEET_NAME + "_" + context.getId());
        context.setRawSpeciesListTableName(TABLE_NAME_PREFIX + "RAW_SL" + "_" + context.getId());
        context.setStrategyTableName(TABLE_NAME_PREFIX + STRATEGY_SHEET_NAME + "_" + context.getId());
        context.setDetailTableName(TABLE_NAME_PREFIX + DETAIL_SHEET_NAME + "_" + context.getId());
        context.setSpeciesListTableName(TABLE_NAME_PREFIX + SPECIES_LIST_SHEET_NAME + "_" + context.getId());
        context.setSpeciesLengthTableName(TABLE_NAME_PREFIX + SPECIES_LENGTH_SHEET_NAME + "_" + context.getId());

        // Set sheet names
        context.setTripSheetName(TRIP_SHEET_NAME);
        context.setStationSheetName(STATION_SHEET_NAME);
        context.setGearSheetName(GEAR_SHEET_NAME);
        context.setStrategySheetName(STRATEGY_SHEET_NAME);
        context.setDetailSheetName(DETAIL_SHEET_NAME);
        context.setSpeciesListSheetName(SPECIES_LIST_SHEET_NAME);
        context.setSpeciesLengthSheetName(SPECIES_LENGTH_SHEET_NAME);

    }

    protected XMLQuery createTripQuery(C context) {
        XMLQuery xmlQuery = createXMLQuery(context, "createTripTable");
        xmlQuery.bind("tripTableName", context.getTripTableName());

        // Bind some referential ids
        xmlQuery.bind("contractCodePmfmId", String.valueOf(PmfmEnum.CONTRACT_CODE.getId()));

        // Date filters
        xmlQuery.setGroup("startDateFilter", context.getStartDate() != null);
        xmlQuery.bind("startDate", Daos.getSqlToDate(context.getStartDate()));
        xmlQuery.setGroup("endDateFilter", context.getEndDate() != null);
        xmlQuery.bind("endDate", Daos.getSqlToDate(context.getEndDate()));

        // Program tripFilter
        xmlQuery.setGroup("programFilter", CollectionUtils.isNotEmpty(context.getProgramLabels()));
        xmlQuery.bind("progLabels", Daos.getSqlInEscapedStrings(context.getProgramLabels()));

        // Location Filter
        xmlQuery.setGroup("locationFilter", CollectionUtils.isNotEmpty(context.getLocationIds()));
        xmlQuery.bind("locationIds", Daos.getSqlInNumbers(context.getLocationIds()));

        // Recorder Department tripFilter
        xmlQuery.setGroup("departmentFilter", CollectionUtils.isNotEmpty(context.getRecorderDepartmentIds()));
        xmlQuery.bind("recDepIds", Daos.getSqlInNumbers(context.getRecorderDepartmentIds()));

        // Vessel tripFilter
        xmlQuery.setGroup("vesselFilter", CollectionUtils.isNotEmpty(context.getVesselIds()));
        xmlQuery.bind("vesselIds", Daos.getSqlInNumbers(context.getVesselIds()));

        return xmlQuery;
    }

    protected XMLQuery createStationQuery(C context) {

        XMLQuery xmlQuery = createXMLQuery(context, "createStationTable");
        xmlQuery.bind("tripTableName", context.getTripTableName());
        xmlQuery.bind("stationTableName", context.getStationTableName());

        // Bind some PMFM ids
        xmlQuery.bind("normalProgressPmfmId", String.valueOf(PmfmEnum.TRIP_PROGRESS.getId()));
        xmlQuery.bind("seaStatePmfmId", String.valueOf(PmfmEnum.SEA_STATE.getId()));
        xmlQuery.bind("mainFishingDepthPmfmId", String.valueOf(PmfmEnum.GEAR_DEPTH_M.getId()));
        xmlQuery.bind("mainWaterDepthPmfmId", String.valueOf(PmfmEnum.BOTTOM_DEPTH_M.getId()));

        xmlQuery.bind("meshSizePmfmId", String.valueOf(PmfmEnum.SMALLER_MESH_GAUGE_MM.getId()));

        xmlQuery.bind("headlineCumulativeLengthPmfmId", String.valueOf(PmfmEnum.HEADLINE_CUMULATIVE_LENGTH.getId()));
        xmlQuery.bind("beamCumulativeLengthPmfmId", String.valueOf(PmfmEnum.BEAM_CUMULATIVE_LENGTH.getId()));
        xmlQuery.bind("netLengthPmfmId", String.valueOf(PmfmEnum.NET_LENGTH.getId()));

        xmlQuery.bind("gearSpeedPmfmId", String.valueOf(PmfmEnum.GEAR_SPEED.getId()));


        xmlQuery.bind("selectionDevicePmfmId", String.valueOf(PmfmEnum.SELECTIVITY_DEVICE.getId()));


        // TODO: add SIH Ifremer missing parameters (see FREE1 format specification) ?
        //        TOTAL_LENGTH_HAULED Longueur levée,               = NET_LENGTH ?
        //        TOTAL_NB_HOOKS Nombre total d'hameçons,           missing in SUMARIS
        //        NB_FISH_POT Nombre de casiers nasses ou poches,   missing in SUMARIS
        //        HEADLINE_LENGTH Longueur de la corde de dos (cumulée si jumeaux),   = HEADLINE_CUMULATIVE_LENGTH ?
        //        WIDTH_GEAR Largeur cumulée (drague),              missing in SUMARIS
        //        SEINE_LENGTH Longueur de la bolinche ou senne     missing in SUMARIS


        return xmlQuery;
    }


    /* -- protected methods -- */

    protected long createGearTable(C context) {

        log.debug(String.format("Extraction #%s > Creating gear table...", context.getId()));
        XMLQuery xmlQuery = createXMLQuery(context, "createGearTable");
        xmlQuery.bind("stationTableName", context.getStationTableName());
        xmlQuery.bind("gearTableName", context.getGearTableName());

        // Bind some PMFMs
        xmlQuery.bind("noneUnitId", String.valueOf(UnitEnum.NONE.getId()));

        // execute
        execute(xmlQuery);

        long count = countFrom(context.getGearTableName());

        // Clean row using generic tripFilter
        if (count > 0) {
            count -= cleanRow(context.getGearTableName(), context.getFilter(), GEAR_SHEET_NAME);
        }

        // Add result table to context
        if (count > 0) {
            context.addTableName(context.getGearTableName(),
                    GEAR_SHEET_NAME,
                    xmlQuery.getHiddenColumnNames(),
                    xmlQuery.hasDistinctOption());
            log.debug(String.format("Extraction #%s > gear table: %s rows inserted", context.getId(), count));
        }
        else {
            context.addRawTableName(context.getGearTableName());
        }
        return count;
    }

    /**
     * Create raw table (with hidden columns used by sub table - e.g. SAMPLE_ID)
     * @param context
     * @param excludeInvalidStation
     * @return
     */
    protected long createRawSpeciesListTable(C context, boolean excludeInvalidStation) {
        String tableName = context.getRawSpeciesListTableName();

        XMLQuery rawXmlQuery = createRawSpeciesListQuery(context, excludeInvalidStation);
        execute(rawXmlQuery);

        // Clean row using generic filter
        long count = countFrom(tableName);
        if (count > 0) {
            cleanRow(tableName, context.getFilter(), context.getSpeciesListSheetName());
        }

        // Add as a raw table (to be able to clean it later)
        context.addRawTableName(tableName);

        return count;
    }

    protected XMLQuery createRawSpeciesListQuery(C context, boolean excludeInvalidStation) {
        XMLQuery xmlQuery = createXMLQuery(context, "createRawSpeciesListTable");
        xmlQuery.bind("stationTableName", context.getStationTableName());
        xmlQuery.bind("rawSpeciesListTableName", context.getRawSpeciesListTableName());

        // Bind some ids
        xmlQuery.bind("catchCategoryPmfmId", String.valueOf(PmfmEnum.DISCARD_OR_LANDING.getId()));
        xmlQuery.bind("landingQvId", String.valueOf(QualitativeValueEnum.LANDING.getId()));
        xmlQuery.bind("discardQvId", String.valueOf(QualitativeValueEnum.DISCARD.getId()));

        // Exclude not valid station
        xmlQuery.setGroup("excludeInvalidStation", excludeInvalidStation);

        return xmlQuery;
    }

    protected long createStrategyTable(C context) {
        String tableName = context.getStrategyTableName();

        log.debug(String.format("Extraction #%s > Creating strategy table", context.getId()));
        XMLQuery xmlQuery = createXMLQuery(context, "createStrategyTable");
        xmlQuery.bind("rawSpeciesListTableName", context.getRawSpeciesListTableName());
        xmlQuery.bind("strategyTableName", tableName);

        // Bind some PMFMs
        //xmlQuery.bind("noneUnitId", String.valueOf(UnitEnum.NONE.getId()));

        // execute
        execute(xmlQuery);

        long count = countFrom(tableName);

        // Clean row using generic tripFilter
        if (count > 0) {
            count -= cleanRow(tableName, context.getFilter(), STRATEGY_SHEET_NAME);
        }

        // Add result table to context
        if (count > 0) {
            context.addTableName(tableName,
                    STRATEGY_SHEET_NAME,
                    xmlQuery.getHiddenColumnNames(),
                    xmlQuery.hasDistinctOption());
            log.debug(String.format("Extraction #%s > strategy table: %s rows inserted", context.getId(), count));
        }
        else {
            context.addRawTableName(tableName);
        }
        return count;
    }

    protected long createDetailTable(C context) {
        String tableName = context.getDetailTableName();

        log.debug(String.format("Extraction #%s > Creating detail table...", context.getId()));
        XMLQuery xmlQuery = createXMLQuery(context, "createDetailTable");
        xmlQuery.bind("rawSpeciesListTableName", context.getRawSpeciesListTableName());
        xmlQuery.bind("detailTableName", tableName);

        // Bind some PMFMs
        //xmlQuery.bind("noneUnitId", String.valueOf(UnitEnum.NONE.getId()));

        // execute
        execute(xmlQuery);

        long count = countFrom(tableName);

        // Clean row using filter
        if (count > 0) {
            count -= cleanRow(tableName, context.getFilter(), DETAIL_SHEET_NAME);
        }

        // Add result table to context
        if (count > 0) {
            context.addTableName(tableName,
                    DETAIL_SHEET_NAME,
                    xmlQuery.getHiddenColumnNames(),
                    xmlQuery.hasDistinctOption());
            log.debug(String.format("Extraction #%s > detail table: %s rows inserted", context.getId(), count));
        }
        else {
            context.addRawTableName(tableName);
        }
        return count;
    }

    @Override
    protected XMLQuery createSpeciesListQuery(C context) {
        return super.createSpeciesListQuery(context);
    }

    protected long createSpeciesLengthTable(C context) {

        // TODO
       return 0;
    }

    protected String getQueryFullName(C context, String queryName) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(context.getVersion());

        String versionStr = VERSION_1_9.replaceAll("[.]", "_");
        switch (queryName) {
            case "createTripTable":
            case "createStationTable":
            case "createRawSpeciesListTable":
            case "createSpeciesListTable":
            case "createStrategyTable":
            case "createDetailTable":
            case "createGearTable":
                return String.format(XML_QUERY_FREE_PATH, versionStr, queryName);
            default:
                return super.getQueryFullName(context, queryName);
        }
    }
}

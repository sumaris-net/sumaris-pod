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

package net.sumaris.core.extraction.dao.trip.free;

import com.google.common.base.Preconditions;
import net.sumaris.core.dao.technical.schema.SumarisDatabaseMetadata;
import net.sumaris.core.extraction.dao.technical.Daos;
import net.sumaris.core.extraction.dao.technical.XMLQuery;
import net.sumaris.core.extraction.dao.technical.table.ExtractionTableDao;
import net.sumaris.core.extraction.dao.trip.rdb.ExtractionRdbTripDaoImpl;
import net.sumaris.core.extraction.vo.ExtractionFilterVO;
import net.sumaris.core.extraction.vo.trip.free.ExtractionFreeTripVersion;
import net.sumaris.core.extraction.vo.trip.free.ExtractionFreeV2ContextVO;
import net.sumaris.core.model.referential.location.LocationLevel;
import net.sumaris.core.model.referential.location.LocationLevelEnum;
import net.sumaris.core.model.referential.pmfm.PmfmEnum;
import net.sumaris.core.service.administration.programStrategy.ProgramService;
import net.sumaris.core.service.administration.programStrategy.StrategyService;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Repository;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 */
@Repository("extractionFreeV2TripDao")
@Lazy
public class ExtractionFreeV2TripDaoImpl<C extends ExtractionFreeV2ContextVO> extends ExtractionRdbTripDaoImpl<C> implements ExtractionFreeV2TripDao {

    private static final Logger log = LoggerFactory.getLogger(ExtractionFreeV2TripDaoImpl.class);


    private static final String TR_TABLE_NAME_PATTERN = TABLE_NAME_PREFIX + ExtractionFreeV2TripDao.TR_SHEET_NAME + "_%s";
    private static final String HH_TABLE_NAME_PATTERN = TABLE_NAME_PREFIX + ExtractionFreeV2TripDao.HH_SHEET_NAME + "_%s";

    private static final String GEAR_TABLE_NAME_PATTERN = TABLE_NAME_PREFIX + GEAR_SHEET_NAME + "_%s";

    private static final String XML_QUERY_FREE_PATH = "free/v%s/%s";
    private static final String VERSION_2_LABEL = ExtractionFreeTripVersion.VERSION_2.getLabel();

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
    public C execute(ExtractionFilterVO filter) {
        String sheetName = filter != null ? filter.getSheetName() : null;

        C context = super.execute(filter);

        // Override some context properties
        context.setFormatName(FREE2_FORMAT);
        context.setFormatVersion(VERSION_2_LABEL);

        // Stop here, if sheet already filled
        if (sheetName != null && context.hasSheet(sheetName)) return context;

        // Gear table
        /*
        long rowCount = createGearTable(context);
        if (rowCount == 0) return context;
        if (sheetName != null && context.hasSheet(sheetName)) return context;
        */

        return context;
    }


    /* -- protected methods -- */

    @Override
    protected Class<? extends ExtractionFreeV2ContextVO> getContextClass() {
        return ExtractionFreeV2ContextVO.class;
    }

    @Override
    protected void fillContextTableNames(C context) {
        super.fillContextTableNames(context);

        // Overwrite some table names
        context.setTripTableName(String.format(TR_TABLE_NAME_PATTERN, context.getId()));
        context.setStationTableName(String.format(HH_TABLE_NAME_PATTERN, context.getId()));

        context.setGearTableName(String.format(GEAR_TABLE_NAME_PATTERN, context.getId()));

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
        xmlQuery.bind("progLabels", Daos.getSqlInValueFromStringCollection(context.getProgramLabels()));

        // Location Filter
        xmlQuery.setGroup("locationFilter", CollectionUtils.isNotEmpty(context.getLocationIds()));
        xmlQuery.bind("locationIds", Daos.getSqlInValueFromIntegerCollection(context.getLocationIds()));

        // Recorder Department tripFilter
        xmlQuery.setGroup("departmentFilter", CollectionUtils.isNotEmpty(context.getRecorderDepartmentIds()));
        xmlQuery.bind("recDepIds", Daos.getSqlInValueFromIntegerCollection(context.getRecorderDepartmentIds()));

        // Vessel tripFilter
        xmlQuery.setGroup("vesselFilter", CollectionUtils.isNotEmpty(context.getVesselIds()));
        xmlQuery.bind("vesselIds", Daos.getSqlInValueFromIntegerCollection(context.getVesselIds()));

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

    @Override
    protected long createSpeciesListTable(C context) {
        // TODO
        return 0;
    }

    /* -- protected methods -- */

    protected long createGearTable(C context) {

        log.debug(String.format("Extraction #%s > Creating gear table...", context.getId()));
        XMLQuery xmlQuery = createXMLQuery(context, "createGearTable");
        xmlQuery.bind("stationTableName", context.getStationTableName());
        xmlQuery.bind("gearTableName", context.getGearTableName());

        // aggregate insertion
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

    protected String getQueryFullName(C context, String queryName) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(context.getFormatVersion());

        String versionStr = VERSION_2_LABEL.replaceAll("[.]", "_");
        switch (queryName) {
            case "createTripTable":
            case "createStationTable":
                return String.format(XML_QUERY_FREE_PATH, versionStr, queryName);
            default:
                return super.getQueryFullName(context, queryName);
        }
    }
}

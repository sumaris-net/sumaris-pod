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

import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.model.referential.pmfm.PmfmEnum;
import net.sumaris.extraction.core.dao.technical.xml.XMLQuery;
import net.sumaris.extraction.core.dao.trip.rdb.ExtractionRdbTripDaoImpl;
import net.sumaris.extraction.core.format.LiveFormatEnum;
import net.sumaris.extraction.core.specification.data.trip.SurvivalTestSpecification;
import net.sumaris.extraction.core.vo.ExtractionFilterVO;
import net.sumaris.extraction.core.vo.trip.rdb.ExtractionRdbTripContextVO;
import net.sumaris.extraction.core.vo.trip.survivalTest.ExtractionSurvivalTestContextVO;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;

import javax.persistence.PersistenceException;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 */
@Repository("extractionSurvivalTestDao")
@Lazy
@Slf4j
public class ExtractionSurvivalTestDaoImpl<C extends ExtractionSurvivalTestContextVO, F extends ExtractionFilterVO>
        extends ExtractionRdbTripDaoImpl<C, F>
        implements SurvivalTestSpecification {

    private static final String ST_TABLE_NAME_PATTERN = TABLE_NAME_PREFIX + ST_SHEET_NAME + "_%s";
    private static final String RL_TABLE_NAME_PATTERN = TABLE_NAME_PREFIX + RL_SHEET_NAME + "_%s";

    @Override
    public LiveFormatEnum getFormat() {
        return LiveFormatEnum.SURVIVAL_TEST;
    }

    @Override
    public <R extends C> R execute(F filter) {
        // Execute inherited extraction
        R context = super.execute(filter);

        // Override some context properties
        context.setFormat(LiveFormatEnum.SURVIVAL_TEST);
        context.setSurvivalTestTableName(String.format(ST_TABLE_NAME_PATTERN, context.getId()));
        context.setReleaseTableName(String.format(RL_TABLE_NAME_PATTERN, context.getId()));

        // Stop here, if sheet already filled
        String sheetName = filter != null && filter.isPreview() ? filter.getSheetName() : null;
        if (sheetName != null && context.hasSheet(sheetName)) return context;

        try {
            // Survival test table
            long rowCount = createSurvivalTestTable(context);
            if (rowCount == 0) return context;
            if (sheetName != null && context.hasSheet(sheetName)) return context;

            // Release table
            createReleaseTable(context);
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
    protected XMLQuery createTripQuery(C context) {
        XMLQuery xmlQuery = super.createTripQuery(context);

        // Inject specific select clause
        xmlQuery.injectQuery(getXMLQueryURL(context, "injectionTripTable"));

        // Disable filter on controlled trips
        xmlQuery.setGroup("controlledFilter", false);

        // Bind PMFM ids
        xmlQuery.bind("mainMetierPmfmId", String.valueOf(PmfmEnum.MAIN_METIER.getId()));
        xmlQuery.bind("conveyorBeltPmfmId", String.valueOf(PmfmEnum.CONVEYOR_BELT.getId()));
        xmlQuery.bind("nbFishermenPmfmId", String.valueOf(PmfmEnum.NB_FISHERMEN.getId()));
        xmlQuery.bind("nbSamplingOperPmfmId", String.valueOf(PmfmEnum.NB_SAMPLING_OPERATION.getId()));
        xmlQuery.bind("randomSamplingOperPmfmId", String.valueOf(PmfmEnum.RANDOM_SAMPLING_OPERATION.getId()));

        return xmlQuery;
    }

    @Override
    protected XMLQuery createStationQuery(C context) {
        XMLQuery xmlQuery = super.createStationQuery(context);

        // Inject specific select clause
        xmlQuery.injectQuery(getXMLQueryURL(context, "injectionStationTable"));

        // Bind PMFM ids
        xmlQuery.bind("substrateTypePmfmId", String.valueOf(PmfmEnum.SUBSTRATE_TYPE.getId()));
        xmlQuery.bind("bottomTempPmfmId", String.valueOf(PmfmEnum.BOTTOM_TEMP_C.getId()));
        xmlQuery.bind("seaStatePmfmId", String.valueOf(PmfmEnum.SEA_STATE.getId()));
        xmlQuery.bind("survivalSamplingTypePmfmId", String.valueOf(PmfmEnum.SURVIVAL_SAMPLING_TYPE.getId()));

        xmlQuery.bind("landingWeightPmfmId", String.valueOf(PmfmEnum.LANDING_WEIGHT.getId()));
        xmlQuery.bind("sandStonesWeightRangePmfmId", String.valueOf(PmfmEnum.SAND_STONES_WEIGHT_RANGE.getId()));
        xmlQuery.bind("benthosWeightRangePmfmId", String.valueOf(PmfmEnum.BENTHOS_WEIGHT_RANGE.getId()));
        xmlQuery.bind("onDeckDateTimePmfmId", String.valueOf(PmfmEnum.ON_DECK_DATE_TIME.getId()));
        xmlQuery.bind("sortingDateTimePmfmId", String.valueOf(PmfmEnum.SORTING_START_DATE_TIME.getId()));
        xmlQuery.bind("sortingEndDateTimePmfmId", String.valueOf(PmfmEnum.SORTING_END_DATE_TIME.getId()));

        return xmlQuery;
    }

    @Override
    protected XMLQuery createSpeciesLengthQuery(C context) {
        XMLQuery xmlQuery = super.createSpeciesLengthQuery(context);

        // Inject specific select clause
        xmlQuery.injectQuery(getXMLQueryURL(context, "injectionSpeciesLengthTable"));

        // Bind PMFM ids
        xmlQuery.bind("isDeadPmfmId", String.valueOf(PmfmEnum.IS_DEAD.getId()));
        xmlQuery.bind("tagIdPmfmId", String.valueOf(PmfmEnum.TAG_ID.getId()));
        xmlQuery.bind("discardReasonPmfmId", String.valueOf(PmfmEnum.DISCARD_REASON.getId()));

        return xmlQuery;
    }

    @Override
    protected Class<? extends ExtractionRdbTripContextVO> getContextClass() {
        return ExtractionSurvivalTestContextVO.class;
    }

    protected String getQueryFullName(C context, String queryName) {

        switch (queryName) {
            case "injectionTripTable":
            case "injectionStationTable":
            case "injectionSpeciesLengthTable":
            case "createSurvivalTestTable":
            case "createReleaseTable":
                return getQueryFullName(SurvivalTestSpecification.FORMAT, SurvivalTestSpecification.VERSION_1_0, queryName);
            default:
                return super.getQueryFullName(context, queryName);
        }
    }

    protected long createSurvivalTestTable(C context) {

        log.debug(String.format("Extraction #%s > Creating survival tests table...", context.getId()));
        XMLQuery xmlQuery = createXMLQuery(context, "createSurvivalTestTable");
        xmlQuery.bind("stationTableName", context.getStationTableName());
        xmlQuery.bind("survivalTestTableName", context.getSurvivalTestTableName());

        // aggregate insertion
        execute(xmlQuery);
        long count = countFrom(context.getSurvivalTestTableName());

        // Clean row using generic tripFilter
        if (count > 0) {
            count -= cleanRow(context.getSurvivalTestTableName(), context.getFilter(), ST_SHEET_NAME);
        }

        // Add result table to context
        if (count > 0) {
            context.addTableName(context.getSurvivalTestTableName(),
                    ST_SHEET_NAME,
                    xmlQuery.getHiddenColumnNames(),
                    xmlQuery.hasDistinctOption());
            log.debug(String.format("Extraction #%s > Survival test table: %s rows inserted", context.getId(), count));
        }
        else {
            context.addRawTableName(context.getSurvivalTestTableName());
        }
        return count;
    }

    protected long createReleaseTable(C context) {

        log.debug(String.format("Extraction #%s > Creating releases table...", context.getId()));
        XMLQuery xmlQuery = createXMLQuery(context, "createReleaseTable");
        xmlQuery.bind("stationTableName", context.getStationTableName());
        xmlQuery.bind("releaseTableName", context.getReleaseTableName());

        // aggregate insertion
        execute(xmlQuery);
        long count = countFrom(context.getReleaseTableName());

        // Clean row using generic tripFilter
        if (count > 0) {
            count -= cleanRow(context.getReleaseTableName(), context.getFilter(), RL_SHEET_NAME);
        }

        // Add result table to context
        if (count > 0) {
            context.addTableName(context.getReleaseTableName(),
                    RL_SHEET_NAME,
                    xmlQuery.getHiddenColumnNames(),
                    xmlQuery.hasDistinctOption());
            log.debug(String.format("Extraction #%s > Release table: %s rows inserted", context.getId(), count));
        }
        else {
            context.addRawTableName(context.getReleaseTableName());
        }
        return count;
    }

}

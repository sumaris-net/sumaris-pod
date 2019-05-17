package net.sumaris.core.extraction.dao.trip.survivalTest;

import com.google.common.base.Preconditions;
import net.sumaris.core.extraction.dao.trip.rdb.ExtractionRdbTripDaoImpl;
import net.sumaris.core.extraction.dao.technical.XMLQuery;
import net.sumaris.core.extraction.vo.ExtractionFilterVO;
import net.sumaris.core.extraction.vo.live.trip.ExtractionTripFilterVO;
import net.sumaris.core.extraction.vo.live.trip.rdb.ExtractionRdbTripContextVO;
import net.sumaris.core.extraction.vo.live.trip.survivalTest.ExtractionSurvivalTestContextVO;
import net.sumaris.core.extraction.vo.live.trip.survivalTest.ExtractionSurvivalTestVersion;
import net.sumaris.core.model.referential.pmfm.PmfmEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 */
@Repository("extractionSurvivalTestDao")
@Lazy
public class ExtractionSurvivalTestDaoImpl<C extends ExtractionSurvivalTestContextVO> extends ExtractionRdbTripDaoImpl<C> implements ExtractionSurvivalTestDao {

    private static final Logger log = LoggerFactory.getLogger(ExtractionSurvivalTestDaoImpl.class);

    private static final String XML_QUERY_ST_PATH = "survivalTest/v%s/%s";

    public static final String ST_SHEET_NAME = "ST"; // Survival test
    public static final String RL_SHEET_NAME = "RL"; // Release

    private static final String ST_TABLE_NAME_PATTERN = TABLE_NAME_PREFIX + ST_SHEET_NAME + "_%s";
    private static final String RL_TABLE_NAME_PATTERN = TABLE_NAME_PREFIX + RL_SHEET_NAME + "_%s";

    private String version = ExtractionSurvivalTestVersion.VERSION_1_0.getLabel();

    @Override
    public C execute(ExtractionTripFilterVO filter, ExtractionFilterVO genericFilter) {
        String sheetName = filter != null ? filter.getSheetName() : null;

        // Execute RDB extraction
        C context = super.execute(filter, genericFilter);

        // Override some context properties
        context.setFormatName(SURVIVAL_TEST_FORMAT);
        context.setFormatVersion(version);
        context.setSurvivalTestTableName(String.format(ST_TABLE_NAME_PATTERN, context.getId()));
        context.setReleaseTableName(String.format(RL_TABLE_NAME_PATTERN, context.getId()));

        // Stop here
        if (sheetName != null && context.hasSheet(sheetName)) return context;

        // Survival test table
        long rowCount = createSurvivalTestTable(context);
        if (rowCount == 0) return context;
        if (sheetName != null && context.hasSheet(sheetName)) return context;

        // Release table
        rowCount = createReleaseTable(context);
        if (rowCount == 0) return context;
        if (sheetName != null && context.hasSheet(sheetName)) return context;

        return context;
    }


    /* -- protected methods -- */

    @Override
    protected XMLQuery createTripQuery(C context) {
        XMLQuery xmlQuery = super.createTripQuery(context);

        // Inject specific select clause
        xmlQuery.injectQuery(getXMLQueryURL(context, "injectionTripTable"));

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
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(context.getFormatVersion());

        String versionStr = version.replaceAll("[.]", "_");
        switch (queryName) {
            case "injectionTripTable":
            case "injectionStationTable":
            case "injectionSpeciesLengthTable":
            case "createSurvivalTestTable":
            case "createReleaseTable":
                return String.format(XML_QUERY_ST_PATH, versionStr, queryName);
            default:
                return super.getQueryFullName(context, queryName);
        }
    }

    protected long createSurvivalTestTable(C context) {

        log.info("Processing survival tests...");
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
            log.debug(String.format("Survival test table: %s rows inserted", count));
        }
        return count;
    }

    protected long createReleaseTable(C context) {

        log.info("Processing releases...");
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
            log.debug(String.format("Release table: %s rows inserted", count));
        }
        return count;
    }

}

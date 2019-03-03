package net.sumaris.core.extraction.dao.trip.survivalTest;

import com.google.common.base.Preconditions;
import net.sumaris.core.extraction.dao.trip.ices.ExtractionIcesDaoImpl;
import net.sumaris.core.extraction.technical.XMLQuery;
import net.sumaris.core.extraction.vo.trip.ExtractionTripFilterVO;
import net.sumaris.core.extraction.vo.trip.ExtractionTripFormat;
import net.sumaris.core.extraction.vo.trip.ices.ExtractionIcesContextVO;
import net.sumaris.core.extraction.vo.trip.survivalTest.ExtractionSurvivalTestContextVO;
import net.sumaris.core.extraction.vo.trip.ExtractionTripContextVO;
import net.sumaris.core.extraction.vo.trip.survivalTest.ExtractionSurvivalTestVersion;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.filter.TripFilterVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 */
@Repository("extractionSurvivalTestDao")
@Lazy
public class ExtractionSurvivalTestDaoImpl<C extends ExtractionSurvivalTestContextVO> extends ExtractionIcesDaoImpl<C> implements ExtractionSurvivalTestDao {

    private static final Logger log = LoggerFactory.getLogger(ExtractionSurvivalTestDaoImpl.class);

    public static final String FORMAT = StringUtils.underscoreToChangeCase(ExtractionTripFormat.SURVIVAL_TEST.name());

    private static final String XML_QUERY_ST_PATH = "survivalTest/v%s/%s";

    private static final String ST_TABLE_NAME_PATTERN = TABLE_NAME_PREFIX + "ST_%s"; // Survival test table

    @Override
    public C execute(ExtractionTripFilterVO filter) {

        // Execute ICES extraction
        C context = super.execute(filter);

        // Stop here
        if (filter != null && filter.isFirstTableOnly()) return context;

        context.setFormatName(FORMAT);
        context.setFormatVersion(ExtractionSurvivalTestVersion.VERSION_1_0.getLabel());

        // Init context for survival test
        context.setSurvivalTestTableName(String.format(ST_TABLE_NAME_PATTERN, context.getId()));

        // Survival test table
        createSurvivalTestTable(context);

        return context;
    }


    /* -- protected methods -- */

    @Override
    protected Class<? extends ExtractionIcesContextVO> getContextClass() {
        return ExtractionSurvivalTestContextVO.class;
    }

    protected String getQueryFullName(ExtractionTripContextVO context, String queryName) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(context.getFormatVersion());

        String versionStr;
        switch (queryName) {
            case "createSurvivalTestTable":
                versionStr = context.getFormatVersion().replaceAll("[.]", "_");
                return String.format(XML_QUERY_ST_PATH, versionStr, queryName);
            default:
                return super.getQueryFullName(context, queryName);
        }
    }

    protected long createSurvivalTestTable(ExtractionSurvivalTestContextVO context) {

        log.info("Processing survival tests...");
        XMLQuery xmlQuery = createXMLQuery(context,"createSurvivalTestTable");
        xmlQuery.bind("stationTableName", context.getStationTableName());
        xmlQuery.bind("survivalTestTableName", context.getSurvivalTestTableName());

        // Bind some PMFM ids
        //xmlQuery.bind("fishingDepthPmfmId", String.valueOf(PmfmId.FISHING_DEPTH.getId()));
        //xmlQuery.bind("mainWaterDepthPmfmId", String.valueOf(PmfmId.BOTTOM_DEPTH_M.getId()));

        // execute insertion
        execute(xmlQuery);

        long count = countFrom(context.getStationTableName());
        if (count > 0) {
            context.addTableName(context.getStationTableName(), "HH");
        }
        return count;
    }
}

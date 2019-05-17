package net.sumaris.core.extraction.dao.trip.cost;

import com.google.common.base.Preconditions;
import net.sumaris.core.extraction.dao.technical.XMLQuery;
import net.sumaris.core.extraction.dao.trip.rdb.ExtractionRdbTripDaoImpl;
import net.sumaris.core.extraction.vo.ExtractionFilterVO;
import net.sumaris.core.extraction.vo.live.trip.ExtractionTripFilterVO;
import net.sumaris.core.extraction.vo.live.trip.cost.ExtractionCostTripVersion;
import net.sumaris.core.extraction.vo.live.trip.rdb.ExtractionRdbTripContextVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 */
@Repository("extractionCostTripDao")
@Lazy
public class ExtractionCostTripDaoImpl<C extends ExtractionRdbTripContextVO> extends ExtractionRdbTripDaoImpl<C> implements ExtractionCostTripDao {

    private static final Logger log = LoggerFactory.getLogger(ExtractionCostTripDaoImpl.class);

    private static final String XML_QUERY_COST_PATH = "cost/v%s/%s";
    private String version = ExtractionCostTripVersion.VERSION_1_4.getLabel();

    @Override
    public C execute(ExtractionTripFilterVO filter, ExtractionFilterVO genericFilter) {
        C context = super.execute(filter, genericFilter);

        // Override some context properties
        context.setFormatName(COST_FORMAT);
        context.setFormatVersion(version);

        return context;
    }

    /* -- protected methods -- */

    protected XMLQuery createStationQuery(C context) {

        XMLQuery xmlQuery = super.createStationQuery(context);

        // Special case for COST format:
        // - Hide GearType (not in the COST format)
        xmlQuery.setGroup("gearType", false);

        return xmlQuery;
    }

    @Override
    protected XMLQuery createSpeciesLengthQuery(C context) {
        XMLQuery xmlQuery = super.createSpeciesLengthQuery(context);

        // Special case for COST format:

        // - Hide sex columns, then replace by a new columns
        xmlQuery.setGroup("sex", false);
        xmlQuery.setGroup("lengthClass", false);
        xmlQuery.setGroup("numberAtLength", false);

        xmlQuery.injectQuery(getXMLQueryURL(context, "injectionSpeciesLengthTable"));

        return xmlQuery;
    }

    protected String getQueryFullName(C context, String queryName) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(context.getFormatVersion());

        String versionStr = version.replaceAll("[.]", "_");
        switch (queryName) {
            case "injectionSpeciesLengthTable":
                return String.format(XML_QUERY_COST_PATH, versionStr, queryName);
            default:
                return super.getQueryFullName(context, queryName);
        }
    }
}

package net.sumaris.core.extraction.dao.trip.cost;

import net.sumaris.core.extraction.dao.trip.rdb.ExtractionRdbTripDao;
import net.sumaris.core.extraction.vo.live.ExtractionLiveFormat;
import net.sumaris.core.util.StringUtils;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 */
public interface ExtractionCostTripDao extends ExtractionRdbTripDao {
    String COST_FORMAT = StringUtils.underscoreToChangeCase(ExtractionLiveFormat.COST.name());

}
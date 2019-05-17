package net.sumaris.core.extraction.dao.trip.rdb;

import net.sumaris.core.extraction.vo.live.ExtractionLiveFormat;
import net.sumaris.core.extraction.vo.live.trip.rdb.ExtractionRdbTripContextVO;
import net.sumaris.core.util.StringUtils;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 */
public interface AggregationRdbDao<C extends ExtractionRdbTripContextVO> {

    String RDB_FORMAT = StringUtils.underscoreToChangeCase(ExtractionLiveFormat.RDB.name());

    String HH_SHEET_NAME = ExtractionRdbTripDao.HH_SHEET_NAME;
    String SL_SHEET_NAME = ExtractionRdbTripDao.SL_SHEET_NAME;
    String HL_SHEET_NAME = ExtractionRdbTripDao.HL_SHEET_NAME;
    String CA_SHEET_NAME = ExtractionRdbTripDao.CA_SHEET_NAME;

    <R extends C> R aggregate(C rawDataContext);
}
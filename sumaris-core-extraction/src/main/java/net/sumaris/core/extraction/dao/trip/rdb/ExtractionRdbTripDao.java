package net.sumaris.core.extraction.dao.trip.rdb;

import net.sumaris.core.extraction.dao.trip.ExtractionTripDao;
import net.sumaris.core.extraction.vo.ExtractionFilterVO;
import net.sumaris.core.extraction.vo.ExtractionRawFormatEnum;
import net.sumaris.core.extraction.vo.trip.rdb.ExtractionRdbTripContextVO;
import net.sumaris.core.util.StringUtils;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 */
public interface ExtractionRdbTripDao<C extends ExtractionRdbTripContextVO> extends ExtractionTripDao {

    String RDB_FORMAT = StringUtils.underscoreToChangeCase(ExtractionRawFormatEnum.RDB.name());

    String HH_SHEET_NAME = "HH";
    String SL_SHEET_NAME = "SL";
    String HL_SHEET_NAME = "HL";
    String CA_SHEET_NAME = "CA";

    <R extends C> R execute(ExtractionFilterVO filter);
}
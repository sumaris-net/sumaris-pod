package net.sumaris.core.extraction.dao.trip;

import net.sumaris.core.extraction.dao.AggregationDao;
import net.sumaris.core.extraction.dao.ExtractionDao;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 */
public interface AggregationTripDao extends AggregationDao  {

    String TR_SHEET_NAME = ExtractionDao.TABLE_NAME_PREFIX;

}

package net.sumaris.core.extraction.dao.trip;

import com.google.common.collect.Lists;
import net.sumaris.core.extraction.dao.AggregationDao;
import net.sumaris.core.extraction.dao.ExtractionDao;
import net.sumaris.core.extraction.vo.ExtractionFilterCriterionVO;
import net.sumaris.core.extraction.vo.ExtractionFilterOperatorEnum;
import net.sumaris.core.extraction.vo.ExtractionFilterVO;
import net.sumaris.core.extraction.vo.live.trip.ExtractionTripFilterVO;
import net.sumaris.core.util.Beans;
import net.sumaris.core.util.Dates;
import net.sumaris.core.util.StringUtils;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 */
public interface AggregationTripDao extends AggregationDao  {

    String TR_SHEET_NAME = ExtractionDao.TABLE_NAME_PREFIX;

}

package net.sumaris.core.extraction.dao.trip.ices;

import net.sumaris.core.extraction.dao.trip.ExtractionTripDao;
import net.sumaris.core.extraction.vo.ExtractionFilterVO;
import net.sumaris.core.extraction.vo.live.trip.ExtractionTripFilterVO;
import net.sumaris.core.extraction.vo.live.ExtractionLiveFormat;
import net.sumaris.core.extraction.vo.live.trip.ices.ExtractionIcesContextVO;
import net.sumaris.core.util.StringUtils;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 */
public interface ExtractionIcesDao<C extends ExtractionIcesContextVO, F extends ExtractionTripFilterVO> extends ExtractionTripDao {

    String ICES_FORMAT = StringUtils.underscoreToChangeCase(ExtractionLiveFormat.ICES.name());

    <R extends C> R execute(F filter, ExtractionFilterVO genericFilter);
}
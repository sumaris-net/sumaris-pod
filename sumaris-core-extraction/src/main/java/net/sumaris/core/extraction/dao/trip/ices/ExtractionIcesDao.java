package net.sumaris.core.extraction.dao.trip.ices;

import net.sumaris.core.extraction.vo.trip.ExtractionTripFilterVO;
import net.sumaris.core.extraction.vo.trip.ExtractionTripFormat;
import net.sumaris.core.extraction.vo.trip.ices.ExtractionIcesContextVO;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.filter.TripFilterVO;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 */
public interface ExtractionIcesDao<C extends ExtractionIcesContextVO, F extends ExtractionTripFilterVO> {

    String ICES_FORMAT = StringUtils.underscoreToChangeCase(ExtractionTripFormat.ICES.name());

    <R extends C> R execute(F filter);
}
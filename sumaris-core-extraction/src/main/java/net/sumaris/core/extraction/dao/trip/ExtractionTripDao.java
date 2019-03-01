package net.sumaris.core.extraction.dao.trip;

import net.sumaris.core.extraction.vo.trip.ExtractionTripContextVO;
import net.sumaris.core.extraction.vo.trip.ExtractionTripFilterVO;
import net.sumaris.core.vo.filter.TripFilterVO;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 */
public interface ExtractionTripDao<C extends ExtractionTripContextVO, F extends ExtractionTripFilterVO> {

    String CATEGORY = "trip";

}

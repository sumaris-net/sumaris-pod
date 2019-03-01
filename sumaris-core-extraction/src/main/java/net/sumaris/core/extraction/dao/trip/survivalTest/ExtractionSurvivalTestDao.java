package net.sumaris.core.extraction.dao.trip.survivalTest;

import net.sumaris.core.extraction.vo.trip.ExtractionTripFilterVO;
import net.sumaris.core.extraction.vo.trip.survivalTest.ExtractionSurvivalTestContextVO;
import net.sumaris.core.vo.filter.TripFilterVO;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 */
public interface ExtractionSurvivalTestDao<C extends ExtractionSurvivalTestContextVO, F extends ExtractionTripFilterVO> {

    <R extends C> R execute(F filter);
}

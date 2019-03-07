package net.sumaris.core.extraction.dao.trip.survivalTest;

import net.sumaris.core.extraction.dao.trip.ExtractionTripDao;
import net.sumaris.core.extraction.vo.ExtractionFilterVO;
import net.sumaris.core.extraction.vo.live.trip.ExtractionTripFilterVO;
import net.sumaris.core.extraction.vo.live.ExtractionLiveFormat;
import net.sumaris.core.extraction.vo.live.trip.survivalTest.ExtractionSurvivalTestContextVO;
import net.sumaris.core.util.StringUtils;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 */
public interface ExtractionSurvivalTestDao<C extends ExtractionSurvivalTestContextVO, F extends ExtractionTripFilterVO> extends ExtractionTripDao {

    String SURVIVAL_TEST_FORMAT = StringUtils.underscoreToChangeCase(ExtractionLiveFormat.SURVIVAL_TEST.name());

    <R extends C> R execute(F filter, ExtractionFilterVO genericFilter);
}

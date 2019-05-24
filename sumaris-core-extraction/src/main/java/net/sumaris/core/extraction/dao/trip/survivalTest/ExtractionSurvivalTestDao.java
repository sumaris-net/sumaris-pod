package net.sumaris.core.extraction.dao.trip.survivalTest;

import net.sumaris.core.extraction.dao.trip.ExtractionTripDao;
import net.sumaris.core.extraction.vo.ExtractionFilterVO;
import net.sumaris.core.extraction.vo.trip.ExtractionTripFilterVO;
import net.sumaris.core.extraction.vo.ExtractionRawFormatEnum;
import net.sumaris.core.extraction.vo.trip.survivalTest.ExtractionSurvivalTestContextVO;
import net.sumaris.core.util.StringUtils;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 */
public interface ExtractionSurvivalTestDao<C extends ExtractionSurvivalTestContextVO> extends ExtractionTripDao {

    String SURVIVAL_TEST_FORMAT = StringUtils.underscoreToChangeCase(ExtractionRawFormatEnum.SURVIVAL_TEST.name());

    <R extends C> R execute(ExtractionFilterVO genericFilter);
}

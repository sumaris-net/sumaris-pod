package net.sumaris.core.extraction.dao.cost;

import net.sumaris.core.extraction.vo.cost.ExtractionCostContextVO;
import net.sumaris.core.vo.filter.TripFilterVO;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author peck7 on 17/12/2018.
 */
public interface ExtractionCostDao {

    ExtractionCostContextVO execute(TripFilterVO filter);


}

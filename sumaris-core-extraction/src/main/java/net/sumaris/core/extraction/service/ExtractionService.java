package net.sumaris.core.extraction.service;

import net.sumaris.core.vo.filter.TripFilterVO;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author peck7 on 17/12/2018.
 */
@Transactional
public interface ExtractionService {

    void performExtraction(TripFilterVO filter);
}

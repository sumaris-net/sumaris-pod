package net.sumaris.core.extraction.service;

import net.sumaris.core.extraction.vo.ExtractionContextVO;
import net.sumaris.core.extraction.vo.ExtractionFilterVO;
import net.sumaris.core.extraction.vo.ExtractionTypeVO;
import net.sumaris.core.extraction.vo.live.trip.ExtractionTripFilterVO;
import org.springframework.transaction.annotation.Transactional;

/**
 * Create aggregation tables, from a data extraction.
 * @author benoit.lavenier@e-is.pro
 * @since 0.12.0
 */
@Transactional
public interface AggregationService {


    /**
     * Do an aggregate
     * @param type
     * @param filter
     */
    ExtractionContextVO aggregate(ExtractionTypeVO type, ExtractionFilterVO filter);

    /**
     * Do an aggregate, on trip's data
     * @param type
     * @param filter
     */
//    void aggregate(ExtractionTypeVO type, ExtractionTripFilterVO filter);

}

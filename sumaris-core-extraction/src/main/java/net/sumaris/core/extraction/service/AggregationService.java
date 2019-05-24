package net.sumaris.core.extraction.service;

import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.extraction.vo.*;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Create aggregation tables, from a data extraction.
 * @author benoit.lavenier@e-is.pro
 * @since 0.12.0
 */
@Transactional
public interface AggregationService {

    @Transactional(readOnly = true)
    List<AggregationTypeVO> getAllAggregationTypes();

    /**
     * Do an aggregate
     * @param type
     * @param filter
     */
    @Transactional
    AggregationContextVO execute(AggregationTypeVO type,
                                 @Nullable ExtractionFilterVO filter);

    @Transactional(readOnly = true)
    AggregationResultVO read(AggregationContextVO context,
                             @Nullable  ExtractionFilterVO filter,
                             @Nullable AggregationStrataVO strata,
                             int offset, int size, String sort, SortDirection direction);

    @Transactional
    AggregationResultVO executeAndRead(AggregationTypeVO type,
                                       @Nullable ExtractionFilterVO filter,
                                       @Nullable AggregationStrataVO strata,
                                       int offset, int size,
                                       @Nullable String sort,
                                       @Nullable SortDirection direction);
}

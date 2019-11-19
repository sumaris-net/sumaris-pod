package net.sumaris.core.extraction.service;

import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.extraction.vo.*;
import net.sumaris.core.extraction.vo.filter.AggregationTypeFilterVO;
import net.sumaris.core.vo.technical.extraction.ProductFetchOptions;
import net.sumaris.core.vo.technical.extraction.ExtractionProductColumnVO;
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
    List<AggregationTypeVO> findByFilter(@Nullable AggregationTypeFilterVO filter, ProductFetchOptions fetchOptions);

    @Transactional(readOnly = true)
    AggregationTypeVO get(int id, ProductFetchOptions fetchOptions);

    /**
     * Do an aggregate
     * @param type
     * @param filter
     */
    @Transactional
    AggregationContextVO execute(AggregationTypeVO type,
                                 @Nullable ExtractionFilterVO filter);

    @Transactional(readOnly = true)
    AggregationResultVO read(AggregationTypeVO type,
                             @Nullable  ExtractionFilterVO filter,
                             @Nullable AggregationStrataVO strata,
                             int offset, int size, String sort, SortDirection direction);

    @Transactional(readOnly = true)
    AggregationResultVO read(AggregationContextVO context,
                             @Nullable  ExtractionFilterVO filter,
                             @Nullable AggregationStrataVO strata,
                             int offset, int size, String sort, SortDirection direction);

    @Transactional(readOnly = true)
    List<ExtractionProductColumnVO> getColumnsBySheetName(AggregationTypeVO type, String sheetName);

    @Transactional
    AggregationResultVO executeAndRead(AggregationTypeVO type,
                                       @Nullable ExtractionFilterVO filter,
                                       @Nullable AggregationStrataVO strata,
                                       int offset, int size,
                                       @Nullable String sort,
                                       @Nullable SortDirection direction);

    @Transactional
    AggregationTypeVO save(AggregationTypeVO type, @Nullable ExtractionFilterVO filter);

    @Transactional
    void delete(int id);
}

package net.sumaris.extraction.core.service;

/*-
 * #%L
 * SUMARiS:: Core Extraction
 * %%
 * Copyright (C) 2018 - 2019 SUMARiS Consortium
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.extraction.core.vo.*;
import net.sumaris.extraction.core.vo.filter.AggregationTypeFilterVO;
import net.sumaris.core.model.technical.extraction.IExtractionFormat;
import net.sumaris.core.vo.technical.extraction.AggregationStrataVO;
import net.sumaris.core.vo.technical.extraction.ExtractionProductFetchOptions;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Create aggregation tables, from a data extraction.
 *
 * @author benoit.lavenier@e-is.pro
 * @since 0.12.0
 */
@Transactional
public interface AggregationService {

    @Transactional(readOnly = true)
    AggregationTypeVO getTypeByFormat(IExtractionFormat format);

    @Transactional(readOnly = true)
    List<AggregationTypeVO> findTypesByFilter(@Nullable AggregationTypeFilterVO filter, ExtractionProductFetchOptions fetchOptions);

    @Transactional(readOnly = true)
    AggregationTypeVO getTypeById(int id, ExtractionProductFetchOptions fetchOptions);

    /**
     * Refresh a product (execute the aggregation, using the filter stored in the product)
     * @param productId
     */
    @Transactional
    AggregationTypeVO updateProduct(int productId);

    /**
     * Do an aggregate
     *
     * @param type
     * @param filter
     * @param strata
     */
    @Transactional
    AggregationContextVO aggregate(AggregationTypeVO type,
                                   @Nullable ExtractionFilterVO filter,
                                   @Nullable AggregationStrataVO strata);

    @Transactional(readOnly = true)
    AggregationResultVO getAggBySpace(AggregationTypeVO type,
                                      @Nullable ExtractionFilterVO filter,
                                      @Nullable AggregationStrataVO strata,
                                      Page page);

    @Transactional(readOnly = true)
    AggregationResultVO getAggBySpace(AggregationContextVO context,
                                      @Nullable ExtractionFilterVO filter,
                                      @Nullable AggregationStrataVO strata,
                                      Page page);

    @Transactional(readOnly = true)
    AggregationTechResultVO getAggByTech(AggregationTypeVO format,
                                         @Nullable ExtractionFilterVO filter,
                                         @Nullable AggregationStrataVO strata,
                                         String sort,
                                         SortDirection direction);

    @Transactional(readOnly = true)
    MinMaxVO getAggMinMaxByTech(AggregationTypeVO format,
                                @Nullable ExtractionFilterVO filter,
                                @Nullable AggregationStrataVO strata);

    @Transactional(rollbackFor = IOException.class)
    File executeAndDump(AggregationTypeVO type,
                        @Nullable ExtractionFilterVO filter,
                        @Nullable AggregationStrataVO strata);

    @Transactional
    AggregationResultVO executeAndRead(AggregationTypeVO type,
                                       @Nullable ExtractionFilterVO filter,
                                       @Nullable AggregationStrataVO strata,
                                       Page page);

    @Transactional(timeout = 10000000)
    AggregationTypeVO save(AggregationTypeVO type, @Nullable ExtractionFilterVO filter);

    @Async
    @Transactional(timeout = 10000000, propagation = Propagation.REQUIRES_NEW)
    CompletableFuture<AggregationTypeVO> asyncSave(AggregationTypeVO type, @Nullable ExtractionFilterVO filter);

    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRES_NEW)
    void clean(AggregationContextVO context);

    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRES_NEW)
    @Async
    CompletableFuture<Boolean> asyncClean(AggregationContextVO context);
}

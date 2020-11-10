package net.sumaris.core.extraction.service;

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

import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.model.technical.extraction.IExtractionFormat;
import net.sumaris.core.extraction.vo.*;
import net.sumaris.core.extraction.vo.filter.AggregationTypeFilterVO;
import net.sumaris.core.vo.technical.extraction.ExtractionProductFetchOptions;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Create aggregation tables, from a data extraction.
 * @author benoit.lavenier@e-is.pro
 * @since 0.12.0
 */
@Transactional
public interface AggregationService {

    @Transactional(readOnly = true)
    AggregationTypeVO getByFormat(IExtractionFormat format);

    @Transactional(readOnly = true)
    List<AggregationTypeVO> findByFilter(@Nullable AggregationTypeFilterVO filter, ExtractionProductFetchOptions fetchOptions);

    @Transactional(readOnly = true)
    AggregationTypeVO get(int id, ExtractionProductFetchOptions fetchOptions);


    /**
     * Do an aggregate
     * @param type
     * @param filter
     */
    @Transactional
    AggregationContextVO execute(AggregationTypeVO type,
                                 @Nullable ExtractionFilterVO filter,
                                 @Nullable AggregationStrataVO strata);

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

    Map<String, Object> readTech(AggregationTypeVO format,
                                 ExtractionFilterVO filter,
                                 AggregationStrataVO strata,
                                 String sort,
                                 SortDirection direction);

    @Transactional(rollbackFor = IOException.class)
    File executeAndDump(AggregationTypeVO type,
                        @Nullable ExtractionFilterVO filter,
                        @Nullable AggregationStrataVO strata);

    @Transactional
    AggregationResultVO executeAndRead(AggregationTypeVO type,
                                       @Nullable ExtractionFilterVO filter,
                                       @Nullable AggregationStrataVO strata,
                                       int offset, int size,
                                       @Nullable String sort,
                                       @Nullable SortDirection direction);

    @Transactional
    AggregationTypeVO save(AggregationTypeVO type, @Nullable ExtractionFilterVO filter);

    @Transactional(isolation = Isolation.READ_UNCOMMITTED, propagation = Propagation.REQUIRES_NEW)
    void clean(AggregationContextVO context);

    @Transactional(propagation = Propagation.SUPPORTS)
    void asyncClean(AggregationContextVO context);
}

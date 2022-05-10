/*
 * #%L
 * SUMARiS
 * %%
 * Copyright (C) 2019 SUMARiS Consortium
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

package net.sumaris.extraction.core.dao;

import lombok.NonNull;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.exception.DataNotFoundException;
import net.sumaris.core.model.technical.extraction.IExtractionType;
import net.sumaris.core.vo.technical.extraction.AggregationStrataVO;
import net.sumaris.core.vo.technical.extraction.IExtractionTypeWithTablesVO;
import net.sumaris.extraction.core.vo.*;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nullable;
import java.util.Set;

/**
 * Create aggregation tables, from a data extraction.
 *
 * @author benoit.lavenier@e-is.pro
 * @since 0.12.0
 */

public interface AggregationDaoDispatcher {

    Set<IExtractionType> getManagedTypes();

    AggregationContextVO execute(@NonNull IExtractionType type,
                                 @NonNull IExtractionType source,
                                 @Nullable ExtractionFilterVO filter,
                                 AggregationStrataVO strata);

    @Transactional(readOnly = true, noRollbackFor = DataNotFoundException.class)
    AggregationResultVO read(IExtractionType type,
                             @Nullable ExtractionFilterVO filter,
                             @Nullable AggregationStrataVO strata,
                             Page page);

    @Transactional(readOnly = true)
    AggregationTechResultVO readByTech(IExtractionType type,
                                       @Nullable ExtractionFilterVO filter,
                                       @Nullable AggregationStrataVO strata,
                                       String sort,
                                       SortDirection direction);

    @Transactional(readOnly = true)
    MinMaxVO getTechMinMax(IExtractionType type,
                           @Nullable ExtractionFilterVO filter,
                           @Nullable AggregationStrataVO strata);

    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRES_NEW)
    void clean(AggregationContextVO context);
}

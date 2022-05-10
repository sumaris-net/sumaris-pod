package net.sumaris.extraction.core.dao;

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
import net.sumaris.core.model.technical.extraction.IExtractionType;
import net.sumaris.core.vo.technical.extraction.AggregationStrataVO;
import net.sumaris.core.vo.technical.extraction.IExtractionTypeWithStratumVO;
import net.sumaris.core.vo.technical.extraction.IExtractionTypeWithTablesVO;
import net.sumaris.extraction.core.vo.*;

import javax.annotation.Nullable;
import java.util.Set;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 */
public interface AggregationDao<
    C extends AggregationContextVO,
    F extends ExtractionFilterVO,
    S extends AggregationStrataVO> {

    String TABLE_NAME_PREFIX = "AGG_";

    Set<IExtractionType> getManagedTypes();

    <R extends C> R aggregate(IExtractionTypeWithTablesVO source,
                              @Nullable F filter,
                              S strata);

    AggregationResultVO read(String tableName, @Nullable F filter, S strata, Page page);

    AggregationTechResultVO readByTech(String tableName, @Nullable F filter, S strata, String sortAttribute, SortDirection direction);

    MinMaxVO getTechMinMax(String tableName, @Nullable F filter, S strata);

    <R extends AggregationContextVO> void clean(R context);
}

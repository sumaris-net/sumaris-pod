package net.sumaris.core.extraction.dao;

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
import net.sumaris.core.extraction.format.ProductFormatEnum;
import net.sumaris.core.extraction.vo.*;
import net.sumaris.core.vo.technical.extraction.AggregationStrataVO;
import net.sumaris.core.vo.technical.extraction.ExtractionProductVO;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 */
public interface AggregationDao<
    C extends AggregationContextVO,
    F extends ExtractionFilterVO,
    S extends AggregationStrataVO> {

    String TABLE_NAME_PREFIX = "AGG_";

    ProductFormatEnum getFormat();

    <R extends C> R aggregate(ExtractionProductVO source,
                              F filter,
                              S strata);

    AggregationResultVO getAggBySpace(String tableName, F filter, S strata, Page page);

    AggregationTechResultVO getAggByTech(String tableName, F filter, S strata, String sortAttribute, SortDirection direction);

    MinMaxVO getAggMinMaxByTech(String tableName, F filter, S strata);

    void clean(C context);
}

package net.sumaris.core.extraction.dao.trip.rdb;

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
import net.sumaris.core.extraction.dao.trip.AggregationTripDao;
import net.sumaris.core.extraction.vo.*;
import net.sumaris.core.extraction.vo.trip.rdb.AggregationRdbTripContextVO;
import net.sumaris.core.vo.technical.extraction.AggregationStrataVO;
import net.sumaris.core.vo.technical.extraction.ExtractionProductVO;

import java.util.Map;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 */
public interface AggregationRdbTripDao<
        C extends AggregationRdbTripContextVO,
        F extends ExtractionFilterVO,
        S extends AggregationStrataVO>
        extends AggregationTripDao {

    <R extends C> R aggregate(ExtractionProductVO source, F filter, S strata);

    AggregationResultVO getAggBySpace(String tableName, F filter, S strata, int offset, int size, String sortAttribute, SortDirection sortDirection);

    AggregationTechResultVO getAggByTech(String tableName, F filter, S strata, String sortAttribute, SortDirection direction);

    MinMaxVO getAggMinMaxByTech(String tableName, F filter, S strata);

    void clean(C context);

}

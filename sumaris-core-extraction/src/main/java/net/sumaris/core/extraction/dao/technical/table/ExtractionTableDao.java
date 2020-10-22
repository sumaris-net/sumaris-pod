package net.sumaris.core.extraction.dao.technical.table;

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
import net.sumaris.core.extraction.vo.ExtractionFilterVO;
import net.sumaris.core.extraction.vo.ExtractionResultVO;
import net.sumaris.core.vo.technical.extraction.ExtractionTableColumnVO;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 */
public interface ExtractionTableDao {

    enum SQLAggregatedFunction {
        SUM,
        AVG,
        COUNT,
        COUNT_DISTINCT
    }

    List<String> getAllTableNames();

    ExtractionResultVO getTable(String tableName);

    long getRowCount(String tableName);

    List<ExtractionTableColumnVO> getColumns(String tableName);

    ExtractionResultVO getTableRows(String tableName, ExtractionFilterVO filter, int offset, int size, String sort, SortDirection direction);

    ExtractionResultVO getTableGroupByRows(String tableName,
                                           ExtractionFilterVO filter,
                                           Set<String> groupByColumnNames,
                                           Map<String, SQLAggregatedFunction> otherColumnNames,
                                           int offset, int size, String sort, SortDirection direction);

    void dropTable(String tableName);

}

package net.sumaris.extraction.core.dao.technical.table;

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
import net.sumaris.extraction.core.vo.ExtractionFilterVO;
import net.sumaris.extraction.core.vo.ExtractionResultVO;
import net.sumaris.extraction.core.vo.MinMaxVO;
import net.sumaris.core.vo.technical.extraction.ExtractionTableColumnFetchOptions;
import net.sumaris.core.vo.technical.extraction.ExtractionTableColumnVO;

import javax.annotation.Nullable;
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

    List<ExtractionTableColumnVO> getColumns(String tableName, ExtractionTableColumnFetchOptions fetchOptions);

    ExtractionResultVO read(String tableName, ExtractionFilterVO filter, Page page);

    ExtractionResultVO readWithAggColumns(String tableName,
                                          @Nullable ExtractionFilterVO filter,
                                          Set<String> groupByColumnNames,
                                          Map<String, SQLAggregatedFunction> otherColumnNames,
                                          Page page);

    Map<String, Object> readAggColumnByTech(String tableName,
                                            @Nullable ExtractionFilterVO filter,
                                            String aggColumnName,
                                            SQLAggregatedFunction aggFunction,
                                            String techColumnName,
                                            String sort, SortDirection direction);

    MinMaxVO getTechMinMax(String tableName,
                           @Nullable ExtractionFilterVO filter,
                           Set<String> groupByColumns,
                           String aggColumnName,
                           SQLAggregatedFunction aggFunction,
                           String techColumnName);

    void dropTable(String tableName);

    /**
     * Create a sequence for a table.
     *
     * @param tableName
     * @return the sequence name
     */
    String createSequence(String tableName);

    void dropSequence(String sequenceName);

}

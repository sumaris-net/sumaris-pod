package net.sumaris.core.extraction.dao.technical.table;

import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.extraction.vo.ExtractionFilterVO;
import net.sumaris.core.extraction.vo.ExtractionResultVO;
import net.sumaris.core.vo.technical.extraction.ExtractionProductColumnVO;

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

    List<ExtractionProductColumnVO> getColumns(String tableName);

    ExtractionResultVO getTableRows(String tableName, ExtractionFilterVO filter, int offset, int size, String sort, SortDirection direction);

    ExtractionResultVO getTableGroupByRows(String tableName,
                                           ExtractionFilterVO filter,
                                           Set<String> groupByColumnNames,
                                           Map<String, SQLAggregatedFunction> otherColumnNames,
                                           int offset, int size, String sort, SortDirection direction);

    void dropTable(String tableName);

}

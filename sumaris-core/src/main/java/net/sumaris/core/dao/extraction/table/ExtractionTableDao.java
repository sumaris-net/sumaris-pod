package net.sumaris.core.dao.extraction.table;

import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.dao.technical.schema.DatabaseTableEnum;
import net.sumaris.core.vo.extraction.ExtractionFilterVO;
import net.sumaris.core.vo.extraction.ExtractionResultVO;

import java.util.List;

public interface ExtractionTableDao {

    String CATEGORY = "table";

    List<String> getAllTableNames();

    ExtractionResultVO getTable(DatabaseTableEnum table);

    ExtractionResultVO getTableRows(DatabaseTableEnum table, ExtractionFilterVO filter, int offset, int size, String sort, SortDirection direction);
}

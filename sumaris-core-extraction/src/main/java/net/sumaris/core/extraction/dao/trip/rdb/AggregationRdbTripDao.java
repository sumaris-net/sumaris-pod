package net.sumaris.core.extraction.dao.trip.rdb;

import com.google.common.collect.ImmutableMap;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.extraction.vo.*;
import net.sumaris.core.extraction.vo.trip.rdb.AggregationRdbTripContextVO;
import net.sumaris.core.model.technical.extraction.rdb.ProductRdbStation;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.technical.extraction.ExtractionProductVO;

import java.util.Map;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 */
public interface AggregationRdbTripDao<C extends AggregationRdbTripContextVO, F extends ExtractionFilterVO, S extends AggregationStrataVO> {

    String RDB_FORMAT = StringUtils.underscoreToChangeCase(ExtractionRawFormatEnum.RDB.name());

    String HH_SHEET_NAME = ExtractionRdbTripDao.HH_SHEET_NAME;
    String SL_SHEET_NAME = ExtractionRdbTripDao.SL_SHEET_NAME;
    String HL_SHEET_NAME = ExtractionRdbTripDao.HL_SHEET_NAME;
    String CA_SHEET_NAME = ExtractionRdbTripDao.CA_SHEET_NAME;


    String COLUMN_YEAR  = ProductRdbStation.COLUMN_YEAR;
    String COLUMN_QUARTER = "quarter";
    String COLUMN_MONTH = "month";

    String COLUMN_AREA = ProductRdbStation.COLUMN_AREA;
    String COLUMN_STATISTICAL_RECTANGLE = ProductRdbStation.COLUMN_STATISTICAL_RECTANGLE;
    String COLUMN_SQUARE = "square";

    ImmutableMap<String, String> COLUMN_ALIAS = ImmutableMap.<String, String>builder()
        .put("rect", COLUMN_STATISTICAL_RECTANGLE)
            .build();

    <R extends C> R aggregate(ExtractionProductVO source, F filter);

    AggregationResultVO read(String tableName, F filter, S strata, int offset, int size, String sortAttribute, SortDirection sortDirection);
}
package net.sumaris.core.extraction.vo.trip.rdb;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import net.sumaris.core.extraction.vo.trip.AggregationTripContextVO;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 */
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AggregationRdbTripContextVO extends AggregationTripContextVO {
    String tripTableName; // TR table
    String stationTableName; // HH table
    String speciesListTableName; // SL table
    String speciesLengthTableName; // HL table
    String sampleTableName; // CA table

    boolean enableAnalyze = true;
}

package net.sumaris.core.extraction.vo.live.trip.rdb;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import net.sumaris.core.extraction.vo.live.trip.ExtractionTripContextVO;

/**
 * @author peck7 on 17/12/2018.
 */
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ExtractionRdbTripContextVO extends ExtractionTripContextVO {

    String tripTableName; // TR table
    String stationTableName; // HH table
    String rawSpeciesListTableName; // SL-raw (not distinct) table
    String speciesListTableName; // SL table
    String speciesLengthTableName; // HL table
    String sampleTableName; // CA table

}

package net.sumaris.core.extraction.vo.live.trip.survivalTest;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import net.sumaris.core.extraction.vo.live.trip.rdb.ExtractionRdbTripContextVO;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 */
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ExtractionSurvivalTestContextVO extends ExtractionRdbTripContextVO {

    String survivalTestTableName; // ST (survival test) table

    String releaseTableName; // RL (release) table

}

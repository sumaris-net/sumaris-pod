package net.sumaris.core.extraction.vo;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>*
 */
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AggregationStrataVO {

    String space;
    String time;
    String tech;

    String techFunction;
}

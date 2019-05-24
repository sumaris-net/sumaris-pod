package net.sumaris.core.extraction.vo;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.List;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>*
 */
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AggregationTypeVO extends ExtractionTypeVO {

    @Data
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public class Strata {
        List<String> space;
        List<String> time;
        List<String> tech;
    }

    Strata strata;
}

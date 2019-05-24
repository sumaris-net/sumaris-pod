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
public class AggregationResultVO extends ExtractionResultVO {

    List<String> spaceStrata;
    List<String> timeStrata;
    List<String> techStrata;

    public AggregationResultVO() {
        super();
    }

    public <T extends ExtractionResultVO> AggregationResultVO(T source) {
        super(source);
    }
}

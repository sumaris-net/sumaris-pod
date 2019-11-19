package net.sumaris.core.extraction.vo;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import net.sumaris.core.model.technical.extraction.ExtractionProductStrata;
import net.sumaris.core.vo.technical.extraction.ExtractionProductStrataVO;

import java.util.Date;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>*
 */
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AggregationStrataVO extends ExtractionProductStrataVO {

}

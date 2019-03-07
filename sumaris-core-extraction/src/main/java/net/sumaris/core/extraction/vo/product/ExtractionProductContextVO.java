package net.sumaris.core.extraction.vo.product;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import net.sumaris.core.extraction.vo.ExtractionContextVO;

/**
 * @author Ludovic Pecquot <ludovic.pecquot>
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 */
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ExtractionProductContextVO extends ExtractionContextVO {

    ExtractionProduct product;

    @Override
    public String getLabel() {
        return product.name().toUpperCase();
    }
}

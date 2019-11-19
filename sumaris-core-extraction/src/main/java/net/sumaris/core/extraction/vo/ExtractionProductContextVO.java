package net.sumaris.core.extraction.vo;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import net.sumaris.core.vo.technical.extraction.ExtractionProductVO;

import java.util.Map;

/**
 * @author Ludovic Pecquot <ludovic.pecquot>
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 */
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ExtractionProductContextVO extends ExtractionContextVO {

    String label;

    public ExtractionProductContextVO(ExtractionProductVO product) {
        this(product.getLabel(), product.getItems());
    }

    public ExtractionProductContextVO(String label, Map<String, String> items) {
        super();
        this.label = label;
        items.entrySet().stream().forEach(e -> this.addTableName(e.getValue(), e.getKey()));
    }

    @Override
    public String getLabel() {
        return label;
    }

}

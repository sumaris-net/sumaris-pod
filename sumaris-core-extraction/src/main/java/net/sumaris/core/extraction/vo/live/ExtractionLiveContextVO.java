package net.sumaris.core.extraction.vo.live;

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
public abstract class ExtractionLiveContextVO extends ExtractionContextVO {

    String formatName;
    String formatVersion;

    @Override
    public String getLabel() {
        return formatName.toUpperCase();
    }
}

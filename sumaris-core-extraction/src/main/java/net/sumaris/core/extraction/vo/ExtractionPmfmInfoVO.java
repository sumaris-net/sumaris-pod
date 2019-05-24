package net.sumaris.core.extraction.vo;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

/**
 * @author peck7 on 18/12/2018.
 */
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ExtractionPmfmInfoVO {

    int pmfmId;

    /**
     * the table name owning the result and its extraction alias
     */
    String tableName;
    String alias;

    /**
     * program, acquisition level and rank order from the strategy
     */
    int programId;
    String acquisitionLevel;
    int rankOrder;
}

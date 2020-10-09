package net.sumaris.core.vo.filter;

import lombok.*;
import lombok.experimental.FieldNameConstants;

/**
 * @author peck7 on 24/08/2020.
 */
@Data
@FieldNameConstants
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StrategyFilterVO implements IReferentialFilter {

    private String label;
    private String name;

    private Integer[] statusIds;

    private Integer levelId;
    private Integer[] levelIds;

    private String searchJoin;
    private String searchText;
    private String searchAttribute;

    private Integer programId;

}

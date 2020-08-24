package net.sumaris.core.vo.filter;

import lombok.*;

/**
 * @author peck7 on 24/08/2020.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class StrategyFilterVO extends ReferentialFilterVO {

    private Integer programId;

    @Builder(builderMethodName = "strategyFilterBuilder")
    public StrategyFilterVO(String label, String name,
                             Integer[] statusIds, Integer levelId, Integer[] levelIds,
                             String searchJoin, String searchText, String searchAttribute,
                             Integer programId) {
        super(label, name, statusIds, levelId, levelIds, searchJoin, searchText, searchAttribute);
        this.programId = programId;
    }


}

package net.sumaris.core.vo.filter;

import lombok.Builder;
import lombok.Data;

/**
 * @author peck7 on 24/08/2020.
 */
@Data
@Builder
public class StrategyRelatedFilterVO {

    private Integer programId;
    private Integer strategyId;
    private Integer acquisitionLevelId;

}

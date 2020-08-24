package net.sumaris.core.dao.administration.programStrategy;

import net.sumaris.core.dao.referential.ReferentialRepository;
import net.sumaris.core.model.administration.programStrategy.Strategy;
import net.sumaris.core.vo.administration.programStrategy.StrategyFetchOptions;
import net.sumaris.core.vo.administration.programStrategy.StrategyVO;
import net.sumaris.core.vo.filter.StrategyFilterVO;

/**
 * @author peck7 on 24/08/2020.
 */
public interface StrategyRepository
    extends ReferentialRepository<Strategy, StrategyVO, StrategyFilterVO, StrategyFetchOptions>,
    StrategyRepositoryExtend {


}

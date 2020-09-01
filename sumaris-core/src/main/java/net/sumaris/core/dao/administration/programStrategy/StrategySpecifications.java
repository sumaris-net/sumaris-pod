package net.sumaris.core.dao.administration.programStrategy;

import net.sumaris.core.dao.technical.jpa.BindableSpecification;
import net.sumaris.core.model.administration.programStrategy.Program;
import net.sumaris.core.model.administration.programStrategy.Strategy;
import net.sumaris.core.vo.administration.programStrategy.StrategyVO;
import net.sumaris.core.vo.administration.programStrategy.TaxonGroupStrategyVO;
import net.sumaris.core.vo.administration.programStrategy.TaxonNameStrategyVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.ParameterExpression;
import java.util.List;

/**
 * @author peck7 on 24/08/2020.
 */
public interface StrategySpecifications {

    String PROGRAM_ID_PARAM = "programId";

    default Specification<Strategy> hasProgramId(Integer programId) {
        BindableSpecification<Strategy> specification = BindableSpecification.where((root, query, criteriaBuilder) -> {
            ParameterExpression<Integer> param = criteriaBuilder.parameter(Integer.class, PROGRAM_ID_PARAM);
            return criteriaBuilder.or(
                criteriaBuilder.isNull(param),
                criteriaBuilder.equal(root.get(Strategy.Fields.PROGRAM).get(Program.Fields.ID), param)
            );
        });
        specification.addBind(PROGRAM_ID_PARAM, programId);
        return specification;
    }

    List<StrategyVO> saveByProgramId(int programId, List<StrategyVO> sources);

    List<ReferentialVO> getGears(int strategyId);

    List<TaxonGroupStrategyVO> getTaxonGroupStrategies(int strategyId);

    List<TaxonNameStrategyVO> getTaxonNameStrategies(int strategyId);

}

package net.sumaris.core.dao.data.sample;

import net.sumaris.core.dao.data.RootDataSpecifications;
import net.sumaris.core.dao.technical.jpa.BindableSpecification;
import net.sumaris.core.dao.technical.model.IEntity;
import net.sumaris.core.model.data.Sample;
import net.sumaris.core.vo.data.SampleVO;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.ParameterExpression;
import java.util.List;

/**
 * @author peck7 on 01/09/2020.
 */
public interface SampleSpecifications extends RootDataSpecifications<Sample> {

    String OPERATION_ID_PARAM = "operationId";
    String LANDING_ID_PARAM = "landingId";

    default Specification<Sample> hasOperationId(Integer operationId) {
        BindableSpecification<Sample> specification = BindableSpecification.where((root, query, criteriaBuilder) -> {
            query.orderBy(criteriaBuilder.asc(root.get(Sample.Fields.RANK_ORDER)));
            ParameterExpression<Integer> param = criteriaBuilder.parameter(Integer.class, OPERATION_ID_PARAM);
            return criteriaBuilder.or(
                criteriaBuilder.isNull(param),
                criteriaBuilder.equal(root.get(Sample.Fields.OPERATION).get(IEntity.Fields.ID), param)
            );
        });
        specification.addBind(OPERATION_ID_PARAM, operationId);
        return specification;
    }

    default Specification<Sample> hasLandingId(Integer landingId) {
        BindableSpecification<Sample> specification = BindableSpecification.where((root, query, criteriaBuilder) -> {
            query.orderBy(criteriaBuilder.asc(root.get(Sample.Fields.RANK_ORDER)));
            ParameterExpression<Integer> param = criteriaBuilder.parameter(Integer.class, LANDING_ID_PARAM);
            return criteriaBuilder.or(
                criteriaBuilder.isNull(param),
                criteriaBuilder.equal(root.get(Sample.Fields.LANDING).get(IEntity.Fields.ID), param)
            );
        });
        specification.addBind(LANDING_ID_PARAM, landingId);
        return specification;
    }

    List<SampleVO> saveByOperationId(int operationId, List<SampleVO> samples);

    List<SampleVO> saveByLandingId(int landingId, List<SampleVO> samples);

}

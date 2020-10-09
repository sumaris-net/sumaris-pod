package net.sumaris.core.dao.data.operation;

import net.sumaris.core.dao.data.DataSpecifications;
import net.sumaris.core.dao.technical.jpa.BindableSpecification;
import net.sumaris.core.dao.technical.model.IEntity;
import net.sumaris.core.model.data.Operation;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.ParameterExpression;

/**
 * @author peck7 on 01/09/2020.
 */
public interface OperationSpecifications
    extends DataSpecifications<Operation> {

    String TRIP_ID_PARAM = "tripId";

    default Specification<Operation> hasTripId(Integer tripId) {
        BindableSpecification<Operation> specification = BindableSpecification.where((root, query, criteriaBuilder) -> {
            ParameterExpression<Integer> param = criteriaBuilder.parameter(Integer.class, TRIP_ID_PARAM);
            return criteriaBuilder.or(
                criteriaBuilder.isNull(param),
                criteriaBuilder.equal(root.get(Operation.Fields.TRIP).get(IEntity.Fields.ID), param)
            );
        });
        specification.addBind(TRIP_ID_PARAM, tripId);
        return specification;
    }


}

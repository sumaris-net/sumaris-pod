package net.sumaris.core.dao.data;

import net.sumaris.core.dao.technical.jpa.BindableSpecification;
import net.sumaris.core.dao.technical.model.IEntity;
import net.sumaris.core.model.data.IDataEntity;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.ParameterExpression;
import java.io.Serializable;

/**
 * @author peck7 on 28/08/2020.
 */
public interface DataSpecifications<E extends IDataEntity<? extends Serializable>> {

    String RECORDER_DEPARTMENT_ID_PARAM = "recorderDepartmentId";

    default Specification<E> hasRecorderDepartmentId(Integer recorderDepartmentId) {
        BindableSpecification<E> specification = BindableSpecification.where((root, query, criteriaBuilder) -> {
            ParameterExpression<Integer> param = criteriaBuilder.parameter(Integer.class, RECORDER_DEPARTMENT_ID_PARAM);
            return criteriaBuilder.or(
                criteriaBuilder.isNull(param),
                criteriaBuilder.equal(root.get(E.Fields.RECORDER_DEPARTMENT).get(IEntity.Fields.ID), param)
            );
        });
        specification.addBind(RECORDER_DEPARTMENT_ID_PARAM, recorderDepartmentId);
        return specification;
    }
}

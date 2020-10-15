package net.sumaris.core.dao.data;

import net.sumaris.core.dao.technical.jpa.BindableSpecification;
import net.sumaris.core.dao.technical.model.IEntity;
import net.sumaris.core.model.data.IRootDataEntity;
import net.sumaris.core.model.referential.IItemReferentialEntity;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.ParameterExpression;
import java.io.Serializable;

/**
 * @author peck7 on 28/08/2020.
 */
public interface RootDataSpecifications<E extends IRootDataEntity<? extends Serializable>> extends DataSpecifications<E> {

    String RECORDER_PERSON_ID_PARAM = "recorderPersonId";
    String PROGRAM_LABEL_PARAM = "programLabel";

    default Specification<E> hasRecorderPersonId(Integer recorderPersonId) {
        BindableSpecification<E> specification = BindableSpecification.where((root, query, criteriaBuilder) -> {
            ParameterExpression<Integer> param = criteriaBuilder.parameter(Integer.class, RECORDER_PERSON_ID_PARAM);
            return criteriaBuilder.or(
                criteriaBuilder.isNull(param),
                criteriaBuilder.equal(root.get(E.Fields.RECORDER_PERSON).get(IEntity.Fields.ID), param)
            );
        });
        specification.addBind(RECORDER_PERSON_ID_PARAM, recorderPersonId);
        return specification;
    }

    default Specification<E> hasProgramLabel(String programLabel) {
        BindableSpecification<E> specification = BindableSpecification.where((root, query, criteriaBuilder) -> {
            ParameterExpression<String> param = criteriaBuilder.parameter(String.class, PROGRAM_LABEL_PARAM);
            return criteriaBuilder.or(
                criteriaBuilder.isNull(param),
                criteriaBuilder.equal(root.get(E.Fields.PROGRAM).get(IItemReferentialEntity.Fields.LABEL), param)
            );
        });
        specification.addBind(PROGRAM_LABEL_PARAM, programLabel);
        return specification;
    }

}
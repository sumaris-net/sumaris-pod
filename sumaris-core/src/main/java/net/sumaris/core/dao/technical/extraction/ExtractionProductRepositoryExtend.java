package net.sumaris.core.dao.technical.extraction;

import net.sumaris.core.dao.technical.jpa.BindableSpecification;
import net.sumaris.core.dao.technical.model.IEntity;
import net.sumaris.core.model.technical.extraction.ExtractionProduct;
import net.sumaris.core.vo.technical.extraction.ExtractionProductColumnVO;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.ParameterExpression;
import java.util.List;

/**
 * @author peck7 on 21/08/2020.
 */
public interface ExtractionProductRepositoryExtend {

    String DEPARTMENT_ID_PARAM = "departmentId";

    default Specification<ExtractionProduct> withDepartmentId(Integer departmentId) {
        BindableSpecification<ExtractionProduct> specification = BindableSpecification.where((root, query, criteriaBuilder) -> {
            ParameterExpression<Integer> parameter = criteriaBuilder.parameter(Integer.class, DEPARTMENT_ID_PARAM);
            return criteriaBuilder.or(
                criteriaBuilder.isNull(parameter),
                criteriaBuilder.equal(root.get(ExtractionProduct.Fields.RECORDER_DEPARTMENT).get(IEntity.Fields.ID), parameter)
            );
        });
        specification.addBind(DEPARTMENT_ID_PARAM, departmentId);
        return specification;
    }


    List<ExtractionProductColumnVO> getColumnsByIdAndTableLabel(int id, String tableLabel);

}

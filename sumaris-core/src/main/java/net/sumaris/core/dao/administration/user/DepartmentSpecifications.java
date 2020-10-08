package net.sumaris.core.dao.administration.user;

import net.sumaris.core.dao.referential.ReferentialSpecifications;
import net.sumaris.core.dao.technical.jpa.BindableSpecification;
import net.sumaris.core.model.administration.user.Department;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.ParameterExpression;

/**
 * @author peck7 on 20/08/2020.
 */
public interface DepartmentSpecifications extends ReferentialSpecifications<Department> {

    String LOGO_PARAMETER = "logo";

    default Specification<Department> withLogo(Boolean withLogo) {
        BindableSpecification<Department> specification = BindableSpecification.where((root, query, criteriaBuilder) -> {
            ParameterExpression<Boolean> logoParam = criteriaBuilder.parameter(Boolean.class, LOGO_PARAMETER);
            return criteriaBuilder.or(
                criteriaBuilder.isNull(logoParam),
                criteriaBuilder.isNotNull(root.get(Department.Fields.LOGO))
            );
        });
        specification.addBind(LOGO_PARAMETER, Boolean.TRUE.equals(withLogo) ? true : null);
        return specification;
    }
}

package net.sumaris.core.dao.administration.programStrategy;

import net.sumaris.core.dao.technical.jpa.BindableSpecification;
import net.sumaris.core.model.administration.programStrategy.Program;
import net.sumaris.core.model.administration.programStrategy.ProgramProperty;
import net.sumaris.core.vo.administration.programStrategy.ProgramFetchOptions;
import net.sumaris.core.vo.administration.programStrategy.ProgramVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import net.sumaris.core.vo.referential.TaxonGroupVO;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.ParameterExpression;
import java.util.List;

/**
 * @author peck7 on 24/08/2020.
 */
public interface ProgramRepositoryExtend {

    String PROPERTY_LABEL_PARAM = "propertyLabel";

    default Specification<Program> hasProperty(String propertyLabel) {
        BindableSpecification<Program> specification = BindableSpecification.where((root, query, criteriaBuilder) -> {
            ParameterExpression<String> param = criteriaBuilder.parameter(String.class, PROPERTY_LABEL_PARAM);
            return criteriaBuilder.or(
                criteriaBuilder.isNull(param),
                criteriaBuilder.equal(root.join(Program.Fields.PROPERTIES, JoinType.LEFT).get(ProgramProperty.Fields.LABEL), param)
            );
        });
        specification.addBind(PROPERTY_LABEL_PARAM, propertyLabel);
        return specification;
    }

    ProgramVO toVO(Program source, ProgramFetchOptions fetchOptions);

    List<TaxonGroupVO> getTaxonGroups(int programId);

    List<ReferentialVO> getGears(int programId);

}

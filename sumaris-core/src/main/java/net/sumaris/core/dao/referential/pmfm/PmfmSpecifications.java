package net.sumaris.core.dao.referential.pmfm;

/*-
 * #%L
 * SUMARiS:: Core
 * %%
 * Copyright (C) 2018 - 2020 SUMARiS Consortium
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import net.sumaris.core.dao.technical.jpa.BindableSpecification;
import net.sumaris.core.model.referential.pmfm.*;
import net.sumaris.core.vo.referential.PmfmVO;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.ParameterExpression;
import java.util.List;

/**
 * @author peck7 on 19/08/2020.
 */
public interface PmfmSpecifications {

    String PARAMETER_ID_PARAM = "parameterId";
    String MATRIX_ID_PARAM = "matrixId";
    String FRACTION_ID_PARAM = "fractionId";
    String METHOD_ID_PARAM = "methodId";

    default Specification<Pmfm> hasPmfmPart(Parameter parameter, Matrix matrix, Fraction fraction, Method method) {
        BindableSpecification<Pmfm> specification = BindableSpecification.where((root, query, criteriaBuilder) -> {
            ParameterExpression<Integer> parameterParam = criteriaBuilder.parameter(Integer.class, PARAMETER_ID_PARAM);
            ParameterExpression<Integer> matrixParam = criteriaBuilder.parameter(Integer.class, MATRIX_ID_PARAM);
            ParameterExpression<Integer> fractionParam = criteriaBuilder.parameter(Integer.class, FRACTION_ID_PARAM);
            ParameterExpression<Integer> methodParam = criteriaBuilder.parameter(Integer.class, METHOD_ID_PARAM);
            return criteriaBuilder.and(
                    criteriaBuilder.or(
                            criteriaBuilder.isNull(parameterParam),
                            criteriaBuilder.equal(root.get(Pmfm.Fields.PARAMETER).get(Parameter.Fields.ID), parameterParam)
                    ),
                    criteriaBuilder.or(
                            criteriaBuilder.isNull(matrixParam),
                            criteriaBuilder.equal(root.get(Pmfm.Fields.MATRIX).get(Parameter.Fields.ID), matrixParam)
                    ),
                    criteriaBuilder.or(
                            criteriaBuilder.isNull(fractionParam),
                            criteriaBuilder.equal(root.get(Pmfm.Fields.FRACTION).get(Parameter.Fields.ID), fractionParam)
                    ),
                    criteriaBuilder.or(
                            criteriaBuilder.isNull(methodParam),
                            criteriaBuilder.equal(root.get(Pmfm.Fields.METHOD).get(Parameter.Fields.ID), methodParam)
                    )
            );
        });
        specification.addBind(PARAMETER_ID_PARAM, parameter != null ? parameter.getId() : null);
        specification.addBind(MATRIX_ID_PARAM, matrix != null ? matrix.getId() : null);
        specification.addBind(FRACTION_ID_PARAM, fraction != null ? fraction.getId() : null);
        specification.addBind(METHOD_ID_PARAM, method != null ? method.getId() : null);
        return specification;
    }

    List<Pmfm> findAll(Parameter parameter, Matrix matrix, Fraction fraction, Method method);

    boolean hasLabelPrefix(int pmfmId, String... labelPrefixes);

    boolean hasLabelSuffix(int pmfmId, String... labelSuffixes);

    boolean hasMatrixId(int pmfmId, int... matrixIds);

}

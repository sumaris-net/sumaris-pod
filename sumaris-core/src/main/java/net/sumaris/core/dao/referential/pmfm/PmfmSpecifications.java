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
import net.sumaris.core.model.referential.Status;
import net.sumaris.core.model.referential.StatusEnum;
import net.sumaris.core.model.referential.pmfm.*;
import net.sumaris.core.vo.administration.programStrategy.PmfmStrategyVO;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.ParameterExpression;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

/**
 * @author peck7 on 19/08/2020.
 */
public interface PmfmSpecifications {

    String STATUS_PARAMETER = "status";
    String STATUS_SET_PARAMETER = "statusSet";

    default Specification<Pmfm> hasPmfmPart(Integer parameterId, Integer matrixId, Integer fractionId, Integer methodId) {
        BindableSpecification<Pmfm> specification = BindableSpecification.where((root, query, criteriaBuilder) -> {
            ParameterExpression<Integer> parameterParam = criteriaBuilder.parameter(Integer.class, PmfmStrategyVO.Fields.PARAMETER_ID);
            ParameterExpression<Integer> matrixParam = criteriaBuilder.parameter(Integer.class, PmfmStrategyVO.Fields.MATRIX_ID);
            ParameterExpression<Integer> fractionParam = criteriaBuilder.parameter(Integer.class, PmfmStrategyVO.Fields.FRACTION_ID);
            ParameterExpression<Integer> methodParam = criteriaBuilder.parameter(Integer.class, PmfmStrategyVO.Fields.METHOD_ID);
            return criteriaBuilder.and(
                    criteriaBuilder.or(
                            criteriaBuilder.isNull(parameterParam),
                            criteriaBuilder.equal(root.get(Pmfm.Fields.PARAMETER).get(Parameter.Fields.ID), parameterParam)
                    ),
                    criteriaBuilder.or(
                            criteriaBuilder.isNull(matrixParam),
                            criteriaBuilder.equal(root.get(Pmfm.Fields.MATRIX).get(Matrix.Fields.ID), matrixParam)
                    ),
                    criteriaBuilder.or(
                            criteriaBuilder.isNull(fractionParam),
                            criteriaBuilder.equal(root.get(Pmfm.Fields.FRACTION).get(Fraction.Fields.ID), fractionParam)
                    ),
                    criteriaBuilder.or(
                            criteriaBuilder.isNull(methodParam),
                            criteriaBuilder.equal(root.get(Pmfm.Fields.METHOD).get(Method.Fields.ID), methodParam)
                    )
            );
        });
        specification.addBind(PmfmStrategyVO.Fields.PARAMETER_ID, parameterId);
        specification.addBind(PmfmStrategyVO.Fields.MATRIX_ID, matrixId);
        specification.addBind(PmfmStrategyVO.Fields.FRACTION_ID, fractionId);
        specification.addBind(PmfmStrategyVO.Fields.METHOD_ID, methodId);
        return specification;
    }

    default Specification<Pmfm> inStatusIds(StatusEnum... status) {
        Integer[] statusIds = Arrays.stream(status).map(StatusEnum::getId).toArray(Integer[]::new);
        return inStatusIds(statusIds);
    }

    default Specification<Pmfm> inStatusIds(Integer... statusIds) {
        BindableSpecification<Pmfm> specification = BindableSpecification.where((root, query, criteriaBuilder) -> {
            query.distinct(true); // Set distinct here because inStatusIds is always used (usually ...)
            ParameterExpression<Collection> statusParam = criteriaBuilder.parameter(Collection.class, STATUS_PARAMETER);
            ParameterExpression<Boolean> statusSetParam = criteriaBuilder.parameter(Boolean.class, STATUS_SET_PARAMETER);
            return criteriaBuilder.or(
                    criteriaBuilder.isFalse(statusSetParam),
                    criteriaBuilder.in(root.get(Pmfm.Fields.STATUS).get(Status.Fields.ID)).value(statusParam)
            );
        });
        specification.addBind(STATUS_SET_PARAMETER, ArrayUtils.isNotEmpty(statusIds));
        specification.addBind(STATUS_PARAMETER, ArrayUtils.isEmpty(statusIds) ? null : Arrays.asList(statusIds));
        return specification;
    }

    List<Pmfm> findByPmfmParts(Integer parameterId, Integer matrixId, Integer fractionId, Integer methodId);

    Stream<Pmfm> streamByPmfmParts(Integer parameterId, Integer matrixId, Integer fractionId, Integer methodId);

    boolean hasLabelPrefix(int pmfmId, String... labelPrefixes);

    boolean hasLabelSuffix(int pmfmId, String... labelSuffixes);

    boolean hasMatrixId(int pmfmId, int... matrixIds);

    boolean hasParameterGroupId(int pmfmId, int... parameterGroupIds);

    String computeCompleteName(int pmfmId);

}

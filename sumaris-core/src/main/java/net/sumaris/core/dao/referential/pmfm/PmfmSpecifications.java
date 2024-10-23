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

import net.sumaris.core.dao.referential.IEntityWithStatusSpecifications;
import net.sumaris.core.dao.technical.jpa.BindableSpecification;
import net.sumaris.core.model.referential.pmfm.*;
import net.sumaris.core.vo.filter.PmfmPartsVO;
import net.sumaris.core.vo.referential.pmfm.PmfmVO;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.ParameterExpression;
import java.util.List;
import java.util.stream.Stream;

/**
 * @author peck7 on 19/08/2020.
 */
public interface PmfmSpecifications extends IEntityWithStatusSpecifications<Integer, Pmfm> {

    default Specification<Pmfm> hasPmfmPart(PmfmPartsVO filter) {
        if (filter == null || filter.isEmpty()) return null; // Skip

        return BindableSpecification.where((root, query, cb) -> {
            ParameterExpression<Integer> parameterParam = cb.parameter(Integer.class, PmfmVO.Fields.PARAMETER_ID);
            ParameterExpression<Integer> matrixParam = cb.parameter(Integer.class, PmfmVO.Fields.MATRIX_ID);
            ParameterExpression<Integer> fractionParam = cb.parameter(Integer.class, PmfmVO.Fields.FRACTION_ID);
            ParameterExpression<Integer> methodParam = cb.parameter(Integer.class, PmfmVO.Fields.METHOD_ID);
            ParameterExpression<Integer> unitParam = cb.parameter(Integer.class, PmfmVO.Fields.UNIT_ID);
            return cb.and(
                    cb.or(
                            cb.isNull(parameterParam),
                            cb.equal(root.get(Pmfm.Fields.PARAMETER).get(Parameter.Fields.ID), parameterParam)
                    ),
                    cb.or(
                            cb.isNull(matrixParam),
                            cb.equal(root.get(Pmfm.Fields.MATRIX).get(Matrix.Fields.ID), matrixParam)
                    ),
                    cb.or(
                            cb.isNull(fractionParam),
                            cb.equal(root.get(Pmfm.Fields.FRACTION).get(Fraction.Fields.ID), fractionParam)
                    ),
                    cb.or(
                            cb.isNull(methodParam),
                            cb.equal(root.get(Pmfm.Fields.METHOD).get(Method.Fields.ID), methodParam)
                    ),
                    cb.or(
                        cb.isNull(unitParam),
                        cb.equal(root.get(Pmfm.Fields.UNIT).get(Method.Fields.ID), unitParam)
                    )
            );
        })
        .addBind(PmfmVO.Fields.PARAMETER_ID, filter.getParameterId())
        .addBind(PmfmVO.Fields.MATRIX_ID, filter.getMatrixId())
        .addBind(PmfmVO.Fields.FRACTION_ID, filter.getFractionId())
        .addBind(PmfmVO.Fields.METHOD_ID, filter.getMethodId())
        .addBind(PmfmVO.Fields.UNIT_ID, filter.getUnitId());
    }


    List<Integer> findIdsByParts(PmfmPartsVO parts);

    Stream<Pmfm> streamAllByParts(PmfmPartsVO parts);

    boolean hasLabelPrefix(int pmfmId, String... labelPrefixes);

    boolean hasLabelSuffix(int pmfmId, String... labelSuffixes);

    boolean hasMatrixId(int pmfmId, int... matrixIds);

    boolean hasParameterGroupId(int pmfmId, int... parameterGroupIds);

    String computeCompleteName(int pmfmId);

}

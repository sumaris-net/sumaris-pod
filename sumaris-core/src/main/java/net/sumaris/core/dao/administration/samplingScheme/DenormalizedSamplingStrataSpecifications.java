/*
 * #%L
 * SUMARiS
 * %%
 * Copyright (C) 2019 SUMARiS Consortium
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

package net.sumaris.core.dao.administration.samplingScheme;

import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.dao.technical.jpa.BindableSpecification;
import net.sumaris.core.model.administration.programStrategy.Program;
import net.sumaris.core.model.administration.samplingScheme.DenormalizedSamplingStrata;
import net.sumaris.core.vo.administration.samplingScheme.DenormalizedSamplingStrataFetchOptions;
import net.sumaris.core.vo.administration.samplingScheme.DenormalizedSamplingStrataVO;
import net.sumaris.core.vo.administration.samplingScheme.SamplingStrataFilterVO;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.ParameterExpression;
import java.util.List;

public interface DenormalizedSamplingStrataSpecifications {

    default Specification<DenormalizedSamplingStrata> programId(Integer programId) {
        if (programId == null) return null;
        return BindableSpecification.where((root, query, cb) -> {
                ParameterExpression<Integer> param = cb.parameter(Integer.class, SamplingStrataFilterVO.Fields.PROGRAM_ID);
                return cb.equal(root.get(DenormalizedSamplingStrataVO.Fields.PROGRAM).get(Program.Fields.ID), param);
            })
            .addBind(SamplingStrataFilterVO.Fields.PROGRAM_ID, programId);
    }

    default Specification<DenormalizedSamplingStrata> programLabel(String programLabel) {
        if (programLabel == null) return null;
        return BindableSpecification.where((root, query, cb) -> {
                ParameterExpression<String> param = cb.parameter(String.class, SamplingStrataFilterVO.Fields.PROGRAM_LABEL);
                return cb.equal(root.get(DenormalizedSamplingStrataVO.Fields.PROGRAM).get(Program.Fields.LABEL), param);
            })
            .addBind(SamplingStrataFilterVO.Fields.PROGRAM_LABEL, programLabel);
    }

    List<DenormalizedSamplingStrataVO> findAll(SamplingStrataFilterVO filter, Page page, DenormalizedSamplingStrataFetchOptions fetchOptions);
}

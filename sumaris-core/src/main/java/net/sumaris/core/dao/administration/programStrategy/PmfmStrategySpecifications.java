package net.sumaris.core.dao.administration.programStrategy;

/*-
 * #%L
 * SUMARiS:: Core
 * %%
 * Copyright (C) 2018 SUMARiS Consortium
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
import net.sumaris.core.model.administration.programStrategy.AcquisitionLevel;
import net.sumaris.core.model.administration.programStrategy.PmfmStrategy;
import net.sumaris.core.model.administration.programStrategy.Program;
import net.sumaris.core.model.administration.programStrategy.Strategy;
import net.sumaris.core.vo.filter.PmfmStrategyFilterVO;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.ParameterExpression;

public interface PmfmStrategySpecifications {

    String PROGRAM_ID_PARAM = "programId";
    String ACQUISITION_LEVEL_ID_PARAM = "acquisitionLevelId";
    String STRATEGY_ID_PARAM = "strategyId";

    default Specification<PmfmStrategy> hasProgramId(Integer programId) {
        if (programId == null) return null;
        return BindableSpecification.where((root, query, criteriaBuilder) -> {
            ParameterExpression<Integer> param = criteriaBuilder.parameter(Integer.class, PROGRAM_ID_PARAM);
            return criteriaBuilder.equal(root.get(PmfmStrategy.Fields.STRATEGY).get(Strategy.Fields.PROGRAM).get(Program.Fields.ID), param);
        }).addBind(PROGRAM_ID_PARAM, programId);
    }

    default Specification<PmfmStrategy> hasAcquisitionLevelId(Integer acquisitionLevelId) {
        if (acquisitionLevelId == null) return null;
        return BindableSpecification.where((root, query, criteriaBuilder) -> {
            ParameterExpression<Integer> param = criteriaBuilder.parameter(Integer.class, ACQUISITION_LEVEL_ID_PARAM);
            return criteriaBuilder.equal(root.get(PmfmStrategy.Fields.ACQUISITION_LEVEL).get(AcquisitionLevel.Fields.ID), param);
        }).addBind(ACQUISITION_LEVEL_ID_PARAM, acquisitionLevelId);
    }

    default Specification<PmfmStrategy> hasStrategyId(Integer strategyId) {
        if (strategyId == null) return null;
        return BindableSpecification.where((root, query, criteriaBuilder) -> {
            ParameterExpression<Integer> param = criteriaBuilder.parameter(Integer.class, STRATEGY_ID_PARAM);
            return criteriaBuilder.equal(root.get(PmfmStrategy.Fields.STRATEGY).get(Strategy.Fields.ID), param);
        }).addBind(STRATEGY_ID_PARAM, strategyId);
    }

    default Specification<PmfmStrategy> toSpecification(PmfmStrategyFilterVO filter) {

        return BindableSpecification
                .where(hasProgramId(filter.getProgramId()))
                .and(hasStrategyId(filter.getStrategyId()))
                .and(hasAcquisitionLevelId(filter.getAcquisitionLevelId()));
    }

}

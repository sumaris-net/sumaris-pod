package net.sumaris.core.dao.administration.programStrategy;

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
import net.sumaris.core.model.administration.programStrategy.Program;
import net.sumaris.core.model.administration.programStrategy.Strategy;
import net.sumaris.core.vo.administration.programStrategy.*;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.ParameterExpression;
import java.util.List;

/**
 * @author peck7 on 24/08/2020.
 */
public interface StrategySpecifications {

    String PROGRAM_ID_PARAM = "programId";

    default Specification<Strategy> hasProgramId(Integer programId) {
        BindableSpecification<Strategy> specification = BindableSpecification.where((root, query, criteriaBuilder) -> {
            ParameterExpression<Integer> param = criteriaBuilder.parameter(Integer.class, PROGRAM_ID_PARAM);
            return criteriaBuilder.or(
                criteriaBuilder.isNull(param),
                criteriaBuilder.equal(root.get(Strategy.Fields.PROGRAM).get(Program.Fields.ID), param)
            );
        });
        specification.addBind(PROGRAM_ID_PARAM, programId);
        return specification;
    }

    List<StrategyVO> saveByProgramId(int programId, List<StrategyVO> sources);

    List<ReferentialVO> getGears(int strategyId);

    List<TaxonGroupStrategyVO> getTaxonGroupStrategies(int strategyId);

    List<TaxonNameStrategyVO> getTaxonNameStrategies(int strategyId);

    List<AppliedStrategyVO> getAppliedStrategies(int strategyId);

    List<StrategyDepartmentVO> getStrategyDepartments(int strategyId);

    List<TaxonGroupStrategyVO> saveTaxonGroupStrategiesByStrategyId(int strategyId, List<TaxonGroupStrategyVO> sources);

    List<TaxonNameStrategyVO> saveReferenceTaxonStrategiesByStrategyId(int strategyId, List<TaxonNameStrategyVO> sources);

    List<AppliedStrategyVO> saveAppliedStrategiesByStrategyId(int strategyId, List<AppliedStrategyVO> sources);

    List<StrategyDepartmentVO> saveStrategyDepartmentsByStrategyId(int strategyId, List<StrategyDepartmentVO> sources);

    void saveProgramLocationsByStrategyId(int strategyId);

}

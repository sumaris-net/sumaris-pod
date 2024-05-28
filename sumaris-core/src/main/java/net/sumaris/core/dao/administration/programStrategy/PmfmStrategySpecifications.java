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

import net.sumaris.core.dao.referential.ReferentialEntities;
import net.sumaris.core.dao.technical.Daos;
import net.sumaris.core.dao.technical.jpa.BindableSpecification;
import net.sumaris.core.model.administration.programStrategy.AcquisitionLevel;
import net.sumaris.core.model.administration.programStrategy.PmfmStrategy;
import net.sumaris.core.model.administration.programStrategy.Program;
import net.sumaris.core.model.administration.programStrategy.Strategy;
import net.sumaris.core.model.referential.IItemReferentialEntity;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.administration.programStrategy.PmfmStrategyFetchOptions;
import net.sumaris.core.vo.filter.PmfmStrategyFilterVO;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.ParameterExpression;
import java.util.Arrays;
import java.util.Collection;

public interface PmfmStrategySpecifications {

    String PROGRAM_ID_PARAM = "programId";
    String PROGRAM_LABELS_PARAM = "programLabels";
    String ACQUISITION_LEVEL_ID_PARAM = "acquisitionLevelId";
    String ACQUISITION_LEVEL_IDS_PARAM = "acquisitionLevelIds";
    String STRATEGY_ID_PARAM = "strategyId";

    default Specification<PmfmStrategy> hasProgramId(Integer programId) {
        if (programId == null) return null;
        return BindableSpecification.where((root, query, cb) -> {
            ParameterExpression<Integer> param = cb.parameter(Integer.class, PROGRAM_ID_PARAM);
            Join<?, Program> programJoin = Daos.composeJoin(root, StringUtils.doting(PmfmStrategy.Fields.STRATEGY, Strategy.Fields.PROGRAM));
            return cb.equal(programJoin.get(Program.Fields.ID), param);
        }).addBind(PROGRAM_ID_PARAM, programId);
    }

    default Specification<PmfmStrategy> inProgramLabels(String[] programLabels) {
        // If empty: skip to avoid an unused join
        if (ArrayUtils.isEmpty(programLabels)) return null;

        return BindableSpecification.where((root, query, cb) -> {
            ParameterExpression<Collection> param = cb.parameter(Collection.class, PROGRAM_LABELS_PARAM);
            Join<?, Program> programJoin = Daos.composeJoin(root, StringUtils.doting(PmfmStrategy.Fields.STRATEGY, Strategy.Fields.PROGRAM));
            return cb.in(programJoin.get(Program.Fields.LABEL)).value(param);
        }).addBind(PROGRAM_LABELS_PARAM, Arrays.asList(programLabels));
    }

    default Specification<PmfmStrategy> hasAcquisitionLevelId(Integer acquisitionLevelId) {
        if (acquisitionLevelId == null) return null;
        return BindableSpecification.where((root, query, cb) -> {
            ParameterExpression<Integer> param = cb.parameter(Integer.class, ACQUISITION_LEVEL_ID_PARAM);
            return cb.equal(root.get(PmfmStrategy.Fields.ACQUISITION_LEVEL).get(AcquisitionLevel.Fields.ID), param);
        }).addBind(ACQUISITION_LEVEL_ID_PARAM, acquisitionLevelId);
    }


    default Specification<PmfmStrategy> inAcquisitionLevelIds(Integer[] acquisitionLevelIds) {
        // If empty: skip to avoid an unused join
        if (ArrayUtils.isEmpty(acquisitionLevelIds)) return null;
        return BindableSpecification.where((root, query, cb) -> {
            ParameterExpression<Collection> param = cb.parameter(Collection.class, ACQUISITION_LEVEL_IDS_PARAM);
            return cb.in(root.get(PmfmStrategy.Fields.ACQUISITION_LEVEL).get(AcquisitionLevel.Fields.ID)).value(param);
        }).addBind(ACQUISITION_LEVEL_IDS_PARAM, Arrays.asList(acquisitionLevelIds));
    }

    default Specification<PmfmStrategy> hasStrategyId(Integer strategyId) {
        if (strategyId == null) return null;
        return BindableSpecification.where((root, query, cb) -> {
            ParameterExpression<Integer> param = cb.parameter(Integer.class, STRATEGY_ID_PARAM);
            Join<?, Strategy> strategyJoin = Daos.composeJoin(root, PmfmStrategy.Fields.STRATEGY);
            return cb.equal(strategyJoin.get(Strategy.Fields.ID), param);
        }).addBind(STRATEGY_ID_PARAM, strategyId);
    }

    default Specification<PmfmStrategy> toSpecification(PmfmStrategyFilterVO filter, PmfmStrategyFetchOptions fetchOptions) {
        return BindableSpecification
            .where(hasProgramId(filter.getProgramId()))
            .and(inProgramLabels(filter.getProgramLabels()))
            .and(hasStrategyId(filter.getStrategyId()))
            .and(hasAcquisitionLevelId(filter.getAcquisitionLevelId()))
            .and(inAcquisitionLevelIds(filter.getAcquisitionLevelIds()))
            ;
    }

}

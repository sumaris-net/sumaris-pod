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

import net.sumaris.core.dao.referential.ReferentialSpecifications;
import net.sumaris.core.dao.technical.jpa.BindableSpecification;
import net.sumaris.core.model.administration.programStrategy.*;
import net.sumaris.core.model.referential.Status;
import net.sumaris.core.model.referential.taxon.ReferenceTaxon;
import net.sumaris.core.vo.administration.programStrategy.*;
import net.sumaris.core.vo.filter.StrategyFilterVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * @author peck7 on 24/08/2020.
 */
public interface StrategySpecifications extends ReferentialSpecifications<Strategy> {

    String REFERENCE_TAXON_IDS = "referenceTaxonIds";
    String UPDATE_DATE_GREATER_THAN_PARAM = "updateDateGreaterThan";

    default Specification<Strategy> hasProgramIds(Integer... programIds) {
        return inLevelIds(Strategy.class, programIds);
    }

    default Specification<Strategy> newerThan(Date updateDate) {
        BindableSpecification<Strategy> specification = BindableSpecification.where((root, query, criteriaBuilder) -> {
            ParameterExpression<Date> updateDateParam = criteriaBuilder.parameter(Date.class, UPDATE_DATE_GREATER_THAN_PARAM);
            return criteriaBuilder.greaterThan(root.get(Strategy.Fields.UPDATE_DATE), updateDateParam);
        });
        specification.addBind(UPDATE_DATE_GREATER_THAN_PARAM, updateDate);
        return specification;
    }

    default Specification<Strategy> hasReferenceTaxonIds(Integer... referenceTaxonIds) {
        if (ArrayUtils.isEmpty(referenceTaxonIds)) return null;
        BindableSpecification<Strategy> specification = BindableSpecification.where((root, query, criteriaBuilder) -> {

            // Avoid duplictaed entries (because of inner join)
            query.distinct(true);

            ParameterExpression<Collection> referenceTaxonIdsParam = criteriaBuilder.parameter(Collection.class, REFERENCE_TAXON_IDS);
            return criteriaBuilder.in(
                root.join(Strategy.Fields.REFERENCE_TAXONS, JoinType.INNER)
                    .join(ReferenceTaxonStrategy.Fields.REFERENCE_TAXON, JoinType.INNER)
                    .get(ReferenceTaxon.Fields.ID))
                .value(referenceTaxonIdsParam);
        });
        specification.addBind(REFERENCE_TAXON_IDS, Arrays.asList(referenceTaxonIds));
        return specification;
    }

    default Specification<Strategy> betweenDate(Date startDate, Date endDate) {
        if (startDate == null && endDate == null) return null;
        return (root, query, cb) -> {

            Join<?,?> appliedPeriods = root.join(Strategy.Fields.APPLIED_STRATEGIES, JoinType.LEFT)
                    .join(AppliedStrategy.Fields.APPLIED_PERIODS, JoinType.LEFT);

            // Start + end date
            if (startDate != null && endDate != null) {
                return cb.not(
                    cb.or(
                        cb.greaterThan(appliedPeriods.get(AppliedPeriod.Fields.START_DATE), endDate),
                        cb.lessThan(appliedPeriods.get(AppliedPeriod.Fields.END_DATE), startDate)
                    )
                );
            }
            // Start date
            else if (startDate != null) {
                return cb.greaterThanOrEqualTo(appliedPeriods.get(AppliedPeriod.Fields.END_DATE), startDate);
            }
            // End date
            else {
                return cb.lessThanOrEqualTo(appliedPeriods.get(AppliedPeriod.Fields.START_DATE), endDate);
            }
        };
    }

    List<StrategyVO> saveByProgramId(int programId, List<StrategyVO> sources);

    List<ReferentialVO> getGears(int strategyId);

    List<TaxonGroupStrategyVO> getTaxonGroupStrategies(int strategyId);

    List<TaxonNameStrategyVO> getTaxonNameStrategies(int strategyId);

    List<AppliedStrategyVO> getAppliedStrategies(int strategyId);

    List<StrategyDepartmentVO> getDepartmentsById(int strategyId);

    List<TaxonGroupStrategyVO> saveTaxonGroupStrategiesByStrategyId(int strategyId, List<TaxonGroupStrategyVO> sources);

    List<TaxonNameStrategyVO> saveReferenceTaxonStrategiesByStrategyId(int strategyId, List<TaxonNameStrategyVO> sources);

    List<AppliedStrategyVO> saveAppliedStrategiesByStrategyId(int strategyId, List<AppliedStrategyVO> sources);

    List<StrategyDepartmentVO> saveDepartmentsByStrategyId(int strategyId, List<StrategyDepartmentVO> sources);

    String computeNextLabelByProgramId(int programId, String labelPrefix, int nbDigit);

    List<StrategyVO> findNewerByProgramId(final int programId, final Date updateDate, final StrategyFetchOptions fetchOptions);

    void saveProgramLocationsByStrategyId(int strategyId);

    boolean hasUserPrivilege(int strategyId, int personId, ProgramPrivilegeEnum privilege);

    boolean hasDepartmentPrivilege(int strategyId, int departmentId, ProgramPrivilegeEnum privilege);
}

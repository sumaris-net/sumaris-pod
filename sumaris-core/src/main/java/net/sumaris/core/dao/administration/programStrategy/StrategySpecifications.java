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
import net.sumaris.core.model.administration.user.Department;
import net.sumaris.core.model.referential.location.Location;
import net.sumaris.core.model.referential.pmfm.Parameter;
import net.sumaris.core.model.referential.pmfm.Pmfm;
import net.sumaris.core.model.referential.taxon.ReferenceTaxon;
import net.sumaris.core.vo.administration.programStrategy.*;
import net.sumaris.core.vo.filter.PeriodVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Predicate;
import java.util.*;

/**
 * @author peck7 on 24/08/2020.
 */
public interface StrategySpecifications extends ReferentialSpecifications<Integer, Strategy> {

    String ANALYTIC_REFERENCES = "analyticReferences";
    String REFERENCE_TAXON_IDS = "referenceTaxonIds";
    String DEPARTMENT_IDS = "departmentIds";
    String LOCATION_IDS = "locationIds";
    String PARAMETER_IDS = "parameterIds";
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

    default Specification<Strategy> hasAnalyticReferences(String... analyticReferences) {
        if (ArrayUtils.isEmpty(analyticReferences)) return null;

        return BindableSpecification.where((root, query, criteriaBuilder) -> {
            ParameterExpression<String> parameter = criteriaBuilder.parameter(String.class, ANALYTIC_REFERENCES);
            return criteriaBuilder.in(
                    root.get(Strategy.Fields.ANALYTIC_REFERENCE))
                    .value(parameter);
        }).addBind(ANALYTIC_REFERENCES, Arrays.asList(analyticReferences));
    }

    default Specification<Strategy> hasReferenceTaxonIds(Integer... referenceTaxonIds) {
        if (ArrayUtils.isEmpty(referenceTaxonIds)) return null;

        return BindableSpecification.where((root, query, criteriaBuilder) -> {

            // Avoid duplicated entries (because of inner join)
            query.distinct(true);

            ParameterExpression<Collection> parameter = criteriaBuilder.parameter(Collection.class, REFERENCE_TAXON_IDS);
            return criteriaBuilder.in(
                    root.join(Strategy.Fields.REFERENCE_TAXONS, JoinType.INNER)
                            .join(ReferenceTaxonStrategy.Fields.REFERENCE_TAXON, JoinType.INNER)
                            .get(ReferenceTaxon.Fields.ID))
                    .value(parameter);
        })
            .addBind(REFERENCE_TAXON_IDS, Arrays.asList(referenceTaxonIds));
    }

    default Specification<Strategy> hasDepartmentIds(Integer... departmentIds) {
        if (ArrayUtils.isEmpty(departmentIds)) return null;
        return BindableSpecification.where((root, query, criteriaBuilder) -> {

            // Avoid duplicated entries (because of inner join)
            query.distinct(true);

            ParameterExpression<Collection> parameter = criteriaBuilder.parameter(Collection.class, DEPARTMENT_IDS);
            return criteriaBuilder.in(
                    root.join(Strategy.Fields.DEPARTMENTS, JoinType.INNER)
                            .join(StrategyDepartment.Fields.DEPARTMENT, JoinType.INNER)
                            .get(Department.Fields.ID))
                    .value(parameter);
        }).addBind(DEPARTMENT_IDS, Arrays.asList(departmentIds));
    }

    default Specification<Strategy> hasLocationIds(Integer... locationIds) {
        if (ArrayUtils.isEmpty(locationIds)) return null;

        return BindableSpecification.where((root, query, criteriaBuilder) -> {

            // Avoid duplicated entries (because of inner join)
            query.distinct(true);

            ParameterExpression<Collection> parameter = criteriaBuilder.parameter(Collection.class, LOCATION_IDS);
            return criteriaBuilder.in(
                    root.join(Strategy.Fields.APPLIED_STRATEGIES, JoinType.INNER)
                            .join(AppliedStrategy.Fields.LOCATION, JoinType.INNER)
                            .get(Location.Fields.ID))
                    .value(parameter);
        }).addBind(LOCATION_IDS, Arrays.asList(locationIds));
    }

    default Specification<Strategy> hasParameterIds(Integer... parameterIds) {
        if (ArrayUtils.isEmpty(parameterIds)) return null;
        return BindableSpecification.where((root, query, criteriaBuilder) -> {

            // Avoid duplicated entries (because of inner join)
            query.distinct(true);

            Join<Strategy, PmfmStrategy> pmfmsInnerJoin = root.joinList(Strategy.Fields.PMFMS, JoinType.INNER);

            ParameterExpression<Collection> parameter = criteriaBuilder.parameter(Collection.class, PARAMETER_IDS);
            return criteriaBuilder.or(
                    criteriaBuilder.in(
                            pmfmsInnerJoin
                                    .join(PmfmStrategy.Fields.PMFM, JoinType.LEFT)
                                    .join(Pmfm.Fields.PARAMETER, JoinType.LEFT)
                                    .get(Parameter.Fields.ID))
                            .value(parameter),
                    criteriaBuilder.in(
                            pmfmsInnerJoin
                                    .join(PmfmStrategy.Fields.PARAMETER, JoinType.LEFT)
                                    .get(Parameter.Fields.ID))
                            .value(parameter)
            );
        }).addBind(PARAMETER_IDS, Arrays.asList(parameterIds));
    }

    default Specification<Strategy> hasPeriods(PeriodVO... periods) {
        if (ArrayUtils.isEmpty(periods)) return null;
        return (root, query, cb) -> {
            final List<Predicate> predicates = new ArrayList<Predicate>();

            Join<?, ?> appliedPeriods = root.join(Strategy.Fields.APPLIED_STRATEGIES, JoinType.LEFT)
                    .join(AppliedStrategy.Fields.APPLIED_PERIODS, JoinType.LEFT);

            for (PeriodVO dates : periods) {
                Date startDate = dates.getStartDate();
                Date endDate = dates.getEndDate();

                // Start + end date
                if (startDate != null && endDate != null) {
                    predicates.add(
                            cb.not(
                                    cb.or(
                                            cb.greaterThan(appliedPeriods.get(AppliedPeriod.Fields.START_DATE), endDate),
                                            cb.lessThan(appliedPeriods.get(AppliedPeriod.Fields.END_DATE), startDate)
                                    )
                            )
                    );
                }

                // Start date
                else if (startDate != null) {
                    predicates.add(
                            cb.greaterThanOrEqualTo(appliedPeriods.get(AppliedPeriod.Fields.END_DATE), startDate)
                    );
                }

                // End date
                else if (endDate != null) {
                    predicates.add(
                            cb.lessThanOrEqualTo(appliedPeriods.get(AppliedPeriod.Fields.START_DATE), endDate)
                    );
                }
            }

            if (CollectionUtils.isEmpty(predicates)) return null;
            return cb.or(predicates.toArray(new Predicate[predicates.size()]));
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

    String computeNextSampleLabelByStrategy(String strategyLabel, String labelSeparator, int nbDigit);

    List<StrategyVO> findNewerByProgramId(final int programId, final Date updateDate, final StrategyFetchOptions fetchOptions);

    void saveProgramLocationsByStrategyId(int strategyId);

    boolean hasUserPrivilege(int strategyId, int personId, ProgramPrivilegeEnum privilege);

    boolean hasDepartmentPrivilege(int strategyId, int departmentId, ProgramPrivilegeEnum privilege);

}

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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import net.sumaris.core.dao.referential.ReferentialDao;
import net.sumaris.core.dao.referential.ReferentialEntities;
import net.sumaris.core.dao.referential.ReferentialExternalDao;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.dao.technical.hibernate.HibernateDaoSupport;
import net.sumaris.core.model.IEntity;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.model.administration.programStrategy.*;
import net.sumaris.core.model.administration.user.Department;
import net.sumaris.core.model.referential.IItemReferentialEntity;
import net.sumaris.core.model.referential.IReferentialEntity;
import net.sumaris.core.model.referential.Status;
import net.sumaris.core.model.referential.StatusEnum;
import net.sumaris.core.model.referential.location.Location;
import net.sumaris.core.model.referential.location.LocationClassificationEnum;
import net.sumaris.core.model.referential.location.LocationLevel;
import net.sumaris.core.model.referential.taxon.ReferenceTaxon;
import net.sumaris.core.model.referential.taxon.TaxonName;
import net.sumaris.core.vo.filter.ReferentialFilterVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import java.util.List;
import java.util.stream.Collectors;

@Repository("strategyPredocDao")
public class StrategyPredocDaoImpl extends HibernateDaoSupport implements StrategyPredocDao {

    @Autowired
    private ReferentialDao referentialDao;

    @Autowired
    protected ReferentialExternalDao referentialExternalDao;

    @Override
    public List<ReferentialVO> findStrategiesReferentials(final String entityName,
                                                          int programId,
                                                          LocationClassificationEnum locationClassification,
                                                          int offset,
                                                          int size,
                                                          String sortAttribute,
                                                          SortDirection sortDirection) {
        Preconditions.checkNotNull(entityName, "Missing 'entityName' argument");

        // Special case: AnalyticReference
        if (entityName.equalsIgnoreCase(Strategy.Fields.ANALYTIC_REFERENCE)) {
            List<String> labels = findStrategiesAnalyticReferences(programId);
            return findAnalyticReferencesByLabels(labels, offset, size, sortAttribute, sortDirection);
        }

        // Get entity class from entityName
        Class<? extends IReferentialEntity> entityClass = ReferentialEntities.getEntityClass(entityName);

        // switch entityName
        List<Integer> entityIds = switch (entityName) {
            case "Department" -> findStrategiesDepartments(programId);
            case "Location" -> findStrategiesLocations(programId, locationClassification);
            case "TaxonName" -> findStrategiesTaxonNames(programId);
            case "Pmfm" -> findStrategiesPmfms(programId, null, PmfmStrategy.Fields.PMFM);
            case "Parameter" -> findStrategiesPmfms(programId, null, PmfmStrategy.Fields.PARAMETER);
            case "Matrix" -> findStrategiesPmfms(programId, null, PmfmStrategy.Fields.MATRIX);
            case "Fraction" -> findStrategiesPmfms(programId, null, PmfmStrategy.Fields.FRACTION);
            case "Method" -> findStrategiesPmfms(programId, null, PmfmStrategy.Fields.METHOD);
            default ->
                throw new SumarisTechnicalException(String.format("Unable to find data on entity '%s': not implemented", entityName));
        };

        return findByIds(entityClass, entityIds, offset, size, sortAttribute, sortDirection);
    }

    @Override
    public List<String> findStrategiesAnalyticReferences(int programId) {
        CriteriaBuilder builder = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<String> query = builder.createQuery(String.class);
        Root<Strategy> root = query.from(Strategy.class);

        ParameterExpression<Integer> programIdParam = builder.parameter(Integer.class);

        query.select(root.get(Strategy.Fields.ANALYTIC_REFERENCE))
                .where(
                        builder.and(
                                // Program
                                builder.equal(root.get(Strategy.Fields.PROGRAM).get(Program.Fields.ID), programIdParam),
                                // Status (temporary or valid)
                                builder.in(root.get(Strategy.Fields.STATUS).get(Status.Fields.ID)).value(ImmutableList.of(StatusEnum.ENABLE.getId(), StatusEnum.TEMPORARY.getId()))
                        ));

        // Get last created strategies first
        query.orderBy(builder.desc(root.get(Strategy.Fields.CREATION_DATE)));

        return getEntityManager()
                .createQuery(query)
                .setParameter(programIdParam, programId)
                .getResultStream()
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public List<Integer> findStrategiesDepartments(int programId) {
        CriteriaBuilder builder = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<Department> query = builder.createQuery(Department.class);
        Root<Strategy> root = query.from(Strategy.class);

        ParameterExpression<Integer> programIdParam = builder.parameter(Integer.class);

        Join<Strategy, StrategyDepartment> strategyDepartmentInnerJoin = root.joinList(Strategy.Fields.DEPARTMENTS, JoinType.INNER);

        query.select(strategyDepartmentInnerJoin.get(StrategyDepartment.Fields.DEPARTMENT))
                .where(
                        builder.and(
                                // Program
                                builder.equal(root.get(Strategy.Fields.PROGRAM).get(Program.Fields.ID), programIdParam),
                                // Status (temporary or valid)
                                builder.in(root.get(Strategy.Fields.STATUS).get(Status.Fields.ID)).value(ImmutableList.of(StatusEnum.ENABLE.getId(), StatusEnum.TEMPORARY.getId())),
                                // Referential status
                                builder.in(strategyDepartmentInnerJoin.get(StrategyDepartment.Fields.DEPARTMENT).get(Strategy.Fields.STATUS).get(Status.Fields.ID)).value(ImmutableList.of(StatusEnum.ENABLE.getId(), StatusEnum.TEMPORARY.getId()))
                        ));

        // Get last created strategies first
        query.orderBy(builder.desc(root.get(Strategy.Fields.CREATION_DATE)));

        return getEntityManager()
                .createQuery(query)
                .setParameter(programIdParam, programId)
                .getResultStream()
                .distinct()
                .map(Department::getId)
                .collect(Collectors.toList());
    }

    @Override
    public List<Integer> findStrategiesLocations(int programId, LocationClassificationEnum locationClassification) {
        CriteriaBuilder builder = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<Location> query = builder.createQuery(Location.class);
        Root<Strategy> root = query.from(Strategy.class);

        ParameterExpression<Integer> programIdParam = builder.parameter(Integer.class);

        Join<Strategy, AppliedStrategy> appliedStrategyInnerJoin = root.joinList(Strategy.Fields.APPLIED_STRATEGIES, JoinType.INNER);

        // Location classification
        Predicate locationPredicate = builder.isNotNull(appliedStrategyInnerJoin.get(AppliedStrategy.Fields.LOCATION));
        if (locationClassification != null) {
            locationPredicate = builder.equal(appliedStrategyInnerJoin.get(AppliedStrategy.Fields.LOCATION).get(Location.Fields.LOCATION_LEVEL).get(LocationLevel.Fields.LOCATION_CLASSIFICATION), locationClassification.getId());
        }

        query.select(appliedStrategyInnerJoin.get(AppliedStrategy.Fields.LOCATION))
                .where(
                        builder.and(
                                // Program
                                builder.equal(root.get(Strategy.Fields.PROGRAM).get(Program.Fields.ID), programIdParam),
                                // Status (temporary or valid)
                                builder.in(root.get(Strategy.Fields.STATUS).get(Status.Fields.ID)).value(ImmutableList.of(StatusEnum.ENABLE.getId(), StatusEnum.TEMPORARY.getId())),
                                // Referential status
                                builder.in(appliedStrategyInnerJoin.get(AppliedStrategy.Fields.LOCATION).get(Strategy.Fields.STATUS).get(Status.Fields.ID)).value(ImmutableList.of(StatusEnum.ENABLE.getId(), StatusEnum.TEMPORARY.getId())),
                                // Location classification
                                locationPredicate
                                ));

        // Get last created strategies first
        query.orderBy(builder.desc(root.get(Strategy.Fields.CREATION_DATE)));

        return getEntityManager()
                .createQuery(query)
                .setParameter(programIdParam, programId)
                .getResultStream()
                .distinct()
                .map(Location::getId)
                .collect(Collectors.toList());
    }

    @Override
    public List<Integer> findStrategiesTaxonNames(int programId) {
        CriteriaBuilder builder = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<TaxonName> query = builder.createQuery(TaxonName.class);
        Root<Strategy> root = query.from(Strategy.class);

        ParameterExpression<Integer> programIdParam = builder.parameter(Integer.class);

        Join<Strategy, ReferenceTaxonStrategy> referenceTaxonStrategyInnerJoin = root.joinList(Strategy.Fields.REFERENCE_TAXONS, JoinType.INNER);
        Join<ReferenceTaxonStrategy, ReferenceTaxon> referenceTaxonInnerJoin = referenceTaxonStrategyInnerJoin.join(ReferenceTaxonStrategy.Fields.REFERENCE_TAXON, JoinType.INNER);
        Join<ReferenceTaxon, TaxonName> taxonNameInnerJoin = referenceTaxonInnerJoin.joinList(ReferenceTaxon.Fields.TAXON_NAMES, JoinType.INNER);

        query.select(taxonNameInnerJoin)
                .where(
                        builder.and(
                                // Program
                                builder.equal(root.get(Strategy.Fields.PROGRAM).get(Program.Fields.ID), programIdParam),
                                // Status (temporary or valid)
                                builder.in(root.get(Strategy.Fields.STATUS).get(Status.Fields.ID)).value(ImmutableList.of(StatusEnum.ENABLE.getId(), StatusEnum.TEMPORARY.getId())),
                                // Referential status
                                builder.in(taxonNameInnerJoin.get(TaxonName.Fields.STATUS).get(Status.Fields.ID)).value(ImmutableList.of(StatusEnum.ENABLE.getId(), StatusEnum.TEMPORARY.getId())),
                                // Taxon name
                                builder.isTrue(taxonNameInnerJoin.get(TaxonName.Fields.IS_REFERENT))
                        ));

        // Get last created strategies first
        query.orderBy(builder.desc(root.get(Strategy.Fields.CREATION_DATE)));

        return getEntityManager()
                .createQuery(query)
                .setParameter(programIdParam, programId)
                .getResultStream()
                .distinct()
                .map(TaxonName::getId)
                .collect(Collectors.toList());
    }

    private static final ImmutableList<String> PMFM_STRATEGY_SEARCH_VALID_FIELDS = ImmutableList.of(
        PmfmStrategy.Fields.PMFM,
        PmfmStrategy.Fields.PARAMETER,
        PmfmStrategy.Fields.MATRIX,
        PmfmStrategy.Fields.FRACTION,
        PmfmStrategy.Fields.METHOD
    );

    @Override
    public List<Integer> findStrategiesPmfms(int programId, Integer referenceTaxonId, String field) {
        Preconditions.checkNotNull(field, "Missing field argument");
        Preconditions.checkArgument(PMFM_STRATEGY_SEARCH_VALID_FIELDS.contains(field), "Invalid field. Must be in " + PMFM_STRATEGY_SEARCH_VALID_FIELDS);

        CriteriaBuilder builder = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<Integer> query = builder.createQuery(Integer.class);
        Root<Strategy> root = query.from(Strategy.class);

        ParameterExpression<Integer> programIdParam = builder.parameter(Integer.class);
        ParameterExpression<Integer> referenceTaxonIdParam = builder.parameter(Integer.class);

        Join<Strategy, PmfmStrategy> pmfmStrategyInnerJoin = root.joinList(Strategy.Fields.PMFMS, JoinType.INNER);

        Predicate predicate = builder.and(
                // Program
                builder.equal(root.get(Strategy.Fields.PROGRAM).get(Program.Fields.ID), programIdParam),
                // Status (temporary or valid)
                builder.in(root.get(Strategy.Fields.STATUS).get(Status.Fields.ID)).value(ImmutableList.of(StatusEnum.ENABLE.getId(), StatusEnum.TEMPORARY.getId())),
                // Status (on field)
                builder.in(pmfmStrategyInnerJoin.get(field).get(IItemReferentialEntity.Fields.STATUS).get(Status.Fields.ID)).value(ImmutableList.of(StatusEnum.ENABLE.getId(), StatusEnum.TEMPORARY.getId()))
        );

        // Taxon
        if (referenceTaxonId != null) {
            Join<Strategy, ReferenceTaxonStrategy> referenceTaxonStrategyInnerJoin = root.joinList(Strategy.Fields.REFERENCE_TAXONS, JoinType.INNER);
            predicate = builder.and(
                    predicate,
                    builder.equal(referenceTaxonStrategyInnerJoin.get(ReferenceTaxonStrategy.Fields.REFERENCE_TAXON).get(ReferenceTaxon.Fields.ID), referenceTaxonIdParam)
            );
        }

        query.select(pmfmStrategyInnerJoin.get(field).get(IEntity.Fields.ID))
             .where(predicate);

        // Get last created strategies first
        query.orderBy(builder.desc(root.get(Strategy.Fields.CREATION_DATE)));

        TypedQuery<Integer> typedQuery = getEntityManager().createQuery(query);

        if (referenceTaxonId != null) {
            typedQuery.setParameter(referenceTaxonIdParam, referenceTaxonId);
        }

        return typedQuery
                .setParameter(programIdParam, programId)
                .getResultStream()
                .distinct()
                .collect(Collectors.toList());
    }


    /* -- protected methods -- */

    protected List<ReferentialVO> findAnalyticReferencesByLabels(List<String> labels,
                                                                 int offset,
                                                                 int size,
                                                                 String sortAttribute,
                                                                 SortDirection sortDirection) {
        ReferentialFilterVO filter = ReferentialFilterVO.builder()
                .levelLabels(labels.toArray(new String[0]))
                //.statusIds(new Integer[]{StatusEnum.ENABLE.getId()})
                .build();

        return referentialExternalDao.findAnalyticReferencesByFilter(filter, offset, size, sortAttribute, sortDirection);
    }

    protected <T extends IReferentialEntity> List<ReferentialVO> findByIds(final Class<T> entityClass,
                                                                           List<Integer> entityIds,
                                                                           int offset,
                                                                           int size,
                                                                           String sortAttribute,
                                                                           SortDirection sortDirection) {
        CriteriaBuilder builder = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<T> query = builder.createQuery(entityClass);
        Root<T> root = query.from(entityClass);

        query.select(root)
                .where(
                        builder.and(
                                // Id
                                builder.in(root.get(IItemReferentialEntity.Fields.ID)).value(entityIds),
                                // Status (temporary or valid)
                                builder.in(root.get(IItemReferentialEntity.Fields.STATUS).get(Status.Fields.ID)).value(ImmutableList.of(StatusEnum.ENABLE.getId(), StatusEnum.TEMPORARY.getId()))
                        ));

        // Apply sorting
        addSorting(query, builder, root, sortAttribute, sortDirection);

        return getEntityManager()
                .createQuery(query)
                .setFirstResult(offset)
                .setMaxResults(size)
                .getResultStream()
                .map(referentialDao::toVO)
                //.map(source -> toTypedVO(source))
                .collect(Collectors.toList());
    }


}

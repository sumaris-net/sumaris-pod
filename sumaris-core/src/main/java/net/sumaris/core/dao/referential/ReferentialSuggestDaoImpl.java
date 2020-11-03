package net.sumaris.core.dao.referential;

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
import net.sumaris.core.dao.referential.taxon.TaxonNameRepository;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.dao.technical.hibernate.HibernateDaoSupport;
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
import net.sumaris.core.util.Beans;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Repository;

import javax.persistence.criteria.*;
import java.util.List;
import java.util.stream.Collectors;

@Repository("referentialSuggestDao")
public class ReferentialSuggestDaoImpl extends HibernateDaoSupport implements ReferentialSuggestDao {

    @Autowired
    private ReferentialDao referentialDao;

    @Autowired
    private TaxonNameRepository taxonNameRepository;

    @Autowired
    private ConversionService conversionService;

    public List<ReferentialVO> findFromStrategy(final String entityName,
                                                int programId,
                                                LocationClassificationEnum locationClassification,
                                                int offset,
                                                int size,
                                                String sortAttribute,
                                                SortDirection sortDirection) {
        Preconditions.checkNotNull(entityName, "Missing entityName argument");

        // Special case: AnalyticReference
        if (entityName.equals("AnalyticReference")) {
            List<String> labels = getStrategyAnalyticReferences(programId);
            return findAnalyticReferencesByLabels(labels, offset, size, sortAttribute, sortDirection);
        }

        // Get entity class from entityName
        Class<? extends IReferentialEntity> entityClass = ((ReferentialDaoImpl)referentialDao).getEntityClass(entityName);

        // switch entityName
        List<Integer> entityIds;
        switch (entityName) { // TODO lieu (zone pêche/port déb), espèce (reftax/taxonname), PMFM (P/M/F/M), code (AAAA_BIO_XXXX)
            case "Department":
                entityIds = getStrategyDepartmentIds(programId);
                break;
            case "Location":
                entityIds = getStrategyLocationIds(programId, locationClassification);
                break;
            case "TaxonName":
                entityIds = getStrategyTaxonNameIds(programId);
                break;
            default:
                throw new SumarisTechnicalException(String.format("Unable to find data on entity '%s': not implemented", entityName));
        }

        return findByIds(entityClass, entityIds, offset, size, sortAttribute, sortDirection);
    }


    /* -- protected methods -- */

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
                .distinct()
                //.map(referentialDao::toVO)
                .map(source -> toTypedVO(source))
                .collect(Collectors.toList());
    }

    protected List<Integer> getStrategyDepartmentIds(int programId) {
        CriteriaBuilder builder = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<Department> query = builder.createQuery(Department.class);
        Root<Strategy> root = query.from(Strategy.class);

        ParameterExpression<Integer> programIdParam = builder.parameter(Integer.class);

        Join<Strategy, StrategyDepartment> strategyDepartmentInnerJoin = root.joinList(Strategy.Fields.STRATEGY_DEPARTMENTS, JoinType.INNER);

        query.select(strategyDepartmentInnerJoin.get(StrategyDepartment.Fields.DEPARTMENT))
                .where(
                        builder.and(
                                // Program
                                builder.equal(root.get(Strategy.Fields.PROGRAM).get(Program.Fields.ID), programIdParam),
                                // Status (temporary or valid)
                                builder.in(root.get(Strategy.Fields.STATUS).get(Status.Fields.ID)).value(ImmutableList.of(StatusEnum.ENABLE.getId(), StatusEnum.TEMPORARY.getId()))
                                ));

        return getEntityManager()
                .createQuery(query)
                .setParameter(programIdParam, programId)
                .getResultStream()
                .map(source -> source.getId())
                .collect(Collectors.toList());
    }

    protected List<Integer> getStrategyLocationIds(int programId, LocationClassificationEnum locationClassification) {
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
                                // Location classification
                                locationPredicate
                                ));

        return getEntityManager()
                .createQuery(query)
                .setParameter(programIdParam, programId)
                .getResultStream()
                .map(source -> source.getId())
                .collect(Collectors.toList());
    }

    protected List<Integer> getStrategyTaxonNameIds(int programId) {
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
                                // Taxon name
                                builder.isTrue(taxonNameInnerJoin.get(TaxonName.Fields.IS_REFERENT))
                                ));

        return getEntityManager()
                .createQuery(query)
                .setParameter(programIdParam, programId)
                .getResultStream()
                .map(source -> source.getId())
                .collect(Collectors.toList());
    }

    protected List<String> getStrategyAnalyticReferences(int programId) {
        CriteriaBuilder builder = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<Strategy> query = builder.createQuery(Strategy.class);
        Root<Strategy> root = query.from(Strategy.class);

        ParameterExpression<Integer> programIdParam = builder.parameter(Integer.class);

        query.select(root)
                .where(
                        builder.and(
                                // Program
                                builder.equal(root.get(Strategy.Fields.PROGRAM).get(Program.Fields.ID), programIdParam),
                                // Status (temporary or valid)
                                builder.in(root.get(Strategy.Fields.STATUS).get(Status.Fields.ID)).value(ImmutableList.of(StatusEnum.ENABLE.getId(), StatusEnum.TEMPORARY.getId()))
                        ));

        return getEntityManager()
                .createQuery(query)
                .setParameter(programIdParam, programId)
                .getResultStream()
                .map(source -> source.getAnalyticReference())
                .collect(Collectors.toList());
    }

    // TODO move in a dedicated service
    protected List<ReferentialVO> findAnalyticReferencesByLabels(List<String> labels,
                                                                 int offset,
                                                                 int size,
                                                                 String sortAttribute,
                                                                 SortDirection sortDirection) {
        return labels.stream()
                .skip(offset)
                .limit(size)
                .distinct()
                .map(source -> { // TODO get id, label, name, status from EOTP webservice
                    ReferentialVO target = new ReferentialVO();
                    target.setId(source.hashCode());
                    target.setLabel(source);
                    target.setName(source);
                    target.setStatusId(StatusEnum.ENABLE.getId());
                    return target;
                })
                .sorted(Beans.naturalComparator(sortAttribute, sortDirection))
                .collect(Collectors.toList()
        );
    }

    protected ReferentialVO toTypedVO(IReferentialEntity source) {
        Preconditions.checkNotNull(source);

        // Get VO class from entityName
        String targetClazzName = ReferentialVO.class.getPackage().getName() + "." + source.getClass().getSimpleName() + "VO";
        try {
            Class<? extends ReferentialVO> targetClazz = Class.forName(targetClazzName).asSubclass(ReferentialVO.class);
            ReferentialVO target = conversionService.convert(source, targetClazz);
            return target;
        } catch (ClassNotFoundException | ClassCastException e) {
            throw new IllegalArgumentException(String.format("Referential value object [%s] not exists", targetClazzName));
        }
    }

}

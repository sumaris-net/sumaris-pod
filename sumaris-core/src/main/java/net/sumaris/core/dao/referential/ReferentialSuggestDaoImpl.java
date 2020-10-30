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
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.dao.technical.hibernate.HibernateDaoSupport;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.model.administration.programStrategy.Program;
import net.sumaris.core.model.administration.programStrategy.Strategy;
import net.sumaris.core.model.administration.programStrategy.StrategyDepartment;
import net.sumaris.core.model.administration.user.Department;
import net.sumaris.core.model.referential.IItemReferentialEntity;
import net.sumaris.core.model.referential.IReferentialEntity;
import net.sumaris.core.model.referential.Status;
import net.sumaris.core.model.referential.StatusEnum;
import net.sumaris.core.util.Beans;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.persistence.criteria.*;
import java.util.List;
import java.util.stream.Collectors;

@Repository("referentialSuggestDao")
public class ReferentialSuggestDaoImpl extends HibernateDaoSupport implements ReferentialSuggestDao {

    @Autowired
    private ReferentialDao referentialDao;

    @Deprecated
    public List<String> getAnalyticReferences(int programId) {
        return getAnalyticReferences(programId, null);
    }

    @Deprecated
    public List<String> getAnalyticReferences(int programId, Integer nbYear) {
        CriteriaBuilder builder = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<Strategy> query = builder.createQuery(Strategy.class);
        Root<Strategy> root = query.from(Strategy.class);

        ParameterExpression<Integer> programIdParam = builder.parameter(Integer.class);

        query.select(root)
                .distinct(true)
                .where(
                        builder.and(
                                // program
                                builder.equal(root.get(Strategy.Fields.PROGRAM).get(Program.Fields.ID), programIdParam),
                                // Status (temporary or valid)
                                builder.in(root.get(Strategy.Fields.STATUS).get(Status.Fields.ID)).value(ImmutableList.of(StatusEnum.ENABLE.getId(), StatusEnum.TEMPORARY.getId()))
                        ));

        // Sort by label
        query.orderBy(builder.asc(root.get(Strategy.Fields.ANALYTIC_REFERENCE)));

        return getEntityManager()
                .createQuery(query)
                .setParameter(programIdParam, programId)
                .getResultStream()
                .map(source -> source.getAnalyticReference())
                .collect(Collectors.toList());
    }

    @Deprecated
    public List<ReferentialVO> getDepartments(int programId) {
        CriteriaBuilder builder = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<Department> query = builder.createQuery(Department.class);
        Root<Strategy> root = query.from(Strategy.class);

        ParameterExpression<Integer> programIdParam = builder.parameter(Integer.class);

        Join<Strategy, StrategyDepartment> strategyDepartmentInnerJoin = root.joinList(Strategy.Fields.STRATEGY_DEPARTMENTS, JoinType.INNER);
        Join<StrategyDepartment, Department> departmentInnerJoin = strategyDepartmentInnerJoin.join(StrategyDepartment.Fields.DEPARTMENT, JoinType.INNER);

        query.select(departmentInnerJoin)
                .distinct(true)
                .where(
                        builder.and(
                                // program
                                builder.equal(root.get(Strategy.Fields.PROGRAM).get(Program.Fields.ID), programIdParam),
                                builder.in(root.get(Strategy.Fields.STATUS).get(Status.Fields.ID)).value(ImmutableList.of(StatusEnum.ENABLE.getId(), StatusEnum.TEMPORARY.getId())),
                                // Status (temporary or valid)
                                builder.in(departmentInnerJoin.get(Department.Fields.STATUS).get(Status.Fields.ID)).value(ImmutableList.of(StatusEnum.ENABLE.getId(), StatusEnum.TEMPORARY.getId()))
                        ));

        // Sort by label
        query.orderBy(builder.asc(departmentInnerJoin.get(Department.Fields.LABEL)));

        return getEntityManager()
                .createQuery(query)
                .setParameter(programIdParam, programId)
                .getResultStream()
                .map(referentialDao::toVO)
                .collect(Collectors.toList());
    }

    @Deprecated
    public List<ReferentialVO> find(final String entityName,
                                    int programId,
                                    int offset,
                                    int size,
                                    String sortAttribute,
                                    SortDirection sortDirection) {
        Preconditions.checkNotNull(entityName, "Missing entityName argument");

        // Get entity class from entityName
        Class<? extends IReferentialEntity> entityClass = ((ReferentialDaoImpl)referentialDao).getEntityClass(entityName);

        //referentialDao.findByFilter(entityName, ReferentialFilterVO.builder().build(), offset, size, sortAttribute, sortDirection);
        //loadAll(entityClass, ids, true);

        CriteriaBuilder builder = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<Department> query = builder.createQuery(Department.class);
        Root<Strategy> root = query.from(Strategy.class);

        ParameterExpression<Integer> programIdParam = builder.parameter(Integer.class);

        Join<Strategy, StrategyDepartment> strategyDepartmentInnerJoin = root.joinList(Strategy.Fields.STRATEGY_DEPARTMENTS, JoinType.INNER);
        Join<StrategyDepartment, Department> departmentInnerJoin = strategyDepartmentInnerJoin.join(StrategyDepartment.Fields.DEPARTMENT, JoinType.INNER);

        query.select(departmentInnerJoin)
                .distinct(true)
                .where(
                        builder.and(
                                // program
                                builder.equal(root.get(Strategy.Fields.PROGRAM).get(Program.Fields.ID), programIdParam),
                                builder.in(root.get(Strategy.Fields.STATUS).get(Status.Fields.ID)).value(ImmutableList.of(StatusEnum.ENABLE.getId(), StatusEnum.TEMPORARY.getId())),
                                // Status (temporary or valid)
                                builder.in(departmentInnerJoin.get(Department.Fields.STATUS).get(Status.Fields.ID)).value(ImmutableList.of(StatusEnum.ENABLE.getId(), StatusEnum.TEMPORARY.getId()))
                        ));

        // Apply sorting
        query.orderBy(builder.asc(departmentInnerJoin.get(Department.Fields.LABEL)));
        //addSorting(query, builder, vesselRoot, sortAttribute, sortDirection);

        return getEntityManager()
                .createQuery(query)
                .setParameter(programIdParam, programId)
                .setFirstResult(offset)
                .setMaxResults(size)
                .getResultStream()
                .map(referentialDao::toVO)
                .collect(Collectors.toList());
    }

    public List<ReferentialVO> findFromStrategy(final String entityName,
                                                int programId,
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
        switch (entityName) {
            case "Department":
                entityIds = getStrategyDepartmentIds(programId);
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
                .map(referentialDao::toVO)
                .collect(Collectors.toList());
    }

    protected List<Integer> getStrategyDepartmentIds(int programId) {
        CriteriaBuilder builder = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<Department> query = builder.createQuery(Department.class);
        Root<Strategy> root = query.from(Strategy.class);

        ParameterExpression<Integer> programIdParam = builder.parameter(Integer.class);

        Join<Strategy, StrategyDepartment> strategyDepartmentInnerJoin = root.joinList(Strategy.Fields.STRATEGY_DEPARTMENTS, JoinType.INNER);
        Join<StrategyDepartment, Department> departmentInnerJoin = strategyDepartmentInnerJoin.join(StrategyDepartment.Fields.DEPARTMENT, JoinType.INNER);

        query.select(departmentInnerJoin)
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

}

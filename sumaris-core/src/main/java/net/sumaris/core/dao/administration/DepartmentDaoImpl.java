package net.sumaris.core.dao.administration;

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
import net.sumaris.core.dao.technical.Beans;
import net.sumaris.core.dao.technical.Dates;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.dao.technical.hibernate.HibernateDaoSupport;
import net.sumaris.core.exception.BadUpdateDateException;
import net.sumaris.core.model.administration.user.Department;
import net.sumaris.core.model.data.ImageAttachment;
import net.sumaris.core.model.referential.IReferentialEntity;
import net.sumaris.core.model.referential.Status;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.filter.DepartmentFilterVO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.nuiton.i18n.I18n;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.NoResultException;
import javax.persistence.criteria.*;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Repository("departmentDao")
public class DepartmentDaoImpl extends HibernateDaoSupport implements DepartmentDao {

    /** Logger. */
    private static final Logger log =
            LoggerFactory.getLogger(DepartmentDaoImpl.class);

    @Override
    public List<DepartmentVO> findByFilter(DepartmentFilterVO filter, int offset, int size, String sortAttribute, SortDirection sortDirection) {
        Preconditions.checkNotNull(filter);
        Preconditions.checkArgument(offset >= 0);
        Preconditions.checkArgument(size > 0);

        List<Integer> statusIds = CollectionUtils.isEmpty(filter.getStatusIds()) ?
                null : filter.getStatusIds();

        EntityManager entityManager = getEntityManager();
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Department> query = builder.createQuery(Department.class);
        Root<Department> root = query.from(Department.class);

        Join<Department, Status> statusJoin = root.join(Department.PROPERTY_STATUS, JoinType.INNER);
        Join<Department, ImageAttachment> logoJoin = root.join(Department.PROPERTY_LOGO, JoinType.LEFT);

        ParameterExpression<Boolean> hasStatusIdsParam = builder.parameter(Boolean.class);
        ParameterExpression<Boolean> withLogoParam = builder.parameter(Boolean.class);

        query.select(root)
                .where(
                        builder.and(
                                // status Id
                                CollectionUtils.isEmpty(filter.getStatusIds()) ? builder.isFalse(hasStatusIdsParam) : builder.in(statusJoin.get(IReferentialEntity.PROPERTY_ID)).value(filter.getStatusIds()),
                                // with logo
                                builder.or(
                                        builder.isNull(withLogoParam),
                                        builder.isNotNull(logoJoin.get(IReferentialEntity.PROPERTY_ID))
                                )
                        ));

        if (StringUtils.isNotBlank(sortAttribute)) {
            if (sortDirection == SortDirection.ASC) {
                query.orderBy(builder.asc(root.get(sortAttribute)));
            } else {
                query.orderBy(builder.desc(root.get(sortAttribute)));
            }
        }

        return entityManager.createQuery(query)
                .setParameter(hasStatusIdsParam, CollectionUtils.isNotEmpty(statusIds))
                .setParameter(withLogoParam, isTrueOrNull(filter.getWithLogo()))
                .setFirstResult(offset)
                .setMaxResults(size)
                .getResultList()
                .stream()
                .map(this::toDepartmentVO)
                .filter(Objects::nonNull)
                .sorted(Beans.naturalComparator(sortAttribute, sortDirection))
                .collect(Collectors.toList());
    }

    @Override
    public DepartmentVO get(int id) {
        return toDepartmentVO(get(Department.class, id));
    }

    @Override
    public Department getByLabelOrNull(final String label) {

        EntityManager entityManager = getEntityManager();
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Department> query = builder.createQuery(Department.class);
        Root<Department> root = query.from(Department.class);

        ParameterExpression<String> labelParam = builder.parameter(String.class);

        query.select(root)
                .where(builder.equal(root.get(Department.PROPERTY_LABEL), labelParam));

        try {
            return entityManager.createQuery(query)
                    .setParameter(labelParam, label)
                    .getSingleResult();
        } catch (EmptyResultDataAccessException | NoResultException e) {
            return null;
        }
    }

    @Override
    public DepartmentVO save(DepartmentVO source) {
        Preconditions.checkNotNull(source);
        Preconditions.checkNotNull(source.getLabel());
        Preconditions.checkNotNull(source.getName());
        Preconditions.checkNotNull(source.getSiteUrl());

        EntityManager entityManager = getEntityManager();
        Department entity = null;
        if (source.getId() != null) {
            entity = entityManager.find(Department.class, source.getId());
        }
        boolean isNew = (entity == null);
        if (isNew) {
            entity = new Department();
        }

        if (!isNew) {
            // Check update date
            checkUpdateDateForUpdate(source, entity);

            // Lock entityName
            lockForUpdate(entity, LockModeType.PESSIMISTIC_WRITE);
        }

        departmentVOToEntity(source, entity, true);

        // Update update_dt
        Timestamp newUpdateDate = getDatabaseCurrentTimestamp();
        entity.setUpdateDate(newUpdateDate);

        // Save entityName
        if (isNew) {
            // Force creation date
            entity.setCreationDate(newUpdateDate);
            source.setCreationDate(newUpdateDate);

            entityManager.persist(entity);
            source.setId(entity.getId());
        } else {
            entityManager.merge(entity);
        }

        source.setUpdateDate(newUpdateDate);

        getEntityManager().flush();
        getEntityManager().clear();

        return source;
    }

    @Override
    public void delete(int id) {

        log.debug(String.format("Deleting department {id=%s}...", id));
        delete(Department.class, id);
    }

    @Override
    public DepartmentVO toDepartmentVO(Department source) {
        if (source == null) return null;
        DepartmentVO target = new DepartmentVO();

        Beans.copyProperties(source, target);

        // Status
        target.setStatusId(source.getStatus().getId());

        // Has logo
        target.setHasLogo(source.getLogo() != null);

        return target;
    }

    /* -- protected methods -- */

    protected List<DepartmentVO> toDepartmentVOs(List<Department> source) {
        return source.stream()
                .map(this::toDepartmentVO)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    protected List<DepartmentVO> toDepartmentVOs(Stream<Department> source, String sortAttribute, SortDirection sortDirection) {
        return source
                .map(this::toDepartmentVO)
                .filter(Objects::nonNull)
                .sorted(Beans.naturalComparator(sortAttribute, sortDirection))
                .collect(Collectors.toList());
    }

    protected void departmentVOToEntity(DepartmentVO source, Department target, boolean copyIfNull) {

        Beans.copyProperties(source, target);

        // Status
        if (copyIfNull || source.getStatusId() != null) {
            if (source.getStatusId() == null) {
                target.setStatus(null);
            }
            else {
                target.setStatus(load(Status.class, source.getStatusId()));
            }
        }
    }

    private Boolean isTrueOrNull(Boolean value) {
        return Boolean.TRUE.equals(value) ? value : null;
    }

}

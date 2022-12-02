package net.sumaris.core.dao.technical.hibernate;

/*-
 * #%L
 * SUMARiS :: Sumaris Core Shared
 * $Id:$
 * $HeadURL:$
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


import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.dao.technical.Daos;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.model.IEntity;
import net.sumaris.core.exception.DataLockedException;
import net.sumaris.core.exception.DataNotFoundException;
import net.sumaris.core.exception.SumarisTechnicalException;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.nuiton.i18n.I18n;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.LockTimeoutException;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Root;
import javax.sql.DataSource;
import java.io.Serializable;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * <p>HibernateDaoSupport class.</p>
 *
 */
@Slf4j
public abstract class HibernateDaoSupport {

    private boolean debugEntityLoad;

    @Autowired
    private SumarisConfiguration configuration;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private DataSource dataSource;

    /**
     * <p>Constructor for HibernateDaoSupport.</p>
     */
    public HibernateDaoSupport() {
    }

    public HibernateDaoSupport(SumarisConfiguration configuration) {
        this.configuration = configuration;
    }

    @PostConstruct
    private void init() {
        this.debugEntityLoad = getConfig().debugEntityLoad();
    }

    /**
     * <p>Setter for the field <code>entityManager</code>.</p>
     *
     * @param entityManager a {@link EntityManager} object.
     */
    protected final void setEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * <p>getEntityManager.</p>
     *
     * @return a {@link Session} object.
     */
    protected final EntityManager getEntityManager() {
        return entityManager;
    }

    protected Session getSession() {
        return (Session) entityManager.getDelegate();
    }

    protected SessionFactoryImplementor getSessionFactory() {
        return (SessionFactoryImplementor) getSession().getSessionFactory();
    }

    public SumarisConfiguration getConfig() {
        return configuration;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    /**
     * <p>deleteAll.</p>
     *
     * @param entities a {@link Collection} object.
     */
    protected void deleteAll(Collection<?> entities) {
        for (Object entity : entities) {
            entityManager.remove(entity);
        }
    }

    /**
     * <p>deleteAll.</p>
     *
     * @param entityClass a {@link Collection} object.
     * @param identifier  a {@link Serializable} object.
     */
    protected <T> void delete(Class<T> entityClass, Serializable identifier) {
        T entity = entityManager.find(entityClass, identifier);
        if (entity != null) {
            entityManager.remove(entity);
        }
    }

    /**
     * <p>load.</p>
     *
     * @param clazz a {@link Class} object.
     * @param id    a {@link Serializable} object.
     * @param <T>   a T object.
     * @return a T object.
     */
    protected <T> T getReference(Class<? extends T> clazz, Serializable id) {

        if (debugEntityLoad) {
            T load = entityManager.find(clazz, id);
            if (load == null) {
                throw new DataIntegrityViolationException("Unable to load entity " + clazz.getName() + " with identifier '" + id + "': not found in database.");
            }
        }
        return entityManager.getReference(clazz, id);
    }

    /**
     * <p>load many entities.</p>
     *
     * @param clazz a {@link Class} object.
     * @param ids   list of identifiers.
     * @param <T>   a T object.
     * @return a list of T object.
     */
    @SuppressWarnings("unchecked")
    protected <T> List<T> loadAll(Class<? extends T> clazz, Collection<? extends Serializable> ids, boolean failedIfMissing) {

        List<T> result = entityManager.createQuery(String.format("from %s where id in (:id)", clazz.getSimpleName()))
            .setParameter("id", ids)
            .getResultList();
        if (failedIfMissing && result.size() != ids.size()) {
            throw new DataIntegrityViolationException(String.format("Unable to load entities %s from ids. Expected %s entities, but found %s entities.",
                clazz.getName(),
                ids.size(),
                result.size()));
        }
        return result;
    }

    /**
     * <p>load.</p>
     *
     * @param clazz a {@link Class} object.
     * @param ids   list of identifiers.
     * @param <T>   a T object.
     * @return a list of T object.
     */
    protected <T> Set<T> loadAllAsSet(Class<? extends T> clazz, Collection<? extends Serializable> ids, boolean failedIfMissing) {
        List<T> result = loadAll(clazz, ids, failedIfMissing);
        return Sets.newHashSet(result);
    }

    /**
     * <p>find an entity (can be null, if not found).</p>
     *
     * @param clazz a {@link Class} object.
     * @param id    a {@link Serializable} object.
     * @param <T>   a T object.
     * @return a T object.
     */
    protected <T> T find(Class<? extends T> clazz, Serializable id) {
        return entityManager.find(clazz, id);
    }

    /**
     * <p>get and lock an entity. Throw a DataNotFoundException if not found</p>
     *
     * @param clazz        a {@link Class} object.
     * @param id           a {@link Serializable} object.
     * @param lockModeType a {@link LockModeType} object.
     * @param <T>          a T object.
     * @return a T object.
     */
    protected <T extends Serializable> T getById(Class<? extends T> clazz, Serializable id, LockModeType lockModeType) {
        T entity = getById(clazz, id);
        entityManager.lock(entity, lockModeType);
        return entity;
    }

    /**
     * <p>get an entity. Throw a DataNotFoundException if not found</p>
     *
     * @param clazz a {@link Class} object.
     * @param id    a {@link Serializable} object.
     * @param <T>   a T object.
     * @return a T object.
     */
    protected <T> T getById(Class<? extends T> clazz, Serializable id) {
        T entity = entityManager.find(clazz, id); // Can be null
        if (entity == null) throw new DataNotFoundException(I18n.t("sumaris.persistence.error.entityNotFound", clazz.getSimpleName(), id));
        return entity;
    }


    /**
     * <p>getDatabaseCurrentTimestamp.</p>
     *
     * @return a {@link Timestamp} object.
     */
    protected Timestamp getDatabaseCurrentTimestamp() {
        try {
            final Dialect dialect = getSessionFactory().getJdbcServices().getDialect();
            return Daos.getDatabaseCurrentTimestamp(getDataSource(), dialect);
        } catch (DataAccessResourceFailureException | SQLException e) {
            throw new SumarisTechnicalException(e);
        }
    }

    /**
     * <p>getDatabaseCurrentDate.</p>
     *
     * @return a {@link Date} object.
     */
    protected Date getDatabaseCurrentDate() {
        try {
            final Dialect dialect = getSessionFactory().getJdbcServices().getDialect();
            return Daos.getDatabaseCurrentDate(getDataSource(), dialect);
        } catch (DataAccessResourceFailureException | SQLException e) {
            throw new SumarisTechnicalException(e);
        }
    }

    protected String getTableName(String entityName) {
        return I18n.t("sumaris.persistence.table." + entityName.substring(0, 1).toLowerCase() + entityName.substring(1));
    }

    protected void lockForUpdate(IEntity<?> entity) {
        lockForUpdate(entity, LockModeType.PESSIMISTIC_WRITE);
    }

    protected void lockForUpdate(IEntity<?> entity, LockModeType modeType) {
        // Lock entityName
        try {
            entityManager.lock(entity, modeType);
        } catch (LockTimeoutException e) {
            throw new DataLockedException(I18n.t("sumaris.persistence.error.locked",
                getTableName(entity.getClass().getSimpleName()), entity.getId()), e);
        }
    }

    protected void delete(IEntity<?> entity) {
        entityManager.remove(entity);
    }

    /**
     * Add a orderBy on query
     *
     * @param query         the query
     * @param cb            criteria builder
     * @param root          the root of the query
     * @param sortAttribute the sort attribute (can be a nested attribute)
     * @param sortDirection the direction
     * @param <T>           type of query
     * @return the query itself
     */
    protected <T> CriteriaQuery<T> addSorting(CriteriaQuery<T> query,
                                              CriteriaBuilder cb,
                                              Root<?> root, String sortAttribute, SortDirection sortDirection) {
        // Add sorting
        if (StringUtils.isNotBlank(sortAttribute)) {
            Expression<?> sortExpression = Daos.composePath(root, sortAttribute);
            query.orderBy(SortDirection.DESC.equals(sortDirection) ?
                cb.desc(sortExpression) :
                cb.asc(sortExpression)
            );
        }
        return query;
    }

}
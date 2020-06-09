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


import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.dao.technical.Daos;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.dao.technical.model.IEntity;
import net.sumaris.core.dao.technical.model.IUpdateDateEntityBean;
import net.sumaris.core.exception.BadUpdateDateException;
import net.sumaris.core.exception.DataLockedException;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.util.Dates;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.nuiton.i18n.I18n;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.LockTimeoutException;
import javax.persistence.Query;
import javax.persistence.criteria.*;
import javax.sql.DataSource;
import java.io.Serializable;
import java.math.BigInteger;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>HibernateDaoSupport class.</p>
 *
 */
public abstract class HibernateDaoSupport {

    /**
     * Logger.
     */
    protected static final Logger logger =
            LoggerFactory.getLogger(HibernateDaoSupport.class);

    private boolean debugEntityLoad;

    @Autowired
    protected EntityManager entityManager;

    @Autowired
    protected SumarisConfiguration config;

    @Autowired
    private DataSource dataSource;

    /**
     * <p>Constructor for HibernateDaoSupport.</p>
     */
    public HibernateDaoSupport() {
        this.debugEntityLoad = SumarisConfiguration.getInstance().debugEntityLoad();
    }

    /**
     * <p>Setter for the field <code>entityManager</code>.</p>
     *
     * @param entityManager a {@link EntityManager} object.
     */
    protected void setEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * @deprecated use EntityManager instead
     * @param sf
     */
    @Deprecated
    protected void setSessionFactory(SessionFactory sf) {
        logger.warn("TODO: remove call to deprecated setSessionFactory()");
    }

    /**
     * <p>getEntityManager.</p>
     *
     * @return a {@link Session} object.
     */
    protected EntityManager getEntityManager() {
        return entityManager;
    }


    protected Session getSession() {
        return (Session) entityManager.getDelegate();
    }

    protected SessionFactoryImplementor getSessionFactory() {
        return (SessionFactoryImplementor) getSession().getSessionFactory();
    }

    /**
     * <p>deleteAll.</p>
     *
     * @param entities a {@link Collection} object.
     */
    protected void deleteAll(Collection<?> entities) {
        EntityManager entityManager = getEntityManager();
        for (Object entity : entities) {
            entityManager.remove(entity);
        }
    }

    /**
     * <p>deleteAll.</p>
     *
     * @param entityClass a {@link Collection} object.
     * @param identifier a {@link Serializable} object.
     */
    protected <T> void delete(Class<T> entityClass, Serializable identifier) {
        EntityManager entityManager = getEntityManager();
        T entity = entityManager.find(entityClass, identifier);
        if (entity != null) {
            entityManager.remove(entity);
        }
    }

    /**
     * <p>load.</p>
     *
     * @param clazz a {@link Class} object.
     * @param id a {@link Serializable} object.
     * @param <T> a T object.
     * @return a T object.
     */
    @SuppressWarnings("unchecked")
    protected <T> T load(Class<? extends T> clazz, Serializable id) {

        if (debugEntityLoad) {
            T load = entityManager.find(clazz, id);
            if (load == null) {
                throw new DataIntegrityViolationException("Unable to load entity " + clazz.getName() + " with identifier '" + id + "': not found in database.");
            }
        }
        return entityManager.unwrap(Session.class).load(clazz, id);
    }

    /**
     * <p>load many entities.</p>
     *
     * @param clazz a {@link Class} object.
     * @param ids list of identifiers.
     * @param <T> a T object.
     * @return a list of T object.
     */
    @SuppressWarnings("unchecked")
    protected <T> List<T> loadAll(Class<? extends T> clazz, Collection<? extends Serializable> ids, boolean failedIfMissing) {

        List result = getEntityManager().createQuery(String.format("from %s where id in (:id)", clazz.getSimpleName()))
                .setParameter("ids", ids)
                .getResultList();
        if (failedIfMissing && result.size() != ids.size()) {
            throw new DataIntegrityViolationException(String.format("Unable to load entities %s from ids. Expected %s entities, but found %s entities.",
                    clazz.getName(),
                    ids.size(),
                    result.size()));
        }
        return (List<T>)result;
    }

    /**
     * <p>load.</p>
     *
     * @param clazz a {@link Class} object.
     * @param ids list of identifiers.
     * @param <T> a T object.
     * @return a list of T object.
     */
    @SuppressWarnings("unchecked")
    protected <T> Set<T> loadAllAsSet(Class<? extends T> clazz, Collection<? extends Serializable> ids, boolean failedIfMissing) {

        List<T> result = loadAll(clazz, ids, failedIfMissing);
        return Sets.newHashSet(result);
    }

    /**
     * <p>get.</p>
     *
     * @param clazz a {@link Class} object.
     * @param id a {@link Serializable} object.
     * @param <T> a T object.
     * @return a T object.
     */
    @SuppressWarnings("unchecked")
    protected <T> T get(Class<? extends T> clazz, Serializable id) {
        return getEntityManager().find(clazz, id);
    }

    /**
     * <p>get.</p>
     *
     * @param clazz a {@link Class} object.
     * @param id a {@link Serializable} object.
     * @param lockModeType a {@link LockModeType} object.
     * @param <T> a T object.
     * @return a T object.
     */
    @SuppressWarnings("unchecked")
    protected <T extends Serializable> T get(Class<? extends T> clazz, Serializable id, LockModeType lockModeType) {
        T entity = entityManager.find(clazz, id);
        entityManager.lock(entity, lockModeType);
        return entity;
    }

    /**
     * <p>executeMultipleCountWithNotNullCondition.</p>
     *
     * @param columnNamesByTableNames a {@link Multimap} object.
     * @param notNullConditionColumnNameByTableNames a {@link Map} object.
     * @param source a int.
     * @return a boolean.
     */
    protected boolean executeMultipleCountWithNotNullCondition(Multimap<String, String> columnNamesByTableNames, Map<String, String> notNullConditionColumnNameByTableNames, int source) {

        String countQueryString = "SELECT COUNT(*) FROM %s WHERE %s = %d";
        return executeMultipleCount(countQueryString, columnNamesByTableNames, " AND %s IS NOT NULL", notNullConditionColumnNameByTableNames, source);
    }

    /**
     * <p>executeMultipleCount.</p>
     *
     * @param columnNamesByTableNames a {@link Multimap} object.
     * @param source a int.
     * @return a boolean.
     */
    protected boolean executeMultipleCount(Multimap<String, String> columnNamesByTableNames, int source) {

        String countQueryString = "SELECT COUNT(*) FROM %s WHERE %s = %d";
        return executeMultipleCount(countQueryString, columnNamesByTableNames, null, null, source);
    }

    /**
     * <p>executeMultipleCount.</p>
     *
     * @param columnNamesByTableNames a {@link Multimap} object.
     * @param source a {@link String} object.
     * @return a boolean.
     */
    protected boolean executeMultipleCount(Multimap<String, String> columnNamesByTableNames, String source) {

        String countQueryString = "SELECT COUNT(*) FROM %s WHERE %s = '%s'";
        return executeMultipleCount(countQueryString, columnNamesByTableNames, null, null, source);
    }

    private boolean executeMultipleCount(
            String countQueryString,
            Multimap<String, String> columnNamesByTableNames,
            String conditionQueryAppendix,
            Map<String, String> conditionColumnNameByTableNames,
            Object source) {

        String queryString;
        Query query;
        for (String tableName : columnNamesByTableNames.keySet()) {
            Collection<String> columnNames = columnNamesByTableNames.get(tableName);
            String conditionColumnName = conditionColumnNameByTableNames == null ? null : conditionColumnNameByTableNames.get(tableName);
            for (String columnName : columnNames) {
                if (StringUtils.isNotBlank(conditionQueryAppendix) && StringUtils.isNotBlank(conditionColumnName)) {
                    queryString = String.format(countQueryString.concat(conditionQueryAppendix), tableName, columnName, source, conditionColumnName);
                } else {
                    queryString = String.format(countQueryString, tableName, columnName, source);
                }

                query = entityManager.createNativeQuery(queryString);
                if (logger.isDebugEnabled()) {
                    logger.debug(queryString);
                }
                BigInteger count = (BigInteger) query.getSingleResult();
                if (count.intValue() > 0) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * <p>executeMultipleUpdate.</p>
     *
     * @param columnNamesByTableNames a {@link Multimap} object.
     * @param sourceId a int.
     * @param targetId a int.
     */
    protected void executeMultipleUpdate(Multimap<String, String> columnNamesByTableNames, int sourceId, int targetId) {

        String updateQueryString = "UPDATE %s SET %s = %d WHERE %s = %d";
        executeMultipleUpdate(updateQueryString, columnNamesByTableNames, null, null, sourceId, targetId);
    }

    /**
     * <p>executeMultipleUpdateWithNullCondition.</p>
     *
     * @param columnNamesByTableNames a {@link Multimap} object.
     * @param nullConditionColumnNameByTableNames a {@link Map} object.
     * @param sourceId a int.
     * @param targetId a int.
     */
    protected void executeMultipleUpdateWithNullCondition(Multimap<String, String> columnNamesByTableNames, Map<String, String> nullConditionColumnNameByTableNames, int sourceId, int targetId) {

        String updateQueryString = "UPDATE %s SET %s = %d WHERE %s = %d";
        executeMultipleUpdate(updateQueryString, columnNamesByTableNames, " AND %s IS NULL", nullConditionColumnNameByTableNames, sourceId, targetId);
    }

    /**
     * <p>executeMultipleUpdate.</p>
     *
     * @param columnNamesByTableNames a {@link Multimap} object.
     * @param sourceCode a {@link String} object.
     * @param targetCode a {@link String} object.
     */
    protected void executeMultipleUpdate(Multimap<String, String> columnNamesByTableNames, String sourceCode, String targetCode) {

        String updateQueryString = "UPDATE %s SET %s = '%s' WHERE %s = '%s'";
        executeMultipleUpdate(updateQueryString, columnNamesByTableNames, null, null, sourceCode, targetCode);
    }

    private void executeMultipleUpdate(
            String updateQueryString,
            Multimap<String, String> columnNamesByTableNames,
            String conditionQueryAppendix,
            Map<String, String> conditionColumnNameByTableNames,
            Object source,
            Object target) {

        String queryString;
        Query query;

        for (String tableName : columnNamesByTableNames.keySet()) {
            Collection<String> columnNames = columnNamesByTableNames.get(tableName);
            String conditionColumnName = conditionColumnNameByTableNames == null ? null : conditionColumnNameByTableNames.get(tableName);
            for (String columnName : columnNames) {
                if (StringUtils.isNotBlank(conditionQueryAppendix) && StringUtils.isNotBlank(conditionColumnName)) {
                    queryString = String.format(updateQueryString.concat(conditionQueryAppendix), tableName, columnName, target, columnName, source, conditionColumnName);
                } else {
                    queryString = String.format(updateQueryString, tableName, columnName, target, columnName, source);
                }

                query = entityManager.createNativeQuery(queryString);
                if (logger.isDebugEnabled()) {
                    logger.debug(queryString);
                }
                query.executeUpdate();
            }
        }
    }

    /**
     * <p>getDatabaseCurrentTimestamp.</p>
     *
     * @return a {@link Timestamp} object.
     */
    protected Timestamp getDatabaseCurrentTimestamp() {
        try {
            final Dialect dialect = getSessionFactory().getJdbcServices().getDialect();
            return Daos.getDatabaseCurrentTimestamp(dataSource, dialect);
        }catch(DataAccessResourceFailureException | SQLException e) {
            throw new SumarisTechnicalException(e);
        }
    }


    protected String getTableName(String entityName) {

        return I18n.t("sumaris.persistence.table."+ entityName.substring(0,1).toLowerCase() + entityName.substring(1));
    }

    protected void checkUpdateDateForUpdate(IUpdateDateEntityBean<?, ? extends Date> source,
                                            IUpdateDateEntityBean<?, ? extends Date> entity) {
        // Check update date
        if (entity.getUpdateDate() != null) {
            Timestamp serverUpdateDtNoMillisecond = Dates.resetMillisecond(entity.getUpdateDate());
            Timestamp sourceUpdateDtNoMillisecond = Dates.resetMillisecond(source.getUpdateDate());
            if (!Objects.equals(sourceUpdateDtNoMillisecond, serverUpdateDtNoMillisecond)) {
                throw new BadUpdateDateException(I18n.t("sumaris.persistence.error.badUpdateDate",
                        getTableName(entity.getClass().getSimpleName()), source.getId(), serverUpdateDtNoMillisecond,
                        sourceUpdateDtNoMillisecond));
            }
        }
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
     * @param query the query
     * @param cb criteria builder
     * @param root the root of the query
     * @param sortAttribute the sort attribute (can be a nested attribute)
     * @param sortDirection the direction
     * @param <T> type of query
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

    /**
     * Compose a Path from root, accepting nested property name
     *
     * @param root the root expression
     * @param attributePath the attribute path, can contains '.'
     * @param <X> Type of Path
     * @return the composed Path
     * @deprecated use Daos.composePath()
     */
    protected <X> Path<X> composePath(Root<?> root, String attributePath) {

        String[] paths = attributePath.split("\\.");
        From<?, ?> from = root; // starting from root
        Path<X> result = null;

        for (int i = 0; i < paths.length; i++) {
            String path = paths[i];

            if (i == paths.length - 1) {
                // last path, get it
                result = from.get(path);
            } else {
                // need a join (find it from existing joins of from)
                Join join = from.getJoins().stream()
                        .filter(j -> j.getAttribute().getName().equals(path))
                        .findFirst().orElse(null);
                if (join == null) {
                    throw new IllegalArgumentException(String.format("the join %s from %s doesn't exists", path, from.getClass().getSimpleName()));
                }
                from = join;
            }
        }

        return result;
    }
}

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
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.dao.technical.Daos;
import net.sumaris.core.dao.technical.Dates;
import net.sumaris.core.dao.technical.model.IEntityBean;
import net.sumaris.core.dao.technical.model.IUpdateDateEntityBean;
import net.sumaris.core.exception.BadUpdateDateException;
import net.sumaris.core.exception.DataLockedException;
import net.sumaris.core.exception.SumarisTechnicalException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.LockOptions;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.dialect.Dialect;
import org.nuiton.i18n.I18n;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.LockTimeoutException;
import javax.persistence.Query;
import javax.sql.DataSource;
import java.io.Serializable;
import java.math.BigInteger;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Objects;

/**
 * <p>HibernateDaoSupport class.</p>
 *
 */
public abstract class HibernateDaoSupport {

    /**
     * Logger.
     */
    protected static final Log logger =
            LogFactory.getLog(HibernateDaoSupport.class);

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

    protected void setSessionFactory(SessionFactory sf) {
        logger.warn("TODO: remove call to setSessionFactory()");
    }

    /**
     * <p>getEntityManager.</p>
     *
     * @return a {@link Session} object.
     */
    protected EntityManager getEntityManager() {
        return entityManager;
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
    protected <T> T load(Class<T> clazz, Serializable id) {

        if (debugEntityLoad) {
            T load = getEntityManager().find(clazz, id);
            if (load == null) {
                throw new DataIntegrityViolationException("Unable to load entity " + clazz.getName() + " with identifier '" + id + "': not found in database.");
            }
        }
        return getEntityManager().getReference(clazz, id);
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
     * @param lockOptions a {@link LockOptions} object.
     * @param <T> a T object.
     * @return a T object.
     */
    @SuppressWarnings("unchecked")
    protected <T extends Serializable> T get(Class<? extends T> clazz, Serializable id, LockModeType lockModeType) {
        T entity = getEntityManager().find(clazz, id);
        getEntityManager().lock(entity, lockModeType);
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

                query = getEntityManager().createNativeQuery(queryString);
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

                query = getEntityManager().createNativeQuery(queryString);
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
            final Dialect dialect = Dialect.getDialect(SumarisConfiguration.getInstance().getConnectionProperties());
            final String sql = dialect.getCurrentTimestampSelectString();
            Object r = Daos.sqlUnique(dataSource, sql);
            return Daos.toTimestampFromJdbcResult(r);
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

    protected void lockForUpdate(IEntityBean<?> entity) {
       lockForUpdate(entity, LockModeType.PESSIMISTIC_WRITE);
    }

    protected void lockForUpdate(IEntityBean<?> entity, LockModeType modeType) {
        // Lock entityName
        try {
            getEntityManager().lock(entity, modeType);
        } catch (LockTimeoutException e) {
            throw new DataLockedException(I18n.t("sumaris.persistence.error.locked",
                    getTableName(entity.getClass().getSimpleName()), entity.getId()), e);
        }
    }

    protected void delete(IEntityBean<?> entity) {
        getEntityManager().remove(entity);
    }
}

package net.sumaris.core.dao.technical.jpa;

/*-
 * #%L
 * SUMARiS:: Core shared
 * %%
 * Copyright (C) 2018 - 2019 SUMARiS Consortium
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
import com.querydsl.jpa.impl.JPAQuery;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.dao.technical.Daos;
import net.sumaris.core.dao.technical.model.IEntity;
import net.sumaris.core.dao.technical.model.IUpdateDateEntityBean;
import net.sumaris.core.dao.technical.model.IValueObject;
import net.sumaris.core.exception.DataLockedException;
import net.sumaris.core.exception.DataNotFoundException;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.util.Beans;
import org.apache.commons.lang3.NotImplementedException;
import org.hibernate.LockOptions;
import org.hibernate.Session;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.nuiton.i18n.I18n;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import javax.persistence.*;
import javax.sql.DataSource;
import java.io.Serializable;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>*
 */
@NoRepositoryBean
public abstract class SumarisJpaRepositoryImpl<E extends IEntity<ID>, ID extends Serializable, V extends IValueObject<ID>>
    extends SimpleJpaRepository<E, ID>
    implements SumarisJpaRepository<E, ID, V> {

    private static final Logger log = LoggerFactory.getLogger(SumarisJpaRepositoryImpl.class);

    private boolean debugEntityLoad = false;
    private boolean checkUpdateDate = true;
    private boolean lockForUpdate = false;

    private EntityManager entityManager;

    private Class<V> voClass;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private SumarisConfiguration configuration;

    protected SumarisJpaRepositoryImpl(Class<E> domainClass, EntityManager entityManager) {
        this(domainClass, null, entityManager);
    }

    protected SumarisJpaRepositoryImpl(Class<E> domainClass, Class<V> voClass, EntityManager entityManager) {
        super(domainClass, entityManager);

        this.voClass = voClass;

        // This is the recommended method for accessing inherited class dependencies.
        this.entityManager = entityManager;
    }

    public boolean isCheckUpdateDate() {
        return checkUpdateDate;
    }

    public void setCheckUpdateDate(boolean checkUpdateDate) {
        this.checkUpdateDate = checkUpdateDate;
    }

    public boolean isLockForUpdate() {
        return lockForUpdate;
    }

    public void setLockForUpdate(boolean lockForUpdate) {
        this.lockForUpdate = lockForUpdate;
    }

    public SumarisConfiguration getConfig() {
        return configuration;
    }

    @Override
    public E createEntity() {
        try {
            return getDomainClass().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public V save(V vo) {
        E entity = toEntity(vo);

        boolean isNew = entity.getId() == null;

        // Entity has update date
        if (entity instanceof IUpdateDateEntityBean && vo instanceof IUpdateDateEntityBean) {

            if (!isNew && isCheckUpdateDate()) {
                // Check update date
                Daos.checkUpdateDateForUpdate((IUpdateDateEntityBean) vo, (IUpdateDateEntityBean) entity);
            }

            // Update update_dt
            ((IUpdateDateEntityBean) entity).setUpdateDate(getDatabaseCurrentTimestamp());
        }

        if (!isNew && isLockForUpdate()) {
            lockForUpdate(entity);
        }

        onBeforeSaveEntity(vo, entity, isNew);

        // Save entity
        E savedEntity = super.save(entity);

        // Update VO
        onAfterSaveEntity(vo, savedEntity, isNew);

        return vo;
    }

    public E toEntity(V vo) {
        Preconditions.checkNotNull(vo);
        E entity;
        if (vo.getId() != null) {
            entity = getOne(vo.getId());
        } else {
            entity = createEntity();
        }

        // Remember the entity's update date
        boolean keepEntityUpdateDate = entity instanceof IUpdateDateEntityBean;
        Date entityUpdateDate = keepEntityUpdateDate
            ? ((IUpdateDateEntityBean) entity).getUpdateDate()
            : null;

        toEntity(vo, entity, true);

        // Restore the update date (can be override by Beans.copyProperties())
        if (keepEntityUpdateDate)
            ((IUpdateDateEntityBean) entity).setUpdateDate(entityUpdateDate);

        return entity;
    }

    public void toEntity(V source, E target, boolean copyIfNull) {
        Beans.copyProperties(source, target);
    }

    protected void onBeforeSaveEntity(V vo, E entity, boolean isNew) {
        // can be overridden
    }

    protected void onAfterSaveEntity(V vo, E savedEntity, boolean isNew) {
        vo.setId(savedEntity.getId());
        // copy updateDate to source vo
        if (savedEntity instanceof IUpdateDateEntityBean && vo instanceof IUpdateDateEntityBean) {
            ((IUpdateDateEntityBean) vo).setUpdateDate(((IUpdateDateEntityBean) savedEntity).getUpdateDate());
        }
    }

    public V toVO(E source) {
        if (source == null) return null;
        V target = createVO();
        toVO(source, target, true);
        return target;
    }

    public void toVO(E source, V target, boolean copyIfNull) {
        Beans.copyProperties(source, target);
    }

    public V createVO() {
        try {
            return getVOClass().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected Class<V> getVOClass() {
        if (voClass == null) throw new NotImplementedException("Not implemented yet. Should be override by subclass or use correct constructor");
        return voClass;
    }

    @Override
    public void deleteById(ID id) {
        if (log.isDebugEnabled()) {
            log.debug(String.format("Deleting %s (id=%s)", getDomainClass().getSimpleName(), id));
        }
        super.deleteById(id);
    }

    /* -- protected method -- */

    protected EntityManager getEntityManager() {
        return entityManager;
    }

    protected Session getSession() {
        return (Session) getEntityManager().getDelegate();
    }

    protected SessionFactoryImplementor getSessionFactory() {
        return (SessionFactoryImplementor) getSession().getSessionFactory();
    }

    protected <C> C load(Class<C> clazz, Serializable id) {

        if (debugEntityLoad) {
            C load = entityManager.find(clazz, id);
            if (load == null) {
                throw new EntityNotFoundException("Unable to load entity " + clazz.getName() + " with identifier '" + id + "': not found in database.");
            }
        }
        return entityManager.unwrap(Session.class).load(clazz, id);
    }

    /**
     * <p>load many entities.</p>
     *
     * @param clazz               a {@link Class} object.
     * @param identifierAttribute name of the identifier attribute
     * @param identifiers         list of identifiers.
     * @param failedIfMissing     Throw error if missing ?
     * @return a list of T object.
     */
    @SuppressWarnings("unchecked")
    protected <C> List<C> loadAll(Class<C> clazz,
                                  String identifierAttribute,
                                  Collection<? extends Serializable> identifiers,
                                  boolean failedIfMissing) {

        List<C> result = getEntityManager().createQuery(String.format("from %s where id in (:id)", clazz.getSimpleName()))
            .setParameter(identifierAttribute, identifiers)
            .getResultList();
        if (failedIfMissing && result.size() != identifiers.size()) {
            throw new EntityNotFoundException(String.format("Unable to load entities %s from ids. Expected %s entities, but found %s entities.",
                clazz.getName(),
                identifiers.size(),
                result.size()));
        }
        return result;
    }

    protected <C> List<C> loadAll(Class<C> clazz,
                                  Collection<? extends Serializable> identifiers,
                                  boolean failedIfMissing) {
        return loadAll(clazz, IEntity.Fields.ID, identifiers, failedIfMissing);
    }

    /**
     * <p>load.</p>
     *
     * @param clazz               a {@link Class} object.
     * @param identifierAttribute name of the identifier attribute
     * @param identifiers         list of identifiers.
     * @param <C>                 a C object.
     * @return a list of T object.
     */
    protected <C> Set<C> loadAllAsSet(Class<C> clazz,
                                      String identifierAttribute,
                                      Collection<? extends Serializable> identifiers,
                                      boolean failedIfMissing) {

        return new HashSet<>(loadAll(clazz, identifierAttribute, identifiers, failedIfMissing));
    }

    protected <C> Set<C> loadAllAsSet(Class<C> clazz,
                                      Collection<? extends Serializable> identifiers,
                                      boolean failedIfMissing) {

        return new HashSet<>(loadAll(clazz, identifiers, failedIfMissing));
    }

    /**
     * <p>find.</p>
     *
     * @param clazz a {@link Class} object.
     * @param id    a {@link Serializable} object.
     * @param <C>   a C object.
     * @return a C object.
     */
    @SuppressWarnings("unchecked")
    protected <C> C find(Class<C> clazz, Serializable id) {
        return this.entityManager.find(clazz, id);
    }

    /**
     * <p>get and lock an entity. Throw a DataNotFoundException if not found</p>
     *
     * @param clazz        a {@link Class} object.
     * @param id           a {@link Serializable} object.
     * @param lockModeType a {@link LockOptions} object.
     * @param <C>          a C object.
     * @return a C object.
     */
    @SuppressWarnings("unchecked")
    protected <C> C getOne(Class<? extends C> clazz, Serializable id, LockModeType lockModeType) throws DataNotFoundException  {
        C entity = getOne(clazz, id);
        entityManager.lock(entity, lockModeType);
        return entity;
    }

    /**
     * <p>get an entity. Throw a DataNotFoundException if not found</p>
     *
     * @param clazz a {@link Class} object.
     * @param id    a {@link Serializable} object.
     * @param <C>   a C object.
     * @return a C object.
     */
    protected <C> C getOne(Class<? extends C> clazz, Serializable id) throws DataNotFoundException {
        C entity = this.entityManager.find(clazz, id); // Can be null
        if (entity == null) throw new DataNotFoundException(I18n.t("sumaris.persistence.error.entityNotFound", clazz.getSimpleName(), id));
        return entity;
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

    protected String getTableName(String entityName) {
        return I18n.t("sumaris.persistence.table." + entityName.substring(0, 1).toLowerCase() + entityName.substring(1));
    }

    protected Timestamp getDatabaseCurrentTimestamp() {

        if (dataSource == null) return new Timestamp(System.currentTimeMillis());

        try {
            final Dialect dialect = getSessionFactory().getJdbcServices().getDialect();
            return Daos.getDatabaseCurrentTimestamp(dataSource, dialect);
        } catch (DataAccessResourceFailureException | SQLException e) {
            throw new SumarisTechnicalException(e);
        }
    }

    protected <T> JPAQuery<T> createQuery() {
        return new JPAQuery<>(entityManager);
    }

    @Override
    protected <S extends E> TypedQuery<S> getQuery(Specification<S> spec, Class<S> domainClass, Sort sort) {
        return applyBindings(super.getQuery(spec, domainClass, sort), spec);
    }

    @Override
    protected <S extends E> TypedQuery<Long> getCountQuery(Specification<S> spec, Class<S> domainClass) {
        return applyBindings(super.getCountQuery(spec, domainClass), spec);
    }

    protected <S extends E, R> TypedQuery<R> applyBindings(TypedQuery<R> query, Specification<S> specification) {
        if (specification instanceof BindableSpecification) {
            BindableSpecification<S> specificationWithParameters = (BindableSpecification<S>) specification;
            specificationWithParameters.getBindings().forEach(binding -> binding.accept(query));
        }
        else if (specification != null){
            String message = "DEPRECATED use of Specification. Please use BindableSpecification instead, to be able to bind parameters";
            if (log.isDebugEnabled()) {
                log.warn(message, new Exception(message));
            }
            else {
                log.warn(message);
            }
        }
        return query;
    }

}

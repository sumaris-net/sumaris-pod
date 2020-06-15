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
import com.google.common.collect.Sets;
import com.querydsl.jpa.impl.JPAQuery;
import net.sumaris.core.dao.technical.Daos;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.dao.technical.model.IEntity;
import net.sumaris.core.dao.technical.model.IValueObject;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.util.Beans;
import org.apache.commons.lang3.NotImplementedException;
import org.hibernate.LockOptions;
import org.hibernate.Session;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.Parameter;
import javax.persistence.TypedQuery;
import javax.sql.DataSource;
import java.io.Serializable;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>*
 */
@NoRepositoryBean
public abstract class SumarisJpaRepositoryImpl<E extends IEntity<ID>, ID extends Serializable, V extends IValueObject<ID>>
        extends SimpleJpaRepository<E, ID>
        implements SumarisJpaRepository<E, ID, V> {

    private boolean debugEntityLoad = false;

    private EntityManager entityManager;

    @Autowired
    private DataSource dataSource;

    protected SumarisJpaRepositoryImpl(Class<E> domainClass, EntityManager entityManager) {
        super(domainClass, entityManager);

        // This is the recommended method for accessing inherited class dependencies.
        this.entityManager = entityManager;

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

        E savedEntity = save(entity);

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

        toEntity(vo, entity, true);

        return entity;
    }

    public void toEntity(V source, E target, boolean copyIfNull) {
        Beans.copyProperties(source, target);
    }

    public V toVO(E source) {
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

    public Class<V> getVOClass() {
        throw new NotImplementedException("Not implemented yet. Should be override by subclass");
    }

    /* -- protected method -- */

    protected void onAfterSaveEntity(V vo, E savedEntity, boolean isNew) {
        vo.setId(savedEntity.getId());
    }

    protected EntityManager getEntityManager() {
        return entityManager;
    }

    protected Session getSession() {
        return (Session) getEntityManager().getDelegate();
    }

    protected SessionFactoryImplementor getSessionFactory() {
        return (SessionFactoryImplementor) getSession().getSessionFactory();
    }


    @SuppressWarnings("unchecked")
    protected <C extends Serializable> C load(Class<C> clazz, Serializable id) {

        if (debugEntityLoad) {
            C load = entityManager.find(clazz, id);
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
     * @param identifierAttribute name of the identifier attribute
     * @param identifiers list of identifiers.
     * @param <T> a T object.
     * @return a list of T object.
     */
    @SuppressWarnings("unchecked")
    protected <C extends Serializable> List<C> loadAll(Class<? extends C> clazz,
                                                       String identifierAttribute,
                                                       Collection<? extends Serializable> identifiers,
                                                       boolean failedIfMissing) {

        List result = getEntityManager().createQuery(String.format("from %s where id in (:id)", clazz.getSimpleName()))
                .setParameter(identifierAttribute, identifiers)
                .getResultList();
        if (failedIfMissing && result.size() != identifiers.size()) {
            throw new DataIntegrityViolationException(String.format("Unable to load entities %s from ids. Expected %s entities, but found %s entities.",
                    clazz.getName(),
                    identifiers.size(),
                    result.size()));
        }
        return (List<C>)result;
    }

    /**
     * <p>load.</p>
     *
     * @param clazz a {@link Class} object.
     * @param identifierAttribute name of the identifier attribute
     * @param identifiers list of identifiers.
     * @param <C> a C object.
     * @return a list of T object.
     */
    @SuppressWarnings("unchecked")
    protected <C extends Serializable> Set<C> loadAllAsSet(Class<? extends C> clazz,
                                                           String identifierAttribute,
                                                           Collection<? extends Serializable> identifiers,
                                                           boolean failedIfMissing) {

        List<C> result = loadAll(clazz, identifierAttribute, identifiers, failedIfMissing);
        return Sets.newHashSet(result);
    }

    /**
     * <p>get.</p>
     *
     * @param clazz a {@link Class} object.
     * @param id a {@link Serializable} object.
     * @param <C> a C object.
     * @return a C object.
     */
    @SuppressWarnings("unchecked")
    protected <C extends Serializable> C get(Class<? extends C> clazz, Serializable id) {
        return this.entityManager.find(clazz, id);
    }

    /**
     * <p>get.</p>
     *
     * @param clazz a {@link Class} object.
     * @param id a {@link Serializable} object.
     * @param lockModeType a {@link LockOptions} object.
     * @param <C> a C object.
     * @return a C object.
     */
    @SuppressWarnings("unchecked")
    protected <C extends Serializable> C get(Class<? extends C> clazz, Serializable id, LockModeType lockModeType) {
        C entity = entityManager.find(clazz, id);
        entityManager.lock(entity, lockModeType);
        return entity;
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

    protected <T extends IEntity<?>> Specification<T> and(Specification<T>... specs) {
        Specification<T> result = null;
        for (Specification<T> item: specs) {
            if (item != null) {
                if (result == null) {
                    result = Specification.where(item);
                }
                else {
                    result.and(item);
                }
            }
        }
        return result;
    }

    protected Pageable getPageable(int offset, int size, String sortAttribute, SortDirection sortDirection) {
        if (sortAttribute != null) {
            return PageRequest.of(offset / size, size,
                    (sortDirection == null) ? Sort.Direction.ASC :
                            Sort.Direction.fromString(sortDirection.toString()),
                    sortAttribute);
        }
        return PageRequest.of(offset / size, size);
    }

    protected Pageable getPageable(Page page) {
        return getPageable((int)page.getOffset(), page.getSize(), page.getSortAttribute(), page.getSortDirection());
    }

    protected <E, T extends Object> TypedQuery<E> setParameterIfExists(TypedQuery<E> query, String parameterName, T value) {
        try {
            Parameter<T> parameter = (Parameter<T>) query.getParameter(parameterName, Object.class);
            if (parameter != null) query.setParameter(parameter, value);
        }
        catch(IllegalArgumentException iae) {
            // Not found
        }
        return query;
    }

}

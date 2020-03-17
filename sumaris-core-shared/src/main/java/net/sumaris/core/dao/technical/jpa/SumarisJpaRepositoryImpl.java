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

import com.querydsl.jpa.impl.JPAQuery;
import net.sumaris.core.dao.technical.Daos;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.dao.technical.model.IEntity;
import net.sumaris.core.exception.SumarisTechnicalException;
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

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.sql.DataSource;
import java.io.Serializable;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>*
 */
public class SumarisJpaRepositoryImpl<T, ID extends Serializable>
        extends SimpleJpaRepository<T, ID>
        implements SumarisJpaRepository<T, ID> {

    private boolean debugEntityLoad = false;

    private EntityManager entityManager;

    @Autowired
    private DataSource dataSource;

    // There are two constructors to choose from, either can be used.
    public SumarisJpaRepositoryImpl(Class<T> domainClass, EntityManager entityManager) {
        super(domainClass, entityManager);

        // This is the recommended method for accessing inherited class dependencies.
        this.entityManager = entityManager;

    }

    @Override
    public T createEntity() {
        try {
            return getDomainClass().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <C extends Serializable> C load(Class<C> clazz, Serializable id) {

        if (debugEntityLoad) {
            C load = entityManager.find(clazz, id);
            if (load == null) {
                throw new DataIntegrityViolationException("Unable to load entity " + clazz.getName() + " with identifier '" + id + "': not found in database.");
            }
        }
        return entityManager.unwrap(Session.class).load(clazz, id);
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
    public <C extends Serializable> C get(Class<? extends C> clazz, Serializable id) {
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
    public <C extends Serializable> C get(Class<? extends C> clazz, Serializable id, LockModeType lockModeType) {
        C entity = entityManager.find(clazz, id);
        entityManager.lock(entity, lockModeType);
        return entity;
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
}

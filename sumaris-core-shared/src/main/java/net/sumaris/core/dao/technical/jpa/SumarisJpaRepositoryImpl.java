package net.sumaris.core.dao.technical.jpa;

import com.querydsl.jpa.impl.JPAQuery;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.dao.technical.Daos;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.dao.technical.model.IEntity;
import net.sumaris.core.exception.SumarisTechnicalException;
import org.hibernate.LockOptions;
import org.hibernate.Session;
import org.hibernate.dialect.Dialect;
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
import javax.sql.DataSource;
import java.io.Serializable;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Arrays;

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
     * @param <T> a T object.
     * @return a T object.
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
     * @param lockOptions a {@link LockOptions} object.
     * @param <T> a T object.
     * @return a T object.
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

    protected Timestamp getDatabaseCurrentTimestamp() {

        if (dataSource == null) return new Timestamp(System.currentTimeMillis());

        try {
            final Dialect dialect = Dialect.getDialect(SumarisConfiguration.getInstance().getConnectionProperties());
            final String sql = dialect.getCurrentTimestampSelectString();
            Object r = Daos.sqlUnique(dataSource, sql);
            return Daos.toTimestampFromJdbcResult(r);
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

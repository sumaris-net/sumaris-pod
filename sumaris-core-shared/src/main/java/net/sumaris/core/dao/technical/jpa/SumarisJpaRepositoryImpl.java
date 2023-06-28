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
import com.google.common.collect.ImmutableMap;
import com.querydsl.jpa.impl.JPAQuery;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.config.SumarisConfigurationOption;
import net.sumaris.core.dao.technical.Daos;
import net.sumaris.core.dao.technical.DatabaseType;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.model.IEntity;
import net.sumaris.core.model.IUpdateDateEntity;
import net.sumaris.core.model.IValueObject;
import net.sumaris.core.model.function.ToEntityFunction;
import net.sumaris.core.event.entity.EntityDeleteEvent;
import net.sumaris.core.event.entity.EntityInsertEvent;
import net.sumaris.core.event.entity.EntityUpdateEvent;
import net.sumaris.core.exception.DataLockedException;
import net.sumaris.core.exception.DataNotFoundException;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.util.Beans;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.LockOptions;
import org.hibernate.Session;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.nuiton.i18n.I18n;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.query.QueryUtils;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.lang.Nullable;

import javax.annotation.PostConstruct;
import javax.persistence.*;
import javax.persistence.criteria.*;
import javax.sql.DataSource;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;
import java.util.function.LongSupplier;
import java.util.stream.Stream;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>*
 */
@NoRepositoryBean
@Slf4j
public abstract class SumarisJpaRepositoryImpl<E extends IEntity<ID>, ID extends Serializable, V extends IValueObject<ID>>
    extends SimpleJpaRepository<E, ID>
    implements SumarisJpaRepository<E, ID, V> {

    private boolean debugEntityLoad = false;
    private boolean checkUpdateDate = true;
    private boolean publishEvent = false;
    private boolean lockForUpdate = false;
    private LockModeType lockForUpdateMode;
    private Map<String, Object> lockForUpdateProperties;
    private EntityManager entityManager;
    private Class<V> voClass;
    private final String entityName;

    @Autowired
    private DataSource dataSource;

    private DatabaseType databaseType;

    @Autowired
    private SumarisConfiguration configuration;

    @Autowired
    private ApplicationEventPublisher publisher;


    protected SumarisJpaRepositoryImpl(Class<E> domainClass, EntityManager entityManager) {
        this(domainClass, null, entityManager);
    }

    protected SumarisJpaRepositoryImpl(Class<E> domainClass, Class<V> voClass, EntityManager entityManager) {
        super(domainClass, entityManager);
        this.entityName = domainClass.getSimpleName();
        this.voClass = voClass;

        // This is the recommended method for accessing inherited class dependencies.
        this.entityManager = entityManager;
    }

    @PostConstruct
    private void setup() {
        // Set lock timeout - see lockForUpdate()
        lockForUpdateProperties = ImmutableMap.of(SumarisConfigurationOption.LOCK_TIMEOUT.getKey(), configuration.getLockTimeout());
        lockForUpdateMode = configuration.getLockModeType();
        databaseType = configuration.getDatabaseType();
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

    public LockModeType getLockForUpdateMode() {
        return lockForUpdateMode;
    }

    public void setLockForUpdateMode(LockModeType lockForUpdateMode) {
        this.lockForUpdateMode = lockForUpdateMode;
    }

    public boolean getPublishEvent() {
        return publishEvent;
    }

    public void setPublishEvent(boolean publishEvent) {
        this.publishEvent = publishEvent;
    }

    public String getEntityName() {
        return entityName;
    }

    public SumarisConfiguration getConfig() {
        return configuration;
    }

    public DatabaseType getDatabaseType() {
        return databaseType;
    }

    @Override
    public E createEntity() {
        try {
            return getDomainClass().getConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public V save(V source) {
        return save(source, isCheckUpdateDate(), isLockForUpdate());
    }

    public E toEntity(V vo) {
        Preconditions.checkNotNull(vo);
        E entity;
        if (vo.getId() != null) {
            entity = getById(vo.getId());
        } else {
            entity = createEntity();
        }

        // Remember the entity's update date
        boolean keepEntityUpdateDate = entity instanceof IUpdateDateEntity;
        Date entityUpdateDate = keepEntityUpdateDate
            ? ((IUpdateDateEntity) entity).getUpdateDate()
            : null;

        toEntity(vo, entity, true);

        // Restore the update date (can be override by Beans.copyProperties())
        if (keepEntityUpdateDate)
            ((IUpdateDateEntity) entity).setUpdateDate(entityUpdateDate);

        return entity;
    }

    public void toEntity(V source, E target, boolean copyIfNull) {
        Beans.copyProperties(source, target);
    }

    public V save(V source, boolean checkUpdateDate, boolean lockForUpdate) {
        E entity = toEntity(source);

        boolean isNew = entity.getId() == null;

        // Entity has update date
        if (entity instanceof IUpdateDateEntity && source instanceof IUpdateDateEntity) {

            if (!isNew && checkUpdateDate) {
                // Check update date
                Daos.checkUpdateDateForUpdate((IUpdateDateEntity) source, (IUpdateDateEntity) entity);
            }

            // Update update_dt
            ((IUpdateDateEntity) entity).setUpdateDate(getDatabaseCurrentDate());
        }

        if (!isNew && lockForUpdate) {
            lockForUpdate(entity);
        }

        onBeforeSaveEntity(source, entity, isNew);

        // Save entity
        E savedEntity = save(entity);

        // Update VO
        onAfterSaveEntity(source, savedEntity, isNew);

        if (publishEvent) publishSaveEvent(source, isNew);

        return source;
    }

    protected void onBeforeSaveEntity(V source, E target, boolean isNew) {
        // can be overridden
    }

    protected void onAfterSaveEntity(V vo, E savedEntity, boolean isNew) {
        vo.setId(savedEntity.getId());
        // copy updateDate to source vo
        if (savedEntity instanceof IUpdateDateEntity && vo instanceof IUpdateDateEntity) {
            ((IUpdateDateEntity) vo).setUpdateDate(((IUpdateDateEntity) savedEntity).getUpdateDate());
        }
    }

    protected void publishSaveEvent(V vo, boolean isNew) {
        // Publish event
        if (isNew) {
            publisher.publishEvent(new EntityInsertEvent(vo.getId(), entityName, vo));
        } else {
            publisher.publishEvent(new EntityUpdateEvent(vo.getId(), entityName, vo));
        }
    }

    protected void publishDeleteEvent(V vo) {
        publisher.publishEvent(new EntityDeleteEvent(vo.getId(), entityName, vo));
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
            return getVOClass().getConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected Class<V> getVOClass() {
        if (voClass == null) throw new NotImplementedException("Not implemented yet. Should be override by subclass or use correct constructor");
        return voClass;
    }

    @Override
    public void deleteAllById(Iterable<? extends ID> ids) {
        // Simple deletion (no event to publish)
        if (!publishEvent) {
            super.deleteAllById(ids);
            return;
        }

        // First, load entity to deleted
        List<V> vos = findAllById((Iterable<ID>) ids).stream().map(this::toVO).toList();

        if (CollectionUtils.isEmpty(vos)) return; // Nothing to delete

        // Do deletion
        super.deleteAllById(Beans.collectIds(vos));

        // Emit delete events
        vos.forEach(this::publishDeleteEvent);
    }

    @Override
    public void deleteById(ID id) {
        log.debug("Deleting {}#{}", entityName, id);

        // Simple deletion (no event to publish)
        if (!publishEvent) {
            super.deleteById(id);
            return;
        }

        // First, load entity to deleted
        V vo = findById(id).map(this::toVO).orElse(null);

        if (vo == null) return; // Nothing to delete

        // Do deletion
        super.deleteById(id);

        // Emit delete event
        publishDeleteEvent(vo);
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

    protected <C> C getReference(Class<C> clazz, Serializable id) {

        if (debugEntityLoad) {
            C load = entityManager.find(clazz, id);
            if (load == null) {
                throw new EntityNotFoundException("Unable to load entity " + clazz.getName() + " with identifier '" + id + "': not found in database.");
            }
        }
        return entityManager.getReference(clazz, id);
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
        return this.entityManager.find(clazz, id, null, null);
    }

    /**
     * <p>find.</p>
     *
     * @param clazz a {@link Class} object.
     * @param id    a {@link Serializable} object.
     * @param lockModeType    a {@link LockModeType} object.
     * @param <C>   a C object.
     * @return a C object.
     */
    @SuppressWarnings("unchecked")
    protected <C> C find(Class<C> clazz, Serializable id, LockModeType lockModeType) {
        return this.entityManager.find(clazz, id, lockModeType);
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
    protected <C> C getById(Class<? extends C> clazz, Serializable id, LockModeType lockModeType) throws DataNotFoundException  {
        C entity = getById(clazz, id);
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
    protected <C> C getById(Class<? extends C> clazz, Serializable id) throws DataNotFoundException {
        C entity = this.entityManager.find(clazz, id); // Can be null
        if (entity == null) throw new DataNotFoundException(I18n.t("sumaris.persistence.error.entityNotFound", clazz.getSimpleName(), id));
        return entity;
    }

    protected Stream<E> streamAll(@Nullable Specification<E> spec, Sort sort) {
        return this.getQuery(spec, sort).getResultStream();
    }

    protected Stream<E> streamAll(@Nullable Specification<E> spec) {
        return this.getQuery(spec, Sort.unsorted()).getResultStream();
    }

    protected <S> Stream<S> streamQuery(TypedQuery<S> query) {
        return query.getResultList().stream();
    }

    protected <S extends E> TypedQuery<S> getQuery(@Nullable Specification<S> spec,
                                                   @Nullable net.sumaris.core.dao.technical.Page page,
                                                   Class<S> domainClass) {
        if (page == null) {
            return getQuery(spec, domainClass, Pageable.unpaged());
        }

        return getQuery(spec, (int)page.getOffset(), page.getSize(), page.getSortBy(), page.getSortDirection(), domainClass);
    }

    protected <S extends E> TypedQuery<S> getQuery(@Nullable Specification<S> spec,
                                                   int offset, int size,
                                                   String sortBy, SortDirection sortDirection,
                                                   Class<S> domainClass) {
        CriteriaBuilder builder = this.getEntityManager().getCriteriaBuilder();
        CriteriaQuery<S> criteriaQuery = builder.createQuery(domainClass);
        Root<S> root = criteriaQuery.from(domainClass);

        Predicate predicate = spec != null ? spec.toPredicate(root, criteriaQuery, builder) : null;
        if (predicate != null) criteriaQuery.where(predicate);

        // Add sorting
        addSorting(criteriaQuery, builder, root, sortBy, sortDirection);

        TypedQuery<S> query = getEntityManager().createQuery(criteriaQuery);

        // Bind parameters
        applyBindings(query, spec);

        // Set page
        query.setFirstResult(offset);
        query.setMaxResults(size);

        return query;
    }

    protected <T> Page<T> readPage(TypedQuery<T> query, Pageable pageable, LongSupplier totalSupplier) {
        if (pageable.isPaged()) {
            query.setFirstResult((int)pageable.getOffset());
            query.setMaxResults(pageable.getPageSize());
        }

        return PageableExecutionUtils.getPage(query.getResultList(), pageable, totalSupplier);
    }

    protected void lockForUpdate(IEntity<?> entity) {
        lockForUpdate(entity, (LockModeType)null, (Map)null);
    }

    protected void lockForUpdate(IEntity<?> entity, LockModeType modeType) {
        lockForUpdate(entity, modeType, (Map)null);
    }

    protected void lockForUpdate(IEntity<?> entity, LockModeType modeType, Map<String, Object> properties) {
        modeType = modeType != null ? modeType : getLockForUpdateMode();

        properties = properties != null ? properties : lockForUpdateProperties;
        // Lock entityName
        try {
            entityManager.lock(entity, modeType, properties);
        } catch (LockTimeoutException e) {
            throw new DataLockedException(I18n.t("sumaris.persistence.error.locked",
                getTableName(Daos.getEntityName(entity)), entity.getId()), e);
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

    protected Date getDatabaseCurrentDate() {

        if (dataSource == null) return new Date(System.currentTimeMillis());

        try {
            final Dialect dialect = getSessionFactory().getJdbcServices().getDialect();
            return Daos.getDatabaseCurrentDate(dataSource, dialect);
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

    /**
     * Add a orderBy on query
     *
     * @param query         the query
     * @param builder       criteria builder
     * @param root          the root of the query
     * @param pageable      page spec
     * @param <T>           type of query
     * @return the query itself
     */
    protected <T> CriteriaQuery<T> addSorting(CriteriaQuery<T> query,
                                              CriteriaBuilder builder,
                                              Root<?> root,
                                              Pageable pageable) {
        Sort sort = pageable.isPaged() ? pageable.getSort() : Sort.unsorted();
        if (sort.isSorted()) {
            query.orderBy(QueryUtils.toOrders(sort, root, builder));
        }
        return query;
    }
    /**
     * Add a orderBy on query
     *
     * @param query         the query
     * @param builder       criteria builder
     * @param root          the root of the query
     * @param sortAttribute the sort attribute (can be a nested attribute)
     * @param sortDirection the direction
     * @param <T>           type of query
     * @return the query itself
     */
    protected <T> CriteriaQuery<T> addSorting(CriteriaQuery<T> query,
                                              CriteriaBuilder builder,
                                              Root<?> root, String sortAttribute, SortDirection sortDirection) {
        // Add sorting
        if (StringUtils.isNotBlank(sortAttribute)) {
            Expression<?> sortExpression = Daos.composePath(root, sortAttribute);
            query.orderBy(SortDirection.DESC.equals(sortDirection) ?
                builder.desc(sortExpression) :
                builder.asc(sortExpression)
            );
        }
        return query;
    }

    protected <ID extends Serializable, CV extends IValueObject<ID>, CT extends IEntity<ID>, PT extends IUpdateDateEntity<?, Date>>
        List<CV> saveChildren(List<CV> sources,
                              List<CT> targets,
                              Class<CT> targetClass,
                              ToEntityFunction<ID, CV, CT> toEntity,
                              PT parent) {
        Preconditions.checkNotNull(parent);

        final EntityManager em = getEntityManager();
        final Date updateDate = parent.getUpdateDate();

        Map<ID, CT> entitiesToRemove = Beans.splitById(targets);

        Beans.getStream(sources)
            .forEach(source -> {
                CT target = source.getId() != null ? entitiesToRemove.remove(source.getId()) : null;
                boolean isNew = (target == null);
                if (isNew) {
                    source.setId(null); // Reset id, because not found (to avoid copy into target)
                    try {
                        target = targetClass.getConstructor().newInstance();
                    } catch (Exception e) {
                        throw new SumarisTechnicalException(e);
                    }
                }

                // Copy update, from parent
                if (updateDate != null && source instanceof IUpdateDateEntity) {
                    ((IUpdateDateEntity<ID, Date>)source).setUpdateDate(updateDate);
                }

                // Convert to entity
                toEntity.call(source, target, true);

                if (isNew) {
                    em.persist(target);
                    source.setId(target.getId());
                } else {
                    em.merge(target);
                }
            });


        // Remove unused entities
        if (MapUtils.isNotEmpty(entitiesToRemove)) {
            entitiesToRemove.values().forEach(em::remove);
        }

        return CollectionUtils.isEmpty(sources) ? null : sources;
    }
}

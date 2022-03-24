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
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.config.CacheConfiguration;
import net.sumaris.core.dao.technical.Daos;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.dao.technical.hibernate.HibernateDaoSupport;
import net.sumaris.core.dao.technical.model.IEntity;
import net.sumaris.core.dao.technical.model.ITreeNodeEntityBean;
import net.sumaris.core.dao.technical.model.IUpdateDateEntity;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.model.referential.*;
import net.sumaris.core.util.Beans;
import net.sumaris.core.vo.filter.IReferentialFilter;
import net.sumaris.core.vo.filter.ReferentialFilterVO;
import net.sumaris.core.vo.referential.IReferentialVO;
import net.sumaris.core.vo.referential.ReferentialTypeVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.nuiton.i18n.I18n;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Repository("referentialDao")
@Slf4j
public class ReferentialDaoImpl
    extends HibernateDaoSupport
    implements ReferentialDao {

    protected  <T extends IReferentialEntity> Stream<T> streamByFilter(final Class<T> entityClass,
                                                                   IReferentialFilter filter,
                                                                   int offset,
                                                                   int size,
                                                                   String sortAttribute,
                                                                   SortDirection sortDirection) {
        Preconditions.checkNotNull(entityClass, "Missing 'entityClass' argument");
        Preconditions.checkNotNull(filter, "Missing 'filter' argument");

        return createFindQuery(entityClass, filter, sortAttribute, sortDirection)
            .setFirstResult(offset)
            .setMaxResults(size)
            // FIXME BLA: replace with getResultStream()
            // Now, it failed at startup (error "invalid cursor state: identified cursor is not open")
            .getResultList().stream();
    }

    @Override
    public List<ReferentialVO> findByFilter(final String entityName,
                                            IReferentialFilter filter,
                                            int offset,
                                            int size,
                                            String sortAttribute,
                                            SortDirection sortDirection) {
        Preconditions.checkNotNull(entityName, "Missing entityName argument");

        // Get entity class from entityName
        Class<? extends IReferentialEntity> entityClass = ReferentialEntities.getEntityClass(entityName);

        return streamByFilter(entityClass, filter, offset, size, sortAttribute, sortDirection)
            .map(s -> toVO(entityName, s))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    @Override
    public Long countByFilter(final String entityName, IReferentialFilter filter) {
        Preconditions.checkNotNull(entityName, "Missing entityName argument");
        Preconditions.checkNotNull(filter);

        // Get entity class from entityName
        Class<? extends IReferentialEntity> entityClass = ReferentialEntities.getEntityClass(entityName);

        return createCountQuery(entityClass, filter).getSingleResult();
    }

    @Override
    @Cacheable(cacheNames = CacheConfiguration.Names.LOCATION_LEVEL_BY_LABEL, key = "#label", condition = "#entityName == 'LocationLevel'")
    public Optional<ReferentialVO> findByUniqueLabel(String entityName, String label) {
        Preconditions.checkNotNull(entityName, "Missing entityName argument");
        Preconditions.checkNotNull(label);

        // Get entity class from entityName
        Class<? extends IReferentialEntity> entityClass = ReferentialEntities.getEntityClass(entityName);

        IReferentialEntity result = null;
        try {
            result = createFindByUniqueLabelQuery(entityClass, label).getSingleResult();
        } catch (NoResultException e) {
            // let result to null
        }
        return result == null ? Optional.empty() : Optional.of(toVO(entityName, result));
    }

    @Override
    public Date getLastUpdateDate(Collection<String> entityNames) {
        return entityNames.parallelStream()
            .map(entityName -> {
                try {
                    return this.maxUpdateDate(entityName);
                } catch (Exception e) {
                    log.warn(String.format("Error while getting max(updateDate) of entity %s: %s", entityName, e.getMessage()), e);
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .max(Comparator.naturalOrder())
            .orElse(null);
    }

    @Override
    @Cacheable(cacheNames = CacheConfiguration.Names.REFERENTIAL_TYPES)
    public List<ReferentialTypeVO> getAllTypes() {
        return ReferentialEntities.CLASSES_BY_NAME.keySet().stream()
            .map(this::getTypeByEntityName)
            .collect(Collectors.toList());
    }

    @Override
    public ReferentialVO get(String entityName, int id) {
        Class<? extends IReferentialEntity> entityClass = ReferentialEntities.getEntityClass(entityName);
        return get(entityClass, id);
    }

    @Override
    public ReferentialVO get(Class<? extends IReferentialEntity> entityClass, int id) {
        return toVO(getById(entityClass, id));
    }

    @Override
    public List<ReferentialVO> getAllLevels(final String entityName) {

        return ReferentialEntities.getLevelProperty(entityName)
                .map(levelDescriptor -> {
                    String levelEntityName = levelDescriptor.getPropertyType().getSimpleName();
                    return findByFilter(levelEntityName, ReferentialFilterVO.builder().build(), 0, 1000, IItemReferentialEntity.Fields.NAME, SortDirection.ASC);
                })
                .orElseGet(ImmutableList::of);
    }

    @Override
    public ReferentialVO getLevelById(String entityName, int levelId) {
        Preconditions.checkNotNull(entityName, "Missing entityName argument");

        PropertyDescriptor levelDescriptor = ReferentialEntities.getLevelProperty(entityName)
                .orElseThrow(() -> new DataRetrievalFailureException("Unable to find level with id=" + levelId + " for entityName=" + entityName));

        Class<?> levelClass = levelDescriptor.getPropertyType();
        if (!IReferentialEntity.class.isAssignableFrom(levelClass)) {
            throw new DataRetrievalFailureException("Unable to convert class=" + levelClass.getName() + " to a referential bean");
        }

        return toVO(levelClass.getSimpleName(), (IReferentialEntity) getById(levelClass, levelId));
    }

    @Override
    public <T extends IReferentialEntity> ReferentialVO toVO(T source) {
        if (source == null)
            throw new EntityNotFoundException();

        return toVO(getEntityName(source), source);
    }

    @Override
    public <T extends IReferentialVO, S extends IReferentialEntity> Optional<T> toTypedVO(S source, Class<T> targetClazz) {
        if (source == null)
            return Optional.empty();

        try {
            T target = targetClazz.newInstance();
            Beans.copyProperties(source, target);
            return Optional.of(target);
        } catch (IllegalAccessException | InstantiationException e) {
            throw new SumarisTechnicalException(e.getMessage(), e);
        }
    }

    public void clearCache() {
        log.debug("Cleaning all referential cache...");

        ReferentialEntities.ROOT_CLASSES.stream()
            .map(Class::getSimpleName)
            .forEach(this::clearCache);
    }

    @Caching(evict = {
        @CacheEvict(cacheNames = CacheConfiguration.Names.REFERENTIAL_MAX_UPDATE_DATE_BY_TYPE, key = "#entityName"),
        @CacheEvict(cacheNames = CacheConfiguration.Names.PERSON_BY_ID, allEntries = true, condition = "#entityName == 'Person'"),
        @CacheEvict(cacheNames = CacheConfiguration.Names.PERSON_BY_PUBKEY, allEntries = true, condition = "#entityName == 'Person'"),
        @CacheEvict(cacheNames = CacheConfiguration.Names.DEPARTMENT_BY_ID, allEntries = true, condition = "#entityName == 'Department'"),
        @CacheEvict(cacheNames = CacheConfiguration.Names.DEPARTMENT_BY_LABEL, allEntries = true, condition = "#entityName == 'Department'"),
        @CacheEvict(cacheNames = CacheConfiguration.Names.PMFM_BY_ID, allEntries = true, condition = "#entityName == 'Pmfm'"),
        @CacheEvict(cacheNames = CacheConfiguration.Names.PMFM_COMPLETE_NAME_BY_ID, allEntries = true, condition = "#entityName == 'Pmfm'"),
        @CacheEvict(cacheNames = CacheConfiguration.Names.PMFM_HAS_SUFFIX, allEntries = true, condition = "#entityName == 'Pmfm'"),
        @CacheEvict(cacheNames = CacheConfiguration.Names.PMFM_HAS_PREFIX, allEntries = true, condition = "#entityName == 'Pmfm'"),
        @CacheEvict(cacheNames = CacheConfiguration.Names.PMFM_HAS_PARAMETER_GROUP, allEntries = true, condition = "#entityName == 'Pmfm'"),
        @CacheEvict(cacheNames = CacheConfiguration.Names.PROGRAM_BY_ID, allEntries = true, condition = "#entityName == 'Program'"),
        @CacheEvict(cacheNames = CacheConfiguration.Names.PROGRAM_BY_LABEL, allEntries = true, condition = "#entityName == 'Program'"),
        @CacheEvict(cacheNames = CacheConfiguration.Names.PROGRAM_PRIVILEGE_BY_ID, allEntries = true, condition = "#entityName == 'ProgramPrivilege'"),
        @CacheEvict(cacheNames = CacheConfiguration.Names.LOCATION_BY_ID, allEntries = true, condition = "#entityName == 'Location'"),
        @CacheEvict(cacheNames = CacheConfiguration.Names.LOCATION_LEVEL_BY_LABEL, allEntries = true, condition = "#entityName == 'LocationLevel'"),
        @CacheEvict(cacheNames = CacheConfiguration.Names.TAXON_NAME_BY_ID, allEntries = true, condition = "#entityName == 'TaxonName'"),
        @CacheEvict(cacheNames = CacheConfiguration.Names.TAXON_NAME_BY_FILTER, allEntries = true, condition = "#entityName == 'TaxonName'"),
        @CacheEvict(cacheNames = CacheConfiguration.Names.TAXON_NAME_BY_TAXON_REFERENCE_ID, allEntries = true, condition = "#entityName == 'TaxonName' || #entityName == 'ReferenceTaxon'"),
        @CacheEvict(cacheNames = CacheConfiguration.Names.TAXON_NAMES_BY_TAXON_GROUP_ID, allEntries = true, condition = "#entityName == 'TaxonName' || #entityName == 'TaxonGroup'"),
        @CacheEvict(cacheNames = CacheConfiguration.Names.TAXONONOMIC_LEVEL_BY_ID, allEntries = true, condition = "#entityName == 'TaxonomicLevel'"),
        @CacheEvict(cacheNames = CacheConfiguration.Names.GEAR_BY_ID, allEntries = true, condition = "#entityName == 'Gear'")
    })
    public void clearCache(String entityName) {
        log.debug("Cleaning {}'s cache...", entityName);
    }

    @Caching(evict = {
        @CacheEvict(cacheNames = CacheConfiguration.Names.REFERENTIAL_MAX_UPDATE_DATE_BY_TYPE, key = "#entityName"),
        @CacheEvict(cacheNames = CacheConfiguration.Names.PERSON_BY_ID, key = "#id", condition = "#entityName == 'Person'"),
        @CacheEvict(cacheNames = CacheConfiguration.Names.PERSON_BY_PUBKEY, allEntries = true, condition = "#entityName == 'Person'"),
        @CacheEvict(cacheNames = CacheConfiguration.Names.DEPARTMENT_BY_ID, key = "#id", condition = "#entityName == 'Department'"),
        @CacheEvict(cacheNames = CacheConfiguration.Names.DEPARTMENT_BY_LABEL, allEntries = true, condition = "#entityName == 'Department'"),
        @CacheEvict(cacheNames = CacheConfiguration.Names.PMFM_BY_ID, key = "#id", condition = "#entityName == 'Pmfm'"),
        @CacheEvict(cacheNames = CacheConfiguration.Names.PMFM_COMPLETE_NAME_BY_ID, key = "#id", condition = "#entityName == 'Pmfm'"),
        @CacheEvict(cacheNames = CacheConfiguration.Names.PMFM_HAS_SUFFIX, allEntries = true, condition = "#entityName == 'Pmfm'"),
        @CacheEvict(cacheNames = CacheConfiguration.Names.PMFM_HAS_PREFIX, allEntries = true, condition = "#entityName == 'Pmfm'"),
        @CacheEvict(cacheNames = CacheConfiguration.Names.PMFM_HAS_PARAMETER_GROUP, allEntries = true, condition = "#entityName == 'Pmfm'"),
        @CacheEvict(cacheNames = CacheConfiguration.Names.PROGRAM_BY_ID, key = "#id", condition = "#entityName == 'Program'"),
        @CacheEvict(cacheNames = CacheConfiguration.Names.PROGRAM_BY_LABEL, allEntries = true, condition = "#entityName == 'Program'"),
        @CacheEvict(cacheNames = CacheConfiguration.Names.PROGRAM_PRIVILEGE_BY_ID, key = "#id", condition = "#entityName == 'ProgramPrivilege'"),
        @CacheEvict(cacheNames = CacheConfiguration.Names.LOCATION_BY_ID, key = "#id", condition = "#entityName == 'Location'"),
        @CacheEvict(cacheNames = CacheConfiguration.Names.LOCATION_LEVEL_BY_LABEL, allEntries = true, condition = "#entityName == 'LocationLevel'"),
        @CacheEvict(cacheNames = CacheConfiguration.Names.TAXON_NAME_BY_ID, key = "#id", condition = "#entityName == 'TaxonName'"),
        @CacheEvict(cacheNames = CacheConfiguration.Names.TAXON_NAME_BY_FILTER, allEntries = true, condition = "#entityName == 'TaxonName'"),
        @CacheEvict(cacheNames = CacheConfiguration.Names.TAXON_NAME_BY_TAXON_REFERENCE_ID, allEntries = true, condition = "#entityName == 'TaxonName' || #entityName == 'ReferenceTaxon'"),
        @CacheEvict(cacheNames = CacheConfiguration.Names.TAXON_NAMES_BY_TAXON_GROUP_ID, allEntries = true, condition = "#entityName == 'TaxonName' || #entityName == 'TaxonGroup'"),
        @CacheEvict(cacheNames = CacheConfiguration.Names.TAXONONOMIC_LEVEL_BY_ID, key = "#id", condition = "#entityName == 'TaxonomicLevel'"),
        @CacheEvict(cacheNames = CacheConfiguration.Names.GEAR_BY_ID, key = "#id", condition = "#entityName == 'Gear'")
    })
    public void clearCache(String entityName, int id) {
        log.debug("Cleaning {}#{} cache...", entityName, id);
    }

    @Override
    @Caching(evict = {
        @CacheEvict(cacheNames = CacheConfiguration.Names.PERSON_BY_ID, key = "#id", condition = "#entityName == 'Person'"),
        @CacheEvict(cacheNames = CacheConfiguration.Names.DEPARTMENT_BY_ID, key = "#id", condition = "#entityName == 'Department'"),
        @CacheEvict(cacheNames = CacheConfiguration.Names.PMFM_BY_ID, key = "#id", condition = "#entityName == 'Pmfm'"),
        @CacheEvict(cacheNames = CacheConfiguration.Names.PROGRAM_BY_ID, key = "#id", condition = "#entityName == 'Program'"),
        @CacheEvict(cacheNames = CacheConfiguration.Names.PROGRAM_PRIVILEGE_BY_ID, key = "#id", condition = "#entityName == 'ProgramPrivilege'"),
        @CacheEvict(cacheNames = CacheConfiguration.Names.LOCATION_BY_ID, key = "#id", condition = "#entityName == 'Location'"),
        @CacheEvict(cacheNames = CacheConfiguration.Names.TAXON_NAME_BY_ID, key = "#id", condition = "#entityName == 'TaxonName'"),
        @CacheEvict(cacheNames = CacheConfiguration.Names.TAXONONOMIC_LEVEL_BY_ID, key = "#id", condition = "#entityName == 'TaxonomicLevel'"),
        @CacheEvict(cacheNames = CacheConfiguration.Names.GEAR_BY_ID, key = "#id", condition = "#entityName == 'Gear'")
    })
    public void delete(final String entityName, int id) {

        // Get the entity class
        Class<? extends IReferentialEntity> entityClass = ReferentialEntities.getEntityClass(entityName);

        IReferentialEntity entity = find(entityClass, id);
        if (entity != null) {
            log.debug("Deleting {}#{}", entityName, id);
            getEntityManager().remove(entity);

            // Cleaning entity cache
            clearCache(entityName, id);
        }
    }

    @Override
    public Long count(String entityName) {
        Preconditions.checkNotNull(entityName, "Missing entityName argument");

        // Get entity class from entityName
        Class<? extends IReferentialEntity> entityClass = ReferentialEntities.getEntityClass(entityName);

        CriteriaBuilder builder = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<Long> criteriaQuery = builder.createQuery(Long.class);
        criteriaQuery.select(builder.count(criteriaQuery.from(entityClass)));

        return getEntityManager().createQuery(criteriaQuery).getSingleResult();
    }

    @Override
    public Long countByLevelId(String entityName, Integer... levelIds) {
        Preconditions.checkNotNull(entityName, "Missing entityName argument");

        // Get entity class from entityName
        Class<? extends IReferentialEntity> entityClass = ReferentialEntities.getEntityClass(entityName);

        CriteriaBuilder builder = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<Long> criteriaQuery = builder.createQuery(Long.class);

        Root<? extends IReferentialEntity> entityRoot = criteriaQuery.from(entityClass);
        criteriaQuery.select(builder.count(entityRoot));

        // Level ids
        Predicate levelClause = null;
        ParameterExpression<Collection> levelIdsParam = builder.parameter(Collection.class);
        PropertyDescriptor pd = ReferentialEntities.getLevelProperty(entityClass.getSimpleName()).orElse(null);
        if (pd != null && ArrayUtils.isNotEmpty(levelIds)) {
            levelClause = builder.in(entityRoot.get(pd.getName()).get(IReferentialEntity.Fields.ID)).value(levelIdsParam);
            criteriaQuery.where(levelClause);
        }

        TypedQuery<Long> query = getEntityManager().createQuery(criteriaQuery);
        if (levelClause != null) {
            query.setParameter(levelIdsParam, ImmutableList.copyOf(levelIds));
        }

        return query.getSingleResult();
    }

    @Override
    @Caching(
        evict = {
            @CacheEvict(cacheNames = CacheConfiguration.Names.REFERENTIAL_MAX_UPDATE_DATE_BY_TYPE, key = "#source.entityName"),
            @CacheEvict(cacheNames = CacheConfiguration.Names.PERSON_BY_ID, key = "#source.id", condition = "#source.entityName == 'Person'"),
            @CacheEvict(cacheNames = CacheConfiguration.Names.DEPARTMENT_BY_ID, key = "#source.id", condition = "#source.entityName == 'Department'"),
            @CacheEvict(cacheNames = CacheConfiguration.Names.DEPARTMENT_BY_LABEL, key = "#source.label", condition = "#source.entityName == 'Department'"),
            @CacheEvict(cacheNames = CacheConfiguration.Names.PMFM_BY_ID, key = "#source.id", condition = "#source.entityName == 'Pmfm'"),
            @CacheEvict(cacheNames = CacheConfiguration.Names.PMFM_HAS_SUFFIX, allEntries = true, condition = "#source.entityName == 'Pmfm'"),
            @CacheEvict(cacheNames = CacheConfiguration.Names.PMFM_HAS_PREFIX, allEntries = true, condition = "#source.entityName == 'Pmfm'"),
            @CacheEvict(cacheNames = CacheConfiguration.Names.PMFM_HAS_MATRIX, allEntries = true, condition = "#source.entityName == 'Pmfm'"),
            @CacheEvict(cacheNames = CacheConfiguration.Names.PMFM_HAS_PARAMETER_GROUP, allEntries = true, condition = "#source.entityName == 'Pmfm'"),
            @CacheEvict(cacheNames = CacheConfiguration.Names.PROGRAM_BY_ID, key = "#source.id", condition = "#source.entityName == 'Program'"),
            @CacheEvict(cacheNames = CacheConfiguration.Names.PROGRAM_BY_LABEL, key = "#source.label", condition = "#source.entityName == 'Program'"),
            @CacheEvict(cacheNames = CacheConfiguration.Names.PROGRAM_PRIVILEGE_BY_ID,  key = "#source.id", condition = "#source.entityName == 'ProgramPrivilege'"),
            @CacheEvict(cacheNames = CacheConfiguration.Names.LOCATION_BY_ID, key = "#source.id", condition = "#source.entityName == 'Location'"),
            @CacheEvict(cacheNames = CacheConfiguration.Names.LOCATION_LEVEL_BY_LABEL, key = "#source.label", condition = "#source.entityName == 'LocationLevel'"),
            @CacheEvict(cacheNames = CacheConfiguration.Names.TAXON_NAME_BY_TAXON_REFERENCE_ID, allEntries = true, condition = "#source.entityName == 'TaxonName'"),
            @CacheEvict(cacheNames = CacheConfiguration.Names.REFERENCE_TAXON_ID_BY_TAXON_NAME_ID, allEntries = true, condition = "#source.entityName == 'TaxonName'"),
            @CacheEvict(cacheNames = CacheConfiguration.Names.TAXON_NAMES_BY_TAXON_GROUP_ID, allEntries = true, condition = "#source.entityName == 'TaxonName' || #source.entityName == 'TaxonGroup'"),
            @CacheEvict(cacheNames = CacheConfiguration.Names.TAXONONOMIC_LEVEL_BY_ID, key = "#source.id", condition = "#source.entityName == 'TaxonomicLevel'"),
            @CacheEvict(cacheNames = CacheConfiguration.Names.GEAR_BY_ID, key = "#source.id", condition = "#source.entityName == 'Gear'")
        }
    )
    public ReferentialVO save(final ReferentialVO source) {
        Preconditions.checkNotNull(source);

        // Get the entity class
        Class<? extends IReferentialEntity> entityClass = ReferentialEntities.getEntityClass(source.getEntityName());

        EntityManager entityManager = getEntityManager();

        IReferentialEntity entity = null;
        if (source.getId() != null) {
            entity = find(entityClass, source.getId());
        }
        boolean isNew = (entity == null);
        if (isNew) {
            try {
                entity = entityClass.newInstance();
            } catch (IllegalAccessException | InstantiationException e) {
                throw new IllegalArgumentException(String.format("Entity with name [%s] has no empty constructor", source.getEntityName()));
            }
        }

        if (!isNew) {
            // Check update date
            Daos.checkUpdateDateForUpdate(source, entity);

            // Lock entityName
            // TODO
        }

        // VO -> Entity
        toEntity(source, entity, true);

        // Update update_dt
        Timestamp newUpdateDate = getDatabaseCurrentTimestamp();
        entity.setUpdateDate(newUpdateDate);

        // (Save) entity
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

        entityManager.flush();
        entityManager.clear();

        return source;
    }

    @Cacheable(cacheNames = CacheConfiguration.Names.REFERENTIAL_MAX_UPDATE_DATE_BY_TYPE, key = "#entityName")
    public Date maxUpdateDate(String entityName) {
        Preconditions.checkNotNull(entityName, "Missing entityName argument");

        try {
            // Get entity class from entityName
            Class<? extends IUpdateDateEntity> entityClass = ReferentialEntities.getEntityClass(entityName);

            String hql = String.format("SELECT max(%s) FROM %s",
                IUpdateDateEntity.Fields.UPDATE_DATE,
                    entityClass.getSimpleName());

            return (Timestamp)getEntityManager().createQuery(hql).getSingleResult();
        }
        catch (Exception e) {
            log.error("Error while getting max(updateDate) from " + entityName, e);
            return null;
        }
    }

    /* -- protected methods -- */

    protected ReferentialTypeVO getTypeByEntityName(final String entityName) {

        final ReferentialTypeVO type = new ReferentialTypeVO();
        type.setId(entityName);

        ReferentialEntities.getLevelProperty(entityName).ifPresent(levelProperty ->
                type.setLevel(levelProperty.getPropertyType().getSimpleName()));

        return type;
    }

    protected <T extends IReferentialEntity> ReferentialVO toVO(final String entityName, T source) {
        Preconditions.checkNotNull(entityName);
        Preconditions.checkNotNull(source);

        ReferentialVO target = new ReferentialVO();

        Beans.copyProperties(source, target);

        // Status
        if (source instanceof IWithStatusEntity) {
            target.setStatusId(((IWithStatusEntity<?, ?>) source).getStatus().getId());
        } else {
            // No status in the entity = ENABLE
            target.setStatusId(StatusEnum.ENABLE.getId());
        }

        // Level
        ReferentialEntities.getLevelProperty(entityName).ifPresent(levelDescriptor -> {
            try {
                IReferentialEntity level = (IReferentialEntity) levelDescriptor.getReadMethod().invoke(source, new Object[0]);
                if (level != null) {
                    target.setLevelId(level.getId());
                }
            } catch (Exception e) {
                throw new SumarisTechnicalException(e);
            }
        });

        // Parent
        if (source instanceof ITreeNodeEntityBean) {
            IEntity<?> parent = ((ITreeNodeEntityBean<?, ?>) source).getParent();
            Object parentId = parent != null ? parent.getId() : null;
            if (parentId == null) {
                target.setParentId(null);
            }
            else {
                try {
                    target.setParentId(Integer.parseInt(parentId.toString()));
                } catch (Exception e) {
                    log.error("Cannot cast to integer the property '{}.parent.id'. Actual value is {}", entityName, parentId, e);
                    target.setParentId(null);
                }
            }
        } else {
            target.setParentId(null);
        }

        // EntityName (as metadata)
        target.setEntityName(entityName);

        return target;
    }

    protected List<ReferentialVO> toVOs(List<? extends IReferentialEntity> source) {
        return toVOs(source.stream());
    }

    protected List<ReferentialVO> toVOs(Stream<? extends IReferentialEntity> source) {
        return source
            .map(this::toVO)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    private Specification<IReferentialEntity> toSpecification(IReferentialFilter filter) {
        return null; // TODO
    }

    protected  <T> TypedQuery<T> createFindQuery(Class<T> entityClass,
                                             IReferentialFilter filter,
                                             String sortAttribute,
                                             SortDirection sortDirection) {

        CriteriaBuilder builder = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<T> query = builder.createQuery(entityClass);
        Root<T> entityRoot = query.from(entityClass);
        query.select(entityRoot).distinct(true);

        addSorting(query, builder, entityRoot, sortAttribute, sortDirection);

        return createFilteredQuery(builder, entityClass, query, entityRoot, filter );
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

    protected <T> TypedQuery<Long> createCountQuery(Class<T> entityClass,
                                                    IReferentialFilter filter) {

        CriteriaBuilder builder = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<Long> query = builder.createQuery(Long.class);
        Root<T> root = query.from(entityClass);
        query.select(builder.count(root));

        return createFilteredQuery(builder, entityClass, query, root, filter);
    }

    protected <R, T> TypedQuery<R> createFilteredQuery(CriteriaBuilder builder,
                                                       Class<T> entityClass,
                                                       CriteriaQuery<R> query,
                                                       Root<T> root,
                                                       IReferentialFilter filter
                                                       //QueryVisitor<R, T> queryVisitor
    ) {
        Integer[] levelIds = filter.getLevelIds();
        String[] levelLabels = filter.getLevelLabels();
        String searchText = StringUtils.trimToNull(filter.getSearchText());
        String searchAttribute = StringUtils.trimToNull(filter.getSearchAttribute());
        Integer[] statusIds = filter.getStatusIds();
        Integer[] includedIds = filter.getIncludedIds();
        Integer[] excludedIds = filter.getExcludedIds();

        // Level Ids
        Predicate levelIdClause = null;
        ParameterExpression<Collection> levelIdsParam = null;
        if (ArrayUtils.isNotEmpty(levelIds)) {
            levelIdsParam = builder.parameter(Collection.class);
            String levelPropertyName = ReferentialEntities.getLevelPropertyName(entityClass.getSimpleName()).orElse(null);
            if (levelPropertyName != null) {
                levelIdClause = builder.in(root.get(levelPropertyName).get(IReferentialEntity.Fields.ID)).value(levelIdsParam);
            } else {
                log.warn(String.format("Trying to request  on level, but no level found for entity {%s}", entityClass.getSimpleName()));
            }
        }

        // Level Labels
        Predicate levelLabelClause = null;
        ParameterExpression<Collection> levelLabelsParam = null;
        if (ArrayUtils.isNotEmpty(levelLabels)) {
            levelLabelsParam = builder.parameter(Collection.class);
            String levelPropertyName = ReferentialEntities.getLevelPropertyName(entityClass.getSimpleName()).orElse(null);
            if (levelPropertyName != null) {
                levelLabelClause = builder.in(root.get(levelPropertyName).get(IItemReferentialEntity.Fields.LABEL)).value(levelLabelsParam);
            } else {
                log.warn(String.format("Trying to request on level, but no level found for entity {%s}", entityClass.getSimpleName()));
            }
        }

        // Filter on id
        Predicate idClause = null;
        ParameterExpression<Integer> idParam = null;
        if (filter.getId() != null) {
            idParam = builder.parameter(Integer.class);
            idClause = builder.equal(root.get(IItemReferentialEntity.Fields.ID), idParam);
        }

        // Filter on label
        Predicate labelClause = null;
        ParameterExpression<String> labelParam = null;
        if (StringUtils.isNotBlank(filter.getLabel())) {
            labelParam = builder.parameter(String.class);
            labelClause = builder.equal(builder.upper(root.get(IItemReferentialEntity.Fields.LABEL)), builder.upper(labelParam));
        }

        // Filter on search text
        ParameterExpression<String> searchAsPrefixParam = builder.parameter(String.class);
        ParameterExpression<String> searchAnyMatchParam = builder.parameter(String.class);
        Predicate searchTextClause = null;
        if (labelClause == null && searchText != null) {
            // Search on the given search attribute, if exists
            if (StringUtils.isNotBlank(searchAttribute) && BeanUtils.getPropertyDescriptor(entityClass, searchAttribute) != null) {
                searchTextClause = builder.or(
                    builder.isNull(searchAnyMatchParam),
                    builder.like(builder.upper(Daos.composePath(root, searchAttribute)), builder.upper(searchAsPrefixParam), Daos.LIKE_ESCAPE_CHAR)
                );
            } else if (IItemReferentialEntity.class.isAssignableFrom(entityClass)) {
                // Search on label+name
                searchTextClause = builder.or(
                    builder.isNull(searchAnyMatchParam),
                    builder.like(builder.upper(root.get(IItemReferentialEntity.Fields.LABEL)), builder.upper(searchAsPrefixParam), Daos.LIKE_ESCAPE_CHAR),
                    builder.like(builder.upper(root.get(IItemReferentialEntity.Fields.NAME)), builder.upper(searchAnyMatchParam), Daos.LIKE_ESCAPE_CHAR)
                );
            } else if (BeanUtils.getPropertyDescriptor(entityClass, IItemReferentialEntity.Fields.LABEL) != null) {
                // Search on label
                searchTextClause = builder.or(
                    builder.isNull(searchAnyMatchParam),
                    builder.like(builder.upper(root.get(IItemReferentialEntity.Fields.LABEL)), builder.upper(searchAsPrefixParam), Daos.LIKE_ESCAPE_CHAR)
                );
            } else if (BeanUtils.getPropertyDescriptor(entityClass, IItemReferentialEntity.Fields.NAME) != null) {
                // Search on name
                searchTextClause = builder.or(
                    builder.isNull(searchAnyMatchParam),
                    builder.like(builder.upper(root.get(IItemReferentialEntity.Fields.NAME)), builder.upper(searchAnyMatchParam), Daos.LIKE_ESCAPE_CHAR)
                );
            }
        }

        // Filter on status
        ParameterExpression<Collection> statusIdsParam = builder.parameter(Collection.class);
        Predicate statusIdsClause = null;
        if (ArrayUtils.isNotEmpty(statusIds) && IWithStatusEntity.class.isAssignableFrom(entityClass)) {
            statusIdsClause = builder.in(root.get(IWithStatusEntity.Fields.STATUS).get(IEntity.Fields.ID)).value(statusIdsParam);
        }

        // Included Ids
        Predicate includedClause = null;
        ParameterExpression<Collection> includedIdsParam = null;
        if (ArrayUtils.isNotEmpty(includedIds)) {
            includedIdsParam = builder.parameter(Collection.class);
            includedClause = builder.in(root.get(IEntity.Fields.ID)).value(includedIdsParam);
        }

        // Excluded Ids
        Predicate excludedClause = null;
        ParameterExpression<Collection> excludedIdsParam = null;
        if (ArrayUtils.isNotEmpty(excludedIds)) {
            excludedIdsParam = builder.parameter(Collection.class);
            excludedClause = builder.not(
                builder.in(root.get(IEntity.Fields.ID)).value(excludedIdsParam)
            );
        }

        // Compute where clause
        Expression<Boolean> whereClause = null;
        if (levelIdClause != null) {
            whereClause = levelIdClause;
        }
        if (levelLabelClause != null) {
            whereClause = (whereClause == null) ? levelLabelClause : builder.and(whereClause, levelLabelClause);
        }
        if (idClause != null) {
            whereClause = (whereClause == null) ? idClause : builder.and(whereClause, idClause);
        }
        if (labelClause != null) {
            whereClause = (whereClause == null) ? labelClause : builder.and(whereClause, labelClause);
        } else if (searchTextClause != null) {
            whereClause = (whereClause == null) ? searchTextClause : builder.and(whereClause, searchTextClause);
        }

        if (statusIdsClause != null) {
            whereClause = (whereClause == null) ? statusIdsClause : builder.and(whereClause, statusIdsClause);
        }
        if (includedIdsParam != null) {
            whereClause = (whereClause == null) ? includedClause : builder.and(whereClause, includedClause);
        }
        if (excludedIdsParam != null) {
            whereClause = (whereClause == null) ? excludedClause : builder.and(whereClause, excludedClause);
        }

        // Delegate to visitor
        /*if (queryVisitor != null) {
            Expression<Boolean> additionalWhere = queryVisitor.apply(query, root);
            if (additionalWhere != null) {
                whereClause = (whereClause == null) ? additionalWhere : builder.and(whereClause, additionalWhere);
            }
        }*/

        if (whereClause != null) {
            query.where(whereClause);
        }

        TypedQuery<R> typedQuery = getEntityManager().createQuery(query);

        // Bind parameters
        if (idClause != null) {
            typedQuery.setParameter(idParam, filter.getId());
        }
        if (labelClause != null) {
            typedQuery.setParameter(labelParam, filter.getLabel());
        } else if (searchTextClause != null) {
            String searchTextAsPrefix = Daos.getEscapedSearchText(searchText);
            String searchTextAnyMatch = StringUtils.isNotBlank(searchTextAsPrefix) ? ("%" + searchTextAsPrefix) : null;
            typedQuery.setParameter(searchAsPrefixParam, searchTextAsPrefix);
            typedQuery.setParameter(searchAnyMatchParam, searchTextAnyMatch);
        }
        if (levelIdClause != null && levelIds != null) {
            typedQuery.setParameter(levelIdsParam, Arrays.asList(levelIds));
        }
        if (levelLabelClause != null && levelLabels != null) {
            typedQuery.setParameter(levelLabelsParam, Arrays.asList(levelLabels));
        }
        if (statusIdsClause != null) {
            typedQuery.setParameter(statusIdsParam, Arrays.asList(statusIds));
        }
        if (includedClause != null) {
            typedQuery.setParameter(includedIdsParam, Arrays.asList(includedIds));
        }
        if (excludedClause != null) {
            typedQuery.setParameter(excludedIdsParam, Arrays.asList(excludedIds));
        }

        return typedQuery;
    }

    private <T> TypedQuery<T> createFindByUniqueLabelQuery(Class<T> entityClass, String label) {
        CriteriaBuilder builder = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<T> query = builder.createQuery(entityClass);
        Root<T> tripRoot = query.from(entityClass);
        query.select(tripRoot).distinct(true);

        // Filter on text
        ParameterExpression<String> labelParam = builder.parameter(String.class);
        query.where(builder.equal(tripRoot.get(IItemReferentialEntity.Fields.LABEL), labelParam));

        return getEntityManager().createQuery(query)
            .setParameter(labelParam, label);
    }

    protected String getTableName(String entityName) {
        return I18n.t("sumaris.persistence.table." + entityName.substring(0, 1).toLowerCase() + entityName.substring(1));
    }

    protected <T extends IReferentialEntity> String getEntityName(T source) {
        String classname = source.getClass().getSimpleName();
        int index = classname.indexOf("$HibernateProxy");
        if (index > 0) {
            return classname.substring(0, index);
        }
        return classname;
    }

    protected void toEntity(final ReferentialVO source, IReferentialEntity target, boolean copyIfNull) {

        Beans.copyProperties(source, target);

        // Status
        if ((copyIfNull || source.getStatusId() != null) && target instanceof IWithStatusEntity) {
            IWithStatusEntity<Integer, Status> targetWithStatus = (IWithStatusEntity<Integer, Status>) target;
            if (source.getStatusId() == null) {
                targetWithStatus.setStatus(null);
            } else {
                targetWithStatus.setStatus(getReference(Status.class, source.getStatusId()));
            }
        }

        // Level
        Integer levelID = source.getLevelId();
        if (copyIfNull || levelID != null) {
            ReferentialEntities.getLevelProperty(source.getEntityName()).ifPresent(levelDescriptor -> {
                try {
                    if (levelID == null) {
                        levelDescriptor.getWriteMethod().invoke(target, new Object[]{null});
                    } else {
                        Object level = getReference(levelDescriptor.getPropertyType().asSubclass(Serializable.class), levelID);
                        levelDescriptor.getWriteMethod().invoke(target, level);
                    }
                } catch (Exception e) {
                    throw new SumarisTechnicalException(e);
                }
            });
        }

        // Validity status
        if (target instanceof IWithValidityStatusEntity) {
            if (source.getValidityStatusId() == null) {
                // Pending by default
                ((IWithValidityStatusEntity<?, ValidityStatus>) target).setValidityStatus(getReference(ValidityStatus.class, ValidityStatusEnum.PENDING.getId()));
            } else {
                ((IWithValidityStatusEntity<?, ValidityStatus>) target).setValidityStatus(getReference(ValidityStatus.class, source.getValidityStatusId()));
            }
        }
    }

}

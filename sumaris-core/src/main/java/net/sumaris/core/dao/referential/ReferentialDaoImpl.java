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
import com.google.common.collect.Maps;
import net.sumaris.core.dao.technical.Daos;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.dao.technical.hibernate.HibernateDaoSupport;
import net.sumaris.core.dao.technical.model.IEntity;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.model.administration.programStrategy.AcquisitionLevel;
import net.sumaris.core.model.administration.programStrategy.Program;
import net.sumaris.core.model.administration.programStrategy.ProgramPrivilege;
import net.sumaris.core.model.administration.programStrategy.Strategy;
import net.sumaris.core.model.administration.user.Department;
import net.sumaris.core.model.referential.*;
import net.sumaris.core.model.referential.gear.Gear;
import net.sumaris.core.model.referential.gear.GearClassification;
import net.sumaris.core.model.referential.grouping.Grouping;
import net.sumaris.core.model.referential.grouping.GroupingClassification;
import net.sumaris.core.model.referential.grouping.GroupingLevel;
import net.sumaris.core.model.referential.location.Location;
import net.sumaris.core.model.referential.location.LocationClassification;
import net.sumaris.core.model.referential.location.LocationLevel;
import net.sumaris.core.model.referential.metier.Metier;
import net.sumaris.core.model.referential.pmfm.*;
import net.sumaris.core.model.referential.taxon.TaxonGroup;
import net.sumaris.core.model.referential.taxon.TaxonGroupType;
import net.sumaris.core.model.referential.taxon.TaxonName;
import net.sumaris.core.model.referential.taxon.TaxonomicLevel;
import net.sumaris.core.model.referential.transcribing.TranscribingItem;
import net.sumaris.core.model.technical.configuration.Software;
import net.sumaris.core.model.technical.extraction.ExtractionProduct;
import net.sumaris.core.model.technical.extraction.ExtractionProductTable;
import net.sumaris.core.model.technical.versionning.SystemVersion;
import net.sumaris.core.util.Beans;
import net.sumaris.core.vo.filter.ReferentialFilterVO;
import net.sumaris.core.vo.referential.IReferentialVO;
import net.sumaris.core.vo.referential.ReferentialTypeVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.nuiton.i18n.I18n;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Repository("referentialDao")
public class ReferentialDaoImpl extends HibernateDaoSupport implements ReferentialDao {

    /** Logger. */
    private static final Logger log =
            LoggerFactory.getLogger(ReferentialDaoImpl.class);

    private static Map<String, Class<? extends IReferentialEntity>> entityClassMap = Maps.uniqueIndex(
            ImmutableList.of(
                    Status.class,
                    Department.class,
                    Location.class,
                    LocationLevel.class,
                    LocationClassification.class,
                    Gear.class,
                    GearClassification.class,
                    UserProfile.class,
                    SaleType.class,
                    VesselType.class,
                    // Taxon group
                    TaxonGroupType.class,
                    TaxonGroup.class,
                    // Taxon
                    TaxonomicLevel.class,
                    TaxonName.class,
                    // Métier
                    Metier.class,
                    // Pmfm
                    Parameter.class,
                    Pmfm.class,
                    Matrix.class,
                    Fraction.class,
                    Method.class,
                    QualitativeValue.class,
                    Unit.class,
                    // Quality
                    QualityFlag.class,
                    // Program/strategy
                    Program.class,
                    Strategy.class,
                    AcquisitionLevel.class,
                    // Transcribing
                    TranscribingItem.class,
                    // Grouping
                    GroupingClassification.class,
                    GroupingLevel.class,
                    Grouping.class,
                    // Product
                    ExtractionProduct.class,
                    ExtractionProductTable.class,
                    // Software
                    Software.class,
                    // Program
                    ProgramPrivilege.class,
                    // Technical
                    SystemVersion.class
            ), Class::getSimpleName);

    private Map<String, PropertyDescriptor> levelPropertyNameMap = initLevelPropertyNameMap();

    static {
        I18n.n("sumaris.persistence.table.location");
        I18n.n("sumaris.persistence.table.locationLevel");
        I18n.n("sumaris.persistence.table.gear");
        I18n.n("sumaris.persistence.table.gearLevel");
        I18n.n("sumaris.persistence.table.parameter");
        I18n.n("sumaris.persistence.table.userProfile");
        I18n.n("sumaris.persistence.table.saleType");
        I18n.n("sumaris.persistence.table.taxonGroup");
        I18n.n("sumaris.persistence.table.taxonGroupType");
        I18n.n("sumaris.persistence.table.taxonomicLevel");
        I18n.n("sumaris.persistence.table.referenceTaxon");
        I18n.n("sumaris.persistence.table.taxonName");
        I18n.n("sumaris.persistence.table.metier");
        I18n.n("sumaris.persistence.table.parameter");
        I18n.n("sumaris.persistence.table.pmfm");
        I18n.n("sumaris.persistence.table.matrix");
        I18n.n("sumaris.persistence.table.fraction");
        I18n.n("sumaris.persistence.table.method");
        I18n.n("sumaris.persistence.table.unit");
        I18n.n("sumaris.persistence.table.qualitativeValue");
        I18n.n("sumaris.persistence.table.program");
        I18n.n("sumaris.persistence.table.acquisitionLevel");
        I18n.n("sumaris.persistence.table.transcribingItem");
        I18n.n("sumaris.persistence.table.groupingClassification");
        I18n.n("sumaris.persistence.table.groupingLevel");
        I18n.n("sumaris.persistence.table.grouping");
        I18n.n("sumaris.persistence.table.extractionProduct");
        I18n.n("sumaris.persistence.table.extractionProductTable");
        I18n.n("sumaris.persistence.table.systemVersion");
    }

    protected static Map<String, PropertyDescriptor> initLevelPropertyNameMap() {
        Map<String, PropertyDescriptor> result = new HashMap<>();

        // Detect level properties, by name
        entityClassMap.values().stream().forEach((clazz) -> {
            PropertyDescriptor[] pds = BeanUtils.getPropertyDescriptors(clazz);
            for (PropertyDescriptor pd: pds) {
                if (pd.getName().matches("^.*[Ll]evel([A−Z].*)?$")) {
                    result.put(clazz.getSimpleName(), pd);
                    break;
                }
            }
        });

        // Other level (not having "level" in id)
        result.put(Pmfm.class.getSimpleName(), BeanUtils.getPropertyDescriptor(Pmfm.class, Pmfm.Fields.PARAMETER));
        result.put(Fraction.class.getSimpleName(), BeanUtils.getPropertyDescriptor(Fraction.class, Fraction.Fields.MATRIX));
        result.put(QualitativeValue.class.getSimpleName(), BeanUtils.getPropertyDescriptor(QualitativeValue.class, QualitativeValue.Fields.PARAMETER));
        result.put(TaxonGroup.class.getSimpleName(), BeanUtils.getPropertyDescriptor(TaxonGroup.class, TaxonGroup.Fields.TAXON_GROUP_TYPE));
        result.put(TaxonName.class.getSimpleName(), BeanUtils.getPropertyDescriptor(TaxonName.class, TaxonName.Fields.TAXONOMIC_LEVEL));
        result.put(Strategy.class.getSimpleName(), BeanUtils.getPropertyDescriptor(Strategy.class, Strategy.Fields.PROGRAM));
        result.put(Metier.class.getSimpleName(), BeanUtils.getPropertyDescriptor(Metier.class, Metier.Fields.GEAR));
        result.put(GroupingLevel.class.getSimpleName(), BeanUtils.getPropertyDescriptor(GroupingLevel.class, GroupingLevel.Fields.GROUPING_CLASSIFICATION));
        result.put(Grouping.class.getSimpleName(), BeanUtils.getPropertyDescriptor(Grouping.class, Grouping.Fields.GROUPING_LEVEL));
        result.put(ExtractionProductTable.class.getSimpleName(), BeanUtils.getPropertyDescriptor(ExtractionProductTable.class, ExtractionProductTable.Fields.PRODUCT));
        result.put(LocationLevel.class.getSimpleName(), BeanUtils.getPropertyDescriptor(LocationLevel.class, LocationLevel.Fields.LOCATION_CLASSIFICATION));
        result.put(Gear.class.getSimpleName(), BeanUtils.getPropertyDescriptor(Gear.class, Gear.Fields.GEAR_CLASSIFICATION));
        result.put(Program.class.getSimpleName(), BeanUtils.getPropertyDescriptor(Program.class, Program.Fields.GEAR_CLASSIFICATION));
        result.put(Program.class.getSimpleName(), BeanUtils.getPropertyDescriptor(Program.class, Program.Fields.TAXON_GROUP_TYPE));

        return result;
    }


    @Override
    public <T extends IReferentialEntity> Stream<T> streamByFilter(final Class<T> entityClass,
                                            ReferentialFilterVO filter,
                                            int offset,
                                            int size,
                                            String sortAttribute,
                                            SortDirection sortDirection) {
        Preconditions.checkNotNull(entityClass, "Missing 'entityClass' argument");
        Preconditions.checkNotNull(filter, "Missing 'filter' argument");

        return createFindQuery(entityClass,
                filter,
                sortAttribute,
                sortDirection,
                null
        )
                .setFirstResult(offset)
                .setMaxResults(size)
                .getResultStream();
    }

    @Override
    public List<ReferentialVO> findByFilter(final String entityName,
                                            ReferentialFilterVO filter,
                                            int offset,
                                            int size,
                                            String sortAttribute,
                                            SortDirection sortDirection) {
        Preconditions.checkNotNull(entityName, "Missing entityName argument");

        // Get entity class from entityName
        Class<? extends IReferentialEntity> entityClass = getEntityClass(entityName);

        return streamByFilter(entityClass,
                filter,
                offset,
                size,
                sortAttribute,
                sortDirection
        )
        .map(s -> toReferentialVO(entityName, s))
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
    }

    @Override
    public Long countByFilter(final String entityName, ReferentialFilterVO filter) {
        Preconditions.checkNotNull(entityName, "Missing entityName argument");
        Preconditions.checkNotNull(filter);

        // Get entity class from entityName
        Class<? extends IReferentialEntity> entityClass = getEntityClass(entityName);

        return createCountQuery(entityClass,filter)
                .getSingleResult();
    }

    @Override
    public ReferentialVO findByUniqueLabel(String entityName, String label) {
        Preconditions.checkNotNull(entityName, "Missing entityName argument");
        Preconditions.checkNotNull(label);

        // Get entity class from entityName
        Class<? extends IReferentialEntity> entityClass = getEntityClass(entityName);

        return toReferentialVO(entityName, createFindByUniqueLabelQuery(entityClass, label).getSingleResult());
    }

    @Override
    public List<ReferentialTypeVO> getAllTypes() {

        return entityClassMap.keySet().stream()
                .map(this::getTypeByEntityName)
                .collect(Collectors.toList());
    }

    @Override
    public ReferentialVO get(String entityName, int id) {
        Class<? extends IReferentialEntity> entityClass = getEntityClass(entityName);
        return get(entityClass, id);
    }

    @Override
    public ReferentialVO get(Class<? extends IReferentialEntity> entityClass, int id) {
        return toReferentialVO(super.get(entityClass, id));
    }

    @Override
    public List<ReferentialVO> getAllLevels(final String entityName) {
        Preconditions.checkNotNull(entityName, "Missing entityName argument");

        PropertyDescriptor levelDescriptor = levelPropertyNameMap.get(entityName);
        if (levelDescriptor == null) {
            return ImmutableList.of();
        }

        String levelEntityName = levelDescriptor.getPropertyType().getSimpleName();
        return findByFilter(levelEntityName, ReferentialFilterVO.builder().build(), 0, 100,
                IItemReferentialEntity.Fields.NAME, SortDirection.ASC);
    }

    @Override
    public ReferentialVO getLevelById(String entityName, int levelId) {
        Preconditions.checkNotNull(entityName, "Missing entityName argument");

        PropertyDescriptor levelDescriptor = levelPropertyNameMap.get(entityName);
        if (levelDescriptor == null) {
            throw new DataRetrievalFailureException("Unable to find level with id=" + levelId + " for entityName=" + entityName);
        }

        Class<?> levelClass = levelDescriptor.getPropertyType();
        if (!IReferentialEntity.class.isAssignableFrom(levelClass)){
            throw new DataRetrievalFailureException("Unable to convert class=" + levelClass.getName() + " to a referential bean");
        }
        return toReferentialVO(levelClass.getSimpleName(), (IReferentialEntity)entityManager.find(levelClass, levelId));
    }

    @Override
    public <T extends IReferentialEntity> ReferentialVO toReferentialVO(T source) {
        return toReferentialVO(getEntityName(source), source);
    }

    @Override
    public <T extends IReferentialVO, S extends IReferentialEntity> Optional<T> toTypedVO(S source, Class<T> targetClazz) {

        if (source == null)
            return Optional.empty();

        try {
            T target = targetClazz.newInstance();
            Beans.copyProperties(source, target);
            return Optional.of(target);
        }catch(IllegalAccessException | InstantiationException e) {
            throw new SumarisTechnicalException(e.getMessage(), e);
        }
    }

    @Override
    public void delete(final String entityName, int id) {

        // Get the entity class
        Class<? extends IReferentialEntity> entityClass = getEntityClass(entityName);

        log.debug(String.format("Deleting %s {id=%s}...", entityName, id));
        delete(entityClass, id);
    }

    @Override
    public Long count(String entityName) {
        Preconditions.checkNotNull(entityName, "Missing entityName argument");

        // Get entity class from entityName
        Class<? extends IReferentialEntity> entityClass = getEntityClass(entityName);

        CriteriaBuilder builder = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<Long> criteriaQuery = builder.createQuery(Long.class);
        criteriaQuery.select(builder.count(criteriaQuery.from(entityClass)));

        return getEntityManager().createQuery(criteriaQuery).getSingleResult();
    }

    @Override
    public Long countByLevelId(String entityName, Integer... levelIds) {
        Preconditions.checkNotNull(entityName, "Missing entityName argument");

        // Get entity class from entityName
        Class<? extends IReferentialEntity> entityClass = getEntityClass(entityName);

        CriteriaBuilder builder = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<Long> criteriaQuery = builder.createQuery(Long.class);

        Root<? extends IReferentialEntity> entityRoot = criteriaQuery.from(entityClass);
        criteriaQuery.select(builder.count(entityRoot));

        // Level ids
        Predicate levelClause = null;
        ParameterExpression<Collection> levelIdsParam = builder.parameter(Collection.class);
        PropertyDescriptor pd = levelPropertyNameMap.get(entityClass.getSimpleName());
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
    public ReferentialVO save(final ReferentialVO source) {
        Preconditions.checkNotNull(source);

        // Get the entity class
        Class<? extends IReferentialEntity> entityClass = getEntityClass(source.getEntityName());

        EntityManager entityManager = getEntityManager();

        IReferentialEntity entity = null;
        if (source.getId() != null) {
            entity = get(entityClass, source.getId());
        }
        boolean isNew = (entity == null);
        if (isNew) {
            try {
                entity = entityClass.newInstance();
            } catch (IllegalAccessException |InstantiationException e) {
                throw new IllegalArgumentException(String.format("Entity with name [%s] has no empty constructor", source.getEntityName()));
            }
        }

        if (!isNew) {
            // Check update date
            checkUpdateDateForUpdate(source, entity);

            // Lock entityName
            // TODO
        }

        // VO -> Entity
        referentialVOToEntity(source, entity, true);

        // Update update_dt
        Timestamp newUpdateDate = getDatabaseCurrentTimestamp();
        entity.setUpdateDate(newUpdateDate);

        // Save entity
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

    /* -- protected methods -- */

    protected ReferentialTypeVO getTypeByEntityName(final String entityName) {

        ReferentialTypeVO type = new ReferentialTypeVO();
        type.setId(entityName);

        PropertyDescriptor levelDescriptor = levelPropertyNameMap.get(entityName);
        if (levelDescriptor != null) {
            type.setLevel(levelDescriptor.getPropertyType().getSimpleName());
        }
        return type;
    }

    protected <T extends IReferentialEntity> ReferentialVO toReferentialVO(final String entityName, T source) {
        Preconditions.checkNotNull(entityName);
        Preconditions.checkNotNull(source);

        ReferentialVO target = new ReferentialVO();

        Beans.copyProperties(source, target);

        // Status
        if (source instanceof IWithStatusEntity) {
            target.setStatusId(((IWithStatusEntity<?, ?>)source).getStatus().getId());
        }
        else {
            // No status in the entity = ENABLE
            target.setStatusId(StatusEnum.ENABLE.getId());
        }

        // Level
        PropertyDescriptor levelDescriptor = levelPropertyNameMap.get(entityName);
        if (levelDescriptor != null) {
            try {
                IReferentialEntity level = (IReferentialEntity)levelDescriptor.getReadMethod().invoke(source, new Object[0]);
                if (level != null) {
                    target.setLevelId(level.getId());
                }
            } catch(Exception e) {
                throw new SumarisTechnicalException(e);
            }
        }

        // EntityName (as metadata)
        target.setEntityName(entityName);

        return target;
    }

    protected List<ReferentialVO> toReferentialVOs(List<? extends IReferentialEntity> source) {
        return toReferentialVOs(source.stream());
    }

    protected List<ReferentialVO> toReferentialVOs(Stream<? extends IReferentialEntity> source) {
        return source
                .map(this::toReferentialVO)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public <T> TypedQuery<T> createFindQuery(Class<T> entityClass,
                                      ReferentialFilterVO filter,
                                      String sortAttribute,
                                      SortDirection sortDirection,
                                      QueryVisitor<T, T> queryVisitor) {

        CriteriaBuilder builder = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<T> query = builder.createQuery(entityClass);
        Root<T> entityRoot = query.from(entityClass);
        query.select(entityRoot).distinct(true);

        addSorting(query, builder, entityRoot, sortAttribute, sortDirection);

        return createFilteredQuery(builder, entityClass,
                query, entityRoot,
                filter,
                queryVisitor);
    }

    protected <T> TypedQuery<Long> createCountQuery(Class<T> entityClass,
                                             ReferentialFilterVO filter) {

        CriteriaBuilder builder = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<Long> query = builder.createQuery(Long.class);
        Root<T> root = query.from(entityClass);
        query.select(builder.count(root));

        return createFilteredQuery(builder, entityClass,
                query, root,
                filter,
                null);
    }

    protected <R, T> TypedQuery<R> createFilteredQuery(CriteriaBuilder builder,
                                                    Class<T> entityClass,
                                                    CriteriaQuery<R> query,
                                                    Root<T> entityRoot,
                                                    ReferentialFilterVO filter,
                                                    QueryVisitor<R, T> queryVisitor) {

        Integer levelId = filter.getLevelId();
        Integer[] levelIds = filter.getLevelIds();
        String searchText = StringUtils.trimToNull(filter.getSearchText());
        String searchAttribute = StringUtils.trimToNull(filter.getSearchAttribute());
        Integer[] statusIds = filter.getStatusIds();

        // Level Ids
        Predicate levelClause = null;
        ParameterExpression<Collection> levelIdsParam = null;
        if (ArrayUtils.isNotEmpty(levelIds)) {
            if (levelIds.length == 1) {
                levelId = levelIds[0];
                levelIds = null;
            } else {
                levelId = null;
                levelIdsParam = builder.parameter(Collection.class);
                PropertyDescriptor pd = levelPropertyNameMap.get(entityClass.getSimpleName());
                if (pd != null) {
                    levelClause = builder.in(entityRoot.get(pd.getName()).get(IReferentialEntity.Fields.ID)).value(levelIdsParam);
                }
                else {
                    log.warn(String.format("Trying to request  on level, but no level found for entity {%s}", entityClass.getSimpleName()));
                }
            }
        }
        // Level Id
        ParameterExpression<Integer> levelIdParam = null;
        if (levelId != null) {
            levelIdParam = builder.parameter(Integer.class);
            PropertyDescriptor pd = levelPropertyNameMap.get(entityClass.getSimpleName());
            if (pd != null) {
                levelClause = builder.equal(entityRoot.get(pd.getName()).get(IReferentialEntity.Fields.ID), levelIdParam);
            }
            else {
                log.warn(String.format("Trying to request  on level, but no level found for entity {%s}", entityClass.getSimpleName()));
            }
        }

        // Filter on label
        Predicate labelClause = null;
        ParameterExpression<String> labelParam = null;
        if (StringUtils.isNotBlank(filter.getLabel())) {
            labelParam = builder.parameter(String.class);
            labelClause = builder.equal(builder.upper(entityRoot.get(IItemReferentialEntity.Fields.LABEL)), builder.upper(labelParam));
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
                        builder.like(builder.upper(Daos.composePath(entityRoot, searchAttribute)), builder.upper(searchAsPrefixParam))
                );
            }
            else if (IItemReferentialEntity.class.isAssignableFrom(entityClass)) {
                // Search on label+name
                searchTextClause = builder.or(
                        builder.isNull(searchAnyMatchParam),
                        builder.like(builder.upper(entityRoot.get(IItemReferentialEntity.Fields.LABEL)), builder.upper(searchAsPrefixParam)),
                        builder.like(builder.upper(entityRoot.get(IItemReferentialEntity.Fields.NAME)), builder.upper(searchAnyMatchParam))
                );
            } else if (BeanUtils.getPropertyDescriptor(entityClass, IItemReferentialEntity.Fields.LABEL) != null) {
                // Search on label
                searchTextClause = builder.or(
                        builder.isNull(searchAnyMatchParam),
                        builder.like(builder.upper(entityRoot.get(IItemReferentialEntity.Fields.LABEL)), builder.upper(searchAsPrefixParam))
                );
            } else if (BeanUtils.getPropertyDescriptor(entityClass, IItemReferentialEntity.Fields.NAME) != null) {
                // Search on name
                searchTextClause = builder.or(
                        builder.isNull(searchAnyMatchParam),
                        builder.like(builder.upper(entityRoot.get(IItemReferentialEntity.Fields.NAME)), builder.upper(searchAnyMatchParam))
                );
            }
        }

        // Filter on status
        ParameterExpression<Collection> statusIdsParam = builder.parameter(Collection.class);
        Predicate statusIdsClause = null;
        if (ArrayUtils.isNotEmpty(statusIds) && IWithStatusEntity.class.isAssignableFrom(entityClass)) {
            statusIdsClause = builder.in(entityRoot.get(IWithStatusEntity.Fields.STATUS).get(IEntity.Fields.ID)).value(statusIdsParam);
        }

        // Compute where clause
        Expression<Boolean> whereClause = null;
        if (levelClause != null) {
            whereClause = levelClause;
        }
        if (labelClause != null) {
            whereClause = (whereClause == null) ? labelClause : builder.and(whereClause, labelClause);
        }
        else if (searchTextClause != null) {
            whereClause = (whereClause == null) ? searchTextClause : builder.and(whereClause, searchTextClause);
        }

        if (statusIdsClause != null) {
            whereClause = (whereClause == null) ? statusIdsClause : builder.and(whereClause, statusIdsClause);
        }

        // Delegate to visitor
        if (queryVisitor != null) {
            Expression<Boolean> additionalWhere = queryVisitor.apply(query, entityRoot);
            if (additionalWhere != null) {
                whereClause = (whereClause == null) ? additionalWhere : builder.and(whereClause, additionalWhere);
            }
        }

        if (whereClause != null) {
            query.where(whereClause);
        }

        TypedQuery<R> typedQuery = getEntityManager().createQuery(query);

        // Bind parameters
        if (labelClause != null) {
            typedQuery.setParameter(labelParam, filter.getLabel());
        }
        else if (searchTextClause != null) {
            String searchTextAsPrefix = Daos.getEscapedSearchText(searchText);
            String searchTextAnyMatch = StringUtils.isNotBlank(searchTextAsPrefix) ? ("%"+searchTextAsPrefix) : null;
            typedQuery.setParameter(searchAsPrefixParam, searchTextAsPrefix);
            typedQuery.setParameter(searchAnyMatchParam, searchTextAnyMatch);
        }
        if (levelClause != null) {
            if (levelIds != null) {
                typedQuery.setParameter(levelIdsParam, ImmutableList.copyOf(levelIds));
            }
            else {
                typedQuery.setParameter(levelIdParam, levelId);
            }
        }
        if (statusIdsClause != null) {
            typedQuery.setParameter(statusIdsParam, ImmutableList.copyOf(statusIds));
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

    protected Class<? extends IReferentialEntity> getEntityClass(String entityName) {
        Preconditions.checkNotNull(entityName);

        // Get entity class from entityName
        Class<? extends IReferentialEntity> entityClass = entityClassMap.get(entityName);
        if (entityClass == null)
            throw new IllegalArgumentException(String.format("Referential entity [%s] not exists", entityName));
        return entityClass;
    }

    protected String getTableName(String entityName) {

        return I18n.t("sumaris.persistence.table."+ entityName.substring(0,1).toLowerCase() + entityName.substring(1));
    }

    protected <T extends IReferentialEntity> String getEntityName(T source) {
        String classname = source.getClass().getSimpleName();
        int index = classname.indexOf("$HibernateProxy");
        if (index > 0) {
            return classname.substring(0, index);
        }
        return classname;
    }

    protected void referentialVOToEntity(final ReferentialVO source, IReferentialEntity target, boolean copyIfNull) {

        Beans.copyProperties(source, target);

        // Status
        if ((copyIfNull || source.getStatusId() != null) && target instanceof IWithStatusEntity) {
            IWithStatusEntity<Integer, Status> targetWithStatus = (IWithStatusEntity)target;
            if (source.getStatusId() == null) {
                targetWithStatus.setStatus(null);
            }
            else {
                targetWithStatus.setStatus(load(Status.class, source.getStatusId()));
            }
        }

        // Level
        Integer levelID = source.getLevelId();
        if (copyIfNull || levelID != null) {
            PropertyDescriptor levelDescriptor = levelPropertyNameMap.get(source.getEntityName());
            if (levelDescriptor != null) {
                try {
                    if (levelID == null) {
                        levelDescriptor.getWriteMethod().invoke(target, new Object[]{null});
                    }
                    else {
                        Object level = load(levelDescriptor.getPropertyType().asSubclass(Serializable.class), levelID);
                        levelDescriptor.getWriteMethod().invoke(target, level);
                    }
                } catch(Exception e) {
                    throw new SumarisTechnicalException(e);
                }
            }
        }

        // Validity status
        if (target instanceof IWithValidityStatusEntity) {
            if (source.getValidityStatusId() == null) {
                // Pending by default
                ((IWithValidityStatusEntity<?, ValidityStatus>) target).setValidityStatus(load(ValidityStatus.class, ValidityStatusEnum.PENDING.getId()));
            }
            else {
                ((IWithValidityStatusEntity<?, ValidityStatus>) target).setValidityStatus(load(ValidityStatus.class, source.getValidityStatusId()));
            }
        }
    }

}

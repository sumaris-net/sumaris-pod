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
import net.sumaris.core.dao.technical.Beans;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.dao.technical.hibernate.HibernateDaoSupport;
import net.sumaris.core.dao.technical.model.IEntityBean;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.model.administration.programStrategy.AcquisitionLevel;
import net.sumaris.core.model.administration.programStrategy.Program;
import net.sumaris.core.model.administration.programStrategy.Strategy;
import net.sumaris.core.model.referential.*;
import net.sumaris.core.model.administration.user.Department;
import net.sumaris.core.model.referential.gear.Gear;
import net.sumaris.core.model.referential.gear.GearLevel;
import net.sumaris.core.model.referential.metier.Metier;
import net.sumaris.core.model.referential.taxon.TaxonGroup;
import net.sumaris.core.model.referential.taxon.TaxonGroupType;
import net.sumaris.core.vo.filter.ReferentialFilterVO;
import net.sumaris.core.vo.referential.IReferentialVO;
import net.sumaris.core.vo.referential.ReferentialTypeVO;
import net.sumaris.core.vo.referential.ReferentialVO;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Repository("referentialDao")
public class ReferentialDaoImpl extends HibernateDaoSupport implements ReferentialDao {

    /** Logger. */
    private static final Logger log =
            LoggerFactory.getLogger(ReferentialDaoImpl.class);

    private static Map<String, Class<? extends IReferentialEntity>> entityClassMap = Maps.uniqueIndex(
            ImmutableList.of(
                    Department.class,
                    Location.class,
                    LocationLevel.class,
                    Gear.class,
                    GearLevel.class,
                    UserProfile.class,
                    SaleType.class,
                    VesselType.class,
                    // Taxon
                    TaxonGroupType.class,
                    TaxonGroup.class,
                    // Métier
                    Metier.class,
                    // Pmfm
                    Parameter.class,
                    Pmfm.class,
                    Matrix.class,
                    Fraction.class,
                    QualitativeValue.class,
                    // Program/strategy
                    Program.class,
                    Strategy.class,
                    AcquisitionLevel.class
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
        I18n.n("sumaris.persistence.table.metier");
        I18n.n("sumaris.persistence.table.parameter");
        I18n.n("sumaris.persistence.table.pmfm");
        I18n.n("sumaris.persistence.table.matrix");
        I18n.n("sumaris.persistence.table.fraction");
        I18n.n("sumaris.persistence.table.qualitativeValue");
        I18n.n("sumaris.persistence.table.program");
        I18n.n("sumaris.persistence.table.acquisitionLevel");
    }

    protected static Map<String, PropertyDescriptor> initLevelPropertyNameMap() {
        Map<String, PropertyDescriptor> result = new HashMap<>();
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
        result.put(Fraction.class.getSimpleName(), BeanUtils.getPropertyDescriptor(Fraction.class, Fraction.PROPERTY_MATRIX));
        result.put(QualitativeValue.class.getSimpleName(), BeanUtils.getPropertyDescriptor(QualitativeValue.class, QualitativeValue.PROPERTY_PARAMETER));
        result.put(TaxonGroup.class.getSimpleName(), BeanUtils.getPropertyDescriptor(TaxonGroup.class, TaxonGroup.PROPERTY_TAXON_GROUP_TYPE));
        result.put(Strategy.class.getSimpleName(), BeanUtils.getPropertyDescriptor(Strategy.class, Strategy.PROPERTY_PROGRAM));
        result.put(Metier.class.getSimpleName(), BeanUtils.getPropertyDescriptor(Metier.class, Metier.PROPERTY_GEAR));

        return result;
    }


    @Override
    public List<ReferentialVO> findByFilter(final String entityName, ReferentialFilterVO filter, int offset, int size, String sortAttribute, SortDirection sortDirection) {
        Preconditions.checkNotNull(entityName, "Missing entityName argument");
        Preconditions.checkNotNull(filter);

        // Get entity class from entityName
        Class<? extends IReferentialEntity> entityClass = getEntityClass(entityName);

        return createFindQuery(entityClass, filter.getLevelId(), StringUtils.trimToNull(filter.getSearchText()), StringUtils.trimToNull(filter.getSearchAttribute()), sortAttribute, sortDirection)
                .setFirstResult(offset)
                .setMaxResults(size)
                .getResultList()
                .stream()
                .map(s -> toReferentialVO(entityName, s))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
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
    public List<ReferentialVO> getAllLevels(final String entityName) {
        Preconditions.checkNotNull(entityName, "Missing entityName argument");

        PropertyDescriptor levelDescriptor = levelPropertyNameMap.get(entityName);
        if (levelDescriptor == null) {
            return ImmutableList.of();
        }

        String levelEntityName = levelDescriptor.getPropertyType().getSimpleName();
        return findByFilter(levelEntityName, new ReferentialFilterVO(), 0, 100,
                IItemReferentialEntity.PROPERTY_NAME, SortDirection.ASC);
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
    public <T extends IReferentialVO, S extends IReferentialEntity> T toTypedVO(S source, Class<T> targetClazz) {
        Preconditions.checkNotNull(source);
        try {
            T target = targetClazz.newInstance();
            Beans.copyProperties(source, target);
            return target;
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
        target.setStatusId(source.getStatus().getId());

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

    private <T> TypedQuery<T> createFindQuery(Class<T> entityClass,
                                         Integer levelId,
                                         String searchText,
                                         String searchAttribute,
                                         String sortAttribute,
                                         SortDirection sortDirection) {
        CriteriaBuilder builder = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<T> query = builder.createQuery(entityClass);
        Root<T> tripRoot = query.from(entityClass);
        query.select(tripRoot).distinct(true);

        // Special case with level
        Predicate levelClause = null;
        ParameterExpression<Integer> levelParam = builder.parameter(Integer.class);
        if (levelId != null) {
            // Location
            if (Location.class.isAssignableFrom(entityClass)) {
                levelClause = builder.equal(tripRoot.get(Location.PROPERTY_LOCATION_LEVEL).get(IReferentialEntity.PROPERTY_ID), levelParam);
            }

            // Gear
            else if (Gear.class.isAssignableFrom(entityClass)) {
                levelClause = builder.equal(tripRoot.get(Gear.PROPERTY_GEAR_LEVEL).get(IReferentialEntity.PROPERTY_ID), levelParam);
            }

            else {
                PropertyDescriptor pd = levelPropertyNameMap.get(entityClass.getSimpleName());
                if (pd != null) {
                    levelClause = builder.equal(tripRoot.get(pd.getName()).get(IReferentialEntity.PROPERTY_ID), levelParam);
                }
            }
        }

        // Filter on text
        ParameterExpression<String> searchAsPrefixParam = builder.parameter(String.class);
        ParameterExpression<String> searchAnyMatchParam = builder.parameter(String.class);
        Predicate searchTextClause = null;
        if (searchText != null) {
            // Search on the given search attribute, if exists
            if (StringUtils.isNotBlank(searchAttribute) && BeanUtils.getPropertyDescriptor(entityClass, searchAttribute) != null) {
                searchTextClause = builder.or(
                        builder.isNull(searchAnyMatchParam),
                        builder.like(builder.upper(tripRoot.get(searchAttribute)), builder.upper(searchAsPrefixParam))
                );
            }
            else if (IItemReferentialEntity.class.isAssignableFrom(entityClass)) {
                // Search on label+name
                searchTextClause = builder.or(
                        builder.isNull(searchAnyMatchParam),
                        builder.like(builder.upper(tripRoot.get(IItemReferentialEntity.PROPERTY_LABEL)), builder.upper(searchAsPrefixParam)),
                        builder.like(builder.upper(tripRoot.get(IItemReferentialEntity.PROPERTY_NAME)), builder.upper(searchAnyMatchParam))
                );
            } else if (BeanUtils.getPropertyDescriptor(entityClass, IItemReferentialEntity.PROPERTY_LABEL) != null) {
                // Search on label
                searchTextClause = builder.or(
                        builder.isNull(searchAnyMatchParam),
                        builder.like(builder.upper(tripRoot.get(IItemReferentialEntity.PROPERTY_LABEL)), builder.upper(searchAsPrefixParam))
                );
            } else if (BeanUtils.getPropertyDescriptor(entityClass, IItemReferentialEntity.PROPERTY_NAME) != null) {
                // Search on name
                searchTextClause = builder.or(
                        builder.isNull(searchAnyMatchParam),
                        builder.like(builder.upper(tripRoot.get(IItemReferentialEntity.PROPERTY_NAME)), builder.upper(searchAnyMatchParam))
                );
            }
        }


        // Compute where clause
        if (levelClause != null) {
            if (searchTextClause != null) {
                query.where(builder.and(levelClause, searchTextClause));
            }
            else {
                query.where(levelClause);
            }
        }
        else if (searchTextClause != null) {
            query.where(searchTextClause);
        }

        // Add sorting
        if (StringUtils.isNotBlank(sortAttribute)) {

            // Convert level into the correct property name
            if (ReferentialVO.PROPERTY_LEVEL.equals(sortAttribute)) {
                PropertyDescriptor levelDescriptor = levelPropertyNameMap.get(entityClass.getSimpleName());
                if (levelDescriptor != null) {
                    sortAttribute = levelDescriptor.getName();
                }
                else {
                    sortAttribute = null;
                }
            }

            if (StringUtils.isNotBlank(sortAttribute)) {
                Expression<?> sortExpression = tripRoot.get(sortAttribute);
                query.orderBy(SortDirection.DESC.equals(sortDirection) ?
                        builder.desc(sortExpression) :
                        builder.asc(sortExpression)
                );
            }
        }

        TypedQuery<T> typedQuery = getEntityManager().createQuery(query);

        // Bind parameters
        if (searchTextClause != null) {
            String searchTextAsPrefix = null;
            if (StringUtils.isNotBlank(searchText)) {
                searchTextAsPrefix = (searchText + "*"); // add trailing escape char
                searchTextAsPrefix = searchTextAsPrefix.replaceAll("[*]+", "*"); // group escape chars
                searchTextAsPrefix = searchTextAsPrefix.replaceAll("[%]", "\\%"); // protected '%' chars
                searchTextAsPrefix = searchTextAsPrefix.replaceAll("[*]", "%"); // replace asterix
            }
            String searchTextAnyMatch = StringUtils.isNotBlank(searchTextAsPrefix) ? ("%"+searchTextAsPrefix) : null;
            typedQuery.setParameter(searchAsPrefixParam, searchTextAsPrefix);
            typedQuery.setParameter(searchAnyMatchParam, searchTextAnyMatch);
        }
        if (levelClause != null) {
            typedQuery.setParameter(levelParam, levelId);
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
        query.where(builder.equal(tripRoot.get(IItemReferentialEntity.PROPERTY_LABEL), labelParam));

        return getEntityManager().createQuery(query)
                .setParameter(labelParam, label);
    }

    protected Class<? extends IReferentialEntity> getEntityClass(String entityName) {
        Preconditions.checkNotNull(entityName);

        // Get entity class from entityName
        Class<? extends IReferentialEntity> entityClass = entityClassMap.get(entityName);
        if (entityClass == null) {
            throw new IllegalArgumentException("No entity with name [" + entityName + "]");
        }
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
        if (copyIfNull || source.getStatusId() != null) {
            if (source.getStatusId() == null) {
                target.setStatus(null);
            }
            else {
                target.setStatus(load(Status.class, source.getStatusId()));
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

    }

}

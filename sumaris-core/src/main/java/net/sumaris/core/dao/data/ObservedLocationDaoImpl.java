package net.sumaris.core.dao.data;

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
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.dao.administration.programStrategy.ProgramDao;
import net.sumaris.core.dao.administration.user.DepartmentDao;
import net.sumaris.core.dao.administration.user.PersonDao;
import net.sumaris.core.dao.referential.location.LocationDao;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.dao.technical.hibernate.HibernateDaoSupport;
import net.sumaris.core.dao.technical.model.IDataEntity;
import net.sumaris.core.model.administration.programStrategy.Program;
import net.sumaris.core.model.administration.user.Person;
import net.sumaris.core.model.data.ObservedLocation;
import net.sumaris.core.model.referential.location.Location;
import net.sumaris.core.util.Beans;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.data.ObservedLocationVO;
import net.sumaris.core.vo.filter.ObservedLocationFilterVO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

@Repository("observedLocationDao")
public class ObservedLocationDaoImpl extends HibernateDaoSupport implements ObservedLocationDao {

    /** Logger. */
    private static final Logger log =
            LoggerFactory.getLogger(ObservedLocationDaoImpl.class);

    @Autowired
    private SumarisConfiguration config;

    @Autowired
    private LocationDao locationDao;

    @Autowired
    private PersonDao personDao;

    @Autowired
    private DepartmentDao departmentDao;

    @Autowired
    private ProgramDao programDao;

    public ObservedLocationDaoImpl() {
        super();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<ObservedLocationVO> getAll(int offset, int size, String sortAttribute, SortDirection sortDirection) {

        CriteriaBuilder builder = entityManager.getCriteriaBuilder(); //getEntityManager().getCriteriaBuilder();
        CriteriaQuery<ObservedLocation> query = builder.createQuery(ObservedLocation.class);
        Root<ObservedLocation> observedLocationRoot = query.from(ObservedLocation.class);
        query.select(observedLocationRoot)
                .distinct(true);

        // Add sorting
        if (StringUtils.isNotBlank(sortAttribute)) {
            Expression<?> sortExpression = observedLocationRoot.get(sortAttribute);
            query.orderBy(SortDirection.DESC.equals(sortDirection) ?
                    builder.desc(sortExpression) :
                    builder.asc(sortExpression)
            );
        }

        return toObservedLocationVOs(entityManager.createQuery(query).
                setFirstResult(offset)
                .setMaxResults(size)
                .getResultList());
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<ObservedLocationVO> findByFilter(ObservedLocationFilterVO filter, int offset, int size, String sortAttribute, SortDirection sortDirection) {
        Preconditions.checkNotNull(filter);
        Preconditions.checkArgument(offset >= 0);
        Preconditions.checkArgument(size > 0);

        Integer programId = null;
        if (StringUtils.isNotBlank(filter.getProgramLabel())) {
            programId = programDao.getByLabel(filter.getProgramLabel()).getId();
        }

        // Fetch locations
        //getEntityManager().enableFetchProfile("with-location");

        CriteriaBuilder builder = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<ObservedLocation> query = builder.createQuery(ObservedLocation.class);
        Root<ObservedLocation> root = query.from(ObservedLocation.class);

        ParameterExpression<Date> startDateParam = builder.parameter(Date.class);
        ParameterExpression<Date> endDateParam = builder.parameter(Date.class);
        ParameterExpression<Integer> locationIdParam = builder.parameter(Integer.class);
        ParameterExpression<Integer> programIdParam = builder.parameter(Integer.class);

        query.select(root)
            .where(builder.and(
                // Filter: program
                builder.or(
                        builder.isNull(programIdParam),
                        builder.equal(root.get(ObservedLocation.PROPERTY_PROGRAM).get(Program.PROPERTY_ID), programIdParam)
                ),
                // Filter: startDate
                builder.or(
                        builder.isNull(startDateParam),
                        builder.not(builder.lessThan(root.get(ObservedLocation.PROPERTY_END_DATE_TIME), startDateParam))
                ),
                // Filter: endDate
                builder.or(
                    builder.isNull(endDateParam),
                    builder.not(builder.greaterThan(root.get(ObservedLocation.PROPERTY_START_DATE_TIME), endDateParam))
                ),
                // Filter: location
                builder.or(
                        builder.isNull(locationIdParam),
                        builder.equal(root.get(ObservedLocation.PROPERTY_LOCATION).get(Location.PROPERTY_ID), locationIdParam)
                )
            ));

        // Add sorting
        if (StringUtils.isNotBlank(sortAttribute)) {
            Expression<?> sortExpression = root.get(sortAttribute);
            query.orderBy(SortDirection.DESC.equals(sortDirection) ?
                    builder.desc(sortExpression) :
                    builder.asc(sortExpression)
            );
        }

        TypedQuery<ObservedLocation> q = getEntityManager().createQuery(query)
                .setParameter(programIdParam, programId)
                .setParameter(startDateParam, filter.getStartDate())
                .setParameter(endDateParam, filter.getEndDate())
                .setParameter(locationIdParam, filter.getLocationId())
                .setFirstResult(offset)
                .setMaxResults(size);
        return toObservedLocationVOs(q.getResultList());
    }

    @Override
    public Long countByFilter(ObservedLocationFilterVO filter) {

        CriteriaBuilder builder = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<Long> criteriaQuery = builder.createQuery(Long.class);

        Integer programId = null;
        if (filter != null && StringUtils.isNotBlank(filter.getProgramLabel())) {
            programId = programDao.getByLabel(filter.getProgramLabel()).getId();
        }

        ParameterExpression<Date> startDateParam = builder.parameter(Date.class);
        ParameterExpression<Date> endDateParam = builder.parameter(Date.class);
        ParameterExpression<Integer> locationIdParam = builder.parameter(Integer.class);
        ParameterExpression<Integer> programIdParam = builder.parameter(Integer.class);

        Root<ObservedLocation> root = criteriaQuery.from(ObservedLocation.class);
        criteriaQuery.select(builder.count(root));
        if (filter != null) {
            criteriaQuery.where(builder.and(
                    // Filter: program
                    builder.or(
                            builder.isNull(programIdParam),
                            builder.equal(root.get(ObservedLocation.PROPERTY_PROGRAM).get(Program.PROPERTY_ID), programIdParam)
                    ),
                    // Filter: startDate
                    builder.or(
                            builder.isNull(startDateParam),
                            builder.not(builder.lessThan(root.get(ObservedLocation.PROPERTY_END_DATE_TIME), startDateParam))
                    ),
                    // Filter: endDate
                    builder.or(
                            builder.isNull(endDateParam),
                            builder.not(builder.greaterThan(root.get(ObservedLocation.PROPERTY_START_DATE_TIME), endDateParam))
                    ),
                    // Filter: location
                    builder.or(
                            builder.isNull(locationIdParam),
                            builder.equal(root.get(ObservedLocation.PROPERTY_LOCATION).get(Location.PROPERTY_ID), locationIdParam)
                    )
            ));
        }

        if (filter != null) {
            return getEntityManager()
                    .createQuery(criteriaQuery)
                    .setParameter(programIdParam, programId)
                    .setParameter(startDateParam, filter.getStartDate())
                    .setParameter(endDateParam, filter.getEndDate())
                    .setParameter(locationIdParam, filter.getLocationId())
                    .getSingleResult();
        } else {
            return getEntityManager()
                    .createQuery(criteriaQuery)
                    .getSingleResult();
        }
    }

    @Override
    public ObservedLocationVO get(int id) {
        ObservedLocation entity = get(ObservedLocation.class, id);
        return toObservedLocationVO(entity);
    }

    @Override
    public <T> T get(int id, Class<T> targetClass) {
        if (targetClass.isAssignableFrom(ObservedLocation.class)) return (T)get(ObservedLocation.class, id);
        if (targetClass.isAssignableFrom(ObservedLocationVO.class)) return (T)get(id);
        throw new IllegalArgumentException("Unable to convert into " + targetClass.getName());
    }

    @Override
    public ObservedLocationVO save(ObservedLocationVO source) {
        Preconditions.checkNotNull(source);

        EntityManager entityManager = getEntityManager();
        ObservedLocation entity = null;
        if (source.getId() != null) {
            entity = get(ObservedLocation.class, source.getId());
        }
        boolean isNew = (entity == null);
        if (isNew) {
            entity = new ObservedLocation();
        }

        else {
            // Check update date
            checkUpdateDateForUpdate(source, entity);

            // Lock entityName
            lockForUpdate(entity);
        }

        // VO -> Entity
        observedLocationVOToEntity(source, entity, true);

        // Update update_dt
        Timestamp newUpdateDate = getDatabaseCurrentTimestamp();
        entity.setUpdateDate(newUpdateDate);

        // Save entityName
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


        //session.flush();
        //session.clear();

        return source;
    }

    @Override
    public void delete(int id) {

        log.debug(String.format("Deleting observedLocation {id=%s}...", id));
        delete(ObservedLocation.class, id);
    }

    @Override
    public ObservedLocationVO toObservedLocationVO(ObservedLocation source) {
        if (source == null) return null;

        ObservedLocationVO target = new ObservedLocationVO();

        Beans.copyProperties(source, target);

        // Program
        target.setProgram(programDao.toProgramVO(source.getProgram()));


        target.setQualityFlagId(source.getQualityFlag().getId());

        // Location
        target.setLocation(locationDao.toLocationVO(source.getLocation()));

        // Recorder department
        DepartmentVO recorderDepartment = departmentDao.toDepartmentVO(source.getRecorderDepartment());
        target.setRecorderDepartment(recorderDepartment);

        // Recorder person
        if (source.getRecorderPerson() != null) {
            PersonVO recorderPerson = personDao.toPersonVO(source.getRecorderPerson());
            target.setRecorderPerson(recorderPerson);
        }

        // Observers
        if (CollectionUtils.isNotEmpty(source.getObservers())) {
            Set<PersonVO> observers = source.getObservers().stream().map(personDao::toPersonVO).collect(Collectors.toSet());
            target.setObservers(observers);
        }

        return target;
    }

    @Override
    public ObservedLocationVO control(ObservedLocationVO source) {
        Preconditions.checkNotNull(source);

        ObservedLocation entity = get(ObservedLocation.class, source.getId());

        // Check update date
        checkUpdateDateForUpdate(source, entity);

        // Lock entityName
        lockForUpdate(entity);

        // TODO CONTROL PROCESS HERE
        Date controlDate = getDatabaseCurrentTimestamp();
        entity.setControlDate(controlDate);

        // Update update_dt
        Timestamp newUpdateDate = getDatabaseCurrentTimestamp();
        entity.setUpdateDate(newUpdateDate);

        // Save entityName
        getEntityManager().merge(entity);

        // Update source
        source.setControlDate(controlDate);
        source.setUpdateDate(newUpdateDate);

        return source;
    }

    @Override
    public ObservedLocationVO validate(ObservedLocationVO source) {
        Preconditions.checkNotNull(source);

        ObservedLocation entity = get(ObservedLocation.class, source.getId());

        // Check update date
        checkUpdateDateForUpdate(source, entity);

        // Lock entityName
//        lockForUpdate(entity);

        // TODO VALIDATION PROCESS HERE
        Date validationDate = getDatabaseCurrentTimestamp();
        entity.setValidationDate(validationDate);

        // Update update_dt
        Timestamp newUpdateDate = getDatabaseCurrentTimestamp();
        entity.setUpdateDate(newUpdateDate);

        // Save entityName
//        getEntityManager().merge(entity);

        // Update source
        source.setValidationDate(validationDate);
        source.setUpdateDate(newUpdateDate);

        return source;
    }

    @Override
    public ObservedLocationVO unvalidate(ObservedLocationVO source) {
        Preconditions.checkNotNull(source);

        ObservedLocation entity = get(ObservedLocation.class, source.getId());

        // Check update date
        checkUpdateDateForUpdate(source, entity);

        // Lock entityName
//        lockForUpdate(entity);

        // TODO UNVALIDATION PROCESS HERE
        entity.setValidationDate(null);

        // Update update_dt
        Timestamp newUpdateDate = getDatabaseCurrentTimestamp();
        entity.setUpdateDate(newUpdateDate);

        // Save entityName
//        getEntityManager().merge(entity);

        // Update source
        source.setValidationDate(null);
        source.setUpdateDate(newUpdateDate);

        return source;
    }

    /* -- protected methods -- */

    protected List<ObservedLocationVO> toObservedLocationVOs(List<ObservedLocation> source) {
        return source.stream()
                .map(this::toObservedLocationVO)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    protected void observedLocationVOToEntity(ObservedLocationVO source, ObservedLocation target, boolean copyIfNull) {

        // Copy properties
        DataDaos.copyDataRootProperties(getEntityManager(), source, target, copyIfNull);

        // Departure location
        if (copyIfNull || source.getLocation() != null) {
            if (source.getLocation() == null || source.getLocation().getId() == null) {
                target.setLocation(null);
            }
            else {
                target.setLocation(load(Location.class, source.getLocation().getId()));
            }
        }

        // Observers
        if (copyIfNull || source.getObservers() != null) {
            if (CollectionUtils.isEmpty(source.getObservers())) {
                if (CollectionUtils.isNotEmpty(target.getObservers())) {
                    target.getObservers().clear();
                }
            }
            else {
                Map<Integer, Person> observersToRemove = Beans.splitById(target.getObservers());
                source.getObservers().stream()
                        .map(IDataEntity::getId)
                        .forEach(personId -> {
                    if (observersToRemove.remove(personId) == null) {
                        // Add new item
                        target.getObservers().add(load(Person.class, personId));
                    }
                });

                // Remove deleted items
                target.getObservers().removeAll(observersToRemove.values());
            }
        }
    }
}

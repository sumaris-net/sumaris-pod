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
import net.sumaris.core.dao.administration.programStrategy.ProgramRepository;
import net.sumaris.core.dao.administration.user.DepartmentRepository;
import net.sumaris.core.dao.administration.user.PersonRepository;
import net.sumaris.core.dao.referential.location.LocationRepository;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.dao.technical.model.IEntity;
import net.sumaris.core.model.administration.programStrategy.Program;
import net.sumaris.core.model.administration.user.Person;
import net.sumaris.core.model.data.ObservedLocation;
import net.sumaris.core.model.referential.location.Location;
import net.sumaris.core.util.Beans;
import net.sumaris.core.vo.administration.programStrategy.ProgramFetchOptions;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.data.DataFetchOptions;
import net.sumaris.core.vo.data.ObservedLocationVO;
import net.sumaris.core.vo.filter.ObservedLocationFilterVO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

@Repository("observedLocationDao")
public class ObservedLocationDaoImpl extends BaseDataDaoImpl implements ObservedLocationDao {

    /**
     * Logger.
     */
    private static final Logger log =
            LoggerFactory.getLogger(ObservedLocationDaoImpl.class);

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private ProgramRepository programRepository;

    public ObservedLocationDaoImpl() {
        super();
    }

    @Override
    public List<ObservedLocationVO> getAll(int offset, int size, String sortAttribute, SortDirection sortDirection, DataFetchOptions fetchOptions) {

        CriteriaBuilder builder = getEntityManager().getCriteriaBuilder(); //getEntityManager().getCriteriaBuilder();
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

        return toVOs(getEntityManager().createQuery(query).
                setFirstResult(offset)
                .setMaxResults(size)
                .getResultList(), fetchOptions);
    }

    @Override
    public List<ObservedLocationVO> findByFilter(ObservedLocationFilterVO filter, int offset, int size, String sortAttribute,
                                                 SortDirection sortDirection, DataFetchOptions fetchOptions) {
        Preconditions.checkNotNull(filter);
        Preconditions.checkArgument(offset >= 0);
        Preconditions.checkArgument(size > 0);

        Integer programId = null;
        if (StringUtils.isNotBlank(filter.getProgramLabel())) {
            programId = programRepository.getByLabel(filter.getProgramLabel()).getId();
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
                                builder.equal(root.get(ObservedLocation.Fields.PROGRAM).get(Program.Fields.ID), programIdParam)
                        ),
                        // Filter: startDate
                        builder.or(
                                builder.isNull(startDateParam),
                                builder.not(builder.lessThan(root.get(ObservedLocation.Fields.END_DATE_TIME), startDateParam))
                        ),
                        // Filter: endDate
                        builder.or(
                                builder.isNull(endDateParam),
                                builder.not(builder.greaterThan(root.get(ObservedLocation.Fields.START_DATE_TIME), endDateParam))
                        ),
                        // Filter: location
                        builder.or(
                                builder.isNull(locationIdParam),
                                builder.equal(root.get(ObservedLocation.Fields.LOCATION).get(Location.Fields.ID), locationIdParam)
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
        return toVOs(q.getResultList(), fetchOptions);
    }

    @Override
    public Long countByFilter(ObservedLocationFilterVO filter) {

        CriteriaBuilder builder = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<Long> criteriaQuery = builder.createQuery(Long.class);

        Integer programId = null;
        if (filter != null && StringUtils.isNotBlank(filter.getProgramLabel())) {
            programId = programRepository.getByLabel(filter.getProgramLabel()).getId();
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
                            builder.equal(root.get(ObservedLocation.Fields.PROGRAM).get(Program.Fields.ID), programIdParam)
                    ),
                    // Filter: startDate
                    builder.or(
                            builder.isNull(startDateParam),
                            builder.not(builder.lessThan(root.get(ObservedLocation.Fields.END_DATE_TIME), startDateParam))
                    ),
                    // Filter: endDate
                    builder.or(
                            builder.isNull(endDateParam),
                            builder.not(builder.greaterThan(root.get(ObservedLocation.Fields.START_DATE_TIME), endDateParam))
                    ),
                    // Filter: location
                    builder.or(
                            builder.isNull(locationIdParam),
                            builder.equal(root.get(ObservedLocation.Fields.LOCATION).get(Location.Fields.ID), locationIdParam)
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
        ObservedLocation entity = find(ObservedLocation.class, id);
        return toVO(entity, null);
    }

    @Override
    public <T> T get(int id, Class<T> targetClass) {
        if (targetClass.isAssignableFrom(ObservedLocation.class)) return (T) find(ObservedLocation.class, id);
        if (targetClass.isAssignableFrom(ObservedLocationVO.class)) return (T) get(id);
        throw new IllegalArgumentException("Unable to convert into " + targetClass.getName());
    }

    @Override
    public ObservedLocationVO save(ObservedLocationVO source) {
        Preconditions.checkNotNull(source);

        ObservedLocation entity = null;
        if (source.getId() != null) {
            entity = find(ObservedLocation.class, source.getId());
        }
        boolean isNew = (entity == null);
        if (isNew) {
            entity = new ObservedLocation();
        } else {
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

            getEntityManager().persist(entity);
            source.setId(entity.getId());
        } else {
            getEntityManager().merge(entity);
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
    public ObservedLocationVO toVO(ObservedLocation source) {
        return toVO(source, null);
    }

    @Override
    public ObservedLocationVO control(ObservedLocationVO source) {
        Preconditions.checkNotNull(source);

        ObservedLocation entity = find(ObservedLocation.class, source.getId());

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

        ObservedLocation entity = find(ObservedLocation.class, source.getId());

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

        ObservedLocation entity = find(ObservedLocation.class, source.getId());

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

    protected List<ObservedLocationVO> toVOs(List<ObservedLocation> source, DataFetchOptions fetchOptions) {
        return source.stream()
                .map(item -> toVO(item, fetchOptions))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }


    protected ObservedLocationVO toVO(ObservedLocation source, DataFetchOptions fetchOptions) {
        if (source == null) return null;

        ObservedLocationVO target = new ObservedLocationVO();

        Beans.copyProperties(source, target);

        // Remove endDateTime if same as startDateTime
        if (target.getEndDateTime() != null && target.getEndDateTime().equals(target.getStartDateTime())) {
            target.setEndDateTime(null);
        }

        // Program
        target.setProgram(programRepository.toVO(source.getProgram(),
                ProgramFetchOptions.builder().withProperties(false)
                        .build()));


        target.setQualityFlagId(source.getQualityFlag().getId());

        // Location
        target.setLocation(locationRepository.toVO(source.getLocation()));

        // Recorder department
        if (fetchOptions == null || fetchOptions.isWithRecorderDepartment()) {
            DepartmentVO recorderDepartment = departmentRepository.toVO(source.getRecorderDepartment());
            target.setRecorderDepartment(recorderDepartment);
        }

        // Recorder person
        if ((fetchOptions == null || fetchOptions.isWithRecorderPerson()) && source.getRecorderPerson() != null) {
            PersonVO recorderPerson = personRepository.toVO(source.getRecorderPerson());
            target.setRecorderPerson(recorderPerson);
        }

        // Observers
        if ((fetchOptions == null || fetchOptions.isWithObservers()) && CollectionUtils.isNotEmpty(source.getObservers())) {
            Set<PersonVO> observers = source.getObservers().stream().map(personRepository::toVO).collect(Collectors.toSet());
            target.setObservers(observers);
        }

        return target;
    }

    protected void observedLocationVOToEntity(ObservedLocationVO source, ObservedLocation target, boolean copyIfNull) {

        // Copy properties
        copyRootDataProperties(source, target, copyIfNull);

        // If endDateTime is empty, fill using startDateTime
        if (target.getEndDateTime() == null) {
            target.setEndDateTime(target.getStartDateTime());
        }

        // Departure location
        if (copyIfNull || source.getLocation() != null) {
            if (source.getLocation() == null || source.getLocation().getId() == null) {
                target.setLocation(null);
            } else {
                target.setLocation(load(Location.class, source.getLocation().getId()));
            }
        }

        // Observers
        if (copyIfNull || source.getObservers() != null) {
            if (CollectionUtils.isEmpty(source.getObservers())) {
                if (CollectionUtils.isNotEmpty(target.getObservers())) {
                    target.getObservers().clear();
                }
            } else {
                Map<Integer, Person> observersToRemove = Beans.splitById(target.getObservers());
                source.getObservers().stream()
                        .filter(Objects::nonNull)
                        .map(IEntity::getId)
                        .filter(Objects::nonNull)
                        .forEach(personId -> {
                            if (observersToRemove.remove(personId) == null) {
                                // Add new item
                                target.getObservers().add(load(Person.class, personId));
                            }
                        });

                // Remove deleted tableNames
                target.getObservers().removeAll(observersToRemove.values());
            }
        }
    }
}

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
import net.sumaris.core.model.QualityFlagEnum;
import net.sumaris.core.model.administration.programStrategy.Program;
import net.sumaris.core.model.data.Trip;
import net.sumaris.core.model.referential.QualityFlag;
import net.sumaris.core.model.referential.location.Location;
import net.sumaris.core.util.Beans;
import net.sumaris.core.vo.administration.programStrategy.ProgramFetchOptions;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.data.DataFetchOptions;
import net.sumaris.core.vo.data.TripVO;
import net.sumaris.core.vo.data.VesselSnapshotVO;
import net.sumaris.core.vo.filter.TripFilterVO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Repository("tripDao")
public class TripDaoImpl extends BaseDataDaoImpl implements TripDao {

    /**
     * Logger.
     */
    private static final Logger log =
            LoggerFactory.getLogger(TripDaoImpl.class);

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

    public TripDaoImpl() {
        super();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<TripVO> findAll(int offset, int size, String sortAttribute,
                                SortDirection sortDirection,
                                DataFetchOptions fieldOptions) {

        CriteriaBuilder builder = entityManager.getCriteriaBuilder(); //getEntityManager().getCriteriaBuilder();
        CriteriaQuery<Trip> query = builder.createQuery(Trip.class);
        Root<Trip> tripRoot = query.from(Trip.class);
        query.select(tripRoot)
                .distinct(true);

        // Add sorting
        if (StringUtils.isNotBlank(sortAttribute)) {
            Expression<?> sortExpression = tripRoot.get(sortAttribute);
            query.orderBy(SortDirection.DESC.equals(sortDirection) ?
                    builder.desc(sortExpression) :
                    builder.asc(sortExpression)
            );
        }

        // Enable fetch profiles
        Session session = getSession();
        if (fieldOptions.isWithRecorderDepartment() || fieldOptions.isWithRecorderPerson())
            session.enableFetchProfile(Trip.FETCH_PROFILE_RECORDER);
        if (fieldOptions.isWithObservers())
            session.enableFetchProfile(Trip.FETCH_PROFILE_OBSERVERS);
        session.enableFetchProfile(Trip.FETCH_PROFILE_LOCATION);

        return toTripVOs(entityManager.createQuery(query)
                .setFirstResult(offset)
                .setMaxResults(size)
                .getResultList(), fieldOptions);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<TripVO> findAll(TripFilterVO filter, int offset, int size, String sortAttribute,
                                SortDirection sortDirection,
                                DataFetchOptions fieldOptions) {
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
        CriteriaQuery<Trip> query = builder.createQuery(Trip.class);
        Root<Trip> root = query.from(Trip.class);

        ParameterExpression<Date> startDateParam = builder.parameter(Date.class);
        ParameterExpression<Date> endDateParam = builder.parameter(Date.class);
        ParameterExpression<Integer> locationIdParam = builder.parameter(Integer.class);
        ParameterExpression<Integer> programIdParam = builder.parameter(Integer.class);
        ParameterExpression<Integer> vesselIdParam = builder.parameter(Integer.class);

        query.select(root)
                .where(builder.and(
                        // Filter: program
                        builder.or(
                                builder.isNull(programIdParam),
                                builder.equal(root.get(Trip.Fields.PROGRAM).get(Program.Fields.ID), programIdParam)
                        ),
                        // Filter: startDate
                        builder.or(
                                builder.isNull(startDateParam),
                                builder.not(builder.lessThan(root.get(Trip.Fields.RETURN_DATE_TIME), startDateParam))
                        ),
                        // Filter: endDate
                        builder.or(
                                builder.isNull(endDateParam),
                                builder.not(builder.greaterThan(root.get(Trip.Fields.DEPARTURE_DATE_TIME), endDateParam))
                        ),
                        // Filter: location
                        builder.or(
                                builder.isNull(locationIdParam),
                                builder.equal(root.get(Trip.Fields.DEPARTURE_LOCATION).get(Location.Fields.ID), locationIdParam),
                                builder.equal(root.get(Trip.Fields.RETURN_LOCATION).get(Location.Fields.ID), locationIdParam)
                        ),
                        // Filter: vessel
                        builder.or(
                                builder.isNull(vesselIdParam),
                                builder.equal(root.get(Trip.Fields.VESSEL).get(Location.Fields.ID), vesselIdParam)
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

        TypedQuery<Trip> q = getEntityManager().createQuery(query)
                .setParameter(programIdParam, programId)
                .setParameter(startDateParam, filter.getStartDate())
                .setParameter(endDateParam, filter.getEndDate())
                .setParameter(locationIdParam, filter.getLocationId())
                .setParameter(vesselIdParam, filter.getVesselId())
                .setFirstResult(offset)
                .setMaxResults(size);
        return toTripVOs(q.getResultList(), fieldOptions);
    }

    @Override
    public Long countByFilter(TripFilterVO filter) {

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

        Root<Trip> root = criteriaQuery.from(Trip.class);
        criteriaQuery.select(builder.count(root));
        if (filter != null) {
            criteriaQuery.where(builder.and(
                    // Filter: program
                    builder.or(
                            builder.isNull(programIdParam),
                            builder.equal(root.get(Trip.Fields.PROGRAM).get(Program.Fields.ID), programIdParam)
                    ),
                    // Filter: startDate
                    builder.or(
                            builder.isNull(startDateParam),
                            builder.not(builder.lessThan(root.get(Trip.Fields.RETURN_DATE_TIME), startDateParam))
                    ),
                    // Filter: endDate
                    builder.or(
                            builder.isNull(endDateParam),
                            builder.not(builder.greaterThan(root.get(Trip.Fields.DEPARTURE_DATE_TIME), endDateParam))
                    ),
                    // Filter: location
                    builder.or(
                            builder.isNull(locationIdParam),
                            builder.equal(root.get(Trip.Fields.DEPARTURE_LOCATION).get(Location.Fields.ID), locationIdParam),
                            builder.equal(root.get(Trip.Fields.RETURN_LOCATION).get(Location.Fields.ID), locationIdParam)
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
    public TripVO get(int id) {
        Trip entity = get(Trip.class, id);
        return toVO(entity);
    }

    @Override
    public TripVO save(TripVO source) {
        Preconditions.checkNotNull(source);

        EntityManager entityManager = getEntityManager();
        Trip entity = null;
        if (source.getId() != null && source.getId().intValue() >= 0) {
            entity = get(Trip.class, source.getId());
        }
        boolean isNew = (entity == null);
        if (isNew) {
            entity = new Trip();
        } else {
            // Check update date
            checkUpdateDateForUpdate(source, entity);

            // Lock entityName
            lockForUpdate(entity);
        }

        // VO -> Entity
        tripVOToEntity(source, entity, true);

        // Update update_dt
        Timestamp newUpdateDate = getDatabaseCurrentTimestamp();
        entity.setUpdateDate(newUpdateDate);

        // Save entityName
        if (isNew) {
            // Force id to null, to use the generator
            entity.setId(null);

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

        log.debug(String.format("Deleting trip {id=%s}...", id));
        delete(Trip.class, id);
    }

    @Override
    public TripVO toVO(Trip source) {
        return toTripVO(source, null);
    }

    @Override
    public TripVO control(TripVO source) {
        Preconditions.checkNotNull(source);

        Trip entity = get(Trip.class, source.getId());
        if (entity == null) {
            throw new DataRetrievalFailureException(String.format("Trip {%s} not found", source.getId()));
        }

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
    public TripVO validate(TripVO source) {
        Preconditions.checkNotNull(source);

        Trip entity = get(Trip.class, source.getId());
        if (entity == null) {
            throw new DataRetrievalFailureException(String.format("Trip {%s} not found", source.getId()));
        }

        // Check update date
        checkUpdateDateForUpdate(source, entity);

        // Lock entityName
        // lockForUpdate(entity);

        // Update update_dt
        Timestamp newUpdateDate = getDatabaseCurrentTimestamp();
        entity.setUpdateDate(newUpdateDate);

        // TODO VALIDATION PROCESS HERE
        entity.setValidationDate(newUpdateDate);

        // Save entityName
        getEntityManager().merge(entity);

        // Update source
        source.setValidationDate(newUpdateDate);
        source.setUpdateDate(newUpdateDate);

        return source;
    }

    @Override
    public TripVO unvalidate(TripVO source) {
        Preconditions.checkNotNull(source);

        Trip entity = get(Trip.class, source.getId());
        if (entity == null) {
            throw new DataRetrievalFailureException(String.format("Trip {%s} not found", source.getId()));
        }

        // Check update date
        checkUpdateDateForUpdate(source, entity);

        // Lock entityName
//        lockForUpdate(entity);

        // TODO UNVALIDATION PROCESS HERE
        entity.setValidationDate(null);
        entity.setQualificationDate(null);
        entity.setQualityFlag(load(QualityFlag.class, QualityFlagEnum.NOT_QUALIFED.getId()));

        // Update update_dt
        Timestamp newUpdateDate = getDatabaseCurrentTimestamp();
        entity.setUpdateDate(newUpdateDate);

        // Save entityName
        getEntityManager().merge(entity);

        // Update source
        source.setValidationDate(null);
        source.setQualificationDate(null);
        source.setQualityFlagId(QualityFlagEnum.NOT_QUALIFED.getId());
        source.setUpdateDate(newUpdateDate);

        return source;
    }

    @Override
    public TripVO qualify(TripVO source) {
        Preconditions.checkNotNull(source);

        Trip entity = get(Trip.class, source.getId());
        if (entity == null) {
            throw new DataRetrievalFailureException(String.format("Trip {%s} not found", source.getId()));
        }

        // Check update date
        checkUpdateDateForUpdate(source, entity);

        // Lock entityName
//        lockForUpdate(entity);


        // Update update_dt
        Timestamp newUpdateDate = getDatabaseCurrentTimestamp();
        entity.setUpdateDate(newUpdateDate);

        int qualityFlagId = source.getQualityFlagId() != null ? source.getQualityFlagId().intValue() : 0;

        // If not qualify, then remove the qualification date
        if (qualityFlagId == QualityFlagEnum.NOT_QUALIFED.getId()) {
            entity.setQualificationDate(null);
        }
        else {
            entity.setQualificationDate(newUpdateDate);
        }
        // Apply a get, because can return a null value (e.g. if id is not in the DB instance)
        entity.setQualityFlag(get(QualityFlag.class, Integer.valueOf(qualityFlagId)));

        // TODO UNVALIDATION PROCESS HERE
        // - insert into qualification history

        // Save entityName
        getEntityManager().merge(entity);

        // Update source
        source.setQualificationDate(entity.getQualificationDate());
        source.setQualityFlagId(entity.getQualityFlag() != null ? entity.getQualityFlag().getId() : 0);
        source.setUpdateDate(newUpdateDate);

        return source;
    }

    /* -- protected methods -- */

    protected List<TripVO> toTripVOs(List<Trip> source, DataFetchOptions fieldOptions) {
        return source.stream()
                .map(item -> this.toTripVO(item, fieldOptions))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    protected TripVO toTripVO(Trip source, DataFetchOptions fieldOptions) {
        if (source == null) return null;

        TripVO target = new TripVO();

        Beans.copyProperties(source, target);

        // Program
        target.setProgram(programDao.toProgramVO(source.getProgram(),
                ProgramFetchOptions.builder().withProperties(false).build()));

        // Vessel
        VesselSnapshotVO vesselSnapshot = new VesselSnapshotVO();
        vesselSnapshot.setId(source.getVessel().getId());
        target.setVesselSnapshot(vesselSnapshot);
        target.setQualityFlagId(source.getQualityFlag().getId());

        // Departure & return locations
        target.setDepartureLocation(locationDao.toLocationVO(source.getDepartureLocation()));
        target.setReturnLocation(locationDao.toLocationVO(source.getReturnLocation()));

        // Recorder department
        if ((fieldOptions == null || fieldOptions.isWithRecorderDepartment()) && source.getRecorderDepartment() != null) {
            DepartmentVO recorderDepartment = departmentDao.toDepartmentVO(source.getRecorderDepartment());
            target.setRecorderDepartment(recorderDepartment);
        }

        // Recorder person
        if ((fieldOptions == null || fieldOptions.isWithRecorderPerson()) && source.getRecorderPerson() != null) {
            PersonVO recorderPerson = personDao.toPersonVO(source.getRecorderPerson());
            target.setRecorderPerson(recorderPerson);
        }

        // Observers
        if ((fieldOptions == null || fieldOptions.isWithObservers()) && CollectionUtils.isNotEmpty(source.getObservers())) {
            Set<PersonVO> observers = source.getObservers().stream().map(personDao::toPersonVO).collect(Collectors.toSet());
            target.setObservers(observers);
        }

        return target;
    }

    protected void tripVOToEntity(TripVO source, Trip target, boolean copyIfNull) {
        // Copy properties
        copyRootDataProperties(source, target, copyIfNull);

        // Observers
        copyObservers(source, target, copyIfNull);

        // Vessel
        copyVessel(source, target, copyIfNull);

        // Departure location
        if (copyIfNull || source.getDepartureLocation() != null) {
            if (source.getDepartureLocation() == null || source.getDepartureLocation().getId() == null) {
                target.setDepartureLocation(null);
            } else {
                target.setDepartureLocation(load(Location.class, source.getDepartureLocation().getId()));
            }
        }

        // Return location
        if (copyIfNull || source.getReturnLocation() != null) {
            if (source.getReturnLocation() == null || source.getReturnLocation().getId() == null) {
                target.setReturnLocation(null);
            } else {
                target.setReturnLocation(load(Location.class, source.getReturnLocation().getId()));
            }
        }

    }
}

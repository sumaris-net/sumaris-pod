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
import net.sumaris.core.dao.referential.LocationDao;
import net.sumaris.core.dao.technical.Beans;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.dao.technical.hibernate.HibernateDaoSupport;
import net.sumaris.core.model.administration.programStrategy.Program;
import net.sumaris.core.model.administration.user.Department;
import net.sumaris.core.model.administration.user.Person;
import net.sumaris.core.model.data.Trip;
import net.sumaris.core.model.data.Vessel;
import net.sumaris.core.model.referential.IReferentialEntity;
import net.sumaris.core.model.referential.Location;
import net.sumaris.core.model.referential.QualityFlag;
import net.sumaris.core.vo.administration.programStrategy.ProgramVO;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.data.TripVO;
import net.sumaris.core.vo.data.VesselFeaturesVO;
import net.sumaris.core.vo.filter.TripFilterVO;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Repository("tripDao")
public class TripDaoImpl extends HibernateDaoSupport implements TripDao {

    /** Logger. */
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
    public List<TripVO> getAllTrips(int offset, int size, String sortAttribute, SortDirection sortDirection) {

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

        return toTripVOs(entityManager.createQuery(query).
                setFirstResult(offset)
                .setMaxResults(size)
                .getResultList());
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<TripVO> findByFilter(TripFilterVO filter, int offset, int size, String sortAttribute, SortDirection sortDirection) {
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

        query.select(root)
            .where(builder.and(
                // Filter: program
                builder.or(
                        builder.isNull(programIdParam),
                        builder.equal(root.get(Trip.PROPERTY_PROGRAM).get(Program.PROPERTY_ID), programIdParam)
                ),
                // Filter: startDate
                builder.or(
                        builder.isNull(startDateParam),
                        builder.not(builder.lessThan(root.get(Trip.PROPERTY_RETURN_DATE_TIME), startDateParam))
                ),
                // Filter: endDate
                builder.or(
                    builder.isNull(endDateParam),
                    builder.not(builder.greaterThan(root.get(Trip.PROPERTY_DEPARTURE_DATE_TIME), endDateParam))
                ),
                // Filter: location
                builder.or(
                        builder.isNull(locationIdParam),
                        builder.equal(root.get(Trip.PROPERTY_DEPARTURE_LOCATION).get(Location.PROPERTY_ID), locationIdParam),
                        builder.equal(root.get(Trip.PROPERTY_RETURN_LOCATION).get(Location.PROPERTY_ID), locationIdParam)
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
                .setFirstResult(offset)
                .setMaxResults(size);
        return toTripVOs(q.getResultList());
    }

    @Override
    public TripVO get(int id) {
        Trip entity = get(Trip.class, id);
        return toTripVO(entity);
    }

    @Override
    public <T> T get(int id, Class<T> targetClass) {
        if (targetClass.isAssignableFrom(Trip.class)) return (T)get(Trip.class, id);
        if (targetClass.isAssignableFrom(TripVO.class)) return (T)get(id);
        throw new IllegalArgumentException("Unable to convert into " + targetClass.getName());
    }

    @Override
    public TripVO save(TripVO source) {
        Preconditions.checkNotNull(source);

        EntityManager entityManager = getEntityManager();
        Trip entity = null;
        if (source.getId() != null) {
            entity = get(Trip.class, source.getId());
        }
        boolean isNew = (entity == null);
        if (isNew) {
            entity = new Trip();
        }

        else {
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
    public TripVO toTripVO(Trip source) {
        if (source == null) return null;

        TripVO target = new TripVO();

        Beans.copyProperties(source, target);

        // Program
        target.setProgram(programDao.toProgramVO(source.getProgram()));

        // Vessel
        VesselFeaturesVO vesselFeatures = new VesselFeaturesVO();
        vesselFeatures.setVesselId(source.getVessel().getId());
        target.setVesselFeatures(vesselFeatures);
        target.setQualityFlagId(source.getQualityFlag().getId());

        // Departure & return locations
        target.setDepartureLocation(locationDao.toLocationVO(source.getDepartureLocation()));
        target.setReturnLocation(locationDao.toLocationVO(source.getReturnLocation()));

        // Recorder department
        DepartmentVO recorderDepartment = departmentDao.toDepartmentVO(source.getRecorderDepartment());
        target.setRecorderDepartment(recorderDepartment);

        // Recorder person
        if (source.getRecorderPerson() != null) {
            PersonVO recorderPerson = personDao.toPersonVO(source.getRecorderPerson());
            target.setRecorderPerson(recorderPerson);
        }

        return target;
    }

    /* -- protected methods -- */

    protected List<TripVO> toTripVOs(List<Trip> source) {
        return source.stream()
                .map(this::toTripVO)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    protected void tripVOToEntity(TripVO source, Trip target, boolean copyIfNull) {

        Beans.copyProperties(source, target);

        // Program
        if (copyIfNull || (source.getProgram() != null && (source.getProgram().getId() != null || source.getProgram().getLabel() != null))) {
            if (source.getProgram() == null || (source.getProgram().getId() == null && source.getProgram().getLabel() == null)) {
                target.setProgram(null);
            }
            // Load by id
            else if (source.getProgram().getId() != null){
                target.setProgram(load(Program.class, source.getProgram().getId()));
            }
            // Load by label
            else {
                ProgramVO program = programDao.getByLabel(source.getProgram().getLabel());
                target.setProgram(load(Program.class, program.getId()));
            }
        }

        // Vessel
        if (copyIfNull || (source.getVesselFeatures() != null && source.getVesselFeatures().getVesselId() != null)) {
            if (source.getVesselFeatures() == null || source.getVesselFeatures().getVesselId() == null) {
                target.setVessel(null);
            }
            else {
                target.setVessel(load(Vessel.class, source.getVesselFeatures().getVesselId()));
            }
        }

        // Departure location
        if (copyIfNull || source.getDepartureLocation() != null) {
            if (source.getDepartureLocation() == null || source.getDepartureLocation().getId() == null) {
                target.setDepartureLocation(null);
            }
            else {
                target.setDepartureLocation(load(Location.class, source.getDepartureLocation().getId()));
            }
        }

        // Return location
        if (copyIfNull || source.getReturnLocation() != null) {
            if (source.getReturnLocation() == null || source.getReturnLocation().getId() == null) {
                target.setReturnLocation(null);
            }
            else {
                target.setReturnLocation(load(Location.class, source.getReturnLocation().getId()));
            }
        }

        // Recorder department
        if (copyIfNull || source.getRecorderDepartment() != null) {
            if (source.getRecorderDepartment() == null || source.getRecorderDepartment().getId() == null) {
                target.setRecorderDepartment(null);
            }
            else {
                target.setRecorderDepartment(load(Department.class, source.getRecorderDepartment().getId()));
            }
        }

        // Recorder person
        if (copyIfNull || source.getRecorderPerson() != null) {
            if (source.getRecorderPerson() == null || source.getRecorderPerson().getId() == null) {
                target.setRecorderPerson(null);
            }
            else {
                target.setRecorderPerson(load(Person.class, source.getRecorderPerson().getId()));
            }
        }

        // Quality flag
        if (copyIfNull || source.getQualityFlagId() != null) {
            if (source.getQualityFlagId() == null) {
                target.setQualityFlag(load(QualityFlag.class, config.getDefaultQualityFlagId()));
            }
            else {
                target.setQualityFlag(load(QualityFlag.class, source.getQualityFlagId()));
            }
        }
    }
}

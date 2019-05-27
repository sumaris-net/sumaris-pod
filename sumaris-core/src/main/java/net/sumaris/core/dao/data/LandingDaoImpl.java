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
import net.sumaris.core.dao.administration.user.PersonDao;
import net.sumaris.core.dao.referential.ReferentialDao;
import net.sumaris.core.dao.referential.location.LocationDao;
import net.sumaris.core.model.data.Landing;
import net.sumaris.core.model.data.ObservedLocation;
import net.sumaris.core.model.data.Trip;
import net.sumaris.core.model.data.Vessel;
import net.sumaris.core.model.referential.location.Location;
import net.sumaris.core.util.Beans;
import net.sumaris.core.vo.administration.programStrategy.ProgramVO;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.data.LandingVO;
import net.sumaris.core.vo.data.TripVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Root;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Repository("landingDao")
public class LandingDaoImpl extends BaseDataDaoImpl implements LandingDao {

    /** Logger. */
    private static final Logger log =
            LoggerFactory.getLogger(LandingDaoImpl.class);

    @Autowired
    private LocationDao locationDao;

    @Autowired
    private ReferentialDao referentialDao;

    @Autowired
    private PersonDao personDao;

    @Autowired
    private VesselDao vesselDao;

    @Override
    @SuppressWarnings("unchecked")
    public List<LandingVO> getAllByTripId(int tripId) {

        CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<Landing> query = cb.createQuery(Landing.class);
        Root<Landing> landingRoot = query.from(Landing.class);

        query.select(landingRoot);

        ParameterExpression<Integer> tripIdParam = cb.parameter(Integer.class);

        query.where(cb.equal(landingRoot.get(Landing.PROPERTY_TRIP).get(Trip.PROPERTY_ID), tripIdParam));

        return toLandingVOs(getEntityManager().createQuery(query)
                .setParameter(tripIdParam, tripId).getResultList(), false);
    }


    @Override
    public LandingVO get(int id) {
        Landing entity = get(Landing.class, id);
        return toLandingVO(entity, false);
    }

    @Override
    public List<LandingVO> saveAllByObservedLocationId(int observedLocationId, List<LandingVO> sources) {
        // Load parent entity
        ObservedLocation parent = get(ObservedLocation.class, observedLocationId);
        ProgramVO parentProgram = new ProgramVO();
        parentProgram.setId(parent.getProgram().getId());

        // Remember existing entities
        final List<Integer> sourcesIdsToRemove = Beans.collectIds(Beans.getList(parent.getLandings()));

        // Save each gears
        List<LandingVO> result = sources.stream().map(source -> {
            source.setObservedLocationId(observedLocationId);
            source.setProgram(parentProgram);

            if (source.getId() != null) {
                sourcesIdsToRemove.remove(source.getId());
            }
            return save(source);
        }).collect(Collectors.toList());

        // Remove unused entities
        if (CollectionUtils.isNotEmpty(sourcesIdsToRemove)) {
            sourcesIdsToRemove.forEach(this::delete);
        }

        return result;
    }

    @Override
    public LandingVO save(LandingVO source) {
        Preconditions.checkNotNull(source);

        EntityManager entityManager = getEntityManager();
        Landing entity = null;
        if (source.getId() != null) {
            entity = get(Landing.class, source.getId());
        }
        boolean isNew = (entity == null);
        if (isNew) {
            entity = new Landing();
        }

        if (!isNew) {
            // Check update date
            checkUpdateDateForUpdate(source, entity);

            // Lock entityName
            //lockForUpdate(entity);
        }

        // VO -> Entity
        landingVOToEntity(source, entity, true);

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

        entityManager.flush();
        entityManager.clear();

        return source;
    }

    @Override
    public void delete(int id) {

        log.debug(String.format("Deleting landing {id=%s}...", id));
        delete(Landing.class, id);
    }

    @Override
    public LandingVO toLandingVO(Landing source) {
        return this.toLandingVO(source, true);
    }

    public LandingVO toLandingVO(Landing source, boolean allFields) {
        if (source == null) return null;

        LandingVO target = new LandingVO();

        Beans.copyProperties(source, target);

        // Landing location
        target.setLandingLocation(locationDao.toLocationVO(source.getLandingLocation()));

        if (allFields) {
            target.setVesselFeatures(vesselDao.getByVesselIdAndDate(source.getVessel().getId(), source.getLandingDateTime()));
            target.setQualityFlagId(source.getQualityFlag().getId());

            // Recorder department
            DepartmentVO recorderDepartment = referentialDao.toTypedVO(source.getRecorderDepartment(), DepartmentVO.class);
            target.setRecorderDepartment(recorderDepartment);

            // Recorder person
            if (source.getRecorderPerson() != null) {
                PersonVO recorderPerson = personDao.toPersonVO(source.getRecorderPerson());
                target.setRecorderPerson(recorderPerson);
            }
        }

        return target;
    }

    /* -- protected methods -- */

    protected List<LandingVO> toLandingVOs(List<Landing> source, boolean allFields) {
        return this.toLandingVOs(source.stream(), allFields);
    }

    protected List<LandingVO> toLandingVOs(Stream<Landing> source, boolean allFields) {
        return source.map(s -> this.toLandingVO(s, allFields))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    protected void landingVOToEntity(LandingVO source, Landing target, boolean copyIfNull) {

        copyRootDataProperties(source, target, copyIfNull);

        // Vessel
        copyVessel(source, target, copyIfNull);

        // Landing location
        if (copyIfNull || source.getLandingLocation() != null) {
            if (source.getLandingLocation() == null || source.getLandingLocation().getId() == null) {
                target.setLandingLocation(null);
            }
            else {
                target.setLandingLocation(load(Location.class, source.getLandingLocation().getId()));
            }
        }

        // Observed Location
        Integer observedLocationId = source.getObservedLocationId() != null ? source.getObservedLocationId() : (source.getObservedLocation() != null ? source.getObservedLocation().getId() : null);
        if (copyIfNull || (observedLocationId != null)) {
            if (observedLocationId == null) {
                target.setObservedLocation(null);
            }
            else {
                target.setObservedLocation(load(ObservedLocation.class, observedLocationId));
            }
        }

        // Trip
        Integer tripId = source.getTripId() != null ? source.getTripId() : (source.getTrip() != null ? source.getTrip().getId() : null);
        if (copyIfNull || (tripId != null)) {
            if (tripId == null) {
                target.setTrip(null);
            }
            else {
                target.setTrip(load(Trip.class, tripId));
            }
        }
    }
}

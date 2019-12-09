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
import com.google.common.collect.Lists;
import net.sumaris.core.dao.referential.ReferentialDao;
import net.sumaris.core.dao.technical.Daos;
import net.sumaris.core.dao.technical.model.IEntity;
import net.sumaris.core.model.data.PhysicalGear;
import net.sumaris.core.model.data.PhysicalGearMeasurement;
import net.sumaris.core.model.data.Trip;
import net.sumaris.core.model.referential.gear.Gear;
import net.sumaris.core.util.Beans;
import net.sumaris.core.vo.administration.programStrategy.ProgramVO;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.data.MeasurementVO;
import net.sumaris.core.vo.data.PhysicalGearVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Root;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Repository("physicalGearDao")
public class PhysicalGearDaoImpl extends BaseDataDaoImpl implements PhysicalGearDao {

    /** Logger. */
    private static final Logger log =
            LoggerFactory.getLogger(PhysicalGearDaoImpl.class);

    @Autowired
    private ReferentialDao referentialDao;

    @Autowired
    private MeasurementDao measurementDao;


    @Override
    public List<PhysicalGearVO> getPhysicalGearByTripId(int tripId) {
        CriteriaBuilder builder = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<PhysicalGear> query = builder.createQuery(PhysicalGear.class);
        Root<PhysicalGear> root = query.from(PhysicalGear.class);

        ParameterExpression<Integer> tripIdParam = builder.parameter(Integer.class);

        query.select(root)
            .where(builder.equal(root.get(PhysicalGear.Fields.TRIP).get(IEntity.Fields.ID), tripIdParam));

        TypedQuery<PhysicalGear> q = getEntityManager().createQuery(query)
                .setParameter(tripIdParam, tripId);
        return toPhysicalGearVOs(q.getResultList());
    }

    @Override
    public List<PhysicalGearVO> save(final int tripId, final List<PhysicalGearVO> sources) {

        // Load parent entity
        Trip parent = get(Trip.class, tripId);
        ProgramVO parentProgram = new ProgramVO();
        parentProgram.setId(parent.getProgram().getId());

        // Remember existing entities
        final Map<Integer, PhysicalGear> sourcesToRemove = Beans.splitById(parent.getPhysicalGears());

        // Save each sources
        List<PhysicalGearVO> result = sources.stream().map(gear -> {
            gear.setTripId(tripId);
            gear.setProgram(parentProgram);

            boolean isNew = (gear.getId() == null) || (sourcesToRemove.remove(gear.getId()) == null);
            if (isNew) {
                gear.setId(null);
            }

            return save(gear);
        }).collect(Collectors.toList());

        // Remove unused entities
        if (MapUtils.isNotEmpty(sourcesToRemove)) {
            sourcesToRemove.values().forEach(this::delete);
        }

        // Update the parent list
        Daos.replaceEntities(parent.getPhysicalGears(),
                result,
                (vo) -> load(PhysicalGear.class, vo.getId()));

        // Save measurements on each gears
        // NOTE: using the savedGear to be sure to get an id
        result.forEach(source -> {

            if (source.getMeasurementValues() != null) {
                measurementDao.savePhysicalGearMeasurementsMap(source.getId(), source.getMeasurementValues());
            }
            else {
                List<MeasurementVO> measurements = Beans.getList(source.getMeasurements());
                int rankOrder = 1;
                for (MeasurementVO m : measurements) {
                    fillDefaultProperties(source, m);
                    m.setRankOrder(rankOrder++);
                }
                measurements = measurementDao.savePhysicalGearMeasurements(source.getId(), measurements);
                source.setMeasurements(measurements);
            }
        });

        return result;
    }

    @Override
    public PhysicalGearVO save(PhysicalGearVO source) {
        Preconditions.checkNotNull(source);
        Preconditions.checkNotNull(source.getGear(), "Missing gear");
        Preconditions.checkNotNull(source.getGear().getId(), "Missing gear.id");

        EntityManager session = getEntityManager();
        PhysicalGear entity = null;
        if (source.getId() != null) {
            entity = get(PhysicalGear.class, source.getId());
        }
        boolean isNew = (entity == null);
        if (isNew) {
            entity = new PhysicalGear();
        }
        else {
            // Lock entityName
            // TODO: Use an optimistic lock, as we already lock the parent entity
            //lockForUpdate(entity, LockModeType.OPTIMISTIC);
        }

        // VO -> Entity
        physicalGearVOToEntity(source, entity, true );

        // Update update_dt
        Timestamp newUpdateDate = getDatabaseCurrentTimestamp();
        entity.setUpdateDate(newUpdateDate);

        // Save entityName
        if (isNew) {
            // Force creation date
            entity.setCreationDate(newUpdateDate);
            source.setCreationDate(newUpdateDate);

            session.persist(entity);
            source.setId(entity.getId());
        } else {
            session.merge(entity);
        }

        source.setUpdateDate(newUpdateDate);

        //session.flush();
        //session.clear();

        return source;
    }

    @Override
    public PhysicalGearVO toPhysicalGearVO(PhysicalGear source, boolean withDetails) {
        if (source == null) return null;

        PhysicalGearVO target = new PhysicalGearVO();

        Beans.copyProperties(source, target);

        // Gear
        ReferentialVO gear = referentialDao.toReferentialVO(source.getGear());
        target.setGear(gear);

        if (withDetails) {

            // Quality flag
            target.setQualityFlagId(source.getQualityFlag().getId());

            // Recorder department
            DepartmentVO recorderDepartment = referentialDao.toTypedVO(source.getRecorderDepartment(), DepartmentVO.class).orElse(null);
            target.setRecorderDepartment(recorderDepartment);

        }

        return target;
    }

    @Override
    public PhysicalGearVO toPhysicalGearVO(PhysicalGear source) {
        return toPhysicalGearVO(source, true);
    }

    /* -- protected methods -- */

    protected void delete(PhysicalGear entity) {
        Preconditions.checkNotNull(entity);

        if (CollectionUtils.isNotEmpty(entity.getMeasurements())) {
            // TODO: check if deleted
            log.warn("Check if measurements of physical gear id=" +entity.getId()+ " has been deleted");
        }
        getEntityManager().remove(entity);
    }

    protected void physicalGearVOToEntity(PhysicalGearVO source, PhysicalGear target, boolean copyIfNull) {

        // Copy properties
        copyRootDataProperties(source, target, copyIfNull);

        // Gear
        target.setGear(load(Gear.class, source.getGear().getId()));

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

    protected  List<PhysicalGearVO> toPhysicalGearVOs(List<PhysicalGear> source) {
        return source.stream()
                .map(this::toPhysicalGearVO)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    void fillDefaultProperties(PhysicalGearVO parent, MeasurementVO measurement) {
        if (measurement == null) return;

        // Copy recorder department from the parent
        if (measurement.getRecorderDepartment() == null || measurement.getRecorderDepartment().getId() == null) {
            measurement.setRecorderDepartment(parent.getRecorderDepartment());
        }
        // Copy recorder person from the parent
        if (measurement.getRecorderPerson() == null || measurement.getRecorderPerson().getId() == null) {
            measurement.setRecorderPerson(parent.getRecorderPerson());
        }

        measurement.setEntityName(PhysicalGearMeasurement.class.getSimpleName());
    }
}

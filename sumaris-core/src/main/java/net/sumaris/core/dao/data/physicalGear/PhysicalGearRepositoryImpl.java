package net.sumaris.core.dao.data.physicalGear;

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
import net.sumaris.core.dao.data.MeasurementDao;
import net.sumaris.core.dao.data.RootDataRepositoryImpl;
import net.sumaris.core.dao.referential.BaseRefRepository;
import net.sumaris.core.dao.technical.Daos;
import net.sumaris.core.model.data.PhysicalGear;
import net.sumaris.core.model.data.PhysicalGearMeasurement;
import net.sumaris.core.model.data.Trip;
import net.sumaris.core.model.referential.gear.Gear;
import net.sumaris.core.util.Beans;
import net.sumaris.core.vo.administration.programStrategy.ProgramVO;
import net.sumaris.core.vo.data.DataFetchOptions;
import net.sumaris.core.vo.data.MeasurementVO;
import net.sumaris.core.vo.data.PhysicalGearVO;
import net.sumaris.core.vo.filter.PhysicalGearFilterVO;
import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PhysicalGearRepositoryImpl
    extends RootDataRepositoryImpl<PhysicalGear, PhysicalGearVO, PhysicalGearFilterVO, DataFetchOptions>
    implements PhysicalGearSpecifications {

    private static final Logger log =
        LoggerFactory.getLogger(PhysicalGearRepositoryImpl.class);

    private final BaseRefRepository baseRefRepository;
    private final MeasurementDao measurementDao;

    @Autowired
    public PhysicalGearRepositoryImpl(EntityManager entityManager,
                                      MeasurementDao measurementDao,
                                      BaseRefRepository baseRefRepository) {
        super(PhysicalGear.class, PhysicalGearVO.class, entityManager);
        this.measurementDao = measurementDao;
        this.baseRefRepository = baseRefRepository;
    }

    @Override
    public Specification<PhysicalGear> toSpecification(PhysicalGearFilterVO filter) {
        return super.toSpecification(filter)
            .and(hasVesselId(filter.getVesselId()))
            .and(hasTripId(filter.getTripId()))
            .and(betweenDate(filter.getStartDate(), filter.getEndDate()));
    }

    @Override
    public void toVO(PhysicalGear source, PhysicalGearVO target, DataFetchOptions fetchOptions, boolean copyIfNull) {
        super.toVO(source, target, fetchOptions, copyIfNull);

        // Gear
        Gear gear = source.getGear();
        if (copyIfNull || gear != null) {
            if (gear == null) {
                target.setGear(null);
            } else {
                target.setGear(baseRefRepository.toVO(gear));
            }
        }

        // Trip
        Trip trip = source.getTrip();
        if (copyIfNull || trip != null) {
            if (trip == null) {
                target.setTripId(null);
            } else {
                target.setTripId(trip.getId());
            }
        }
    }

    @Override
    public void toEntity(PhysicalGearVO source, PhysicalGear target, boolean copyIfNull) {
        Preconditions.checkNotNull(source);
        Preconditions.checkNotNull(source.getGear(), "Missing gear");
        Preconditions.checkNotNull(source.getGear().getId(), "Missing gear.id");

        // Copy properties
        super.toEntity(source, target, copyIfNull);

        // Gear
        Integer gearId = source.getGear() != null ? source.getGear().getId() : null;
        if (copyIfNull || gearId != null) {
            if (gearId == null) {
                target.setGear(null);
            } else {
                target.setGear(load(Gear.class, gearId));
            }
        }

        // Trip
        Integer tripId = source.getTripId() != null ? source.getTripId() : (source.getTrip() != null ? source.getTrip().getId() : null);
        if (copyIfNull || (tripId != null)) {
            if (tripId == null) {
                target.setTrip(null);
            } else {
                target.setTrip(load(Trip.class, tripId));
            }
        }

    }

    public List<PhysicalGearVO> saveAllByTripId(final int tripId, final List<PhysicalGearVO> sources) {

        // Load parent entity
        Trip parent = find(Trip.class, tripId);
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
        // NOTE: using the savedGear to be sure to find an id
        result.forEach(source -> {

            if (source.getMeasurementValues() != null) {
                measurementDao.savePhysicalGearMeasurementsMap(source.getId(), source.getMeasurementValues());
            } else {
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

    /* -- protected methods -- */

    protected void fillDefaultProperties(PhysicalGearVO parent, MeasurementVO measurement) {
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

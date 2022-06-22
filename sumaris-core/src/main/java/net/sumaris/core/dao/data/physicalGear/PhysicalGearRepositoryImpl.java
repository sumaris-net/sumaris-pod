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
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.data.MeasurementDao;
import net.sumaris.core.dao.data.RootDataRepositoryImpl;
import net.sumaris.core.dao.referential.ReferentialDao;
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
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
public class PhysicalGearRepositoryImpl
    extends RootDataRepositoryImpl<PhysicalGear, PhysicalGearVO, PhysicalGearFilterVO, DataFetchOptions>
    implements PhysicalGearSpecifications {

    private final ReferentialDao referentialDao;
    private final MeasurementDao measurementDao;

    @Autowired
    public PhysicalGearRepositoryImpl(EntityManager entityManager,
                                      MeasurementDao measurementDao,
                                      ReferentialDao referentialDao) {
        super(PhysicalGear.class, PhysicalGearVO.class, entityManager);
        this.measurementDao = measurementDao;
        this.referentialDao = referentialDao;
    }

    @Override
    public Specification<PhysicalGear> toSpecification(PhysicalGearFilterVO filter, DataFetchOptions fetchOptions) {
        return super.toSpecification(filter, fetchOptions)
            .and(betweenDate(filter.getStartDate(), filter.getEndDate()))
            .and(hasVesselId(filter.getVesselId()))
            // Trip
            .and(hasTripId(filter.getTripId()))
            .and(excludeTripId(filter.getExcludeTripId()))
            // Parent
            .and(hasParentGearId(filter.getParentGearId()))
            .and(excludeParentGearId(filter.getExcludeParentGearId()))
            .and(excludeParentGear(filter.getExcludeParentGear()))
            .and(excludeChildGear(filter.getExcludeChildGear()))
            // Quality
            .and(inDataQualityStatus(filter.getDataQualityStatus()));
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
                target.setGear(referentialDao.toVO(gear));
            }
        }

        // Parent
        if (source.getParent() != null) {
            target.setParentId(source.getParent().getId());
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

        // Fetch children
        if (fetchOptions != null && fetchOptions.isWithChildrenEntities()) {
            // Measurements values
            target.setMeasurementValues(measurementDao.getPhysicalGearMeasurementsMap(source.getId()));
        }
    }

    @Override
    public void toEntity(PhysicalGearVO source, PhysicalGear target, boolean copyIfNull) {
        Preconditions.checkNotNull(source);
        Preconditions.checkNotNull(source.getGear(), "Missing gear");
        Preconditions.checkNotNull(source.getGear().getId(), "Missing gear.id");

        // Copy some data from parent
        if (source.getParent() != null) {
            source.setProgram(source.getParent().getProgram());
            source.setRecorderDepartment(source.getParent().getRecorderDepartment());
            source.setRecorderPerson(source.getParent().getRecorderPerson());
        }

        // Parent
        Integer parentId = source.getParent() != null ? source.getParent().getId() : source.getParentId();
        Integer tripId = source.getTripId() != null ? source.getTripId() : (source.getTrip() != null ? source.getTrip().getId() : null);
        if (copyIfNull || (parentId != null)) {

            // Check if parent changed
            PhysicalGear previousParent = target.getParent();
            if (previousParent != null && !Objects.equals(parentId, previousParent.getId()) && CollectionUtils.isNotEmpty(previousParent.getChildren())) {
                // Remove in the parent children list (to avoid a DELETE CASCADE if the parent is delete later - fix #15)
                previousParent.getChildren().remove(target);
            }

            if (parentId == null) {
                target.setParent(null);
            }
            else {
                PhysicalGear parent = getReference(PhysicalGear.class, parentId);
                target.setParent(parent);

                // Not need to update the children collection, because mapped by the 'parent' property
                //if (!parent.getChildren().contains(target)) {
                //    parent.getChildren().add(target);
                //}

                // Force using the parent's trip
                tripId = parent.getTrip().getId();
            }
        }

        // /!\ IMPORTANT: update source's parentId, tripId, BEFORE calling hashCode()
        source.setParentId(parentId);
        source.setTripId(tripId);

        // Copy properties, and data stuff (program, qualityFlag, recorder, ...)
        super.toEntity(source, target, copyIfNull);

        // Gear
        Integer gearId = source.getGear() != null ? source.getGear().getId() : null;
        if (copyIfNull || gearId != null) {
            if (gearId == null) {
                target.setGear(null);
            } else {
                target.setGear(getReference(Gear.class, gearId));
            }
        }

        // Trip
        if (copyIfNull || (tripId != null)) {
            if (tripId == null) {
                target.setTrip(null);
            } else {
                target.setTrip(getReference(Trip.class, tripId));
            }
        }
    }

    public List<PhysicalGearVO> saveAllByTripId(final int tripId, final List<PhysicalGearVO> sources) {
        return saveAllByTripId(tripId, sources, null);
    }

    public List<PhysicalGearVO> saveAllByTripId(final int tripId, final List<PhysicalGearVO> sources, List<Integer> idsToRemove) {

        // Load parent entity
        Trip parent = getById(Trip.class, tripId);
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
            if (idsToRemove != null){
                idsToRemove.addAll(sourcesToRemove.keySet());
            }
            else {
                sourcesToRemove.values().forEach(this::delete);
            }
        }

        // Update the parent list
        Daos.replaceEntities(parent.getPhysicalGears(),
            result,
            (vo) -> getReference(PhysicalGear.class, vo.getId()));

        // Save measurements on each gears
        // NOTE: using the savedGear to be sure to find an id
        result.forEach(source -> {

            if (source.getMeasurementValues() != null) {
                measurementDao.savePhysicalGearMeasurementsMap(source.getId(), source.getMeasurementValues());
            } else {
                List<MeasurementVO> measurements = Beans.getList(source.getMeasurements());
                short rankOrder = 1;
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

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

import net.sumaris.core.dao.referential.ReferentialDao;
import net.sumaris.core.dao.referential.location.LocationDao;
import net.sumaris.core.model.data.PhysicalGear;
import net.sumaris.core.model.data.ObservedLocation;
import net.sumaris.core.model.data.Trip;
import net.sumaris.core.model.referential.gear.Gear;
import net.sumaris.core.model.referential.location.Location;
import net.sumaris.core.util.Beans;
import net.sumaris.core.vo.administration.programStrategy.ProgramVO;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.data.DataFetchOptions;
import net.sumaris.core.vo.data.PhysicalGearVO;
import net.sumaris.core.vo.filter.PhysicalGearFilterVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class PhysicalGearRepositoryImpl
    extends DataRepositoryImpl<PhysicalGear, Integer, PhysicalGearVO, PhysicalGearFilterVO>
    implements PhysicalGearRepositoryExtend {

    private static final Logger log =
        LoggerFactory.getLogger(PhysicalGearRepositoryImpl.class);

    private final ReferentialDao referentialDao;

    @Autowired
    public PhysicalGearRepositoryImpl(EntityManager entityManager, ReferentialDao referentialDao) {
        super(PhysicalGear.class, entityManager);
        this.referentialDao = referentialDao;
    }

    @Override
    public Specification<PhysicalGear> toSpecification(PhysicalGearFilterVO filter) {
        if (filter == null) return null;

        return Specification.where(hasVesselId(filter.getVesselId()))
            .and(betweenDate(filter.getStartDate(), filter.getEndDate()));
    }

    public Class<PhysicalGearVO> getVOClass() {
        return PhysicalGearVO.class;
    }

    @Override
    public void toVO(PhysicalGear source, PhysicalGearVO target, DataFetchOptions fetchOptions, boolean copyIfNull) {
        super.toVO(source, target, fetchOptions, copyIfNull);
        Beans.copyProperties(source, target);

        // Gear
        if (source.getGear() != null) {
            ReferentialVO gear = referentialDao.toReferentialVO(source.getGear());
            target.setGear(gear);
        }

        // Trip
        if (source.getTrip() != null) {
            target.setTripId(source.getTrip().getId());
        }
    }

    @Override
    public void toEntity(PhysicalGearVO source, PhysicalGear target, boolean copyIfNull) {

        // Copy properties
        super.toEntity(source, target, copyIfNull);

        // Gear
        Integer gearId = source.getGear() != null ? source.getGear().getId() : null;
        if (copyIfNull || gearId != null){
            if (gearId == null) {
                target.setGear(null);
            }
            else {
                target.setGear(load(Gear.class, gearId));
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

package net.sumaris.core.dao.data.vessel;

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

import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.dao.referential.location.LocationRepository;
import net.sumaris.core.dao.technical.Pageables;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.dao.technical.jpa.BindableSpecification;
import net.sumaris.core.dao.technical.jpa.SumarisJpaRepositoryImpl;
import net.sumaris.core.model.data.Vessel;
import net.sumaris.core.model.data.VesselRegistrationPeriod;
import net.sumaris.core.model.referential.QualityFlag;
import net.sumaris.core.model.referential.location.Location;
import net.sumaris.core.vo.data.VesselRegistrationPeriodVO;
import net.sumaris.core.vo.filter.VesselFilterVO;
import net.sumaris.core.vo.referential.LocationVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.EntityManager;
import java.util.Date;
import java.util.Optional;

@Slf4j
public class VesselRegistrationPeriodRepositoryImpl
    extends SumarisJpaRepositoryImpl<VesselRegistrationPeriod, Integer, VesselRegistrationPeriodVO>
    implements VesselRegistrationPeriodSpecifications {

    private final LocationRepository locationRepository;

    @Autowired
    public VesselRegistrationPeriodRepositoryImpl(EntityManager entityManager,
                                                  LocationRepository locationRepository) {
        super(VesselRegistrationPeriod.class, VesselRegistrationPeriodVO.class, entityManager);
        this.locationRepository = locationRepository;
    }

    @Override
    public Specification<VesselRegistrationPeriod> toSpecification(VesselFilterVO filter) {
        return BindableSpecification.where(vesselId(filter.getVesselId()))
            .and(betweenDate(filter.getStartDate(), filter.getEndDate()));
    }

    @Override
    public Page<VesselRegistrationPeriodVO> findAll(VesselFilterVO filter, Pageable pageable) {
        return super.findAll(toSpecification(filter), pageable)
            .map(this::toVO);
    }

    @Override
    public Optional<VesselRegistrationPeriodVO> getLastByVesselId(int vesselId) {
        return getByVesselIdAndDate(vesselId, null).map(this::toVO);
    }

    @Override
    public Optional<VesselRegistrationPeriod> getByVesselIdAndDate(int vesselId, Date date) {

        Pageable pageable = Pageables.create(0, 1,
            VesselRegistrationPeriod.Fields.START_DATE,
            SortDirection.DESC);
        Specification<VesselRegistrationPeriod> specification = vesselId(vesselId).and(atDate(date));
        Optional<VesselRegistrationPeriod> result = findAll(specification, pageable).get().findFirst();

        // Nothing found: retry without a date, if not already the case
        if (!result.isPresent() && date != null) {
            return getByVesselIdAndDate(vesselId, null);
        }

        return result;
    }

    @Override
    public void toVO(VesselRegistrationPeriod source, VesselRegistrationPeriodVO target, boolean copyIfNull) {
        super.toVO(source, target, copyIfNull);

        // Registration location
        LocationVO registrationLocation = locationRepository.toVO(source.getRegistrationLocation());
        target.setRegistrationLocation(registrationLocation);

        // Quality flag
        target.setQualityFlagId(source.getQualityFlag().getId());
    }

    @Override
    public void toEntity(VesselRegistrationPeriodVO source, VesselRegistrationPeriod target, boolean copyIfNull) {
        super.toEntity(source, target, copyIfNull);

        // Registration location
        if (copyIfNull || source.getRegistrationLocation() != null) {
            if (source.getRegistrationLocation() == null || source.getRegistrationLocation().getId() == null) {
                target.setRegistrationLocation(null);
            } else {
                target.setRegistrationLocation(getReference(Location.class, source.getRegistrationLocation().getId()));
            }
        }

        // Vessel
        if (copyIfNull || source.getVessel() != null) {
            if (source.getVessel() == null || source.getVessel().getId() == null) {
                target.setVessel(null);
            } else {
                target.setVessel(getReference(Vessel.class, source.getVessel().getId()));
            }
        }

        // default quality flag
        if (target.getQualityFlag() == null) {
            target.setQualityFlag(getReference(QualityFlag.class, SumarisConfiguration.getInstance().getDefaultQualityFlagId()));
        }

        // default rank order
        if (target.getRankOrder() == null) {
            target.setRankOrder(1);
        }
    }

    @Override
    protected void onAfterSaveEntity(VesselRegistrationPeriodVO vo, VesselRegistrationPeriod savedEntity, boolean isNew) {
        super.onAfterSaveEntity(vo, savedEntity, isNew);
    }
}

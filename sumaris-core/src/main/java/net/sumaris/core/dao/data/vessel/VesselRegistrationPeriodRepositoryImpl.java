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
import net.sumaris.core.dao.data.DataRepositoryImpl;
import net.sumaris.core.dao.referential.ReferentialDao;
import net.sumaris.core.dao.referential.location.LocationRepository;
import net.sumaris.core.dao.technical.Pageables;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.dao.technical.jpa.SumarisJpaRepository;
import net.sumaris.core.dao.technical.jpa.SumarisJpaRepositoryImpl;
import net.sumaris.core.model.data.VesselRegistrationPeriod;
import net.sumaris.core.util.Beans;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.data.DataFetchOptions;
import net.sumaris.core.vo.data.VesselRegistrationPeriodVO;
import net.sumaris.core.vo.filter.VesselFilterVO;
import net.sumaris.core.vo.referential.LocationVO;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.EntityManager;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Slf4j
public class VesselRegistrationPeriodRepositoryImpl
    extends SumarisJpaRepositoryImpl<VesselRegistrationPeriod, Integer, VesselRegistrationPeriodVO>
    implements VesselRegistrationPeriodSpecifications {

    private final ReferentialDao referentialDao;
    private final LocationRepository locationRepository;

    @Autowired
    public VesselRegistrationPeriodRepositoryImpl(EntityManager entityManager,
                                                  ReferentialDao referentialDao,
                                                  LocationRepository locationRepository) {
        super(VesselRegistrationPeriod.class, VesselRegistrationPeriodVO.class, entityManager);
        this.referentialDao = referentialDao;
        this.locationRepository = locationRepository;
    }

    @Override
    public Optional<VesselRegistrationPeriodVO> getByVesselIdAndDate(int vesselId, Date date) {

        Pageable pageable = Pageables.create(0, 1,
            VesselRegistrationPeriod.Fields.START_DATE,
            SortDirection.DESC);
        Specification<VesselRegistrationPeriod> specification = vesselId(vesselId).and(atDate(date));
        Optional<VesselRegistrationPeriod> result = findAll(specification, pageable).get().findFirst();

        // Nothing at this date: retry to get the last period
        if (!result.isPresent() && date != null) {
            specification = vesselId(vesselId);
            result = findAll(specification, pageable).get().findFirst();
        }

        // Transform to VO
        return result.map(this::toVO);
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
    }

    @Override
    protected void onAfterSaveEntity(VesselRegistrationPeriodVO vo, VesselRegistrationPeriod savedEntity, boolean isNew) {
        super.onAfterSaveEntity(vo, savedEntity, isNew);
    }
}

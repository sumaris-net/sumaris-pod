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
import net.sumaris.core.model.data.Vessel;
import net.sumaris.core.model.data.VesselFeatures;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.data.DataFetchOptions;
import net.sumaris.core.vo.data.VesselSnapshotVO;
import net.sumaris.core.vo.filter.VesselFilterVO;
import net.sumaris.core.vo.referential.LocationVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.EntityManager;

@Slf4j
public class VesselSnapshotRepositoryImpl
    extends DataRepositoryImpl<VesselFeatures, VesselSnapshotVO, VesselFilterVO, DataFetchOptions>
    implements VesselFeaturesSpecifications<VesselFeatures, VesselSnapshotVO, VesselFilterVO, DataFetchOptions> {

    private final VesselRegistrationPeriodRepository vesselRegistrationPeriodRepository;
    private final LocationRepository locationRepository;
    private final ReferentialDao referentialDao;

    @Autowired
    public VesselSnapshotRepositoryImpl(EntityManager entityManager,
                                        VesselRegistrationPeriodRepository vesselRegistrationPeriodRepository,
                                        LocationRepository locationRepository,
                                        ReferentialDao referentialDao) {
        super(VesselFeatures.class, VesselSnapshotVO.class, entityManager);
        this.vesselRegistrationPeriodRepository = vesselRegistrationPeriodRepository;
        this.locationRepository = locationRepository;
        this.referentialDao = referentialDao;
    }

    @Override
    public Specification<VesselFeatures> toSpecification(VesselFilterVO filter, DataFetchOptions fetchOptions) {
        return super.toSpecification(filter, fetchOptions)
            .and(id(filter.getVesselFeaturesId()))
            .and(vesselId(filter.getVesselId()))
            .and(betweenDate(filter.getStartDate(), filter.getEndDate()))
            .and(statusIds(filter.getStatusIds()))
            .and(searchText(filter.getSearchAttributes(), filter.getSearchText()));
    }

    @Override
    public void toVO(VesselFeatures source, VesselSnapshotVO target, DataFetchOptions fetchOptions, boolean copyIfNull) {
        super.toVO(source, target, fetchOptions, copyIfNull);

        // Convert from cm to m
        if (source.getLengthOverAll() != null) {
            target.setLengthOverAll(source.getLengthOverAll().doubleValue() / 100);
        }
        // Convert tonnage (divide by 100)
        if (source.getGrossTonnageGrt() != null) {
            target.setGrossTonnageGrt(source.getGrossTonnageGrt().doubleValue() / 100);
        }
        if (source.getGrossTonnageGt() != null) {
            target.setGrossTonnageGt(source.getGrossTonnageGt().doubleValue() / 100);
        }

        // Base port location
        LocationVO basePortLocation = locationRepository.toVO(source.getBasePortLocation());
        target.setBasePortLocation(basePortLocation);

        // Recorder department
        DepartmentVO recorderDepartment = referentialDao.toTypedVO(source.getRecorderDepartment(), DepartmentVO.class).orElse(null);
        target.setRecorderDepartment(recorderDepartment);

        // Vessel
        Vessel vessel = source.getVessel();
        {
            target.setId(vessel.getId());
            target.setVesselStatusId(vessel.getStatus().getId());
            target.setQualityFlagId(vessel.getQualityFlag().getId());

            // Vessel type
            ReferentialVO vesselType = referentialDao.toVO(vessel.getVesselType());
            target.setVesselType(vesselType);
        }

        // Registration period
        vesselRegistrationPeriodRepository.getByVesselIdAndDate(source.getId(), source.getStartDate())
            .ifPresent(period -> {
                // Registration code
                target.setRegistrationCode(period.getRegistrationCode());

                // International registration code
                target.setIntRegistrationCode(period.getIntRegistrationCode());

                // Registration location
                target.setRegistrationLocation(period.getRegistrationLocation());
            });
    }

    @Override
    public void toEntity(VesselSnapshotVO source, VesselFeatures target, boolean copyIfNull) {
        super.toEntity(source, target, copyIfNull);
    }

    @Override
    protected void onAfterSaveEntity(VesselSnapshotVO vo, VesselFeatures savedEntity, boolean isNew) {
        super.onAfterSaveEntity(vo, savedEntity, isNew);
    }
}

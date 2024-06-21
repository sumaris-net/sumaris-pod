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
import net.sumaris.core.dao.referential.location.LocationRepository;
import net.sumaris.core.model.data.*;
import net.sumaris.core.model.referential.location.Location;
import net.sumaris.core.vo.data.DataFetchOptions;
import net.sumaris.core.vo.data.VesselUseFeaturesVO;
import net.sumaris.core.vo.filter.VesselUseFeaturesFilterVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.EntityManager;
import java.util.List;

@Slf4j
public class VesselUseFeaturesRepositoryImpl
        extends UseFeaturesRepositoryImpl<VesselUseFeatures, VesselUseFeaturesVO, VesselUseFeaturesFilterVO, DataFetchOptions>
        implements VesselUseFeaturesSpecifications {

    private final LocationRepository locationRepository;

    @Autowired
    public VesselUseFeaturesRepositoryImpl(EntityManager entityManager,
                                           LocationRepository locationRepository) {
        super(VesselUseFeatures.class, VesselUseFeaturesVO.class, entityManager);
        this.locationRepository = locationRepository;
    }

    @Override
    public Specification<VesselUseFeatures> toSpecification(VesselUseFeaturesFilterVO filter, DataFetchOptions fetchOptions) {
        return super.toSpecification(filter, fetchOptions)
            // ID
            .and(id(filter.getVesselUseFeaturesId(), Integer.class))
            .and(excludedIds(filter.getExcludedIds()))
            .and(includedIds(filter.getIncludedIds()))
            // Program and vessel
            .and(hasProgramLabel(filter.getProgramLabel()))
            .and(hasProgramIds(filter.getProgramIds()))
            .and(hasVesselId(filter.getVesselId()))
            // Parent
            .and(hasActivityCalendarId(filter.getActivityCalendarId()))
            .and(hasDailyActivityCalendarId(filter.getDailyActivityCalendarId()))
            // Dates
            .and(betweenDate(filter.getStartDate(), filter.getEndDate()))
            // Is active
            .and(hasIsActive(filter.getIsActive()))
            // Location
            .and(hasLocationId(filter.getLocationId()))
            .and(hasLocationIds(filter.getLocationIds()))
            // Quality
            .and(inQualityFlagIds(filter.getQualityFlagIds()))
            .and(inDataQualityStatus(filter.getDataQualityStatus()))
            ;
    }

    @Override
    public void toVO(VesselUseFeatures source, VesselUseFeaturesVO target, DataFetchOptions fetchOptions, boolean copyIfNull) {
        super.toVO(source, target, fetchOptions, copyIfNull);

        // Base port Location
        target.setBasePortLocation(locationRepository.toVO(source.getBasePortLocation()));

        // Measurements
        if (fetchOptions != null && fetchOptions.isWithMeasurementValues()) {
            target.setMeasurementValues(measurementDao.getVesselUseFeaturesMeasurementsMap(source.getId()));
        }

        // Activity Calendar
        if (copyIfNull || source.getActivityCalendar() != null) {
            if (source.getActivityCalendar() == null) {
                target.setActivityCalendarId(null);
            }
            else {
                target.setActivityCalendarId(source.getActivityCalendar().getId());
            }
        }

        // Daily Activity Calendar
        if (copyIfNull || source.getDailyActivityCalendar() != null) {
            if (source.getDailyActivityCalendar() == null) {
                target.setDailyActivityCalendarId(null);
            }
            else {
                target.setDailyActivityCalendarId(source.getDailyActivityCalendar().getId());
            }
        }
    }


    public boolean toEntity(VesselUseFeaturesVO source, VesselUseFeatures target, boolean copyIfNull, boolean allowSkipSameHash) {
        if (super.toEntity(source, target, copyIfNull, allowSkipSameHash)) {
            return true;
        }

        // Location
        Integer basePortLocationId = source.getBasePortLocation() != null ? source.getBasePortLocation().getId() : null;
        if (copyIfNull || basePortLocationId != null) {
            if (basePortLocationId == null) {
                target.setBasePortLocation(null);
            }
            else {
                target.setBasePortLocation(getReference(Location.class, basePortLocationId));
            }
        }

        // Activity calendar
        if (copyIfNull || source.getActivityCalendarId() != null) {
            if (source.getActivityCalendarId() == null) {
                target.setActivityCalendar(null);
            }
            else {
                target.setActivityCalendar(getReference(ActivityCalendar.class, source.getActivityCalendarId()));
            }
        }

        // Daily activity calendar
        if (copyIfNull || source.getDailyActivityCalendarId() != null) {
            if (source.getDailyActivityCalendarId() == null) {
                target.setDailyActivityCalendar(null);
            }
            else {
                target.setDailyActivityCalendar(getReference(DailyActivityCalendar.class, source.getDailyActivityCalendarId()));
            }
        }

        return false;
    }

    @Override
    public List<VesselUseFeaturesVO> saveAllByActivityCalendarId(int parentId, List<VesselUseFeaturesVO> sources) {
        ActivityCalendar parent = getById(ActivityCalendar.class, parentId);
        sources.forEach(source -> source.setActivityCalendarId(parentId));
        boolean dirty = this.saveAllByList(parent.getVesselUseFeatures(), sources);
        return sources;
    }

    @Override
    public List<VesselUseFeaturesVO> saveAllByDailyActivityCalendarId(int parentId, List<VesselUseFeaturesVO> sources) {
        DailyActivityCalendar parent = getById(DailyActivityCalendar.class, parentId);
        sources.forEach(source -> source.setDailyActivityCalendarId(parentId));
        boolean dirty = this.saveAllByList(parent.getVesselUseFeatures(), sources);
        return sources;
    }

    /* -- protected functions -- */


    @Override
    protected void onAfterSaveEntity(VesselUseFeaturesVO vo, VesselUseFeatures savedEntity, boolean isNew) {
        super.onAfterSaveEntity(vo, savedEntity, isNew);

        // Save measurements
        measurementDao.saveVesselUseFeaturesMeasurementsMap(savedEntity.getId(), vo.getMeasurementValues());
    }
}

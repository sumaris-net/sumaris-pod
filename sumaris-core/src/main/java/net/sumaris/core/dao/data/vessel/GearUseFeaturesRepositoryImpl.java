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
import net.sumaris.core.dao.data.fishingArea.FishingAreaRepository;
import net.sumaris.core.dao.referential.ReferentialDao;
import net.sumaris.core.dao.referential.metier.MetierRepository;
import net.sumaris.core.model.data.ActivityCalendar;
import net.sumaris.core.model.data.DailyActivityCalendar;
import net.sumaris.core.model.data.GearUseFeatures;
import net.sumaris.core.model.referential.gear.Gear;
import net.sumaris.core.model.referential.metier.Metier;
import net.sumaris.core.vo.data.DataFetchOptions;
import net.sumaris.core.vo.data.GearUseFeaturesVO;
import net.sumaris.core.vo.filter.GearUseFeaturesFilterVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.EntityManager;
import java.util.List;

@Slf4j
public class GearUseFeaturesRepositoryImpl
        extends UseFeaturesRepositoryImpl<GearUseFeatures, GearUseFeaturesVO, GearUseFeaturesFilterVO, DataFetchOptions>
        implements GearUseFeaturesSpecifications
{

    @Autowired
    private MetierRepository metierRepository;

    @Autowired
    private ReferentialDao referentialDao;

    @Autowired
    private FishingAreaRepository fishingAreaRepository;

    @Autowired
    public GearUseFeaturesRepositoryImpl(EntityManager entityManager
    ) {
        super(GearUseFeatures.class, GearUseFeaturesVO.class, entityManager);
    }

    public Specification<GearUseFeatures> toSpecification(GearUseFeaturesFilterVO filter, DataFetchOptions fetchOptions) {
        return super.toSpecification(filter, fetchOptions)
            // ID
            .and(id(filter.getGearUseFeaturesId(), Integer.class))
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
            // Metier
            .and(hasMetierId(filter.getMetierId()))
            .and(hasMetierIds(filter.getMetierIds()))
            // Gear
            .and(hasGearId(filter.getGearId()))
            .and(hasGearIds(filter.getGearIds()))
            // Quality
            .and(inQualityFlagIds(filter.getQualityFlagIds()))
            .and(inDataQualityStatus(filter.getDataQualityStatus()))
            ;
    }

    @Override
    public void toVO(GearUseFeatures source, GearUseFeaturesVO target, DataFetchOptions fetchOptions, boolean copyIfNull) {
        super.toVO(source, target, fetchOptions, copyIfNull);

        // Metier
        if (source.getMetier() != null) {
            target.setMetier(metierRepository.toVO(source.getMetier()));
        }

        // Gear
        if (source.getGear() != null) {
            target.setGear(referentialDao.toVO(source.getGear()));
        }
        if (source.getOtherGear() != null) {
            target.setOtherGear(referentialDao.toVO(source.getOtherGear()));
        }

        // Measurements
        if (fetchOptions != null && fetchOptions.isWithMeasurementValues()) {
            target.setMeasurementValues(measurementDao.getGearUseFeaturesMeasurementsMap(source.getId()));
        }

        // Fishing areas
        if (fetchOptions != null && fetchOptions.isWithChildrenEntities()) {
            target.setFishingAreas(fishingAreaRepository.getAllByGearUseFeaturesId(source.getId()));
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

    @Override
    public void toEntity(GearUseFeaturesVO source, GearUseFeatures target, boolean copyIfNull) {
        super.toEntity(source, target, copyIfNull);

        // Metier
        Integer metierId = source.getMetier() != null ? source.getMetier().getId() : null;
        if (copyIfNull || metierId != null) {
            if (metierId == null) {
                target.setMetier(null);
            }
            else {
                target.setMetier(getReference(Metier.class, metierId));
            }
        }

        // Gear
        Integer gearId = source.getGear() != null ? source.getGear().getId() : null;
        if (copyIfNull || gearId != null) {
            if (gearId == null) {
                target.setGear(null);
            }
            else {
                target.setGear(getReference(Gear.class, gearId));
            }
        }

        // Other gear
        Integer otherGearId = source.getOtherGear() != null ? source.getOtherGear().getId() : null;
        if (copyIfNull || otherGearId != null) {
            if (otherGearId == null) {
                target.setOtherGear(null);
            }
            else {
                target.setOtherGear(getReference(Gear.class, otherGearId));
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
    }

    @Override
    public List<GearUseFeaturesVO> saveAllByActivityCalendarId(int parentId, List<GearUseFeaturesVO> sources) {
        ActivityCalendar parent = getReference(ActivityCalendar.class, parentId);
        return this.saveAllByList(parent.getGearUseFeatures(), sources);
    }

    @Override
    public List<GearUseFeaturesVO> saveAllByDailyActivityCalendarId(int parentId, List<GearUseFeaturesVO> sources) {
        DailyActivityCalendar parent = getReference(DailyActivityCalendar.class, parentId);
        return this.saveAllByList(parent.getGearUseFeatures(), sources);
    }

    /* -- protected functions -- */

    @Override
    protected void onAfterSaveEntity(GearUseFeaturesVO vo, GearUseFeatures savedEntity, boolean isNew) {
        super.onAfterSaveEntity(vo, savedEntity, isNew);

        // Save measurements
        measurementDao.saveGearUseFeaturesMeasurementsMap(savedEntity.getId(), vo.getMeasurementValues());

        // Save fishing areas
        fishingAreaRepository.saveAllByGearUseFeaturesId(savedEntity.getId(), vo.getFishingAreas());
    }
}

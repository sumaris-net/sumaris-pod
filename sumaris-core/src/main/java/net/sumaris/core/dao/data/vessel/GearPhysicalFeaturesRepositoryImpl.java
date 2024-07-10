package net.sumaris.core.dao.data.vessel;

/*-
 * #%L
 * SUMARiS:: Core
 * %%
 * Copyright (C) 2024 SUMARiS Consortium
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

import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.administration.programStrategy.ProgramRepository;
import net.sumaris.core.dao.data.trip.TripRepository;
import net.sumaris.core.dao.referential.ReferentialDao;
import net.sumaris.core.dao.referential.metier.MetierRepository;
import net.sumaris.core.model.administration.programStrategy.AcquisitionLevel;
import net.sumaris.core.model.administration.programStrategy.Program;
import net.sumaris.core.model.data.*;
import net.sumaris.core.model.referential.gear.Gear;
import net.sumaris.core.model.referential.metier.Metier;
import net.sumaris.core.util.Beans;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.data.DataFetchOptions;
import net.sumaris.core.vo.data.DataOriginVO;
import net.sumaris.core.vo.data.GearPhysicalFeaturesVO;
import net.sumaris.core.vo.filter.GearPhysicalFeaturesFilterVO;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.EntityManager;
import java.util.*;

@Slf4j
public class GearPhysicalFeaturesRepositoryImpl
        extends UseFeaturesRepositoryImpl<GearPhysicalFeatures, GearPhysicalFeaturesVO, GearPhysicalFeaturesFilterVO, DataFetchOptions>
        implements GearPhysicalFeaturesSpecifications
{

    @Autowired
    private MetierRepository metierRepository;

    @Autowired
    private ReferentialDao referentialDao;

    @Autowired
    private ProgramRepository programRepository;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    public GearPhysicalFeaturesRepositoryImpl(EntityManager entityManager
    ) {
        super(GearPhysicalFeatures.class, GearPhysicalFeaturesVO.class, entityManager);
    }

    public Specification<GearPhysicalFeatures> toSpecification(GearPhysicalFeaturesFilterVO filter, DataFetchOptions fetchOptions) {
        return super.toSpecification(filter, fetchOptions)
            // ID
            .and(id(filter.getGearPhysicalFeaturesId(), Integer.class))
            .and(excludedIds(filter.getExcludedIds()))
            .and(includedIds(filter.getIncludedIds()))
            // Program
            .and(hasProgramLabel(filter.getProgramLabel()))
            .and(hasProgramIds(filter.getProgramIds()))
            // Vessel
            .and(hasVesselIds(concat(filter.getVesselId(), filter.getVesselIds())))
            // Parent
            .and(hasActivityCalendarId(filter.getActivityCalendarId()))
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
    public void toVO(GearPhysicalFeatures source, GearPhysicalFeaturesVO target, DataFetchOptions fetchOptions, boolean copyIfNull) {
        super.toVO(source, target, fetchOptions, copyIfNull);

        // Metier
        if (source.getMetier() != null) {
            target.setMetier(metierRepository.toVO(source.getMetier()));
        }

        // Gear
        if (source.getGear() != null) {
            target.setGear(referentialDao.toVO(source.getGear()));
        }

        // Other Gear
        if (source.getOtherGear() != null) {
            target.setOtherGear(referentialDao.toVO(source.getOtherGear()));
        }

        // Measurements
        if (fetchOptions != null && fetchOptions.isWithMeasurementValues()) {
            target.setMeasurementValues(measurementDao.getGearPhysicalFeaturesMeasurementsMap(source.getId()));
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

        // Children entities
        if (fetchOptions != null && fetchOptions.isWithChildrenEntities()) {

            // Origins
            target.setDataOrigins(toOriginVOs(source.getOrigins()));
        }

    }

    @Override
    public boolean toEntity(GearPhysicalFeaturesVO source, GearPhysicalFeatures target, boolean copyIfNull, boolean allowSkipSameHash) {
        boolean sameHash = super.toEntity(source, target, copyIfNull, allowSkipSameHash);
        if (sameHash) return true;

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

        return false;
    }

    @Override
    public List<GearPhysicalFeaturesVO> saveAllByActivityCalendarId(int parentId, @NonNull List<GearPhysicalFeaturesVO> sources) {
        ActivityCalendar parent = getById(ActivityCalendar.class, parentId);
        sources.forEach(source -> source.setActivityCalendarId(parentId));
        boolean dirty = this.saveAllByList(parent.getGearPhysicalFeatures(), sources);
        return sources;
    }

    /* -- protected functions -- */

    @Override
    protected void onBeforeSaveEntity(GearPhysicalFeaturesVO source, GearPhysicalFeatures target, boolean isNew) {
        super.onBeforeSaveEntity(source, target, isNew);

    }

    @Override
    protected void onAfterSaveEntity(GearPhysicalFeaturesVO vo, GearPhysicalFeatures savedEntity, boolean isNew) {
        super.onAfterSaveEntity(vo, savedEntity, isNew);

        // Save origins
        saveAllOrigins(vo.getDataOrigins(), savedEntity);

        // Save measurements
        measurementDao.saveGearPhysicalFeaturesMeasurementsMap(savedEntity.getId(), vo.getMeasurementValues());

    }

    protected List<DataOriginVO> toOriginVOs(List<GearPhysicalFeaturesOrigin> sources) {
        return Beans.getStream(sources).map(this::toOriginVO).toList();
    }

    protected DataOriginVO toOriginVO(GearPhysicalFeaturesOrigin source) {
        DataOriginVO target = new DataOriginVO();
        target.setProgramId(source.getProgram().getId());
        if (target.getProgramId() != null) {
            target.setProgram(programRepository.get(target.getProgramId()));
        }

        if (source.getAcquisitionLevel() != null) {
            target.setAcquisitionLevel(referentialDao.getAcquisitionLevelLabelById(source.getAcquisitionLevel().getId()));
        }

        target.setVesselUseFeaturesId(null);
        target.setGearUseFeaturesId(null);
        target.setGearPhysicalFeaturesId(source.getGearPhysicalFeatures().getId());

        return target;
    }

    protected List<DataOriginVO> saveAllOrigins(List<DataOriginVO> sources, GearPhysicalFeatures parent) {

        EntityManager em = getEntityManager();
        if (parent.getOrigins() == null) {
            parent.setOrigins(Lists.newArrayList());
        }

        ListMultimap<Integer, GearPhysicalFeaturesOrigin> existingByProgramId = Beans.splitByNotUniqueProperty(parent.getOrigins(), StringUtils.doting(GearPhysicalFeaturesOrigin.Fields.PROGRAM,  Program.Fields.ID), -1);
        final List<GearPhysicalFeaturesOrigin> targets = parent.getOrigins();

        Beans.getStream(sources)
            .forEach(source -> {
                Integer programId = source.getProgramId() != null ? source.getProgramId() :
                    (source.getProgram() != null ? source.getProgram().getId() : null);

                if (programId == null || programId < 0) return; // Skip if no program

                source.setGearPhysicalFeaturesId(parent.getId());
                source.setVesselUseFeaturesId(null);
                source.setGearUseFeaturesId(null);
                source.setProgramId(programId);
                if (source.getProgram() != null) {
                    source.setProgram(programRepository.get(programId));
                }

                // Check if exists
                GearPhysicalFeaturesOrigin target = existingByProgramId.containsKey(programId) ? existingByProgramId.get(programId).get(0) : null;
                boolean isNew = target == null;

                if (isNew) {
                    target = new GearPhysicalFeaturesOrigin();
                    target.setGearPhysicalFeatures(parent);
                    target.setProgram(getReference(Program.class, programId));
                    targets.add(target);
                }
                else {
                    existingByProgramId.remove(programId, target);
                }

                // Acquisition level
                if (StringUtils.isNotBlank(source.getAcquisitionLevel())) {
                    target.setAcquisitionLevel(getReference(AcquisitionLevel.class, referentialDao.getAcquisitionLevelIdByLabel(source.getAcquisitionLevel())));
                }
                else {
                    target.setAcquisitionLevel(null);
                }

                if (isNew) {
                    em.persist(target);
                }
                else {
                    em.merge(target);
                }

            });

        // Remove unused existing origins
        Collection<GearPhysicalFeaturesOrigin> entitiesToRemove = existingByProgramId.values();
        if (CollectionUtils.isNotEmpty(entitiesToRemove)) {
            targets.removeAll(entitiesToRemove);
            entitiesToRemove.forEach(em::remove);
        }

        return sources;
    }
}

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
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.data.MeasurementDao;
import net.sumaris.core.dao.data.RootDataRepositoryImpl;
import net.sumaris.core.dao.referential.ReferentialDao;
import net.sumaris.core.dao.technical.Daos;
import net.sumaris.core.event.config.ConfigurationReadyEvent;
import net.sumaris.core.event.config.ConfigurationUpdatedEvent;
import net.sumaris.core.model.data.IWithGearsEntity;
import net.sumaris.core.model.data.PhysicalGear;
import net.sumaris.core.model.data.Trip;
import net.sumaris.core.model.referential.gear.Gear;
import net.sumaris.core.util.Beans;
import net.sumaris.core.util.TimeUtils;
import net.sumaris.core.vo.administration.programStrategy.ProgramVO;
import net.sumaris.core.vo.data.DataFetchOptions;
import net.sumaris.core.vo.data.PhysicalGearVO;
import net.sumaris.core.vo.filter.PhysicalGearFilterVO;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.EntityManager;
import java.util.*;
import java.util.stream.Stream;

@Slf4j
public class PhysicalGearRepositoryImpl
    extends RootDataRepositoryImpl<PhysicalGear, PhysicalGearVO, PhysicalGearFilterVO, DataFetchOptions>
    implements PhysicalGearSpecifications {

    private final ReferentialDao referentialDao;
    private final MeasurementDao measurementDao;

    private boolean enableSaveUsingHash;

    @Autowired
    public PhysicalGearRepositoryImpl(EntityManager entityManager,
                                      MeasurementDao measurementDao,
                                      ReferentialDao referentialDao) {
        super(PhysicalGear.class, PhysicalGearVO.class, entityManager);
        this.measurementDao = measurementDao;
        this.referentialDao = referentialDao;

        // TODO: to remove after test
        //setCheckUpdateDate(false);
    }

    @EventListener({ConfigurationReadyEvent.class, ConfigurationUpdatedEvent.class})
    public void onConfigurationReady() {
        this.enableSaveUsingHash = getConfig().enablePhysicalGearHashOptimization();
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

        // Parent physical gear
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

        // Fetch measurement values
        Integer physicalGearId = source.getId();
        if (fetchOptions != null && fetchOptions.isWithMeasurementValues() && physicalGearId != null) {
            target.setMeasurementValues(measurementDao.getPhysicalGearMeasurementsMap(physicalGearId));
        }
    }

    @Override
    public void toEntity(PhysicalGearVO source, PhysicalGear target, boolean copyIfNull) {
        toEntity(source, target, copyIfNull, target.getId() != null && enableSaveUsingHash);
    }

    protected boolean toEntity(PhysicalGearVO source, PhysicalGear target, boolean copyIfNull, boolean allowSkipSameHash) {
        Preconditions.checkNotNull(source);
        Preconditions.checkNotNull(source.getGear(), "Missing gear");
        Preconditions.checkNotNull(source.getGear().getId(), "Missing gear.id");

        // Copy some fields from parent
        if (source.getParent() != null) {
            source.setProgram(source.getParent().getProgram());
            source.setRecorderDepartment(source.getParent().getRecorderDepartment());
            source.setRecorderPerson(source.getParent().getRecorderPerson());
        }

        // Parent
        Integer parentId = source.getParent() != null ? source.getParent().getId() : source.getParentId();
        Integer tripId = source.getTripId() != null ? source.getTripId() : (source.getTrip() != null ? source.getTrip().getId() : null);
        if (copyIfNull || parentId != null) {

            // Check if parent changed
            PhysicalGear previousParent = target.getParent();
            if (previousParent != null && !Objects.equals(parentId, previousParent.getId()) && CollectionUtils.isNotEmpty(previousParent.getChildren())) {
                // Remove in the parent children list (to avoid a DELETE CASCADE if the parent is delete later - fix #15)
                previousParent.getChildren().remove(target);
            }

            if (parentId == null) {
                target.setParent(null);
            } else {
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

        Integer newHash = source.hashCode();

        // If same hash, then skip (if allow)
        if (allowSkipSameHash && Objects.equals(target.getHash(), newHash)) {
            return true; // Skip
        }

        // Copy properties, and data stuff (program, qualityFlag, recorder, ...)
        super.toEntity(source, target, copyIfNull);

        // Set the new Hash
        target.setHash(newHash);

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

        return false;
    }

    public List<PhysicalGearVO> saveAllByTripId(final int tripId, final List<PhysicalGearVO> sources) {
        return saveAllByTripId(tripId, sources, null);
    }

    public List<PhysicalGearVO> saveAllByTripId(final int tripId,
                                                final List<PhysicalGearVO> sources,
                                                List<Integer> idsToRemoveLater) {

        long debugTime = log.isDebugEnabled() ? System.currentTimeMillis() : 0L;
        if (debugTime != 0L)
            log.debug(String.format("Saving trip {id:%s} physical gears... {hash_optimization:%s}", tripId, enableSaveUsingHash));

        // Load parent entity
        Trip parent = getById(Trip.class, tripId);
        ProgramVO parentProgram = new ProgramVO();
        parentProgram.setId(parent.getProgram().getId());

        sources.forEach(sample -> {
            sample.setTripId(tripId);
            sample.setProgram(parentProgram);
        });


        // Save all, by parent
        boolean dirty = saveAllByParent(parent, sources, idsToRemoveLater);

        if (dirty) {
            getEntityManager().flush();
            getEntityManager().clear();
        }

        if (debugTime != 0L)
            log.debug("Saving trip {id:{}} physical gears [OK] in {}", tripId, TimeUtils.printDurationFrom(debugTime));

        return sources;
    }

    /* -- protected methods -- */


    protected boolean saveAllByParent(IWithGearsEntity<Integer, PhysicalGear> parent,
                                      List<PhysicalGearVO> sources,
                                      List<Integer> idsToRemoveLater) {
        final boolean trace = log.isTraceEnabled();

        // Load existing entities
        final Map<Integer, PhysicalGear> sourcesByIds = Beans.splitById(Beans.getList(parent.getGears()));
        final Set<Integer> sourcesIdsToSkip = Sets.newHashSet();

        // Get current update date
        Date newUpdateDate = getDatabaseCurrentDate();

        // Save each sources
        long updatesCount = sources.stream().map(source -> {
            PhysicalGear target = null;
            if (source.getId() != null) {
                target = sourcesByIds.remove(source.getId());
            }
            // Check can be skipped
            boolean skip = enableSaveUsingHash && source.getId() != null && sourcesIdsToSkip.contains(source.getId());
            if (!skip) {
                source = optimizedSave(source, target, false, newUpdateDate, enableSaveUsingHash);
                skip = !Objects.equals(source.getUpdateDate(), newUpdateDate);

                // If not changed, skip all children
                if (skip) {
                    streamRecursiveChildren(source)
                        .map(PhysicalGearVO::getId)
                        .forEach(sourcesIdsToSkip::add);
                }
            }
            if (skip && trace) {
                log.trace("Skip save {}", source);
            }
            return !skip;
        })
        // Count updates
        .filter(Boolean::booleanValue).count();

        boolean dirty = updatesCount > 0;

        // Remove unused entities
        if (!sourcesByIds.isEmpty()) {
            // Deletion can be done later, if a list has been given in arguments
            // In this case, we simply add items to this list
            if (idsToRemoveLater != null) {
                idsToRemoveLater.addAll(sourcesByIds.keySet());
            }
            else {
                sourcesByIds.keySet().forEach(sampleId -> {
                    try {
                        this.deleteById(sampleId);
                    } catch (EmptyResultDataAccessException e) {
                        // Continue (can occur because of delete cascade)
                    }
                });
                dirty = true;
            }
        }

        // Remove parent (use only parentId)
        sources.forEach(source -> {
            if (source.getParent() != null) {
                source.setParentId(source.getParent().getId());
                source.setParent(null);
            }
        });

        return dirty;
    }

    protected PhysicalGearVO optimizedSave(PhysicalGearVO source,
                                           PhysicalGear entity,
                                           boolean checkUpdateDate,
                                           Date newUpdateDate,
                                           boolean enableHashOptimization) {
        Preconditions.checkNotNull(source);
        EntityManager em = getEntityManager();

        if (entity == null && source.getId() != null) {
            entity = findById(source.getId()).orElse(null);
        }
        boolean isNew = (entity == null);
        if (isNew) {
            entity = new PhysicalGear();
            source.setId(null); // Make sure to not re-affect the ID
        }

        if (!isNew && checkUpdateDate) {
            // Check update date
            Daos.checkUpdateDateForUpdate(source, entity);

            // Lock entityName
            //lockForUpdate(entity);
        }

        onBeforeSaveEntity(source, entity, isNew);

        // VO -> Entity
        boolean skipSave = toEntity(source, entity, true, !isNew && enableHashOptimization);

        // Stop here (without change on the update_date)
        if (skipSave) return source;

        // Update update_dt
        entity.setUpdateDate(newUpdateDate);
        source.setUpdateDate(newUpdateDate);

        // Save entity
        if (isNew) {
            // Set creation date
            entity.setCreationDate(newUpdateDate);
            source.setCreationDate(newUpdateDate);

            // Add the new physicalGear
            em.persist(entity);
            source.setId(entity.getId());
            if (log.isTraceEnabled())
                log.trace("Adding {}...", entity);
        } else {

            // Workaround, to be sure to have a creation_date
            if (entity.getCreationDate() == null) {
                log.warn("Updating {} without creation_date!", entity);
                entity.setCreationDate(newUpdateDate);
                source.setCreationDate(newUpdateDate);
            }

            // Update existing physicalGear
            if (log.isTraceEnabled())
                log.trace("Updating {}...", entity);
            em.merge(entity);
        }

        return source;
    }


    protected Stream<PhysicalGearVO> streamRecursiveChildren(PhysicalGearVO source) {
        if (CollectionUtils.isEmpty(source.getChildren())) {
            return Stream.empty();
        }
        return source.getChildren().stream().flatMap(c -> Stream.concat(Stream.of(c), streamRecursiveChildren(c)));
    }

}

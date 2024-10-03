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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.administration.programStrategy.ProgramRepository;
import net.sumaris.core.dao.data.DataRepositoryImpl;
import net.sumaris.core.dao.data.MeasurementDao;
import net.sumaris.core.dao.technical.Daos;
import net.sumaris.core.event.config.ConfigurationReadyEvent;
import net.sumaris.core.event.config.ConfigurationUpdatedEvent;
import net.sumaris.core.model.administration.programStrategy.Program;
import net.sumaris.core.model.data.*;
import net.sumaris.core.util.Beans;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.ValueObjectFlags;
import net.sumaris.core.vo.administration.programStrategy.ProgramFetchOptions;
import net.sumaris.core.vo.data.IDataFetchOptions;
import net.sumaris.core.vo.data.IUseFeaturesVO;
import net.sumaris.core.vo.filter.IDataFilter;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.data.repository.NoRepositoryBean;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.criteria.*;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@NoRepositoryBean
@Slf4j
public abstract class UseFeaturesRepositoryImpl<E extends IUseFeaturesEntity, V extends IUseFeaturesVO, F extends IDataFilter, O extends IDataFetchOptions>
        extends DataRepositoryImpl<E, V, F, O>
        implements UseFeaturesSpecifications<E> {

    @Autowired
    private ProgramRepository programRepository;

    @Autowired
    private GenericConversionService conversionService;

    @Autowired
    protected MeasurementDao measurementDao;

    private boolean enableVesselRegistrationNaturalOrder;

    protected boolean enableHashOptimization = false;

    protected UseFeaturesRepositoryImpl(Class<E> domainClass, Class<V> voClass,
                                     EntityManager entityManager) {
        super(domainClass, voClass, entityManager);
    }

    @PostConstruct
    @EventListener({ConfigurationReadyEvent.class, ConfigurationUpdatedEvent.class})
    public void onConfigurationReady() {
        this.enableVesselRegistrationNaturalOrder = configuration.enableVesselRegistrationCodeNaturalOrder();
        conversionService.addConverter(getDomainClass(), getVOClass(), this::toVO);
        this.enableHashOptimization = getConfig().enableVesselUseFeaturesHashOptimization();
    }

    @Override
    public void toVO(E source, V target, O fetchOptions, boolean copyIfNull) {
        super.toVO(source, target, fetchOptions, copyIfNull);

        // Program
        if (source.getProgram() != null) {
            target.setProgram(programRepository.toVO(source.getProgram(), ProgramFetchOptions.MINIMAL));
        }

        // Vessel
        target.setVesselId(source.getVessel() != null ? source.getVessel().getId() : null);
    }

    public void toEntity(V source, E target, boolean copyIfNull) {
        toEntity(source, target, copyIfNull, target.getId() != null && enableHashOptimization);
    }

    /**
     *
     * @param source
     * @param target
     * @param copyIfNull
     * @param allowSkipSameHash
     * @return true if same hash
     */
    public boolean toEntity(V source, E target, boolean copyIfNull, boolean allowSkipSameHash) {
        // Compute source hash
        Integer newHash = source.hashCode();

        // If same hash, then skip (if allow)
        if (allowSkipSameHash && Objects.equals(target.getHash(), newHash)) {
            return true; // Same hash
        }

        super.toEntity(source, target, copyIfNull);

        // Update hash
        target.setHash(newHash);

        // Program
        Integer programId = source.getProgram() != null ? source.getProgram().getId() : null;
        if (programId != null || copyIfNull) {
            if (programId == null) {
                target.setProgram(null);
            }
            else {
                target.setProgram(getReference(Program.class, programId));
            }
        }

        // Vessel
        if (source.getVesselId() != null || copyIfNull) {
            if (source.getVesselId() == null) {
                target.setVessel(null);
            }
            else {
                target.setVessel(getReference(Vessel.class, source.getVesselId()));
            }
        }

        return false;
    }



    /* -- protected functions -- */

    @Override
    protected String toEntityProperty(@NonNull String property) {
        if (E.Fields.VESSEL.equalsIgnoreCase(property) || property.endsWith(VesselRegistrationPeriod.Fields.REGISTRATION_CODE)) {
            return StringUtils.doting(VesselUseFeatures.Fields.VESSEL, Vessel.Fields.VESSEL_REGISTRATION_PERIODS, VesselRegistrationPeriod.Fields.REGISTRATION_CODE);
        }
        if (property.endsWith(VesselRegistrationPeriod.Fields.INT_REGISTRATION_CODE)) {
            return StringUtils.doting(VesselUseFeatures.Fields.VESSEL, Vessel.Fields.VESSEL_REGISTRATION_PERIODS, VesselRegistrationPeriod.Fields.INT_REGISTRATION_CODE);
        }
        if (property.endsWith(VesselFeatures.Fields.EXTERIOR_MARKING)) {
            return StringUtils.doting(VesselUseFeatures.Fields.VESSEL, Vessel.Fields.VESSEL_FEATURES, VesselFeatures.Fields.EXTERIOR_MARKING);
        }
        if (property.endsWith(VesselFeatures.Fields.NAME)) {
            return StringUtils.doting(VesselUseFeatures.Fields.VESSEL, Vessel.Fields.VESSEL_FEATURES, VesselFeatures.Fields.NAME);
        }
        return super.toEntityProperty(property);
    }

    @Override
    protected List<Expression<?>> toSortExpressions(CriteriaQuery<?> query, Root<E> root, CriteriaBuilder cb, String property) {

        Expression<?> expression = null;

        // Add left join on vessel registration period (VRP)
        if (property.endsWith(VesselRegistrationPeriod.Fields.REGISTRATION_CODE)
            || property.endsWith(VesselRegistrationPeriod.Fields.INT_REGISTRATION_CODE)) {

            ListJoin<Vessel, VesselRegistrationPeriod> vrp = composeVrpJoin(root, cb);
            expression = vrp.get(property.endsWith(VesselRegistrationPeriod.Fields.REGISTRATION_CODE)
                ? VesselRegistrationPeriod.Fields.REGISTRATION_CODE
                : VesselRegistrationPeriod.Fields.INT_REGISTRATION_CODE);
            // Natural sort
            if (enableVesselRegistrationNaturalOrder) {
                expression = Daos.naturalSort(cb, expression);
            }
        }

        // Add left join on vessel features (VF)
        if (property.endsWith(VesselFeatures.Fields.NAME)
            || property.endsWith(VesselFeatures.Fields.EXTERIOR_MARKING)) {
            ListJoin<Vessel, VesselFeatures> vf = composeVfJoin(root, cb);
            expression = vf.get(property.endsWith(VesselFeatures.Fields.EXTERIOR_MARKING)
                ? VesselFeatures.Fields.EXTERIOR_MARKING
                : VesselFeatures.Fields.NAME);

            // Natural sort on exterior marking
            if (enableVesselRegistrationNaturalOrder && property.endsWith(VesselFeatures.Fields.EXTERIOR_MARKING)) {
                expression = Daos.naturalSort(cb, expression);
            };
        }

        return (expression != null) ? ImmutableList.of(expression) : super.toSortExpressions(query, root, cb, property);
    }

    @Override
    protected void onBeforeSaveEntity(V source, E target, boolean isNew) {
        super.onBeforeSaveEntity(source, target, isNew);

        // When new entity: set the creation date
        if (isNew || target.getCreationDate() == null) {
            target.setCreationDate(target.getUpdateDate());
        }
    }

    @Override
    protected void onAfterSaveEntity(V vo, E savedEntity, boolean isNew) {
        super.onAfterSaveEntity(vo, savedEntity, isNew);

        if (isNew) {
            vo.setCreationDate(savedEntity.getCreationDate());
        }

    }

    protected boolean saveAllByList(List<E> targets,
                                    List<V> sources) {
        final boolean trace = log.isTraceEnabled();
        List<Integer> remoteIds = Beans.collectIds(targets);
        List<Integer> sourceIds = Beans.collectIds(sources);
        List<Integer> remoteIdsToDelete =  remoteIds.stream().filter((remoteId) -> !sourceIds.contains(remoteId)).toList();
        boolean dirty = false;
        
        if (CollectionUtils.isNotEmpty(remoteIdsToDelete)) {
            this.deleteAllById(remoteIdsToDelete);
            dirty = true;
        }
        // Get current update date
        Date newUpdateDate = getDatabaseCurrentDate();

        long updatesCount = sources.stream().map((source) -> {
            boolean isNew = source.getId() == null;
            if (!isNew) {
                remoteIds.remove(source.getId());
            }

            // TODO Reuse existing entity from target
            V savedEntity = optimizedSave(source, false, newUpdateDate);
            boolean skip = source.hasFlag(ValueObjectFlags.SAME_HASH); // !Objects.equals(source.getUpdateDate(), newUpdateDate);

            if (skip && trace) {
                log.trace("Skip save {}", source);
            }

            return !skip;
        })
        // Count updates
        .filter(Boolean::booleanValue).count();

         dirty = dirty || updatesCount > 0;

        return dirty;
    }

    protected V optimizedSave(V source,
                              boolean checkUpdateDate,
                              Date newUpdateDate) {
        Preconditions.checkNotNull(source);
        EntityManager em = getEntityManager();

        E entity = null;
        if (source.getId() != null) {
            entity = findById(source.getId()).orElse(null);
        }

        boolean isNew = (entity == null);
        if (isNew) {
            entity = createEntity();
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
        boolean sameHash = toEntity(source, entity, true, !isNew && enableHashOptimization);

        if (sameHash) {
            // Flag as same hash
            source.addFlag(ValueObjectFlags.SAME_HASH);

            // Stop here (without change on the update_date)
            return source;
        }

        // Remove same hash flag
        source.removeFlag(ValueObjectFlags.SAME_HASH);

        // Update update_dt
        entity.setUpdateDate(newUpdateDate);
        source.setUpdateDate(newUpdateDate);

        // Save entity
        if (isNew) {
            // Set creation date
            entity.setCreationDate(newUpdateDate);
        }

        // Workaround, to be sure to have a creation_date
        else if (entity.getCreationDate() == null) {
            log.warn("Updating {} without creation_date!", entity);
            entity.setCreationDate(newUpdateDate);
            source.setCreationDate(newUpdateDate);
        }

        E savedEntity = save(entity);

        // Update VO
        onAfterSaveEntity(source, savedEntity, isNew);

        if (isPublishEvent()) publishSaveEvent(source, isNew);

        return source;
    }
}

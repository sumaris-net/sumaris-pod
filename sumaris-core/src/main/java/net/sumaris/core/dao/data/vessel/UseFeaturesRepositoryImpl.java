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
import java.util.List;

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

    protected UseFeaturesRepositoryImpl(Class<E> domainClass, Class<V> voClass,
                                     EntityManager entityManager) {
        super(domainClass, voClass, entityManager);
    }

    @PostConstruct
    @EventListener({ConfigurationReadyEvent.class, ConfigurationUpdatedEvent.class})
    public void onConfigurationReady() {
        this.enableVesselRegistrationNaturalOrder = configuration.enableVesselRegistrationCodeNaturalOrder();
        conversionService.addConverter(getDomainClass(), getVOClass(), this::toVO);
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

    @Override
    public void toEntity(V source, E target, boolean copyIfNull) {
        super.toEntity(source, target, copyIfNull);

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

    protected List<V> saveAllByList(List<E> targets,
                                    List<V> sources) {

        List<Integer> remoteIds = Beans.collectIds(targets);

        List<V> savedTargets = sources.stream().map(vuf -> {
            boolean isNew = vuf.getId() == null;
            if (!isNew) remoteIds.remove(vuf.getId());
            return save(vuf);
        }).toList();

        if (CollectionUtils.isNotEmpty(remoteIds)) {
            this.deleteAllById(remoteIds);
        }

        return savedTargets;
    }
}

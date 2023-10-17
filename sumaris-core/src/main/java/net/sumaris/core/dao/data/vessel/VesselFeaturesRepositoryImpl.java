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
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.data.DataDaos;
import net.sumaris.core.dao.data.DataRepositoryImpl;
import net.sumaris.core.dao.referential.ReferentialDao;
import net.sumaris.core.dao.referential.location.LocationRepository;
import net.sumaris.core.dao.technical.Daos;
import net.sumaris.core.event.config.ConfigurationEvent;
import net.sumaris.core.event.config.ConfigurationReadyEvent;
import net.sumaris.core.event.config.ConfigurationUpdatedEvent;
import net.sumaris.core.model.data.Vessel;
import net.sumaris.core.model.data.VesselFeatures;
import net.sumaris.core.model.data.VesselRegistrationPeriod;
import net.sumaris.core.model.referential.QualityFlag;
import net.sumaris.core.model.referential.QualityFlagEnum;
import net.sumaris.core.model.referential.location.Location;
import net.sumaris.core.model.referential.pmfm.QualitativeValue;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.data.DataFetchOptions;
import net.sumaris.core.vo.data.VesselFeaturesVO;
import net.sumaris.core.vo.filter.VesselFilterVO;
import net.sumaris.core.vo.referential.LocationVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.EntityManager;
import javax.persistence.criteria.*;
import java.util.List;

@Slf4j
public class VesselFeaturesRepositoryImpl
        extends DataRepositoryImpl<VesselFeatures, VesselFeaturesVO, VesselFilterVO, DataFetchOptions>
        implements VesselFeaturesSpecifications<VesselFeatures, VesselFeaturesVO, VesselFilterVO, DataFetchOptions> {

    private final ReferentialDao referentialDao;
    private final LocationRepository locationRepository;
    private boolean enableRegistrationCodeSearchAsPrefix;
    private boolean enableVesselRegistrationNaturalOrder;

    @Autowired
    public VesselFeaturesRepositoryImpl(EntityManager entityManager,
                                        ReferentialDao referentialDao,
                                        LocationRepository locationRepository) {
        super(VesselFeatures.class, VesselFeaturesVO.class, entityManager);
        this.referentialDao = referentialDao;
        this.locationRepository = locationRepository;
    }


    @EventListener({ConfigurationReadyEvent.class, ConfigurationUpdatedEvent.class})
    public void onConfigurationReady(ConfigurationEvent event) {
        this.enableRegistrationCodeSearchAsPrefix = event.getConfiguration().enableVesselRegistrationCodeSearchAsPrefix();
        this.enableVesselRegistrationNaturalOrder = event.getConfiguration().enableVesselRegistrationCodeNaturalOrder();
    }

    @Override
    public boolean enableRegistrationCodeSearchAsPrefix() {
        return enableRegistrationCodeSearchAsPrefix;
    }

    @Override
    public Specification<VesselFeatures> toSpecification(VesselFilterVO filter, DataFetchOptions fetchOptions) {
        return super.toSpecification(filter, fetchOptions)
                .and(vesselId(filter.getVesselId()))
                .and(betweenFeaturesDate(filter.getStartDate(), filter.getEndDate()))
                ;
    }

    @Override
    public void toVO(VesselFeatures source, VesselFeaturesVO target, DataFetchOptions fetchOptions, boolean copyIfNull) {
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

        target.setQualityFlagId(source.getQualityFlag().getId());

        if (source.getHullMaterial() != null) {
            ReferentialVO hullMaterial = referentialDao.toVO(source.getHullMaterial());
            target.setHullMaterial(hullMaterial);
        }
        else {
            target.setHullMaterial(null);
        }

        // Base port location
        LocationVO basePortLocation = locationRepository.toVO(source.getBasePortLocation());
        target.setBasePortLocation(basePortLocation);

        // Recorder department
        DepartmentVO recorderDepartment = referentialDao.toTypedVO(source.getRecorderDepartment(), DepartmentVO.class).orElse(null);
        target.setRecorderDepartment(recorderDepartment);
    }

    @Override
    public void toEntity(VesselFeaturesVO source, VesselFeatures target, boolean copyIfNull) {
        super.toEntity(source, target, copyIfNull);

        // Recorder department and person
        DataDaos.copyRecorderPerson(getEntityManager(), source, target, copyIfNull);

        // Convert from meter to centimeter
        if (source.getLengthOverAll() != null) {
            target.setLengthOverAll((int) (source.getLengthOverAll() * 100));
        }
        // Convert tonnage (x100)
        if (source.getGrossTonnageGrt() != null) {
            target.setGrossTonnageGrt((int) (source.getGrossTonnageGrt() * 100));
        }
        if (source.getGrossTonnageGt() != null) {
            target.setGrossTonnageGt((int) (source.getGrossTonnageGt() * 100));
        }

        // Hull material
        if (copyIfNull || source.getHullMaterial() != null) {
            if (source.getHullMaterial() == null || source.getHullMaterial().getId() == null) {
                target.setHullMaterial(null);
            } else {
                target.setHullMaterial(getReference(QualitativeValue.class, source.getHullMaterial().getId()));
            }
        }

        // Base port location
        if (copyIfNull || source.getBasePortLocation() != null) {
            if (source.getBasePortLocation() == null || source.getBasePortLocation().getId() == null) {
                target.setBasePortLocation(null);
            } else {
                target.setBasePortLocation(getReference(Location.class, source.getBasePortLocation().getId()));
            }
        }

        // Quality flag
        if (copyIfNull || source.getQualityFlagId() != null) {
            if (source.getQualityFlagId() == null) {
                target.setQualityFlag(null);
            } else {
                target.setQualityFlag(getReference(QualityFlag.class, source.getQualityFlagId()));
            }
        } else if (copyIfNull) {
            // Set default
            target.setQualityFlag(getReference(QualityFlag.class, QualityFlagEnum.NOT_QUALIFIED.getId()));
        }

        // Vessel
        if (copyIfNull || (source.getVessel() != null)) {
            if (source.getVessel() == null) {
                target.setVessel(null);
            } else {
                target.setVessel(getReference(Vessel.class, source.getVessel().getId()));
            }
        }
    }

    @Override
    protected List<Expression<?>> toSortExpressions(CriteriaQuery<?> query, Root<VesselFeatures> root, CriteriaBuilder cb, String property) {

        Expression<?> expression = null;

        if (enableVesselRegistrationNaturalOrder) {
            // Add left join on vessel registration period (VRP)
            if (property.endsWith(VesselRegistrationPeriod.Fields.REGISTRATION_CODE)
                || property.endsWith(VesselRegistrationPeriod.Fields.INT_REGISTRATION_CODE)) {

                ListJoin<Vessel, VesselRegistrationPeriod> vrp = composeVrpJoin(root, cb);
                expression = vrp.get(property.endsWith(VesselRegistrationPeriod.Fields.REGISTRATION_CODE)
                    ? VesselRegistrationPeriod.Fields.REGISTRATION_CODE
                    : VesselRegistrationPeriod.Fields.INT_REGISTRATION_CODE);

                expression = Daos.naturalSort(cb, expression);
            }

            // Add left join on vessel features (VF)
            // Natural sort on exterior marking
            if (property.endsWith(VesselFeatures.Fields.EXTERIOR_MARKING)) {
                expression = Daos.naturalSort(cb, expression);
            }
        }

        return (expression != null) ? ImmutableList.of(expression) : super.toSortExpressions(query, root, cb, property);
    }

    @Override
    protected void onBeforeSaveEntity(VesselFeaturesVO source, VesselFeatures target, boolean isNew) {
        super.onBeforeSaveEntity(source, target, isNew);

        // When new entity: set the creation date
        if (isNew || target.getCreationDate() == null) {
            target.setCreationDate(target.getUpdateDate());
        }
    }

    @Override
    protected void onAfterSaveEntity(VesselFeaturesVO vo, VesselFeatures savedEntity, boolean isNew) {
        super.onAfterSaveEntity(vo, savedEntity, isNew);

        if (isNew) {
            vo.setCreationDate(savedEntity.getCreationDate());
        }

    }
}

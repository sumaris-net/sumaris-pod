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
import net.sumaris.core.model.referential.QualityFlag;
import net.sumaris.core.model.referential.QualityFlagEnum;
import net.sumaris.core.model.referential.location.Location;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.data.DataFetchOptions;
import net.sumaris.core.vo.data.VesselFeaturesVO;
import net.sumaris.core.vo.filter.VesselFilterVO;
import net.sumaris.core.vo.referential.LocationVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.data.jpa.domain.Specification;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;

@Slf4j
public class VesselFeaturesRepositoryImpl
        extends DataRepositoryImpl<VesselFeatures, VesselFeaturesVO, VesselFilterVO, DataFetchOptions>
        implements VesselFeaturesSpecifications<VesselFeatures, VesselFeaturesVO, VesselFilterVO, DataFetchOptions> {

    private final ReferentialDao referentialDao;
    private final LocationRepository locationRepository;
    private final SumarisConfiguration configuration;
    private boolean isOracleDatabase;
    private boolean enableRegistrationCodeSearchAsPrefix;

    @Autowired
    public VesselFeaturesRepositoryImpl(EntityManager entityManager,
                                        ReferentialDao referentialDao,
                                        LocationRepository locationRepository,
                                        SumarisConfiguration configuration) {
        super(VesselFeatures.class, VesselFeaturesVO.class, entityManager);
        this.referentialDao = referentialDao;
        this.locationRepository = locationRepository;
        this.configuration = configuration;
    }

    @PostConstruct
    private void setup() {
        isOracleDatabase = Daos.isOracleDatabase(configuration.getJdbcURL());
    }

    @EventListener({ConfigurationReadyEvent.class, ConfigurationUpdatedEvent.class})
    public void onConfigurationReady(ConfigurationEvent event) {
        enableRegistrationCodeSearchAsPrefix = event.getConfiguration().enableVesselRegistrationCodeSearchAsPrefix();
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

    @Override
    public boolean isOracleDatabase() {
        return isOracleDatabase;
    }
}

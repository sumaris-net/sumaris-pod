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

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.config.SumarisConfiguration;
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
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.data.VesselSnapshotVO;
import net.sumaris.core.vo.data.vessel.VesselFetchOptions;
import net.sumaris.core.vo.filter.VesselFilterVO;
import net.sumaris.core.vo.referential.LocationVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class VesselSnapshotRepositoryImpl
    extends DataRepositoryImpl<VesselFeatures, VesselSnapshotVO, VesselFilterVO, VesselFetchOptions>
    implements VesselSnapshotSpecifications {

    private final VesselRegistrationPeriodRepository vesselRegistrationPeriodRepository;
    private final LocationRepository locationRepository;
    private final ReferentialDao referentialDao;
    private final SumarisConfiguration configuration;

    protected boolean enableRegistrationCodeSearchAsPrefix;
    protected boolean enableAdagioOptimization;
    protected String adagioSchema;

    @Autowired
    public VesselSnapshotRepositoryImpl(EntityManager entityManager,
                                        VesselRegistrationPeriodRepository vesselRegistrationPeriodRepository,
                                        LocationRepository locationRepository,
                                        ReferentialDao referentialDao,
                                        SumarisConfiguration configuration) {
        super(VesselFeatures.class, VesselSnapshotVO.class, entityManager);
        this.vesselRegistrationPeriodRepository = vesselRegistrationPeriodRepository;
        this.locationRepository = locationRepository;
        this.referentialDao = referentialDao;
        this.configuration = configuration;
    }

    @PostConstruct
    @EventListener({ConfigurationReadyEvent.class, ConfigurationUpdatedEvent.class})
    public void onConfigurationReady() {
        this.enableRegistrationCodeSearchAsPrefix = configuration.enableVesselRegistrationCodeSearchAsPrefix();
        this.adagioSchema = configuration.getAdagioSchema();
        this.enableAdagioOptimization = configuration.enableAdagioOptimization() && StringUtils.isNotBlank(adagioSchema);
    }

    @Override
    public boolean enableRegistrationCodeSearchAsPrefix() {
        return enableRegistrationCodeSearchAsPrefix;
    }

    @Override
    public Page<VesselSnapshotVO> findAll(@NonNull VesselFilterVO filter,
                                          @NonNull Pageable pageable,
                                          @NonNull VesselFetchOptions fetchOptions) {

        CriteriaBuilder cb = this.getEntityManager().getCriteriaBuilder();
        CriteriaQuery<Tuple> criteriaQuery = cb.createTupleQuery();

        Root<VesselFeatures> root = criteriaQuery.from(VesselFeatures.class);
        Join<VesselFeatures, Vessel> vessel = Daos.composeJoin(root, VesselFeatures.Fields.VESSEL, JoinType.INNER);
        ListJoin<Vessel, VesselRegistrationPeriod> vrp = composeVrpJoin(root, cb);

        criteriaQuery.multiselect(root, vessel, vrp)
            .distinct(true);

        // Apply specification
        Specification<VesselFeatures> spec = toSpecification(filter, fetchOptions);
        Predicate predicate = spec.toPredicate(root, criteriaQuery, cb);
        if (predicate != null) criteriaQuery.where(predicate);

        // Add sorting
        addSorting(criteriaQuery, root, cb, pageable);

        TypedQuery<Tuple> query = getEntityManager().createQuery(criteriaQuery);

        // Bind parameters
        applyBindings(query, spec);

        // Configure (set hints)
        configureQuery(query, fetchOptions);

        return readPage(query, pageable, () -> count(spec))
            .map(tuple -> toVO(tuple, fetchOptions, true));

    }

    @Override
    public List<VesselSnapshotVO> findAll(@NonNull VesselFilterVO filter,
                                          @NonNull net.sumaris.core.dao.technical.Page page,
                                          @NonNull VesselFetchOptions fetchOptions) {

        CriteriaBuilder cb = this.getEntityManager().getCriteriaBuilder();
        CriteriaQuery<Tuple> criteriaQuery = cb.createTupleQuery();

        Root<VesselFeatures> root = criteriaQuery.from(VesselFeatures.class);
        Join<VesselFeatures, Vessel> vessel = Daos.composeJoin(root, VesselFeatures.Fields.VESSEL, JoinType.INNER);
        ListJoin<Vessel, VesselRegistrationPeriod> vrp = composeVrpJoin(vessel, cb, null);

        criteriaQuery.multiselect(root, vessel, vrp)
            .distinct(true);

        // Apply specification
        Specification<VesselFeatures> spec = toSpecification(filter, fetchOptions);
        Predicate predicate = spec.toPredicate(root, criteriaQuery, cb);
        if (predicate != null) criteriaQuery.where(predicate);

        // Add sorting
        addSorting(criteriaQuery, root, cb, page.getSortBy(), page.getSortDirection());

        TypedQuery<Tuple> query = getEntityManager().createQuery(criteriaQuery);

        // Bind parameters
        applyBindings(query, spec);

        // Set Limit
        query.setFirstResult((int)page.getOffset());
        query.setMaxResults(page.getSize());

        // Configure (set hints)
        configureQuery(query, fetchOptions);

        try (Stream<Tuple> stream = streamQuery(query)) {
            return stream.map(tuple -> toVO(tuple, fetchOptions, true))
                .toList();
        }

    }

    @Override
    public Specification<VesselFeatures> toSpecification(@NonNull VesselFilterVO filter, VesselFetchOptions fetchOptions) {
        return super.toSpecification(filter, fetchOptions)
            // IDs
            .and(id(filter.getVesselFeaturesId(), Integer.class))
            .and(vesselId(filter.getVesselId()))
            .and(vesselTypeId(filter.getVesselTypeId()))
            // by locations
            .and(registrationLocation(filter.getRegistrationLocationId()))
            .and(basePortLocation(filter.getBasePortLocationId()))
            // by Status
            .and(statusIds(filter.getStatusIds()))
            // Dates
            .and(betweenFeaturesDate(filter.getStartDate(), filter.getEndDate()))
            .and(betweenRegistrationDate(filter.getStartDate(), filter.getEndDate()))
            // Text
            .and(searchText(toEntityProperties(filter.getSearchAttributes()), filter.getSearchText()));
    }

    @Override
    public void toVO(VesselFeatures source, VesselSnapshotVO target, VesselFetchOptions fetchOptions, boolean copyIfNull) {

        VesselRegistrationPeriod registrationPeriod = vesselRegistrationPeriodRepository.getByVesselIdAndDate(source.getVessel().getId(),
            source.getStartDate()).orElse(null);

        this.toVO(source, source.getVessel(), registrationPeriod,
            target, fetchOptions, copyIfNull);
    }

    @Override
    public void toEntity(VesselSnapshotVO source, VesselFeatures target, boolean copyIfNull) {
        super.toEntity(source, target, copyIfNull);
    }

    @Override
    protected void onAfterSaveEntity(VesselSnapshotVO vo, VesselFeatures savedEntity, boolean isNew) {
        super.onAfterSaveEntity(vo, savedEntity, isNew);
    }



    @Override
    protected String toEntityProperty(@NonNull String property) {
        String fixedPropertyName = switch (property) {
            case VesselRegistrationPeriod.Fields.REGISTRATION_CODE, VesselRegistrationPeriod.Fields.INT_REGISTRATION_CODE ->
                StringUtils.doting(VesselFeatures.Fields.VESSEL, Vessel.Fields.VESSEL_REGISTRATION_PERIODS, property);
            default -> property;
        };
        if (!property.equals(fixedPropertyName)) {
            log.debug("Map property 'VesselSnapshotVO.{}' -> 'VesselRegistrationPeriod.{}'", property, fixedPropertyName);
            return fixedPropertyName;
        }

        return property;
    }

    protected VesselSnapshotVO toVO(Tuple source, VesselFetchOptions fetchOptions, boolean copyIfNull) {
        VesselSnapshotVO target = new VesselSnapshotVO();

        VesselFeatures features = source.get(0, VesselFeatures.class);
        Vessel vessel = source.get(1, Vessel.class);
        VesselRegistrationPeriod registrationPeriod = source.get(2, VesselRegistrationPeriod.class);

        toVO(features, vessel, registrationPeriod, target, fetchOptions, copyIfNull);

        return target;
    }

    protected void toVO(VesselFeatures features,
                     Vessel vessel,
                     VesselRegistrationPeriod registrationPeriod,
                     VesselSnapshotVO target, VesselFetchOptions fetchOptions, boolean copyIfNull) {
        super.toVO(features, target, fetchOptions, copyIfNull);

        // Convert from cm to m
        if (features.getLengthOverAll() != null) {
            target.setLengthOverAll(features.getLengthOverAll().doubleValue() / 100);
        }
        // Convert tonnage (divide by 100)
        if (features.getGrossTonnageGrt() != null) {
            target.setGrossTonnageGrt(features.getGrossTonnageGrt().doubleValue() / 100);
        }
        if (features.getGrossTonnageGt() != null) {
            target.setGrossTonnageGt(features.getGrossTonnageGt().doubleValue() / 100);
        }

        // Base port location
        if (fetchOptions != null && fetchOptions.isWithBasePortLocation()) {
            LocationVO basePortLocation = locationRepository.toVO(features.getBasePortLocation());
            if (copyIfNull || basePortLocation != null) {
                target.setBasePortLocation(basePortLocation);
            }
        }

        // Recorder department
        if (fetchOptions != null && fetchOptions.isWithRecorderDepartment()) {
            DepartmentVO recorderDepartment = referentialDao.toTypedVO(features.getRecorderDepartment(), DepartmentVO.class).orElse(null);
            target.setRecorderDepartment(recorderDepartment);
        }

        // Vessel
        if (vessel != null) {
            target.setId(vessel.getId());
            target.setVesselStatusId(vessel.getStatus().getId());
            target.setQualityFlagId(vessel.getQualityFlag().getId());

            // Vessel type
            if (vessel.getVesselType() != null) {
                ReferentialVO vesselType = referentialDao.toVO(vessel.getVesselType());
                target.setVesselType(vesselType);
            } else if (copyIfNull) {
                target.setVesselType(null);
            }
        }
        else if (copyIfNull) {
            target.setId(null);
            target.setVesselStatusId(null);
            target.setQualityFlagId(null);
            target.setVesselType(null);
        }

        // Registration period
        if (fetchOptions != null && fetchOptions.isWithVesselRegistrationPeriod()) {
            if (registrationPeriod != null) {
                // Registration code
                target.setRegistrationCode(registrationPeriod.getRegistrationCode());

                // International registration code
                target.setIntRegistrationCode(registrationPeriod.getIntRegistrationCode());

                // Registration location
                LocationVO location = locationRepository.toVO(registrationPeriod.getRegistrationLocation());
                target.setRegistrationLocation(location);
            }
            else if (copyIfNull) {
                target.setRegistrationCode(null);
                target.setIntRegistrationCode(null);
                target.setRegistrationLocation(null);
            }
        }
    }

    protected void configureQuery(TypedQuery<?> query, @Nullable VesselFetchOptions fetchOptions) {
        // Set hints
        if (enableAdagioOptimization) {
            query.setHint("org.hibernate.comment", String.format("+ INDEX(%s.VESSEL_REGISTRATION_PERIOD IX_VESSEL_REG_PER_END_DATE)", adagioSchema));
        }

    }
}

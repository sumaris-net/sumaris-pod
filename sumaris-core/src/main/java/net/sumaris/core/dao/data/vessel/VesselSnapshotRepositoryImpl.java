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

import com.google.common.collect.Lists;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.data.DataRepositoryImpl;
import net.sumaris.core.dao.referential.ReferentialDao;
import net.sumaris.core.dao.referential.location.LocationRepository;
import net.sumaris.core.dao.technical.Daos;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.event.config.ConfigurationReadyEvent;
import net.sumaris.core.event.config.ConfigurationUpdatedEvent;
import net.sumaris.core.model.data.Vessel;
import net.sumaris.core.model.data.VesselFeatures;
import net.sumaris.core.model.data.VesselRegistrationPeriod;
import net.sumaris.core.model.referential.location.Location;
import net.sumaris.core.util.Dates;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.data.VesselSnapshotVO;
import net.sumaris.core.vo.data.vessel.VesselFetchOptions;
import net.sumaris.core.vo.filter.VesselFilterVO;
import net.sumaris.core.vo.referential.LocationVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.hibernate.jpa.QueryHints;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.data.jpa.domain.Specification;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
public class VesselSnapshotRepositoryImpl
    extends DataRepositoryImpl<VesselFeatures, VesselSnapshotVO, VesselFilterVO, VesselFetchOptions>
    implements VesselSnapshotSpecifications {

    private final VesselRegistrationPeriodRepository vesselRegistrationPeriodRepository;
    private final LocationRepository locationRepository;
    private final ReferentialDao referentialDao;

    protected boolean enableRegistrationCodeSearchAsPrefix;
    protected boolean enableAdagioOptimization;
    protected String adagioSchema;

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

    @PostConstruct
    @EventListener({ConfigurationReadyEvent.class, ConfigurationUpdatedEvent.class})
    public void onConfigurationReady() {
        this.enableRegistrationCodeSearchAsPrefix = getConfig().enableVesselRegistrationCodeSearchAsPrefix();
        this.adagioSchema = getConfig().getAdagioSchema();
        this.enableAdagioOptimization = getConfig().enableAdagioOptimization() && StringUtils.isNotBlank(adagioSchema);
    }

    @Override
    public boolean enableRegistrationCodeSearchAsPrefix() {
        return enableRegistrationCodeSearchAsPrefix;
    }

    @Override
    public List<VesselSnapshotVO> findAll(@NonNull VesselFilterVO filter, int offset, int size, String sortAttribute, SortDirection sortDirection, VesselFetchOptions fetchOptions) {
        return this.findAll(filter, Page.create(offset, size, sortAttribute, sortDirection), fetchOptions);
    }

    @Override
    public List<VesselSnapshotVO> findAll(@NonNull VesselFilterVO filter,
                                          @Nullable Page page,
                                          @Nullable VesselFetchOptions fetchOptions) {

        CriteriaBuilder cb = this.getEntityManager().getCriteriaBuilder();
        CriteriaQuery<Tuple> criteriaQuery = cb.createTupleQuery();

        Root<VesselFeatures> root = criteriaQuery.from(VesselFeatures.class);
        Join<VesselFeatures, Vessel> vessel = Daos.composeJoin(root, VesselFeatures.Fields.VESSEL, JoinType.INNER);
        List<Selection<?>> selection = Lists.newArrayList(root, vessel);

        if (fetchOptions != null && fetchOptions.isWithBasePortLocation()) {
            Join<?, Location> basePortLocation = Daos.composeJoin(root, VesselFeatures.Fields.BASE_PORT_LOCATION);
            selection.add(basePortLocation);
        }
        if (fetchOptions != null && fetchOptions.isWithVesselRegistrationPeriod()) {
            ListJoin<Vessel, VesselRegistrationPeriod> vrp = composeVrpJoin(vessel, cb, null);
            selection.add(vrp);
            Join<?, Location> registrationLocation = Daos.composeJoin(vrp, VesselRegistrationPeriod.Fields.REGISTRATION_LOCATION);
            selection.add(registrationLocation);
        }

        criteriaQuery.multiselect(selection).distinct(true);

        // Apply specification
        Specification<VesselFeatures> spec = filter != null ? toSpecification(filter, fetchOptions) : null;
        Predicate predicate = spec != null ? spec.toPredicate(root, criteriaQuery, cb) : null;
        if (predicate != null) criteriaQuery.where(predicate);

        // Add sorting
        addSorting(criteriaQuery, root, cb, page);

        TypedQuery<Tuple> query = getEntityManager().createQuery(criteriaQuery);

        // Bind parameters
        applyBindings(query, spec);

        // Set Limit
        if (page != null) {
            query.setFirstResult((int) page.getOffset());
            query.setMaxResults(page.getSize());
        }

        // Set hints
        configureIndexHints(query, fetchOptions);

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
            .and(includedVesselIds(filter.getIncludedIds()))
            .and(excludedVesselIds(filter.getExcludedIds()))
            // Type
            .and(vesselTypeId(filter.getVesselTypeId()))
            .and(vesselTypeIds(filter.getVesselTypeIds()))
            // by locations
            .and(registrationLocation(filter.getRegistrationLocationId()))
            .and(basePortLocation(filter.getBasePortLocationId()))
            // by Status
            .and(statusIds(filter.getStatusIds()))
            // by program
            .and(programLabel(filter.getProgramLabel()))
            .and(programIds(filter.getProgramIds()))
            // Dates
            .and(betweenFeaturesDate(filter.getStartDate(), filter.getEndDate()))
            .and(betweenRegistrationDate(filter.getStartDate(), filter.getEndDate(), filter.getOnlyWithRegistration()))
            .and(newerThan(filter.getMinUpdateDate()))
            .and(newerThan(filter.getMinUpdateDate()))
            // Text
            .and(searchText(toEntityProperties(filter.getSearchAttributes()), filter.getSearchText()));
    }

    @Override
    public void toVO(VesselFeatures source, VesselSnapshotVO target, VesselFetchOptions fetchOptions, boolean copyIfNull) {

        // Fetch vessel registration period, at the startDate
        VesselRegistrationPeriod registrationPeriod = null;
        if ((fetchOptions == null || fetchOptions.isWithVesselRegistrationPeriod())) {
            registrationPeriod = vesselRegistrationPeriodRepository.findByVesselIdAndDate(
                source.getVessel().getId(),
                source.getStartDate()).orElse(null);
        }

        this.toVO(source, source.getVessel(), source.getBasePortLocation(),
            registrationPeriod, registrationPeriod != null ? registrationPeriod.getRegistrationLocation() : null,
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

        int index = 0;
        VesselFeatures features = source.get(index++, VesselFeatures.class);
        Vessel vessel = source.get(index++, Vessel.class);
        Location basePortLocation = (fetchOptions != null && fetchOptions.isWithBasePortLocation())
            ? source.get(index++, Location.class) : null;

        VesselRegistrationPeriod registrationPeriod = null ;
        Location registrationLocation = null ;
        if (fetchOptions != null && fetchOptions.isWithVesselRegistrationPeriod()) {
            registrationPeriod = source.get(index++, VesselRegistrationPeriod.class);
            registrationLocation = source.get(index++, Location.class);
        }

        toVO(features, vessel, basePortLocation, registrationPeriod, registrationLocation, target, fetchOptions, copyIfNull);

        return target;
    }

    protected void toVO(VesselFeatures source,
                        Vessel vessel,
                        Location basePortLocation,
                        VesselRegistrationPeriod registrationPeriod,
                        Location registrationLocation,
                        VesselSnapshotVO target,
                        VesselFetchOptions fetchOptions, boolean copyIfNull) {
        super.toVO(source, target, fetchOptions, copyIfNull);

        fetchOptions = VesselFetchOptions.nullToDefault(fetchOptions);

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
        if (fetchOptions.isWithBasePortLocation()) {
            if (basePortLocation != null) {
                LocationVO targetBasePortLocation = locationRepository.toVO(basePortLocation);
                if (copyIfNull || targetBasePortLocation != null) {
                    target.setBasePortLocation(targetBasePortLocation);
                }
            }
            else if (copyIfNull) {
                target.setBasePortLocation(null);
            }
        }

        // Recorder department
        if (fetchOptions.isWithRecorderDepartment()) {
            DepartmentVO recorderDepartment = referentialDao.toTypedVO(source.getRecorderDepartment(), DepartmentVO.class).orElse(null);
            target.setRecorderDepartment(recorderDepartment);
        }

        // Vessel
        if (vessel != null) {
            target.setVesselId(vessel.getId());
            target.setVesselStatusId(vessel.getStatus().getId());
            target.setQualityFlagId(vessel.getQualityFlag().getId());

            // Vessel type
            if (vessel.getVesselType() != null) {
                ReferentialVO vesselType = referentialDao.toVO(vessel.getVesselType());
                target.setVesselType(vesselType);
            } else if (copyIfNull) {
                target.setVesselType(null);
            }

            // Update date
            target.setUpdateDate(Dates.max(source.getUpdateDate(), vessel.getUpdateDate()));
        }
        else if (copyIfNull) {
            target.setVesselId(null);
            target.setVesselStatusId(null);
            target.setQualityFlagId(null);
            target.setVesselType(null);
            target.setUpdateDate(source.getUpdateDate());
        }

        // Registration period
        if (fetchOptions.isWithVesselRegistrationPeriod()) {
            if (registrationPeriod != null) {
                // Registration code
                target.setRegistrationCode(registrationPeriod.getRegistrationCode());

                // International registration code
                target.setIntRegistrationCode(registrationPeriod.getIntRegistrationCode());

                // Registration location
                if (registrationLocation != null) {
                    LocationVO location = locationRepository.toVO(registrationLocation);
                    if (copyIfNull || location != null) {
                        target.setRegistrationLocation(location);
                    }
                }
            }
            else if (copyIfNull) {
                target.setRegistrationCode(null);
                target.setIntRegistrationCode(null);
                target.setRegistrationLocation(null);
            }
        }
    }

    @Override
    protected void configureQuery(TypedQuery<VesselFeatures> query,
                                  @Nullable VesselFetchOptions fetchOptions) {
        super.configureQuery(query, fetchOptions);

        // Prepare load graph
        EntityManager em = getEntityManager();
        EntityGraph<?> entityGraph = em.getEntityGraph(VesselFeatures.GRAPH_SNAPSHOT);

        if (fetchOptions != null) {
            if (fetchOptions.isWithBasePortLocation())
                entityGraph.addSubgraph(VesselFeatures.Fields.BASE_PORT_LOCATION);
            if (fetchOptions.isWithRecorderPerson())
                entityGraph.addSubgraph(VesselFeatures.Fields.RECORDER_PERSON);
            if (fetchOptions.isWithRecorderDepartment())
                entityGraph.addSubgraph(VesselFeatures.Fields.RECORDER_DEPARTMENT);
        }

        query.setHint(QueryHints.HINT_LOADGRAPH, entityGraph);

        // Configure hints
        this.configureIndexHints(query, fetchOptions);
    }

    protected void configureIndexHints(TypedQuery<?> query,
                                       @Nullable VesselFetchOptions fetchOptions) {

        // Adagio optimization
        if (enableAdagioOptimization) {
            query.setHint("org.hibernate.comment", String.format("+ INDEX(%s.VESSEL_REGISTRATION_PERIOD IX_VESSEL_REG_PER_END_DATE)", adagioSchema));
        }
    }
}

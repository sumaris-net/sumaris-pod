package net.sumaris.core.dao.data.trip;

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
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.dao.data.RootDataRepositoryImpl;
import net.sumaris.core.dao.data.landing.LandingRepository;
import net.sumaris.core.dao.referential.location.LocationRepository;
import net.sumaris.core.dao.technical.Daos;
import net.sumaris.core.event.config.ConfigurationEvent;
import net.sumaris.core.event.config.ConfigurationReadyEvent;
import net.sumaris.core.event.config.ConfigurationUpdatedEvent;
import net.sumaris.core.model.data.*;
import net.sumaris.core.model.referential.location.Location;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.data.TripFetchOptions;
import net.sumaris.core.vo.data.TripVO;
import net.sumaris.core.vo.filter.TripFilterVO;
import org.apache.commons.collections.CollectionUtils;
import org.hibernate.jpa.QueryHints;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.data.jpa.domain.Specification;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import java.util.List;
import java.util.Objects;

@Slf4j
public class TripRepositoryImpl
        extends RootDataRepositoryImpl<Trip, TripVO, TripFilterVO, TripFetchOptions>
        implements TripSpecifications {

    private final LocationRepository locationRepository;
    private final LandingRepository landingRepository;

    private boolean enableVesselRegistrationNaturalOrder;

    @Autowired
    public TripRepositoryImpl(EntityManager entityManager,
                              LocationRepository locationRepository,
                              LandingRepository landingRepository,
                              SumarisConfiguration configuration,
                              GenericConversionService conversionService) {
        super(Trip.class, TripVO.class, entityManager);
        this.locationRepository = locationRepository;
        this.landingRepository = landingRepository;
        conversionService.addConverter(Trip.class, TripVO.class, this::toVO);
    }

    @PostConstruct
    @EventListener({ConfigurationReadyEvent.class, ConfigurationUpdatedEvent.class})
    public void onConfigurationReady() {
        this.enableVesselRegistrationNaturalOrder = configuration.enableVesselRegistrationCodeNaturalOrder();
    }

    @Override
    public Specification<Trip> toSpecification(TripFilterVO filter, TripFetchOptions fetchOptions) {
        return super.toSpecification(filter, fetchOptions)
            .and(id(filter.getTripId(), Integer.class))
            .and(betweenDate(filter.getStartDate(), filter.getEndDate()))
            .and(hasLocationId(filter.getLocationId()))
            .and(hasLocationIds(filter.getLocationIds()))
            .and(hasObservedLocationId(filter.getObservedLocationId()))
            .and(hasVesselId(filter.getVesselId()))
            .and(hasVesselIds(filter.getVesselIds()))
            .and(excludedIds(filter.getExcludedIds()))
            .and(includedIds(filter.getIncludedIds()))
            .and(hasObserverPersonIds(filter.getObserverPersonIds()))
            .and(inQualityFlagIds(filter.getQualityFlagIds()))
            .and(inDataQualityStatus(filter.getDataQualityStatus()))
            .and(withOperationIds(filter.getOperationIds()))
            .and(hasObservedLocation(filter.getHasObservedLocation()))
            .and(hasScientificCruise(filter.getHasScientificCruise()))
            ;
    }

    @Override
    public void toVO(Trip source, TripVO target, TripFetchOptions fetchOptions, boolean copyIfNull) {
        super.toVO(source, target, fetchOptions, copyIfNull);

        // Departure & return locations
        target.setDepartureLocation(locationRepository.toVO(source.getDepartureLocation()));
        target.setReturnLocation(locationRepository.toVO(source.getReturnLocation()));

        // Parent link
        if ((fetchOptions == null || fetchOptions.isWithLanding())
            && CollectionUtils.size(source.getLandings()) == 1) {
            Landing landing = source.getLandings().get(0);
            if (landing != null) {
                // Landing id
                target.setLandingId(landing.getId());

                // Observed location id
                if (landing.getObservedLocation() != null) {
                    target.setObservedLocationId(landing.getObservedLocation().getId());
                }
            }
        }

        // Scientific cruise
        if (source.getScientificCruise() != null) {
            target.setScientificCruiseId(source.getScientificCruise().getId());
        }
    }

    @Override
    public void toEntity(TripVO source, Trip target, boolean copyIfNull) {

        super.toEntity(source, target, copyIfNull);

        // Departure location
        if (copyIfNull || source.getDepartureLocation() != null) {
            if (source.getDepartureLocation() == null || source.getDepartureLocation().getId() == null) {
                target.setDepartureLocation(null);
            } else {
                target.setDepartureLocation(getReference(Location.class, source.getDepartureLocation().getId()));
            }
        }

        // Return location
        if (copyIfNull || source.getReturnLocation() != null) {
            if (source.getReturnLocation() == null || source.getReturnLocation().getId() == null) {
                target.setReturnLocation(null);
            } else {
                target.setReturnLocation(getReference(Location.class, source.getReturnLocation().getId()));
            }
        }
    }

    /* -- protected functions -- */

    @Override
    protected String toEntityProperty(@NonNull String property) {
        if (Trip.Fields.VESSEL.equalsIgnoreCase(property) || property.endsWith(VesselRegistrationPeriod.Fields.REGISTRATION_CODE)) {
            return StringUtils.doting(Trip.Fields.VESSEL, Vessel.Fields.VESSEL_REGISTRATION_PERIODS, VesselRegistrationPeriod.Fields.REGISTRATION_CODE);
        }
        if (property.endsWith(VesselRegistrationPeriod.Fields.INT_REGISTRATION_CODE)) {
            return StringUtils.doting(Trip.Fields.VESSEL, Vessel.Fields.VESSEL_REGISTRATION_PERIODS, VesselRegistrationPeriod.Fields.INT_REGISTRATION_CODE);
        }
        if (property.endsWith(VesselFeatures.Fields.EXTERIOR_MARKING)) {
            return StringUtils.doting(Trip.Fields.VESSEL, Vessel.Fields.VESSEL_FEATURES, VesselFeatures.Fields.EXTERIOR_MARKING);
        }
        if (property.endsWith(VesselFeatures.Fields.NAME)) {
            return StringUtils.doting(Trip.Fields.VESSEL, Vessel.Fields.VESSEL_FEATURES, VesselFeatures.Fields.NAME);
        }
        return super.toEntityProperty(property);
    }

    @Override
    protected List<Expression<?>> toSortExpressions(CriteriaQuery<?> query, Root<Trip> root, CriteriaBuilder cb, String property) {

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
    protected void onAfterSaveEntity(TripVO vo, Trip savedEntity, boolean isNew) {
        super.onAfterSaveEntity(vo, savedEntity, isNew);

        // Update landing link, if exists
        Integer landingId = vo.getLanding() != null && vo.getLanding().getId() != null ? vo.getLanding().getId() : vo.getLandingId();
        Integer observedLocationId = null;
        if (landingId != null) {
            Landing landing = getReference(Landing.class, landingId);
            if (landing != null) {
                if (landing.getTrip() == null || !Objects.equals(landing.getTrip().getId(), savedEntity.getId())) {
                    landing.setTrip(savedEntity);
                    landingRepository.save(landing);
                }
                if (landing.getObservedLocation() != null) {
                    observedLocationId = landing.getObservedLocation().getId();
                }
            }

        }
        // Update the given VO (will be returned by the save() function)
        vo.setLandingId(landingId);
        vo.setObservedLocationId(observedLocationId);
    }

    @Override
    protected void configureQuery(TypedQuery<Trip> query, @Nullable TripFetchOptions fetchOptions) {
        super.configureQuery(query, fetchOptions);

        if (fetchOptions == null || fetchOptions.isWithLocations() || fetchOptions.isWithProgram()) {
            // Prepare load graph
            EntityManager em = getEntityManager();
            EntityGraph<?> entityGraph = em.getEntityGraph(Trip.GRAPH_LOCATIONS_AND_PROGRAM);
            if (fetchOptions == null || fetchOptions.isWithRecorderPerson())
                entityGraph.addSubgraph(Trip.Fields.RECORDER_PERSON);
            if (fetchOptions == null || fetchOptions.isWithRecorderDepartment())
                entityGraph.addSubgraph(Trip.Fields.RECORDER_DEPARTMENT);

            // WARNING: should not enable this fetch, because page cannot be applied
            //if (fetchOptions.isWithObservers()) entityGraph.addSubgraph(Trip.Fields.OBSERVERS);

            query.setHint(QueryHints.HINT_LOADGRAPH, entityGraph);
        }
    }
}

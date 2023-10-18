package net.sumaris.core.dao.data.landing;

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
import net.sumaris.core.dao.data.RootDataRepositoryImpl;
import net.sumaris.core.dao.referential.location.LocationRepository;
import net.sumaris.core.dao.technical.Daos;
import net.sumaris.core.dao.technical.jpa.BindableSpecification;
import net.sumaris.core.event.config.ConfigurationEvent;
import net.sumaris.core.event.config.ConfigurationReadyEvent;
import net.sumaris.core.event.config.ConfigurationUpdatedEvent;
import net.sumaris.core.model.data.*;
import net.sumaris.core.model.referential.location.Location;
import net.sumaris.core.util.Beans;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.administration.programStrategy.ProgramVO;
import net.sumaris.core.vo.data.LandingFetchOptions;
import net.sumaris.core.vo.data.LandingVO;
import net.sumaris.core.vo.filter.LandingFilterVO;
import org.apache.commons.collections4.CollectionUtils;
import org.hibernate.jpa.QueryHints;
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
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public class LandingRepositoryImpl
    extends RootDataRepositoryImpl<Landing, LandingVO, LandingFilterVO, LandingFetchOptions>
    implements LandingSpecifications {

    private final LocationRepository locationRepository;
    protected boolean enableVesselRegistrationNaturalOrder;
    protected boolean enableAdagioOptimization;

    public LandingRepositoryImpl(EntityManager entityManager,
                                 LocationRepository locationRepository,
                                 GenericConversionService conversionService) {
        super(Landing.class, LandingVO.class, entityManager);
        this.locationRepository = locationRepository;

        // FIXME BLA 30/09/2021 - temporary workaround for issue IMAGINE-540
        setCheckUpdateDate(false);
        setLockForUpdate(false);

        conversionService.addConverter(Landing.class, LandingVO.class, this::toVO);
    }

    @PostConstruct
    @EventListener({ConfigurationReadyEvent.class, ConfigurationUpdatedEvent.class})
    public void onConfigurationReady() {
        this.enableVesselRegistrationNaturalOrder = configuration.enableVesselRegistrationCodeNaturalOrder();
        this.enableAdagioOptimization = configuration.enableAdagioOptimization();
    }

    @Override
    public List<LandingVO> findAllByObservedLocationId(int observedLocationId) {
        return findAllVO(hasObservedLocationId(observedLocationId));
    }

    @Override
    public List<LandingVO> findAllByTripIds(List<Integer> tripIds) {
        return findAllVO(hasTripIds(tripIds));
    }

    @Override
    public Specification<Landing> toSpecification(LandingFilterVO filter, LandingFetchOptions fetchOptions) {
        if (filter.getObservedLocationId() != null && Beans.beanIsEmpty(filter, LandingFilterVO.Fields.OBSERVED_LOCATION_ID)) {
            return BindableSpecification
                .where(hasObservedLocationId(filter.getObservedLocationId()));
        }

        return super.toSpecification(filter, fetchOptions)
            .and(hasObservedLocationId(filter.getObservedLocationId()))
            .and(hasTripId(filter.getTripId()))
            .and(hasObserverPersonIds(filter))
            .and(betweenDate(filter.getStartDate(), filter.getEndDate()))
            .and(hasLocationId(filter.getLocationId()))
            .and(inLocationIds(filter.getLocationIds()))
            .and(hasVesselId(filter.getVesselId()))
            .and(hasExcludeVesselIds(filter.getExcludeVesselIds()))
            .and(inDataQualityStatus(filter.getDataQualityStatus()))
            .and(inQualityFlagIds(filter.getQualityFlagIds()))
            .and(hasStrategyLabels(filter.getStrategyLabels()))
            .and(hasSampleLabels(filter.getSampleLabels(), this.enableAdagioOptimization))
            .and(hasSampleTagIds(filter.getSampleTagIds(), this.enableAdagioOptimization));
    }

    @Override
    public List<LandingVO> saveAllByObservedLocationId(int observedLocationId, List<LandingVO> sources) {
        // Load parent entity
        ObservedLocation parent = getById(ObservedLocation.class, observedLocationId);
        ProgramVO parentProgram = new ProgramVO();
        parentProgram.setId(parent.getProgram().getId());

        // Remember existing entities
        final List<Integer> sourcesIdsToRemove = Beans.collectIds(Beans.getList(parent.getLandings()));

        // Save each landing
        List<LandingVO> result = sources.stream().map(source -> {
            source.setObservedLocationId(observedLocationId);
            source.setProgram(parentProgram);

            if (source.getId() != null) {
                sourcesIdsToRemove.remove(source.getId());
            }
            return save(source);
        }).collect(Collectors.toList());

        // Remove unused entities
        if (CollectionUtils.isNotEmpty(sourcesIdsToRemove)) {
            sourcesIdsToRemove.forEach(this::deleteById);
        }

        return result;
    }

    @Override
    public void toVO(Landing source, LandingVO target, LandingFetchOptions fetchOptions, boolean copyIfNull) {
        super.toVO(source, target, fetchOptions, copyIfNull);

        // location
        target.setLocation(locationRepository.toVO(source.getLocation()));

        // Parent link
        if (source.getObservedLocation() != null) {
            target.setObservedLocationId(source.getObservedLocation().getId());
        }
        if (source.getTrip() != null) {
            target.setTripId(source.getTrip().getId());
        }
    }

    @Override
    public void toEntity(LandingVO source, Landing target, boolean copyIfNull) {

        super.toEntity(source, target, copyIfNull);

        // Landing location
        if (copyIfNull || source.getLocation() != null) {
            if (source.getLocation() == null || source.getLocation().getId() == null) {
                target.setLocation(null);
            } else {
                target.setLocation(getReference(Location.class, source.getLocation().getId()));
            }
        }

        // Observed Location
        Integer observedLocationId = source.getObservedLocationId() != null ? source.getObservedLocationId() : (source.getObservedLocation() != null ? source.getObservedLocation().getId() : null);
        if (copyIfNull || (observedLocationId != null)) {
            if (observedLocationId == null) {
                target.setObservedLocation(null);
            } else {
                target.setObservedLocation(getReference(ObservedLocation.class, observedLocationId));
            }
        }

        // Trip
        Integer tripId = source.getTripId() != null ? source.getTripId() : (source.getTrip() != null ? source.getTrip().getId() : null);
        if (copyIfNull || (tripId != null)) {
            if (tripId == null) {
                target.setTrip(null);
            } else {
                target.setTrip(getReference(Trip.class, tripId));
            }
        }

        // RankOrder
        if (source.getRankOrder() == null) {
            // must compute next rank order
            target.setRankOrder(getNextRankOrder(target));
        }

    }

    /* -- internal functions -- */

    @Override
    protected String toEntityProperty(@NonNull String property) {
        if (Landing.Fields.VESSEL.equalsIgnoreCase(property) || property.endsWith(VesselRegistrationPeriod.Fields.REGISTRATION_CODE)) {
            return StringUtils.doting(Landing.Fields.VESSEL, Vessel.Fields.VESSEL_REGISTRATION_PERIODS, VesselRegistrationPeriod.Fields.REGISTRATION_CODE);
        }
        if (property.endsWith(VesselRegistrationPeriod.Fields.INT_REGISTRATION_CODE)) {
            return StringUtils.doting(Landing.Fields.VESSEL, Vessel.Fields.VESSEL_REGISTRATION_PERIODS, VesselRegistrationPeriod.Fields.INT_REGISTRATION_CODE);
        }
        if (property.endsWith(VesselFeatures.Fields.EXTERIOR_MARKING)) {
            return StringUtils.doting(Landing.Fields.VESSEL, Vessel.Fields.VESSEL_FEATURES, VesselFeatures.Fields.EXTERIOR_MARKING);
        }
        if (property.endsWith(VesselFeatures.Fields.NAME)) {
            return StringUtils.doting(Landing.Fields.VESSEL, Vessel.Fields.VESSEL_FEATURES, VesselFeatures.Fields.NAME);
        }
        return super.toEntityProperty(property);
    }

    @Override
    protected List<Expression<?>> toSortExpressions(CriteriaQuery<?> query, Root<Landing> root, CriteriaBuilder cb, String property) {

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
            };
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

    /**
     * Compute rank order for this landing
     * Default value = 1 and incremented if another landing with same vessel found
     *
     * @param landing landing to save
     * @return next rank order
     */
    private Integer getNextRankOrder(Landing landing) {

        // Default value
        int result = 1;

        if (landing.getObservedLocation() == null || landing.getObservedLocation().getId() == null) {
            // Can't find other landings on an undefined observed location
            return result;
        }

        // Find landings
        LandingFilterVO filter = LandingFilterVO.builder()
            .observedLocationId(landing.getObservedLocation().getId())
            .vesselId(landing.getVessel().getId())
            .build();
        Optional<Integer> currentRankOrder = findAll(filter).stream()
            // exclude itself
            .filter(landingVO -> !landingVO.getId().equals(landing.getId()))
            .map(LandingVO::getRankOrder)
            .filter(Objects::nonNull)
            // find max rankOrder
            .max(Integer::compareTo);
        if (currentRankOrder.isPresent()) {
            result = Math.max(result, currentRankOrder.get());
        }
        return result;
    }

    @Override
    protected void configureQuery(TypedQuery<Landing> query, @Nullable LandingFetchOptions fetchOptions) {
        super.configureQuery(query, fetchOptions);

        // Prepare load graph
        EntityManager em = getEntityManager();
        EntityGraph<?> entityGraph = em.getEntityGraph(Landing.GRAPH_LOCATION_AND_PROGRAM);
        if (fetchOptions == null || fetchOptions.isWithRecorderPerson()) entityGraph.addSubgraph(Landing.Fields.RECORDER_PERSON);
        if (fetchOptions == null || fetchOptions.isWithRecorderDepartment()) entityGraph.addSubgraph(Landing.Fields.RECORDER_DEPARTMENT);

        // WARNING: should not enable this fetch, because page cannot be applied
        //if (fetchOptions.isWithObservers()) entityGraph.addSubgraph(Landing.Fields.OBSERVERS);

        query.setHint(QueryHints.HINT_LOADGRAPH, entityGraph);
    }
}

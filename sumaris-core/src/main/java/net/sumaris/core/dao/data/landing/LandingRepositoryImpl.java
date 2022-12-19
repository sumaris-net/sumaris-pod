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

import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.data.RootDataRepositoryImpl;
import net.sumaris.core.dao.referential.location.LocationRepository;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.event.config.ConfigurationEvent;
import net.sumaris.core.event.config.ConfigurationReadyEvent;
import net.sumaris.core.event.config.ConfigurationUpdatedEvent;
import net.sumaris.core.model.data.Landing;
import net.sumaris.core.model.data.ObservedLocation;
import net.sumaris.core.model.data.Trip;
import net.sumaris.core.model.referential.location.Location;
import net.sumaris.core.util.Beans;
import net.sumaris.core.vo.administration.programStrategy.ProgramVO;
import net.sumaris.core.vo.data.LandingFetchOptions;
import net.sumaris.core.vo.data.LandingVO;
import net.sumaris.core.vo.filter.LandingFilterVO;
import org.apache.commons.collections4.CollectionUtils;
import org.hibernate.jpa.QueryHints;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.EventListener;
import org.springframework.core.convert.converter.ConverterRegistry;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.data.jpa.domain.Specification;

import javax.annotation.Nullable;
import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public class LandingRepositoryImpl
    extends RootDataRepositoryImpl<Landing, LandingVO, LandingFilterVO, LandingFetchOptions>
    implements LandingSpecifications {

    private final LocationRepository locationRepository;
    private boolean isOracleDatabase;
    private boolean enableVesselRegistrationNaturalOrder;

    @Autowired
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

    @EventListener({ConfigurationReadyEvent.class, ConfigurationUpdatedEvent.class})
    public void onConfigurationReady(ConfigurationEvent event) {
        isOracleDatabase = event.getConfiguration().isOracleDatabase();
        enableVesselRegistrationNaturalOrder = event.getConfiguration().enableVesselRegistrationCodeNaturalOrder();
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
        return super.toSpecification(filter, fetchOptions)
            .and(hasObservedLocationId(filter.getObservedLocationId()))
            .and(hasTripId(filter.getTripId()))
            .and(betweenDate(filter.getStartDate(), filter.getEndDate()))
            .and(hasLocationId(filter.getLocationId()))
            .and(inLocationIds(filter.getLocationIds()))
            .and(hasVesselId(filter.getVesselId()))
            .and(hasExcludeVesselIds(filter.getExcludeVesselIds()))
            .and(inDataQualityStatus(filter.getDataQualityStatus()));
    }

    @Override
    public List<LandingVO> findAllByObservedLocationId(int observedLocationId, Page page, LandingFetchOptions fetchOptions) {

        // Following natural sort works only for Oracle
        boolean sortByVesselRegistrationCode = Landing.Fields.VESSEL.equalsIgnoreCase(page.getSortBy());

        StringBuilder queryBuilder = new StringBuilder();

        queryBuilder.append("select l from Landing l ");

        if (sortByVesselRegistrationCode) {
            // add joins
            queryBuilder.append("inner join l.vessel v ")
                .append("left outer join v.vesselRegistrationPeriods vrp ");;
        }

        // single filter
        queryBuilder.append("where l.observedLocation.id = :observedLocationId ");

        // Sort by vessel registration code
        if (sortByVesselRegistrationCode) {
            // Oracle + natural order
            if (isOracleDatabase && enableVesselRegistrationNaturalOrder) {
                queryBuilder.append(
                    String.format(
                        "order by regexp_substr(vrp.registrationCode, '[^0-9]*') %1$s, to_number(regexp_substr(vrp.registrationCode, '[0-9]+')) %1$s nulls first",
                        page.getSortDirection()
                    )
                );
            }
            // Sort by vessel registration code
            else {
                queryBuilder.append(String.format("order by vrp.registrationCode %s", page.getSortDirection()));
            }

        } else {
            // other sort
            queryBuilder.append(
              String.format(
                  "order by l.%s %s",
                  page.getSortBy(),
                  page.getSortDirection()
              )
            );
        }


        // Create the JPA query
        TypedQuery<Landing> query = getEntityManager().createQuery(queryBuilder.toString(), Landing.class);
        query.setParameter("observedLocationId", observedLocationId);
        query.setFirstResult((int)page.getOffset());
        query.setMaxResults(page.getSize());

        configureQuery(query, fetchOptions);

        return streamQuery(query)
            .map(landing -> toVO(landing, fetchOptions))
            .collect(Collectors.toList());
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

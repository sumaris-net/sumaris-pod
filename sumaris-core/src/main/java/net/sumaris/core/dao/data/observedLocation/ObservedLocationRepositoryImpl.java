package net.sumaris.core.dao.data.observedLocation;

/*-
 * #%L
 * SUMARiS:: Core
 * %%
 * Copyright (C) 2018 - 2020 SUMARiS Consortium
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
import net.sumaris.core.dao.referential.ReferentialDao;
import net.sumaris.core.dao.referential.location.LocationRepository;
import net.sumaris.core.model.administration.samplingScheme.DenormalizedSamplingStrata;
import net.sumaris.core.model.administration.samplingScheme.SamplingStrata;
import net.sumaris.core.model.data.ObservedLocation;
import net.sumaris.core.model.data.Trip;
import net.sumaris.core.model.referential.location.Location;
import net.sumaris.core.vo.data.ObservedLocationFetchOptions;
import net.sumaris.core.vo.data.ObservedLocationVO;
import net.sumaris.core.vo.filter.ObservedLocationFilterVO;
import net.sumaris.core.vo.referential.ReferentialFetchOptions;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.hibernate.jpa.QueryHints;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.data.jpa.domain.Specification;

import javax.annotation.Nullable;
import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

/**
 * @author peck7 on 31/08/2020.
 */
@Slf4j
public class ObservedLocationRepositoryImpl
    extends RootDataRepositoryImpl<ObservedLocation, ObservedLocationVO, ObservedLocationFilterVO, ObservedLocationFetchOptions>
    implements ObservedLocationSpecifications {

    private final LocationRepository locationRepository;

    private final ReferentialDao referentialDao;

    @Autowired
    public ObservedLocationRepositoryImpl(EntityManager entityManager,
                                          LocationRepository locationRepository,
                                          ReferentialDao referentialDao,
                                          GenericConversionService conversionService) {
        super(ObservedLocation.class, ObservedLocationVO.class, entityManager);
        this.locationRepository = locationRepository;
        this.referentialDao = referentialDao;
        conversionService.addConverter(ObservedLocation.class, ObservedLocationVO.class, this::toVO);
    }

    @Override
    public Specification<ObservedLocation> toSpecification(ObservedLocationFilterVO filter, ObservedLocationFetchOptions fetchOptions) {
        return super.toSpecification(filter, fetchOptions)
            .and(hasLocationId(filter.getLocationId()))
            .and(hasLocationIds(filter.getLocationIds()))
            .and(withStartDate(filter.getStartDate()))
            .and(withEndDate(filter.getEndDate()))
            .and(hasObserverPersonIds(filter.getObserverPersonIds()))
            .and(hasRecorderDepartmentIds(concat(filter.getRecorderDepartmentId(), filter.getRecorderDepartmentIds())))
            .and(hasRecorderPersonIds(concat(filter.getRecorderPersonId(), filter.getRecorderPersonIds())))
            .and(inQualityFlagIds(filter.getQualityFlagIds()))
            .and(inDataQualityStatus(filter.getDataQualityStatus()))
            .and(hasLandingVesselIds(filter.getVesselIds()))
            ;
    }

    @Override
    public void toVO(ObservedLocation source, ObservedLocationVO target, ObservedLocationFetchOptions fetchOptions, boolean copyIfNull) {
        super.toVO(source, target, fetchOptions, copyIfNull);

        // Remove endDateTime if same as startDateTime
        if (target.getEndDateTime() != null && target.getEndDateTime().equals(target.getStartDateTime())) {
            target.setEndDateTime(null);
        }

        // Location
        target.setLocation(locationRepository.toVO(source.getLocation()));

        // Sampling strata
        if (source.getSamplingStrata() != null) {
            target.setSamplingStrataId(source.getSamplingStrata().getId());
            if (fetchOptions != null && fetchOptions.isWithSamplingStrata()) {
                ReferentialVO samplingStrata = referentialDao.get(DenormalizedSamplingStrata.class, source.getSamplingStrata().getId(),
                        ReferentialFetchOptions.builder()
                                .withProperties(true) // Load sampling scheme label
                                .build());
                target.setSamplingStrata(samplingStrata);
            }
        }
    }

    @Override
    public void toEntity(ObservedLocationVO source, ObservedLocation target, boolean copyIfNull) {
        super.toEntity(source, target, copyIfNull);

        // If endDateTime is empty, fill using startDateTime
        if (target.getEndDateTime() == null) {
            target.setEndDateTime(target.getStartDateTime());
        }

        // Location
        if (copyIfNull || source.getLocation() != null) {
            if (source.getLocation() == null || source.getLocation().getId() == null) {
                target.setLocation(null);
            } else {
                target.setLocation(getReference(Location.class, source.getLocation().getId()));
            }
        }

        // Sampling strata
        Integer samplingStrataId = source.getSamplingStrata() != null ? source.getSamplingStrata().getId() : source.getSamplingStrataId();
        if (copyIfNull || samplingStrataId != null) {
            if (samplingStrataId == null) {
                target.setSamplingStrata(null);
            } else {
                target.setSamplingStrata(getReference(SamplingStrata.class, samplingStrataId));
            }
        }
    }

    @Override
    protected void configureQuery(TypedQuery<ObservedLocation> query,
                                  @Nullable ObservedLocationFetchOptions fetchOptions) {
        super.configureQuery(query, fetchOptions);

        if (fetchOptions == null || fetchOptions.isWithLocations() || fetchOptions.isWithProgram() || fetchOptions.isWithSamplingStrata()) {
            // Prepare load graph
            EntityManager em = getEntityManager();
            EntityGraph<?> entityGraph = em.getEntityGraph(ObservedLocation.GRAPH_LOCATION_AND_PROGRAM);
            if (fetchOptions == null || fetchOptions.isWithRecorderPerson())
                entityGraph.addSubgraph(ObservedLocation.Fields.RECORDER_PERSON);
            if (fetchOptions == null || fetchOptions.isWithRecorderDepartment())
                entityGraph.addSubgraph(ObservedLocation.Fields.RECORDER_DEPARTMENT);

            // Sampling strata
            if (fetchOptions == null || fetchOptions.isWithSamplingStrata())
                entityGraph.addSubgraph(Trip.Fields.SAMPLING_STRATA);

            // WARNING: should not enable this fetch, because page cannot be applied
            //if (fetchOptions.isWithObservers()) entityGraph.addSubgraph(Trip.Fields.OBSERVERS);

            query.setHint(QueryHints.HINT_LOADGRAPH, entityGraph);
        }

    }
}

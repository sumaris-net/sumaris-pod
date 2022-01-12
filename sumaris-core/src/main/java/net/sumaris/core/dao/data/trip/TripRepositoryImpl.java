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

import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.data.RootDataRepositoryImpl;
import net.sumaris.core.dao.data.landing.LandingRepository;
import net.sumaris.core.dao.referential.location.LocationRepository;
import net.sumaris.core.model.data.Landing;
import net.sumaris.core.model.data.Trip;
import net.sumaris.core.model.referential.location.Location;
import net.sumaris.core.vo.data.TripFetchOptions;
import net.sumaris.core.vo.data.TripVO;
import net.sumaris.core.vo.filter.TripFilterVO;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.EntityManager;
import java.util.Objects;

@Slf4j
public class TripRepositoryImpl
        extends RootDataRepositoryImpl<Trip, TripVO, TripFilterVO, TripFetchOptions>
        implements TripSpecifications {

    private final LocationRepository locationRepository;
    private final LandingRepository landingRepository;

    @Autowired
    public TripRepositoryImpl(EntityManager entityManager, LocationRepository locationRepository, LandingRepository landingRepository) {
        super(Trip.class, TripVO.class, entityManager);
        this.locationRepository = locationRepository;
        this.landingRepository = landingRepository;
    }

    @Override
    public Specification<Trip> toSpecification(TripFilterVO filter, TripFetchOptions fetchOptions) {
        return super.toSpecification(filter, fetchOptions)
            .and(id(filter.getTripId()))
            .and(betweenDate(filter.getStartDate(), filter.getEndDate()))
            .and(hasLocationId(filter.getLocationId()))
            .and(hasVesselId(filter.getVesselId()))
            .and(excludedIds(filter.getExcludedIds()))
            .and(includedIds(filter.getIncludedIds()))
            .and(hasObserverPersonIds(filter.getObserverPersonIds()))
            .and(hasQualityFlagIds(filter.getQualityFlagIds()))
            .and(inDataQualityStatus(filter.getDataQualityStatus()))
            ;
    }

    @Override
    public void toVO(Trip source, TripVO target, TripFetchOptions fetchOptions, boolean copyIfNull) {
        super.toVO(source, target, fetchOptions, copyIfNull);

        // Departure & return locations
        target.setDepartureLocation(locationRepository.toVO(source.getDepartureLocation()));
        target.setReturnLocation(locationRepository.toVO(source.getReturnLocation()));

        // Parent link
        if (CollectionUtils.size(source.getLandings()) == 1) {
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

        // Parent link
        // TODO scientificCruise

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
}

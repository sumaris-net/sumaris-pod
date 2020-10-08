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

import com.google.common.base.Preconditions;
import net.sumaris.core.dao.data.RootDataRepositoryImpl;
import net.sumaris.core.dao.referential.location.LocationRepository;
import net.sumaris.core.dao.technical.Daos;
import net.sumaris.core.model.referential.QualityFlagEnum;
import net.sumaris.core.model.data.Trip;
import net.sumaris.core.model.referential.QualityFlag;
import net.sumaris.core.model.referential.location.Location;
import net.sumaris.core.vo.data.DataFetchOptions;
import net.sumaris.core.vo.data.TripVO;
import net.sumaris.core.vo.filter.TripFilterVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.EntityManager;
import java.sql.Timestamp;

public class TripRepositoryImpl
    extends RootDataRepositoryImpl<Trip, TripVO, TripFilterVO, DataFetchOptions>
    implements TripSpecifications {

    private static final Logger log =
        LoggerFactory.getLogger(TripRepositoryImpl.class);

    private final LocationRepository locationRepository;

    @Autowired
    public TripRepositoryImpl(EntityManager entityManager, LocationRepository locationRepository) {
        super(Trip.class, TripVO.class, entityManager);
        this.locationRepository = locationRepository;
    }

    @Override
    public Specification<Trip> toSpecification(TripFilterVO filter) {
        return super.toSpecification(filter)
                .and(betweenDate(filter.getStartDate(), filter.getEndDate()))
                .and(hasLocationId(filter.getLocationId()))
                .and(hasVesselId(filter.getVesselId()));
    }

    @Override
    public void toVO(Trip source, TripVO target, DataFetchOptions fetchOptions, boolean copyIfNull) {
        super.toVO(source, target, fetchOptions, copyIfNull);

        // Departure & return locations
        target.setDepartureLocation(locationRepository.toVO(source.getDepartureLocation()));
        target.setReturnLocation(locationRepository.toVO(source.getReturnLocation()));


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
                target.setDepartureLocation(load(Location.class, source.getDepartureLocation().getId()));
            }
        }

        // Return location
        if (copyIfNull || source.getReturnLocation() != null) {
            if (source.getReturnLocation() == null || source.getReturnLocation().getId() == null) {
                target.setReturnLocation(null);
            } else {
                target.setReturnLocation(load(Location.class, source.getReturnLocation().getId()));
            }
        }

    }



    @Override
    public TripVO qualify(TripVO vo) {
        Preconditions.checkNotNull(vo);

        Trip entity = find(Trip.class, vo.getId());
        if (entity == null) {
            throw new DataRetrievalFailureException(String.format("Trip {%s} not found", vo.getId()));
        }

        // Check update date
        Daos.checkUpdateDateForUpdate(vo, entity);

        // Lock entityName
        // lockForUpdate(entity);

        // Update update_dt
        Timestamp newUpdateDate = getDatabaseCurrentTimestamp();
        entity.setUpdateDate(newUpdateDate);

        int qualityFlagId = vo.getQualityFlagId() != null ? vo.getQualityFlagId() : 0;

        // If not qualify, then remove the qualification date
        if (qualityFlagId == QualityFlagEnum.NOT_QUALIFED.getId()) {
            entity.setQualificationDate(null);
        }
        else {
            entity.setQualificationDate(newUpdateDate);
        }
        // Apply a find, because can return a null value (e.g. if id is not in the DB instance)
        entity.setQualityFlag(find(QualityFlag.class, qualityFlagId));

        // TODO UNVALIDATION PROCESS HERE
        // - insert into qualification history

        // Save entityName
        getEntityManager().merge(entity);

        // Update source
        vo.setQualificationDate(entity.getQualificationDate());
        vo.setQualityFlagId(entity.getQualityFlag() != null ? entity.getQualityFlag().getId() : 0);
        vo.setUpdateDate(newUpdateDate);

        return vo;
    }

}

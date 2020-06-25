package net.sumaris.core.dao.data;

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
import net.sumaris.core.dao.referential.location.LocationDao;
import net.sumaris.core.dao.technical.Daos;
import net.sumaris.core.model.QualityFlagEnum;
import net.sumaris.core.model.data.Trip;
import net.sumaris.core.model.data.ObservedLocation;
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
import java.util.Date;

public class TripRepositoryImpl
    extends RootDataRepositoryImpl<Trip, Integer, TripVO, TripFilterVO>
    implements TripRepositoryExtend {

    private static final Logger log =
        LoggerFactory.getLogger(TripRepositoryImpl.class);

    private final LocationDao locationDao;

    @Autowired
    public TripRepositoryImpl(EntityManager entityManager, LocationDao locationDao) {
        super(Trip.class, entityManager);
        this.locationDao = locationDao;
    }

    @Override
    public Specification<Trip> toSpecification(TripFilterVO filter) {
        if (filter == null) return null;

        return Specification
                .where(hasRecorderDepartmentId(filter.getRecorderDepartmentId()))
                .and(hasRecorderPersonId(filter.getRecorderPersonId()))
                .and(hasProgramLabel(filter.getProgramLabel()))
                .and(betweenDate(filter.getStartDate(), filter.getEndDate()))
                .and(hasLocationId(filter.getLocationId()))
                .and(hasVesselId(filter.getVesselId()));
    }

    public Class<TripVO> getVOClass() {
        return TripVO.class;
    }

    @Override
    public void toVO(Trip source, TripVO target, DataFetchOptions fetchOptions, boolean copyIfNull) {
        super.toVO(source, target, fetchOptions, copyIfNull);

        // Departure & return locations
        target.setDepartureLocation(locationDao.toLocationVO(source.getDepartureLocation()));
        target.setReturnLocation(locationDao.toLocationVO(source.getReturnLocation()));


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

        Trip entity = get(Trip.class, vo.getId());
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

        int qualityFlagId = vo.getQualityFlagId() != null ? vo.getQualityFlagId().intValue() : 0;

        // If not qualify, then remove the qualification date
        if (qualityFlagId == QualityFlagEnum.NOT_QUALIFED.getId()) {
            entity.setQualificationDate(null);
        }
        else {
            entity.setQualificationDate(newUpdateDate);
        }
        // Apply a get, because can return a null value (e.g. if id is not in the DB instance)
        entity.setQualityFlag(get(QualityFlag.class, Integer.valueOf(qualityFlagId)));

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

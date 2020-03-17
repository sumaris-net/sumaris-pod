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

import net.sumaris.core.dao.technical.model.IEntity;
import net.sumaris.core.model.data.Landing;
import net.sumaris.core.vo.data.LandingVO;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.Date;

@NoRepositoryBean
public interface LandingRepositoryExtend extends IEntityConverter<Landing, LandingVO> {

    default Specification<Landing> hasObservedLocationId(Integer observedLocationId) {
        if (observedLocationId == null) return null;
        return (root, query, cb) -> cb.equal(root.get(Landing.Fields.OBSERVED_LOCATION).get(IEntity.Fields.ID), observedLocationId);
    }

    default Specification<Landing> hasTripId(Integer tripId) {
        if (tripId == null) return null;
        return (root, query, cb) -> cb.equal(root.get(Landing.Fields.TRIP).get(IEntity.Fields.ID), tripId);
    }

    default Specification<Landing> hasLocationId(Integer locationId) {
        if (locationId == null) return null;
        return (root, query, cb) -> cb.equal(root.get(Landing.Fields.LOCATION).get(IEntity.Fields.ID), locationId);
    }

    default Specification<Landing> hasVesselId(Integer vesselId) {
        if (vesselId == null) return null;
        return (root, query, cb) -> cb.equal(root.get(Landing.Fields.VESSEL).get(IEntity.Fields.ID), vesselId);
    }

    default Specification<Landing> betweenDate(Date startDate, Date endDate) {
        if (startDate == null && endDate == null) return null;
        return (root, query, cb) -> {
            if (startDate != null && endDate != null) {
                return cb.and(
                        cb.greaterThanOrEqualTo(root.get(Landing.Fields.DATE_TIME), startDate),
                        cb.lessThanOrEqualTo(root.get(Landing.Fields.DATE_TIME), endDate)
                );
            } else if (startDate == null && endDate != null) {
                return cb.lessThanOrEqualTo(root.get(Landing.Fields.DATE_TIME), endDate);
            } else {
                return cb.greaterThanOrEqualTo(root.get(Landing.Fields.DATE_TIME), startDate);
            }
        };
    }

    LandingVO toVO(Landing landing);

}

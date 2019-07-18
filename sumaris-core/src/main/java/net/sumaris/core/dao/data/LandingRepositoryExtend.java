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
import net.sumaris.core.vo.filter.LandingFilterVO;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.stereotype.Repository;

import java.util.Date;

@NoRepositoryBean
public interface LandingRepositoryExtend extends IEntityConverter<Landing, LandingVO> {

    default Specification<Landing> hasObservedLocationId(Integer observedLocationId) {
        if (observedLocationId == null) return null;
        return (root, query, cb) -> cb.equal(root.get(Landing.PROPERTY_OBSERVED_LOCATION).get(IEntity.PROPERTY_ID), observedLocationId);
    }

    default Specification<Landing> hasTripId(Integer tripId) {
        if (tripId == null) return null;
        return (root, query, cb) -> cb.equal(root.get(Landing.PROPERTY_TRIP).get(IEntity.PROPERTY_ID), tripId);
    }

    default Specification<Landing> hasLocationId(Integer locationId) {
        if (locationId == null) return null;
        return (root, query, cb) -> cb.equal(root.get(Landing.PROPERTY_LOCATION).get(IEntity.PROPERTY_ID), locationId);
    }

    default Specification<Landing> betweenDate(Date startDate, Date endDate) {
        if (startDate == null && endDate == null) return null;
        return (root, query, cb) -> {
            if (startDate != null && endDate != null) {
                return cb.and(
                        cb.greaterThanOrEqualTo(root.get(Landing.PROPERTY_DATE_TIME), startDate),
                        cb.lessThanOrEqualTo(root.get(Landing.PROPERTY_DATE_TIME), endDate)
                );
            } else if (startDate == null && endDate != null) {
                return cb.lessThanOrEqualTo(root.get(Landing.PROPERTY_DATE_TIME), endDate);
            } else {
                return cb.greaterThanOrEqualTo(root.get(Landing.PROPERTY_DATE_TIME), startDate);
            }
        };
    }

    LandingVO toVO(Landing landing);

}

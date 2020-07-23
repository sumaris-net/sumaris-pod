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
import net.sumaris.core.model.administration.programStrategy.Program;
import net.sumaris.core.model.data.Landing;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.data.LandingVO;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collection;
import java.util.Date;
import java.util.List;

public interface LandingRepositoryExtend {

    default Specification<Landing> hasObservedLocationId(Integer observedLocationId) {
        if (observedLocationId == null) return null;
        return (root, query, cb) -> cb.equal(root.get(Landing.Fields.OBSERVED_LOCATION).get(IEntity.Fields.ID), observedLocationId);
    }

    default Specification<Landing> hasTripId(Integer tripId) {
        if (tripId == null) return null;
        return (root, query, cb) -> cb.equal(root.get(Landing.Fields.TRIP).get(IEntity.Fields.ID), tripId);
    }

    default Specification<Landing> hasRecorderPersonId(Integer personId) {
        if (personId == null) return null;
        return (root, query, cb) -> cb.equal(root.get(Landing.Fields.RECORDER_PERSON).get(IEntity.Fields.ID), personId);
    }

    default Specification<Landing> hasRecorderDepartmentId(Integer depId) {
        if (depId == null) return null;
        return (root, query, cb) -> cb.equal(root.get(Landing.Fields.RECORDER_DEPARTMENT).get(IEntity.Fields.ID), depId);
    }

    default Specification<Landing> hasProgramLabel(String programLabel) {
        if (StringUtils.isBlank(programLabel)) return null;
        return (root, query, cb) -> cb.equal(root.get(Landing.Fields.PROGRAM).get(Program.Fields.LABEL), programLabel);
    }


    default Specification<Landing> hasTripIds(Collection<Integer> tripIds) {
        if (CollectionUtils.isEmpty(tripIds)) return null;
        return (root, query, cb) -> cb.in(root.get(Landing.Fields.TRIP).get(IEntity.Fields.ID)).value(tripIds);
    }

    default Specification<Landing> hasLocationId(Integer locationId) {
        if (locationId == null) return null;
        return (root, query, cb) -> cb.equal(root.get(Landing.Fields.LOCATION).get(IEntity.Fields.ID), locationId);
    }

    default Specification<Landing> hasVesselId(Integer vesselId) {
        if (vesselId == null) return null;
        return (root, query, cb) -> cb.equal(root.get(Landing.Fields.VESSEL).get(IEntity.Fields.ID), vesselId);
    }

    default Specification<Landing> hasExcludeVesselIds(List<Integer> excludeVesselIds) {
        if (CollectionUtils.isEmpty(excludeVesselIds)) return null;
        return (root, query, cb) -> cb.not(root.get(Landing.Fields.VESSEL).get(IEntity.Fields.ID).in(excludeVesselIds));
    }

    default Specification<Landing> betweenDate(Date startDate, Date endDate) {
        if (startDate == null && endDate == null) return null;
        return (root, query, cb) -> {
            // Start + end date
            if (startDate != null && endDate != null) {
                return cb.and(
                    cb.greaterThanOrEqualTo(root.get(Landing.Fields.DATE_TIME), startDate),
                    cb.lessThanOrEqualTo(root.get(Landing.Fields.DATE_TIME), endDate)
                );
            }
            // Start date
            else if (startDate != null) {
                return cb.greaterThanOrEqualTo(root.get(Landing.Fields.DATE_TIME), startDate);
            }
            // End date
            else {
                return cb.lessThanOrEqualTo(root.get(Landing.Fields.DATE_TIME), endDate);
            }
        };
    }

    // Not tested
    List<LandingVO> saveAllByObservedLocationId(int observedLocationId, List<LandingVO> sources);

    List<LandingVO> findAllByTripIds(List<Integer> tripIds);
}

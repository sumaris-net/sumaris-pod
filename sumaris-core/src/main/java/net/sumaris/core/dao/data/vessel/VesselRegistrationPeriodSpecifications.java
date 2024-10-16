package net.sumaris.core.dao.data.vessel;

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

import net.sumaris.core.dao.technical.Daos;
import net.sumaris.core.dao.technical.DatabaseType;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.dao.technical.jpa.BindableSpecification;
import net.sumaris.core.model.data.Vessel;
import net.sumaris.core.model.data.VesselRegistrationPeriod;
import net.sumaris.core.util.ArrayUtils;
import net.sumaris.core.vo.data.VesselRegistrationPeriodVO;
import net.sumaris.core.vo.filter.VesselFilterVO;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.ParameterExpression;
import java.util.*;

public interface VesselRegistrationPeriodSpecifications {

    default Specification<VesselRegistrationPeriod> vesselId(Integer vesselId) {
        if (vesselId == null) return null;
        BindableSpecification<VesselRegistrationPeriod> specification = BindableSpecification.where((root, query, cb) -> {
            ParameterExpression<Integer> param = cb.parameter(Integer.class, VesselFilterVO.Fields.VESSEL_ID);
            return cb.equal(root.get(VesselRegistrationPeriod.Fields.VESSEL).get(Vessel.Fields.ID), param);
        });
        specification.addBind(VesselFilterVO.Fields.VESSEL_ID, vesselId);
        return specification;
    }

    default Specification<VesselRegistrationPeriod> registrationLocationIds(Integer[] registrationIds) {
        if (ArrayUtils.isEmpty(registrationIds)) return null;
        BindableSpecification<net.sumaris.core.model.data.VesselRegistrationPeriod> specification = BindableSpecification.where((root, query, cb) -> {
            ParameterExpression<Collection> param = cb.parameter(Collection.class, VesselFilterVO.Fields.REGISTRATION_LOCATION_IDS);
            return cb.equal(root.get(VesselRegistrationPeriod.Fields.REGISTRATION_LOCATION).get(Vessel.Fields.ID), param);
        });
        specification.addBind(VesselFilterVO.Fields.REGISTRATION_LOCATION_IDS, Arrays.asList(registrationIds));
        return specification;
    }

    default Specification<VesselRegistrationPeriod> atDate(Date date) {
        return betweenDate(date, null);
    }

    default Specification<VesselRegistrationPeriod> betweenDate(Date startDate, Date endDate) {
        if (startDate == null && endDate == null) return null;
        return (root, query, cb) -> {

            // Start + end date
            if (startDate != null && endDate != null) {
                return cb.not(
                    cb.or(
                        cb.lessThan(Daos.nvlEndDate(root.get(VesselRegistrationPeriod.Fields.END_DATE), cb, getDatabaseType()), startDate),
                        cb.greaterThan(root.get(VesselRegistrationPeriod.Fields.START_DATE), endDate)
                    )
                );
            }

            // Start date only
            else if (startDate != null) {
                return cb.greaterThanOrEqualTo(Daos.nvlEndDate(root.get(VesselRegistrationPeriod.Fields.END_DATE), cb, getDatabaseType()), startDate);
            }

            // End date only
            else {
                return cb.not(cb.greaterThan(root.get(VesselRegistrationPeriod.Fields.START_DATE), endDate));
            }
        };
    }

    Optional<VesselRegistrationPeriodVO> findLastByVesselId(int vesselId);

    Specification<VesselRegistrationPeriod> toSpecification(VesselFilterVO filter);

    Optional<VesselRegistrationPeriod> findByVesselIdAndDate(int vesselId, Date date);

    List<VesselRegistrationPeriodVO> findAll(VesselFilterVO filter, Page page);

    DatabaseType getDatabaseType();
}

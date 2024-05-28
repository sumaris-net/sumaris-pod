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

import net.sumaris.core.dao.data.IDataSpecifications;
import net.sumaris.core.dao.data.IValidatableDataSpecifications;
import net.sumaris.core.dao.data.IWithProgramSpecifications;
import net.sumaris.core.dao.data.IWithVesselSpecifications;
import net.sumaris.core.dao.technical.Daos;
import net.sumaris.core.dao.technical.jpa.BindableSpecification;
import net.sumaris.core.model.data.IUseFeaturesEntity;
import net.sumaris.core.model.data.Vessel;
import net.sumaris.core.model.data.VesselFeatures;
import net.sumaris.core.model.data.VesselRegistrationPeriod;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.*;
import java.util.Date;

public interface UseFeaturesSpecifications<E extends IUseFeaturesEntity>
    extends
    IDataSpecifications<Integer, E>,
    IWithVesselSpecifications<Integer, E>,
    IWithProgramSpecifications<Integer, E> {

    String PARENT_ID_PARAM = "parentId";

    default <T> ListJoin<Vessel, VesselRegistrationPeriod> composeVrpJoin(Root<T> root, CriteriaBuilder cb) {
        Join<T, Vessel> vessel = composeVesselJoin(root);
        return composeVrpJoin(vessel, cb, root.get(IUseFeaturesEntity.Fields.START_DATE));
    }

    default <T> ListJoin<Vessel, VesselFeatures> composeVfJoin(Root<T> root, CriteriaBuilder cb) {
        Join<T, Vessel> vessel = composeVesselJoin(root);
        return composeVfJoin(vessel, cb, root.get(IUseFeaturesEntity.Fields.START_DATE));
    }

    default Specification<E> betweenDate(Date startDate, Date endDate) {
        if (startDate == null && endDate == null) return null;
        return (root, query, cb) -> {
            // Start + end date
            if (startDate != null && endDate != null) {
                return cb.not(
                    cb.or(
                        cb.lessThan(Daos.nvlEndDate(root.get(E.Fields.END_DATE), cb, getDatabaseType()), startDate),
                        cb.greaterThan(root.get(E.Fields.START_DATE), endDate)
                    )
                );
            }

            // Start date only
            else if (startDate != null) {
                return cb.greaterThanOrEqualTo(Daos.nvlEndDate(root.get(E.Fields.END_DATE), cb, getDatabaseType()), startDate);
            }

            // End date only
            else {
                return cb.lessThanOrEqualTo(root.get(IUseFeaturesEntity.Fields.START_DATE), endDate);
            }
        };
    }


    default Specification<E> hasParentId(Integer parentId, String parentIdPath) {
        if (parentId == null) return null;
        return BindableSpecification.where((root, query, cb) -> {
            ParameterExpression<Integer> param = cb.parameter(Integer.class, PARENT_ID_PARAM);
            return cb.equal(Daos.composePath(root, parentIdPath), param);
        }).addBind(PARENT_ID_PARAM, parentId);
    }

}

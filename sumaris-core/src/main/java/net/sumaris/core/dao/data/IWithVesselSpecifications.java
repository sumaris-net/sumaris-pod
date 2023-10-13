package net.sumaris.core.dao.data;

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

import net.sumaris.core.dao.referential.IEntitySpecifications;
import net.sumaris.core.dao.technical.Daos;
import net.sumaris.core.dao.technical.jpa.BindableSpecification;
import net.sumaris.core.model.IEntity;
import net.sumaris.core.model.data.*;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.*;
import java.util.Date;

/**
 * @author peck7 on 28/08/2020.
 */
public interface IWithVesselSpecifications<E extends IWithVesselEntity<Integer, Vessel>>
        extends IEntitySpecifications<Integer, E> {

    String VESSEL_ID_PARAM = "vesselId";


    default <T> Join<T, Vessel> composeVesselJoin(Root<T> root) {
        return Daos.composeJoin(root, IWithVesselEntity.Fields.VESSEL, JoinType.INNER);
    }

    default <T> ListJoin<Vessel, VesselRegistrationPeriod> composeVrpJoin(Join<T, Vessel> vessel, CriteriaBuilder cb, Expression<Date> dateExpression) {
        ListJoin<Vessel, VesselRegistrationPeriod> vrp = Daos.composeJoinList(vessel, Vessel.Fields.VESSEL_REGISTRATION_PERIODS, JoinType.LEFT);
        if (vrp.getOn() == null) {
            Predicate vrpCondition = cb.not(
                cb.or(
                    cb.lessThan(Daos.nvlEndDate(vrp, cb, VesselRegistrationPeriod.Fields.END_DATE, getDatabaseType()), dateExpression),
                    cb.greaterThan(vrp.get(VesselRegistrationPeriod.Fields.START_DATE), dateExpression)
                )
            );
            vrp.on(vrpCondition);
        }
        return vrp;
    }

    default <T> ListJoin<Vessel, VesselFeatures> composeVfJoin(Join<T, Vessel> vessel, CriteriaBuilder cb, Expression<Date> dateExpression) {
        ListJoin<Vessel, VesselFeatures> vf = Daos.composeJoinList(vessel, Vessel.Fields.VESSEL_FEATURES, JoinType.LEFT);
        if (vf.getOn() == null) {
            vf.on(
                cb.not(
                    cb.or(
                        cb.lessThan(Daos.nvlEndDate(vf, cb, VesselFeatures.Fields.END_DATE, getDatabaseType()), dateExpression),
                        cb.greaterThan(vf.get(VesselFeatures.Fields.START_DATE), dateExpression)
                    )
                ));
        }
        return vf;
    }


    default Specification<E> hasVesselId(Integer vesselId) {
        if (vesselId == null) return null;
        return BindableSpecification.<E>where((root, query, cb) -> {
            ParameterExpression<Integer> param = cb.parameter(Integer.class, VESSEL_ID_PARAM);
            Join<E, Vessel> vessel = composeVesselJoin(root);
            return cb.equal(vessel.get(IEntity.Fields.ID), param);
        }).addBind(VESSEL_ID_PARAM, vesselId);
    }

}

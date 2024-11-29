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
import net.sumaris.core.model.data.IWithVesselEntity;
import net.sumaris.core.model.data.Vessel;
import net.sumaris.core.model.data.VesselFeatures;
import net.sumaris.core.model.data.VesselRegistrationPeriod;
import net.sumaris.core.util.StringUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.*;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;

/**
 * @author peck7 on 28/08/2020.
 */
public interface IWithVesselSpecifications<ID extends Serializable, E extends IWithVesselEntity<ID, Vessel>>
        extends IEntitySpecifications<ID, E> {

    String VESSEL_IDS_PARAM = "vesselIds";
    String VESSEL_TYPE_IDS_PARAM = "vesselTypeIds";

    default <T> Join<T, Vessel> composeVesselJoin(Root<T> root) {
        return Daos.composeJoin(root, IWithVesselEntity.Fields.VESSEL, JoinType.INNER);
    }

    default <T> ListJoin<Vessel, VesselRegistrationPeriod> composeVrpJoin(Join<T, Vessel> vessel, CriteriaBuilder cb, Expression<Date> dateExpression) {
        return composeVrpJoin(vessel, cb, dateExpression, JoinType.LEFT);
    }

    default <T> ListJoin<Vessel, VesselRegistrationPeriod> composeVrpJoin(Join<T, Vessel> vessel, CriteriaBuilder cb, Expression<Date> dateExpression, JoinType joinType) {
        return composeVrpJoinBetweenDate(vessel, cb, dateExpression, dateExpression, joinType);
    }

    default <T> ListJoin<Vessel, VesselRegistrationPeriod> composeVrpJoinBetweenDate(From<?, Vessel> vessel, CriteriaBuilder cb,
                                                                                     Expression<Date> startDateExpression,
                                                                                     Expression<Date> endDateExpression,
                                                                                     JoinType joinType) {
        ListJoin<Vessel, VesselRegistrationPeriod> vrp = Daos.composeJoinList(vessel, Vessel.Fields.VESSEL_REGISTRATION_PERIODS, joinType);
        if (vrp.getOn() == null && startDateExpression != null) {
            Predicate vrpOnCondition = vrpBetweenDatePredicate(vrp, cb, startDateExpression, endDateExpression);
            vrp.on(vrpOnCondition);
        }
        return vrp;
    }

    default Predicate vrpBetweenDatePredicate(From<?, VesselRegistrationPeriod> vrp,
                                              CriteriaBuilder cb,
                                              Expression<Date> startDateExpression,
                                              Expression<Date> endDateExpression) {
        if (startDateExpression == null)  return null;
        if (endDateExpression == null) endDateExpression = startDateExpression;

        return cb.not(
            cb.or(
                cb.lessThan(Daos.nvlEndDate(vrp.get(VesselRegistrationPeriod.Fields.END_DATE), cb, getDatabaseType()), startDateExpression),
                cb.greaterThan(vrp.get(VesselRegistrationPeriod.Fields.START_DATE), endDateExpression)
            )
        );
    }

    default ListJoin<Vessel, VesselFeatures> composeVfJoin(Join<?, Vessel> vessel, CriteriaBuilder cb, Expression<Date> dateExpression) {
        ListJoin<Vessel, VesselFeatures> vf = Daos.composeJoinList(vessel, Vessel.Fields.VESSEL_FEATURES, JoinType.LEFT);
        if (vf.getOn() == null && dateExpression != null) {
            vf.on(
                cb.not(
                    cb.or(
                        cb.lessThan(Daos.nvlEndDate(vf.get(VesselFeatures.Fields.END_DATE), cb, getDatabaseType()), dateExpression),
                        cb.greaterThan(vf.get(VesselFeatures.Fields.START_DATE), dateExpression)
                    )
                ));
        }
        return vf;
    }


    default Specification<E> hasVesselId(Integer vesselId) {
        if (vesselId == null) return null;
        return hasVesselIds(new Integer[]{vesselId});
    }

    default Specification<E> hasVesselIds(Integer[] vesselIds) {
        if (ArrayUtils.isEmpty(vesselIds)) return null;
        return BindableSpecification.<E>where((root, query, cb) -> {
            ParameterExpression<Collection> param = cb.parameter(Collection.class, VESSEL_IDS_PARAM);
            Join<E, Vessel> vessel = composeVesselJoin(root);
            return cb.in(vessel.get(Vessel.Fields.ID)).value(param);
        })
        .addBind(VESSEL_IDS_PARAM, Arrays.asList(vesselIds));
    }

    default Specification<E> hasVesselTypeIds(Integer[] vesselTypeIds) {
        if (ArrayUtils.isEmpty(vesselTypeIds)) return null;
        return BindableSpecification.<E>where((root, query, cb) -> {
            ParameterExpression<Collection> param = cb.parameter(Collection.class, VESSEL_TYPE_IDS_PARAM);
            Join<E, Vessel> vessel = composeVesselJoin(root);
            return cb.in(Daos.composePath(vessel, StringUtils.doting(Vessel.Fields.VESSEL_TYPE, IEntity.Fields.ID))).value(param);
        }).addBind(VESSEL_TYPE_IDS_PARAM, Arrays.asList(vesselTypeIds));
    }
}

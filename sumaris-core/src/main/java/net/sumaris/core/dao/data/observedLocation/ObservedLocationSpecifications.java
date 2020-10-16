package net.sumaris.core.dao.data.observedLocation;

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

import net.sumaris.core.dao.data.RootDataSpecifications;
import net.sumaris.core.dao.technical.jpa.BindableSpecification;
import net.sumaris.core.dao.technical.model.IEntity;
import net.sumaris.core.model.data.ObservedLocation;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.ParameterExpression;
import java.util.Date;

/**
 * @author peck7 on 31/08/2020.
 */
public interface ObservedLocationSpecifications extends RootDataSpecifications<ObservedLocation> {

    String LOCATION_ID_PARAM = "locationId";
    String START_DATE_PARAM = "startDate";
    String END_DATE_PARAM = "endDate";

    default Specification<ObservedLocation> hasLocationId(Integer locationId) {
        BindableSpecification<ObservedLocation> specification = BindableSpecification.where((root, query, criteriaBuilder) -> {
            ParameterExpression<Integer> param = criteriaBuilder.parameter(Integer.class, LOCATION_ID_PARAM);
            return criteriaBuilder.or(
                criteriaBuilder.isNull(param),
                criteriaBuilder.equal(root.get(ObservedLocation.Fields.LOCATION).get(IEntity.Fields.ID), param)
            );
        });
        specification.addBind(LOCATION_ID_PARAM, locationId);
        return specification;
    }

    default Specification<ObservedLocation> withStartDate(Date startDate) {
        BindableSpecification<ObservedLocation> specification = BindableSpecification.where((root, query, criteriaBuilder) -> {
            ParameterExpression<Date> param = criteriaBuilder.parameter(Date.class, START_DATE_PARAM);
            return criteriaBuilder.or(
                criteriaBuilder.isNull(param),
                criteriaBuilder.greaterThanOrEqualTo(root.get(ObservedLocation.Fields.END_DATE_TIME), param)
            );
        });
        specification.addBind(START_DATE_PARAM, startDate);
        return specification;
    }

    default Specification<ObservedLocation> withEndDate(Date endDate) {
        BindableSpecification<ObservedLocation> specification = BindableSpecification.where((root, query, criteriaBuilder) -> {
            ParameterExpression<Date> param = criteriaBuilder.parameter(Date.class, END_DATE_PARAM);
            return criteriaBuilder.or(
                criteriaBuilder.isNull(param),
                criteriaBuilder.lessThanOrEqualTo(root.get(ObservedLocation.Fields.START_DATE_TIME), param)
            );
        });
        specification.addBind(END_DATE_PARAM, endDate);
        return specification;
    }


}

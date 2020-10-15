package net.sumaris.core.dao.data.operation;

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

import com.google.common.base.Preconditions;
import net.sumaris.core.dao.data.DataSpecifications;
import net.sumaris.core.dao.technical.jpa.BindableSpecification;
import net.sumaris.core.dao.technical.model.IEntity;
import net.sumaris.core.model.data.Operation;
import net.sumaris.core.model.data.Trip;
import net.sumaris.core.vo.data.OperationGroupVO;
import net.sumaris.core.vo.filter.OperationGroupFilterVO;
import net.sumaris.core.vo.referential.MetierVO;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.ParameterExpression;
import java.util.Date;
import java.util.List;

/**
 * @author peck7 on 01/09/2020.
 */
public interface OperationGroupSpecifications
    extends DataSpecifications<Operation> {

    String TRIP_ID_PARAM = "tripId";

    default Specification<Operation> filter(OperationGroupFilterVO filter) {
        Preconditions.checkNotNull(filter);
        Preconditions.checkNotNull(filter.getTripId());
        BindableSpecification<Operation> specification = BindableSpecification.where((root, query, criteriaBuilder) -> {
            query.orderBy(criteriaBuilder.asc(root.get(Operation.Fields.RANK_ORDER_ON_PERIOD))); // Default sort
            ParameterExpression<Integer> param = criteriaBuilder.parameter(Integer.class, TRIP_ID_PARAM);
            if (filter.isOnlyUndefined()) {
                return criteriaBuilder.and(
                    criteriaBuilder.equal(root.get(Operation.Fields.TRIP).get(IEntity.Fields.ID), param),
                    criteriaBuilder.equal(root.get(Operation.Fields.START_DATE_TIME), root.get(Operation.Fields.TRIP).get(Trip.Fields.DEPARTURE_DATE_TIME)),
                    criteriaBuilder.equal(root.get(Operation.Fields.END_DATE_TIME), root.get(Operation.Fields.TRIP).get(Trip.Fields.RETURN_DATE_TIME))
                );
            } else if (filter.isOnlyDefined()) {
                return criteriaBuilder.and(
                    criteriaBuilder.equal(root.get(Operation.Fields.TRIP).get(IEntity.Fields.ID), param),
                    criteriaBuilder.notEqual(root.get(Operation.Fields.START_DATE_TIME), root.get(Operation.Fields.TRIP).get(Trip.Fields.DEPARTURE_DATE_TIME))
                );
            } else {
                return criteriaBuilder.equal(root.get(Operation.Fields.TRIP).get(IEntity.Fields.ID), param);
            }
        });
        specification.addBind(TRIP_ID_PARAM, filter.getTripId());
        return specification;
    }

    List<OperationGroupVO> saveAllByTripId(int tripId, List<OperationGroupVO> operationGroups);

    void updateUndefinedOperationDates(int tripId, Date startDate, Date endDate);

    /**
     * Get metier ( = operations with same start and end date as trip)
     *
     * @param tripId trip id
     * @return metiers of trip
     */
    List<MetierVO> getMetiersByTripId(int tripId);

    List<MetierVO> saveMetiersByTripId(int tripId, List<MetierVO> metiers);
}

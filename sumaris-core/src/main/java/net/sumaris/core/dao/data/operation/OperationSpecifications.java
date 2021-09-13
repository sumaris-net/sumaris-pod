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

import net.sumaris.core.dao.data.DataSpecifications;
import net.sumaris.core.dao.technical.jpa.BindableSpecification;
import net.sumaris.core.dao.technical.model.IEntity;
import net.sumaris.core.model.data.Operation;
import net.sumaris.core.model.data.Trip;
import net.sumaris.core.model.referential.IItemReferentialEntity;
import net.sumaris.core.vo.data.OperationVO;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.ParameterExpression;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * @author peck7 on 01/09/2020.
 */
public interface OperationSpecifications
        extends DataSpecifications<Operation> {

    String TRIP_ID_PARAM = "tripId";
    String VESSEL_ID_PARAM = "vesselId";
    String INCLUDED_IDS_PARAMETER = "includedIds";
    String EXCLUDED_IDS_PARAMETER = "excludedIds";
    String PROGRAM_LABEL_PARAM = "programLabel";
    String EXCLUDE_CHILD_OPERATION_PARAM = "excludeChildOperation";
    String HAS_NO_CHILD_OPERATION_PARAM = "hasNoChildOperation";
    String START_DATE_PARAM = "startDate";
    String END_DATE_PARAM = "endDate";

    default Specification<Operation> hasTripId(Integer tripId) {
        if (tripId == null) return null;
        BindableSpecification<Operation> specification = BindableSpecification.where((root, query, criteriaBuilder) -> {
            ParameterExpression<Integer> param = criteriaBuilder.parameter(Integer.class, TRIP_ID_PARAM);
            return criteriaBuilder.or(
                    criteriaBuilder.isNull(param),
                    criteriaBuilder.equal(root.get(Operation.Fields.TRIP).get(IEntity.Fields.ID), param)
            );
        });
        specification.addBind(TRIP_ID_PARAM, tripId);
        return specification;
    }

    default Specification<Operation> includedIds(Integer[] includedIds) {
        if (ArrayUtils.isEmpty(includedIds)) return null;
        return BindableSpecification.<Operation>where((root, query, criteriaBuilder) -> {
            ParameterExpression<Collection> param = criteriaBuilder.parameter(Collection.class, INCLUDED_IDS_PARAMETER);
            return criteriaBuilder.in(root.get(IEntity.Fields.ID)).value(param);
        })
                .addBind(INCLUDED_IDS_PARAMETER, Arrays.asList(includedIds));
    }

    default Specification<Operation> excludedIds(Integer[] excludedIds) {
        if (ArrayUtils.isEmpty(excludedIds)) return null;
        return BindableSpecification.<Operation>where((root, query, criteriaBuilder) -> {
            ParameterExpression<Collection> param = criteriaBuilder.parameter(Collection.class, EXCLUDED_IDS_PARAMETER);
            return criteriaBuilder.not(
                    criteriaBuilder.in(root.get(IEntity.Fields.ID)).value(param)
            );
        })
                .addBind(EXCLUDED_IDS_PARAMETER, Arrays.asList(excludedIds));
    }

    default Specification<Operation> hasProgramLabel(String programLabel) {
        BindableSpecification<Operation> specification = BindableSpecification.where((root, query, criteriaBuilder) -> {
            ParameterExpression<String> param = criteriaBuilder.parameter(String.class, PROGRAM_LABEL_PARAM);
            Join<Operation, Trip> tripJoin = root.join(Operation.Fields.TRIP, JoinType.INNER);
            return criteriaBuilder.or(
                    criteriaBuilder.isNull(param),
                    criteriaBuilder.equal(tripJoin.get(Trip.Fields.PROGRAM).get(IItemReferentialEntity.Fields.LABEL), param)
            );
        });
        specification.addBind(PROGRAM_LABEL_PARAM, programLabel);
        return specification;
    }

    default Specification<Operation> hasVesselId(Integer vesselId) {
        if (vesselId == null) return null;
        BindableSpecification<Operation> specification = BindableSpecification.where((root, query, criteriaBuilder) -> {
            ParameterExpression<Integer> param = criteriaBuilder.parameter(Integer.class, VESSEL_ID_PARAM);
            Join<Operation, Trip> tripJoin = root.join(Operation.Fields.TRIP, JoinType.INNER);
            return criteriaBuilder.or(
                    criteriaBuilder.isNull(param),
                    criteriaBuilder.equal(tripJoin.get(Trip.Fields.VESSEL).get(IEntity.Fields.ID), param)
            );
        });
        specification.addBind(VESSEL_ID_PARAM, vesselId);
        return specification;
    }

    default Specification<Operation> notChildOperation(Boolean excludeChildOperation) {
        BindableSpecification<Operation> specification = BindableSpecification.where((root, query, criteriaBuilder) -> {
                    ParameterExpression<Boolean> param = criteriaBuilder.parameter(Boolean.class, EXCLUDE_CHILD_OPERATION_PARAM);
                    return criteriaBuilder.or(
                            criteriaBuilder.isNull(param),
                            criteriaBuilder.isFalse(param),
                            criteriaBuilder.isNull(root.get(Operation.Fields.PARENT_OPERATION)));
                }
        );

        specification.addBind(EXCLUDE_CHILD_OPERATION_PARAM, excludeChildOperation);
        return specification;
    }

    default Specification<Operation> hasNoChildOperation(Boolean hasNotChildOperation) {
        BindableSpecification<Operation> specification = BindableSpecification.where((root, query, criteriaBuilder) -> {
                    ParameterExpression<Boolean> param = criteriaBuilder.parameter(Boolean.class, HAS_NO_CHILD_OPERATION_PARAM);
                    Join<Operation, Operation> operationJoin = root.join(Operation.Fields.CHILD_OPERATION, JoinType.LEFT);

                    return criteriaBuilder.or(
                            criteriaBuilder.isNull(param),
                            criteriaBuilder.isFalse(param),
                            criteriaBuilder.isNull(operationJoin));
                }
        );

        specification.addBind(HAS_NO_CHILD_OPERATION_PARAM, hasNotChildOperation);
        return specification;
    }


    default Specification<Operation> isBetweenDates(Date startDate, Date endDate) {
        BindableSpecification<Operation> specification = BindableSpecification.where((root, query, criteriaBuilder) -> {
                    ParameterExpression<Date> startDateparam = criteriaBuilder.parameter(Date.class, START_DATE_PARAM);
                    ParameterExpression<Date> endDateparam = criteriaBuilder.parameter(Date.class, END_DATE_PARAM);

                    return criteriaBuilder.and(
                            criteriaBuilder.or(
                                    criteriaBuilder.isNull(startDateparam.as(String.class)),
                                    criteriaBuilder.or(
                                            criteriaBuilder.and(
                                                    criteriaBuilder.isNotNull(root.get(Operation.Fields.END_DATE_TIME)),
                                                    criteriaBuilder.greaterThan(root.get(Operation.Fields.END_DATE_TIME), startDateparam)
                                            ),
                                            criteriaBuilder.and(
                                                    criteriaBuilder.isNotNull(root.get(Operation.Fields.FISHING_START_DATE_TIME)),
                                                    criteriaBuilder.greaterThan(root.get(Operation.Fields.FISHING_START_DATE_TIME), startDateparam)
                                            )
                                    )
                            ),
                            criteriaBuilder.or(
                                    criteriaBuilder.isNull(endDateparam.as(String.class)),
                                    criteriaBuilder.or(
                                            criteriaBuilder.and(
                                                    criteriaBuilder.isNotNull(root.get(Operation.Fields.END_DATE_TIME)),
                                                    criteriaBuilder.lessThan(root.get(Operation.Fields.END_DATE_TIME), endDateparam)
                                            ),
                                            criteriaBuilder.and(
                                                    criteriaBuilder.isNotNull(root.get(Operation.Fields.FISHING_START_DATE_TIME)),
                                                    criteriaBuilder.lessThan(root.get(Operation.Fields.FISHING_START_DATE_TIME), endDateparam)
                                            )
                                    )
                            )
                    );
                }
        );

        specification.addBind(START_DATE_PARAM, startDate);
        specification.addBind(END_DATE_PARAM, endDate);
        return specification;
    }

    List<OperationVO> saveAllByTripId(int tripId, List<OperationVO> operations);
}

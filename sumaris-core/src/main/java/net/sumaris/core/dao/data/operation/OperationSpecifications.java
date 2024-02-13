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

import net.sumaris.core.dao.data.IDataSpecifications;
import net.sumaris.core.dao.technical.Daos;
import net.sumaris.core.dao.technical.jpa.BindableSpecification;
import net.sumaris.core.model.IEntity;
import net.sumaris.core.model.administration.programStrategy.Program;
import net.sumaris.core.model.data.*;
import net.sumaris.core.model.referential.metier.Metier;
import net.sumaris.core.model.referential.taxon.TaxonGroup;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.data.OperationVO;
import net.sumaris.core.vo.filter.OperationFilterVO;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * @author peck7 on 01/09/2020.
 */
public interface OperationSpecifications
    extends IDataSpecifications<Integer, Operation> {

    String TRIP_ID_PARAM = "tripId";
    String PROGRAM_LABEL_PARAM = "programLabel";
    String START_DATE_PARAM = "startDate";
    String END_DATE_PARAM = "endDate";
    String START_DATE_PARAM_IS_NULL = "startDateIsNull";
    String END_DATE_PARAM_IS_NULL = "endDateIsNull";
    String GEAR_IDS_PARAMETER = "gearIds";
    String PHYSICAL_GEAR_IDS_PARAMETER = "physicalGearIds";
    String TAXON_GROUP_LABELS_PARAM = "targetSpecieIds";

    default Specification<Operation> excludeOperationGroup() {
        return BindableSpecification.where((root, query, cb) -> {
            Join<Operation, Trip> tripJoin = Daos.composeJoin(root, Operation.Fields.TRIP, JoinType.INNER);
            return cb.not(
                cb.and(
                    cb.equal(root.get(Operation.Fields.START_DATE_TIME), tripJoin.get(Trip.Fields.DEPARTURE_DATE_TIME)),
                    cb.equal(root.get(Operation.Fields.END_DATE_TIME), tripJoin.get(Trip.Fields.RETURN_DATE_TIME))
                )
            );
        });
    }

    default Specification<Operation> distinct() {
        return BindableSpecification.where((root, query, cb) -> {
            query.distinct(true);
            return cb.conjunction();
        });
    }

    default Specification<Operation> hasTripId(Integer tripId) {
        if (tripId == null) return null;
        return BindableSpecification.where((root, query, cb) -> {
            Join<Operation, Trip> tripJoin = Daos.composeJoin(root, Operation.Fields.TRIP, JoinType.INNER);
            ParameterExpression<Integer> param = cb.parameter(Integer.class, TRIP_ID_PARAM);
            return cb.equal(tripJoin.get(IEntity.Fields.ID), param);
        }).addBind(TRIP_ID_PARAM, tripId);
    }



    default Specification<Operation> hasProgramLabel(String programLabel) {
        if (StringUtils.isBlank(programLabel)) return null;
        return BindableSpecification.where((root, query, cb) -> {
                ParameterExpression<String> param = cb.parameter(String.class, PROGRAM_LABEL_PARAM);
                return cb.equal(
                    Daos.composePath(root, StringUtils.doting(Operation.Fields.TRIP, Trip.Fields.PROGRAM, Program.Fields.LABEL)),
                    param);
            })
            .addBind(PROGRAM_LABEL_PARAM, programLabel);
    }

    default Specification<Operation> hasVesselId(Integer vesselId) {
        if (vesselId == null) return null;
        return BindableSpecification.where((root, query, cb) -> {
                ParameterExpression<Integer> param = cb.parameter(Integer.class, OperationFilterVO.Fields.VESSEL_ID);
                return cb.equal(
                    Daos.composePath(root, StringUtils.doting(Operation.Fields.TRIP, Trip.Fields.VESSEL, Vessel.Fields.ID)),
                    param);
            })
            .addBind(OperationFilterVO.Fields.VESSEL_ID, vesselId);
    }

    default Specification<Operation> hasVesselIds(Integer... vesselIds) {
        if (ArrayUtils.isEmpty(vesselIds)) return null;
        return BindableSpecification.where((root, query, cb) -> {
                ParameterExpression<Collection> param = cb.parameter(Collection.class, OperationFilterVO.Fields.VESSEL_IDS);
                return cb.in(
                    Daos.composePath(root, StringUtils.doting(Operation.Fields.TRIP, Trip.Fields.VESSEL, Vessel.Fields.ID)))
                        .value(param);
            })
            .addBind(OperationFilterVO.Fields.VESSEL_IDS, Arrays.asList(vesselIds));
    }

    default Specification<Operation> excludeChildOperation(Boolean excludeChildOperation) {
        if (excludeChildOperation == null || !excludeChildOperation) return null;
        return BindableSpecification.where((root, query, cb) ->
            cb.isNull(Daos.composePath(root, Operation.Fields.PARENT_OPERATION))
        );
    }

    default Specification<Operation> hasNoChildOperation(Boolean hasNotChildOperation) {
        if (hasNotChildOperation == null || !hasNotChildOperation) return null;

        return BindableSpecification.where((root, query, cb) ->
            cb.isNull(Daos.composeJoin(root, Operation.Fields.CHILD_OPERATION, JoinType.LEFT))
        );
    }

    default Specification<Operation> inGearIds(Integer[] gearIds) {
        if (ArrayUtils.isEmpty(gearIds)) return null;
        return BindableSpecification.<Operation>where((root, query, cb) -> {
                ParameterExpression<Collection> param = cb.parameter(Collection.class, GEAR_IDS_PARAMETER);
                return cb.in(
                    Daos.composePath(root, StringUtils.doting(Operation.Fields.PHYSICAL_GEAR, PhysicalGear.Fields.GEAR, PhysicalGear.Fields.ID),
                        JoinType.INNER)
                ).value(param);
            })
            .addBind(GEAR_IDS_PARAMETER, Arrays.asList(gearIds));
    }

    default Specification<Operation> inPhysicalGearIds(Integer[] physicalGearIds) {
        if (ArrayUtils.isEmpty(physicalGearIds)) return null;
        return BindableSpecification.<Operation>where((root, query, cb) -> {
                Join<Operation, PhysicalGear> physicalGearJoin = Daos.composeJoin(root, Operation.Fields.PHYSICAL_GEAR, JoinType.INNER);
                ParameterExpression<Collection> param = cb.parameter(Collection.class, PHYSICAL_GEAR_IDS_PARAMETER);
                return cb.in(physicalGearJoin.get(IEntity.Fields.ID)).value(param);
            })
            .addBind(PHYSICAL_GEAR_IDS_PARAMETER, Arrays.asList(physicalGearIds));
    }

    default Specification<Operation> inTaxonGroupLabels(String[] taxonGroupLabels) {
        if (ArrayUtils.isEmpty(taxonGroupLabels)) return null;
        return BindableSpecification.<Operation>where((root, query, cb) -> {
                ParameterExpression<Collection> param = cb.parameter(Collection.class, TAXON_GROUP_LABELS_PARAM);
                return cb.in(
                    Daos.composePath(root, StringUtils.doting(Operation.Fields.METIER, Metier.Fields.TAXON_GROUP, TaxonGroup.Fields.LABEL))
                ).value(param);
            })
            .addBind(TAXON_GROUP_LABELS_PARAM, Arrays.asList(taxonGroupLabels));
    }

    default Specification<Operation> isBetweenDates(Date startDate, Date endDate) {
        if (startDate == null && endDate == null) return null;
        return BindableSpecification.where((root, query, cb) -> {
                ParameterExpression<Date> startDateParam = cb.parameter(Date.class, START_DATE_PARAM);
                ParameterExpression<Boolean> startDateParamIsNull = cb.parameter(Boolean.class, START_DATE_PARAM_IS_NULL);
                ParameterExpression<Date> endDateParam = cb.parameter(Date.class, END_DATE_PARAM);
                ParameterExpression<Boolean> endDateParamIsNull = cb.parameter(Boolean.class, END_DATE_PARAM_IS_NULL);

                // TODO BLA: review this code
                // - use NOT(condition) ?
                // - Use fishingStartDate only if parent Operation ? or make sure to store
                return cb.and(
                    cb.or(
                        cb.isTrue(startDateParamIsNull),
                        cb.or(
                            cb.and(
                                cb.isNotNull(root.get(Operation.Fields.END_DATE_TIME)),
                                cb.greaterThan(root.get(Operation.Fields.END_DATE_TIME), startDateParam)
                            ),
                            cb.and(
                                cb.isNotNull(root.get(Operation.Fields.FISHING_START_DATE_TIME)),
                                cb.greaterThan(root.get(Operation.Fields.FISHING_START_DATE_TIME), startDateParam)
                            )
                        )
                    ),
                    cb.or(
                        cb.isTrue(endDateParamIsNull),
                        cb.or(
                            cb.and(
                                cb.isNotNull(root.get(Operation.Fields.END_DATE_TIME)),
                                cb.lessThan(root.get(Operation.Fields.END_DATE_TIME), endDateParam)
                            ),
                            cb.and(
                                cb.isNotNull(root.get(Operation.Fields.FISHING_START_DATE_TIME)),
                                cb.lessThan(root.get(Operation.Fields.FISHING_START_DATE_TIME), endDateParam)
                            )
                        )
                    )
                );
            })
            .addBind(START_DATE_PARAM, startDate)
            .addBind(START_DATE_PARAM_IS_NULL, startDate == null ? Boolean.TRUE : Boolean.FALSE)
            .addBind(END_DATE_PARAM, endDate)
            .addBind(END_DATE_PARAM_IS_NULL, endDate == null ? Boolean.TRUE : Boolean.FALSE);
    }

    default Specification<Operation> needBatchDenormalization(Boolean needBatchDenormalization) {
        if (!Boolean.TRUE.equals(needBatchDenormalization)) return null;

        return BindableSpecification.where((root, query, cb) -> {

            Join<Operation, Batch> catchBatch = Daos.composeJoin(root, Operation.Fields.BATCHES, JoinType.INNER);

            // Sub select that return the update to date denormalized catch batch
            Subquery<Integer> subQuery = query.subquery(Integer.class);
            Root<DenormalizedBatch> denormalizedBatchRoot = subQuery.from(DenormalizedBatch.class);
            subQuery.select(denormalizedBatchRoot.get(DenormalizedBatch.Fields.ID));
            subQuery.where(
                cb.and(
                    // Catch batch
                    cb.isNull(denormalizedBatchRoot.get(DenormalizedBatch.Fields.PARENT)),
                    // Same operation
                    cb.equal(denormalizedBatchRoot.get(DenormalizedBatch.Fields.OPERATION), root),
                    // Same catch batch
                    cb.equal(denormalizedBatchRoot.get(DenormalizedBatch.Fields.ID), catchBatch.get(Batch.Fields.ID)),
                    // Same date
                    cb.equal(denormalizedBatchRoot.get(DenormalizedBatch.Fields.UPDATE_DATE), catchBatch.get(Batch.Fields.UPDATE_DATE))
                )
            );

            return cb.and(
                // Operation with a catch batch
                cb.isNull(catchBatch.get(Batch.Fields.PARENT)),
                // And without an update to date denormalization
                cb.not(cb.exists(subQuery))
            );
        });
    }



    // Override the default function, because operation has no validation date
    default Specification<Operation> isValidated() {
        return (root, query, cb) ->
            // Trip's validation date must not be not null
            cb.isNotNull(Daos.composePath(root, StringUtils.doting(Operation.Fields.TRIP, Trip.Fields.VALIDATION_DATE)));
    }

    List<OperationVO> saveAllByTripId(int tripId, List<OperationVO> operations);
}

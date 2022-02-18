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
import net.sumaris.core.dao.technical.Daos;
import net.sumaris.core.dao.technical.jpa.BindableSpecification;
import net.sumaris.core.dao.technical.model.IEntity;
import net.sumaris.core.model.administration.programStrategy.Program;
import net.sumaris.core.model.data.Operation;
import net.sumaris.core.model.data.PhysicalGear;
import net.sumaris.core.model.data.Trip;
import net.sumaris.core.model.data.Vessel;
import net.sumaris.core.model.referential.QualityFlag;
import net.sumaris.core.model.referential.metier.Metier;
import net.sumaris.core.model.referential.taxon.TaxonGroup;
import net.sumaris.core.util.StringUtils;
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
    String START_DATE_PARAM = "startDate";
    String END_DATE_PARAM = "endDate";
    String GEAR_IDS_PARAMETER = "gearIds";
    String PHYSICAL_GEAR_IDS_PARAMETER = "physicalGearIds";
    String TAXON_GROUP_LABELS_PARAM = "targetSpecieIds";
    String QUALITY_FLAG_ID_PARAM = "qualityFlagId";

    default Specification<Operation> excludeOperationGroup() {
        return BindableSpecification.where((root, query, criteriaBuilder) -> {
            Join<Operation, Trip> tripJoin = Daos.composeJoin(root, Operation.Fields.TRIP, JoinType.INNER);
            return criteriaBuilder.not(
                criteriaBuilder.and(
                    criteriaBuilder.equal(root.get(Operation.Fields.START_DATE_TIME), tripJoin.get(Trip.Fields.DEPARTURE_DATE_TIME)),
                    criteriaBuilder.equal(root.get(Operation.Fields.END_DATE_TIME), tripJoin.get(Trip.Fields.RETURN_DATE_TIME))
                )
            );
        });
    }

    default Specification<Operation> hasTripId(Integer tripId) {
        if (tripId == null) return null;
        return BindableSpecification.where((root, query, criteriaBuilder) -> {
            Join<Operation, Trip> tripJoin = Daos.composeJoin(root, Operation.Fields.TRIP, JoinType.INNER);
            ParameterExpression<Integer> param = criteriaBuilder.parameter(Integer.class, TRIP_ID_PARAM);
            return criteriaBuilder.equal(tripJoin.get(IEntity.Fields.ID), param);
        }).addBind(TRIP_ID_PARAM, tripId);
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
        if (StringUtils.isBlank(programLabel)) return null;
        return BindableSpecification.where((root, query, criteriaBuilder) -> {
                ParameterExpression<String> param = criteriaBuilder.parameter(String.class, PROGRAM_LABEL_PARAM);
                return criteriaBuilder.equal(
                    Daos.composePath(root, StringUtils.doting(Operation.Fields.TRIP, Trip.Fields.PROGRAM, Program.Fields.LABEL)),
                    param);
            })
            .addBind(PROGRAM_LABEL_PARAM, programLabel);
    }

    default Specification<Operation> hasVesselId(Integer vesselId) {
        if (vesselId == null) return null;
        return BindableSpecification.where((root, query, criteriaBuilder) -> {
                ParameterExpression<Integer> param = criteriaBuilder.parameter(Integer.class, VESSEL_ID_PARAM);
                return criteriaBuilder.equal(
                    Daos.composePath(root, StringUtils.doting(Operation.Fields.TRIP, Trip.Fields.VESSEL, Vessel.Fields.ID)),
                    param);
            })
            .addBind(VESSEL_ID_PARAM, vesselId);
    }

    default Specification<Operation> excludeChildOperation(Boolean excludeChildOperation) {
        if (excludeChildOperation == null || !excludeChildOperation) return null;
        return BindableSpecification.where((root, query, criteriaBuilder) ->
            criteriaBuilder.isNull(root.get(Operation.Fields.PARENT_OPERATION))
        );
    }

    default Specification<Operation> hasNoChildOperation(Boolean hasNotChildOperation) {
        if (hasNotChildOperation == null || !hasNotChildOperation) return null;

        return BindableSpecification.where((root, query, criteriaBuilder) ->
            criteriaBuilder.isNull(Daos.composeJoin(root, Operation.Fields.CHILD_OPERATION, JoinType.LEFT))
        );
    }

    default Specification<Operation> inGearIds(Integer[] gearIds) {
        if (ArrayUtils.isEmpty(gearIds)) return null;
        return BindableSpecification.<Operation>where((root, query, criteriaBuilder) -> {
                ParameterExpression<Collection> param = criteriaBuilder.parameter(Collection.class, GEAR_IDS_PARAMETER);
                return criteriaBuilder.in(
                    Daos.composePath(root, StringUtils.doting(Operation.Fields.PHYSICAL_GEAR, PhysicalGear.Fields.GEAR, PhysicalGear.Fields.ID),
                        JoinType.INNER)
                ).value(param);
            })
            .addBind(GEAR_IDS_PARAMETER, Arrays.asList(gearIds));
    }

    default Specification<Operation> inPhysicalGearIds(Integer[] physicalGearIds) {
        if (ArrayUtils.isEmpty(physicalGearIds)) return null;
        return BindableSpecification.<Operation>where((root, query, criteriaBuilder) -> {
                Join<Operation, PhysicalGear> physicalGearJoin = Daos.composeJoin(root, Operation.Fields.PHYSICAL_GEAR, JoinType.INNER);
                ParameterExpression<Collection> param = criteriaBuilder.parameter(Collection.class, PHYSICAL_GEAR_IDS_PARAMETER);
                return criteriaBuilder.in(physicalGearJoin.get(IEntity.Fields.ID)).value(param);
            })
            .addBind(PHYSICAL_GEAR_IDS_PARAMETER, Arrays.asList(physicalGearIds));
    }

    default Specification<Operation> inTaxonGroupLabels(String[] taxonGroupLabels) {
        if (ArrayUtils.isEmpty(taxonGroupLabels)) return null;
        return BindableSpecification.<Operation>where((root, query, criteriaBuilder) -> {
                ParameterExpression<Collection> param = criteriaBuilder.parameter(Collection.class, TAXON_GROUP_LABELS_PARAM);
                return criteriaBuilder.in(
                    Daos.composePath(root, StringUtils.doting(Operation.Fields.METIER, Metier.Fields.TAXON_GROUP, TaxonGroup.Fields.LABEL))
                ).value(param);
            })
            .addBind(TAXON_GROUP_LABELS_PARAM, Arrays.asList(taxonGroupLabels));
    }

    default Specification<Operation> isBetweenDates(Date startDate, Date endDate) {
        if (startDate == null && endDate == null) return null;
        return BindableSpecification.where((root, query, criteriaBuilder) -> {
                ParameterExpression<Date> startDateParam = criteriaBuilder.parameter(Date.class, START_DATE_PARAM);
                ParameterExpression<Date> endDateParam = criteriaBuilder.parameter(Date.class, END_DATE_PARAM);

                // TODO BLA: review this code
                // - use NOT(condition) ?
                // - Use fishingStartDate only if parent Operation ? or make sure to store
                return criteriaBuilder.and(
                    criteriaBuilder.or(
                        criteriaBuilder.isNull(startDateParam.as(String.class)),
                        criteriaBuilder.or(
                            criteriaBuilder.and(
                                criteriaBuilder.isNotNull(root.get(Operation.Fields.END_DATE_TIME)),
                                criteriaBuilder.greaterThan(root.get(Operation.Fields.END_DATE_TIME), startDateParam)
                            ),
                            criteriaBuilder.and(
                                criteriaBuilder.isNotNull(root.get(Operation.Fields.FISHING_START_DATE_TIME)),
                                criteriaBuilder.greaterThan(root.get(Operation.Fields.FISHING_START_DATE_TIME), startDateParam)
                            )
                        )
                    ),
                    criteriaBuilder.or(
                        criteriaBuilder.isNull(endDateParam.as(String.class)),
                        criteriaBuilder.or(
                            criteriaBuilder.and(
                                criteriaBuilder.isNotNull(root.get(Operation.Fields.END_DATE_TIME)),
                                criteriaBuilder.lessThan(root.get(Operation.Fields.END_DATE_TIME), endDateParam)
                            ),
                            criteriaBuilder.and(
                                criteriaBuilder.isNotNull(root.get(Operation.Fields.FISHING_START_DATE_TIME)),
                                criteriaBuilder.lessThan(root.get(Operation.Fields.FISHING_START_DATE_TIME), endDateParam)
                            )
                        )
                    )
                );
            })
            .addBind(START_DATE_PARAM, startDate)
            .addBind(END_DATE_PARAM, endDate);
    }

    default Specification<Operation> hasQualityFlagIds(Integer[] qualityFlagIds) {
        if (ArrayUtils.isEmpty(qualityFlagIds)) return null;
        return BindableSpecification.where((root, query, criteriaBuilder) -> {
                ParameterExpression<Collection> param = criteriaBuilder.parameter(Collection.class, QUALITY_FLAG_ID_PARAM);
                return criteriaBuilder.in(root.get(Operation.Fields.QUALITY_FLAG).get(QualityFlag.Fields.ID)).value(param);
            })
            .addBind(QUALITY_FLAG_ID_PARAM, Arrays.asList(qualityFlagIds));
    }

    // Override the default function, because operation has no validation date
    default Specification<Operation> isValidated() {
        return (root, query, criteriaBuilder) ->
            // Trip's validation date must not be not null
            criteriaBuilder.isNotNull(Daos.composePath(root, StringUtils.doting(Operation.Fields.TRIP, Trip.Fields.VALIDATION_DATE)));
    }

    List<OperationVO> saveAllByTripId(int tripId, List<OperationVO> operations);
}

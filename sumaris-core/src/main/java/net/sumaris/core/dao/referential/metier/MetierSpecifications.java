package net.sumaris.core.dao.referential.metier;

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

import net.sumaris.core.dao.referential.ReferentialSpecifications;
import net.sumaris.core.dao.technical.Daos;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.dao.technical.jpa.BindableSpecification;
import net.sumaris.core.model.administration.programStrategy.Program;
import net.sumaris.core.model.data.Operation;
import net.sumaris.core.model.data.Trip;
import net.sumaris.core.model.data.Vessel;
import net.sumaris.core.model.referential.metier.Metier;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.filter.IReferentialFilter;
import net.sumaris.core.vo.filter.MetierFilterVO;
import net.sumaris.core.vo.referential.MetierVO;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.NoRepositoryBean;

import javax.persistence.criteria.*;
import java.util.Date;
import java.util.List;

@NoRepositoryBean
public interface MetierSpecifications
    extends ReferentialSpecifications<Metier> {

    String PROGRAM_LABEL_PARAMETER = "programLabel";
    String EXCLUDED_TRIP_ID_PARAMETER = "tripId";
    String VESSEL_ID_PARAMETER = "vesselId";
    String START_DATE_PARAMETER = "startDate";
    String END_DATE_PARAMETER = "endDate";


    default Specification<Metier> inGearIds(Integer[] gearIds) {
        return inJoinPropertyIds(Metier.Fields.GEAR, gearIds);
    }

    default Specification<Metier> alreadyPracticedMetier(MetierFilterVO filter) {

        if (filter.getVesselId() == null
            || filter.getStartDate() == null
            || filter.getEndDate() == null) return null;

        return BindableSpecification.<Metier>where((root, query, builder) -> {

            query.distinct(true); // Avoid duplicate metiers

            Root<Trip> trips = query.from(Trip.class);
            Join<Trip, Operation> operations = Daos.composeJoin(trips, Trip.Fields.OPERATIONS, JoinType.INNER);

            ParameterExpression<Integer> vesselIdParameter = builder.parameter(Integer.class, VESSEL_ID_PARAMETER);
            ParameterExpression<String> programLabelParameter = builder.parameter(String.class, PROGRAM_LABEL_PARAMETER);
            ParameterExpression<Date> startDateParam = builder.parameter(Date.class, START_DATE_PARAMETER);
            ParameterExpression<Date> endDateParam = builder.parameter(Date.class, END_DATE_PARAMETER);
            ParameterExpression<Integer> tripIdParameter = builder.parameter(Integer.class, EXCLUDED_TRIP_ID_PARAMETER);

            return builder.and(
                    // Link metier to operation
                    builder.equal(root.get(Metier.Fields.ID), operations.get(Operation.Fields.METIER)),
                    // Vessel
                    builder.equal(
                        Daos.composePath(trips, StringUtils.doting(Trip.Fields.VESSEL, Vessel.Fields.ID)),
                        vesselIdParameter),
                    // Date
                    builder.not(
                            builder.or(
                                    builder.greaterThan(trips.get(Trip.Fields.DEPARTURE_DATE_TIME), endDateParam),
                                    builder.lessThan(trips.get(Trip.Fields.RETURN_DATE_TIME), startDateParam)
                            )
                    ),

                    // Program
                    builder.or(
                            builder.isNull(programLabelParameter),
                            builder.equal(
                                Daos.composePath(trips, StringUtils.doting(Trip.Fields.PROGRAM, Program.Fields.LABEL)),
                                programLabelParameter)
                    ),
                    // Excluded trip
                    builder.or(
                            builder.isNull(tripIdParameter),
                            builder.notEqual(trips.get(Trip.Fields.ID), tripIdParameter)
                    )
            );
        })
        .addBind(START_DATE_PARAMETER, filter.getStartDate())
        .addBind(END_DATE_PARAMETER, filter.getEndDate())
        .addBind(VESSEL_ID_PARAMETER, filter.getVesselId())
        .addBind(PROGRAM_LABEL_PARAMETER, filter.getProgramLabel())
        .addBind(EXCLUDED_TRIP_ID_PARAMETER, filter.getExcludedTripId());
    }

    List<MetierVO> findByFilter(
            IReferentialFilter filter,
            int offset,
            int size,
            String sortAttribute,
            SortDirection sortDirection);

}

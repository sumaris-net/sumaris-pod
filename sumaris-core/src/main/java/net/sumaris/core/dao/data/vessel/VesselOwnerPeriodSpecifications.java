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
import net.sumaris.core.model.administration.programStrategy.Program;
import net.sumaris.core.model.data.*;
import net.sumaris.core.util.ArrayUtils;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.data.VesselOwnerPeriodVO;
import net.sumaris.core.vo.filter.VesselFilterVO;
import net.sumaris.core.vo.filter.VesselOwnerFilterVO;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.ParameterExpression;
import java.util.*;

public interface VesselOwnerPeriodSpecifications {

    String VESSEL_ID_PARAM = "vesselId";
    String VESSEL_OWNER_ID_PARAM = "vesselOwnerId";


    default Specification<VesselOwnerPeriod> hasProgramLabel(String programLabel) {
        if (StringUtils.isBlank(programLabel)) return null;
        return BindableSpecification.where((root, query, cb) -> {
                    ParameterExpression<String> param = cb.parameter(String.class, VesselOwnerFilterVO.Fields.PROGRAM_LABEL);
                    return cb.equal(
                            Daos.composePath(root, StringUtils.doting(VesselOwnerPeriod.Fields.VESSEL_OWNER, VesselOwner.Fields.PROGRAM, Program.Fields.LABEL)),
                            param);
                })
                .addBind(VesselOwnerFilterVO.Fields.PROGRAM_LABEL, programLabel);
    }

    default Specification<VesselOwnerPeriod> hasProgramIds(Integer[] programIds) {
        if (ArrayUtils.isEmpty(programIds)) return null;
        return BindableSpecification.where((root, query, cb) -> {
                    ParameterExpression<Collection> param = cb.parameter(Collection.class, VesselOwnerFilterVO.Fields.PROGRAM_IDS);
                    return cb.in(
                            Daos.composePath(root, StringUtils.doting(VesselOwnerPeriod.Fields.VESSEL_OWNER, VesselOwner.Fields.PROGRAM, Program.Fields.ID)))
                            .value(param);
                })
                .addBind(VesselOwnerFilterVO.Fields.PROGRAM_IDS, Arrays.asList(programIds));
    }

    default Specification<VesselOwnerPeriod> vesselId(Integer vesselId) {
        if (vesselId == null) return null;
        BindableSpecification<VesselOwnerPeriod> specification = BindableSpecification.where((root, query, cb) -> {
            ParameterExpression<Integer> param = cb.parameter(Integer.class, VESSEL_ID_PARAM);
            return cb.equal(root.get(VesselOwnerPeriod.Fields.VESSEL).get(Vessel.Fields.ID), param);
        });
        specification.addBind(VESSEL_ID_PARAM, vesselId);
        return specification;
    }

    default Specification<VesselOwnerPeriod> vesselOwnerId(Integer vesselOwnerId) {
        if (vesselOwnerId == null) return null;
        BindableSpecification<VesselOwnerPeriod> specification = BindableSpecification.where((root, query, cb) -> {
            ParameterExpression<Integer> param = cb.parameter(Integer.class, VESSEL_OWNER_ID_PARAM);
            return cb.equal(root.get(VesselOwnerPeriod.Fields.VESSEL_OWNER).get(VesselOwner.Fields.ID), param);
        });
        specification.addBind(VESSEL_OWNER_ID_PARAM, vesselOwnerId);
        return specification;
    }


    default Specification<VesselOwnerPeriod> atDate(Date date) {
        return betweenDate(date, null);
    }

    default Specification<VesselOwnerPeriod> betweenDate(Date startDate, Date endDate) {
        if (startDate == null && endDate == null) return null;
        return (root, query, cb) -> {

            // Start + end date
            if (startDate != null && endDate != null) {
                return cb.not(
                    cb.or(
                        cb.lessThan(Daos.nvlEndDate(root.get(VesselOwnerPeriod.Fields.END_DATE), cb, getDatabaseType()), startDate),
                        cb.greaterThan(root.get(StringUtils.doting(VesselOwnerPeriod.Fields.ID, VesselOwnerPeriodId.Fields.START_DATE)), endDate)
                    )
                );
            }

            // Start date only
            else if (startDate != null) {
                return cb.greaterThanOrEqualTo(Daos.nvlEndDate(root.get(VesselOwnerPeriod.Fields.END_DATE), cb, getDatabaseType()), startDate);
            }

            // End date only
            else {
                return cb.not(cb.greaterThan(Daos.composePath(root, StringUtils.doting(VesselOwnerPeriod.Fields.ID, VesselOwnerPeriodId.Fields.START_DATE)), endDate));
            }
        };
    }

    Optional<VesselOwnerPeriodVO> findLastByVesselId(int vesselId);

    Specification<VesselOwnerPeriod> toSpecification(VesselOwnerFilterVO filter);

    Optional<VesselOwnerPeriod> findByVesselIdAndDate(int vesselId, Date date);

    List<VesselOwnerPeriodVO> findAll(VesselOwnerFilterVO filter, Page page);

    DatabaseType getDatabaseType();
}

package net.sumaris.core.dao.administration.programStrategy;

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

import net.sumaris.core.dao.technical.Daos;
import net.sumaris.core.dao.technical.jpa.BindableSpecification;
import net.sumaris.core.dao.technical.model.annotation.EntityEnums;
import net.sumaris.core.model.administration.programStrategy.*;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.administration.programStrategy.ProgramDepartmentVO;
import net.sumaris.core.vo.administration.programStrategy.ProgramFetchOptions;
import net.sumaris.core.vo.administration.programStrategy.ProgramPersonVO;
import net.sumaris.core.vo.administration.programStrategy.ProgramVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import net.sumaris.core.vo.referential.TaxonGroupVO;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StreamUtils;

import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.ListJoin;
import javax.persistence.criteria.ParameterExpression;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author peck7 on 24/08/2020.
 */
public interface ProgramSpecifications {

    String PROPERTY_LABEL_PARAM = "propertyLabel";
    String UPDATE_DATE_GREATER_THAN_PARAM = "updateDateGreaterThan";
    String ACQUISITION_LEVELS_PARAM = "acquisitionLevels";

    default Specification<Program> hasProperty(String propertyLabel) {
        if (propertyLabel == null) return null;
        return BindableSpecification.where((root, query, criteriaBuilder) -> {
            ParameterExpression<String> param = criteriaBuilder.parameter(String.class, PROPERTY_LABEL_PARAM);
            return criteriaBuilder.equal(root.join(Program.Fields.PROPERTIES, JoinType.LEFT).get(ProgramProperty.Fields.LABEL), param);
        })
        .addBind(PROPERTY_LABEL_PARAM, propertyLabel);
    }

    default Specification<Program> newerThan(Date updateDate) {
        if (updateDate == null) return null;
        return BindableSpecification.where((root, query, criteriaBuilder) -> {
            ParameterExpression<Date> updateDateParam = criteriaBuilder.parameter(Date.class, UPDATE_DATE_GREATER_THAN_PARAM);
            return criteriaBuilder.greaterThan(root.get(Program.Fields.UPDATE_DATE), updateDateParam);
        })
        .addBind(UPDATE_DATE_GREATER_THAN_PARAM, updateDate);
    }

    default Specification<Program> hasAcquisitionLevels(String... acquisitionLevels) {
        if (ArrayUtils.isEmpty(acquisitionLevels)) return null;
        return BindableSpecification.where((root, query, cb) -> {
                ParameterExpression<Collection> param = cb.parameter(Collection.class, ACQUISITION_LEVELS_PARAM);

                // Avoid duplication, because of inner join
                query.distinct(true);

                ListJoin<Program, PmfmStrategy> pmfmStrategiesJoin = Daos.composeJoinList(root, StringUtils.doting(Program.Fields.STRATEGIES, Strategy.Fields.PMFMS), JoinType.INNER);
                Join<Program, Strategy> acquisitionLevelJoin = Daos.composeJoin(pmfmStrategiesJoin, PmfmStrategy.Fields.ACQUISITION_LEVEL, JoinType.INNER);

                return cb.in(acquisitionLevelJoin.get(AcquisitionLevel.Fields.LABEL)).value(param);
            })
            .addBind(ACQUISITION_LEVELS_PARAM, Arrays.asList(acquisitionLevels));
    }

    Optional<ProgramVO> findIfNewerById(int id, Date updateDate, ProgramFetchOptions fetchOptions);

    ProgramVO toVO(Program source, ProgramFetchOptions fetchOptions);

    List<TaxonGroupVO> getTaxonGroups(int programId);

    List<ReferentialVO> getGears(int programId);

    boolean hasUserPrivilege(int programId, int personId, ProgramPrivilegeEnum privilege);

    boolean hasDepartmentPrivilege(int programId, int departmentId, ProgramPrivilegeEnum privilege);

    List<ProgramDepartmentVO> getDepartmentsById(int id);

    List<ProgramPersonVO> getPersonsById(int id);

    List<ProgramDepartmentVO> saveDepartmentsByProgramId(int programId, List<ProgramDepartmentVO> sources);

    List<ProgramPersonVO> savePersonsByProgramId(int programId, List<ProgramPersonVO> sources);

    void clearCache();

    Logger getLogger();
}

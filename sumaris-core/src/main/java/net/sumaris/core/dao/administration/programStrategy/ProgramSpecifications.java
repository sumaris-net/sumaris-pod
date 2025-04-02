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
import net.sumaris.core.model.administration.programStrategy.*;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.administration.programStrategy.ProgramDepartmentVO;
import net.sumaris.core.vo.administration.programStrategy.ProgramFetchOptions;
import net.sumaris.core.vo.administration.programStrategy.ProgramPersonVO;
import net.sumaris.core.vo.administration.programStrategy.ProgramVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import net.sumaris.core.vo.referential.taxon.TaxonGroupVO;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.*;
import java.util.*;

/**
 * @author peck7 on 24/08/2020.
 */
public interface ProgramSpecifications {

    String PROPERTY_LABEL_PARAM = "propertyLabel";
    String MIN_UPDATE_DATE_PARAM = "minUpdateDate";
    String INCLUDED_ACQUISITION_LEVELS_PARAM = "includedAcquisitionLevels";
    String EXCLUDED_ACQUISITION_LEVELS_PARAM = "excludedAcquisitionLevels";

    default Specification<Program> hasProperty(String propertyLabel) {
        if (propertyLabel == null) return null;
        return BindableSpecification.where((root, query, cb) -> {
            ParameterExpression<String> param = cb.parameter(String.class, PROPERTY_LABEL_PARAM);
            return cb.equal(root.join(Program.Fields.PROPERTIES, JoinType.LEFT).get(ProgramProperty.Fields.LABEL), param);
        })
        .addBind(PROPERTY_LABEL_PARAM, propertyLabel);
    }

    default Specification<Program> newerThan(Date minUpdateDate) {
        if (minUpdateDate == null) return null;
        return BindableSpecification.where((root, query, cb) -> {
            ParameterExpression<Date> updateDateParam = cb.parameter(Date.class, MIN_UPDATE_DATE_PARAM);
            return cb.greaterThan(root.get(Program.Fields.UPDATE_DATE), updateDateParam);
        })
        .addBind(MIN_UPDATE_DATE_PARAM, minUpdateDate);
    }

    default Specification<Program> includedAcquisitionLevel(String... acquisitionLevels) {
        if (ArrayUtils.isEmpty(acquisitionLevels)) return null;
        return BindableSpecification.where((root, query, cb) -> {
                ParameterExpression<Collection> param = cb.parameter(Collection.class, INCLUDED_ACQUISITION_LEVELS_PARAM);

                // Avoid duplication, because of inner join
                query.distinct(true);

                ListJoin<Program, PmfmStrategy> pmfmStrategiesJoin = Daos.composeJoinList(root, StringUtils.doting(Program.Fields.STRATEGIES, Strategy.Fields.PMFMS), JoinType.INNER);
                Join<Program, Strategy> acquisitionLevelJoin = Daos.composeJoin(pmfmStrategiesJoin, PmfmStrategy.Fields.ACQUISITION_LEVEL, JoinType.INNER);

                return cb.in(acquisitionLevelJoin.get(AcquisitionLevel.Fields.LABEL)).value(param);
            })
            .addBind(INCLUDED_ACQUISITION_LEVELS_PARAM, Arrays.asList(acquisitionLevels));
    }

    default Specification<Program> excludedAcquisitionLevel(String... excludedAcquisitionLevels) {
        if (ArrayUtils.isEmpty(excludedAcquisitionLevels)) return null;

        return BindableSpecification.where((root, query, cb) -> {
            ParameterExpression<Collection> param = cb.parameter(Collection.class, EXCLUDED_ACQUISITION_LEVELS_PARAM);

            // Avoid duplication
            query.distinct(true);

            Subquery<Integer> subQuery = query.subquery(Integer.class);
            Root<Program> subRoot = subQuery.from(Program.class);
            ListJoin<Program, PmfmStrategy> subPmfmStrategiesJoin = Daos.composeJoinList(
                    subRoot,
                    StringUtils.doting(Program.Fields.STRATEGIES, Strategy.Fields.PMFMS),
                    JoinType.LEFT);
            Join<PmfmStrategy, AcquisitionLevel> subAcquisitionLevelJoin = Daos.composeJoin(
                    subPmfmStrategiesJoin,
                    PmfmStrategy.Fields.ACQUISITION_LEVEL,
                    JoinType.LEFT);

            subQuery.select(subRoot.get("id"))
                    .where(
                            cb.and(
                                    cb.equal(subRoot, root),
                                    cb.in(subAcquisitionLevelJoin.get(AcquisitionLevel.Fields.LABEL)).value(param)
                            )
                    );

            // Return the programs that have no correspondence in the sub-query
            return cb.not(cb.exists(subQuery));
        }).addBind(EXCLUDED_ACQUISITION_LEVELS_PARAM, Arrays.asList(excludedAcquisitionLevels));
    }

    Optional<ProgramVO> findIfNewerById(int id, Date updateDate, ProgramFetchOptions fetchOptions);

    ProgramVO toVO(Program source, ProgramFetchOptions fetchOptions);

    List<TaxonGroupVO> getTaxonGroups(int programId);

    List<ReferentialVO> getGears(int programId);

    boolean hasUserPrivilege(int programId, int personId, ProgramPrivilegeEnum privilege);

    boolean hasDepartmentPrivilege(int programId, int departmentId, ProgramPrivilegeEnum privilege);

    List<ProgramPrivilegeEnum> getAllPrivilegeIdsByUserId(int programId, int personId);

    List<ProgramDepartmentVO> getDepartmentsById(int id);

    List<ProgramPersonVO> getPersonsById(int id);

    List<ProgramDepartmentVO> saveDepartmentsByProgramId(int programId, List<ProgramDepartmentVO> sources);

    List<ProgramPersonVO> savePersonsByProgramId(int programId, List<ProgramPersonVO> sources);

    List<ReferentialVO> getAcquisitionLevelsById(int programId);

    void clearCache();

    Logger getLogger();

}

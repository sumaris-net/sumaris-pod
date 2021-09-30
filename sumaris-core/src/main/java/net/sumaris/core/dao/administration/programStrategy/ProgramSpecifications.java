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

import net.sumaris.core.dao.technical.jpa.BindableSpecification;
import net.sumaris.core.model.administration.programStrategy.Program;
import net.sumaris.core.model.administration.programStrategy.ProgramPrivilegeEnum;
import net.sumaris.core.model.administration.programStrategy.ProgramProperty;
import net.sumaris.core.model.administration.programStrategy.Strategy;
import net.sumaris.core.vo.administration.programStrategy.ProgramFetchOptions;
import net.sumaris.core.vo.administration.programStrategy.ProgramVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import net.sumaris.core.vo.referential.TaxonGroupVO;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.ParameterExpression;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * @author peck7 on 24/08/2020.
 */
public interface ProgramSpecifications {

    String PROPERTY_LABEL_PARAM = "propertyLabel";
    String UPDATE_DATE_GREATER_THAN_PARAM = "updateDateGreaterThan";

    default Specification<Program> hasProperty(String propertyLabel) {
        if (propertyLabel == null) return null;
        return BindableSpecification.where((root, query, criteriaBuilder) -> {
            ParameterExpression<String> param = criteriaBuilder.parameter(String.class, PROPERTY_LABEL_PARAM);
            return criteriaBuilder.or(
                criteriaBuilder.isNull(param),
                criteriaBuilder.equal(root.join(Program.Fields.PROPERTIES, JoinType.LEFT).get(ProgramProperty.Fields.LABEL), param)
            );
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

    Optional<ProgramVO> findIfNewerByLabel(String label, Date updateDate, ProgramFetchOptions fetchOptions);

    ProgramVO toVO(Program source, ProgramFetchOptions fetchOptions);

    List<TaxonGroupVO> getTaxonGroups(int programId);

    List<ReferentialVO> getGears(int programId);

    boolean hasUserPrivilege(int programId, int personId, ProgramPrivilegeEnum privilege);

    boolean hasDepartmentPrivilege(int programId, int departmentId, ProgramPrivilegeEnum privilege);

}

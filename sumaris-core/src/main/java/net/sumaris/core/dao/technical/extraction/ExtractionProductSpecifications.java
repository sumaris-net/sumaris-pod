package net.sumaris.core.dao.technical.extraction;

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
import net.sumaris.core.dao.technical.model.IEntity;
import net.sumaris.core.model.technical.extraction.ExtractionProduct;
import net.sumaris.core.vo.technical.extraction.ExtractionProductColumnVO;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.ParameterExpression;
import java.util.List;

/**
 * @author peck7 on 21/08/2020.
 */
public interface ExtractionProductSpecifications {

    String DEPARTMENT_ID_PARAM = "departmentId";

    default Specification<ExtractionProduct> withDepartmentId(Integer departmentId) {
        BindableSpecification<ExtractionProduct> specification = BindableSpecification.where((root, query, criteriaBuilder) -> {
            ParameterExpression<Integer> parameter = criteriaBuilder.parameter(Integer.class, DEPARTMENT_ID_PARAM);
            return criteriaBuilder.or(
                criteriaBuilder.isNull(parameter),
                criteriaBuilder.equal(root.get(ExtractionProduct.Fields.RECORDER_DEPARTMENT).get(IEntity.Fields.ID), parameter)
            );
        });
        specification.addBind(DEPARTMENT_ID_PARAM, departmentId);
        return specification;
    }


    List<ExtractionProductColumnVO> getColumnsByIdAndTableLabel(int id, String tableLabel);

}
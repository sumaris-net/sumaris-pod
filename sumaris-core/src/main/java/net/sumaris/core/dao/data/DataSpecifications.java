package net.sumaris.core.dao.data;

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
import net.sumaris.core.model.data.IDataEntity;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.ParameterExpression;
import java.io.Serializable;

/**
 * @author peck7 on 28/08/2020.
 */
public interface DataSpecifications<E extends IDataEntity<? extends Serializable>> {

    String RECORDER_DEPARTMENT_ID_PARAM = "recorderDepartmentId";

    default Specification<E> hasRecorderDepartmentId(Integer recorderDepartmentId) {
        BindableSpecification<E> specification = BindableSpecification.where((root, query, criteriaBuilder) -> {
            query.distinct(true); // Set distinct here because hasRecorderDepartmentId is always used (usually ...)
            ParameterExpression<Integer> param = criteriaBuilder.parameter(Integer.class, RECORDER_DEPARTMENT_ID_PARAM);
            return criteriaBuilder.or(
                criteriaBuilder.isNull(param),
                criteriaBuilder.equal(root.get(E.Fields.RECORDER_DEPARTMENT).get(IEntity.Fields.ID), param)
            );
        });
        specification.addBind(RECORDER_DEPARTMENT_ID_PARAM, recorderDepartmentId);
        return specification;
    }
}
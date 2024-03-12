package net.sumaris.core.dao.technical.namedFilter;

/*-
 * #%L
 * SUMARiS:: Core
 * %%
 * Copyright (C) 2018 - 2023 SUMARiS Consortium
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
import net.sumaris.core.model.IEntity;
import net.sumaris.core.model.technical.namedFilter.NamedFilter;
import net.sumaris.core.util.StringUtils;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.ParameterExpression;

public interface NamedFilterSpecifications {

    String ENTITY_NAME_PARAM = "entityName";

    String SEARCH_TEXT_PARAMETER = "searchText";

    default Specification<NamedFilter> searchText(String searchText) {
        if (StringUtils.isBlank(searchText)) return null;
        return BindableSpecification.where((root, query, cb) -> {
            ParameterExpression<String> searchTextParam = cb.parameter(String.class, SEARCH_TEXT_PARAMETER);
            return cb.like(root.get(NamedFilter.Fields.NAME), searchTextParam);
        }).addBind(SEARCH_TEXT_PARAMETER, Daos.getEscapedSearchText(searchText, false));
    }

    default Specification<NamedFilter> hasRecorderPersonId(Integer recorderPersonId) {
        if (recorderPersonId == null) return null;
        return BindableSpecification.where((root, query, cb) -> {
            ParameterExpression<Integer> param = cb.parameter(Integer.class, NamedFilter.Fields.RECORDER_PERSON);
            return cb.equal(root.get(NamedFilter.Fields.RECORDER_PERSON).get(IEntity.Fields.ID), param);
        }).addBind(NamedFilter.Fields.RECORDER_PERSON, recorderPersonId);
    }

    default Specification<NamedFilter> hasEntityName(String entityName) {
        if (entityName == null) return null;
        return BindableSpecification.where((root, query, cb) -> {
            ParameterExpression<String> param = cb.parameter(String.class, ENTITY_NAME_PARAM);
            return cb.equal(root.get(NamedFilter.Fields.ENTITY_NAME), param);
        }).addBind(ENTITY_NAME_PARAM, entityName);
    }
				 
}
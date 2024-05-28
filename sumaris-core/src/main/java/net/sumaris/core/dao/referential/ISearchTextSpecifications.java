package net.sumaris.core.dao.referential;

/*-
 * #%L
 * SUMARiS:: Core
 * %%
 * Copyright (C) 2018 - 2019 SUMARiS Consortium
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

import com.google.common.base.Preconditions;
import net.sumaris.core.dao.technical.Daos;
import net.sumaris.core.dao.technical.jpa.BindableSpecification;
import net.sumaris.core.model.IEntity;
import net.sumaris.core.model.referential.IItemReferentialEntity;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.filter.ISearchTextFilter;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Predicate;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

public interface ISearchTextSpecifications<ID extends Serializable, E extends IEntity<ID>> {

    String SEARCH_TEXT_PARAMETER = "searchText";

    default Specification<E> searchOrJoinSearchText(ISearchTextFilter filter) {
        String searchJoinProperty = StringUtils.uncapitalize(filter.getSearchJoin());
        if (StringUtils.isNotBlank(searchJoinProperty)) {
            return joinSearchText(searchJoinProperty, filter.getSearchAttribute(), filter.getSearchText());
        } else {
            return searchText(
                StringUtils.isNotBlank(filter.getSearchAttribute()) ? ArrayUtils.toArray(filter.getSearchAttribute()) : null,
                filter.getSearchText());
        }
    }

    default Specification<E> searchText(String[] searchAttributes, String searchText) {
        return searchText(searchAttributes, searchText, false);
    }

    default Specification<E> searchText(String[] searchAttributes, String searchText, boolean searchAny) {
        if (StringUtils.isBlank(searchText)) return null;
        return BindableSpecification.<E>where((root, query, cb) -> {
            ParameterExpression<String> searchTextParam = cb.parameter(String.class, SEARCH_TEXT_PARAMETER);
            if (ArrayUtils.isNotEmpty(searchAttributes)) {
                // search on all attributes
                List<Predicate> predicates = Arrays.stream(searchAttributes).map(searchAttribute -> cb.like(
                        cb.upper(Daos.composePath(root, searchAttribute)),
                        searchTextParam,
                        Daos.LIKE_ESCAPE_CHAR)
                    ).toList();
                // One predicate
                if (predicates.size() == 1) return predicates.get(0);
                // Many predicates (use OR operator)
                return cb.or(
                    predicates.toArray(new Predicate[predicates.size()])
                );
            }
            // Search on label+name only
            return cb.or(
                cb.like(cb.upper(root.get(IItemReferentialEntity.Fields.LABEL)), searchTextParam, Daos.LIKE_ESCAPE_CHAR),
                cb.like(cb.upper(root.get(IItemReferentialEntity.Fields.NAME)), cb.concat("%", searchTextParam), Daos.LIKE_ESCAPE_CHAR)
            );
        })
            .addBind(SEARCH_TEXT_PARAMETER, Daos.getEscapedSearchText(searchText.toUpperCase(), searchAny));
    }

    default Specification<E> joinSearchText(String joinProperty, String searchAttribute, String searchText) {
        if (StringUtils.isBlank(searchText)) return null;
        Preconditions.checkArgument(StringUtils.isNotBlank(joinProperty), "'joinProperty' cannot be empty");
        return BindableSpecification.<E>where((root, query, cb) -> {
            ParameterExpression<String> searchTextParam = cb.parameter(String.class, SEARCH_TEXT_PARAMETER);

            // Avoid duplication, for 'one to many' join
            if (shouldQueryDistinct(joinProperty)) {
                query.distinct(true);
            }

            // Get the class join, using properties
            Join<Object, Object> join = Daos.composeJoin(root, joinProperty, JoinType.INNER);

            // Search on given attribute
            if (StringUtils.isNotBlank(searchAttribute)) {
                return cb.like(cb.upper(join.get(searchAttribute)), searchTextParam, Daos.LIKE_ESCAPE_CHAR);
            }

            // Search on label+name
            return cb.or(
                cb.like(cb.upper(join.get(IItemReferentialEntity.Fields.LABEL)), searchTextParam, Daos.LIKE_ESCAPE_CHAR),
                cb.like(cb.upper(join.get(IItemReferentialEntity.Fields.NAME)), cb.concat("%", searchTextParam), Daos.LIKE_ESCAPE_CHAR)
            );
        })
            .addBind(SEARCH_TEXT_PARAMETER, Daos.getEscapedSearchText(searchText.toUpperCase()));
    }

    default boolean shouldQueryDistinct(String joinProperty) {
        return true;
    }
}

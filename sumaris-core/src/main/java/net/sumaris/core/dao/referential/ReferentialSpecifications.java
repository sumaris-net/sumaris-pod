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

import com.google.common.collect.ImmutableList;
import net.sumaris.core.dao.technical.Daos;
import net.sumaris.core.dao.technical.jpa.SpecificationWithParameters;
import net.sumaris.core.dao.technical.model.IEntity;
import net.sumaris.core.model.referential.IItemReferentialEntity;
import net.sumaris.core.model.referential.IReferentialWithStatusEntity;
import net.sumaris.core.model.referential.Status;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.filter.ReferentialFilterVO;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.data.jpa.domain.Specification;

import javax.annotation.Nullable;
import javax.persistence.criteria.*;

public interface ReferentialSpecifications {

    String SEARCH_TEXT_PARAMETER = "searchText";

    default <T extends IReferentialWithStatusEntity> Specification<T> inLevelIds(String levelProperty, Integer[] levelIds) {
        if (ArrayUtils.isEmpty(levelIds)) return null;
        return (root, query, cb) -> cb.in(
                root.join(levelProperty, JoinType.INNER).get(IEntity.Fields.ID))
                .value(ImmutableList.copyOf(levelIds));
    }

    default <T extends IReferentialWithStatusEntity> Specification<T> inStatusIds(@Nullable Integer[] statusIds) {
        if (ArrayUtils.isEmpty(statusIds)) return null;
        return (root, query, cb) -> cb.in(
                root.get(IReferentialWithStatusEntity.Fields.STATUS).get(Status.Fields.ID))
                .value(ImmutableList.copyOf(statusIds));
    }

    default <T extends IItemReferentialEntity> Specification<T> searchOrJoinSearchText(ReferentialFilterVO filter) {
        String searchJoinProperty = filter.getSearchJoin() != null ? StringUtils.uncapitalize(filter.getSearchJoin()) : null;
        final boolean enableSearchOnJoin = (searchJoinProperty != null);
        Specification<T> searchTextSpecification;
        if (enableSearchOnJoin) {
            searchTextSpecification = joinSearchText(
                searchJoinProperty,
                filter.getSearchAttribute(), ReferentialSpecifications.SEARCH_TEXT_PARAMETER);
        } else {
            searchTextSpecification = searchText(filter.getSearchAttribute(), ReferentialSpecifications.SEARCH_TEXT_PARAMETER);
        }
        return searchTextSpecification;
    }

    default <T extends IItemReferentialEntity> Specification<T> searchText(String searchAttribute, String paramName) {

        return new SpecificationWithParameters<T>() {

            public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query,
                                         CriteriaBuilder cb) {
                ParameterExpression<String> searchTextParam = add(cb.parameter(String.class, paramName));

                if (StringUtils.isNotBlank(searchAttribute)) {
                    return cb.or(
                            cb.isNull(searchTextParam),
                            cb.like(cb.upper(Daos.composePath(root, searchAttribute)), cb.upper(searchTextParam)));
                }
                // Search on label+name
                return cb.or(
                        cb.isNull(searchTextParam),
                        cb.like(cb.upper(root.get(IItemReferentialEntity.Fields.LABEL)), cb.upper(searchTextParam)),
                        cb.like(cb.upper(root.get(IItemReferentialEntity.Fields.NAME)), cb.upper(cb.concat("%", searchTextParam)))
                );
            }
        };
    }

    default <T extends IItemReferentialEntity> Specification<T> joinSearchText(String joinProperty, String searchAttribute, String paramName) {

        return new SpecificationWithParameters<T>() {

            public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query,
                                         CriteriaBuilder cb) {
                ParameterExpression<String> searchTextParam = add(cb.parameter(String.class, paramName));

                if (StringUtils.isNotBlank(searchAttribute)) {
                    return cb.or(
                            cb.isNull(searchTextParam),
                            cb.like(cb.upper(root.join(joinProperty).get(searchAttribute)), cb.upper(searchTextParam)));
                }
                // Search on label+name
                return cb.or(
                        cb.isNull(searchTextParam),
                        cb.like(cb.upper(root.join(joinProperty)
                                .get(IItemReferentialEntity.Fields.LABEL)), cb.upper(searchTextParam)),
                        cb.like(cb.upper(root.join(joinProperty)
                                .get(IItemReferentialEntity.Fields.NAME)), cb.upper(cb.concat("%", searchTextParam)))
                );
            }
        };
    }
}

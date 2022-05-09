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
import net.sumaris.core.dao.technical.model.IEntity;
import net.sumaris.core.model.referential.IItemReferentialEntity;
import net.sumaris.core.model.referential.IReferentialEntity;
import net.sumaris.core.model.referential.IReferentialWithStatusEntity;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.filter.IReferentialFilter;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public interface ReferentialSpecifications<ID extends Serializable, E extends IReferentialWithStatusEntity<ID>>
    extends IEntityWithStatusSpecifications<E> {

    String PROPERTY_PARAMETER_PREFIX = "property";
    String LEVEL_LABEL_PARAMETER = "levelLabel";
    String SEARCH_TEXT_PARAMETER = "searchText";
    String INCLUDED_IDS_PARAMETER = "includedIds";
    String EXCLUDED_IDS_PARAMETER = "excludedIds";

    default Specification<E> hasId(Integer id) {
        if (id == null) return null;
        return BindableSpecification.<E>where((root, query, criteriaBuilder) -> {
            ParameterExpression<Integer> idParam = criteriaBuilder.parameter(Integer.class, IEntity.Fields.ID);
            return criteriaBuilder.equal(root.get(IEntity.Fields.ID), idParam);
        }).addBind(IEntity.Fields.ID, id);
    }

    default Specification<E> hasLabel(String label) {
        if (label == null) return null;
        return BindableSpecification.<E>where((root, query, criteriaBuilder) -> {
            ParameterExpression<String> labelParam = criteriaBuilder.parameter(String.class, IItemReferentialEntity.Fields.LABEL);
            return criteriaBuilder.equal(criteriaBuilder.upper(root.get(IItemReferentialEntity.Fields.LABEL)), labelParam);
        }).addBind(IItemReferentialEntity.Fields.LABEL, label.toUpperCase());
    }

    default Specification<E> inLevelIds(Class<E> entityClass, Integer... levelIds) {
        if (ArrayUtils.isEmpty(levelIds)) return null;
        return ReferentialEntities.getLevelPropertyNameByClass(entityClass).map(p -> inJoinPropertyIds(p, levelIds))
            .orElse(null);
    }

    default Specification<E> inJoinPropertyIds(String joinPropertyName, Integer... ids) {
        // If empty: skip to avoid an unused join
        if (ArrayUtils.isEmpty(ids)) return null;

        final String parameterName = PROPERTY_PARAMETER_PREFIX + StringUtils.capitalize(joinPropertyName.replaceAll("[.]", "_"));
        return BindableSpecification.<E>where((root, query, cb) -> {
            ParameterExpression<Collection> parameter = cb.parameter(Collection.class, parameterName);
            return cb.in(
                    Daos.composeJoin(root, joinPropertyName, JoinType.INNER).get(IEntity.Fields.ID)
                )
                .value(parameter);
        })
        .addBind(parameterName, Arrays.asList(ids));
    }

    default Specification<E> inLevelLabels(Class<E> entityClass, String[] levelLabels) {
        // If empty: skip to avoid an unused join
        if (ArrayUtils.isEmpty(levelLabels)) return null;

        return ReferentialEntities.getLevelPropertyNameByClass(entityClass).map(levelPropertyName ->
                    BindableSpecification.<E>where((root, query, criteriaBuilder) -> {
                        ParameterExpression<Collection> levelParam = criteriaBuilder.parameter(Collection.class, LEVEL_LABEL_PARAMETER);
                        return criteriaBuilder.in(root.join(levelPropertyName, JoinType.INNER).get(IItemReferentialEntity.Fields.LABEL)).value(levelParam);
                    }).addBind(LEVEL_LABEL_PARAMETER, Arrays.asList(levelLabels))
            )
        .orElse(null);
    }

    default Specification<E> searchOrJoinSearchText(IReferentialFilter filter) {
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
        return BindableSpecification.<E>where((root, query, cb) -> {
            ParameterExpression<String> searchTextParam = cb.parameter(String.class, SEARCH_TEXT_PARAMETER);
            if (ArrayUtils.isNotEmpty(searchAttributes)) {
                // search on all attributes
                List<Predicate> predicates = new ArrayList<>();
                predicates.add(
                    cb.isNull(searchTextParam)
                );
                Arrays.stream(searchAttributes).forEach(searchAttribute -> predicates.add(cb.like(
                        cb.upper(Daos.composePath(root, searchAttribute)),
                        searchTextParam,
                        Daos.LIKE_ESCAPE_CHAR)
                    ));
                return cb.or(
                    // all predicates
                    predicates.toArray(new Predicate[predicates.size()])
                );
            }
            // Search on label+name only
            return cb.or(
                cb.isNull(searchTextParam),
                cb.like(cb.upper(root.get(IItemReferentialEntity.Fields.LABEL)), searchTextParam, Daos.LIKE_ESCAPE_CHAR),
                cb.like(cb.upper(root.get(IItemReferentialEntity.Fields.NAME)), cb.concat("%", searchTextParam), Daos.LIKE_ESCAPE_CHAR)
            );
        })
            .addBind(SEARCH_TEXT_PARAMETER, Daos.getEscapedSearchText(searchText != null ? searchText.toUpperCase() : null, searchAny));
    }

    default Specification<E> joinSearchText(String joinProperty, String searchAttribute, String searchText) {
        Preconditions.checkArgument(StringUtils.isNotBlank(joinProperty), "'joinProperty' cannot be empty");
        return BindableSpecification.<E>where((root, query, criteriaBuilder) -> {
            ParameterExpression<String> searchTextParam = criteriaBuilder.parameter(String.class, SEARCH_TEXT_PARAMETER);

            // Avoid duplication, for 'one to many' join
            if (shouldQueryDistinct(joinProperty)) {
                query.distinct(true);
            }

            // Get the class join, using properties
            Join<Object, Object> join = Daos.composeJoin(root, joinProperty, JoinType.INNER);

            // Search on given attribute
            if (StringUtils.isNotBlank(searchAttribute)) {
                return criteriaBuilder.or(
                    criteriaBuilder.isNull(searchTextParam),
                    criteriaBuilder.like(criteriaBuilder.upper(join.get(searchAttribute)), searchTextParam, Daos.LIKE_ESCAPE_CHAR));
            }

            // Search on label+name
            return criteriaBuilder.or(
                criteriaBuilder.isNull(searchTextParam),
                criteriaBuilder.like(criteriaBuilder.upper(join.get(IItemReferentialEntity.Fields.LABEL)), searchTextParam, Daos.LIKE_ESCAPE_CHAR),
                criteriaBuilder.like(criteriaBuilder.upper(join.get(IItemReferentialEntity.Fields.NAME)), criteriaBuilder.concat("%", searchTextParam), Daos.LIKE_ESCAPE_CHAR)
            );
        })
            .addBind(SEARCH_TEXT_PARAMETER, Daos.getEscapedSearchText(searchText != null ? searchText.toUpperCase() : null));
    }

    default Specification<E> inSearchJoinLevelIds(String searchJoin, Integer... joinLevelIds) {
        if (StringUtils.isBlank(searchJoin) || ArrayUtils.isEmpty(joinLevelIds)) return null;

        // Try to get the entity class, from the filter 'searchJoin' attribute
        Class<? extends IReferentialEntity> joinEntityClass;
        try {
            joinEntityClass = ReferentialEntities.getEntityClass(StringUtils.capitalize(searchJoin));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(String.format("Cannot filter on levelId, when searchJoin in on '%s'", searchJoin), e);
        }

        return ReferentialEntities.getLevelPropertyNameByClass(joinEntityClass)
            .map(levelPath -> StringUtils.doting(StringUtils.uncapitalize(searchJoin), levelPath)) // Create the full path
            .map(fullLevelPath -> inJoinPropertyIds(fullLevelPath, joinLevelIds))
            .orElse(null);
    }

    default Specification<E> includedIds(Integer[] includedIds) {
        if (ArrayUtils.isEmpty(includedIds)) return null;
        return BindableSpecification.<E>where((root, query, criteriaBuilder) -> {
            ParameterExpression<Collection> param = criteriaBuilder.parameter(Collection.class, INCLUDED_IDS_PARAMETER);
            return criteriaBuilder.in(root.get(IEntity.Fields.ID)).value(param);
        })
            .addBind(INCLUDED_IDS_PARAMETER, Arrays.asList(includedIds));
    }

    default Specification<E> excludedIds(Integer[] excludedIds) {
        if (ArrayUtils.isEmpty(excludedIds)) return null;
        return BindableSpecification.<E>where((root, query, criteriaBuilder) -> {
            ParameterExpression<Collection> param = criteriaBuilder.parameter(Collection.class, EXCLUDED_IDS_PARAMETER);
            return criteriaBuilder.not(
                    criteriaBuilder.in(root.get(IEntity.Fields.ID)).value(param)
            );
        })
        .addBind(EXCLUDED_IDS_PARAMETER, Arrays.asList(excludedIds));
    }

    default Specification<E> withPropertyValue(String propertyName, Class<?> propertyClass, Object value) {
        if (value == null) return null;
        final String parameterName = PROPERTY_PARAMETER_PREFIX + StringUtils.capitalize(propertyName.replaceAll("[.]", "_"));
        return BindableSpecification.<E>where((root, query, cb) -> {
                ParameterExpression<?> parameter = cb.parameter(propertyClass, parameterName);
                return cb.equal(Daos.composePath(root, propertyName), parameter);
            })
            .addBind(parameterName, value);
    }

    default boolean shouldQueryDistinct(String joinProperty) {
        return true;
    }
}

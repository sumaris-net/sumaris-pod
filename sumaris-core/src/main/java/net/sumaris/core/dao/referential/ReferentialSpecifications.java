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

import net.sumaris.core.dao.technical.Daos;
import net.sumaris.core.dao.technical.jpa.BindableSpecification;
import net.sumaris.core.dao.technical.model.IEntity;
import net.sumaris.core.model.referential.IItemReferentialEntity;
import net.sumaris.core.model.referential.IReferentialWithStatusEntity;
import net.sumaris.core.model.referential.Status;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.filter.ReferentialFilterVO;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.ParameterExpression;
import java.util.Arrays;
import java.util.Collection;

public interface ReferentialSpecifications<E extends IReferentialWithStatusEntity> {

    String LABEL_PARAMETER = "label";
    String LEVEL_PARAMETER = "level";
    String LEVEL_SET_PARAMETER = "levelSet";
    String STATUS_PARAMETER = "status";
    String STATUS_SET_PARAMETER = "statusSet";
    String SEARCH_TEXT_PARAMETER = "searchText";

    default Specification<E> hasLabel(String label) {
        BindableSpecification<E> specification = BindableSpecification.where((root, query, criteriaBuilder) -> {
            ParameterExpression<String> labelParam = criteriaBuilder.parameter(String.class, LABEL_PARAMETER);
            return criteriaBuilder.or(
                criteriaBuilder.isNull(labelParam),
                criteriaBuilder.equal(root.get(IItemReferentialEntity.Fields.LABEL), labelParam)
            );
        });
        specification.addBind(LABEL_PARAMETER, label);
        return specification;
    }

    default Specification<E> inLevelIds(String levelProperty, ReferentialFilterVO filter) {
        Integer[] levelIds = (filter.getLevelId() != null) ? new Integer[]{filter.getLevelId()} : filter.getLevelIds();
        return inLevelIds(levelProperty, levelIds);
    }

    default Specification<E> inLevelIds(String levelProperty, Integer[] levelIds) {
        BindableSpecification<E> specification = BindableSpecification.where((root, query, criteriaBuilder) -> {
            ParameterExpression<Collection> levelParam = criteriaBuilder.parameter(Collection.class, LEVEL_PARAMETER);
            ParameterExpression<Boolean> levelSetParam = criteriaBuilder.parameter(Boolean.class, LEVEL_SET_PARAMETER);
            return criteriaBuilder.or(
                criteriaBuilder.isFalse(levelSetParam),
                criteriaBuilder.in(root.join(levelProperty, JoinType.INNER).get(IEntity.Fields.ID)).value(levelParam)
            );
        });
        specification.addBind(LEVEL_SET_PARAMETER, !ArrayUtils.isEmpty(levelIds));
        specification.addBind(LEVEL_PARAMETER, ArrayUtils.isEmpty(levelIds) ? null : Arrays.asList(levelIds));
        return specification;
    }

    default Specification<E> inStatusIds(ReferentialFilterVO filter) {
        Integer[] statusIds = filter.getStatusIds();
        BindableSpecification<E> specification = BindableSpecification.where((root, query, criteriaBuilder) -> {
            ParameterExpression<Collection> statusParam = criteriaBuilder.parameter(Collection.class, STATUS_PARAMETER);
            ParameterExpression<Boolean> statusSetParam = criteriaBuilder.parameter(Boolean.class, STATUS_SET_PARAMETER);
            return criteriaBuilder.or(
                criteriaBuilder.isFalse(statusSetParam),
                criteriaBuilder.in(root.get(IReferentialWithStatusEntity.Fields.STATUS).get(Status.Fields.ID)).value(statusParam)
            );
        });
        specification.addBind(STATUS_SET_PARAMETER, !ArrayUtils.isEmpty(statusIds));
        specification.addBind(STATUS_PARAMETER, ArrayUtils.isEmpty(statusIds) ? null : Arrays.asList(statusIds));
        return specification;
    }

    default Specification<E> searchOrJoinSearchText(ReferentialFilterVO filter) {
        String searchText = Daos.getEscapedSearchText(filter.getSearchText());
        String searchJoinProperty = filter.getSearchJoin() != null ? StringUtils.uncapitalize(filter.getSearchJoin()) : null;
        if (searchJoinProperty != null) {
            return joinSearchText(searchJoinProperty, filter.getSearchAttribute(), searchText);
        } else {
            return searchText(filter.getSearchAttribute(), searchText);
        }
    }

    default Specification<E> searchText(String searchAttribute, String searchText) {
        BindableSpecification<E> specification = BindableSpecification.where((root, query, criteriaBuilder) -> {
            ParameterExpression<String> searchTextParam = criteriaBuilder.parameter(String.class, SEARCH_TEXT_PARAMETER);
            if (StringUtils.isNotBlank(searchAttribute)) {
                return criteriaBuilder.or(
                    criteriaBuilder.isNull(searchTextParam),
                    criteriaBuilder.like(criteriaBuilder.upper(Daos.composePath(root, searchAttribute)), criteriaBuilder.upper(searchTextParam)));
            }
            // Search on label+name
            return criteriaBuilder.or(
                criteriaBuilder.isNull(searchTextParam),
                criteriaBuilder.like(criteriaBuilder.upper(root.get(IItemReferentialEntity.Fields.LABEL)), criteriaBuilder.upper(searchTextParam)),
                criteriaBuilder.like(criteriaBuilder.upper(root.get(IItemReferentialEntity.Fields.NAME)), criteriaBuilder.upper(criteriaBuilder.concat("%", searchTextParam)))
            );
        });
        specification.addBind(SEARCH_TEXT_PARAMETER, searchText);
        return specification;
    }

    default Specification<E> joinSearchText(String joinProperty, String searchAttribute, String searchText) {
        BindableSpecification<E> specification = BindableSpecification.where((root, query, criteriaBuilder) -> {
            ParameterExpression<String> searchTextParam = criteriaBuilder.parameter(String.class, SEARCH_TEXT_PARAMETER);

            if (StringUtils.isNotBlank(searchAttribute)) {
                return criteriaBuilder.or(
                    criteriaBuilder.isNull(searchTextParam),
                    criteriaBuilder.like(criteriaBuilder.upper(root.join(joinProperty).get(searchAttribute)), criteriaBuilder.upper(searchTextParam)));
            }
            // Search on label+name
            return criteriaBuilder.or(
                criteriaBuilder.isNull(searchTextParam),
                criteriaBuilder.like(criteriaBuilder.upper(root.join(joinProperty).get(IItemReferentialEntity.Fields.LABEL)), criteriaBuilder.upper(searchTextParam)),
                criteriaBuilder.like(criteriaBuilder.upper(root.join(joinProperty).get(IItemReferentialEntity.Fields.NAME)), criteriaBuilder.upper(criteriaBuilder.concat("%", searchTextParam)))
            );
        });
        specification.addBind(SEARCH_TEXT_PARAMETER, searchText);
        return specification;
    }
}

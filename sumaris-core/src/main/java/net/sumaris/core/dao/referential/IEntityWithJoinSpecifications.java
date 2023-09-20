/*
 * #%L
 * SUMARiS
 * %%
 * Copyright (C) 2019 SUMARiS Consortium
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

package net.sumaris.core.dao.referential;

import net.sumaris.core.dao.technical.Daos;
import net.sumaris.core.dao.technical.jpa.BindableSpecification;
import net.sumaris.core.model.IEntity;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.*;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;

public interface IEntityWithJoinSpecifications<ID extends Serializable, E extends IEntity<ID>>
    extends IEntitySpecifications<ID, E> {

    default Specification<E> hasInnerJoinIds(final String joinIdPath, Integer... ids) {
        if (ArrayUtils.isEmpty(ids)) return null;

        String paramName = joinIdPath.replaceAll("\\.", "_")  + "Id";

        return BindableSpecification.where((root, query, cb) -> {
            Join<?,?> join = Daos.composeJoin(root, joinIdPath, JoinType.INNER);
            ParameterExpression<Collection> parameter = cb.parameter(Collection.class, paramName);
            return cb.in(join.get(IEntity.Fields.ID)).value(parameter);
        }).addBind(paramName, Arrays.asList(ids));
    }

    default <T> Specification<E> hasInnerJoinValues(final String joinPropertyPath, T... values) {
        if (ArrayUtils.isEmpty(values)) return null;

        String paramName = joinPropertyPath.replaceAll("\\.", "_")  + "Values";

        return BindableSpecification.where((root, query, cb) -> {
            Expression path = Daos.composePath(root, joinPropertyPath, JoinType.INNER);
            ParameterExpression<Collection> parameter = cb.parameter(Collection.class, paramName);
            return cb.in(path).value(parameter);
        }).addBind(paramName, Arrays.asList(values));
    }
}

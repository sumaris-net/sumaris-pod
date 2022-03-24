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
import net.sumaris.core.dao.technical.model.IEntity;
import net.sumaris.core.model.referential.*;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.ParameterExpression;
import java.util.Arrays;
import java.util.Collection;

public interface IEntityWithJoinSpecifications<E extends IWithStatusEntity<?, Status>> {

    default Specification<E> hasJoinIds(final String joinPropertyName, Integer... ids) {
        if (ArrayUtils.isEmpty(ids)) return null;

        String paramName = joinPropertyName + "Id";

        return BindableSpecification.where((root, query, cb) -> {
            Join<?,?> join = Daos.composeJoin(root, joinPropertyName, JoinType.INNER);
            ParameterExpression<Collection> parameter = cb.parameter(Collection.class, paramName);
            return cb.in(join.get(IEntity.Fields.ID)).value(parameter);
        }).addBind(paramName, Arrays.asList(ids));
    }
}

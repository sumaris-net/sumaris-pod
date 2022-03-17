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

import net.sumaris.core.dao.technical.jpa.BindableSpecification;
import net.sumaris.core.dao.technical.model.IEntity;
import net.sumaris.core.model.referential.IReferentialWithStatusEntity;
import net.sumaris.core.model.referential.IWithStatusEntity;
import net.sumaris.core.model.referential.Status;
import net.sumaris.core.model.referential.conversion.WeightLengthConversion;
import net.sumaris.core.model.referential.pmfm.Unit;
import net.sumaris.core.vo.filter.IReferentialFilter;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.ParameterExpression;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;

public interface IEntityWithStatusSpecifications<E extends IWithStatusEntity<?, Status>> {

    default Specification<E> hasJoinIds(final String joinPropertyName, Integer... ids) {
        if (ArrayUtils.isEmpty(ids)) return null;

        return BindableSpecification.where((root, query, criteriaBuilder) -> {

            ParameterExpression<Collection> parameter = criteriaBuilder.parameter(Collection.class, joinPropertyName);
            return criteriaBuilder.in(
                    root.join(joinPropertyName, JoinType.INNER)
                        .get(Unit.Fields.ID))
                .value(parameter);
        }).addBind(joinPropertyName, Arrays.asList(ids));
    }

    default Specification<E> inStatusIds(Integer... ids) {
        return hasJoinIds(IReferentialWithStatusEntity.Fields.STATUS, ids);
    }
}

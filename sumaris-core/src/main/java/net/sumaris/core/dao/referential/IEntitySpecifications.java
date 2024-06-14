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
import net.sumaris.core.dao.technical.DatabaseType;
import net.sumaris.core.dao.technical.jpa.BindableSpecification;
import net.sumaris.core.model.IEntity;
import net.sumaris.core.util.StringUtils;
import org.springframework.data.jpa.domain.Specification;

import javax.annotation.Nullable;
import javax.persistence.criteria.ParameterExpression;
import java.io.Serializable;
import net.sumaris.core.util.ArrayUtils;

import java.util.Collection;

public interface IEntitySpecifications<ID extends Serializable, E extends IEntity<ID>> {

    String ID_PARAMETER = "id";
    String EXCLUDED_IDS_PARAMETER = "excludedIds";
    String INCLUDED_IDS_PARAMETER = "includedIds";
    String PROPERTY_PARAMETER_PREFIX = "property";


    default Specification<E> id(ID id, Class<ID> idClass) {
        if (id == null) return null;
        return BindableSpecification.where((root, query, cb) -> {
            ParameterExpression<ID> param = cb.parameter(idClass, ID_PARAMETER);
            return cb.equal(root.get(E.Fields.ID), param);
        }).addBind(ID_PARAMETER, id);
    }

    default Specification<E> includedIds(ID... includedIds) {
        if (ArrayUtils.isEmpty(includedIds)) return null;

        // Use equal, instead of in, when possible
        if (includedIds.length == 1) {
            ID includedId = includedIds[0];
            return BindableSpecification.<E>where((root, query, cb) -> {
                    ParameterExpression<ID> param = cb.parameter((Class<ID>) includedId.getClass(), INCLUDED_IDS_PARAMETER);
                    return cb.equal(root.get(IEntity.Fields.ID), param);
                })
                .addBind(INCLUDED_IDS_PARAMETER, includedId);
        }

        return BindableSpecification.<E>where((root, query, cb) -> {
                ParameterExpression<Collection> param = cb.parameter(Collection.class, INCLUDED_IDS_PARAMETER);
                return cb.in(root.get(IEntity.Fields.ID)).value(param);
            })
            .addBind(INCLUDED_IDS_PARAMETER, ArrayUtils.asList(includedIds));
    }

    default Specification<E> excludedIds(ID... excludedIds) {
        if (ArrayUtils.isEmpty(excludedIds)) return null;

        // Use notEquals, instead of notIn, when possible
        if (excludedIds.length == 1) {
            ID excludedId = excludedIds[0];
            return BindableSpecification.<E>where((root, query, cb) -> {
                    ParameterExpression<ID> param = cb.parameter((Class<ID>)excludedId.getClass(),
                        EXCLUDED_IDS_PARAMETER);
                    return cb.notEqual(root.get(IEntity.Fields.ID), param);
                })
                .addBind(EXCLUDED_IDS_PARAMETER, excludedId);
        }

        return BindableSpecification.<E>where((root, query, cb) -> {
                ParameterExpression<Collection> param = cb.parameter(Collection.class, EXCLUDED_IDS_PARAMETER);
                return cb.not(
                    cb.in(root.get(IEntity.Fields.ID)).value(param)
                );
            })
            .addBind(EXCLUDED_IDS_PARAMETER, ArrayUtils.asList(excludedIds));
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

    @Nullable
    default Integer[] concat(@Nullable Integer value, @Nullable Integer[] values) {
        return ArrayUtils.concat(value, values);
    }

    DatabaseType getDatabaseType();
}

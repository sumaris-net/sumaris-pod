package net.sumaris.core.dao.technical.jpa;

/*-
 * #%L
 * SUMARiS:: Core shared
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

import com.google.common.collect.Maps;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.ParameterExpression;
import java.util.Map;

public abstract class SpecificationWithParameters<T> implements Specification<T> {
        
        private Map<String, ParameterExpression<?>> parameters = Maps.newHashMap();


        public <R> ParameterExpression<R> get(String name) {
            return (ParameterExpression<R>) parameters.get(name);
        }

        protected <R> ParameterExpression<R> add(ParameterExpression<R> param) {
                parameters.put(param.getName(), param);
                return param;
        }


}

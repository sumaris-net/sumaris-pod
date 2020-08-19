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

import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.NonNull;

import javax.persistence.Parameter;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class BindableSpecification<T> implements Specification<T>, Serializable {

    private Specification<T> specification;
    private final List<Consumer<TypedQuery<T>>> bindings;

    BindableSpecification(Specification<T> specification) {
        this.specification = specification;
        this.bindings = new ArrayList<>();
        composeBindings(specification);
    }

    private Optional<Specification<T>> getSpecificationOptional() {
        return Optional.ofNullable(specification);
    }

    private void composeBindings(Specification<T> specification) {
        if (specification instanceof BindableSpecification) {
            this.bindings.addAll(((BindableSpecification<T>) specification).getBindings());
        }
    }

    public List<Consumer<TypedQuery<T>>> getBindings() {
        return bindings;
    }

    public void addBind(String parameterName, Object value) {
        bindings.add(typedQuery -> setParameterIfExists(typedQuery, parameterName, value));
    }

    public static void setParameterIfExists(TypedQuery<?> query, String parameterName, Object value) {
        Parameter<Object> parameter = query.getParameter(parameterName, Object.class);
        if (parameter != null)
            query.setParameter(parameter, value);
    }

    public static <T> BindableSpecification<T> where(Specification<T> specification) {
        return new BindableSpecification<T>(specification);
    }

    @Override
    public BindableSpecification<T> and(@NonNull Specification<T> other) {
        specification = getSpecificationOptional().map(spec -> spec.and(other)).orElse(other);
        composeBindings(other);
        return this;
    }

    @Override
    public BindableSpecification<T> or(@NonNull Specification<T> other) {
        specification = getSpecificationOptional().map(spec -> spec.or(other)).orElse(other);
        composeBindings(other);
        return this;
    }

    @Override
    public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
        return getSpecificationOptional().map(spec -> spec.toPredicate(root, query, criteriaBuilder)).orElse(null);
    }

}

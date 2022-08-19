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

import javax.persistence.Parameter;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class BindableSpecification<T> implements Specification<T>, Serializable {

    /**
     * inner specification
     */
    private Specification<T> specification;
    /**
     * binding list
     */
    private final List<Consumer<TypedQuery<?>>> bindings;

    /**
     * Protected constructor, use 'where' builder method
     */
    BindableSpecification() {
        this.bindings = new ArrayList<>();
    }

    private Optional<Specification<T>> getSpecificationOptional() {
        return Optional.ofNullable(specification);
    }

    /**
     * Compose (concat) the list of bindings from specified specification with current ones
     *
     * @param specification the specification with bindings to compose
     */
    private void composeBindings(Specification<T> specification) {
        if (specification instanceof BindableSpecification) {
            this.bindings.addAll(((BindableSpecification<T>) specification).getBindings());
        }
    }

    /**
     * Get the current list of bindings
     * The TypedQuery should be visited before execution
     * @return the bindings
     */
    public List<Consumer<TypedQuery<?>>> getBindings() {
        return bindings;
    }

    /**
     * Add a binding to the current list
     *
     * @param parameterName the parameter name
     * @param value         the parameter value
     */
    public <E extends T> BindableSpecification<E> addBind(String parameterName, Object value) {
        bindings.add(typedQuery ->
            // Set the parameter value to the visited TypedQuery
            setParameterIfExists(typedQuery, parameterName, value));
        return (BindableSpecification<E>)this;
    }

    /**
     * Set the parameter value if exists
     *
     * @param query the visited TypedQuery
     * @param parameterName the parameter name
     * @param value the parameter value
     */
    public static void setParameterIfExists(TypedQuery<?> query, String parameterName, Object value) {
        Parameter<Object> parameter = query.getParameter(parameterName, Object.class);
        if (parameter != null)
            query.setParameter(parameter, value);
    }

    /**
     * Builder method to create a BindableSpecification
     * @param specification the original (or delegate) specification
     * @param <T> type of specification
     * @return the BindableSpecification instance
     */
    public static <T> BindableSpecification<T> where(Specification<T> specification) {
        BindableSpecification<T> instance = new BindableSpecification<>();
        instance.specification = specification;
        instance.composeBindings(specification);
        return instance;
    }

    /**
     * ANDs the given specification to the current one
     * @param other can be null
     * @return The conjunction of the specifications
     */
    @Override
    public BindableSpecification<T> and(Specification<T> other) {
        specification = getSpecificationOptional().map(spec -> spec.and(other)).orElse(other);
        composeBindings(other);
        return this;
    }

    /**
     * ORs the given specification to the current one
     * @param other can be null
     * @return The disjunction of the specifications
     */
    @Override
    public BindableSpecification<T> or(Specification<T> other) {
        specification = getSpecificationOptional().map(spec -> spec.or(other)).orElse(other);
        composeBindings(other);
        return this;
    }

    @Override
    public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        return getSpecificationOptional().map(spec -> spec.toPredicate(root, query, cb)).orElse(null);
    }

}

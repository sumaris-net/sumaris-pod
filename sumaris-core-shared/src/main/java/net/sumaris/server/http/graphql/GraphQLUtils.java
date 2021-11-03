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

package net.sumaris.server.http.graphql;

import graphql.schema.SelectedField;
import io.leangen.graphql.execution.ResolutionEnvironment;

import java.util.Set;
import java.util.stream.Collectors;

public class GraphQLUtils extends io.leangen.graphql.util.GraphQLUtils {

    protected GraphQLUtils() {
        // helper class
    }

    public static Set<String> fields(ResolutionEnvironment env) {
        return env.dataFetchingEnvironment.getSelectionSet().getFields()
            .stream()
            .map(SelectedField::getQualifiedName)
            .filter(qualifiedName -> !qualifiedName.endsWith("/__typename"))
            .collect(Collectors.toSet());
    }

    public static Set<String> immediateFields(ResolutionEnvironment env) {
        return env.dataFetchingEnvironment.getSelectionSet().getImmediateFields()
            .stream().map(SelectedField::getQualifiedName)
            .filter(qualifiedName -> !qualifiedName.endsWith("/__typename"))
            .collect(Collectors.toSet());
    }
}
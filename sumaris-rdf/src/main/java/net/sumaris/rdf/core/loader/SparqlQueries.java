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

package net.sumaris.rdf.core.loader;

import lombok.NonNull;
import net.sumaris.core.dao.technical.Page;

import java.util.Map;

/**
 * Helper class for SparQL queries
 */
public class SparqlQueries {

    public static final String LIMIT_CLAUSE = "OFFSET %s LIMIT %s";

    public static String asPageableQuery(String baseQuery, Page page) {
        if (page == null || page.getSize() < 0 || page.getOffset() < 0) return baseQuery;
        return baseQuery + "\n" + String.format(LIMIT_CLAUSE, page.getOffset(), page.getSize());
    }

    public static String bindParameters(@NonNull String baseQuery, @NonNull Map<String, String> parameters) {
        String query = baseQuery;
        for (Map.Entry<String, String> param : parameters.entrySet()) {
            query = bindParameter(query, param.getKey(), param.getValue());
        }
        return query;
    }

    public static String bindParameter(String query, String parameterName, String value) {
        return query.replaceAll("\\$\\{" + parameterName + "\\}", value);
    }
}

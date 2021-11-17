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

import net.sumaris.core.dao.technical.Page;

/**
 * Helper class for SparQL queries
 */
public class SparqlQueries {

    public static final String LIMIT_CLAUSE = "LIMIT %s OFFSET %s";

    public static String getConstructQuery(String baseQuery, Page page) {
        if (page == null || page.getSize() < 0 || page.getOffset() < 0) return baseQuery;
        return baseQuery + "\n" + String.format(LIMIT_CLAUSE, page.getSize(), page.getOffset());
    }
}

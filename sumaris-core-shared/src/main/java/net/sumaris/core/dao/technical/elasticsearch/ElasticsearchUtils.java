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

package net.sumaris.core.dao.technical.elasticsearch;

import org.apache.commons.lang3.StringUtils;

public class ElasticsearchUtils {

    protected ElasticsearchUtils() {
        // Helper class
    }

    public static String getEscapedSearchText(String searchText, boolean toLowercase) {
        searchText = getEscapedSearchText(searchText);
        if (searchText == null || !toLowercase) return searchText;
        return searchText.toLowerCase();
    }

    public static String getEscapedSearchText(String searchText) {
        searchText = StringUtils.trimToNull(searchText);
        if (searchText == null) return null;
        return searchText.replaceAll("[*]+", "*") // group many '*' chars
            .replaceAll("[*]+", " *") // Split wildcard using space
            .replaceAll("^[*\\s]+", "") // Remove start wildcard (not need)
            .replaceAll("[*\\s]+$", "") // Remove end wildcard (not need)
            ;
    }
}
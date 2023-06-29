package net.sumaris.core.dao.technical.schema;

/*-
 * #%L
 * SUMARiS:: Core
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

import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Copy from jdbc-synchro library
 */
public class SumarisMetadataUtils {

    /**
     * <p>
     * Trims the passed in value to the maximum name length.
     * </p>
     * If no maximum length has been set then this method does nothing.
     *
     * @param name
     *            the name length to check and trim if necessary
     * @param nameMaxLength
     *            if this is not null, then the name returned will be trimmed to
     *            this length (if it happens to be longer).
     * @param nameMaxLength
     *            if this is not null, then the name returned will be trimmed to
     *            this length (if it happens to be longer).
     * @return String the string to be used as SQL type
     */
    public static String ensureMaximumNameLength(String name,
                                                 Integer nameMaxLength) {
        if (StringUtils.isNotBlank(name) && nameMaxLength != null) {
            int max = nameMaxLength.intValue();
            if (name.length() > max) {
                name = name.substring(0, max);
            }
        }
        return name;
    }

    public static Collection<String> getAliasedColumns(@Nullable String tableAlias, Collection<String> columnNames) {
        if (StringUtils.isBlank(tableAlias)) return columnNames;
        return columnNames.stream().map(c -> tableAlias + "." + c).collect(Collectors.toList());
    }

    public static String getAliasedColumnName(@Nullable String alias, String columnName) {
        return StringUtils.isNotBlank(alias) ? alias + "." + columnName : columnName;
    }


}

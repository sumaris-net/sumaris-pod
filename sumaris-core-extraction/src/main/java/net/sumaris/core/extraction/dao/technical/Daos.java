package net.sumaris.core.extraction.dao.technical;

/*-
 * #%L
 * SUMARiS:: Core Extraction
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

import com.google.common.base.Joiner;
import net.sumaris.core.util.Dates;

import java.util.Collection;
import java.util.Date;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Useful method around DAO and entities.
 *
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 */
public class Daos extends net.sumaris.core.dao.technical.Daos {


    private final static String SQL_TO_DATE = "TO_DATE('%s', '%s')";

    /**
     * Concat single quoted strings with ',' character, without parenthesis
     *
     * @param strings a {@link Collection} object.
     * @return concatenated strings
     */
    public static String getSqlInValueFromStringCollection(Collection<String> strings) {
        if (strings == null) return "";
        return Joiner.on(',').skipNulls().join(strings.stream().filter(Objects::nonNull).map(s -> "'" + s + "'").collect(Collectors.toSet()));
    }

    /**
     * Concat integers with ',' character, without parenthesis
     *
     * @param integers a {@link Collection} object.
     * @return concatenated integers
     */
    public static String getSqlInValueFromIntegerCollection(Collection<Integer> integers) {
        if (integers == null) return "";
        return Joiner.on(',').skipNulls().join(integers.stream().filter(Objects::nonNull).collect(Collectors.toSet()));
    }


    public static String getSqlToDate(Date date) {
        if (date == null) return null;
        return String.format(SQL_TO_DATE,
                Dates.formatDate(date, "yyyy-MM-dd HH:mm:ss"),
                "YYYY-MM-DD HH24:MI:SS");
    }
}

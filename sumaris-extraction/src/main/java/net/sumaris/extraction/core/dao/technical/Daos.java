package net.sumaris.extraction.core.dao.technical;

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

import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.util.Dates;
import org.apache.commons.collections4.MapUtils;
import org.springframework.jdbc.datasource.DataSourceUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Useful method around DAO and entities.
 *
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 */
@Slf4j
public class Daos extends net.sumaris.core.dao.technical.Daos {


    private final static String SQL_TO_DATE = "TO_DATE('%s', '%s')";

    private final static String NON_COLUMN_CHAR_REGEXP = "[^a-z0-9_]";

    protected Daos() {
        // Helper class
    }

    /**
     * Concat single quoted strings with ',' character, without parenthesis
     *
     * @param strings a {@link Collection} object.
     * @return concatenated strings
     */
    public static String getSqlInEscapedStrings(Collection<String> strings) {
        if (strings == null) return "";
        return strings.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.joining("','", "'", "'"));
    }

    /**
     * Concat single quoted strings with ',' character, without parenthesis
     *
     * @param strings a {@link String[]} object.
     * @return concatenated strings
     */
    public static String getSqlInEscapedStrings(String... strings) {
        if (strings == null) return "";
        return Stream.of(strings)
                .filter(Objects::nonNull)
                .map(str -> str.replaceAll("'", "''")) // Escape quote character
                .collect(Collectors.joining("','", "'", "'"));
    }

    /**
     * Prepare a valid column name, by cleaning the given value (e.g. remove special characters)
     *
     * @param value a {@link String} object.
     * @return a usable column name
     */
    public static String sqlColumnName(String value) {
        return value.replaceAll("[^a-zA-Z0-9_]+", "_");
    }

    /**
     * Concat integers with ',' character, without parenthesis
     *
     * @param numbers a {@link Collection} object of numbers (Integer, Double, ...).
     * @return concatenated integers
     */
    public static String getSqlInNumbers(Collection<? extends Object> numbers) {
        if (numbers == null) return "";
        return numbers.stream()
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    /**
     * Concat integers with ',' character, without parenthesis
     *
     * @param numbers a {@link Object[]} object of numbers (Integer, Double, ...).
     * @return concatenated integers
     */
    public static String getSqlInNumbers(Object[] numbers) {
        if (numbers == null) return "";
        return Stream.of(numbers)
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    public static String getSqlInNumbers(Number... numbers) {
        if (numbers == null) return "";
        return Stream.of(numbers)
            .filter(Objects::nonNull)
            .map(String::valueOf)
            .collect(Collectors.joining(","));
    }

    public static String getSqlToDate(Date date) {
        if (date == null) return null;
        return String.format(SQL_TO_DATE,
                Dates.formatDate(date, "yyyy-MM-dd HH:mm:ss"),
                "YYYY-MM-DD HH24:MI:SS");
    }


    public static void commitIfHsqldbOrPgsql(DataSource dataSource) {
        commitIf(dataSource, conn -> isHsqlDatabase(conn) || isPostgresqlDatabase(conn));
    }

    public static void commitIfHsqldb(DataSource dataSource) {
        commitIf(dataSource, conn -> isHsqlDatabase(conn));
    }

    public static void commitIf(DataSource dataSource, Function<Connection, Boolean> test) {
        Connection conn = DataSourceUtils.getConnection(dataSource);
        try {
            if (test.apply(conn) && DataSourceUtils.isConnectionTransactional(conn, dataSource)) {
                try {
                    conn.commit();
                } catch (SQLException e) {
                    log.warn("Cannot execute intermediate commit: " + e.getMessage(), e);
                }
            }
        } finally {
            DataSourceUtils.releaseConnection(conn, dataSource);
        }
    }

    /**
     * Do column names replacement, but escape sql keyword (e.g. 'DATE' replacement will keep TO_DATE(...) unchanged).
     * If ignoreCase=true, replacement will ignore the case.
     * @param sqlQuery
     * @param columnNamesMapping
     * @return
     */
    public static String sqlReplaceColumnNames(String sqlQuery, Map<String, String> columnNamesMapping, boolean ignoreCase) {
        if (MapUtils.isEmpty(columnNamesMapping)) return sqlQuery; // Skip

        String regexpOptions = (ignoreCase ? "(?i)" : "");
        for (Map.Entry<String, String> entry: columnNamesMapping.entrySet()) {
            String sourceColumnName = entry.getKey();
            String targetColumnName = entry.getValue();

            sqlQuery = sqlQuery
                // Do the replacement (should ignore case)
                .replaceAll(regexpOptions
                    + "(^|" + NON_COLUMN_CHAR_REGEXP + ")"
                        + sourceColumnName
                        + "("+NON_COLUMN_CHAR_REGEXP+"|$)",
                    "$1" + targetColumnName + "$2");
        }
        return sqlQuery;
    }
}

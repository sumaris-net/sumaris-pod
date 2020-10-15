package net.sumaris.core.util;

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

import org.hibernate.boot.model.naming.Identifier;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>*
 */
public class StringUtils extends org.apache.commons.lang3.StringUtils {

    public static String underscoreToChangeCase(String value) {
        if (org.apache.commons.lang3.StringUtils.isBlank(value)) return value;
        value = value.toLowerCase();

        // Replace underscore by case change
        int i = value.indexOf('_');
        do {
            if (i > 0 && i+1<value.length()) {
                value = value.substring(0, i)
                        + value.substring(i+1, i+2).toUpperCase()
                        + ((i+1<value.length()) ? value.substring(i + 2) : "");
            }
            // Start with a underscore
            else if (i == 0 && value.length() > 1) {
                value = value.substring(1);
            }
            // Finish with a underscore
            else if (i+1 == value.length()) {
                return value.substring(0, i);
            }
            i = value.indexOf('_', i+1);
        } while (i != -1);
        return value;
    }

    public static String changeCaseToUnderscore(String value) {
        if (org.apache.commons.lang3.StringUtils.isBlank(value)) return value;

        // Replace case change by an underscore
        String regex = "([a-z])([A-Z])";
        String replacement = "$1_$2";
        return value.replaceAll(regex, replacement).toLowerCase();
    }

    public static String doting(String... strings) {
        return join(strings, '.');
    }

    public static String slashing(String... strings) {
        return join(strings, '/');
    }

    /**
     * Method to encode a string value using `UTF-8` encoding scheme
     */
    public static String encodeValueForUrl(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex.getCause());
        }
    }

    public static Function<String, Boolean> startsWithFunction(String... prefixes) {
        return string -> Arrays.stream(prefixes).anyMatch(string::startsWith);
    }

    public static Function<String, Boolean> endsWithFunction(String... suffixes) {
        return string -> Arrays.stream(suffixes).anyMatch(string::endsWith);
    }

    public static String nullToEmpty(String str) {
        return str == null ? "" : str;
    }
}

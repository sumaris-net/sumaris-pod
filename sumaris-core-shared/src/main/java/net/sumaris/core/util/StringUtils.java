package net.sumaris.core.util;

import org.hibernate.boot.model.naming.Identifier;

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
}

package net.sumaris.core.util;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>*
 */
public class StringUtils extends org.apache.commons.lang3.StringUtils {

    public static String underscoreToChangeCase(String columnName) {
        if (org.apache.commons.lang3.StringUtils.isBlank(columnName)) return columnName;
        columnName = columnName.toLowerCase();

        // Replace underscore by case change
        int i = columnName.indexOf('_');
        do {
            if (i > 0 && i+1<columnName.length()) {
                columnName = columnName.substring(0, i)
                        + columnName.substring(i+1, i+2).toUpperCase()
                        + ((i+1<columnName.length()) ? columnName.substring(i + 2) : "");
            }
            // Start with a underscore
            else if (i == 0 && columnName.length() > 1) {
                columnName = columnName.substring(1);
            }
            // Finish with a underscore
            else if (i+1 == columnName.length()) {
                return columnName.substring(0, i);
            }
            i = columnName.indexOf('_', i+1);
        } while (i != -1);
        return columnName;
    }
}

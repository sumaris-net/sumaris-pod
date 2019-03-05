package net.sumaris.core.extraction.dao.technical;

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

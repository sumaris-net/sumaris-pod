package net.sumaris.core.extraction.technical;

import com.google.common.base.Joiner;

import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Useful method around DAO and entities.
 *
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 */
public class Daos extends net.sumaris.core.dao.technical.Daos {

    /**
     * Concat single quoted strings with ',' character, without parenthesis
     *
     * @param strings a {@link Collection} object.
     * @return concatenated strings
     */
    public static String getInStatementFromStringCollection(Collection<String> strings) {
        if (strings == null) return "";
        return Joiner.on(',').skipNulls().join(strings.stream().filter(Objects::nonNull).map(s -> "'" + s + "'").collect(Collectors.toSet()));
    }

    /**
     * Concat integers with ',' character, without parenthesis
     *
     * @param integers a {@link Collection} object.
     * @return concatenated integers
     */
    public static String getInStatementFromIntegerCollection(Collection<Integer> integers) {
        if (integers == null) return "";
        return Joiner.on(',').skipNulls().join(integers.stream().filter(Objects::nonNull).collect(Collectors.toSet()));
    }
}

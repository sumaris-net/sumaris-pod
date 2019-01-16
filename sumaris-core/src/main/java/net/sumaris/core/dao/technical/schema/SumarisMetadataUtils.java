package net.sumaris.core.dao.technical.schema;

import org.apache.commons.lang3.StringUtils;

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
}

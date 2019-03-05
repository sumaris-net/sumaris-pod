package net.sumaris.core.model.referential;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Validity Status
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>*
 */
public enum ValidityStatusEnum implements Serializable {

    INVALID(0),
    VALID(1),
    PENDING(2);

    public static ValidityStatusEnum valueOf(final int id) {
        return Arrays.stream(values())
                .filter(level -> level.id == id)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown ValidityStatusEnum: " + id));
    }

    private int id;

    ValidityStatusEnum(int id) {
        this.id = id;
    }

    /**
     * Returns the database row id
     *
     * @return int the id
     */
    public int getId()
    {
        return this.id;
    }

}

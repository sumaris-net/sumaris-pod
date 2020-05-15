package net.sumaris.core.model.referential;

import java.io.Serializable;
import java.util.Arrays;

/**
 * @author peck7 on 13/05/2020.
 */
public enum SaleTypeEnum implements Serializable {

    OTHER(4, "Other")
    ;

    public static SaleTypeEnum valueOf(final int id) {
        return Arrays.stream(values())
            .filter(level -> level.id == id)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown SaleTypeEnum: " + id));
    }

    private int id;
    private String label;

    SaleTypeEnum(int id, String label) {
        this.id = id;
        this.label = label;
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

    public String getLabel()
    {
        return this.label;
    }

    }

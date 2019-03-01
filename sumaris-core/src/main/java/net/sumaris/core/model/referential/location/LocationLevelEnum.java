package net.sumaris.core.model.referential.location;

import java.io.Serializable;
import java.util.Arrays;

public enum LocationLevelEnum implements Serializable {


    COUNTRY(1, "Country"),
    PORT(2, "Port"),
    RECTANGLE_ICES(3,"ICES_RECTANGLE"),
    RECTANGLE_CGPM_GFCM(4,"CGPM_GFCM_RECTANGLE"),
    SQUARE_10(5, "SQUARE_10")
    ;

    public static LocationLevelEnum valueOf(final int id) {
        return Arrays.stream(values())
                .filter(level -> level.id == id)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown LocationLevelEnum: " + id));
    }

    public static LocationLevelEnum byLabel(final String label) {
        return Arrays.stream(values())
                .filter(level -> level.label == label)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown LocationLevelEnum: " + label));
    }

    private int id;
    private String label;

    LocationLevelEnum(int id, String label) {
        this.id = id;
        this.label = label;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

}

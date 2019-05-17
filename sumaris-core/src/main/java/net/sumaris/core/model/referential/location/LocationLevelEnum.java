package net.sumaris.core.model.referential.location;

import java.io.Serializable;
import java.util.Arrays;

public enum LocationLevelEnum implements Serializable {


    COUNTRY(1, "Country"),
    HARBOUR(2, "Port"),
    AUCTION(3, "Auction"),
    RECTANGLE_ICES(4,"ICES_RECTANGLE"),
    RECTANGLE_CGPM_GFCM(5,"CGPM_GFCM_RECTANGLE"),
    SQUARE_10(6, "SQUARE_10"), // 10' x 10'
    SQUARE_3(7, "SQUARE_3") // 3' x 3'
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

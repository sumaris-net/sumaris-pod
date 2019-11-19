package net.sumaris.core.model.referential.location;

import java.io.Serializable;
import java.util.Arrays;

public enum LocationClassificationEnum implements Serializable {


    LAND(1, "LAND"),
    SEA(2, "SEA")
    ;

    public static LocationClassificationEnum valueOf(final int id) {
        return Arrays.stream(values())
                .filter(classification -> classification.id == id)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown LocationClassificationEnum: " + id));
    }

    public static LocationClassificationEnum byLabel(final String label) {
        return Arrays.stream(values())
                .filter(classification -> label.equals(classification.label))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown LocationClassificationEnum: " + label));
    }

    private int id;
    private String label;

    LocationClassificationEnum(int id, String label) {
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

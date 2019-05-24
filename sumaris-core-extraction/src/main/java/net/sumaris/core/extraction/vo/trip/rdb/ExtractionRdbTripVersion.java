package net.sumaris.core.extraction.vo.trip.rdb;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>*
 */
public enum ExtractionRdbTripVersion {

    // Version v1.3 P01 RDB data exchange format
    VERSION_1_3("1.3")
    ;

    private String label;

    ExtractionRdbTripVersion(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

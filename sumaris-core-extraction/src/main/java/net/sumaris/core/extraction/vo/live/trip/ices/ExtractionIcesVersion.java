package net.sumaris.core.extraction.vo.live.trip.ices;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>*
 */
public enum ExtractionIcesVersion {

    // Version v1.3 ICES RDB data exchange format
    VERSION_1_3("1.3")
    ;

    private String label;

    ExtractionIcesVersion(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

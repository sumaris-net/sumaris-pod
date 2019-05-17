package net.sumaris.core.extraction.vo.live.trip.cost;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>*
 */
public enum ExtractionCostTripVersion {

    // Format compatible with COST v1.4
    VERSION_1_4("1.4")
    ;

    private String label;

    ExtractionCostTripVersion(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

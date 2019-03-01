package net.sumaris.core.extraction.vo.trip.survivalTest;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>*
 */
public enum ExtractionSurvivalTestVersion {

    VERSION_1_0("1.0")
    ;

    private String label;

    ExtractionSurvivalTestVersion(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

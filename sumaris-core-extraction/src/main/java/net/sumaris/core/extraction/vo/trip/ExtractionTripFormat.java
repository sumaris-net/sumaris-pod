package net.sumaris.core.extraction.vo.trip;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>*
 */
public enum ExtractionTripFormat {

    ICES ("TR", "HH", "SL", "HL", "CA"),
    SURVIVAL_TEST("TR", "HH", "ST", "RL")
    ;

    private String[] sheetNames;

    ExtractionTripFormat(String... sheetNames) {
        this.sheetNames = sheetNames;
    }
    ExtractionTripFormat() {
        this.sheetNames = null;
    }

    public String[] getSheetNames() {
        return sheetNames;
    }
}

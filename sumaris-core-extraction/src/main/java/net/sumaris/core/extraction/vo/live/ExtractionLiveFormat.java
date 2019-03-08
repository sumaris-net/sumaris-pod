package net.sumaris.core.extraction.vo.live;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>*
 */
public enum ExtractionLiveFormat {

    ICES ("TR", "HH", "SL", "HL"),
    SURVIVAL_TEST("TR", "HH", "ST", "RL")
    ;

    private String[] sheetNames;

    ExtractionLiveFormat(String... sheetNames) {
        this.sheetNames = sheetNames;
    }
    ExtractionLiveFormat() {
        this.sheetNames = null;
    }

    public String[] getSheetNames() {
        return sheetNames;
    }
}

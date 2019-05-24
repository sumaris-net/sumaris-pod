package net.sumaris.core.extraction.vo;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>*
 */
public enum ExtractionRawFormatEnum {

    RDB("TR", "HH", "SL", "HL"),
    COST ("TR", "HH", "SL", "HL"),
    SURVIVAL_TEST("TR", "HH", "SL", "HL", "ST", "RL")
    ;

    private String[] sheetNames;

    ExtractionRawFormatEnum(String... sheetNames) {
        this.sheetNames = sheetNames;
    }
    ExtractionRawFormatEnum() {
        this.sheetNames = null;
    }

    public String[] getSheetNames() {
        return sheetNames;
    }
}

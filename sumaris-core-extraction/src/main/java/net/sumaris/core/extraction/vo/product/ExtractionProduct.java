package net.sumaris.core.extraction.vo.product;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>*
 */
public enum ExtractionProduct {

    ICES ("ICES",
            "TR", "HH", "SL", "HL", "CA", "CL", "CE")
    ;

    private String label;
    private String[] sheetNames;

    ExtractionProduct(String label, String... sheetNames) {
        this.label = label;
        this.sheetNames = sheetNames;
    }

    public String getLabel() {
        return label;
    }

    public String[] getSheetNames() {
        return sheetNames;
    }
}

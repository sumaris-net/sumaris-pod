package net.sumaris.core.extraction.vo.product;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>*
 */
public enum ExtractionProduct {

    P01_RDB("P01_RDB",
            "TR", "HH", "SL", "HL", "CL"
            // TODO add "CA", "CE"
            )
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

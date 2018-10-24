package net.sumaris.core.vo.referential;

import lombok.Data;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

@Data
public class PmfmVO extends ReferentialVO {

    private String unit;
    private String type;

    private Double minValue;
    private Double maxValue;
    private Integer maximumNumberDecimals;
    private Double defaultValue;

    private Boolean isEstimated; // from the method
    private Boolean isCalculated; // from the method

    List<ReferentialVO> qualitativeValues;
}

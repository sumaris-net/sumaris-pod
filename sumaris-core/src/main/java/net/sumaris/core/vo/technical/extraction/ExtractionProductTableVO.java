package net.sumaris.core.vo.technical.extraction;

import lombok.Data;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.vo.referential.IReferentialVO;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>*
 */
@Data
@FieldNameConstants
public class ExtractionProductTableVO implements IReferentialVO {

    private Integer id;
    private String label;
    private String name;
    private String description;
    private String comments;
    private Date updateDate;
    private Date creationDate;
    private Integer statusId;
    private Boolean isSpatial;

    private ExtractionProductVO product;
    private Integer productId;

    private String tableName;

    private String defaultSpaceColumn;
    private String defaultAggColumn;
    private String defaultTechColumn;

    private List<ExtractionProductColumnVO> columns;
    private Map<String, List<Object>> columnValues;

}

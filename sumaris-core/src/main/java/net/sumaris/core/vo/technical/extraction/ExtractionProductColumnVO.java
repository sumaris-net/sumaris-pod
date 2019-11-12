package net.sumaris.core.vo.technical.extraction;

import lombok.Data;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.dao.technical.model.IEntity;

import java.util.List;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>*
 */
@Data
@FieldNameConstants
public class ExtractionProductColumnVO implements IEntity<Integer> {

    private Integer id;
    private String label;
    private String name;
    private String columnName;

    private String type;
    private String description;
    private Integer rankOrder;

    //private ExtractionProductTableVO table;
    private Integer tableId;

    private List<String> values;

}

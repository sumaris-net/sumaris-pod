package net.sumaris.core.vo.technical.extraction;

import net.sumaris.core.dao.technical.model.IEntity;
import net.sumaris.core.vo.referential.IReferentialVO;

import java.util.Date;
import java.util.List;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>*
 */
@lombok.Data
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

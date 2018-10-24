package net.sumaris.core.vo.data;

import lombok.Data;
import net.sumaris.core.dao.technical.model.IUpdateDateEntityBean;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Data
public class BatchVO implements IUpdateDateEntityBean<Integer, Date> {
    public static final String PROPERTY_OPERATION = "operation";
    public static final String PROPERTY_BATCH = "batch";

    private Integer id;
    private String comments;
    private Date updateDate;
    private Date controlDate;
    private Date validationDate;
    private Integer qualityFlagId;
    private DepartmentVO recorderDepartment;
    private PersonVO recorderPerson;

    private String label;
    private Integer rankOrder;
    private Boolean exhaustiveInventory;
    private Double samplingRatio;
    private String samplingRatioText;
    private Integer individualCount;
    private ReferentialVO taxonGroup;

    private OperationVO operation;
    private Integer operationId;

    private BatchVO parent;
    private Integer parentId;
    private List<BatchVO> children;

    private List<MeasurementVO> sortingMeasurements;
    private List<MeasurementVO> quantificationMeasurements;

    private Map<Integer, String> measurementValues;

    private Map<Integer, String> sortingMeasurementValues;
    private Map<Integer, String> quantificationMeasurementValues;

    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}

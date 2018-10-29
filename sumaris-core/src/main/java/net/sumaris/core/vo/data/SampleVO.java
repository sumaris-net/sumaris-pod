package net.sumaris.core.vo.data;

import lombok.Data;
import net.sumaris.core.dao.technical.model.IUpdateDateEntityBean;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.referential.LocationVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Data
public class SampleVO implements IUpdateDateEntityBean<Integer, Date> {
    public static final String PROPERTY_SAMPLE_DATE = "sampleDate";
    public static final String PROPERTY_OPERATION = "operation";

    private Integer id;
    private String comments;
    private Date creationDate;
    private Date updateDate;
    private Date controlDate;
    private Date validationDate;
    private Integer qualityFlagId;
    private DepartmentVO recorderDepartment;
    private PersonVO recorderPerson;

    private String label;
    private Date sampleDate;
    private Integer rankOrder;
    private Integer individualCount;
    private ReferentialVO matrix;
    private ReferentialVO taxonGroup;

    private SampleVO parent;
    private Integer parentId;
    private List<SampleVO> children;

    private BatchVO batch;
    private Integer batchId;
    private OperationVO operation;
    private Integer operationId;

    private List<MeasurementVO> measurements; // sample_measurement
    private Map<Integer, String> measurementValues; // sample_measurement

    public String toString() {
        return new StringBuilder().append("SampleVO(")
                .append("id=").append(id)
                .append(",label=").append(label)
                .append(")").toString();
    }
}

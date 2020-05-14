package net.sumaris.core.vo.data;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.administration.user.PersonVO;

import java.util.Date;
import java.util.List;

/**
 * @author peck7 on 09/04/2020.
 */
@Data
@FieldNameConstants
@EqualsAndHashCode
public class PacketVO implements IDataVO<Integer> {

    @EqualsAndHashCode.Exclude
    private Integer id;
    private String comments;
    @EqualsAndHashCode.Exclude
    private Date updateDate;
    private Date controlDate;
    private Date validationDate;
    private Date qualificationDate;
    private String qualificationComments;
    private Integer qualityFlagId;
    private DepartmentVO recorderDepartment;
    private PersonVO recorderPerson;

    private Integer rankOrder;
    private Integer number;
    private Double weight;
    private List<Double> sampledWeights;
    private List<PacketCompositionVO> composition;
    private List<ProductVO> saleProducts;

    @EqualsAndHashCode.Exclude
    private OperationVO operation;
    private Integer operationId;

    public String toString() {
        return "PacketVO(id=" + id + ", rankOrder=" + rankOrder +
            ", number=" + number + ", weight=" + weight +
            ", composition=" + composition.toString() + ")";
    }
}

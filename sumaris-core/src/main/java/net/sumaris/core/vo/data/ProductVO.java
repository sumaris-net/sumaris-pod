package net.sumaris.core.vo.data;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.model.data.IWithRecorderPersonEntity;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.referential.ReferentialVO;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author peck7 on 28/11/2019.
 *
 */

@Data
@FieldNameConstants
@EqualsAndHashCode
public class ProductVO implements IDataVO<Integer>, IWithRecorderPersonEntity<Integer, PersonVO> {

    private Integer id;
    private String label;
    private String comments;
    private Date creationDate;
    private Date updateDate;
    private Date controlDate;
    private Date validationDate;
    private Date qualificationDate;
    private String qualificationComments;
    private Integer qualityFlagId;
    private DepartmentVO recorderDepartment;
    private PersonVO recorderPerson;

    private Integer rankOrder;
    private Integer individualCount;
    private Double subgroupCount;
    private ReferentialVO taxonGroup;
    private ReferentialVO saleType;

    private Double weight;
    private boolean weightCalculated;

    // Mapped as measurements
    private Integer dressingId;
    private Integer preservationId;
    private Integer sizeCategoryId;
    private Double cost;
    private List<ProductVO> saleProducts;

    private Map<Integer, String> measurementValues; // = sorting_measurement_p or quantification_measurement_p

    @EqualsAndHashCode.Exclude
    private OperationVO operation;
    private Integer operationId;

    @EqualsAndHashCode.Exclude
    private SaleVO sale;
    private Integer saleId;

    @EqualsAndHashCode.Exclude
    private LandingVO landing;
    private Integer landingId;

    @EqualsAndHashCode.Exclude
    private BatchVO batch;
    private Integer batchId;
}

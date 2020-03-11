package net.sumaris.core.vo.data;

import lombok.Data;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.vo.administration.programStrategy.ProgramVO;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.administration.user.PersonVO;

import java.util.Date;

/**
 * @author peck7 on 28/11/2019.
 *
 * TODO à finir: cf. create-views & create-triggers
 * il manque notamment WEIGHT, COST; et LABEL est non nul ?
 */

@Data
@FieldNameConstants
public class ProductVO implements IRootDataVO<Integer> {

    private Integer id;
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
    private ProgramVO program;


}

package net.sumaris.core.vo.data;

import lombok.Data;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.vo.administration.programStrategy.ProgramVO;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.referential.ReferentialVO;

import java.util.Date;

/**
 * @author peck7 on 19/11/2019.
 */
@Data
@FieldNameConstants
public class VesselVO implements IRootDataVO<Integer> {

    private Integer id;
    private ReferentialVO vesselType;
    private Integer statusId;
    private String comments;
    private ProgramVO program;

    // Features
    private VesselFeaturesVO features;

    // Registration
    private VesselRegistrationVO registration;

    private Date creationDate;
    private Date updateDate;
    private Date controlDate;
    private Date validationDate;
    private Date qualificationDate;
    private String qualificationComments;
    private Integer qualityFlagId;
    private DepartmentVO recorderDepartment;
    private PersonVO recorderPerson;

}

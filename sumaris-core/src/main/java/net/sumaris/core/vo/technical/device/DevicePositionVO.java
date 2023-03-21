package net.sumaris.core.vo.technical.device;

import lombok.Data;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.model.IEntity;
import net.sumaris.core.model.IValueObject;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.data.IDataVO;
import net.sumaris.core.vo.referential.ReferentialVO;

import java.util.Date;
import java.util.List;

@Data
@FieldNameConstants
public class DevicePositionVO implements IDataVO<Integer> {

    private Integer id;
    private Date dateTime;
    private Double latitude;
    private Double longitude;
    private Integer objectId;
    private ReferentialVO objectType;
    private Integer objectTypeId;
    private PersonVO recorderPerson;
    private Integer recorderPersonId;
    private DepartmentVO recorderDepartment;
    private Integer recorderDepartmentId;
    private Date updateDate;
    private Date creationDate;

    private Date controlDate;
    private Date validationDate;
    private Integer qualityFlagId;
    private Date qualificationDate;
    private String qualificationComments;

}

package net.sumaris.core.vo.data;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.dao.technical.model.IEntity;
import net.sumaris.core.dao.technical.model.IValueObject;
import net.sumaris.core.vo.referential.LocationVO;
import net.sumaris.core.vo.referential.ReferentialVO;

import java.util.Date;

/**
 * @author peck7 on 09/06/2020.
 */
@Data
@FieldNameConstants
@EqualsAndHashCode
public class FishingAreaVO implements IEntity<Integer>, IValueObject<Integer> {

    private Integer id;
    private Date qualificationDate;
    private String qualificationComments;
    private Integer qualityFlagId;

    private LocationVO location;

    private ReferentialVO distanceToCoastGradient;
    private ReferentialVO depthGradient;
    private ReferentialVO nearbySpecificArea;

    // parent
    @EqualsAndHashCode.Exclude
    private OperationVO operation;
    private Integer operationId;

}

package net.sumaris.core.vo.technical.device;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.model.data.DataQualityStatusEnum;
import net.sumaris.core.vo.filter.IDataFilter;

@Data
@Builder
@FieldNameConstants
public class DevicePositionFilterVO implements IDataFilter {

    private Integer recorderDepartmentId;
    private Integer[] qualityFlagIds;
    private DataQualityStatusEnum[] dataQualityStatus;
}

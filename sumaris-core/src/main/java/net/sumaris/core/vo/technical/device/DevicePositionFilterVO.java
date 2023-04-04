package net.sumaris.core.vo.technical.device;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.model.data.DataQualityStatusEnum;
import net.sumaris.core.vo.filter.IDataFilter;
import net.sumaris.core.vo.filter.TripFilterVO;

import javax.annotation.Nullable;
import java.util.Date;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldNameConstants
public class DevicePositionFilterVO implements IDataFilter {

    public static DevicePositionFilterVO nullToEmpty(@Nullable DevicePositionFilterVO filter) {
        return filter == null ? new DevicePositionFilterVO() : filter;
    }

    private Date startDate;
    private Date endDate;

    private Integer objectId;
    private String objectTypeLabel;
    private Integer objectTypeId;
    private Integer recorderDepartmentId;
    private Integer recorderPersonId;
    private Integer[] qualityFlagIds;
    private DataQualityStatusEnum[] dataQualityStatus;
}

package net.sumaris.core.vo.data.batch;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.model.data.DataQualityStatusEnum;
import net.sumaris.core.vo.filter.IDataFilter;

@Data
@FieldNameConstants
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DenormalizedBatchesFilterVO implements IDataFilter {
    public static DenormalizedBatchesFilterVO nullToEmpty(DenormalizedBatchesFilterVO f) {
        return f != null ? f : new DenormalizedBatchesFilterVO();
    }

    private Integer tripId;
    private Integer operationId;
    private Integer observedLocationId;
    private Integer saleId;
    private Integer recorderDepartmentId;

    private  Boolean isLanding;
    private  Boolean isDiscard;

    // Quality
    private Integer[] qualityFlagIds;
    private DataQualityStatusEnum[] dataQualityStatus;

}

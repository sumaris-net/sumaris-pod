package net.sumaris.core.vo.technical.device;

import lombok.Builder;
import lombok.Data;
import net.sumaris.core.vo.data.IDataFetchOptions;

@Data
@Builder
public class DevicePositionFetchOptions implements IDataFetchOptions {
    @Builder.Default
    private boolean withRecorderDepartment = false;

    @Builder.Default
    private boolean withRecorderPerson = true;

    @Builder.Default
    private boolean withObservers = false;

    @Builder.Default
    private boolean withChildrenEntities = false;

    @Builder.Default
    private boolean withMeasurementValues = false;
}

package net.sumaris.core.dao.technical.device;

import net.sumaris.core.dao.data.DataRepository;
import net.sumaris.core.model.technical.device.DevicePosition;
import net.sumaris.core.vo.technical.device.DevicePositionFetchOptions;
import net.sumaris.core.vo.technical.device.DevicePositionFilterVO;
import net.sumaris.core.vo.technical.device.DevicePositionVO;
import org.springframework.stereotype.Repository;

@Repository
public interface DevicePositionRepository
        extends DataRepository<DevicePosition, DevicePositionVO, DevicePositionFilterVO, DevicePositionFetchOptions>,
            DevicePositionSpecifications {

}

package net.sumaris.core.service.technical.device;

import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.vo.technical.device.DevicePositionFetchOptions;
import net.sumaris.core.vo.technical.device.DevicePositionFilterVO;
import net.sumaris.core.vo.technical.device.DevicePositionVO;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Transactional
public interface DevicePositionService {
    List<DevicePositionVO> findByFilter(DevicePositionFilterVO filter, Page page, DevicePositionFetchOptions fetchOptions);
    Optional<DevicePositionVO> findById(int id, DevicePositionFetchOptions fetchOptions);
    DevicePositionVO save(DevicePositionVO source);
    void delete(int id);
}

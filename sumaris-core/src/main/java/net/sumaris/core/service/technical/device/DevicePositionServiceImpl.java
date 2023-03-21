package net.sumaris.core.service.technical.device;

import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.dao.technical.device.DevicePositionRepository;
import net.sumaris.core.vo.technical.device.DevicePositionFetchOptions;
import net.sumaris.core.vo.technical.device.DevicePositionFilterVO;
import net.sumaris.core.vo.technical.device.DevicePositionVO;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Optional;

@Service("devicePositionService")
public class DevicePositionServiceImpl implements DevicePositionService {

    @Resource
    private DevicePositionRepository devicePositionRepository;

    @Override
    public List<DevicePositionVO> findByFilter(DevicePositionFilterVO filter, Page page, DevicePositionFetchOptions fetchOptions) {
        return devicePositionRepository.findAll(filter, page, fetchOptions);
    }

    @Override
    public Optional<DevicePositionVO> findById(int id, DevicePositionFetchOptions fetchOptions) {
        return devicePositionRepository.findById(id, fetchOptions);
    }

    @Override
    public DevicePositionVO save(DevicePositionVO source) {
        return devicePositionRepository.save(source);
    }

    @Override
    public void delete(int id) {
        devicePositionRepository.deleteById(id);
    }
}

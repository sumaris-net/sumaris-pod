package net.sumaris.core.service.technical.device;

import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.dao.technical.device.DevicePositionRepository;
import net.sumaris.core.vo.data.DataFetchOptions;
import net.sumaris.core.vo.technical.device.DevicePositionFilterVO;
import net.sumaris.core.vo.technical.device.DevicePositionVO;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Optional;

@Service("devicePositionService")
@Slf4j
public class DevicePositionServiceImpl implements DevicePositionService {

    @Resource
    private DevicePositionRepository devicePositionRepository;

    // TODO
//    @Override
//    public List<DevicePositionVO> findAll(DataFetchOptions filter,
//                                          Page page,
//                                          DataFetchOptions fetchOptions) {
//        return devicePositionRepository.findAll(DataFetchOptions.nullToEmpty(filter), page, fetchOptions);
//    }
    @Override
    public List<DevicePositionVO> findAll(DevicePositionFilterVO filter, Page page, DataFetchOptions fetchOptions) {
        return null;
    }

    @Override
    public List<DevicePositionVO> findAll(
            DevicePositionFilterVO filter,
            int offset,
            int size,
            String sortAttribute,
            SortDirection sortDirection,
            DataFetchOptions fetchOptions
    ) {
        return devicePositionRepository.findAll(DevicePositionFilterVO.nullToEmpty(filter), offset, size, sortAttribute, sortDirection, fetchOptions);
    }


    @Override
    public Optional<DevicePositionVO> findById(int id, DataFetchOptions fetchOptions) {
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

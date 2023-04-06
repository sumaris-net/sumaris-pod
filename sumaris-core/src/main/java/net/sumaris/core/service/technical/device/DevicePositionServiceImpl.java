package net.sumaris.core.service.technical.device;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.dao.technical.device.DevicePositionRepository;
import net.sumaris.core.util.Beans;
import net.sumaris.core.vo.data.DataFetchOptions;
import net.sumaris.core.vo.technical.device.DevicePositionFilterVO;
import net.sumaris.core.vo.technical.device.DevicePositionVO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service("devicePositionService")
@RequiredArgsConstructor
@Slf4j
public class DevicePositionServiceImpl implements DevicePositionService {

    private final DevicePositionRepository devicePositionRepository;

    @Override
    public List<DevicePositionVO> findAll(DevicePositionFilterVO filter,
                                          Page page,
                                          DataFetchOptions fetchOptions) {
        return devicePositionRepository.findAll(DevicePositionFilterVO.nullToEmpty(filter),
            page, fetchOptions);
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
        return devicePositionRepository.findAll(DevicePositionFilterVO.nullToEmpty(filter),
            offset, size,
            sortAttribute, sortDirection,
            fetchOptions);
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
    public List<DevicePositionVO> saveAll(List<DevicePositionVO> sources) {
        return Beans.getStream(sources)
            .map(devicePositionRepository::save)
            .toList();
    }


    @Override
    public void delete(int id) {
        devicePositionRepository.deleteById(id);
    }

    @Override
    public void deleteAll(List<Integer> ids) {
        Beans.getStream(ids)
            .forEach(devicePositionRepository::deleteById);
    }

    public void deleteByFilter(DevicePositionFilterVO filter) {
        List<DevicePositionVO> position = devicePositionRepository.findAll(filter);
        List<Integer> positionIds = position.stream().map(DevicePositionVO::getId).toList();
        deleteAll(positionIds);
    }

    @Override
    public Long countByFilter(DevicePositionFilterVO filter) {
        return devicePositionRepository.count(filter);
    }

}

package net.sumaris.core.service.technical.device;

import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.vo.data.DataFetchOptions;
import net.sumaris.core.vo.technical.device.DevicePositionFilterVO;
import net.sumaris.core.vo.technical.device.DevicePositionVO;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Transactional
public interface DevicePositionService {

    @Transactional(readOnly = true)
    List<DevicePositionVO> findAll(DevicePositionFilterVO filter,
                                   int offset,
                                   int size,
                                   String sortAttribute,
                                   SortDirection sortDirection,
                                   DataFetchOptions fetchOptions);

    @Transactional(readOnly = true)
    List<DevicePositionVO> findAll(DevicePositionFilterVO filter,
                                   Page page,
                                   DataFetchOptions fetchOptions);

    @Transactional(readOnly = true)
    Long countByFilter(DevicePositionFilterVO filter);

    @Transactional(readOnly = true)
    Optional<DevicePositionVO> findById(int id, DataFetchOptions fetchOptions);

    DevicePositionVO save(DevicePositionVO source);

    List<DevicePositionVO> saveAll(List<DevicePositionVO> sources);
    void delete(int id);

    void deleteAll(List<Integer> ids);

}
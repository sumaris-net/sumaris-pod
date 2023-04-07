package net.sumaris.core.service.technical.device;

/*-
 * #%L
 * SUMARiS:: Core
 * %%
 * Copyright (C) 2018 - 2023 SUMARiS Consortium
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

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

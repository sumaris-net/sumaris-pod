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

    void deleteByFilter(DevicePositionFilterVO filter);
}

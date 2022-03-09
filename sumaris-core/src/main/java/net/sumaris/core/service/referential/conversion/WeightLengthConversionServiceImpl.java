/*
 * #%L
 * SUMARiS
 * %%
 * Copyright (C) 2019 SUMARiS Consortium
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

package net.sumaris.core.service.referential.conversion;

import com.google.common.collect.ListMultimap;
import net.sumaris.core.dao.referential.conversion.WeightLengthConversionRepository;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.model.referential.location.LocationLevels;
import net.sumaris.core.service.referential.LocationService;
import net.sumaris.core.util.Beans;
import net.sumaris.core.vo.filter.LocationFilterVO;
import net.sumaris.core.vo.referential.LocationVO;
import net.sumaris.core.vo.referential.conversion.WeightLengthConversionFetchOptions;
import net.sumaris.core.vo.referential.conversion.WeightLengthConversionFilterVO;
import net.sumaris.core.vo.referential.conversion.WeightLengthConversionVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

@Service("weightLengthConversionService")
public class WeightLengthConversionServiceImpl implements WeightLengthConversionService {

    @Resource
    private WeightLengthConversionRepository weightLengthConversionRepository;

    @Resource
    private LocationService locationService;

    @Override
    public List<WeightLengthConversionVO> findByFilter(WeightLengthConversionFilterVO filter,
                                                       Page page,
                                                       WeightLengthConversionFetchOptions fetchOptions) {
        List<WeightLengthConversionVO> result = weightLengthConversionRepository.findAll(filter, page, fetchOptions);

        // Add rectangles
        if (fetchOptions != null && fetchOptions.isWithRectangleLabels()) {
            ListMultimap<Integer, WeightLengthConversionVO> groupByLocationId = Beans.splitByNotUniqueProperty(result,
                WeightLengthConversionVO.Fields.LOCATION_ID);
            groupByLocationId.keySet().forEach(locationId -> {
                String[] rectangleLabels = locationService.findByFilter(LocationFilterVO.builder()
                    .ancestorIds(new Integer[]{locationId})
                    .levelIds(LocationLevels.getStatisticalRectangleLevelIds())
                    .statusIds(filter.getStatusIds())
                    .build()).stream().map(LocationVO::getLabel)
                    .collect(Collectors.toList())
                    .toArray(new String[0]);
                
                // Update 
                groupByLocationId.get(locationId).forEach(item -> item.setRectangleLabels(rectangleLabels));
            });
        }

        return result;
    }

    @Override
    public long countByFilter(WeightLengthConversionFilterVO filter) {
        return weightLengthConversionRepository.count(filter);
    }

    @Override
    public List<WeightLengthConversionVO> saveAll(List<WeightLengthConversionVO> sources) {
        return sources.stream()
            .map(weightLengthConversionRepository::save)
            .collect(Collectors.toList());
    }

    @Override
    public void deleteAllById(List<Integer> ids) {
        weightLengthConversionRepository.deleteAllById(ids);
    }
}

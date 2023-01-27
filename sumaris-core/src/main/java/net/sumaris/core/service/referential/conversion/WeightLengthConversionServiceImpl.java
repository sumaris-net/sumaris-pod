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

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import lombok.NonNull;
import net.sumaris.core.dao.referential.conversion.WeightLengthConversionRepository;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.model.referential.StatusEnum;
import net.sumaris.core.model.referential.location.LocationLevels;
import net.sumaris.core.service.referential.LocationService;
import net.sumaris.core.service.referential.pmfm.PmfmService;
import net.sumaris.core.util.Beans;
import net.sumaris.core.vo.filter.LocationFilterVO;
import net.sumaris.core.vo.filter.PmfmPartsVO;
import net.sumaris.core.vo.referential.LocationVO;
import net.sumaris.core.vo.referential.conversion.WeightLengthConversionFetchOptions;
import net.sumaris.core.vo.referential.conversion.WeightLengthConversionFilterVO;
import net.sumaris.core.vo.referential.conversion.WeightLengthConversionVO;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

@Service("weightLengthConversionService")
public class WeightLengthConversionServiceImpl implements WeightLengthConversionService {

    @Resource
    private WeightLengthConversionRepository weightLengthConversionRepository;

    @Resource
    private PmfmService pmfmService;

    @Resource
    private LocationService locationService;

    @Override
    public List<WeightLengthConversionVO> findByFilter(WeightLengthConversionFilterVO filter,
                                                       Page page,
                                                       WeightLengthConversionFetchOptions fetchOptions) {
        List<WeightLengthConversionVO> result = weightLengthConversionRepository.findAll(filter, page, fetchOptions);

        // Add length pmfm Ids
        if (fetchOptions != null && fetchOptions.isWithRectangleLabels()) {
            // Group by [parameter, unit]
            Joiner mapKeyJoiner = Joiner.on('|');
            Splitter mapKeySplitter = Splitter.on('|').limit(2);
            ListMultimap<String, WeightLengthConversionVO> groupByLengthParts = result.stream().collect(
                ArrayListMultimap::create, (m, vo) -> {
                    String key = mapKeyJoiner.join(vo.getLengthParameterId(), vo.getLengthUnitId());
                    m.put(key, vo);
                }, Multimap::putAll);

            groupByLengthParts.keys().forEach(key -> {
                // Parse the group key
                Iterator<String> parts = mapKeySplitter.split(key).iterator();
                Integer parameterId = Integer.parseInt(parts.next());
                Integer unitId = Integer.parseInt(parts.next());

                // Get Pmfm IDS
                Integer[] pmfmIds = pmfmService.findIdsByParts(PmfmPartsVO.builder()
                    .parameterId(parameterId)
                    .unitId(unitId)
                    .statusId(StatusEnum.ENABLE.getId())
                    .build()).toArray(new Integer[0]);

                // Update vos
                if (ArrayUtils.isNotEmpty(pmfmIds)) {
                    groupByLengthParts.get(key).forEach(vo -> vo.setLengthPmfmIds(pmfmIds));
                }
            });
        }

        // Add rectangle labels
        if (fetchOptions != null && fetchOptions.isWithRectangleLabels()) {
            // Group by location id
            ListMultimap<Integer, WeightLengthConversionVO> groupByLocationId = Beans.splitByNotUniqueProperty(result,
                WeightLengthConversionVO.Fields.LOCATION_ID);
            groupByLocationId.keySet().forEach(locationId -> {
                // Get rectangle labels
                String[] rectangleLabels = locationService.findByFilter(LocationFilterVO.builder()
                    .ancestorIds(new Integer[]{locationId})
                    .levelIds(LocationLevels.getStatisticalRectangleLevelIds())
                    .statusIds(filter.getStatusIds())
                    .build()).stream()
                    .map(LocationVO::getLabel)
                    .toArray(String[]::new);
                
                // Update vos
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
    public BigDecimal computedWeight(@NonNull WeightLengthConversionVO conversion,
                                     @NonNull Number length,
                                     int scale,
                                     Number individualCount) {

        // CoefA * length ^ CoefB
        BigDecimal result = new BigDecimal(conversion.getConversionCoefficientA().toString())
            .multiply(new BigDecimal(
                Math.pow(length.doubleValue(), conversion.getConversionCoefficientB().doubleValue())
            ))
            // * individual count
            .multiply(new BigDecimal(individualCount != null ? individualCount.toString() : "1"));

        // Compute alive weight

        // Round to scale
        result = result.divide(new BigDecimal(1), scale, RoundingMode.HALF_UP);

        return result;
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

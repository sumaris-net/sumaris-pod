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
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import lombok.NonNull;
import net.sumaris.core.config.CacheConfiguration;
import net.sumaris.core.dao.referential.conversion.WeightLengthConversionRepository;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.model.referential.StatusEnum;
import net.sumaris.core.model.referential.conversion.WeightLengthConversion;
import net.sumaris.core.model.referential.location.LocationLevels;
import net.sumaris.core.model.referential.pmfm.UnitEnum;
import net.sumaris.core.service.referential.LocationService;
import net.sumaris.core.service.referential.pmfm.PmfmService;
import net.sumaris.core.util.Beans;
import net.sumaris.core.util.conversion.UnitConversions;
import net.sumaris.core.vo.filter.LocationFilterVO;
import net.sumaris.core.vo.filter.PmfmPartsVO;
import net.sumaris.core.vo.referential.conversion.WeightLengthConversionFetchOptions;
import net.sumaris.core.vo.referential.conversion.WeightLengthConversionFilterVO;
import net.sumaris.core.vo.referential.conversion.WeightLengthConversionVO;
import net.sumaris.core.vo.referential.location.LocationVO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

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

        // Add pmfm Ids into VO
        if (fetchOptions != null && fetchOptions.isWithLengthPmfmIds()) {
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
    @Cacheable(cacheNames = CacheConfiguration.Names.WEIGHT_LENGTH_CONVERSION_FIRST_BY_FILTER, key = "#filter.hashCode()")
    public Optional<WeightLengthConversionVO> loadFirstByFilter(WeightLengthConversionFilterVO filter) {
        return loadFirstByFilter(filter, WeightLengthConversionFetchOptions.DEFAULT);
    }

    @Override
    @Cacheable(cacheNames = CacheConfiguration.Names.WEIGHT_LENGTH_CONVERSION_FIRST_BY_FILTER, key = "#filter.hashCode() * #fetchOptions.hashCode()")
    public Optional<WeightLengthConversionVO> loadFirstByFilter(@NonNull WeightLengthConversionFilterVO filter, @NonNull WeightLengthConversionFetchOptions fetchOptions) {
        Preconditions.checkArgument(ArrayUtils.isNotEmpty(filter.getReferenceTaxonIds()), "Require at least on referenceTaxonId");
        Preconditions.checkArgument(ArrayUtils.isNotEmpty(filter.getLocationIds())
            || ArrayUtils.isNotEmpty(filter.getChildLocationIds())
            || ArrayUtils.isNotEmpty(filter.getRectangleLabels()), "Require at least one of rectangleLabels, childLocationIds or locationIds");

        final Page page = Page.builder().size(1)
            .sortBy(filter.getYear() != null ? WeightLengthConversion.Fields.YEAR : WeightLengthConversion.Fields.START_MONTH)
            .sortDirection(SortDirection.DESC)
            .build();

        // First, try with full filter
        List<WeightLengthConversionVO> matches = this.findByFilter(filter, page, fetchOptions);
        if (CollectionUtils.isNotEmpty(matches)) return Optional.of(matches.get(0));

        if (filter.getYear() != null && filter.getMonth() != null) {
            // Retry on year only (without month)
            WeightLengthConversionFilterVO filterWithoutMonth = filter.clone(); // Copy, to keep original filter unchanged
            filterWithoutMonth.setMonth(null);
            matches = this.findByFilter(filterWithoutMonth, page, fetchOptions);
            if (CollectionUtils.isNotEmpty(matches)) return Optional.of(matches.get(0));

            // Retry on month only (without year)
            WeightLengthConversionFilterVO filterWithoutYear = filter.clone(); // Copy, to keep original filter unchanged
            filterWithoutYear.setYear(null);
            page.setSortBy(WeightLengthConversion.Fields.YEAR);
            matches = this.findByFilter(filterWithoutYear, page, fetchOptions);
            if (CollectionUtils.isNotEmpty(matches)) return Optional.of(matches.get(0));
        }

        // Retry on parameter Id (=skip unit match)
        if (ArrayUtils.isNotEmpty(filter.getLengthPmfmIds()) && ArrayUtils.isEmpty(filter.getLengthParameterIds())) {
            // TODO loop on parameterIds then if result
        }

        // Not found
        return Optional.empty();
    }

    @Override
    public BigDecimal computedWeight(@NonNull WeightLengthConversionVO conversion,
                                     @NonNull Number length,
                                     @NonNull String lengthUnit,
                                     @Nullable Number lengthPrecision,
                                     @Nullable Number individualCount,
                                     @NonNull String weightUnit,
                                     int weightScale) {
        BigDecimal lengthDecimal = new BigDecimal(length.toString());

        // Convert length to expected unit
        String conversionLengthUnit = conversion.getLengthUnit() != null
            ? conversion.getLengthUnit().getLabel()
            : UnitEnum.valueOf(conversion.getLengthUnitId()).getLabel();
        if (!Objects.equals(conversionLengthUnit, lengthUnit)) {
            // create a conversion factor
            BigDecimal lengthUnitConversion = BigDecimal.valueOf(UnitConversions.lengthToMeterConversion(conversionLengthUnit))
                .divide(new BigDecimal(UnitConversions.lengthToMeterConversion(lengthUnit)));

            // Convert length to the expected unit
            lengthDecimal = lengthDecimal.multiply(lengthUnitConversion);

            // Round to half of the precision (see Allegro mantis #5598)
            if (lengthPrecision != null) {
                // length += 0.5 * precision * lengthUnitConversion
                lengthDecimal = lengthDecimal.add(
                    new BigDecimal("0.5")
                        .multiply(new BigDecimal(lengthPrecision.toString()))
                        .multiply(lengthUnitConversion));
            }
        } else {
            // length += 0.5 * precision (Fix issue sumaris-app#522)
            lengthDecimal = lengthDecimal.add(
                new BigDecimal("0.5")
                    .multiply(new BigDecimal(lengthPrecision.toString())));
        }

        // CoefA * length ^ CoefB
        BigDecimal weightKg = BigDecimal.valueOf(conversion.getConversionCoefficientA())
            .multiply(BigDecimal.valueOf(
                    Math.pow(lengthDecimal.doubleValue(), conversion.getConversionCoefficientB())
            ))
            // * individual count
            .multiply(new BigDecimal(individualCount != null ? individualCount.toString() : "1"));

        // Convert to expected weight unit (kg by default)
        if (!Objects.equals(weightUnit, "kg")) {
            return weightKg.divide(
                BigDecimal.valueOf(UnitConversions.weightToKgConversion(weightUnit)),
                weightScale,
                RoundingMode.HALF_UP
            );
        }

        // Round to expected weight scale
        return weightKg.divide(new BigDecimal(1), weightScale, RoundingMode.HALF_UP);
    }

    @Override
    @Caching(
        evict = {
            @CacheEvict(cacheNames = CacheConfiguration.Names.WEIGHT_LENGTH_CONVERSION_FIRST_BY_FILTER, allEntries = true),
            @CacheEvict(cacheNames = CacheConfiguration.Names.WEIGHT_LENGTH_CONVERSION_IS_LENGTH_PARAMETER_ID, allEntries = true),
            @CacheEvict(cacheNames = CacheConfiguration.Names.WEIGHT_LENGTH_CONVERSION_IS_LENGTH_PMFM_ID, allEntries = true)
        }
    )
    public List<WeightLengthConversionVO> saveAll(List<WeightLengthConversionVO> sources) {
        return Beans.getStream(sources)
            .map(weightLengthConversionRepository::save)
            .toList();
    }

    @Override
    @Caching(
        evict = {
            @CacheEvict(cacheNames = CacheConfiguration.Names.WEIGHT_LENGTH_CONVERSION_FIRST_BY_FILTER, allEntries = true),
            @CacheEvict(cacheNames = CacheConfiguration.Names.WEIGHT_LENGTH_CONVERSION_IS_LENGTH_PARAMETER_ID, allEntries = true),
            @CacheEvict(cacheNames = CacheConfiguration.Names.WEIGHT_LENGTH_CONVERSION_IS_LENGTH_PMFM_ID, allEntries = true)
        }
    )
    public void deleteAllById(List<Integer> ids) {
        weightLengthConversionRepository.deleteAllById(ids);
    }

    @Override
    @Cacheable(cacheNames = CacheConfiguration.Names.WEIGHT_LENGTH_CONVERSION_IS_LENGTH_PARAMETER_ID)
    public boolean isWeightLengthParameter(int parameterId) {
        return weightLengthConversionRepository.count(WeightLengthConversionFilterVO.builder()
            .lengthParameterIds(new Integer[]{parameterId})
            .build()) > 0;
    }

    @Override
    @Cacheable(cacheNames = CacheConfiguration.Names.WEIGHT_LENGTH_CONVERSION_IS_LENGTH_PMFM_ID)
    public boolean isWeightLengthPmfm(int pmfmId) {
        return weightLengthConversionRepository.count(WeightLengthConversionFilterVO.builder()
            .lengthPmfmIds(new Integer[]{pmfmId})
            .build()) > 0;
    }
}

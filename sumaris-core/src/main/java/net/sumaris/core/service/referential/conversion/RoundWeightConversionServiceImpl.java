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

import com.google.common.base.Preconditions;
import lombok.NonNull;
import net.sumaris.core.config.CacheConfiguration;
import net.sumaris.core.dao.referential.conversion.RoundWeightConversionRepository;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.model.referential.conversion.RoundWeightConversion;
import net.sumaris.core.vo.referential.conversion.RoundWeightConversionFetchOptions;
import net.sumaris.core.vo.referential.conversion.RoundWeightConversionFilterVO;
import net.sumaris.core.vo.referential.conversion.RoundWeightConversionVO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service("roundWeightConversionService")
public class RoundWeightConversionServiceImpl implements RoundWeightConversionService {

    private static final Page FIND_FIRST_PAGE = Page.builder().size(1)
        .sortBy(RoundWeightConversion.Fields.START_DATE)
        .sortDirection(SortDirection.DESC)
        .build();

    @Resource
    private RoundWeightConversionRepository roundWeightConversionRepository;

    @Override
    public List<RoundWeightConversionVO> findByFilter(RoundWeightConversionFilterVO filter,
                                                       Page page,
                                                       RoundWeightConversionFetchOptions fetchOptions) {
        return roundWeightConversionRepository.findAll(filter, page, fetchOptions);
    }

    @Override
    public Optional<RoundWeightConversionVO> findFirstByFilter(@NonNull RoundWeightConversionFilterVO filter) {
        return findFirstByFilter(filter, RoundWeightConversionFetchOptions.DEFAULT);
    }

    @Override
    @Cacheable(cacheNames = CacheConfiguration.Names.ROUND_WEIGHT_CONVERSION_FIRST_BY_FILTER, key = "#filter.hashCode() * #fetchOptions.hashCode()")
    public Optional<RoundWeightConversionVO> findFirstByFilter(
        @NonNull RoundWeightConversionFilterVO filter, @NonNull RoundWeightConversionFetchOptions fetchOptions) {
        Preconditions.checkArgument(ArrayUtils.isNotEmpty(filter.getTaxonGroupIds()), "Require at least one taxonGroupId");
        Preconditions.checkArgument(ArrayUtils.isNotEmpty(filter.getLocationIds()), "Require at least one locationIds");

        // Try to find a conversion factor
        List<RoundWeightConversionVO> matches = findByFilter(filter,
            FIND_FIRST_PAGE,
            fetchOptions
        );
        if (CollectionUtils.isNotEmpty(matches)) return Optional.of(matches.get(0));

        return Optional.empty();
    }

    @Override
    public long countByFilter(RoundWeightConversionFilterVO filter) {
        return roundWeightConversionRepository.count(filter);
    }

    @Override
    public List<RoundWeightConversionVO> saveAll(List<RoundWeightConversionVO> sources) {
        return sources.stream()
            .map(roundWeightConversionRepository::save)
            .collect(Collectors.toList());
    }

    @Override
    public void deleteAllById(List<Integer> ids) {
        roundWeightConversionRepository.deleteAllById(ids);
    }
}

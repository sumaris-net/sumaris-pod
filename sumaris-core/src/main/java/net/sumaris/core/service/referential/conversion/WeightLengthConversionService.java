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

import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.vo.referential.conversion.WeightLengthConversionFetchOptions;
import net.sumaris.core.vo.referential.conversion.WeightLengthConversionFilterVO;
import net.sumaris.core.vo.referential.conversion.WeightLengthConversionVO;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Transactional
public interface WeightLengthConversionService {

    @Transactional(readOnly = true)
    List<WeightLengthConversionVO> findByFilter(WeightLengthConversionFilterVO filter, Page page,
                                                @Nullable WeightLengthConversionFetchOptions fetchOptions);

    @Transactional(readOnly = true)
    long countByFilter(WeightLengthConversionFilterVO filter);

    /**
     * Get the best fit weight-length conversion.
     * Required at least a reference taxon and location.
     * Will try to load using this order
     * <ul>
     *     <li>pmfmId + year + month</li>
     *     <li>pmfmId + year (without month)</li>
     *     <li>pmfmId + month (without year)</li>
     *     <li>TODO: Loop using parameterId (without pmfmId). If found, will convert unit</li>
     * </ul>
     * @param filter
     * @param page
     * @param fetchOptions
     * @return
     */
    @Transactional(readOnly = true)
    Optional<WeightLengthConversionVO> loadFirstByFilter(WeightLengthConversionFilterVO filter);

    @Transactional(readOnly = true)
    Optional<WeightLengthConversionVO> loadFirstByFilter(WeightLengthConversionFilterVO filter, @Nullable WeightLengthConversionFetchOptions fetchOptions);


    List<WeightLengthConversionVO> saveAll(List<WeightLengthConversionVO> source);

    void deleteAllById(List<Integer> ids);

    @Transactional(readOnly = true)
    BigDecimal computedWeight(WeightLengthConversionVO conversion,
                              Number length,
                              String lengthUnit,
                              @Nullable Number lengthPrecision,
                              @Nullable Number individualCount,
                              String weightUnit,
                              int weightScale);

    @Transactional(readOnly = true)
    boolean isWeightLengthParameter(int parameterId);

    @Transactional(readOnly = true)
    boolean isWeightLengthPmfm(int pmfmId);
}

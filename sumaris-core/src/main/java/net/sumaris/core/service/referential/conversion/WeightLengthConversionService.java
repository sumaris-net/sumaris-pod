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

import java.math.BigDecimal;
import java.util.List;

@Transactional
public interface WeightLengthConversionService {

    List<WeightLengthConversionVO> findByFilter(WeightLengthConversionFilterVO filter, Page page, WeightLengthConversionFetchOptions fetchOptions);

    long countByFilter(WeightLengthConversionFilterVO filter);

    List<WeightLengthConversionVO> saveAll(List<WeightLengthConversionVO> source);

    void deleteAllById(List<Integer> ids);

    BigDecimal computedWeight(WeightLengthConversionVO conversion,
                              Number length,
                              int scale,
                              Number individualCount);
}
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

package net.sumaris.core.service.administration.samplingScheme;

import lombok.RequiredArgsConstructor;
import net.sumaris.core.dao.administration.samplingScheme.DenormalizedSamplingStrataRepository;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.vo.administration.programStrategy.ProgramFetchOptions;
import net.sumaris.core.vo.administration.samplingScheme.DenormalizedSamplingStrataFetchOptions;
import net.sumaris.core.vo.administration.samplingScheme.DenormalizedSamplingStrataVO;
import net.sumaris.core.vo.administration.samplingScheme.SamplingStrataFilterVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nullable;
import java.util.List;

@Transactional
@Service
@RequiredArgsConstructor
public class DenormalizedSamplingStrataService {

    private final DenormalizedSamplingStrataRepository denormalizedSamplingStrataRepository;


    @Transactional(readOnly = true)
    public List<DenormalizedSamplingStrataVO> findByFilter(@Nullable SamplingStrataFilterVO filter,
                                                           @Nullable Page page,
                                                           @Nullable DenormalizedSamplingStrataFetchOptions fetchOptions) {
        return denormalizedSamplingStrataRepository.findAll(filter, page, fetchOptions);
    }
}

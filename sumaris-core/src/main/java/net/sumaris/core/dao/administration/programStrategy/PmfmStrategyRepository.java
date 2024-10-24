package net.sumaris.core.dao.administration.programStrategy;

/*-
 * #%L
 * SUMARiS:: Core
 * %%
 * Copyright (C) 2018 SUMARiS Consortium
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

import net.sumaris.core.dao.technical.jpa.SumarisJpaRepository;
import net.sumaris.core.model.administration.programStrategy.PmfmStrategy;
import net.sumaris.core.model.referential.pmfm.Pmfm;
import net.sumaris.core.vo.administration.programStrategy.PmfmStrategyFetchOptions;
import net.sumaris.core.vo.administration.programStrategy.PmfmStrategyVO;
import net.sumaris.core.vo.administration.programStrategy.StrategyFetchOptions;
import net.sumaris.core.vo.filter.PmfmStrategyFilterVO;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public interface PmfmStrategyRepository
    extends SumarisJpaRepository<PmfmStrategy, Integer, PmfmStrategyVO>,
    PmfmStrategySpecifications {

    List<PmfmStrategyVO> findByFilter(PmfmStrategyFilterVO filter, PmfmStrategyFetchOptions fetchOptions);

    List<PmfmStrategyVO> saveByStrategyId(int strategyId, List<PmfmStrategyVO> sources);

    PmfmStrategyVO toVO(PmfmStrategy source, PmfmStrategyFetchOptions fetchOptions);

    PmfmStrategyVO toVO(PmfmStrategy source, Pmfm pmfm, PmfmStrategyFetchOptions fetchOptions);
}

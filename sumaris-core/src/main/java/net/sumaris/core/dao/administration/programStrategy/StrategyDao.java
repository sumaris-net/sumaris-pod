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

import net.sumaris.core.dao.cache.CacheNames;
import net.sumaris.core.model.administration.programStrategy.PmfmStrategy;
import net.sumaris.core.vo.administration.programStrategy.*;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;

import java.util.List;

public interface StrategyDao {

    @Cacheable(cacheNames = CacheNames.STRATEGIES_BY_PROGRAM_ID, key = "#programId * #fetchOptions.hashCode()")
    List<StrategyVO> findByProgram(int programId, StrategyFetchOptions fetchOptions);

    List<ReferentialVO> getGears(int strategyId);

    List<TaxonGroupStrategyVO> getTaxonGroupStrategies(int strategyId);

    List<TaxonNameStrategyVO> getTaxonNameStrategies(int strategyId);

    @Caching(
            evict = {
                    @CacheEvict(cacheNames = CacheNames.STRATEGIES_BY_PROGRAM_ID, allEntries = true)
            }
    )
    List<StrategyVO> saveByProgramId(int programId, List<StrategyVO> sources);

    @Caching(
            evict = {
                    @CacheEvict(cacheNames = CacheNames.STRATEGIES_BY_PROGRAM_ID, allEntries = true)
            }
    )
    StrategyVO save(StrategyVO source);

    @Caching(
            evict = {
                    @CacheEvict(cacheNames = CacheNames.STRATEGIES_BY_PROGRAM_ID, allEntries = true),
                    @CacheEvict(cacheNames = CacheNames.PMFM_BY_STRATEGY_ID, key = "#id")
            }
    )
    void delete(int id);

}

package net.sumaris.core.dao.administration.programStrategy;

/*-
 * #%L
 * SUMARiS:: Core
 * %%
 * Copyright (C) 2018 - 2020 SUMARiS Consortium
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

import net.sumaris.core.config.CacheConfiguration;
import net.sumaris.core.dao.referential.ReferentialRepository;
import net.sumaris.core.model.administration.programStrategy.Strategy;
import net.sumaris.core.vo.administration.programStrategy.StrategyFetchOptions;
import net.sumaris.core.vo.administration.programStrategy.StrategyVO;
import net.sumaris.core.vo.filter.StrategyFilterVO;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * @author peck7 on 24/08/2020.
 */
public interface StrategyRepository
    extends ReferentialRepository<Integer, Strategy, StrategyVO, StrategyFilterVO, StrategyFetchOptions>,
    StrategySpecifications {

    @Caching(
        evict = {
            @CacheEvict(cacheNames = CacheConfiguration.Names.STRATEGY_BY_ID, key = "#id", condition = "#id != null"),
            @CacheEvict(cacheNames = CacheConfiguration.Names.STRATEGY_BY_LABEL, allEntries = true),
            @CacheEvict(cacheNames = CacheConfiguration.Names.STRATEGIES_BY_FILTER, allEntries = true),
            @CacheEvict(cacheNames = CacheConfiguration.Names.PMFM_STRATEGIES_BY_FILTER, allEntries = true),
            @CacheEvict(cacheNames = CacheConfiguration.Names.DENORMALIZED_PMFM_BY_FILTER, allEntries = true)
        }
    )
    @Modifying
    @Query("delete from Strategy s where s.id = :id")
    void deleteById(@Param("id") int id);

}

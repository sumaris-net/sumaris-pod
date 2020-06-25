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
import net.sumaris.core.model.administration.programStrategy.AcquisitionLevel;
import net.sumaris.core.model.administration.programStrategy.PmfmStrategy;
import net.sumaris.core.model.administration.programStrategy.Program;
import net.sumaris.core.model.administration.programStrategy.Strategy;
import net.sumaris.core.model.data.Trip;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.administration.programStrategy.PmfmStrategyVO;
import net.sumaris.core.vo.administration.programStrategy.StrategyFetchOptions;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.jpa.domain.Specification;

import javax.annotation.Nonnull;
import java.util.List;

public interface PmfmStrategyExtendRepository {

    default Specification<Trip> hasProgramLabel(String programLabel) {
        if (StringUtils.isBlank(programLabel)) return null;
        return (root, query, cb) -> cb.equal(root
                .get(PmfmStrategy.Fields.STRATEGY)
                .get(Strategy.Fields.PROGRAM)
                .get(Program.Fields.LABEL), programLabel);
    }

    default Specification<Trip> hasAcquisitionLevelLabel(String acquisitionLevelLabel) {
        if (StringUtils.isBlank(acquisitionLevelLabel)) return null;
        return (root, query, cb) -> cb.equal(root
                .get(PmfmStrategy.Fields.ACQUISITION_LEVEL)
                .get(AcquisitionLevel.Fields.LABEL), acquisitionLevelLabel);
    }

    default Specification<Trip> hasStrategy(int strategyId) {
        return (root, query, cb) -> cb.equal(root.get(PmfmStrategy.Fields.STRATEGY).get(Strategy.Fields.ID), strategyId);
    }

    @Cacheable(cacheNames = CacheNames.PMFM_BY_STRATEGY_ID)
    List<PmfmStrategyVO> findByStrategyId(int strategyId, boolean enablePmfmInheritance);

    List<PmfmStrategyVO> findByProgramAndAcquisitionLevel(int programId, int acquisitionLevelId, boolean enablePmfmInheritance);

    @Caching(
            evict = {
                    @CacheEvict(cacheNames = CacheNames.PMFM_BY_STRATEGY_ID, allEntries = true) // FIXME fix error 'null' when using key='#strategyId'
            }
    )
    List<PmfmStrategyVO> saveByStrategyId(int strategyId, @Nonnull List<PmfmStrategyVO> sources);

    PmfmStrategyVO toVO(PmfmStrategy source, boolean enablePmfmInheritance);

    PmfmStrategyVO toVO(PmfmStrategy source, StrategyFetchOptions fetchOptions);

}

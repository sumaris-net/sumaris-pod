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

package net.sumaris.core.dao.referential.pmfm;

import net.sumaris.core.dao.cache.CacheNames;
import net.sumaris.core.model.referential.pmfm.Pmfm;
import net.sumaris.core.vo.referential.PmfmVO;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;

import java.util.Optional;

public interface PmfmDao {

    Optional<PmfmVO> findByLabel(String label);

    PmfmVO getByLabel(String label);

    @Cacheable(cacheNames = CacheNames.PMFM_BY_ID, unless = "#result == null")
    PmfmVO get(int id);

    PmfmVO toVO(Pmfm source);

    @Caching(
            evict = {
                    @CacheEvict(cacheNames = CacheNames.PMFM_BY_ID, key = "#source.id", condition = "#source != null && #source.id != null"),
                    @CacheEvict(cacheNames = CacheNames.PMFM_HAS_PREFIX, allEntries = true),
                    @CacheEvict(cacheNames = CacheNames.PMFM_HAS_SUFFIX, allEntries = true),
                    @CacheEvict(cacheNames = CacheNames.PMFM_HAS_MATRIX, allEntries = true)
            }
    )
    PmfmVO save(PmfmVO source);

    @Cacheable(cacheNames = CacheNames.PMFM_HAS_PREFIX)
    boolean hasLabelPrefix(int pmfmId, String... labelPrefixes);

    @Cacheable(cacheNames = CacheNames.PMFM_HAS_SUFFIX)
    boolean hasLabelSuffix(int pmfmId, String... labelSuffixes);

    @Cacheable(cacheNames = CacheNames.PMFM_HAS_MATRIX)
    boolean hasMatrixId(int pmfmId, int... matrixIds);
}

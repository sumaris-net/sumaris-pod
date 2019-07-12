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
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.model.administration.programStrategy.Program;
import net.sumaris.core.vo.administration.programStrategy.ProgramFetchOptions;
import net.sumaris.core.vo.administration.programStrategy.ProgramVO;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.filter.ProgramFilterVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import net.sumaris.core.vo.referential.TaxonGroupVO;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ProgramDao {

    List<ProgramVO> getAll();

    List<ProgramVO> findByFilter(ProgramFilterVO filter, int offset, int size, String sortAttribute, SortDirection sortDirection);

    @Cacheable(cacheNames = CacheNames.PROGRAM_BY_ID)
    ProgramVO get(int id);

    @Cacheable(cacheNames = CacheNames.PROGRAM_BY_LABEL)
    ProgramVO getByLabel(String label);

    ProgramVO toProgramVO(Program source);

    ProgramVO toProgramVO(Program source, ProgramFetchOptions fetchOptions);

    @Caching(
            evict = {
                    @CacheEvict(cacheNames = CacheNames.PROGRAM_BY_ID, key = "#source.id", condition = "#source.id != null"),
                    @CacheEvict(cacheNames = CacheNames.PROGRAM_BY_LABEL, key = "#source.label", condition = "#source.id != null"),
            },
            put = {
                    @CachePut(cacheNames= CacheNames.PROGRAM_BY_ID, key="#source.id", condition = " #source.id != null"),
                    @CachePut(cacheNames= CacheNames.PROGRAM_BY_LABEL, key="#source.label", condition = "#source.id != null")
            }
    )
    ProgramVO save(ProgramVO source);

    @Caching(
            evict = {
                    @CacheEvict(cacheNames = CacheNames.PROGRAM_BY_LABEL, key = "#id", condition = "#source.id != null"),
                    @CacheEvict(cacheNames = CacheNames.PROGRAM_BY_ID, allEntries = true)
            }
    )
    void delete(int id);

    List<TaxonGroupVO> getTaxonGroups(int programId);

    List<ReferentialVO> getGears(int programId);
}

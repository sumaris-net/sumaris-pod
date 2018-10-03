package net.sumaris.core.dao.administration.user;

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
import net.sumaris.core.model.administration.user.Department;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.filter.DepartmentFilterVO;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;

import java.util.List;

public interface DepartmentDao {

    List<DepartmentVO> findByFilter(DepartmentFilterVO filter,
                                    int offset,
                                    int size,
                                    String sortAttribute,
                                    SortDirection sortDirection);

    @Cacheable(cacheNames = CacheNames.DEPARTMENT_BY_ID, key = "#id", unless="#result==null")
    DepartmentVO get(int id);

    @Cacheable(cacheNames = CacheNames.DEPARTMENT_BY_LABEL, key = "#label", unless="#result==null")
    Department getByLabelOrNull(String label);

    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.DEPARTMENT_BY_ID, key = "#id"),
            @CacheEvict(cacheNames = CacheNames.DEPARTMENT_BY_LABEL, allEntries = true)
    })
    void delete(int id);

    @Caching(put = {
            @CachePut(cacheNames= CacheNames.DEPARTMENT_BY_ID, key="#source.id", condition = "#source != null && #source.id != null"),
            @CachePut(cacheNames= CacheNames.DEPARTMENT_BY_LABEL, key="#source.label", condition = "#source != null && #source.id != null && #source.label != null")
    })
    DepartmentVO save(DepartmentVO source);

    DepartmentVO toDepartmentVO(Department department);
}

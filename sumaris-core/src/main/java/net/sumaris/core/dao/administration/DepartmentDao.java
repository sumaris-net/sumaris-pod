package net.sumaris.core.dao.administration;

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

import java.util.List;

public interface DepartmentDao {

    List<DepartmentVO> findByFilter(DepartmentFilterVO filter,
                                    int offset,
                                    int size,
                                    String sortAttribute,
                                    SortDirection sortDirection);

    @Cacheable(cacheNames = CacheNames.DEPARTMENT_BY_ID, key = "#id")
    DepartmentVO get(int id);

    Department getByLabelOrNull(String label);

    @CacheEvict(cacheNames = CacheNames.DEPARTMENT_BY_ID, key = "#id")
    void delete(int id);

    @CachePut(cacheNames= CacheNames.DEPARTMENT_BY_ID, key="#department.id", condition = "#department.id != null")
    DepartmentVO save(DepartmentVO department);

    DepartmentVO toDepartmentVO(Department department);
}

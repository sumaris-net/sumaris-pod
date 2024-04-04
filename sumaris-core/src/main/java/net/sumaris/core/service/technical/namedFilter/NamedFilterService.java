package net.sumaris.core.service.technical.namedFilter;

/*-
 * #%L
 * SUMARiS:: Core
 * %%
 * Copyright (C) 2018 - 2024 SUMARiS Consortium
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

import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.vo.technical.namedFilter.NamedFilterFetchOptions;
import net.sumaris.core.vo.technical.namedFilter.NamedFilterFilterVO;
import net.sumaris.core.vo.technical.namedFilter.NamedFilterSaveOptions;
import net.sumaris.core.vo.technical.namedFilter.NamedFilterVO;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Transactional
public interface NamedFilterService {

    @Transactional(readOnly = true)
    Optional<NamedFilterVO> findById(int id, NamedFilterFetchOptions fetchOptions);

    @Transactional(readOnly = true)
    List<NamedFilterVO> findAll(NamedFilterFilterVO filter,
                                int offset,
                                int size,
                                String sortAttribute,
                                SortDirection sortDirection,
                                NamedFilterFetchOptions fetchOptions);

    @Transactional(readOnly = true)
    long countByFilter(NamedFilterFilterVO filter);

    NamedFilterVO save(NamedFilterVO source, NamedFilterSaveOptions options);

    List<NamedFilterVO> saveAll(List<NamedFilterVO> sources, NamedFilterSaveOptions options);

    void delete(int id);

    void deleteAll(List<Integer> ids);

}

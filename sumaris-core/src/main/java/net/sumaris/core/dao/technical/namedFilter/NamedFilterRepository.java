package net.sumaris.core.dao.technical.namedFilter;

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
import net.sumaris.core.dao.technical.jpa.SumarisJpaRepository;
import net.sumaris.core.model.technical.namedFilter.NamedFilter;
import net.sumaris.core.vo.technical.namedFilter.NamedFilterFetchOptions;
import net.sumaris.core.vo.technical.namedFilter.NamedFilterFilterVO;
import net.sumaris.core.vo.technical.namedFilter.NamedFilterVO;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NamedFilterRepository
        extends SumarisJpaRepository<NamedFilter, Integer, NamedFilterVO>,
            NamedFilterSpecifications {

    Optional<NamedFilterVO> findById(int id, NamedFilterFetchOptions fetchOptions);

    List<NamedFilterVO> findAll(
            NamedFilterFilterVO filter,
            int offset,
            int size,
            String sortAttribute,
            SortDirection sortDirection,
            NamedFilterFetchOptions fetchOptions
            );

    long count(NamedFilterFilterVO filter);
}

package net.sumaris.core.dao.technical;

/*-
 * #%L
 * SUMARiS:: Core shared
 * %%
 * Copyright (C) 2018 - 2019 SUMARiS Consortium
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

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Optional;

/**
 * Helper class
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>*
 */
public class Pageables  {

    protected Pageables() {

    }

    public static Pageable create(long offset, int size) {
        return create(offset, size, (Sort.Direction)null, null);
    }

    /**
     * @deprecated
     * @param offset
     * @param size
     * @param sortAttribute
     * @param sortDirection
     * @return
     */
    @Deprecated
    public static Pageable create(long offset, int size, String sortAttribute, SortDirection sortDirection) {
        return create(offset, size,
                sortDirection,
                sortAttribute);
    }

    public static Pageable create(Long offset, Integer size, SortDirection sortDirection, String... sortAttributes) {
        Sort.Direction direction = Optional.ofNullable(sortDirection)
                .map(SortDirection::name)
                .map(Sort.Direction::fromString)
                .orElse(Sort.Direction.ASC);
        return create(offset, size, direction, sortAttributes);
    }

    public static Pageable create(Long offset, Integer size, Sort.Direction sortDirection, String... sortAttributes) {
        if (offset == null || size == null) {
            return Pageable.unpaged();
        }
        int page = (int)((offset - offset % size) / size);
        if (sortDirection != null && sortAttributes != null) {
            return PageRequest.of(page, size, sortDirection, sortAttributes);
        }
        return PageRequest.of(page, size);
    }
}


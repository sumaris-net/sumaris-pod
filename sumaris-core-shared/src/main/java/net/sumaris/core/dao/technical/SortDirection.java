package net.sumaris.core.dao.technical;

/*-
 * #%L
 * SUMARiS:: Core shared
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

import org.springframework.data.domain.Sort;

import java.util.Optional;

public enum SortDirection {
    ASC,
    DESC;

    public static SortDirection fromString(String direction) {
        return direction != null ? SortDirection.valueOf(direction.toUpperCase()) : null;
    }

    public static SortDirection fromString(String direction, SortDirection defaultValue) {
        return direction != null ? SortDirection.valueOf(direction.toUpperCase()) : defaultValue;
    }

    public static Optional<SortDirection> fromSort(Sort sort) {
        return sort == null ? Optional.empty() : sort.stream().findFirst()
                .map(o -> o.isAscending() ? ASC : DESC);
    }

    public static Sort.Direction toJpaDirection(SortDirection direction)  {
        return SortDirection.DESC.equals(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;
    }
}

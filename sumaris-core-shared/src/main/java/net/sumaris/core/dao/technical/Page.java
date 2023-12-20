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

import lombok.*;
import org.springframework.data.domain.Pageable;

import java.io.Serializable;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>*
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class Page implements Serializable {

    public static Page create(long offset, int size, String sortAttribute, SortDirection sortDirection) {
        return Page.builder()
            .offset(offset)
            .size(size)
            .sortBy(sortAttribute)
            .sortDirection(sortDirection)
            .build();
    }
    @Builder.Default
    private long offset = 0L;

    @Builder.Default
    private int size = 100;

    @Builder.Default
    private String sortBy = "id";

    @Builder.Default
    private SortDirection sortDirection = SortDirection.ASC;

    public Pageable asPageable() {
        return Pageables.create(offset, size, sortBy, sortDirection);
    }
}


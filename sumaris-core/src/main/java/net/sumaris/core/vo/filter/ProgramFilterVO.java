package net.sumaris.core.vo.filter;

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

import lombok.*;
import lombok.experimental.FieldNameConstants;

@Data
@EqualsAndHashCode(callSuper = true)
@FieldNameConstants
@NoArgsConstructor
@AllArgsConstructor
public class ProgramFilterVO extends ReferentialFilterVO {

    private String withProperty;

    @Builder(builderMethodName = "programFilterBuilder")
    public ProgramFilterVO(String label, String name,
                           Integer[] statusIds, Integer levelId, Integer[] levelIds,
                           String searchJoin, String searchText, String searchAttribute,
                           String withProperty) {
        super(label, name, statusIds, levelId, levelIds, searchJoin, searchText, searchAttribute);
        this.withProperty = withProperty;
    }
}

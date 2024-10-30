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

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;

@Data
@FieldNameConstants
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ReferentialFilterVO implements IReferentialFilter {

    public static ReferentialFilterVO nullToEmpty(ReferentialFilterVO filter) {
        return filter == null ? new ReferentialFilterVO() : filter;
    }


    @Deprecated // used includedIds
    private Integer id;

    private String label;
    private String name;

    private Integer[] statusIds;
    private Integer[] levelIds;
    private String[] levelLabels;

    private String searchJoin;
    private Integer[] searchJoinLevelIds;
    private String searchText;
    private String searchAttribute;

    private Integer[] includedIds;
    private Integer[] excludedIds;

    private Integer[] locationIds;

    @Deprecated
    private Integer levelId;

    @Deprecated
    private String levelLabel;

}

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
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

@Data
@FieldNameConstants
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StrategyFilterVO implements IReferentialFilter {

    /* -- Inherited from IReferentialFilter -- */
    private Integer id;
    private String label;
    private String name;

    private Integer[] statusIds;

    private String searchJoin;
    private String searchText;
    private String searchAttribute;

    private String withProperty;

    private Integer[] excludedIds;

    /* -- Specific properties -- */

    private Integer programId;
    private Integer[] programIds;

    // TODO BLA renommer en programLabel ?
    private String levelLabel;
    private String[] levelLabels;

    /* -- Synonym properties (need by IReferentialFilter) -- */

    public Integer getLevelId() {
        return programId;
    }

    public void setLevelId(Integer levelId) {
        this.programId = levelId;
    }

    public Integer[] getLevelIds() {
        return programIds;
    }

    public void setLevelIds(Integer[] levelIds) {
        this.programIds = levelIds;
    }
}

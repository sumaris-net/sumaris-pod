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
import org.apache.commons.lang3.ArrayUtils;

import java.util.Date;

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
    private Integer[] searchJoinLevelIds;
    private String searchText;
    private String searchAttribute;

    private String withProperty;

    private Integer[] includedIds;
    private Integer[] excludedIds;

    /* -- Specific properties -- */

    private Integer[] programIds;
    private String[] programLabels;

    private Date startDate;
    private Date endDate;

    private String[] analyticReferences;
    private Integer[] referenceTaxonIds;
    private Integer[] departmentIds;
    private Integer[] locationIds;
    private Integer[] parameterIds;
    private PeriodVO[] periods;

    /* -- Synonym properties (need by IReferentialFilter) -- */

    public Integer[] getLevelIds() {
        return programIds;
    }

    public void setLevelIds(Integer[] levelIds) {
        this.programIds = levelIds;
    }

    public String[] getLevelLabels() {
        return programLabels;
    }

    public void setLevelLabels(String[] levelLabels) {
        this.programLabels = levelLabels;
    }

    @Deprecated
    public Integer getLevelId() {
        if (ArrayUtils.getLength(programIds) == 1) return programIds[0];
        return null;
    }

    @Deprecated
    public void setLevelId(Integer levelId) {
        if (ArrayUtils.isEmpty(this.programIds)) {
            this.programIds = new Integer[]{levelId};
        }
        else {
            this.programIds = ArrayUtils.add(this.programIds, levelId);
        }
    }

    @Deprecated
    public String getLevelLabel() {
        if (ArrayUtils.getLength(programLabels) == 1) return programLabels[0];
        return null;
    }

    @Deprecated
    public void setLevelLabel(String levelLabel) {
        if (ArrayUtils.isEmpty(this.programLabels)) {
            this.programLabels = new String[]{levelLabel};
        }
        else {
            this.programLabels = ArrayUtils.add(this.programLabels, levelLabel);
        }
    }
}

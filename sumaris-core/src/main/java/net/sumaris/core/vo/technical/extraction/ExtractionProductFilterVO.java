/*
 * #%L
 * SUMARiS
 * %%
 * Copyright (C) 2019 SUMARiS Consortium
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

package net.sumaris.core.vo.technical.extraction;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.vo.filter.IReferentialFilter;

@Data
@FieldNameConstants
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtractionProductFilterVO implements IReferentialFilter {

    private Integer id;
    private String label;
    private String name;

    private Integer[] statusIds;

    private Integer levelId;
    private Integer[] levelIds;
    private String levelLabel;
    private String[] levelLabels;

    private String searchJoin;
    private String searchText;
    private String searchAttribute;

    private Integer recorderDepartmentId;
    private Integer recorderPersonId;

    private Integer[] includedIds;
    private Integer[] excludedIds;

    public String getCategory() {
        return levelLabel;
    }

    public void setCategory(String category) {
        levelLabel = category;
    }
}

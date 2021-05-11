package net.sumaris.core.extraction.vo;

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

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants
public class ExtractionFilterVO {

    public static ExtractionFilterVO nullToEmpty(ExtractionFilterVO filter) {
        return filter != null ? filter : new ExtractionFilterVO();
    }

    private String operator;

    private List<ExtractionFilterCriterionVO> criteria;

    private String sheetName;

    private Boolean preview;

    private Boolean distinct;

    private Set<String> includeColumnNames;

    private Set<String> excludeColumnNames;

    public boolean isDistinct() {
        return distinct != null ? distinct.booleanValue() : false;
    }

    public boolean isPreview() {
        return preview != null ? preview.booleanValue() : false;
    }

}

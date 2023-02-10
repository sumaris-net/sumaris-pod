package net.sumaris.extraction.core.vo;

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

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import lombok.experimental.FieldNameConstants;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants
@EqualsAndHashCode
public class ExtractionFilterVO implements Serializable {

    public static ExtractionFilterVO nullToEmpty(ExtractionFilterVO filter) {
        return filter != null ? filter : new ExtractionFilterVO();
    }

    private String operator;

    private List<ExtractionFilterCriterionVO> criteria;

    private String sheetName;

    @Builder.Default
    private Boolean preview = false;

    private Boolean distinct;

    private Set<String> includeColumnNames;

    private Set<String> excludeColumnNames;

    private Map<String, Object> meta;

    @JsonIgnore
    public boolean isDistinct() {
        return distinct != null && distinct;
    }

    @JsonIgnore
    public boolean isPreview() {
        return preview != null && preview;
    }

    @JsonIgnore
    public boolean isEmpty() {
        return sheetName == null
            && distinct == null
            && CollectionUtils.isEmpty(criteria)
            && CollectionUtils.isEmpty(includeColumnNames)
            && CollectionUtils.isEmpty(excludeColumnNames)
            && MapUtils.isEmpty(meta);
    }

}

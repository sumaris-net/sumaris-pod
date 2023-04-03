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

import lombok.*;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.model.technical.extraction.IExtractionType;
import net.sumaris.core.vo.filter.IReferentialFilter;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Pattern;

@Data
@FieldNameConstants
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtractionTypeFilterVO implements IReferentialFilter {

    public static ExtractionTypeFilterVO nullToEmpty(ExtractionTypeFilterVO filter) {
        return filter != null ? filter : ExtractionTypeFilterVO.builder().build();
    }

    public static Predicate<IExtractionType> toPredicate(@NonNull ExtractionTypeFilterVO filter) {

        Pattern searchPattern = net.sumaris.core.dao.technical.Daos.searchTextIgnoreCasePattern(filter.getSearchText(), false);
        Pattern searchAnyPattern = net.sumaris.core.dao.technical.Daos.searchTextIgnoreCasePattern(filter.getSearchText(), true);

        return s -> (filter.getId() == null || filter.getId().equals(s.getId()))
            && (filter.getLabel() == null || filter.getLabel().equalsIgnoreCase(s.getLabel()))
            && (filter.getName() == null || filter.getName().equalsIgnoreCase(s.getName()))
            && (filter.getFormat() == null || filter.getFormat().equalsIgnoreCase(s.getFormat()))
            && (filter.getFormats() == null || ArrayUtils.contains(filter.getFormats(), s.getLabel()))
            && (filter.getVersion() == null || filter.getVersion().equalsIgnoreCase(s.getVersion()))
            && (filter.getIsSpatial() == null || Objects.equals(filter.getIsSpatial(), s.getIsSpatial()))
            && (filter.getStatusIds() == null || Arrays.asList(filter.getStatusIds()).contains(s.getStatusId()))
            && (searchPattern == null || searchAnyPattern == null
            || searchPattern.matcher(s.getLabel()).matches()
            || searchAnyPattern.matcher(s.getName()).matches());
    }

    private String format;
    private String[] formats;
    private String version;
    private Integer parentId;

    private Integer id;
    private String label;
    private String name;

    private Integer[] statusIds;

    private Integer levelId;
    private Integer[] levelIds;
    private String levelLabel;
    private String[] levelLabels;

    private String searchJoin;
    private Integer[] searchJoinLevelIds;
    private String searchText;
    private String searchAttribute;

    private Integer recorderDepartmentId;
    private Integer recorderPersonId;

    private Integer[] includedIds;
    private Integer[] excludedIds;


    private Boolean isSpatial;

    public String getCategory() {
        return levelLabel;
    }

    public void setCategory(String category) {
        levelLabel = category;
    }

}

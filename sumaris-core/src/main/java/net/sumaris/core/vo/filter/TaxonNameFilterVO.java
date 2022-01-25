package net.sumaris.core.vo.filter;

/*-
 * #%L
 * SUMARiS:: Core
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
import lombok.experimental.FieldNameConstants;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>*
 */
@Data
@FieldNameConstants
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaxonNameFilterVO implements IReferentialFilter {

    public static TaxonNameFilterVO nullToEmpty(TaxonNameFilterVO filter) {
        return filter == null ? new TaxonNameFilterVO() : filter;
    }

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

    private Integer taxonGroupId;
    private Integer[] taxonGroupIds;
    private Boolean withSynonyms;
    private Integer referenceTaxonId;
    private String taxonomicLevelLabel;

    private Integer[] includedIds;
    private Integer[] excludedIds;

    @Deprecated
    private Integer levelId;
    @Deprecated
    private String levelLabel;
}

package net.sumaris.core.vo.technical.extraction;

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
import net.sumaris.core.vo.referential.IReferentialVO;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>*
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants
public class ExtractionTableVO implements IReferentialVO<Integer> {

    private Integer id;
    private String label;
    private String name;
    private String description;
    private String comments;
    private Date updateDate;
    private Date creationDate;
    private Integer statusId;
    private Boolean isSpatial;
    private Integer rankOrder;

    @ToString.Exclude
    private ExtractionProductVO product;
    private Integer productId;

    private String tableName;

    private String defaultSpaceColumn;
    private String defaultAggColumn;
    private String defaultTechColumn;

    private List<ExtractionTableColumnVO> columns;
    private Map<String, List<Object>> columnValues;

    public String getSheetName() {
        return label;
    }
}

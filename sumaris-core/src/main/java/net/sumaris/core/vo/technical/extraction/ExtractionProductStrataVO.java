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

import lombok.AccessLevel;
import lombok.Data;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.vo.referential.IReferentialVO;

import java.util.Date;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>*
 */
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@FieldNameConstants
public class ExtractionProductStrataVO implements IReferentialVO {

    Integer id;
    String label;
    String name;
    String description;
    String comments;
    Date updateDate;
    Date creationDate;
    Integer statusId;

    Boolean isDefault;

    @ToString.Exclude
    ExtractionProductVO product;
    Integer productId;

    String sheetName;

    String timeColumnName;
    String spaceColumnName;
    String aggColumnName;
    String techColumnName;

    String aggFunction;
}

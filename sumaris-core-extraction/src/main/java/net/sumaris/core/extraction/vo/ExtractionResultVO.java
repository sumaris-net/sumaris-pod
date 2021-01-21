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

import com.google.common.collect.ImmutableList;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import net.sumaris.core.vo.technical.extraction.ExtractionTableColumnVO;

import java.util.List;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ExtractionResultVO {

    ExtractionTypeVO type;
    List<ExtractionTableColumnVO> columns;
    List<String[]> rows;
    Number total;

    public ExtractionResultVO() {
    }

    public ExtractionResultVO(ExtractionResultVO result) {
        type = result.type;
        columns = ImmutableList.copyOf(result.columns);
        rows = result.rows;
        total = result.total;
    }

}

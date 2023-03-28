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

import com.google.common.base.Joiner;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ExtractionFilterCriterionVO {

    private String sheetName;
    private String name;
    private String operator;
    private String value;
    private String[] values;

    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (this.getSheetName() != null) sb.append("Sheet: ").append(this.getSheetName());
        if (this.getName() != null && this.getOperator() != null){
            sb.append(", ").append(this.getName())
                .append(' ')
                .append(this.getOperator())
                .append(' ');
        }
        else {
            sb.append(", ").append("Value: ");
        }
        if (this.getValue() != null) sb.append(this.getValue());
        if (this.getValues() != null) sb.append('(').append(Joiner.on(',').join(this.getValues())).append(')');
        return sb.toString();
    }
}

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

package net.sumaris.extraction.core.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MinMaxVO {

    public static final MinMaxVO ZERO = new MinMaxVO(0d, 0d, 0d, 0d);

    private Double aggMin;
    private Double aggMax;
    private Double techMin;
    private Double techMax;

    public Double getMin()  {
        return aggMin;
    }

    public void setMin(Double min)  {
        aggMin = min;
    }

    public Double getMax() {
        return aggMax;
    }

    public void setMax(Double max) {
        this.aggMax = max;
    }

}

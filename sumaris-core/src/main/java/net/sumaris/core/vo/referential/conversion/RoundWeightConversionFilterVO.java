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

package net.sumaris.core.vo.referential.conversion;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.sumaris.core.util.Beans;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoundWeightConversionFilterVO {

    public static RoundWeightConversionFilterVO nullToEmpty(RoundWeightConversionFilterVO filter) {
        return filter == null ? new RoundWeightConversionFilterVO() : filter;
    }

    Integer[] statusIds;
    Integer[] taxonGroupIds;
    Integer[] locationIds;
    Integer[] dressingIds;
    Integer[] preservingIds;
    Date date;

    public RoundWeightConversionFilterVO clone() {
        return Beans.clone(this, RoundWeightConversionFilterVO.class);
    }
}

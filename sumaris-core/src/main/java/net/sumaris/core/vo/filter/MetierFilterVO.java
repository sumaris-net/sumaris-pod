package net.sumaris.core.vo.filter;

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
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

import java.util.Date;

@Data
@FieldNameConstants
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class MetierFilterVO extends ReferentialFilterVO {

    public static MetierFilterVO nullToEmpty(MetierFilterVO filter) {
        return filter == null ? new MetierFilterVO() : filter;
    }

    public static MetierFilterVO nullToEmpty(IReferentialFilter filter) {
        return filter == null ? new MetierFilterVO() : (MetierFilterVO)filter;
    }

    // options used for predocumentation
    private Date startDate;
    private Date endDate;
    private Integer vesselId;
    private String programLabel;
    private Integer excludedTripId; // optional
    private Integer[] gearIds;
    private Integer[] taxonGroupTypeIds; // optional
}

package net.sumaris.core.vo.data.aggregatedLanding;

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

import lombok.Data;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.vo.data.IWithMeasurementValues;
import net.sumaris.core.vo.referential.ReferentialVO;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Data
@FieldNameConstants
public class VesselActivityVO implements IWithMeasurementValues, Serializable {

    private Date date;
    private Integer rankOrder;
    private String comments;

    private Map<Integer, String> measurementValues;
    private List<ReferentialVO> metiers;

    private Integer recorderPersonId;

    // parent link
    private Integer observedLocationId;
    private Integer landingId;
    private Integer tripId;

    public VesselActivityVO() {
        metiers = new ArrayList<>();
    }

}

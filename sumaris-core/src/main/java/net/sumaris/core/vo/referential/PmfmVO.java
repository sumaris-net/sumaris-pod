package net.sumaris.core.vo.referential;

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

import lombok.*;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.model.referential.pmfm.Pmfm;

import java.util.List;

@Data
@FieldNameConstants
@EqualsAndHashCode(callSuper = true)
public class PmfmVO extends ReferentialVO {

    private String completeName; // Computed field
    private String unitLabel;
    private String type;

    private Double minValue;
    private Double maxValue;
    private Integer maximumNumberDecimals;
    private Integer signifFiguresNumber;
    private Double detectionThreshold;
    private Double precision;
    private Double defaultValue;

    private Boolean isEstimated; // from the method
    private Boolean isCalculated; // from the method

    // Link to other entities
    private Integer parameterId;
    private Integer matrixId;
    private Integer fractionId;
    private Integer methodId;
    private Integer unitId;

    List<ReferentialVO> qualitativeValues;

    public PmfmVO() {
        this.setEntityName(Pmfm.class.getSimpleName());
    }
}

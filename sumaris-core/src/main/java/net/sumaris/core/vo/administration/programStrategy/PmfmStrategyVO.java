package net.sumaris.core.vo.administration.programStrategy;

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
import net.sumaris.core.dao.technical.model.IUpdateDateEntity;
import net.sumaris.core.dao.technical.model.IValueObject;
import net.sumaris.core.vo.referential.PmfmVO;
import net.sumaris.core.vo.referential.ReferentialVO;

import java.util.Date;
import java.util.List;

@Data
@FieldNameConstants
public class PmfmStrategyVO implements IUpdateDateEntity<Integer, Date>, IValueObject<Integer> {

    private Integer id;

    private String acquisitionLevel;
    private Integer rankOrder;
    private Integer acquisitionNumber;
    private Boolean isMandatory;
    private Double minValue;
    private Double maxValue;
    private Double defaultValue;
    private Date updateDate;

    @Deprecated
    private List<String> gears;

    private List<Integer> gearIds;
    private List<Integer> taxonGroupIds;
    private List<Integer> referenceTaxonIds;

    // Link to PMFM, Parameter, Matrix, Fraction, Method
    private Integer pmfmId;
    private PmfmVO pmfm;
    private Integer parameterId;
    private ReferentialVO parameter;
    private Integer matrixId;
    private ReferentialVO matrix;
    private Integer fractionId;
    private ReferentialVO fraction;
    private Integer methodId;
    private ReferentialVO method;

    // Link to parent
    private Integer strategyId;
}


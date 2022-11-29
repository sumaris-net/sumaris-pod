package net.sumaris.core.vo.data;

/*-
 * #%L
 * SUMARiS:: Core
 * %%
 * Copyright (C) 2018 - 2020 SUMARiS Consortium
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
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.model.IEntity;
import net.sumaris.core.model.IValueObject;
import net.sumaris.core.vo.referential.LocationVO;
import net.sumaris.core.vo.referential.ReferentialVO;

import java.util.Date;

/**
 * @author peck7 on 09/06/2020.
 */
@Data
@FieldNameConstants
@EqualsAndHashCode
public class FishingAreaVO implements IEntity<Integer>, IValueObject<Integer> {

    private Integer id;
    private Date qualificationDate;
    private String qualificationComments;
    private Integer qualityFlagId;

    private LocationVO location;

    private ReferentialVO distanceToCoastGradient;
    private ReferentialVO depthGradient;
    private ReferentialVO nearbySpecificArea;

    // parent
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private OperationVO operation;
    private Integer operationId;

}

package net.sumaris.core.vo.data;

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
import lombok.ToString;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.dao.technical.model.IUpdateDateEntity;
import net.sumaris.core.dao.technical.model.IValueObject;
import net.sumaris.core.vo.administration.user.DepartmentVO;

import java.util.Date;

@Data
@FieldNameConstants
public class VesselPositionVO implements IUpdateDateEntity<Integer, Date>, IValueObject<Integer> {

    private Integer id;
    private Date dateTime;
    private Double latitude;
    private Double longitude;

    private Date updateDate;
    private Date controlDate;
    private Date validationDate;
    private Integer qualityFlagId;
    private DepartmentVO recorderDepartment;

    @ToString.Exclude
    private OperationVO operation;
    private Integer operationId;
}

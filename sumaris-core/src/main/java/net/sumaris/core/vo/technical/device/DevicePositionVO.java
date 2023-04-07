package net.sumaris.core.vo.technical.device;

/*-
 * #%L
 * SUMARiS:: Core
 * %%
 * Copyright (C) 2018 - 2023 SUMARiS Consortium
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
import net.sumaris.core.model.referential.ObjectType;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.data.IDataVO;
import net.sumaris.core.vo.referential.ReferentialVO;

import java.util.Date;

@Data
@FieldNameConstants
public class DevicePositionVO implements IDataVO<Integer> {

    private Integer id;
    private Date dateTime;
    private Double latitude;
    private Double longitude;
    private ReferentialVO objectType;
    private Integer objectId;
    private PersonVO recorderPerson;
    private Integer recorderPersonId;
    private DepartmentVO recorderDepartment;
    private Integer recorderDepartmentId;
    private Date updateDate;
    private Date creationDate;
    private Date controlDate;
    private Date validationDate;
    private Integer qualityFlagId;
    private Date qualificationDate;
    private String qualificationComments;

}

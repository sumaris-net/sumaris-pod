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

package net.sumaris.core.vo.data;

import lombok.*;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.model.IValueObject;
import net.sumaris.core.model.administration.programStrategy.Program;
import net.sumaris.core.model.administration.user.Department;
import net.sumaris.core.model.data.*;
import net.sumaris.core.model.referential.QualityFlag;
import net.sumaris.core.model.referential.location.Location;
import net.sumaris.core.vo.administration.programStrategy.ProgramVO;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.referential.LocationVO;
import net.sumaris.core.vo.referential.MetierVO;
import net.sumaris.core.vo.referential.ReferentialVO;

import javax.persistence.*;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Data
@FieldNameConstants
public class VesselUseFeaturesVO implements IValueObject<Integer> {

    private Integer id;
    private Date startDate;
    private Date endDate;

    private Integer isActive; // See enumeration
    private LocationVO basePortLocation;

    private String comments;

    private Date creationDate;
    private Date updateDate;
    private Date controlDate;
    private Integer qualityFlagId;
    private Date qualificationDate;
    private String qualificationComments;
    private DepartmentVO recorderDepartment;
    private PersonVO recorderPerson;

    private Integer vesselId;

    private ProgramVO program;

    private List<MeasurementVO> measurements;
    private Map<Integer, String> measurementValues;

    // TODO create VesselUseFeaturesOriginVO
    //private List<VesselUseFeaturesOriginVO> vesselUseFeaturesOrigins;

    private Integer activityCalendarId;
    private Integer dailyActivityCalendarId;
}
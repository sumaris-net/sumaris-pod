package net.sumaris.core.vo.data;

/*-
 * #%L
 * SUMARiS:: Core
 * %%
 * Copyright (C) 2018 - 2019 SUMARiS Consortium
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
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.vo.administration.programStrategy.ProgramVO;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.referential.ReferentialVO;

import java.util.Date;

/**
 * @author peck7 on 19/11/2019.
 */
@Data
@FieldNameConstants
@EqualsAndHashCode
public class VesselVO implements IRootDataVO<Integer> {

    private Integer id;
    private ReferentialVO vesselType;
    private Integer statusId;
    private String comments;

    private ProgramVO program;

    private Date creationDate;
    private Date updateDate;
    private Date controlDate;
    private Date validationDate;
    private Date qualificationDate;
    private String qualificationComments;
    private Integer qualityFlagId;

    @EqualsAndHashCode.Exclude
    private DepartmentVO recorderDepartment;
    @EqualsAndHashCode.Exclude
    private PersonVO recorderPerson;

    // Features
    private VesselFeaturesVO vesselFeatures;

    // Registration
    private VesselRegistrationPeriodVO vesselRegistrationPeriod;

}

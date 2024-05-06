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

package net.sumaris.core.vo.administration.samplingScheme;

import lombok.Data;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.vo.administration.programStrategy.ProgramVO;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.referential.IReferentialVO;
import net.sumaris.core.vo.referential.ReferentialVO;

import java.util.Date;

@Data
@FieldNameConstants
public class DenormalizedSamplingStrataVO implements IReferentialVO<Integer> {

    private Integer id;
    private String label;
    private String name;
    private Date startDate;
    private Date endDate;
    private String description;
    private String comments;
    private String observationLocationComments;
    private String samplingStrategy;
    private String taxonGroupName;
    private String samplingSchemeLabel;
    private String samplingSchemeName;
    private String samplingSchemeDescription;
    private String gearMeshRange;
    private String vesselLengthRange;
    private String metier;
    private String areaName;
    private String subAreaLocationIds;
    private Date creationDate;
    private Date updateDate;
    
    // Attributes for linked entity ids
    private Integer programId;
    private ProgramVO program;

    private Integer statusId;

    private Integer departmentId;
    private DepartmentVO department;

    private Integer personId;
    private PersonVO person;

    private Integer observationLocationId;
    private ReferentialVO observationLocation;

    private String entityName = "SamplingStrata";
}

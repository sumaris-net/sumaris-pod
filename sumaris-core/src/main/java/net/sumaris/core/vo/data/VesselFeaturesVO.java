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

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.model.data.IWithRecorderPersonEntity;
import net.sumaris.core.vo.administration.programStrategy.ProgramVO;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.referential.LocationVO;
import net.sumaris.core.vo.referential.ReferentialVO;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Data
@FieldNameConstants
@EqualsAndHashCode
public class VesselFeaturesVO implements IRootDataVO<Integer>,
        IWithRecorderPersonEntity<Integer, PersonVO> {

    @EqualsAndHashCode.Exclude
    private Integer id;

    private String name;
    private String exteriorMarking;
    private Integer administrativePower;
    private Integer auxiliaryPower;
    private Double lengthOverAll;
    private Double grossTonnageGrt;
    private Double grossTonnageGt;
    private Integer constructionYear;
    private String ircs;

    private ReferentialVO hullMaterial;
    private LocationVO basePortLocation;
    private String comments;
    private ProgramVO program;

    private Date startDate;
    private Date endDate;

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

    private List<MeasurementVO> measurements;
    private Map<Integer, String> measurementValues;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private VesselVO vessel;

}

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

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.model.ITreeNodeEntity;
import net.sumaris.core.model.IWithFlagsValueObject;
import net.sumaris.core.vo.administration.programStrategy.ProgramVO;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import net.sumaris.core.vo.referential.gear.GearVO;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Data
@ToString(onlyExplicitlyIncluded = true)
@FieldNameConstants
@EqualsAndHashCode
public class PhysicalGearVO implements IRootDataVO<Integer>,
    IWithFlagsValueObject<Integer>,
    ITreeNodeEntity<Integer, PhysicalGearVO>,
    IWithMeasurementValues {

    @EqualsAndHashCode.Exclude
    @ToString.Include
    private Integer id;
    @ToString.Include
    private Integer rankOrder;

    @Deprecated
    private ReferentialVO gear;

    @ToString.Include
    private GearVO fullGear;

    private String comments;
    @EqualsAndHashCode.Exclude
    private Date creationDate;
    @EqualsAndHashCode.Exclude
    private Date updateDate;
    private Date controlDate;
    private Date validationDate;
    private Date qualificationDate;
    private String qualificationComments;
    private Integer qualityFlagId;
    private DepartmentVO recorderDepartment;
    private PersonVO recorderPerson;

    private ProgramVO program;

    private Map<Integer, String> measurementValues;
    private List<MeasurementVO> measurements;

    private Boolean isTowed;
    private Boolean isActive;

    // Parent physical gear
    @EqualsAndHashCode.Exclude
    private PhysicalGearVO parent;
    private Integer parentId;
    private List<PhysicalGearVO> children;

    // Trip
    @EqualsAndHashCode.Exclude
    private TripVO trip;
    private Integer tripId;

    @JsonIgnore
    @EqualsAndHashCode.Exclude
    private int flags = 0;
}

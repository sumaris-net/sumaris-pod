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

package net.sumaris.core.vo.data.activity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.model.data.IWithVesselSnapshotEntity;
import net.sumaris.core.vo.administration.programStrategy.ProgramVO;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.data.*;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Data
@FieldNameConstants
public class ActivityCalendarVO implements IRootDataVO<Integer>,
    IWithVesselSnapshotEntity<Integer, VesselSnapshotVO>,
    IWithMeasurementValues {

    private Integer id;
    private Integer year;
    private Integer[] registrationLocationIds;
    private Integer[] basePortLocationIds;
    private Boolean directSurveyInvestigation;
    private Boolean economicSurvey;
    private String comments;

    private Date creationDate;
    private Date updateDate;
    private Date controlDate;
    private Date validationDate;
    private Integer qualityFlagId;
    private Date qualificationDate;
    private String qualificationComments;
    private DepartmentVO recorderDepartment;
    private PersonVO recorderPerson;

    private Integer vesselId;
    @ToString.Exclude
    private VesselSnapshotVO vesselSnapshot;

    private ProgramVO program;

    private Map<Integer, String> measurementValues;

    @EqualsAndHashCode.Exclude
    private List<VesselUseFeaturesVO> vesselUseFeatures;

    @EqualsAndHashCode.Exclude
    private List<GearUseFeaturesVO> gearUseFeatures;

    @EqualsAndHashCode.Exclude
    private List<GearPhysicalFeaturesVO> gearPhysicalFeatures;

    private List<ImageAttachmentVO> images;

    @Override
    @JsonIgnore
    public Date getVesselDateTime() {
        return null;
    }

    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}

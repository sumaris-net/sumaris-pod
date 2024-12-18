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
import net.sumaris.core.model.data.IWithObserversEntity;
import net.sumaris.core.model.data.IWithVesselSnapshotEntity;
import net.sumaris.core.util.Dates;
import net.sumaris.core.vo.administration.programStrategy.ProgramVO;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.data.*;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
@FieldNameConstants
@EqualsAndHashCode
public class ActivityCalendarVO implements IRootDataVO<Integer>,
    IWithObserversEntity<Integer, PersonVO>,
    IWithVesselSnapshotEntity<Integer, VesselSnapshotVO>,
    IWithMeasurementValues{

    @EqualsAndHashCode.Exclude
    private Integer id;
    private Integer year;
    private Integer[] registrationLocationIds;
    private Integer[] basePortLocationIds;
    private Integer directSurveyInvestigation;
    private Boolean economicSurvey;
    private String comments;

    @EqualsAndHashCode.Exclude
    private Date creationDate;
    @EqualsAndHashCode.Exclude
    private Date updateDate;
    private Date controlDate;
    private Date validationDate;
    private Integer qualityFlagId;
    private Date qualificationDate;
    private String qualificationComments;
    private DepartmentVO recorderDepartment;
    private PersonVO recorderPerson;
    private Set<PersonVO> observers;

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

    @EqualsAndHashCode.Exclude
    private List<ActivityCalendarVesselRegistrationPeriodVO> vesselRegistrationPeriods;

    @Override
    @JsonIgnore
    public Date getVesselDateTime() {
        // Use the first day of the year
        if (this.year != null) return Dates.getFirstDayOfYear(this.year);
        return null;
    }

    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}

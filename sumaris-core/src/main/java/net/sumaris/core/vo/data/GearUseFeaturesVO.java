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

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.vo.administration.programStrategy.ProgramVO;
import net.sumaris.core.vo.referential.MetierVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Data
@FieldNameConstants
@EqualsAndHashCode
public class GearUseFeaturesVO implements IUseFeaturesVO {

    @EqualsAndHashCode.Exclude
    private Integer id;
    private Date startDate;
    private Date endDate;

    private Integer vesselId;
    @ToString.Exclude
    private VesselSnapshotVO vesselSnapshot;

    private ProgramVO program;

    private Short rankOrder;
    private MetierVO metier;
    private ReferentialVO gear;
    private ReferentialVO otherGear;
    private Map<Integer, String> measurementValues;
    private List<FishingAreaVO> fishingAreas;
    private List<DataOriginVO> dataOrigins;

    private String comments;

    @EqualsAndHashCode.Exclude
    private Date creationDate;
    @EqualsAndHashCode.Exclude
    private Date updateDate;
    private Date controlDate;
    private Integer qualityFlagId;
    private Date validationDate;
    private Date qualificationDate;
    private String qualificationComments;
    private Integer recorderDepartmentId;
    private Integer recorderPersonId;

    /* -- link to parent -- */

    private Integer activityCalendarId;
    private Integer dailyActivityCalendarId;

    @JsonIgnore
    @EqualsAndHashCode.Exclude
    private int flags = 0;

    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
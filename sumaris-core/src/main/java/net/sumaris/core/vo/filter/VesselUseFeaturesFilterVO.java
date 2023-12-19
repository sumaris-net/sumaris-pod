package net.sumaris.core.vo.filter;

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

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.model.data.DataQualityStatusEnum;

import javax.annotation.Nullable;
import java.util.Date;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldNameConstants
public class VesselUseFeaturesFilterVO implements IDataFilter, IVesselFilter {

    public static VesselUseFeaturesFilterVO nullToEmpty(@Nullable VesselUseFeaturesFilterVO filter) {
        return filter == null ? new VesselUseFeaturesFilterVO() : filter;
    }

    private Date startDate;
    private Date endDate;
    private Integer isActive;

    private String programLabel;
    private Integer[] programIds;

    private Integer vesselId;
    private Integer locationId;
    private Integer[] locationIds;

    private Integer[] excludedIds;
    private Integer[] includedIds;
    private Integer vesselUseFeaturesId;

    private Integer[] qualityFlagIds;
    private DataQualityStatusEnum[] dataQualityStatus;

    private Integer recorderDepartmentId;
    private Integer recorderPersonId;

    // Parent link
    private Integer activityCalendarId;
    private Integer dailyActivityCalendarId;
}

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

import lombok.*;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.model.data.DataQualityStatusEnum;
import net.sumaris.core.util.Beans;

import java.util.Date;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldNameConstants
@EqualsAndHashCode
public class VesselFilterVO implements IRootDataFilter {

    public static VesselFilterVO nullToEmpty(VesselFilterVO f) {
        return f != null ? f : new VesselFilterVO();
    }

    private String programLabel;
    private Integer[] programIds;

    private Integer vesselId;

    private Integer[] excludedIds;
    private Integer[] includedIds;
    private Integer vesselFeaturesId;

    private Integer vesselTypeId;
    private Integer[] vesselTypeIds;

    private String[] searchAttributes;
    private String searchText;

    private List<Integer> statusIds;

    private Integer recorderDepartmentId; // TODO: use it in repository
    private Integer recorderPersonId; // TODO: use it in repository

    private Date startDate;
    private Date endDate;

    private Integer registrationLocationId;
    private Integer[] registrationLocationIds;

    private Integer basePortLocationId;
    private Integer[] basePortLocationIds;

    @Builder.Default
    private Boolean onlyWithRegistration = false;

    private Integer vesselOwnerId;

    // Quality
    private Integer[] qualityFlagIds; // not used
    private DataQualityStatusEnum[] dataQualityStatus; // not used

    private Date minUpdateDate;

    /**
     * @deprecated use basePortLocationId instead
     * @return basePortLocationId
     */
    @Override
    @Deprecated
    public Integer getLocationId() {
        return basePortLocationId;
    }

    /**
     * @deprecated use basePortLocationId instead
     */
    @Override
    @Deprecated
    public void setLocationId(Integer locationId) {
        basePortLocationId = locationId;
    }


    /**
     * @deprecated use startDate instead
     */
    @Deprecated
    public void setDate(Date date) {
        this.startDate = date;
        this.endDate = date;
    }

    /**
     * @deprecated use startDate instead
     * @return startDate
     */
    @Deprecated
    public Date getDate() {
        return this.startDate;
    }

    public VesselFilterVO clone() {
        VesselFilterVO target = new VesselFilterVO();
        Beans.copyProperties(this, target);
        return target;
    }

    public String toString(String separator) {
        separator = (separator == null) ? ", " : separator;
        StringBuilder sb = new StringBuilder();
        if (this.getProgramLabel() != null) sb.append(separator).append("Program (label): ").append(this.getProgramLabel());
        if (this.getStartDate() != null) sb.append(separator).append("Start date: ").append(this.getStartDate());
        if (this.getEndDate() != null) sb.append(separator).append("End date: ").append(this.getEndDate());
        if (this.getRegistrationLocationId() != null) sb.append(separator).append("Registration location (id): ").append(this.getRegistrationLocationId());
        if (this.getBasePortLocationId() != null) sb.append(separator).append("Base port location (id): ").append(this.getBasePortLocationId());
        if (this.getRecorderPersonId() != null) sb.append(separator).append("Recorder person (id): ").append(this.getRecorderPersonId());
        if (this.getRecorderDepartmentId() != null) sb.append(separator).append("Recorder department (id): ").append(this.getRecorderDepartmentId());
        return sb.toString();
    }
}

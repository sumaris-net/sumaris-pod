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
        return f != null ? f : VesselFilterVO.builder().build();
    }

    private String programLabel;
    private Integer[] programIds;

    private Integer vesselId;
    private Integer vesselFeaturesId;

    private Integer vesselTypeId;

    private String[] searchAttributes;
    private String searchText;

    private List<Integer> statusIds;

    private Integer recorderDepartmentId; // TODO: use it in repository
    private Integer recorderPersonId; // TODO: use it in repository

    private Date startDate;
    private Date endDate;

    private Integer registrationLocationId;
    private Integer basePortLocationId;

    @Builder.Default
    private Boolean onlyWithRegistration = false;

    private DataQualityStatusEnum[] dataQualityStatus; // Not used

    public void setDate(Date date) {
        this.startDate = date;
        this.endDate = date;
    }

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
}

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

import lombok.Data;
import net.sumaris.core.dao.technical.model.IUpdateDateEntityBean;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

@Data
public class OperationVO implements IUpdateDateEntityBean<Integer, Date> {

    public static final String PROPERTY_START_DATE_TIME = "startDateTime";
    public static final String PROPERTY_END_DATE_TIME = "endDateTime";
    public static final String PROPERTY_RANK_ORDER_ON_PERIOD = "rankOrderOnPeriod";
    public static final String PROPERTY_TRIP = "trip";


    private Integer id;
    private Integer rankOrderOnPeriod;
    private Date updateDate;
    private Date controlDate;
    private Date validationDate;
    private Integer qualityFlagId;
    private DepartmentVO recorderDepartment;
    private Date startDateTime;
    private Date endDateTime;
    private Date fishingStartDateTime;
    private Date fishingEndDateTime;
    private Boolean hasCatch;
    private String comments;

    private ReferentialVO metier;

    private TripVO trip;
    private Integer tripId;

    private PhysicalGearVO physicalGear;
    private Integer physicalGearId;

    private List<VesselPositionVO> positions;
    private List<MeasurementVO> measurements;
    private List<MeasurementVO> gearMeasurements;

    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}

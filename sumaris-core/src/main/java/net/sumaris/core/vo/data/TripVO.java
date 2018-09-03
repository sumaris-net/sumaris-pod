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
import net.sumaris.core.vo.referential.LocationVO;
import net.sumaris.core.vo.administration.user.PersonVO;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

@Data
public class TripVO implements IUpdateDateEntityBean<Integer, Date> {

    public static final String PROPERTY_DEPARTURE_DATE_TIME = "departureDateTime";
    public static final String PROPERTY_RETURN_DATE_TIME = "returnDateTime";
    public static final String PROPERTY_DEPARTURE_LOCATION = "departureLocation";
    public static final String PROPERTY_RETURN_LOCATION = "returnLocation";
    public static final String PROPERTY_RECORDER_PERSON = "recorderPerson";
    public static final String PROPERTY_RECORDER_DEPARTMENT = "recorderDepartment";
    public static final String PROPERTY_VESSEL_FEATURES = "vesselFeatures";

    private Integer id;
    private String comments;
    private Date creationDate;
    private Date updateDate;
    private Date controlDate;
    private Date validationDate;
    private Integer qualityFlagId;
    private DepartmentVO recorderDepartment;
    private PersonVO recorderPerson;

    private VesselFeaturesVO vesselFeatures;

    private Date departureDateTime;
    private Date returnDateTime;
    private LocationVO departureLocation;
    private LocationVO returnLocation;

    private List<SaleVO> sales;
    private SaleVO sale;

    private List<OperationVO> operations;
    private List<PhysicalGearVO> gears;
    private List<MeasurementVO> measurements; // vessel_use_measurement


    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

}

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
import net.sumaris.core.model.data.IWithRecorderDepartmentEntityBean;
import net.sumaris.core.model.data.IWithRecorderPersonEntityBean;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Date;

@Data
public class MeasurementVO implements IUpdateDateEntityBean<Integer, Date>,
        IWithRecorderDepartmentEntityBean<Integer, DepartmentVO>,
        IWithRecorderPersonEntityBean<Integer, PersonVO>{

    public static final String PROPERTY_NUMERICAL_VALUE = "numericalValue";
    public static final String PROPERTY_ALPHANUMERICAL_VALUE = "alphanumericalValue";
    public static final String PROPERTY_DIGIT_COUNT = "digitCount";
    public static final String PROPERTY_PRECISION_VALUE = "precisionValue";

    private Integer id;

    private int pmfmId;
    private Double numericalValue;
    private String alphanumericalValue;
    private Integer digitCount;
    private Integer rankOrder;
    private Double precisionValue;
    private ReferentialVO qualitativeValue;

    private Date creationDate;
    private Date updateDate;
    private Date controlDate;
    private Date validationDate;
    private Integer qualityFlagId;
    private DepartmentVO recorderDepartment;
    private PersonVO recorderPerson;

    // Parent entity
    private Integer tripId;
    private Integer physicalGearId;
    private Integer operationId;
    private Integer sampleId;
    private Integer observedLocationId;

    private String entityName;

    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

}

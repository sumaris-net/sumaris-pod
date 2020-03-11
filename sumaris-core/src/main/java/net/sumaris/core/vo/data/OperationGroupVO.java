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
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.dao.technical.model.IUpdateDateEntityBean;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.referential.MetierVO;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Data
@FieldNameConstants
public class OperationGroupVO implements IUpdateDateEntityBean<Integer, Date> {

    private Integer id;
    private Integer rankOrderOnPeriod;
    private Date updateDate;
    private Date controlDate;
    private Date validationDate;
    private Integer qualityFlagId;
    private DepartmentVO recorderDepartment;
    private Boolean hasCatch;
    private String comments;

    private MetierVO metier;

    private TripVO trip;
    private Integer tripId;

    private PhysicalGearVO physicalGear;
    private Integer physicalGearId;

    private List<MeasurementVO> measurements;
    private Map<Integer, String> measurementValues;

    private List<MeasurementVO> gearMeasurements;
    private Map<Integer, String> gearMeasurementValues;

    private BatchVO catchBatch;
    private List<BatchVO> batches;

    private List<ProductVO> products;

    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}

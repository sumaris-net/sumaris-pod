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
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.dao.technical.model.IUpdateDateEntityBean;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import net.sumaris.core.vo.referential.TaxonNameVO;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Data
@FieldNameConstants
@EqualsAndHashCode
public class BatchVO implements IUpdateDateEntityBean<Integer, Date> {

    @EqualsAndHashCode.Exclude
    private Integer id;
    private String comments;
    private Date updateDate;
    private Date controlDate;
    private Date validationDate;
    private Integer qualityFlagId;
    private DepartmentVO recorderDepartment;
    private PersonVO recorderPerson;

    private String label;
    private Integer rankOrder;
    private Boolean exhaustiveInventory;
    private Double samplingRatio;
    private String samplingRatioText;
    private Integer individualCount;
    private ReferentialVO taxonGroup;
    private TaxonNameVO taxonName;

    @EqualsAndHashCode.Exclude
    private OperationVO operation;
    private Integer operationId;

    @EqualsAndHashCode.Exclude
    private BatchVO parent;
    private Integer parentId;

    @EqualsAndHashCode.Exclude
    private List<BatchVO> children;

    @EqualsAndHashCode.Exclude
    private List<MeasurementVO> sortingMeasurements;
    @EqualsAndHashCode.Exclude
    private List<MeasurementVO> quantificationMeasurements;

    private Map<Integer, String> measurementValues;


    @EqualsAndHashCode.Exclude
    private Map<Integer, String> sortingMeasurementValues;     // TODO: remove (not used anymore)
    @EqualsAndHashCode.Exclude
    private Map<Integer, String> quantificationMeasurementValues;     // TODO: remove (not used anymore)


    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}

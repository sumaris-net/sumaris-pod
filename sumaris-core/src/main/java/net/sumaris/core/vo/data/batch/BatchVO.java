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

package net.sumaris.core.vo.data.batch;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.model.ITreeNodeEntity;
import net.sumaris.core.model.IWithFlagsValueObject;
import net.sumaris.core.model.data.IWithRecorderPersonEntity;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.data.*;
import net.sumaris.core.vo.referential.ReferentialVO;
import net.sumaris.core.vo.referential.taxon.TaxonNameVO;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Data
@ToString(onlyExplicitlyIncluded = true)
@FieldNameConstants
@EqualsAndHashCode
public class BatchVO implements IDataVO<Integer>,
        IWithRecorderPersonEntity<Integer, PersonVO>,
        IWithFlagsValueObject<Integer>,
        ITreeNodeEntity<Integer, BatchVO> {

    @EqualsAndHashCode.Exclude
    @ToString.Include
    private Integer id;
    @EqualsAndHashCode.Exclude
    private Date updateDate;
    private Date controlDate;

    /**
     * @deprecated Not in the Batch entity. (We add it just for compatibility with IDataVO interface)
     */
    @Deprecated
    private Date validationDate;

    private Date qualificationDate;
    private String qualificationComments;
    private Integer qualityFlagId;
    private DepartmentVO recorderDepartment;
    private PersonVO recorderPerson;

    @ToString.Include
    private String label;
    private Integer rankOrder;
    private Boolean exhaustiveInventory;
    private Double samplingRatio;
    private String samplingRatioText;
    private Integer individualCount;
    private Integer subgroupCount;
    private ReferentialVO taxonGroup;
    private TaxonNameVO taxonName;
    private String comments;

    // TODO: add it to entity
    private Integer locationId;

    @EqualsAndHashCode.Exclude
    private OperationVO operation;
    private Integer operationId;

    @EqualsAndHashCode.Exclude
    private SaleVO sale;
    private Integer saleId;

    @EqualsAndHashCode.Exclude
    private BatchVO parent;
    private Integer parentId;

    private List<BatchVO> children;

    private Map<Integer, String> measurementValues; // = sorting_measurement_b or quantification_measurement_b
    private List<MeasurementVO> sortingMeasurements; // = sorting_measurement_b (from a list)
    private List<QuantificationMeasurementVO> quantificationMeasurements; // = quantification_measurement_b (from a list)

    private List<ImageAttachmentVO> images;

    @JsonIgnore
    @EqualsAndHashCode.Exclude
    private int flags = 0;

}

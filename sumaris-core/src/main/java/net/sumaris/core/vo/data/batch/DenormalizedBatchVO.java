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
import com.google.common.collect.Lists;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.model.ITreeNodeEntity;
import net.sumaris.core.model.IUpdateDateEntity;
import net.sumaris.core.model.IValueObject;
import net.sumaris.core.vo.data.OperationVO;
import net.sumaris.core.vo.data.SaleVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import net.sumaris.core.vo.referential.TaxonNameVO;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Data
@ToString(onlyExplicitlyIncluded = true)
@FieldNameConstants
@EqualsAndHashCode
public class DenormalizedBatchVO
        implements IValueObject<Integer>,
    IUpdateDateEntity<Integer, Date>,
    ITreeNodeEntity<Integer, DenormalizedBatchVO> {

    @EqualsAndHashCode.Exclude
    @ToString.Include
    private Integer id;
    @EqualsAndHashCode.Exclude
    private Date updateDate;

    @ToString.Include
    private String label;
    private Integer rankOrder;
    private Short flatRankOrder;
    private Double weight;
    private Double indirectWeight;

    private Double indirectRtpWeight;
    private Double elevateRtpWeight;
    private Double elevateContextWeight;
    private Double indirectContextWeight;
    private Double elevateWeight;
    private Double taxonElevateContextWeight;
    private Integer individualCount;
    private Integer indirectIndividualCount;

    private Integer taxonElevateIndividualCount;
    private Integer  elevateIndividualCount;
    private Double samplingRatio;
    private String samplingRatioText;
    private Boolean exhaustiveInventory;
    private Short treeLevel;
    private String treeIndent;
    private String sortingValuesText;
    private Boolean isLanding;
    private Boolean isDiscard;
    private Integer weightMethodId;
    private Integer qualityFlagId;
    private Integer locationId;

    private ReferentialVO inheritedTaxonGroup;
    private ReferentialVO calculatedTaxonGroup;
    private ReferentialVO taxonGroup;

    private TaxonNameVO inheritedTaxonName;
    private TaxonNameVO taxonName;

    private String comments;

    private List<DenormalizedBatchVO> children;

    @EqualsAndHashCode.Exclude
    private DenormalizedBatchVO parent;
    private Integer parentId;

    @EqualsAndHashCode.Exclude
    private OperationVO operation;
    private Integer operationId;

    @EqualsAndHashCode.Exclude
    private SaleVO sale;
    private Integer saleId;

    private List<DenormalizedBatchSortingValueVO> sortingValues = Lists.newArrayList();
    private Map<Integer, String> measurementValues;

    @JsonIgnore
    public void addSortingValue(@NonNull DenormalizedBatchSortingValueVO sv) {
        getSortingValues().add(sv);
        sv.setBatch(this);
    }

}

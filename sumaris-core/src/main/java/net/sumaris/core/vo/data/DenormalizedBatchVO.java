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
import net.sumaris.core.dao.technical.model.ITreeNodeEntityBean;
import net.sumaris.core.dao.technical.model.IUpdateDateEntityBean;
import net.sumaris.core.dao.technical.model.IValueObject;
import net.sumaris.core.model.data.*;
import net.sumaris.core.model.referential.QualityFlag;
import net.sumaris.core.model.referential.pmfm.Method;
import net.sumaris.core.model.referential.taxon.ReferenceTaxon;
import net.sumaris.core.model.referential.taxon.TaxonGroup;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import net.sumaris.core.vo.referential.TaxonGroupVO;
import net.sumaris.core.vo.referential.TaxonNameVO;
import org.hibernate.annotations.Cascade;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Data
@FieldNameConstants
@EqualsAndHashCode
public class DenormalizedBatchVO implements IValueObject<Integer>,
        IUpdateDateEntityBean<Integer, Date>,
        ITreeNodeEntityBean<Integer, DenormalizedBatchVO> {

    @EqualsAndHashCode.Exclude
    private Integer id;
    @EqualsAndHashCode.Exclude
    private Date updateDate;

    private String label;
    private Integer rankOrder;
    private Short flatRankOrder;
    private Double weight;
    private Double indirectWeight;
    private Double elevateContextWeight;
    private Double indirectContextWeight;
    private Double elevateWeight;
    private Integer individualCount;
    private Integer indirectIndividualCount;
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

    private List<DenormalizedBatchVO> children = new ArrayList<>();

    @EqualsAndHashCode.Exclude
    private DenormalizedBatchVO parent;
    private Integer parentId;

    @EqualsAndHashCode.Exclude
    private Operation operation;
    private Integer operationId;

    @EqualsAndHashCode.Exclude
    private Sale sale;
    private Integer saleId;

    private List<DenormalizedBatchSortingValue> sortingValues = new ArrayList<>();

    public String toString() {
        return new StringBuilder().append("DenormalizedBatchVO(")
                .append("id=").append(id)
                .append(",label=").append(label)
                .append(")").toString();
    }
}
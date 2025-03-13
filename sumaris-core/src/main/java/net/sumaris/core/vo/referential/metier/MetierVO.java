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

package net.sumaris.core.vo.referential.metier;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.model.referential.metier.Metier;
import net.sumaris.core.vo.referential.ReferentialVO;
import net.sumaris.core.vo.referential.gear.GearVO;
import net.sumaris.core.vo.referential.taxon.TaxonGroupVO;

@Data
@FieldNameConstants
@EqualsAndHashCode(callSuper = true)
public class MetierVO extends ReferentialVO {

    private ReferentialVO gear;
    private GearVO fullGear;
    private TaxonGroupVO taxonGroup;

    public MetierVO() {
        this.setEntityName(Metier.ENTITY_NAME);
    }

//    @Override
//    public Map<String, Object> getProperties() {
//        Map<String, Object> properties = super.getProperties();
//        TaxonGroupVO taxonGroup = getTaxonGroup();
//        if (taxonGroup != null) {
//            return ImmutableMap.of(
//                StringUtils.doting(MetierVO.Fields.TAXON_GROUP, TaxonGroup.Fields.ID), taxonGroup.getId(),
//                StringUtils.doting(MetierVO.Fields.TAXON_GROUP, TaxonGroup.Fields.LABEL), taxonGroup.getLabel()
//            );
//        }
//
//        return null;
//    }
//
//    @Override
//    public void setProperties(Map<String, Object> properties) {
//        if (MapUtils.isEmpty(properties)) return;
//
//        Object taxonGroupId = properties.get(StringUtils.doting(MetierVO.Fields.TAXON_GROUP, TaxonGroup.Fields.ID));
//        Object taxonGroupLabel = properties.get(StringUtils.doting(MetierVO.Fields.TAXON_GROUP, TaxonGroup.Fields.LABEL));
//        if (taxonGroupId != null) {
//            TaxonGroupVO taxonGroup = new TaxonGroupVO();
//            taxonGroup.setId(Integer.parseInt(taxonGroupId.toString()));
//            taxonGroup.setLabel(taxonGroupLabel != null ? taxonGroupLabel.toString() : null);
//            setProperties(ImmutableMap.of(MetierVO.Fields.TAXON_GROUP, taxonGroup));
//            setTaxonGroup(taxonGroup);
//        }
//    }
}

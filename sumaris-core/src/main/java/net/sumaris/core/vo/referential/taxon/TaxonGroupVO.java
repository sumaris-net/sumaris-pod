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

package net.sumaris.core.vo.referential.taxon;

import lombok.Data;
import lombok.EqualsAndHashCode;
import net.sumaris.core.model.referential.taxon.TaxonGroup;
import net.sumaris.core.vo.referential.IReferentialVO;
import net.sumaris.core.vo.referential.ReferentialVO;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class TaxonGroupVO extends ReferentialVO {

    public interface Fields extends IReferentialVO.Fields {
        String TAXON_NAMES = "taxonNames";
    }

    // Fill using TaxonGroup2TaxonHierarchy (filled from TaxonGroupHistoricalRecord)
    List<TaxonNameVO> taxonNames;

    public TaxonGroupVO() {
        this.setEntityName(TaxonGroup.class.getSimpleName()); // Need by client (e.f. GraphQL cache)
    }
}

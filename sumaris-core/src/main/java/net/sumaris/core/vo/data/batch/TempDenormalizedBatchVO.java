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
import lombok.experimental.FieldNameConstants;

import java.math.BigDecimal;

@Data
@FieldNameConstants
@EqualsAndHashCode
public class TempDenormalizedBatchVO extends DenormalizedBatchVO {

    // Factors
    private BigDecimal samplingFactor;
    private BigDecimal elevateContextFactor;
    private BigDecimal elevateFactor;
    private BigDecimal taxonElevateFactor;

    private Double aliveWeightFactor;

    // Individual count
    private BigDecimal indirectIndividualCountDecimal;

    // Weights
    private Double rtpContextWeight;

    /**
     * Indirect RTP weight (not alive weight, and not elevate)
     */
    private Double indirectRtpContextWeight;

    /**
     * Elevate RTP weights (keeping dressing/perservation = not alive weight)
     */
    private Double elevateRtpContextWeight;

    private Double indirectRtpElevateWeight;

    private Double indirectElevateWeight;

    private Integer taxonGroupId;
    private Integer referenceTaxonId;

    @JsonIgnore
    public Integer getTaxonGroupId() {
        if (taxonGroupId == null) {
            taxonGroupId = this.getTaxonGroup() != null
                ? this.getTaxonGroup().getId()
                : (
                this.getInheritedTaxonGroup() != null
                    ? this.getInheritedTaxonGroup().getId()
                    // TODO: return the calculated taxon group ?
                    : null);
        }
        return taxonGroupId;
    }

    public Integer getReferenceTaxonId() {
        if (referenceTaxonId == null) {
            referenceTaxonId = this.getTaxonName() != null
                ? this.getTaxonName().getReferenceTaxonId()
                : (this.getInheritedTaxonName() != null
                ? this.getInheritedTaxonName().getReferenceTaxonId()
                : null);
        }
        return referenceTaxonId;
    }

    public boolean hasTaxonGroup() {
        return getTaxonGroupId() != null;
    }

    public boolean hasTaxonName() {
        return getReferenceTaxonId() != null;
    }
}

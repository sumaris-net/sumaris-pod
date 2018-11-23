package net.sumaris.core.vo.referential;

import lombok.Data;
import net.sumaris.core.model.referential.taxon.TaxonGroup;

@Data
public class TaxonNameVO extends ReferentialVO {

    private Integer referenceTaxonId;
    private Boolean isReferent;

    public TaxonNameVO() {
        this.setEntityName(TaxonGroup.class.getSimpleName());
    }
}